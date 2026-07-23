package com.personalai.os.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 记忆冲突实体类，存储检测到的记忆冲突记录
 * @author: 琦
 */
@Data
@TableName("memory_conflict")
public class MemoryConflict {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long factId;
    
    @com.baomidou.mybatisplus.annotation.TableField("`key`")
    private String key;
    
    private String oldValue;
    
    private String newValue;
    
    private Integer conflictScore;
    
    private String conflictType;
    
    private String reviewStatus;
    
    private String reviewResult;
    
    private String aiAnalysis;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime reviewedAt;
    
    public enum ConflictType {
        CONTRADICTION, 
        CHANGE, 
        LOW_CONFIDENCE, 
        SOURCE_ANOMALY
    }
    
    public enum ReviewStatus {
        PENDING, 
        RESOLVED, 
        IGNORED
    }
    
    public enum ReviewResult {
        OVERWRITE, 
        KEEP_HISTORY, 
        DELETE_NEW, 
        DELETE_OLD
    }
}
