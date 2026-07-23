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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * @description: 记忆衰减定时任务，定期根据时间衰减记忆的重要性评分
 * @author: 琦
 */
@Component
public class DecayJob {

    private static final Logger logger = LoggerFactory.getLogger(DecayJob.class);

    private static final int DORMANT_DAYS_THRESHOLD = 30;
    private static final int ARCHIVED_DAYS_THRESHOLD = 90;
    private static final int DELETED_DAYS_THRESHOLD = 365;

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

    @Scheduled(cron = "0 0 2 * * ?")
    public void runDecay() {
        logger.info("Starting memory decay job at {}", LocalDateTime.now());

        int factChanges = processFacts();
        int timelineChanges = processTimelines();
        int goalChanges = processGoals();
        int todoChanges = processTodos();

        logger.info("Memory decay job completed. Fact changes: {}, Timeline changes: {}, Goal changes: {}, Todo changes: {}",
                factChanges, timelineChanges, goalChanges, todoChanges);
    }

    private int processFacts() {
        int changes = 0;
        
        List<MemoryFact> facts = factMapper.selectList(new LambdaQueryWrapper<MemoryFact>()
                .ne(MemoryFact::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        for (MemoryFact fact : facts) {
            if (skipDecay(fact.getMemoryType())) {
                continue;
            }

            String newStatus = determineNewStatus(fact);
            
            if (!newStatus.equals(fact.getStatus())) {
                fact.setStatus(newStatus);
                fact.setUpdatedAt(LocalDateTime.now());
                factMapper.updateById(fact);
                changes++;
                
                logger.info("Fact status changed: id={}, key={}, oldStatus={}, newStatus={}",
                        fact.getId(), fact.getKey(), fact.getStatus(), newStatus);
            }
        }

        return changes;
    }

    private int processTimelines() {
        int changes = 0;
        
        List<MemoryTimeline> timelines = timelineMapper.selectList(new LambdaQueryWrapper<MemoryTimeline>()
                .ne(MemoryTimeline::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        for (MemoryTimeline timeline : timelines) {
            String newStatus = determineNewStatus(timeline);
            
            if (!newStatus.equals(timeline.getStatus())) {
                timeline.setStatus(newStatus);
                timeline.setUpdatedAt(LocalDateTime.now());
                timelineMapper.updateById(timeline);
                changes++;
                
                logger.info("Timeline status changed: id={}, title={}, oldStatus={}, newStatus={}",
                        timeline.getId(), timeline.getTitle(), timeline.getStatus(), newStatus);
            }
        }

        return changes;
    }

    private int processGoals() {
        int changes = 0;
        
        List<MemoryGoal> goals = goalMapper.selectList(new LambdaQueryWrapper<MemoryGoal>()
                .ne(MemoryGoal::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        for (MemoryGoal goal : goals) {
            String newStatus = determineNewStatus(goal);
            
            if (!newStatus.equals(goal.getStatus())) {
                goal.setStatus(newStatus);
                goal.setUpdatedAt(LocalDateTime.now());
                goalMapper.updateById(goal);
                changes++;
                
                logger.info("Goal status changed: id={}, title={}, oldStatus={}, newStatus={}",
                        goal.getId(), goal.getTitle(), goal.getStatus(), newStatus);
            }
        }

        return changes;
    }

    private int processTodos() {
        int changes = 0;
        
        List<MemoryTodo> todos = todoMapper.selectList(new LambdaQueryWrapper<MemoryTodo>()
                .ne(MemoryTodo::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        for (MemoryTodo todo : todos) {
            if (todo.getCompleted()) {
                String newStatus = determineNewStatusForCompleted(todo);
                
                if (!newStatus.equals(todo.getStatus())) {
                    todo.setStatus(newStatus);
                    todo.setUpdatedAt(LocalDateTime.now());
                    todoMapper.updateById(todo);
                    changes++;
                    
                    logger.info("Todo status changed (completed): id={}, title={}, oldStatus={}, newStatus={}",
                            todo.getId(), todo.getTitle(), todo.getStatus(), newStatus);
                }
            } else {
                String newStatus = determineNewStatus(todo);
                
                if (!newStatus.equals(todo.getStatus())) {
                    todo.setStatus(newStatus);
                    todo.setUpdatedAt(LocalDateTime.now());
                    todoMapper.updateById(todo);
                    changes++;
                    
                    logger.info("Todo status changed: id={}, title={}, oldStatus={}, newStatus={}",
                            todo.getId(), todo.getTitle(), todo.getStatus(), newStatus);
                }
            }
        }

        return changes;
    }

    private boolean skipDecay(String memoryType) {
        return "Name".equals(memoryType) || "Birthday".equals(memoryType);
    }

    private String determineNewStatus(MemoryFact fact) {
        return determineNewStatusInternal(
                fact.getLastAccessTime(),
                fact.getMemoryType(),
                fact.getStatus()
        );
    }

    private String determineNewStatus(MemoryTimeline timeline) {
        return determineNewStatusInternal(
                timeline.getLastAccessTime(),
                timeline.getMemoryType(),
                timeline.getStatus()
        );
    }

    private String determineNewStatus(MemoryGoal goal) {
        return determineNewStatusInternal(
                goal.getLastAccessTime(),
                goal.getMemoryType(),
                goal.getStatus()
        );
    }

    private String determineNewStatus(MemoryTodo todo) {
        return determineNewStatusInternal(
                todo.getLastAccessTime(),
                todo.getMemoryType(),
                todo.getStatus()
        );
    }

    private String determineNewStatusForCompleted(MemoryTodo todo) {
        LocalDateTime lastAccess = todo.getLastAccessTime();
        if (lastAccess == null) {
            lastAccess = todo.getCreatedAt();
        }
        
        long daysSinceLastAccess = ChronoUnit.DAYS.between(lastAccess, LocalDateTime.now());
        
        if (daysSinceLastAccess >= DELETED_DAYS_THRESHOLD) {
            return MemoryLifecycleService.MemoryStatus.DELETED.name();
        } else if (daysSinceLastAccess >= ARCHIVED_DAYS_THRESHOLD) {
            return MemoryLifecycleService.MemoryStatus.ARCHIVED.name();
        } else if (daysSinceLastAccess >= DORMANT_DAYS_THRESHOLD) {
            return MemoryLifecycleService.MemoryStatus.DORMANT.name();
        }
        
        return todo.getStatus();
    }

    private String determineNewStatusInternal(LocalDateTime lastAccessTime, String memoryType, String currentStatus) {
        if (lastAccessTime == null) {
            return currentStatus;
        }

        long daysSinceLastAccess = ChronoUnit.DAYS.between(lastAccessTime, LocalDateTime.now());
        int dormantThreshold = getDormantThreshold(memoryType);
        int archivedThreshold = getArchivedThreshold(memoryType);
        int deletedThreshold = getDeletedThreshold(memoryType);

        if (daysSinceLastAccess >= deletedThreshold) {
            return MemoryLifecycleService.MemoryStatus.DELETED.name();
        } else if (daysSinceLastAccess >= archivedThreshold) {
            return MemoryLifecycleService.MemoryStatus.ARCHIVED.name();
        } else if (daysSinceLastAccess >= dormantThreshold) {
            return MemoryLifecycleService.MemoryStatus.DORMANT.name();
        } else {
            return MemoryLifecycleService.MemoryStatus.ACTIVE.name();
        }
    }

    private int getDormantThreshold(String memoryType) {
        if (memoryType == null) {
            return DORMANT_DAYS_THRESHOLD;
        }

        return switch (memoryType) {
            case "Todo" -> 7;
            case "Timeline" -> 14;
            case "Goal" -> 15;
            case "Preference" -> 30;
            case "Major", "School" -> 60;
            case "Name", "Birthday" -> Integer.MAX_VALUE;
            default -> DORMANT_DAYS_THRESHOLD;
        };
    }

    private int getArchivedThreshold(String memoryType) {
        if (memoryType == null) {
            return ARCHIVED_DAYS_THRESHOLD;
        }

        return switch (memoryType) {
            case "Todo" -> 30;
            case "Timeline" -> 60;
            case "Goal" -> 45;
            case "Preference" -> 90;
            case "Major", "School" -> 180;
            case "Name", "Birthday" -> Integer.MAX_VALUE;
            default -> ARCHIVED_DAYS_THRESHOLD;
        };
    }

    private int getDeletedThreshold(String memoryType) {
        if (memoryType == null) {
            return DELETED_DAYS_THRESHOLD;
        }

        return switch (memoryType) {
            case "Todo" -> 180;
            case "Timeline" -> 365;
            case "Goal" -> 365;
            case "Preference" -> 365;
            case "Major", "School" -> 730;
            case "Name", "Birthday" -> Integer.MAX_VALUE;
            default -> DELETED_DAYS_THRESHOLD;
        };
    }
}