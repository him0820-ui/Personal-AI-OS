package com.personalai.os.service;

import com.personalai.os.controller.WebSocketNotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 通知服务类，管理SSE连接和实时通知推送
 * @author: 琦
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    @Autowired
    private WebSocketNotificationController webSocketNotificationController;

    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        
        userEmitters.put(userId, emitter);
        logger.info("Created SSE emitter for user {}, total connections: {}", userId, userEmitters.size());

        emitter.onCompletion(() -> {
            userEmitters.remove(userId);
            logger.info("SSE connection completed for user {}, total connections: {}", userId, userEmitters.size());
        });

        emitter.onTimeout(() -> {
            userEmitters.remove(userId);
            logger.info("SSE connection timeout for user {}, total connections: {}", userId, userEmitters.size());
        });

        emitter.onError(e -> {
            userEmitters.remove(userId);
            logger.info("SSE connection error for user {}, total connections: {}", userId, userEmitters.size());
        });

        return emitter;
    }
    
    @Scheduled(cron = "0 */30 * * * ?")
    public void sendHeartbeat() {
        for (Map.Entry<Long, SseEmitter> entry : userEmitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                    .name("heartbeat")
                    .data("ping"));
            } catch (IOException e) {
                logger.warn("Failed to send heartbeat to user {}: {}", entry.getKey(), e.getMessage());
                userEmitters.remove(entry.getKey());
            }
        }
        logger.debug("Sent heartbeat to {} active SSE connections", userEmitters.size());
    }

    public void sendNotification(Long userId, String title, String message) {
        webSocketNotificationController.sendNotification(userId, title, message);
        
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                String notificationJson = "{\"type\":\"reminder\",\"title\":\"" + 
                    title.replace("\"", "\\\"") + "\",\"message\":\"" + 
                    message.replace("\"", "\\\"") + "\",\"timestamp\":\"" + 
                    java.time.LocalDateTime.now() + "\"}";
                logger.info("Sending SSE notification to user {}: {}", userId, notificationJson);
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notificationJson));
                logger.info("Sent SSE notification to user {}: {}", userId, title);
            } catch (IOException e) {
                logger.error("Failed to send SSE notification to user {}: {}", userId, e.getMessage());
                userEmitters.remove(userId);
            }
        } else {
            logger.info("No active SSE connection for user {}", userId);
        }
    }

    public void broadcast(String title, String message) {
        for (Map.Entry<Long, SseEmitter> entry : userEmitters.entrySet()) {
            sendNotification(entry.getKey(), title, message);
        }
    }

    public boolean hasActiveConnection(Long userId) {
        return userEmitters.containsKey(userId);
    }

    public int getActiveConnectionsCount() {
        return userEmitters.size();
    }
}
