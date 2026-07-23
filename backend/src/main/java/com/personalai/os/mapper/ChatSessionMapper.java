package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * @description: 聊天会话数据访问层
 * @author: 琦
 */
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<ChatSession> findByUserIdOrderByUpdatedAt(Long userId);
}