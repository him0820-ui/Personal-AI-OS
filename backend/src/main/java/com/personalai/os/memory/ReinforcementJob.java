package com.personalai.os.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalai.os.entity.Conversation;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.ConversationMapper;
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
import java.util.List;

/**
 * @description: 记忆强化定时任务，根据对话提及和完成情况自动强化记忆重要性
 * @author: 琦
 */
@Component
public class ReinforcementJob {

    private static final Logger logger = LoggerFactory.getLogger(ReinforcementJob.class);

    private static final int RECENT_DAYS = 7;
    private static final int REINFORCE_IMPORTANCE_FOR_GOAL_COMPLETE = 5;
    private static final int REINFORCE_IMPORTANCE_FOR_MENTION = 3;
    private static final int REINFORCE_CONFIDENCE_FOR_MENTION = 1;
    private static final int REINFORCE_IMPORTANCE_FOR_TIMELINE_REF = 3;

    @Autowired
    private MemoryFactMapper factMapper;

    @Autowired
    private MemoryTimelineMapper timelineMapper;

    @Autowired
    private MemoryGoalMapper goalMapper;

    @Autowired
    private MemoryTodoMapper todoMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MemoryLifecycleService lifecycleService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void runReinforcement() {
        logger.info("Starting memory reinforcement job at {}", LocalDateTime.now());

        List<MemoryFact> allFacts = factMapper.selectList(new LambdaQueryWrapper<MemoryFact>()
                .ne(MemoryFact::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        List<MemoryGoal> allGoals = goalMapper.selectList(new LambdaQueryWrapper<MemoryGoal>()
                .ne(MemoryGoal::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        List<MemoryTimeline> allTimelines = timelineMapper.selectList(new LambdaQueryWrapper<MemoryTimeline>()
                .ne(MemoryTimeline::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        List<MemoryTodo> allTodos = todoMapper.selectList(new LambdaQueryWrapper<MemoryTodo>()
                .ne(MemoryTodo::getStatus, MemoryLifecycleService.MemoryStatus.DELETED.name()));

        int mentionReinforcements = reinforceByConversationMention(allFacts, allGoals, allTimelines, allTodos);
        int goalReinforcements = reinforceByGoalCompletion(allGoals, allFacts);
        int timelineReinforcements = reinforceByTimelineReference(allTimelines, allFacts);
        int todoReinforcements = reinforceByTodoCompletion(allTodos, allFacts);

        logger.info("Memory reinforcement job completed. Mention: {}, Goal: {}, Timeline: {}, Todo: {}",
                mentionReinforcements, goalReinforcements, timelineReinforcements, todoReinforcements);
    }

    private int reinforceByConversationMention(List<MemoryFact> facts, List<MemoryGoal> goals, 
                                               List<MemoryTimeline> timelines, List<MemoryTodo> todos) {
        int count = 0;

        if (facts.isEmpty()) {
            return 0;
        }

        Long userId = facts.get(0).getUserId();
        List<Conversation> recentConversations = conversationMapper.findRecentDaysByUserId(userId, RECENT_DAYS);

        if (recentConversations.isEmpty()) {
            return 0;
        }

        StringBuilder conversationText = new StringBuilder();
        for (Conversation conv : recentConversations) {
            if (conv.getContent() != null) {
                conversationText.append(conv.getContent()).append(" ");
            }
        }

        String text = conversationText.toString().toLowerCase();

        for (MemoryFact fact : facts) {
            if (containsMention(text, fact)) {
                reinforceFact(fact, REINFORCE_IMPORTANCE_FOR_MENTION, REINFORCE_CONFIDENCE_FOR_MENTION);
                count++;
            }
        }

        for (MemoryGoal goal : goals) {
            if (containsMention(text, goal)) {
                reinforceGoal(goal, REINFORCE_IMPORTANCE_FOR_MENTION);
                count++;
            }
        }

        for (MemoryTimeline timeline : timelines) {
            if (containsMention(text, timeline)) {
                reinforceTimeline(timeline, REINFORCE_IMPORTANCE_FOR_MENTION);
                count++;
            }
        }

        for (MemoryTodo todo : todos) {
            if (containsMention(text, todo)) {
                reinforceTodo(todo, REINFORCE_IMPORTANCE_FOR_MENTION);
                count++;
            }
        }

        return count;
    }

    private boolean containsMention(String text, MemoryFact fact) {
        if (fact.getKey() != null && text.contains(fact.getKey().toLowerCase())) {
            return true;
        }
        if (fact.getValue() != null && text.contains(fact.getValue().toLowerCase())) {
            return true;
        }
        return false;
    }

    private boolean containsMention(String text, MemoryGoal goal) {
        if (goal.getTitle() != null && text.contains(goal.getTitle().toLowerCase())) {
            return true;
        }
        if (goal.getDescription() != null && text.contains(goal.getDescription().toLowerCase())) {
            return true;
        }
        return false;
    }

    private boolean containsMention(String text, MemoryTimeline timeline) {
        if (timeline.getTitle() != null && text.contains(timeline.getTitle().toLowerCase())) {
            return true;
        }
        if (timeline.getDescription() != null && text.contains(timeline.getDescription().toLowerCase())) {
            return true;
        }
        return false;
    }

    private boolean containsMention(String text, MemoryTodo todo) {
        if (todo.getTitle() != null && text.contains(todo.getTitle().toLowerCase())) {
            return true;
        }
        return false;
    }

    private int reinforceByGoalCompletion(List<MemoryGoal> goals, List<MemoryFact> facts) {
        int count = 0;

        for (MemoryGoal goal : goals) {
            if (goal.getProgress() != null && goal.getProgress() >= 100) {
                reinforceGoal(goal, REINFORCE_IMPORTANCE_FOR_GOAL_COMPLETE);
                count++;

                for (MemoryFact fact : facts) {
                    if (fact.getUserId().equals(goal.getUserId())) {
                        if (fact.getKey() != null && goal.getTitle().contains(fact.getKey()) ||
                            fact.getValue() != null && goal.getTitle().contains(fact.getValue())) {
                            reinforceFact(fact, REINFORCE_IMPORTANCE_FOR_GOAL_COMPLETE, 0);
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    private int reinforceByTodoCompletion(List<MemoryTodo> todos, List<MemoryFact> facts) {
        int count = 0;

        for (MemoryTodo todo : todos) {
            if (todo.getCompleted()) {
                reinforceTodo(todo, REINFORCE_IMPORTANCE_FOR_GOAL_COMPLETE);
                count++;

                for (MemoryFact fact : facts) {
                    if (fact.getUserId().equals(todo.getUserId())) {
                        if (fact.getKey() != null && todo.getTitle().contains(fact.getKey()) ||
                            fact.getValue() != null && todo.getTitle().contains(fact.getValue())) {
                            reinforceFact(fact, REINFORCE_IMPORTANCE_FOR_GOAL_COMPLETE, 0);
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    private int reinforceByTimelineReference(List<MemoryTimeline> timelines, List<MemoryFact> facts) {
        int count = 0;

        for (MemoryTimeline timeline : timelines) {
            String timelineText = (timeline.getTitle() != null ? timeline.getTitle() : "") + 
                                 (timeline.getDescription() != null ? timeline.getDescription() : "");

            for (MemoryFact fact : facts) {
                if (fact.getUserId().equals(timeline.getUserId())) {
                    if (fact.getKey() != null && timelineText.contains(fact.getKey()) ||
                        fact.getValue() != null && timelineText.contains(fact.getValue())) {
                        reinforceFact(fact, REINFORCE_IMPORTANCE_FOR_TIMELINE_REF, 0);
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private void reinforceFact(MemoryFact fact, int importanceDelta, int confidenceDelta) {
        int newImportance = Math.min(100, (fact.getImportance() != null ? fact.getImportance() : 50) + importanceDelta);
        int newConfidence = Math.min(100, (fact.getConfidence() != null ? fact.getConfidence() : 0) + confidenceDelta);
        
        fact.setImportance(newImportance);
        fact.setConfidence(newConfidence);
        fact.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(fact.getStatus())) {
            fact.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        factMapper.updateById(fact);
        
        logger.debug("Fact reinforced: id={}, key={}, importance={} (delta={}), confidence={} (delta={})",
                fact.getId(), fact.getKey(), newImportance, importanceDelta, newConfidence, confidenceDelta);
    }

    private void reinforceGoal(MemoryGoal goal, int importanceDelta) {
        int newImportance = Math.min(100, (goal.getImportance() != null ? goal.getImportance() : 50) + importanceDelta);
        
        goal.setImportance(newImportance);
        goal.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(goal.getStatus())) {
            goal.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        goalMapper.updateById(goal);
        
        logger.debug("Goal reinforced: id={}, title={}, importance={} (delta={})",
                goal.getId(), goal.getTitle(), newImportance, importanceDelta);
    }

    private void reinforceTimeline(MemoryTimeline timeline, int importanceDelta) {
        int newImportance = Math.min(100, (timeline.getImportance() != null ? timeline.getImportance() : 50) + importanceDelta);
        
        timeline.setImportance(newImportance);
        timeline.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(timeline.getStatus())) {
            timeline.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        timelineMapper.updateById(timeline);
        
        logger.debug("Timeline reinforced: id={}, title={}, importance={} (delta={})",
                timeline.getId(), timeline.getTitle(), newImportance, importanceDelta);
    }

    private void reinforceTodo(MemoryTodo todo, int importanceDelta) {
        int newImportance = Math.min(100, (todo.getImportance() != null ? todo.getImportance() : 50) + importanceDelta);
        
        todo.setImportance(newImportance);
        todo.setLastAccessTime(LocalDateTime.now());
        
        if (!MemoryLifecycleService.MemoryStatus.DELETED.name().equals(todo.getStatus())) {
            todo.setStatus(MemoryLifecycleService.MemoryStatus.ACTIVE.name());
        }
        
        todoMapper.updateById(todo);
        
        logger.debug("Todo reinforced: id={}, title={}, importance={} (delta={})",
                todo.getId(), todo.getTitle(), newImportance, importanceDelta);
    }
}