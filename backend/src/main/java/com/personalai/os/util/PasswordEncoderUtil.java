package com.personalai.os.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @description: 密码加密工具类，用于生成BCrypt密码哈希值
 * @author: 琦
 */
public class PasswordEncoderUtil {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("5201314"));
    }
}