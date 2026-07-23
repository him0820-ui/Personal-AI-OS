package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemoryTodo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * @description: 待办记忆数据访问层
 * @author: 琦
 */
public interface MemoryTodoMapper extends BaseMapper<MemoryTodo> {
}
