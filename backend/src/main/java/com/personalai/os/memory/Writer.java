package com.personalai.os.memory;

import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.MemoryFactMapper;
import com.personalai.os.mapper.MemoryGoalMapper;
import com.personalai.os.mapper.MemoryTimelineMapper;
import com.personalai.os.mapper.MemoryTodoMapper;
import com.personalai.os.memory.dto.Attribute;
import com.personalai.os.memory.dto.ExtractResult;
import com.personalai.os.memory.dto.ExtractedInfo;
import com.personalai.os.memory.dto.Fact;
import com.personalai.os.memory.dto.Goal;
import com.personalai.os.memory.dto.Timeline;
import com.personalai.os.memory.dto.Todo;
import com.personalai.os.service.ConflictDetectionService;
import com.personalai.os.service.MemoryAttributeService;
import com.personalai.os.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description: 记忆写入器，负责将提取的记忆写入数据库
 * @author: 琦
 */
@Component
public class Writer {

    private static final Logger logger = LoggerFactory.getLogger(Writer.class);

    @Autowired
    private MemoryFactMapper factMapper;

    @Autowired
    private MemoryTimelineMapper timelineMapper;

    @Autowired
    private MemoryGoalMapper goalMapper;

    @Autowired
    private MemoryTodoMapper todoMapper;

    @Autowired
    private MemoryLifecycleService lifecycleService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    @Autowired
    private MemoryAttributeService memoryAttributeService;

    public void writeDirect(Long userId, ExtractResult result, String conversationText) {
        if (result == null) {
            return;
        }

        if (result.getAttributes() != null) {
            for (Attribute attr : result.getAttributes()) {
                writeAttribute(userId, attr, conversationText);
            }
        }

        if (result.getFacts() != null) {
            for (Fact fact : result.getFacts()) {
                writeFactAsAttribute(userId, fact);
            }
        }
        if (result.getTimelines() != null) {
            for (Timeline timeline : result.getTimelines()) {
                writeTimelineAsAttribute(userId, timeline);
            }
        }
        if (result.getGoals() != null) {
            for (Goal goal : result.getGoals()) {
                writeGoalAsAttribute(userId, goal);
            }
        }
        if (result.getTodos() != null) {
            for (Todo todo : result.getTodos()) {
                writeTodoAsAttribute(userId, todo);
            }
        }
    }

    private void writeAttribute(Long userId, Attribute attr, String conversationText) {
        String category = attr.getCategory();
        String entity = attr.getEntity();
        String attribute = attr.getAttribute();
        String value = attr.getValue();
        Integer importance = attr.getImportance();
        Integer confidence = attr.getConfidence();
        String sourceQuote = attr.getSourceQuote();

        if (sourceQuote != null && conversationText != null && !conversationText.contains(sourceQuote.trim())) {
            logger.warn("Source quote validation failed, confidence set to 0");
            confidence = 0;
        }

        memoryAttributeService.writeAttribute(userId, category, entity, attribute, value, 
            importance, confidence, sourceQuote);
    }

    private void writeFactAsAttribute(Long userId, Fact fact) {
        String category = "Fact";
        String entity = fact.getKey() != null ? fact.getKey() : "其他";
        String attribute = "Value";
        String value = fact.getValue() != null ? fact.getValue() : "";

        memoryAttributeService.writeAttribute(userId, category, entity, attribute, value, 
            fact.getImportance(), fact.getConfidence(), fact.getSourceQuote());
    }

    private void writeTimelineAsAttribute(Long userId, Timeline timeline) {
        memoryAttributeService.writeAttribute(userId, "Timeline", timeline.getTitle(), "Description", 
            timeline.getDescription(), timeline.getImportance(), 
            timeline.getConfidence(), timeline.getSourceQuote());
    }

    private void writeGoalAsAttribute(Long userId, Goal goal) {
        memoryAttributeService.writeAttribute(userId, "Goal", goal.getTitle(), "Description", 
            goal.getDescription(), goal.getImportance(), 
            goal.getConfidence(), goal.getSourceQuote());
    }

