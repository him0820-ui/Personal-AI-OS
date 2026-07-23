package com.personalai.os.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 消息响应DTO
 * @author: 琦
 */
@Data
public class MessageResponse {
    private Long id;
    private String content;
    private String think;
    private String sender;
    private LocalDateTime timestamp;
}
