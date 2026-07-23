package com.personalai.os.controller;

import com.personalai.os.service.NotificationService;
import com.personalai.os.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * @description: 通知控制器，处理实时通知推送和状态查询
 * @author: 琦
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request, @RequestParam(required = false) String token) {
        logger.info("SSE stream request received, path: {}, method: {}", request.getRequestURI(), request.getMethod());
        
        Long userId = (Long) request.getAttribute("userId");
        logger.info("UserId from request attribute: {}", userId);
        
        String authHeader = request.getHeader("Authorization");
        logger.info("Authorization header present: {}", authHeader != null);
        
        if (userId == null && token != null) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token != null && !token.isEmpty()) {
                try {
                    Long parsedUserId = jwtUtil.getUserIdFromToken(token);
                    userId = parsedUserId;
                    logger.info("Parsed userId={} from query param token", userId);
                } catch (Exception e) {
                    logger.warn("Failed to parse token from query param: {}", e.getMessage());
                }
            }
        }
        
        if (userId == null) {
            logger.warn("User not authenticated for SSE stream");
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new IllegalArgumentException("User not authenticated"));
            return emitter;
        }
        
        logger.info("Creating SSE emitter for user {}", userId);
        return notificationService.createEmitter(userId);
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(Map.of(
            "hasActiveConnection", notificationService.hasActiveConnection(userId),
            "totalConnections", notificationService.getActiveConnectionsCount()
        ));
    }

    @PostMapping("/test")
    public ResponseEntity<?> testNotification(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String title = body.getOrDefault("title", "测试提醒");
        String message = body.getOrDefault("message", "这是一条测试提醒");
        
        notificationService.sendNotification(userId, title, message);
        return ResponseEntity.ok(Map.of("success", true, "message", "通知已发送"));
    }
}
