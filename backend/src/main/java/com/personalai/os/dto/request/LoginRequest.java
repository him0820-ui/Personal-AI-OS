package com.personalai.os.dto.request;

import lombok.Data;

/**
 * @description: 登录请求DTO
 * @author: 琦
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
