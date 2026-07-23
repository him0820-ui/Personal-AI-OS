package com.personalai.os.dto.request;

import lombok.Data;

/**
 * @description: 注册请求DTO
 * @author: 琦
 */
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
}
