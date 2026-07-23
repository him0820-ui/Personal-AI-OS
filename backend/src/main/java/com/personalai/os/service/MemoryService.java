package com.personalai.os.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.MemoryFactMapper;
import com.personalai.os.mapper.MemoryGoalMapper;
import com.personalai.os.mapper.MemoryTimelineMapper;
import com.personalai.os.mapper.MemoryTodoMapper;
import com.personalai.os.memory.MemoryLifecycleService;
import com.personalai.os.memory.Reader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @description: 记忆服务类，提供事实、目标、时间线、待办等记忆的CRUD操作和生命周期管理
 * @author: 琦
 */
@Service
public class MemoryService {

    @Autowired
    private MemoryFactMapper factMapper;

    @Autowired
    private MemoryTimelineMapper timelineMapper;

    @Autowired
    private MemoryGoalMapper goalMapper;

    @Autowired
    private MemoryTodoMapper todoMapper;

    @Autowired
    private Reader reader;

    @Autowired
    private MemoryLifecycleService lifecycleService;

    public List<MemoryFact> getFacts(Long userId) {
        return reader.readFacts(userId);
    }

    public List<MemoryFact> getActiveFacts(Long userId) {
        return reader.readActiveFacts(userId);
    }

    public List<MemoryFact> getFactsForPrompt(Long userId) {
        return reader.readActiveFactsForPrompt(userId);
    }

    public List<MemoryGoal> getGoalsForPrompt(Long userId) {
        return reader.readActiveGoalsForPrompt(userId);
    }

    public List<MemoryTodo> getTodosForPrompt(Long userId) {
        return reader.readActiveTodosForPrompt(userId);
    }

    public List<MemoryTimeline> getTimelinesForPrompt(Long userId) {
        return reader.readRecentTimelinesForPrompt(userId);
    }

