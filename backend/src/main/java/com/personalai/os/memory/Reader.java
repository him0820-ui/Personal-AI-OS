package com.personalai.os.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.MemoryFactMapper;
import com.personalai.os.mapper.MemoryGoalMapper;
import com.personalai.os.mapper.MemoryTimelineMapper;
import com.personalai.os.mapper.MemoryTodoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 记忆读取器，负责从数据库读取各类记忆数据
 * @author: 琦
 */
@Component
public class Reader {

    private static final Logger logger = LoggerFactory.getLogger(Reader.class);
    private static final int MAX_PROMPT_MEMORIES = 20;

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

    public List<MemoryFact> readFacts(Long userId) {
        return readFacts(userId, null);
    }

    public List<MemoryFact> readFacts(Long userId, String status) {
        logger.info("Reading facts for user {}, status={}", userId, status);
        
        LambdaQueryWrapper<MemoryFact> wrapper = new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getUserId, userId);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MemoryFact::getStatus, status);
        } else {
            wrapper.ne(MemoryFact::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name());
        }
        
        List<MemoryFact> facts = factMapper.selectList(wrapper);
        
        for (MemoryFact fact : facts) {
            reinforceMemory(fact);
        }
        
        return facts.stream()
                .sorted((f1, f2) -> Double.compare(lifecycleService.calculateRank(f2), lifecycleService.calculateRank(f1)))
                .collect(Collectors.toList());
    }

    public List<MemoryTimeline> readTimelines(Long userId) {
        return readTimelines(userId, null);
    }

    public List<MemoryTimeline> readTimelines(Long userId, String status) {
        logger.info("Reading timelines for user {}, status={}", userId, status);
        
        LambdaQueryWrapper<MemoryTimeline> wrapper = new LambdaQueryWrapper<MemoryTimeline>()
                .eq(MemoryTimeline::getUserId, userId);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MemoryTimeline::getStatus, status);
        } else {
            wrapper.ne(MemoryTimeline::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name());
        }
        
        List<MemoryTimeline> timelines = timelineMapper.selectList(wrapper);
        
        for (MemoryTimeline timeline : timelines) {
            reinforceMemory(timeline);
        }
        
        return timelines.stream()
                .sorted((t1, t2) -> Double.compare(lifecycleService.calculateRank(t2), lifecycleService.calculateRank(t1)))
                .collect(Collectors.toList());
    }

    public List<MemoryGoal> readGoals(Long userId) {
        return readGoals(userId, null);
    }

    public List<MemoryGoal> readGoals(Long userId, String status) {
        logger.info("Reading goals for user {}, status={}", userId, status);
        
        LambdaQueryWrapper<MemoryGoal> wrapper = new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getUserId, userId);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MemoryGoal::getStatus, status);
        } else {
            wrapper.ne(MemoryGoal::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name());
        }
        
        List<MemoryGoal> goals = goalMapper.selectList(wrapper);
        
        for (MemoryGoal goal : goals) {
            reinforceMemory(goal);
        }
        
        return goals.stream()
                .sorted((g1, g2) -> Double.compare(lifecycleService.calculateRank(g2), lifecycleService.calculateRank(g1)))
                .collect(Collectors.toList());
    }

    public List<MemoryTodo> readTodos(Long userId) {
        return readTodos(userId, null);
    }

    public List<MemoryTodo> readTodos(Long userId, String status) {
        logger.info("Reading todos for user {}, status={}", userId, status);
        
        LambdaQueryWrapper<MemoryTodo> wrapper = new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getUserId, userId);
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MemoryTodo::getStatus, status);
        } else {
            wrapper.ne(MemoryTodo::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name());
        }
        
        List<MemoryTodo> todos = todoMapper.selectList(wrapper);
        
        for (MemoryTodo todo : todos) {
            reinforceMemory(todo);
        }
        
        return todos.stream()
                .sorted((t1, t2) -> Double.compare(lifecycleService.calculateRank(t2), lifecycleService.calculateRank(t1)))
                .collect(Collectors.toList());
    }

    public MemoryFact readFactById(Long userId, Long id) {
        MemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getId, id)
                .eq(MemoryFact::getUserId, userId));
        
        if (fact != null) {
            reinforceMemory(fact);
        }
        
        return fact;
    }

    public MemoryFact readFactByKey(Long userId, String key) {
        MemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getKey, key)
                .eq(MemoryFact::getUserId, userId));
        
        if (fact != null) {
            reinforceMemory(fact);
        }
        
        return fact;
    }

    public List<MemoryFact> searchFacts(Long userId, String query) {
        LambdaQueryWrapper<MemoryFact> wrapper = new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getUserId, userId)
                .ne(MemoryFact::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name())
                .and(w -> w.like(MemoryFact::getKey, query).or().like(MemoryFact::getValue, query));
        
        List<MemoryFact> facts = factMapper.selectList(wrapper);
        
        for (MemoryFact fact : facts) {
            reinforceMemory(fact);
        }
        
        return facts.stream()
                .sorted((f1, f2) -> Double.compare(lifecycleService.calculateRank(f2), lifecycleService.calculateRank(f1)))
                .collect(Collectors.toList());
    }

    private void reinforceMemory(MemoryFact fact) {
        if (fact == null) return;
        
        int newAccessCount = (fact.getAccessCount() != null ? fact.getAccessCount() : 0) + 1;
        fact.setAccessCount(newAccessCount);
        fact.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(fact.getStatus())) {
            fact.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        factMapper.updateById(fact);
        logger.debug("Reinforced fact: id={}, key={}, accessCount={}", 
                fact.getId(), fact.getKey(), newAccessCount);
    }

    private void reinforceMemory(MemoryTimeline timeline) {
        if (timeline == null) return;
        
        int newAccessCount = (timeline.getAccessCount() != null ? timeline.getAccessCount() : 0) + 1;
        timeline.setAccessCount(newAccessCount);
        timeline.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(timeline.getStatus())) {
            timeline.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        timelineMapper.updateById(timeline);
        logger.debug("Reinforced timeline: id={}, title={}, accessCount={}", 
                timeline.getId(), timeline.getTitle(), newAccessCount);
    }

    private void reinforceMemory(MemoryGoal goal) {
        if (goal == null) return;
        
        int newAccessCount = (goal.getAccessCount() != null ? goal.getAccessCount() : 0) + 1;
        goal.setAccessCount(newAccessCount);
        goal.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(goal.getStatus())) {
            goal.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        goalMapper.updateById(goal);
        logger.debug("Reinforced goal: id={}, title={}, accessCount={}", 
                goal.getId(), goal.getTitle(), newAccessCount);
    }

    private void reinforceMemory(MemoryTodo todo) {
        if (todo == null) return;
        
        int newAccessCount = (todo.getAccessCount() != null ? todo.getAccessCount() : 0) + 1;
        todo.setAccessCount(newAccessCount);
        todo.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(todo.getStatus())) {
            todo.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        todoMapper.updateById(todo);
        logger.debug("Reinforced todo: id={}, title={}, accessCount={}", 
                todo.getId(), todo.getTitle(), newAccessCount);
    }

    public List<MemoryFact> readActiveFacts(Long userId) {
        return readFacts(userId, MemoryLifecycleService.MemoryStatus.ACTIVE.name());
    }

    public List<MemoryFact> readDormantFacts(Long userId) {
        return readFacts(userId, MemoryLifecycleService.MemoryStatus.DORMANT.name());
    }

    public List<MemoryFact> readArchivedFacts(Long userId) {
        return readFacts(userId, MemoryLifecycleService.MemoryStatus.ARCHIVED.name());
    }

    public List<MemoryFact> readActiveFactsForPrompt(Long userId) {
        return readActiveFacts(userId).stream()
                .limit(MAX_PROMPT_MEMORIES)
                .collect(Collectors.toList());
    }

    public List<MemoryGoal> readActiveGoalsForPrompt(Long userId) {
        return readGoals(userId, MemoryLifecycleService.MemoryStatus.ACTIVE.name()).stream()
                .limit(MAX_PROMPT_MEMORIES)
                .collect(Collectors.toList());
    }

    public List<MemoryTodo> readActiveTodosForPrompt(Long userId) {
        return readTodos(userId, MemoryLifecycleService.MemoryStatus.ACTIVE.name()).stream()
                .filter(t -> !t.getCompleted())
                .limit(MAX_PROMPT_MEMORIES)
                .collect(Collectors.toList());
    }

    public List<MemoryTimeline> readRecentTimelinesForPrompt(Long userId) {
        return readTimelines(userId, MemoryLifecycleService.MemoryStatus.ACTIVE.name()).stream()
                .limit(MAX_PROMPT_MEMORIES)
                .collect(Collectors.toList());
    }
}