package com.personalai.os.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 记忆属性实体类，存储用户记忆的属性级别数据
 * @author: 琦
 */
@Data
@TableName("memory_attribute")
public class MemoryAttribute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String category;

    private String entity;

    private String attribute;

    private String value;

    private Integer importance;

    private Integer confidence;

    private String sourceQuote;

    private Integer accessCount;

    private LocalDateTime lastAccessTime;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String oldValue;
}
