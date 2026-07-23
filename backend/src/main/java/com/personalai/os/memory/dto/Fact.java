package com.personalai.os.memory.dto;

import lombok.Data;

/**
 * @description: 事实记忆DTO，用于AI工具调用和记忆提取
 * @author: 琦
 */
@Data
public class Fact {
    private String key;
    private String value;
    private Integer importance;
    private Integer confidence;
    private String sourceQuote;
}