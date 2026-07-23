package com.personalai.os.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 时间线记忆实体类，存储用户的时间线事件
 * @author: 琦
 */
@Data
@TableName("memory_timeline")
public class MemoryTimeline {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private String description;
    
    private LocalDateTime timestamp;
    
    private Integer importance;
    
    private Integer confidence;
    
    @TableField("source_quote")
    private String sourceQuote;
    
    @TableField("access_count")
    private Integer accessCount;
    
    @TableField("last_access_time")
    private LocalDateTime lastAccessTime;
    
    @TableField("memory_type")
    private String memoryType;
    
    private String status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}