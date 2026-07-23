package com.personalai.os.dto.request;

import lombok.Data;

/**
 * @description: 事实记忆请求DTO
 * @author: 琦
 */
@Data
public class FactRequest {
    private String key;
    private String value;
    private Integer score;
}
