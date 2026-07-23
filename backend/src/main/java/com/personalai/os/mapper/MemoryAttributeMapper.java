package com.personalai.os.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.personalai.os.entity.MemoryAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * @description: 记忆属性数据访问层
 * @author: 琦
 */
public interface MemoryAttributeMapper extends BaseMapper<MemoryAttribute> {

    default List<MemoryAttribute> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<MemoryAttribute>()
                .eq(MemoryAttribute::getUserId, userId)
                .eq(MemoryAttribute::getStatus, "ACTIVE"));
    }

    default List<MemoryAttribute> selectByCategory(Long userId, String category) {
        return selectList(new LambdaQueryWrapper<MemoryAttribute>()
                .eq(MemoryAttribute::getUserId, userId)
                .eq(MemoryAttribute::getCategory, category)
                .eq(MemoryAttribute::getStatus, "ACTIVE"));
    }

    default MemoryAttribute selectUnique(Long userId, String category, String entity, String attribute) {
        return selectOne(new LambdaQueryWrapper<MemoryAttribute>()
                .eq(MemoryAttribute::getUserId, userId)
                .eq(MemoryAttribute::getCategory, category)
                .eq(MemoryAttribute::getEntity, entity)
                .eq(MemoryAttribute::getAttribute, attribute)
                .eq(MemoryAttribute::getStatus, "ACTIVE"));
    }

    @Select("SELECT * FROM memory_attribute WHERE user_id = #{userId} AND category = #{category} ORDER BY importance DESC LIMIT #{limit}")
    List<MemoryAttribute> selectTopByCategory(Long userId, String category, Integer limit);

    @Select("SELECT DISTINCT category FROM memory_attribute WHERE user_id = #{userId} AND status = 'ACTIVE'")
    List<String> selectDistinctCategories(Long userId);

    @Select("SELECT DISTINCT entity FROM memory_attribute WHERE user_id = #{userId} AND category = #{category} AND status = 'ACTIVE'")
    List<String> selectDistinctEntities(Long userId, String category);

    @Select("SELECT * FROM memory_attribute WHERE user_id = #{userId} AND entity LIKE CONCAT('%', #{keyword}, '%') OR attribute LIKE CONCAT('%', #{keyword}, '%') OR value LIKE CONCAT('%', #{keyword}, '%')")
    List<MemoryAttribute> searchByKeyword(Long userId, String keyword);
}
