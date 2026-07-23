package com.personalai.os.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 记忆历史实体类，存储事实记忆的变更历史
 * @author: 琦
 */
@Data
@TableName("memory_fact_history")
public class MemoryFactHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long factId;
    
    private Long userId;
    
    @com.baomidou.mybatisplus.annotation.TableField("`key`")
    private String key;
    
    private String value;
    
    private Integer version;
    
    private Integer importance;
    
    private Integer confidence;
    
    private String sourceQuote;
    
    private String changeReason;
    
    private LocalDateTime createdAt;
}