    public MemoryFact createFact(Long userId, String key, String value, Integer score) {
        MemoryFact existing = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getUserId, userId)
                .eq(MemoryFact::getKey, key));
        
        String memoryType = lifecycleService.inferTypeFromKey(key).getValue();
        
        if (existing != null) {
            existing.setValue(value);
            existing.setImportance(score != null ? score : 50);
            existing.setMemoryType(memoryType);
            existing.setUpdatedAt(LocalDateTime.now());
            factMapper.updateById(existing);
            return existing;
        }
        
        MemoryFact fact = new MemoryFact();
        fact.setUserId(userId);
        fact.setKey(key);
        fact.setValue(value);
        fact.setImportance(score != null ? score : 50);
        fact.setConfidence(0);
        fact.setAccessCount(0);
        fact.setLastAccessTime(LocalDateTime.now());
        fact.setMemoryType(memoryType);
        fact.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        fact.setCreatedAt(LocalDateTime.now());
        fact.setUpdatedAt(LocalDateTime.now());
        factMapper.insert(fact);
        return fact;
    }

    public MemoryFact updateFact(Long userId, Long id, String key, String value, Integer score) {
        MemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getId, id)
                .eq(MemoryFact::getUserId, userId));
        
        if (fact == null) {
            throw new RuntimeException("Fact not found");
        }
        
        if (key != null) {
            fact.setKey(key);
            fact.setMemoryType(lifecycleService.inferTypeFromKey(key).getValue());
        }
        if (value != null) fact.setValue(value);
        if (score != null) fact.setImportance(score);
        fact.setUpdatedAt(LocalDateTime.now());
        factMapper.updateById(fact);
        return fact;
    }

    public void deleteFact(Long userId, Long id) {
        MemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getId, id)
                .eq(MemoryFact::getUserId, userId));
        
        if (fact == null) {
            throw new RuntimeException("Fact not found");
        }
        
        factMapper.deleteById(id);
    }

    public List<MemoryTimeline> getTimeline(Long userId) {
        return reader.readTimelines(userId);
    }

    public MemoryTimeline createTimeline(Long userId, String title, String description, String timestampStr) {
        MemoryTimeline timeline = new MemoryTimeline();
        timeline.setUserId(userId);
        timeline.setTitle(title);
        timeline.setDescription(description);
        if (timestampStr != null && !timestampStr.isEmpty()) {
            try {
                timeline.setTimestamp(LocalDateTime.parse(timestampStr.replace("T", " ") + ":00"));
            } catch (Exception e) {
                timeline.setTimestamp(LocalDateTime.now());
            }
        } else {
            timeline.setTimestamp(LocalDateTime.now());
        }
        timeline.setImportance(50);
        timeline.setConfidence(0);
        timeline.setAccessCount(0);
        timeline.setLastAccessTime(LocalDateTime.now());
        timeline.setMemoryType(MemoryLifecycleService.MemoryType.TIMELINE.getValue());
        timeline.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        timeline.setCreatedAt(LocalDateTime.now());
        timeline.setUpdatedAt(LocalDateTime.now());
        timelineMapper.insert(timeline);
        return timeline;
    }

    public MemoryTimeline updateTimeline(Long userId, Long id, String title, String description, String timestampStr) {
        MemoryTimeline timeline = timelineMapper.selectOne(new LambdaQueryWrapper<MemoryTimeline>()
                .eq(MemoryTimeline::getId, id)
                .eq(MemoryTimeline::getUserId, userId));
        
        if (timeline == null) {
            throw new RuntimeException("Timeline not found");
        }
        
        if (title != null) timeline.setTitle(title);
        if (description != null) timeline.setDescription(description);
        if (timestampStr != null && !timestampStr.isEmpty()) {
            try {
                timeline.setTimestamp(LocalDateTime.parse(timestampStr.replace("T", " ") + ":00"));
            } catch (Exception e) {
                // ignore
            }
        }
        timelineMapper.updateById(timeline);
        return timeline;
    }

    public void deleteTimeline(Long userId, Long id) {
        MemoryTimeline timeline = timelineMapper.selectOne(new LambdaQueryWrapper<MemoryTimeline>()
                .eq(MemoryTimeline::getId, id)
                .eq(MemoryTimeline::getUserId, userId));
        
        if (timeline == null) {
            throw new RuntimeException("Timeline not found");
        }
        
        timelineMapper.deleteById(id);
    }

    public List<MemoryGoal> getGoals(Long userId) {
        return reader.readGoals(userId);
    }

    public MemoryGoal createGoal(Long userId, String title, String description, Integer progress, 
                                  Integer priority, java.time.LocalDate deadline, String status) {
        MemoryGoal goal = new MemoryGoal();
        goal.setUserId(userId);
        goal.setTitle(title);
        goal.setDescription(description);
        goal.setProgress(progress != null ? progress : 0);
        goal.setPriority(priority != null ? priority : 0);
        goal.setDeadline(deadline);
        goal.setImportance(50);
        goal.setConfidence(0);
        goal.setAccessCount(0);
        goal.setLastAccessTime(LocalDateTime.now());
        goal.setMemoryType(MemoryLifecycleService.MemoryType.GOAL.getValue());
        goal.setStatus(status != null ? status : MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        goal.setCreatedAt(LocalDateTime.now());
        goal.setUpdatedAt(LocalDateTime.now());
        goalMapper.insert(goal);
        return goal;
    }

    public MemoryGoal updateGoal(Long userId, Long id, String title, String description, Integer progress,
                                  Integer priority, java.time.LocalDate deadline, String status) {
        MemoryGoal goal = goalMapper.selectOne(new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getId, id)
                .eq(MemoryGoal::getUserId, userId));
        
        if (goal == null) {
            throw new RuntimeException("Goal not found");
        }
        
        if (title != null) goal.setTitle(title);
        if (description != null) goal.setDescription(description);
        if (progress != null) goal.setProgress(progress);
        if (priority != null) goal.setPriority(priority);
        if (deadline != null) goal.setDeadline(deadline);
        if (status != null) goal.setStatus(status);
        goal.setUpdatedAt(LocalDateTime.now());
        goalMapper.updateById(goal);
        return goal;
    }

    public void deleteGoal(Long userId, Long id) {
        MemoryGoal goal = goalMapper.selectOne(new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getId, id)
                .eq(MemoryGoal::getUserId, userId));
        
        if (goal == null) {
            throw new RuntimeException("Goal not found");
        }
        
        goalMapper.deleteById(id);
    }

    public List<MemoryTodo> getTodos(Long userId) {
        return reader.readTodos(userId);
    }

    public MemoryTodo createTodo(Long userId, String title, Integer priority, java.time.LocalDate dueDate) {
        MemoryTodo todo = new MemoryTodo();
        todo.setUserId(userId);
        todo.setTitle(title);
        todo.setCompleted(false);
        todo.setPriority(priority != null ? priority : 0);
        todo.setDueDate(dueDate);
        todo.setImportance(50);
        todo.setConfidence(0);
        todo.setAccessCount(0);
        todo.setLastAccessTime(LocalDateTime.now());
        todo.setMemoryType(MemoryLifecycleService.MemoryType.TODO.getValue());
        todo.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.insert(todo);
        return todo;
    }

    public MemoryTodo updateTodo(Long userId, Long id, String title, Boolean completed, 
                                  Integer priority, java.time.LocalDate dueDate) {
        MemoryTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getId, id)
                .eq(MemoryTodo::getUserId, userId));
        
        if (todo == null) {
            throw new RuntimeException("Todo not found");
        }
        
        if (title != null) todo.setTitle(title);
        if (completed != null) todo.setCompleted(completed);
        if (priority != null) todo.setPriority(priority);
        if (dueDate != null) todo.setDueDate(dueDate);
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.updateById(todo);
        return todo;
    }

    public void deleteTodo(Long userId, Long id) {
        MemoryTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getId, id)
                .eq(MemoryTodo::getUserId, userId));
        
        if (todo == null) {
            throw new RuntimeException("Todo not found");
        }
        
        todoMapper.deleteById(id);
    }

    public MemoryFact archiveFact(Long userId, Long id) {
        MemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getId, id)
                .eq(MemoryFact::getUserId, userId));
        
        if (fact == null) {
            throw new RuntimeException("Fact not found");
        }
        
        fact.setStatus(MemoryLifecycleService.MemoryStatus.ARCHIVED.name());
        fact.setUpdatedAt(LocalDateTime.now());
        factMapper.updateById(fact);
        return fact;
    }

    public MemoryFact restoreFact(Long userId, Long id) {
        MemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getId, id)
                .eq(MemoryFact::getUserId, userId));
        
        if (fact == null) {
            throw new RuntimeException("Fact not found");
        }
        
        fact.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        fact.setUpdatedAt(LocalDateTime.now());
        factMapper.updateById(fact);
        return fact;
    }

    public MemoryFact softDeleteFact(Long userId, Long id) {
        MemoryFact fact = factMapper.selectOne(new LambdaQueryWrapper<MemoryFact>()
                .eq(MemoryFact::getId, id)
                .eq(MemoryFact::getUserId, userId));
        
        if (fact == null) {
            throw new RuntimeException("Fact not found");
        }
        
        fact.setStatus(MemoryLifecycleService.MemoryStatus.DELETED.name());
        fact.setUpdatedAt(LocalDateTime.now());
        factMapper.updateById(fact);
        return fact;
    }

    public MemoryGoal archiveGoal(Long userId, Long id) {
        MemoryGoal goal = goalMapper.selectOne(new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getId, id)
                .eq(MemoryGoal::getUserId, userId));
        
        if (goal == null) {
            throw new RuntimeException("Goal not found");
        }
        
        goal.setStatus(MemoryLifecycleService.MemoryStatus.ARCHIVED.name());
        goal.setUpdatedAt(LocalDateTime.now());
        goalMapper.updateById(goal);
        return goal;
    }

    public MemoryGoal restoreGoal(Long userId, Long id) {
        MemoryGoal goal = goalMapper.selectOne(new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getId, id)
                .eq(MemoryGoal::getUserId, userId));
        
        if (goal == null) {
            throw new RuntimeException("Goal not found");
        }
        
        goal.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        goal.setUpdatedAt(LocalDateTime.now());
        goalMapper.updateById(goal);
        return goal;
    }

    public MemoryGoal softDeleteGoal(Long userId, Long id) {
        MemoryGoal goal = goalMapper.selectOne(new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getId, id)
                .eq(MemoryGoal::getUserId, userId));
        
        if (goal == null) {
            throw new RuntimeException("Goal not found");
        }
        
        goal.setStatus(MemoryLifecycleService.MemoryStatus.DELETED.name());
        goal.setUpdatedAt(LocalDateTime.now());
        goalMapper.updateById(goal);
        return goal;
    }

    public MemoryTimeline archiveTimeline(Long userId, Long id) {
        MemoryTimeline timeline = timelineMapper.selectOne(new LambdaQueryWrapper<MemoryTimeline>()
                .eq(MemoryTimeline::getId, id)
                .eq(MemoryTimeline::getUserId, userId));
        
        if (timeline == null) {
            throw new RuntimeException("Timeline not found");
        }
        
        timeline.setStatus(MemoryLifecycleService.MemoryStatus.ARCHIVED.name());
        timeline.setUpdatedAt(LocalDateTime.now());
        timelineMapper.updateById(timeline);
        return timeline;
    }

    public MemoryTimeline restoreTimeline(Long userId, Long id) {
        MemoryTimeline timeline = timelineMapper.selectOne(new LambdaQueryWrapper<MemoryTimeline>()
                .eq(MemoryTimeline::getId, id)
                .eq(MemoryTimeline::getUserId, userId));
        
        if (timeline == null) {
            throw new RuntimeException("Timeline not found");
        }
        
        timeline.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        timeline.setUpdatedAt(LocalDateTime.now());
        timelineMapper.updateById(timeline);
        return timeline;
    }

    public MemoryTimeline softDeleteTimeline(Long userId, Long id) {
        MemoryTimeline timeline = timelineMapper.selectOne(new LambdaQueryWrapper<MemoryTimeline>()
                .eq(MemoryTimeline::getId, id)
                .eq(MemoryTimeline::getUserId, userId));
        
        if (timeline == null) {
            throw new RuntimeException("Timeline not found");
        }
        
        timeline.setStatus(MemoryLifecycleService.MemoryStatus.DELETED.name());
        timeline.setUpdatedAt(LocalDateTime.now());
        timelineMapper.updateById(timeline);
        return timeline;
    }

    public MemoryTodo archiveTodo(Long userId, Long id) {
        MemoryTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getId, id)
                .eq(MemoryTodo::getUserId, userId));
        
        if (todo == null) {
            throw new RuntimeException("Todo not found");
        }
        
        todo.setStatus(MemoryLifecycleService.MemoryStatus.ARCHIVED.name());
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.updateById(todo);
        return todo;
    }

    public MemoryTodo restoreTodo(Long userId, Long id) {
        MemoryTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getId, id)
                .eq(MemoryTodo::getUserId, userId));
        
        if (todo == null) {
            throw new RuntimeException("Todo not found");
        }
        
        todo.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.updateById(todo);
        return todo;
    }

    public MemoryTodo softDeleteTodo(Long userId, Long id) {
        MemoryTodo todo = todoMapper.selectOne(new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getId, id)
                .eq(MemoryTodo::getUserId, userId));
        
        if (todo == null) {
            throw new RuntimeException("Todo not found");
        }
        
        todo.setStatus(MemoryLifecycleService.MemoryStatus.DELETED.name());
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.updateById(todo);
        return todo;
    }

    public List<MemoryFact> getFactsByStatus(Long userId, String status) {
        return reader.readFacts(userId, status);
    }

    public List<MemoryGoal> getGoalsByStatus(Long userId, String status) {
        return reader.readGoals(userId, status);
    }

    public List<MemoryTimeline> getTimelinesByStatus(Long userId, String status) {
        return reader.readTimelines(userId, status);
    }

    public List<MemoryTodo> getTodosByStatus(Long userId, String status) {
        return reader.readTodos(userId, status);
    }
}
