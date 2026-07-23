package com.personalai.os.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 聊天消息类，用于记忆提取的消息格式
 * @author: 琦
 */
@Data
public class ChatMessage {
    private String content;
    private String think;
    private String sender;
    private LocalDateTime timestamp;
}
