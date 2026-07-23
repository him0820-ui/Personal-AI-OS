package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemoryTimeline;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * @description: 时间线记忆数据访问层
 * @author: 琦
 */
public interface MemoryTimelineMapper extends BaseMapper<MemoryTimeline> {
}
