package com.personalai.os.service;

import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.mapper.MemoryAttributeMapper;
import com.personalai.os.memory.dto.Fact;
import com.personalai.os.memory.dto.Goal;
import com.personalai.os.memory.dto.Timeline;
import com.personalai.os.memory.dto.Todo;
import com.personalai.os.service.ConflictDetectionService.ConflictDetectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description: 记忆属性服务类，处理属性级别的记忆存储和冲突检测
 * @author: 琦
 */
@Service
public class MemoryAttributeService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryAttributeService.class);

    @Autowired
    private MemoryAttributeMapper attributeMapper;

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Transactional
    public void writeAttribute(Long userId, String category, String entity, String attribute, 
                               String value, Integer importance, Integer confidence, 
                               String sourceQuote) {
        logger.info("Writing attribute - userId={}, category={}, entity={}, attribute={}, value={}", 
            userId, category, entity, attribute, value);

        MemoryAttribute existing = attributeMapper.selectUnique(userId, category, entity, attribute);

        if (existing != null) {
            if (existing.getValue().equals(value)) {
                logger.info("Same value, no change needed");
                return;
            }

            ConflictDetectionResult conflictResult = conflictDetectionService.detectAttributeConflict(
                userId, existing.getId(), category, entity, attribute, existing.getValue(), value);

            if (conflictResult.hasConflict()) {
                logger.warn("Conflict detected - category={}, entity={}, attribute={}", category, entity, attribute);
                return;
            }

            existing.setValue(value);
            existing.setImportance(importance != null ? importance : existing.getImportance());
            existing.setConfidence(confidence != null ? confidence : existing.getConfidence());
            existing.setSourceQuote(sourceQuote != null ? sourceQuote : existing.getSourceQuote());
            existing.setUpdatedAt(LocalDateTime.now());
            attributeMapper.updateById(existing);
            vectorStoreService.storeAttribute(existing);
            logger.info("Attribute updated - id={}", existing.getId());
        } else {
            MemoryAttribute newAttr = new MemoryAttribute();
            newAttr.setUserId(userId);
            newAttr.setCategory(category);
            newAttr.setEntity(entity);
            newAttr.setAttribute(attribute);
            newAttr.setValue(value);
            newAttr.setImportance(importance != null ? importance : 50);
            newAttr.setConfidence(confidence != null ? confidence : 0);
            newAttr.setSourceQuote(sourceQuote);
            newAttr.setAccessCount(0);
            newAttr.setLastAccessTime(LocalDateTime.now());
            newAttr.setStatus("ACTIVE");
            newAttr.setCreatedAt(LocalDateTime.now());
            newAttr.setUpdatedAt(LocalDateTime.now());
            attributeMapper.insert(newAttr);
            vectorStoreService.storeAttribute(newAttr);
            logger.info("New attribute created - id={}", newAttr.getId());
        }
    }

    public void writeFactAsAttribute(Long userId, Fact fact) {
        String category = inferCategory(fact.getKey());
        String entity = fact.getKey();
        String attribute = "Value";
        String value = fact.getValue();

        writeAttribute(userId, category, entity, attribute, value, 
            fact.getImportance(), fact.getConfidence(), fact.getSourceQuote());
    }

    public void writeTimelineAsAttribute(Long userId, Timeline timeline) {
        writeAttribute(userId, "Timeline", timeline.getTitle(), "Description", 
            timeline.getDescription(), timeline.getImportance(), 
            timeline.getConfidence(), timeline.getSourceQuote());
    }

    public void writeGoalAsAttribute(Long userId, Goal goal) {
        writeAttribute(userId, "Goal", goal.getTitle(), "Description", 
            goal.getDescription(), goal.getImportance(), 
            goal.getConfidence(), goal.getSourceQuote());
    }

    public void writeTodoAsAttribute(Long userId, Todo todo) {
        writeAttribute(userId, "Todo", todo.getTitle(), "Status", 
            "未完成", todo.getImportance(), 
            todo.getConfidence(), todo.getSourceQuote());
    }

    private String inferCategory(String key) {
        if (key == null) return "Fact";
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("姓名") || lowerKey.contains("年龄") || lowerKey.contains("性别") || 
            lowerKey.contains("专业") || lowerKey.contains("学校") || lowerKey.contains("职业")) {
            return "Person";
        }
        if (lowerKey.contains("喜欢") || lowerKey.contains("讨厌") || lowerKey.contains("爱好")) {
            return "Preference";
        }
        if (lowerKey.contains("掌握") || lowerKey.contains("技能") || lowerKey.contains("能力")) {
            return "Skill";
        }
        return "Fact";
    }

    public List<MemoryAttribute> getAttributesByUserId(Long userId) {
        return attributeMapper.selectByUserId(userId);
    }

    public List<MemoryAttribute> getAttributesByCategory(Long userId, String category) {
        return attributeMapper.selectByCategory(userId, category);
    }

    public MemoryAttribute getAttribute(Long userId, String category, String entity, String attribute) {
        return attributeMapper.selectUnique(userId, category, entity, attribute);
    }

    public List<String> getCategories(Long userId) {
        return attributeMapper.selectDistinctCategories(userId);
    }

    public List<String> getEntities(Long userId, String category) {
        return attributeMapper.selectDistinctEntities(userId, category);
    }

    public List<MemoryAttribute> search(Long userId, String keyword) {
        return attributeMapper.searchByKeyword(userId, keyword);
    }

    /**
     * 根据ID删除记忆属性（软删除，将状态设置为DELETED）
     */
    public void deleteAttribute(Long userId, Long id) {
        MemoryAttribute attr = attributeMapper.selectById(id);
        if (attr == null) {
            throw new RuntimeException("Attribute not found");
        }
        if (!attr.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此记忆属性");
        }
        attr.setStatus("DELETED");
        attr.setUpdatedAt(LocalDateTime.now());
        attributeMapper.updateById(attr);
        logger.info("Attribute deleted (soft) - id={}", id);
    }
}
