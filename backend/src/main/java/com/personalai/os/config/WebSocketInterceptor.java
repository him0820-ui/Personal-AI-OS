package com.personalai.os.config;

import com.personalai.os.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * @description: WebSocket握手拦截器，处理JWT认证
 * @author: 琦
 */
@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        logger.info("WebSocket handshake attempt from: {}", request.getRemoteAddress());

        String token = null;
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            token = servletRequest.getServletRequest().getParameter("token");
            if (token == null) {
                token = servletRequest.getServletRequest().getHeader("Authorization");
            }
        }

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        logger.info("WebSocket token found: {}", token != null ? "***" : "null");

        if (token != null && jwtUtil.isTokenValid(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            attributes.put("userId", userId);
            logger.info("WebSocket authenticated for user {}", userId);
            return true;
        }

        logger.warn("WebSocket authentication failed: invalid or missing token");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("WebSocket handshake error: {}", exception.getMessage());
        }
    }
}
