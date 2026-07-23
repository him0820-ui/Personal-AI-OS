package com.personalai.os.memory.dto;

import lombok.Data;

import java.util.List;

/**
 * @description: 记忆提取结果DTO，封装从对话中提取的各类记忆
 * @author: 琦
 */
@Data
public class ExtractResult {
    private List<Fact> facts;
    private List<Timeline> timelines;
    private List<Goal> goals;
    private List<Todo> todos;
    private List<Attribute> attributes;
}
