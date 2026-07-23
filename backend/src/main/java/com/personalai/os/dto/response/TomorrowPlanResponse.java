package com.personalai.os.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * @description: 明日计划响应DTO
 * @author: 琦
 */
@Data
public class TomorrowPlanResponse {
    private LocalDate date;
    private String overview;
    private List<String> focusAreas;
    private List<Task> tasks;
    private String suggestions;

    @Data
    public static class Task {
        private String title;
        private String priority;
        private Integer estimatedHours;
        private String relatedGoal;
        private String description;
        private String timeSlot;
    }
}