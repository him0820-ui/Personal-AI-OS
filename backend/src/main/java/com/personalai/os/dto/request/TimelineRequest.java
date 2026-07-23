package com.personalai.os.dto.request;

import lombok.Data;

/**
 * @description: 时间线记忆请求DTO
 * @author: 琦
 */
@Data
public class TimelineRequest {
    private String title;
    private String description;
    private String timestamp;
}
