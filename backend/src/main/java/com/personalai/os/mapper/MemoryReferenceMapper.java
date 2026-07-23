package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemoryReference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @description: 记忆引用数据访问层
 * @author: 琦
 */
public interface MemoryReferenceMapper extends BaseMapper<MemoryReference> {
    
    List<MemoryReference> selectByUserId(@Param("userId") Long userId);
    
    List<MemoryReference> selectByFactId(@Param("factId") Long factId);
    
    Integer countReferencesByFactId(@Param("factId") Long factId);
    
    Integer sumImportanceGainByFactId(@Param("factId") Long factId);
}
