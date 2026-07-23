package com.personalai.os.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @description: 待办记忆实体类，存储用户的待办事项
 * @author: 琦
 */
@Data
@TableName("memory_todo")
public class MemoryTodo {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String title;
    
    private Boolean completed;
    
    private Integer priority;
    
    private Integer importance;
    
    private Integer confidence;
    
    @TableField("source_quote")
    private String sourceQuote;
    
    private LocalDate dueDate;
    
    @TableField("reminder_time")
    private LocalDateTime reminderTime;
    
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