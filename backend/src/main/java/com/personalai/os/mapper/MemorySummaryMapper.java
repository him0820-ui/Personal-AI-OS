package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemorySummary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * @description: 记忆总结数据访问层
 * @author: 琦
 */
public interface MemorySummaryMapper extends BaseMapper<MemorySummary> {
}
