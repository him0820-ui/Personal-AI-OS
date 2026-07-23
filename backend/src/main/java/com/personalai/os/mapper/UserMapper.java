package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
/**
 * @description: 用户数据访问层
 * @author: 琦
 */
public interface UserMapper extends BaseMapper<User> {
}
