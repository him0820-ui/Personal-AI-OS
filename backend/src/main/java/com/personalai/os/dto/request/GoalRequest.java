package com.personalai.os.dto.request;

import lombok.Data;

/**
 * @description: 目标记忆请求DTO
 * @author: 琦
 */
@Data
public class GoalRequest {
    private String title;
    private String description;
    private Integer progress;
    private Integer priority;
}
