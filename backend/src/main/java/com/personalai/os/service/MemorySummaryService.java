package com.personalai.os.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemorySummary;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.MemoryFactMapper;
import com.personalai.os.mapper.MemoryGoalMapper;
import com.personalai.os.mapper.MemorySummaryMapper;
import com.personalai.os.mapper.MemoryTimelineMapper;
import com.personalai.os.mapper.MemoryTodoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 记忆总结服务类，生成用户记忆摘要和标签
 * @author: 琦
 */
@Service
public class MemorySummaryService {

    private static final Logger logger = LoggerFactory.getLogger(MemorySummaryService.class);

    @Autowired
    private MemorySummaryMapper summaryMapper;

    @Autowired
    private MemoryFactMapper factMapper;

    @Autowired
    private MemoryGoalMapper goalMapper;

    @Autowired
    private MemoryTodoMapper todoMapper;

    @Autowired
    private MemoryTimelineMapper timelineMapper;

    @Autowired
    private AiService aiService;

    public MemorySummary getSummary(Long userId) {
        return summaryMapper.selectOne(new LambdaQueryWrapper<MemorySummary>()
                .eq(MemorySummary::getUserId, userId));
    }

    public MemorySummary updateSummary(Long userId) {
        logger.info("Updating summary for user: {}", userId);
        
        List<MemoryFact> facts = factMapper.selectList(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getUserId, userId)
                .eq(MemoryFact::getStatus, "ACTIVE"));
        
        List<MemoryGoal> goals = goalMapper.selectList(new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getUserId, userId)
                .eq(MemoryGoal::getStatus, "ACTIVE"));
        
        List<MemoryTodo> todos = todoMapper.selectList(new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getUserId, userId)
                .eq(MemoryTodo::getStatus, "ACTIVE"));
        
        List<MemoryTimeline> timelines = timelineMapper.selectList(new LambdaQueryWrapper<MemoryTimeline>()
                .eq(MemoryTimeline::getUserId, userId)
                .eq(MemoryTimeline::getStatus, "ACTIVE"));

        String summary = generateSummary(facts, goals, todos, timelines);
        String tags = extractTags(facts, goals);

        MemorySummary existing = summaryMapper.selectOne(new LambdaQueryWrapper<MemorySummary>()
                .eq(MemorySummary::getUserId, userId));

        if (existing != null) {
            existing.setSummary(summary);
            existing.setFactCount(facts.size());
            existing.setGoalCount(goals.size());
            existing.setTodoCount(todos.size());
            existing.setTimelineCount(timelines.size());
            existing.setTags(tags);
            existing.setUpdatedAt(LocalDateTime.now());
            summaryMapper.updateById(existing);
            logger.info("Updated summary for user: {}, summary length: {}", userId, summary.length());
            return existing;
        }

        MemorySummary newSummary = new MemorySummary();
        newSummary.setUserId(userId);
        newSummary.setSummary(summary);
        newSummary.setFactCount(facts.size());
        newSummary.setGoalCount(goals.size());
        newSummary.setTodoCount(todos.size());
        newSummary.setTimelineCount(timelines.size());
        newSummary.setTags(tags);
        newSummary.setCreatedAt(LocalDateTime.now());
        newSummary.setUpdatedAt(LocalDateTime.now());
        summaryMapper.insert(newSummary);
        logger.info("Created summary for user: {}, summary length: {}", userId, summary.length());
        return newSummary;
    }

    @Async("taskExecutor")
    public void updateSummaryAsync(Long userId) {
        try {
            updateSummary(userId);
        } catch (Exception e) {
            logger.error("Failed to update summary asynchronously for user: {}", userId, e);
        }
    }

    private String generateSummary(List<MemoryFact> facts, List<MemoryGoal> goals, 
                                   List<MemoryTodo> todos, List<MemoryTimeline> timelines) {
        StringBuilder memoryContent = new StringBuilder();

        memoryContent.append("【个人事实】\n");
        for (MemoryFact fact : facts) {
            memoryContent.append("- ").append(fact.getKey()).append(": ").append(fact.getValue()).append("\n");
        }

        memoryContent.append("\n【当前目标】\n");
        for (MemoryGoal goal : goals) {
            memoryContent.append("- ").append(goal.getTitle());
            if (goal.getProgress() != null) {
                memoryContent.append(" (进度: ").append(goal.getProgress()).append("%)");
            }
            memoryContent.append("\n");
        }

        memoryContent.append("\n【待办事项】\n");
        for (MemoryTodo todo : todos.stream().filter(t -> !t.getCompleted()).limit(10).collect(Collectors.toList())) {
            memoryContent.append("- ").append(todo.getTitle()).append("\n");
        }

        memoryContent.append("\n【重要时间线】\n");
        for (MemoryTimeline timeline : timelines.stream().limit(10).collect(Collectors.toList())) {
            memoryContent.append("- ").append(timeline.getTimestamp()).append(": ").append(timeline.getTitle()).append("\n");
        }

        String prompt = """
            请根据以下用户的长期记忆，生成一份简洁的用户画像摘要（User Profile Summary）。
            
            要求：
            1. 用自然语言描述用户的身份、背景、兴趣爱好、技能特点
            2. 突出用户的主要目标和当前状态
            3. 语言简洁，不超过300字
            4. 只返回摘要内容，不要包含其他说明
            
            用户记忆内容：
            %s
            """.formatted(memoryContent.toString());

        try {
            String summary = aiService.generateResponse(prompt);
            return summary != null ? summary.trim() : "暂无足够信息生成摘要";
        } catch (Exception e) {
            logger.error("Failed to generate summary using AI", e);
            return generateFallbackSummary(facts, goals);
        }
    }

    private String generateFallbackSummary(List<MemoryFact> facts, List<MemoryGoal> goals) {
        StringBuilder summary = new StringBuilder();
        
        String name = facts.stream()
                .filter(f -> "姓名".equals(f.getKey()) || "name".equalsIgnoreCase(f.getKey()))
                .map(MemoryFact::getValue)
                .findFirst()
                .orElse("用户");
        
        summary.append(name);

        String age = facts.stream()
                .filter(f -> "年龄".equals(f.getKey()) || "age".equalsIgnoreCase(f.getKey()))
                .map(MemoryFact::getValue)
                .findFirst()
                .orElse(null);
        
        if (age != null) {
            summary.append("，").append(age);
        }

        if (!goals.isEmpty()) {
            summary.append("。当前目标：");
            summary.append(goals.stream()
                    .map(MemoryGoal::getTitle)
                    .limit(3)
                    .collect(Collectors.joining("、")));
        }

        return summary.toString();
    }

    private String extractTags(List<MemoryFact> facts, List<MemoryGoal> goals) {
        StringBuilder tags = new StringBuilder();
        
        facts.stream()
                .filter(f -> f.getMemoryType() != null && !f.getMemoryType().isEmpty())
                .map(MemoryFact::getMemoryType)
                .distinct()
                .forEach(t -> {
                    if (tags.length() > 0) tags.append(",");
                    tags.append(t);
                });
        
        goals.stream()
                .filter(g -> g.getTitle() != null)
                .map(g -> {
                    if (g.getTitle().contains("学习") || g.getTitle().contains("开发")) return "学习";
                    if (g.getTitle().contains("面试") || g.getTitle().contains("工作")) return "职业";
                    return null;
                })
                .filter(t -> t != null)
                .distinct()
                .forEach(t -> {
                    if (tags.length() > 0) tags.append(",");
                    tags.append(t);
                });
        
        return tags.toString();
    }
}
