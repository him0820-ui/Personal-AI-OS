package com.personalai.os.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 记忆总结实体类，存储用户记忆摘要和统计信息
 * @author: 琦
 */
@Data
@TableName("memory_summary")
public class MemorySummary {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String summary;
    
    private Integer factCount;
    
    private Integer goalCount;
    
    private Integer todoCount;
    
    private Integer timelineCount;
    
    private String tags;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime createdAt;
}
