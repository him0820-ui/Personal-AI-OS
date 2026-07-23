package com.personalai.os.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * @description: WebSocket通知控制器，处理基于WebSocket的实时通知推送
 * @author: 琦
 */
@Controller
public class WebSocketNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendNotification(Long userId, String title, String message) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + userId,
                    Map.of(
                            "type", "reminder",
                            "title", title,
                            "message", message,
                            "timestamp", java.time.LocalDateTime.now().toString()
                    ));
            logger.info("WebSocket notification sent to user {}: {}", userId, title);
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }

    @MessageMapping("/subscribe-notifications")
    public void subscribeNotifications(@Payload Map<String, Object> payload) {
        Long userId = Long.parseLong(payload.get("userId").toString());
        logger.info("User {} subscribed to notifications", userId);
    }
}
