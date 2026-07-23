package com.personalai.os.controller;

import com.personalai.os.dto.request.ChatRequest;
import com.personalai.os.dto.response.MessageResponse;
import com.personalai.os.entity.ChatSession;
import com.personalai.os.entity.Conversation;
import com.personalai.os.service.AiService;
import com.personalai.os.service.ChatService;
import com.personalai.os.service.ChatSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @description: 聊天控制器，处理会话管理和AI对话交互
 * @author: 琦
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    private ChatService chatService;

    @Autowired
    private AiService aiService;

    @Autowired
    private ChatSessionService chatSessionService;

    @PostMapping("/session")
    public ResponseEntity<?> createSession(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ChatSession session = chatSessionService.createSession(userId);
        logger.info("Created new session for user {}: {}", userId, session.getId());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(chatSessionService.getSessions(userId));
    }

    @PutMapping("/session/{sessionId}/title")
    public ResponseEntity<?> updateSessionTitle(HttpServletRequest request, @PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String title = body.get("title");
        ChatSession session = chatSessionService.updateTitle(userId, sessionId, title);
        if (session != null) {
            return ResponseEntity.ok(session);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<?> deleteSession(HttpServletRequest request, @PathVariable Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        chatSessionService.deleteSession(userId, sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(HttpServletRequest request, @RequestBody ChatRequest chatRequest) {
        Long userId = (Long) request.getAttribute("userId");
        String userMessage = chatRequest.getMessage();
        Long sessionId = chatRequest.getSessionId();

        if (sessionId == null) {
            ChatSession recentSession = chatSessionService.getRecentSession(userId);
            if (recentSession != null) {
                sessionId = recentSession.getId();
                logger.info("Using recent session {} for user {}", sessionId, userId);
            } else {
                ChatSession session = chatSessionService.createSession(userId);
                sessionId = session.getId();
                logger.info("Created new session {} for user {}", sessionId, userId);
            }
        }

        logger.info("User {} sending message to session {}: {}", userId, sessionId, userMessage);

        chatService.saveUserMessage(userId, sessionId, userMessage);

        try {
            AiService.ThinkContent thinkContent = chatService.processWithToolCalling(userId, sessionId, userMessage);
            String response = thinkContent.content();
            String think = thinkContent.think();

            chatService.saveAiMessage(userId, sessionId, response, think);

            MessageResponse messageResponse = new MessageResponse();
            messageResponse.setContent(response);
            messageResponse.setThink(think);
            messageResponse.setSender("ai");
            messageResponse.setTimestamp(java.time.LocalDateTime.now());

            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("message", messageResponse);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(HttpServletRequest request, @RequestBody ChatRequest chatRequest) {
        Long userId = (Long) request.getAttribute("userId");
        String userMessage = chatRequest.getMessage();
        String messageId = chatRequest.getMessageId();
        Long sessionId = chatRequest.getSessionId();

        if (sessionId == null) {
            ChatSession session = chatSessionService.createSession(userId);
            sessionId = session.getId();
        }

        logger.info("User {} sending message to session {}: {}, messageId: {}", userId, sessionId, userMessage, messageId);

        chatService.saveUserMessage(userId, sessionId, userMessage);

        SseEmitter emitter = new SseEmitter(120000L);
        logger.info("SseEmitter created for user {}, session {}", userId, sessionId);

        StringBuilder fullContent = new StringBuilder();
        StringBuilder fullThink = new StringBuilder();
        final Long finalUserId = userId;
        final Long finalSessionId = sessionId;
        final String finalMessageId = messageId;
        final boolean[] saved = {false};

        logger.info("Starting streaming task for user {}, session {}", userId, sessionId);
        chatService.processWithToolCallingStream(finalUserId, finalSessionId, userMessage)
                .doOnNext(chunk -> {
                    try {
                        emitter.send(chunk, MediaType.TEXT_PLAIN);
                        if (chunk.startsWith("think:")) {
                            fullThink.append(chunk.substring(6));
                        } else if (chunk.startsWith("content:")) {
                            fullContent.append(chunk.substring(8));
                        }
                    } catch (IOException e) {
                        logger.error("Error sending chunk to client: {}", e.getMessage());
                    }
                })
                .doOnComplete(() -> {
                    try {
                        logger.info("Stream completed for user {}: {} chars content, {} chars think",
                            finalUserId, fullContent.length(), fullThink.length());
                        if (!saved[0] && (fullContent.length() > 0 || fullThink.length() > 0)) {
                            chatService.saveStreamResult(finalUserId, finalSessionId,
                                fullContent.toString(), fullThink.toString());
                            saved[0] = true;
                        }
                        emitter.complete();
                    } catch (Exception e) {
                        logger.error("Error on stream completion: {}", e.getMessage(), e);
                    }
                })
                .doOnError(e -> {
                    logger.error("Stream error for user {}: {}", finalUserId, e.getMessage(), e);
                    try {
                        String errorMessage = e.getMessage();
                        if (errorMessage == null) errorMessage = "Unknown error";
                        if (errorMessage.contains("Connection refused") || errorMessage.contains("Failed to connect")) {
                            errorMessage = "AI服务未启动，请确保Ollama服务已运行。";
                        }
                        emitter.send("error:" + errorMessage, MediaType.TEXT_PLAIN);
                    } catch (IOException ie) {
                        logger.error("Error sending error to client: {}", ie.getMessage());
                    }
                    try {
                        emitter.complete();
                    } catch (Exception ce) {
                        logger.error("Error completing emitter: {}", ce.getMessage());
                    }
                })
                .subscribe();

        emitter.onCompletion(() -> {
            logger.info("SSE connection completed for user {}, saving partial result if any", finalUserId);
            if (!saved[0] && (fullContent.length() > 0 || fullThink.length() > 0)) {
                try {
                    chatService.saveStreamResult(finalUserId, finalSessionId,
                        fullContent.toString(), fullThink.toString());
                    saved[0] = true;
                    logger.info("Saved partial result: {} chars content, {} chars think",
                        fullContent.length(), fullThink.length());
                } catch (Exception e) {
                    logger.error("Error saving partial stream result on completion: {}", e.getMessage(), e);
                }
            }
        });

        emitter.onTimeout(() -> {
            logger.warn("SSE connection timeout for user {}", userId);
            if (!saved[0] && (fullContent.length() > 0 || fullThink.length() > 0)) {
                try {
                    chatService.saveStreamResult(finalUserId, finalSessionId,
                        fullContent.toString(), fullThink.toString());
                    saved[0] = true;
                    logger.info("Saved partial result on timeout: {} chars content, {} chars think",
                        fullContent.length(), fullThink.length());
                } catch (Exception e) {
                    logger.error("Error saving partial stream result on timeout: {}", e.getMessage(), e);
                }
            }
            emitter.complete();
        });

        emitter.onError(e -> {
            logger.error("SSE connection error for user {}: {}", userId, e.getMessage());
            if (!saved[0] && (fullContent.length() > 0 || fullThink.length() > 0)) {
                try {
                    chatService.saveStreamResult(finalUserId, finalSessionId,
                        fullContent.toString(), fullThink.toString());
                    saved[0] = true;
                    logger.info("Saved partial result on error: {} chars content, {} chars think",
                        fullContent.length(), fullThink.length());
                } catch (Exception ex) {
                    logger.error("Error saving partial stream result on error: {}", ex.getMessage(), ex);
                }
            }
        });

        return emitter;
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(HttpServletRequest request, @RequestParam(required = false) Long sessionId) {
        Long userId = (Long) request.getAttribute("userId");

        if (sessionId != null) {
            var conversations = chatService.getAllMessages(sessionId);
            var response = conversations.stream()
                    .map(c -> {
                        MessageResponse mr = new MessageResponse();
                        mr.setId(c.getId());
                        mr.setContent(c.getContent());
                        mr.setThink(c.getThink());
                        mr.setSender(c.getSender());
                        mr.setTimestamp(c.getCreatedAt());
                        return mr;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().build();
    }
}