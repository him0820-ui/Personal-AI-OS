package com.personalai.os.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.MemoryGoalMapper;
import com.personalai.os.mapper.MemoryTodoMapper;
import com.personalai.os.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @description: 提醒服务类，管理提醒的添加、查询和定时检测
 * @author: 琦
 */
@Service
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    @Autowired
    private MemoryGoalMapper goalMapper;

    @Autowired
    private MemoryTodoMapper todoMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MemoryLifecycleService lifecycleService;

    @Autowired
    private NotificationService notificationService;

    private static final String REMINDER_KEY_PREFIX = "reminder:";
    private static final String PENDING_REMINDERS_KEY = "pending_reminders";
    private static final String SENT_REMINDERS_KEY = "sent_reminders:";

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkDailyReminders() {
        logger.info("Checking daily reminders at {}", LocalDateTime.now());
        List<Map<String, Object>> allReminders = new ArrayList<>();

        LocalDate today = LocalDate.now();

        allReminders.addAll(checkTodoDeadlines(today));
        allReminders.addAll(checkGoalDeadlines(today));

        if (!allReminders.isEmpty()) {
            redisTemplate.opsForValue().set(PENDING_REMINDERS_KEY, allReminders, 24, TimeUnit.HOURS);
            logger.info("Stored {} pending reminders", allReminders.size());
        }
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void checkUrgentReminders() {
        logger.debug("Checking urgent reminders");
        List<Map<String, Object>> urgentReminders = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        urgentReminders.addAll(checkTodoDeadlines(tomorrow));
        urgentReminders.addAll(checkGoalDeadlines(tomorrow));

        for (Map<String, Object> reminder : urgentReminders) {
            Long userId = (Long) reminder.get("userId");
            String key = REMINDER_KEY_PREFIX + userId + ":" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(key, reminder, 1, TimeUnit.HOURS);
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void checkRealTimeReminders() {
        logger.debug("Checking real-time reminders at {}", LocalDateTime.now());
        
        String pattern = REMINDER_KEY_PREFIX + "*:*";
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                LocalDateTime now = LocalDateTime.now();
                
                for (String key : keys) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reminder = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (reminder == null) {
                        continue;
                    }
                    
                    Long userId = (Long) reminder.get("userId");
                    String title = (String) reminder.get("title");
                    String message = (String) reminder.get("message");
                    Object dueDateTimeObj = reminder.get("dueDateTime");
                    
                    if (dueDateTimeObj != null) {
                        try {
                            LocalDateTime dueDateTime;
                            if (dueDateTimeObj instanceof String) {
                                dueDateTime = LocalDateTime.parse((String) dueDateTimeObj, 
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            } else {
                                continue;
                            }
                            
                            if (!now.isBefore(dueDateTime) && !now.isAfter(dueDateTime.plusMinutes(5))) {
                                String sentKey = SENT_REMINDERS_KEY + userId + ":" + key;
                                Boolean sent = redisTemplate.hasKey(sentKey);
                                if (sent == null || !sent) {
                                    logger.info("Sending real-time reminder to user {}: {} at {}", userId, title, now);
                                    notificationService.sendNotification(userId, title, message);
                                    redisTemplate.opsForValue().set(sentKey, true, 24, TimeUnit.HOURS);
                                } else {
                                    logger.debug("Reminder already sent to user {}: {}", userId, title);
                                }
                            } else if (now.isBefore(dueDateTime)) {
                                logger.debug("Reminder not yet due for user {}: {}, due at {}, now is {}", userId, title, dueDateTime, now);
                            } else {
                                logger.debug("Reminder expired for user {}: {}, due at {}, now is {}", userId, title, dueDateTime, now);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to parse dueDateTime for reminder: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to check real-time reminders: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> checkTodoDeadlines(LocalDate date) {
        List<Map<String, Object>> reminders = new ArrayList<>();

        LambdaQueryWrapper<MemoryTodo> wrapper = new LambdaQueryWrapper<MemoryTodo>()
                .eq(MemoryTodo::getStatus, MemoryLifecycleService.MemoryStatus.ACTIVE.name())
                .eq(MemoryTodo::getCompleted, false)
                .eq(MemoryTodo::getDueDate, date);
        
        List<MemoryTodo> todos = todoMapper.selectList(wrapper);

        for (MemoryTodo todo : todos) {
            Map<String, Object> reminder = new HashMap<>();
            reminder.put("type", "TODO");
            reminder.put("userId", todo.getUserId());
            reminder.put("id", todo.getId());
            reminder.put("title", todo.getTitle());
            reminder.put("dueDate", todo.getDueDate());
            reminder.put("priority", todo.getPriority());
            reminder.put("message", "提醒：待办事项 '" + todo.getTitle() + "' 今天到期！");
            reminders.add(reminder);

            logger.info("Todo reminder: userId={}, title={}, dueDate={}", 
                    todo.getUserId(), todo.getTitle(), todo.getDueDate());
        }

        return reminders;
    }

    private List<Map<String, Object>> checkGoalDeadlines(LocalDate date) {
        List<Map<String, Object>> reminders = new ArrayList<>();

        LambdaQueryWrapper<MemoryGoal> wrapper = new LambdaQueryWrapper<MemoryGoal>()
                .eq(MemoryGoal::getStatus, MemoryLifecycleService.MemoryStatus.ACTIVE.name())
                .lt(MemoryGoal::getProgress, 100)
                .eq(MemoryGoal::getDeadline, date);
        
        List<MemoryGoal> goals = goalMapper.selectList(wrapper);

        for (MemoryGoal goal : goals) {
            Map<String, Object> reminder = new HashMap<>();
            reminder.put("type", "GOAL");
            reminder.put("userId", goal.getUserId());
            reminder.put("id", goal.getId());
            reminder.put("title", goal.getTitle());
            reminder.put("deadline", goal.getDeadline());
            reminder.put("progress", goal.getProgress());
            reminder.put("priority", goal.getPriority());
            reminder.put("message", "提醒：目标 '" + goal.getTitle() + "' 今天到期！当前进度：" + (goal.getProgress() != null ? goal.getProgress() : 0) + "%");
            reminders.add(reminder);

            logger.info("Goal reminder: userId={}, title={}, deadline={}, progress={}%", 
                    goal.getUserId(), goal.getTitle(), goal.getDeadline(), goal.getProgress());
        }

        return reminders;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPendingReminders(Long userId) {
        List<Map<String, Object>> allReminders = (List<Map<String, Object>>) redisTemplate.opsForValue().get(PENDING_REMINDERS_KEY);
        if (allReminders == null) {
            return new ArrayList<>();
        }

        return allReminders.stream()
                .filter(r -> userId.equals(r.get("userId")))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUrgentReminders(Long userId) {
        List<Map<String, Object>> urgentReminders = new ArrayList<>();
        String pattern = REMINDER_KEY_PREFIX + userId + ":*";

        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    Map<String, Object> reminder = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                    if (reminder != null) {
                        urgentReminders.add(reminder);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get urgent reminders: {}", e.getMessage());
        }

        return urgentReminders;
    }

    public void clearReminders(Long userId) {
        String pattern = REMINDER_KEY_PREFIX + userId + ":*";
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            logger.warn("Failed to clear reminders: {}", e.getMessage());
        }
    }

    public void addManualReminder(Long userId, String title, String message, LocalDate dueDate) {
        addManualReminder(userId, title, message, dueDate, null);
    }

    public void addManualReminder(Long userId, String title, String message, LocalDate dueDate, LocalDateTime dueDateTime) {
        Map<String, Object> reminder = new HashMap<>();
        reminder.put("type", "MANUAL");
        reminder.put("userId", userId);
        reminder.put("title", title);
        reminder.put("dueDate", dueDate);
        if (dueDateTime != null) {
            reminder.put("dueDateTime", dueDateTime.toString());
        }
        reminder.put("message", message);

        String key = REMINDER_KEY_PREFIX + userId + ":" + System.currentTimeMillis();
        
        long ttlDays = ChronoUnit.DAYS.between(LocalDate.now(), dueDate) + 1;
        if (dueDateTime != null) {
            long ttlMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), dueDateTime) + 60;
            if (ttlMinutes > 0) {
                redisTemplate.opsForValue().set(key, reminder, ttlMinutes, TimeUnit.MINUTES);
            } else {
                redisTemplate.opsForValue().set(key, reminder, 24, TimeUnit.HOURS);
            }
        } else {
            redisTemplate.opsForValue().set(key, reminder, ttlDays, TimeUnit.DAYS);
        }
        
        try {
            MemoryTodo todo = new MemoryTodo();
            todo.setUserId(userId);
            todo.setTitle(title);
            todo.setCompleted(false);
            todo.setPriority(1);
            todo.setImportance(50);
            todo.setConfidence(50);
            todo.setDueDate(dueDate);
            todo.setReminderTime(dueDateTime);
            todo.setMemoryType("TODO");
            todo.setStatus("ACTIVE");
            todo.setCreatedAt(LocalDateTime.now());
            todo.setUpdatedAt(LocalDateTime.now());
            
            todoMapper.insert(todo);
            logger.info("Manual reminder saved to database: userId={}, title={}, todoId={}, reminderTime={}", userId, title, todo.getId(), dueDateTime);
        } catch (Exception e) {
            logger.warn("Failed to save reminder to database: {}", e.getMessage());
        }
        
        logger.info("Manual reminder added: userId={}, title={}, dueDate={}, dueDateTime={}", userId, title, dueDate, dueDateTime);
    }
}