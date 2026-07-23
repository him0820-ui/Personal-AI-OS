package com.personalai.os.memory.dto;

import lombok.Data;

/**
 * @description: 属性记忆DTO，用于AI工具调用和记忆提取
 * @author: 琦
 */
@Data
public class Attribute {
    private String category;
    private String entity;
    private String attribute;
    private String value;
    private Integer importance;
    private Integer confidence;
    private String sourceQuote;
}
