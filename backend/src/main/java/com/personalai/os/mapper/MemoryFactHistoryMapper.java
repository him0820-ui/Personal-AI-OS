package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemoryFactHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
/**
 * @description: 记忆历史数据访问层
 * @author: 琦
 */
public interface MemoryFactHistoryMapper extends BaseMapper<MemoryFactHistory> {
    
    List<MemoryFactHistory> selectByFactId(@Param("factId") Long factId);
    
    Integer selectMaxVersion(@Param("factId") Long factId);
}
