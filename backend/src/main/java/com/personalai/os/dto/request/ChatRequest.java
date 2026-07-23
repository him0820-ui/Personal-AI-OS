package com.personalai.os.dto.request;

import lombok.Data;

/**
 * @description: 聊天请求DTO
 * @author: 琦
 */
@Data
public class ChatRequest {
    private String message;
    private String messageId;
    private Long sessionId;
}
