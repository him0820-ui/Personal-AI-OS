package com.personalai.os.service;

import com.personalai.os.entity.ChatSession;
import com.personalai.os.entity.Conversation;
import com.personalai.os.mapper.ChatSessionMapper;
import com.personalai.os.mapper.ConversationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description: 聊天会话服务类，管理对话会话的创建、查询和删除
 * @author: 琦
 */
@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    public ChatSession createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle("新会话");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null && !title.trim().isEmpty() ? title : "新会话");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    public List<ChatSession> getSessions(Long userId) {
        return chatSessionMapper.findByUserIdOrderByUpdatedAt(userId);
    }

    public ChatSession getSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null && session.getUserId().equals(userId)) {
            return session;
        }
        return null;
    }

    public ChatSession updateTitle(Long userId, Long sessionId, String title) {
        ChatSession session = getSession(userId, sessionId);
        if (session != null) {
            session.setTitle(title != null && !title.trim().isEmpty() ? title : "新会话");
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
        return session;
    }

    public void deleteSession(Long userId, Long sessionId) {
        ChatSession session = getSession(userId, sessionId);
        if (session != null) {
            conversationMapper.deleteBySessionId(sessionId);
            chatSessionMapper.deleteById(sessionId);
        }
    }

    public void updateSessionTime(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }

    public ChatSession getRecentSession(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.findByUserIdOrderByUpdatedAt(userId);
        if (sessions != null && !sessions.isEmpty()) {
            return sessions.get(0);
        }
        return null;
    }
}