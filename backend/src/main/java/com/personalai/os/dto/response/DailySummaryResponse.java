package com.personalai.os.dto.response;

import lombok.Data;

import java.time.LocalDate;

/**
 * @description: 每日总结响应DTO
 * @author: 琦
 */
@Data
public class DailySummaryResponse {
    private LocalDate date;
    private String summary;
    private Double completionRate;
    private String suggestions;
}