    private void writeTodoAsAttribute(Long userId, Todo todo) {
        memoryAttributeService.writeAttribute(userId, "Todo", todo.getTitle(), "Status", 
            "未完成", todo.getImportance(), 
            todo.getConfidence(), todo.getSourceQuote());
    }

    @Deprecated
    public void write(Long userId, List<ExtractedInfo> infos) {
        for (ExtractedInfo info : infos) {
            switch (info.getCategory()) {
                case "Fact":
                    writeFact(userId, info);
                    break;
                case "Timeline":
                    writeTimeline(userId, info);
                    break;
                case "Goal":
                    writeGoal(userId, info);
                    break;
                case "Todo":
                    writeTodo(userId, info);
                    break;
                default:
                    memoryAttributeService.writeAttribute(userId, info.getCategory(), 
                        info.getKey(), "Value", info.getContent(), info.getScore(), 50, null);
                    break;
            }
        }
    }

    @Deprecated
    private void writeFact(Long userId, ExtractedInfo info) {
        MemoryFact existing = factMapper.selectById(1L);
        if (existing != null) {
            existing.setValue(info.getContent());
            existing.setImportance(info.getScore());
            existing.setUpdatedAt(LocalDateTime.now());
            factMapper.updateById(existing);
        } else {
            MemoryFact fact = new MemoryFact();
            fact.setUserId(userId);
            fact.setKey(info.getKey());
            fact.setValue(info.getContent());
            fact.setImportance(info.getScore());
            fact.setAccessCount(0);
            fact.setLastAccessTime(LocalDateTime.now());
            fact.setMemoryType(lifecycleService.inferTypeFromKey(info.getKey()).getValue());
            fact.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
            fact.setCreatedAt(LocalDateTime.now());
            fact.setUpdatedAt(LocalDateTime.now());
            factMapper.insert(fact);
        }
    }

    @Deprecated
    private void writeTimeline(Long userId, ExtractedInfo info) {
        MemoryTimeline timeline = new MemoryTimeline();
        timeline.setUserId(userId);
        timeline.setTitle(info.getContent());
        timeline.setDescription(info.getKey());
        timeline.setTimestamp(LocalDateTime.now());
        timeline.setAccessCount(0);
        timeline.setLastAccessTime(LocalDateTime.now());
        timeline.setMemoryType(MemoryLifecycleService.MemoryType.TIMELINE.getValue());
        timeline.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        timeline.setCreatedAt(LocalDateTime.now());
        timeline.setUpdatedAt(LocalDateTime.now());
        timelineMapper.insert(timeline);
    }

    @Deprecated
    private void writeGoal(Long userId, ExtractedInfo info) {
        String titleSub = info.getContent().substring(0, Math.min(20, info.getContent().length()));
        MemoryGoal existing = goalMapper.selectById(1L);
        
        if (existing == null) {
            MemoryGoal goal = new MemoryGoal();
            goal.setUserId(userId);
            goal.setTitle(info.getContent());
            goal.setDescription(info.getContent());
            goal.setProgress(0);
            goal.setAccessCount(0);
            goal.setLastAccessTime(LocalDateTime.now());
            goal.setMemoryType(MemoryLifecycleService.MemoryType.GOAL.getValue());
            goal.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
            goal.setCreatedAt(LocalDateTime.now());
            goal.setUpdatedAt(LocalDateTime.now());
            goalMapper.insert(goal);
        }
    }

    @Deprecated
    private void writeTodo(Long userId, ExtractedInfo info) {
        MemoryTodo todo = new MemoryTodo();
        todo.setUserId(userId);
        todo.setTitle(info.getContent());
        todo.setCompleted(false);
        todo.setPriority(info.getScore() / 20);
        todo.setImportance(info.getScore());
        todo.setAccessCount(0);
        todo.setLastAccessTime(LocalDateTime.now());
        todo.setMemoryType(MemoryLifecycleService.MemoryType.TODO.getValue());
        todo.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.insert(todo);
    }
}
