package com.personalai.os.dto.response;

import lombok.Data;

import java.util.List;

/**
 * @description: 任务推荐响应DTO
 * @author: 琦
 */
@Data
public class TaskRecommendationResponse {
    private List<Recommendation> recommendations;
    private Integer totalTasks;
    private String message;

    @Data
    public static class Recommendation {
        private Integer rank;
        private String task;
        private String reason;
        private String priority;
        private Integer estimatedHours;
        private String relatedGoal;
    }
}