package com.personalai.os.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.MemoryFactMapper;
import com.personalai.os.mapper.MemoryGoalMapper;
import com.personalai.os.mapper.MemoryTodoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 进度更新服务类，根据对话内容自动更新目标进度
 * @author: 琦
 */
@Service
public class ProgressUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(ProgressUpdateService.class);

    @Autowired
    private MemoryGoalMapper goalMapper;

    @Autowired
    private MemoryTodoMapper todoMapper;

    @Autowired
    private MemoryFactMapper factMapper;

    @Autowired
    private MemoryLifecycleService lifecycleService;

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(完成|做完|搞定|搞定了|做好|完成了|结束|结束了|搞定啦)([的了]*)\\s*(.*)");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+)[%％]");

    public void analyzeConversationAndUpdate(Long userId, String conversationText) {
        logger.info("Analyzing conversation for progress updates: {}", conversationText);

        String lowerText = conversationText.toLowerCase();

        updateGoalsByCompletion(userId, lowerText);
        updateTodosByCompletion(userId, lowerText);
        updateProgressByPercent(userId, lowerText);
    }

    private void updateGoalsByCompletion(Long userId, String text) {
        LambdaQueryWrapper<MemoryGoal> wrapper = new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getUserId, userId)
                .eq(MemoryGoal::getStatus, MemoryLifecycleService.MemoryStatus.ACTIVE.name())
                .lt(MemoryGoal::getProgress, 100);
        
        List<MemoryGoal> goals = goalMapper.selectList(wrapper);

        for (MemoryGoal goal : goals) {
            if (containsCompletion(text, goal.getTitle()) || 
                (goal.getDescription() != null && containsCompletion(text, goal.getDescription()))) {
                
                int newProgress = Math.min(100, (goal.getProgress() != null ? goal.getProgress() : 0) + 25);
                goal.setProgress(newProgress);
                goal.setUpdatedAt(LocalDateTime.now());
                goal.setLastAccessTime(LocalDateTime.now());
                goalMapper.updateById(goal);
                
                logger.info("Goal progress updated: id={}, title={}, progress={}%", 
                        goal.getId(), goal.getTitle(), newProgress);
                
                if (newProgress >= 100) {
                    reinforceRelatedMemories(userId, goal.getTitle());
                }
            }
        }
    }

    private void updateTodosByCompletion(Long userId, String text) {
        LambdaQueryWrapper<MemoryTodo> wrapper = new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getUserId, userId)
                .eq(MemoryTodo::getStatus, MemoryLifecycleService.MemoryStatus.ACTIVE.name())
                .eq(MemoryTodo::getCompleted, false);
        
        List<MemoryTodo> todos = todoMapper.selectList(wrapper);

        for (MemoryTodo todo : todos) {
            if (containsCompletion(text, todo.getTitle())) {
                todo.setCompleted(true);
                todo.setUpdatedAt(LocalDateTime.now());
                todo.setLastAccessTime(LocalDateTime.now());
                todoMapper.updateById(todo);
                
                logger.info("Todo marked as completed: id={}, title={}", todo.getId(), todo.getTitle());
                
                reinforceRelatedMemories(userId, todo.getTitle());
            }
        }
    }

    private void updateProgressByPercent(Long userId, String text) {
        Matcher percentMatcher = PERCENT_PATTERN.matcher(text);
        if (percentMatcher.find()) {
            int percent = Integer.parseInt(percentMatcher.group(1));
            
            LambdaQueryWrapper<MemoryGoal> wrapper = new LambdaQueryWrapper<MemoryGoal>()
                    .eq(MemoryGoal::getUserId, userId)
                    .eq(MemoryGoal::getStatus, MemoryLifecycleService.MemoryStatus.ACTIVE.name());
            
            List<MemoryGoal> goals = goalMapper.selectList(wrapper);

            for (MemoryGoal goal : goals) {
                if (text.contains(goal.getTitle().toLowerCase())) {
                    goal.setProgress(Math.min(100, percent));
                    goal.setUpdatedAt(LocalDateTime.now());
                    goal.setLastAccessTime(LocalDateTime.now());
                    goalMapper.updateById(goal);
                    
                    logger.info("Goal progress updated by percent: id={}, title={}, progress={}%", 
                            goal.getId(), goal.getTitle(), percent);
                }
            }
        }
    }

    private boolean containsCompletion(String text, String target) {
        if (target == null || target.isEmpty()) {
            return false;
        }
        
        String lowerTarget = target.toLowerCase();
        
        Matcher matcher = PROGRESS_PATTERN.matcher(text);
        if (matcher.find()) {
            String completedItem = matcher.group(3).trim();
            if (completedItem.isEmpty()) {
                return true;
            }
            
            return lowerTarget.contains(completedItem) || completedItem.contains(lowerTarget);
        }
        
        return text.contains("完成") && text.contains(lowerTarget);
    }

    private void reinforceRelatedMemories(Long userId, String completedText) {
        LambdaQueryWrapper<MemoryFact> wrapper = new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getUserId, userId)
                .ne(MemoryFact::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name());
        
        List<MemoryFact> facts = factMapper.selectList(wrapper);

        for (MemoryFact fact : facts) {
            String factText = (fact.getKey() != null ? fact.getKey() : "") + 
                             (fact.getValue() != null ? fact.getValue() : "");
            
            if (completedText.toLowerCase().contains(factText.toLowerCase()) ||
                factText.toLowerCase().contains(completedText.toLowerCase())) {
                
                int newImportance = Math.min(100, (fact.getImportance() != null ? fact.getImportance() : 50) + 5);
                int newConfidence = Math.min(100, (fact.getConfidence() != null ? fact.getConfidence() : 0) + 2);
                
                fact.setImportance(newImportance);
                fact.setConfidence(newConfidence);
                fact.setAccessCount((fact.getAccessCount() != null ? fact.getAccessCount() : 0) + 1);
                fact.setLastAccessTime(LocalDateTime.now());
                fact.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
                factMapper.updateById(fact);
                
                logger.info("Fact reinforced due to completion: id={}, key={}, importance={}", 
                        fact.getId(), fact.getKey(), newImportance);
            }
        }
    }
}