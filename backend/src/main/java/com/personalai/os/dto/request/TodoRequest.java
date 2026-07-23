package com.personalai.os.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * @description: 待办记忆请求DTO
 * @author: 琦
 */
@Data
public class TodoRequest {
    private String title;
    private Boolean completed;
    private Integer priority;
    private LocalDate dueDate;
}
