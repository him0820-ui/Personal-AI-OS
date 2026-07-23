package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemoryConflict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @description: 记忆冲突数据访问层
 * @author: 琦
 */
public interface MemoryConflictMapper extends BaseMapper<MemoryConflict> {
    
    List<MemoryConflict> selectPendingByUserId(@Param("userId") Long userId);
    
    List<MemoryConflict> selectByUserId(@Param("userId") Long userId);
    
    int countPendingByUserId(@Param("userId") Long userId);
}
