package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemoryGoal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * @description: 目标记忆数据访问层
 * @author: 琦
 */
public interface MemoryGoalMapper extends BaseMapper<MemoryGoal> {
}
