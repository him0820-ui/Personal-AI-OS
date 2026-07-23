package com.personalai.os.dto.response;

import lombok.Data;

/**
 * @description: 登录响应DTO
 * @author: 琦
 */
@Data
public class LoginResponse {
    private String token;
    private String username;
    private Long userId;
}
