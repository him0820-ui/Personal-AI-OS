package com.personalai.os.controller;

import com.personalai.os.entity.Conversation;
import com.personalai.os.service.ChatService;
import com.personalai.os.service.AiService.ToolCall;
import com.personalai.os.service.ChatService;
import com.personalai.os.config.ToolCallingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: WebSocket聊天控制器，处理基于WebSocket的实时聊天消息
 * @author: 琦
 */
@Controller
@CrossOrigin(origins = "*")
public class WebSocketChatController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ToolCallingConfig toolCallingConfig;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<Long, StringBuilder> sessionBuffer = new ConcurrentHashMap<>();
    private final Map<Long, StringBuilder> sessionThinkBuffer = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> sessionSaved = new ConcurrentHashMap<>();

    @MessageMapping("/chat")
    public void handleChatMessage(@Payload Map<String, Object> payload) {
        try {
            Long userId = Long.parseLong(payload.get("userId").toString());
            Long sessionId = Long.parseLong(payload.get("sessionId").toString());
            String message = (String) payload.get("message");

            logger.info("WebSocket chat message received: userId={}, sessionId={}, message={}", userId, sessionId, message);

            chatService.saveUserMessage(userId, sessionId, message);

            sessionBuffer.put(sessionId, new StringBuilder());
            sessionThinkBuffer.put(sessionId, new StringBuilder());
            sessionSaved.put(sessionId, false);

            processChatWithToolCalling(userId, sessionId, message);

        } catch (Exception e) {
            logger.error("Error handling WebSocket chat message: {}", e.getMessage(), e);
            messagingTemplate.convertAndSend("/topic/chat/" + payload.get("sessionId"),
                    Map.of("type", "error", "message", e.getMessage()));
        }
    }

    private void processChatWithToolCalling(Long userId, Long sessionId, String userMessage) {
        chatService.processWithToolCallingStream(userId, sessionId, userMessage)
                .doOnNext(chunk -> {
                    try {
                        if (chunk.startsWith("think:")) {
                            sessionThinkBuffer.get(sessionId).append(chunk.substring(6));
                        } else if (chunk.startsWith("content:")) {
                            sessionBuffer.get(sessionId).append(chunk.substring(8));
                        }

                        messagingTemplate.convertAndSend("/topic/chat/" + sessionId,
                                Map.of("type", "stream", "content", chunk));

                    } catch (Exception e) {
                        logger.error("Error sending chunk: {}", e.getMessage());
                    }
                })
                .doOnComplete(() -> {
                    try {
                        StringBuilder fullContent = sessionBuffer.get(sessionId);
                        StringBuilder fullThink = sessionThinkBuffer.get(sessionId);

                        if (!sessionSaved.get(sessionId) && (fullContent.length() > 0 || fullThink.length() > 0)) {
                            chatService.saveStreamResult(userId, sessionId,
                                    fullContent.toString(), fullThink.toString());
                            sessionSaved.put(sessionId, true);
                        }

                        messagingTemplate.convertAndSend("/topic/chat/" + sessionId,
                                Map.of("type", "complete"));

                        logger.info("WebSocket chat completed: sessionId={}, content={}, think={}",
                                sessionId, fullContent.length(), fullThink.length());

                    } catch (Exception e) {
                        logger.error("Error on chat completion: {}", e.getMessage());
                    } finally {
                        sessionBuffer.remove(sessionId);
                        sessionThinkBuffer.remove(sessionId);
                        sessionSaved.remove(sessionId);
                    }
                })
                .subscribe(
                        chunk -> {},
                        error -> {
                            logger.error("Chat stream error: {}", error.getMessage());
                            try {
                                StringBuilder fullContent = sessionBuffer.get(sessionId);
                                StringBuilder fullThink = sessionThinkBuffer.get(sessionId);

                                if (!sessionSaved.get(sessionId) && (fullContent != null && fullContent.length() > 0 || fullThink != null && fullThink.length() > 0)) {
                                    chatService.saveStreamResult(userId, sessionId,
                                            fullContent != null ? fullContent.toString() : "",
                                            fullThink != null ? fullThink.toString() : "");
                                }

                                messagingTemplate.convertAndSend("/topic/chat/" + sessionId,
                                        Map.of("type", "error", "message", error.getMessage()));
                            } catch (Exception e) {
                                logger.error("Error handling stream error: {}", e.getMessage());
                            } finally {
                                sessionBuffer.remove(sessionId);
                                sessionThinkBuffer.remove(sessionId);
                                sessionSaved.remove(sessionId);
                            }
                        }
                );
    }
}
