package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.Conversation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * @description: 对话记录数据访问层
 * @author: 琦
 */
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversation WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Conversation> findRecentByUserId(Long userId, Integer limit);

    @Select("SELECT * FROM conversation WHERE user_id = #{userId} ORDER BY created_at ASC")
    List<Conversation> findByUserIdOrderByCreatedAt(Long userId);

    @Select("SELECT * FROM conversation WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Conversation> findRecentBySessionId(Long sessionId, Integer limit);

    @Select("SELECT * FROM conversation WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<Conversation> findBySessionIdOrderByCreatedAt(Long sessionId);

    @Delete("DELETE FROM conversation WHERE session_id = #{sessionId}")
    void deleteBySessionId(Long sessionId);

    @Select("SELECT * FROM conversation WHERE user_id = #{userId} AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) ORDER BY created_at DESC")
    List<Conversation> findRecentDaysByUserId(Long userId, Integer days);
}
