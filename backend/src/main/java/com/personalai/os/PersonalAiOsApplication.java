package com.personalai.os;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @description: 个人AI操作系统主启动类，基于Spring Boot框架
 * @author: 琦
 */
@SpringBootApplication
@MapperScan("com.personalai.os.mapper")
@EnableScheduling
public class PersonalAiOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalAiOsApplication.class, args);
    }
}
