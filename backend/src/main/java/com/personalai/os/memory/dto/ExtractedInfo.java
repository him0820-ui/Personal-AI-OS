package com.personalai.os.memory.dto;

import lombok.Data;

/**
 * @description: 提取信息DTO，用于封装从对话中提取的单条记忆信息
 * @author: 琦
 */
@Data
public class ExtractedInfo {
    private String content;
    private String category;
    private String key;
    private Integer score;
}
