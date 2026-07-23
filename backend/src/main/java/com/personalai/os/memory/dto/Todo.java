package com.personalai.os.memory.dto;

import lombok.Data;

/**
 * @description: 待办记忆DTO，用于AI工具调用和记忆提取
 * @author: 琦
 */
@Data
public class Todo {
    private String title;
    private String description;
    private Integer importance;
    private Integer confidence;
    private String sourceQuote;
}