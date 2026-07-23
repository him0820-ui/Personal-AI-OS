package com.personalai.os.memory.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 记忆项通用DTO，用于记忆的统一表示和传输
 * @author: 琦
 */
@Data
public class MemoryItem {
    private Long userId;
    private String type;
    private String key;
    private String value;
    private String event;
    private String title;
    private String description;
    private Integer progress;
    private Integer score;
    private String category;
    private String content;
    private Boolean completed;
    private Integer priority;
    private LocalDateTime occurredAt;
}
