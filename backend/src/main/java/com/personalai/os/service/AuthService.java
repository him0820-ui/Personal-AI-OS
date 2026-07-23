package com.personalai.os.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalai.os.dto.request.LoginRequest;
import com.personalai.os.dto.request.RegisterRequest;
import com.personalai.os.dto.response.LoginResponse;
import com.personalai.os.entity.User;
import com.personalai.os.mapper.UserMapper;
import com.personalai.os.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @description: 用户认证服务类，处理登录、注册和用户信息查询
 * @author: 琦
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt with username/email: {}", request.getUsername());
        
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getUsername()));
        }
        
        if (user == null) {
            logger.warn("User not found: {}", request.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }
        
        logger.info("User found: {}, password length: {}", user.getUsername(), user.getPassword().length());
        
        boolean passwordMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
        logger.info("Password match result: {}, input password: {}", passwordMatch, request.getPassword());
        
        if (!passwordMatch) {
            logger.warn("Password mismatch for user: {}", user.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setUserId(user.getId());
        
        logger.info("Login successful for user: {}", user.getUsername());
        return response;
    }

    public void register(RegisterRequest request) {
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        if (request.getEmail() != null) {
            User existingEmail = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getEmail()));
            if (existingEmail != null) {
                throw new RuntimeException("邮箱已被注册");
            }
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
        
        logger.info("User registered: {}", request.getUsername());
    }

    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
}
