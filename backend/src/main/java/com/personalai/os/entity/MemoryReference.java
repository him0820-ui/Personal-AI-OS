package com.personalai.os.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description: 记忆引用实体类，存储记忆之间的引用关系
 * @author: 琦
 */
@Data
@TableName("memory_reference")
public class MemoryReference {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private Long sessionId;
    
    private Long factId;
    
    @com.baomidou.mybatisplus.annotation.TableField("`key`")
    private String key;
    
    private String referencedContent;
    
    private String aiResponse;
    
    private Integer importanceGain;
    
    private LocalDateTime referencedAt;
}
