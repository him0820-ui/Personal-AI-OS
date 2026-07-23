package com.personalai.os.tool;

import com.personalai.os.memory.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @description: 提醒工具，AI可调用此工具添加和查询用户提醒
 * @author: 琦
 */
@Component
public class ReminderTool {

    private static final Logger logger = LoggerFactory.getLogger(ReminderTool.class);

    @Autowired
    private ReminderService reminderService;

    private Long currentUserId;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public String getName() {
        return "get_reminders";
    }

    public String call() {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        List<Map<String, Object>> pending = reminderService.getPendingReminders(currentUserId);
        List<Map<String, Object>> urgent = reminderService.getUrgentReminders(currentUserId);
        
        StringBuilder result = new StringBuilder();
        result.append("用户的待办提醒：\n");
        
        if (pending.isEmpty() && urgent.isEmpty()) {
            result.append("- 暂无待办提醒\n");
        } else {
            for (Map<String, Object> reminder : pending) {
                result.append("- ").append(reminder.get("message")).append("\n");
            }
            for (Map<String, Object> reminder : urgent) {
                result.append("- ⚠️ ").append(reminder.get("message")).append("\n");
            }
        }
        
        return result.toString();
    }

    public String add(Map<String, Object> args) {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        if (args == null) {
            return "缺少提醒参数";
        }
        
        String title = (String) args.get("title");
        String description = (String) args.get("description");
        String time = (String) args.get("time");
        
        if (title == null || title.isEmpty()) {
            return "提醒标题不能为空";
        }
        
        LocalDate dueDate = LocalDate.now();
        LocalDateTime dueDateTime = null;
        
        if (time != null && !time.isEmpty()) {
            dueDateTime = parseTime(time);
            if (dueDateTime != null) {
                dueDate = dueDateTime.toLocalDate();
            }
        }
        
        String message = description != null ? description : title;
        
        if (dueDateTime != null) {
            reminderService.addManualReminder(currentUserId, title, message, dueDate, dueDateTime);
            return "提醒已添加：" + title + "，提醒时间：" + dueDateTime;
        } else {
            reminderService.addManualReminder(currentUserId, title, message, dueDate);
            return "提醒已添加：" + title + "，截止日期：" + dueDate;
        }
    }

    private LocalDateTime parseTime(String time) {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\s*(分钟|分|小时|时|天|日)");
            java.util.regex.Matcher matcher = pattern.matcher(time);
            if (matcher.find()) {
                int amount = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2);
                
                LocalDateTime result = switch (unit) {
                    case "分钟", "分" -> now.plusMinutes(amount);
                    case "小时", "时" -> now.plusHours(amount);
                    case "天", "日" -> now.plusDays(amount);
                    default -> now.plusMinutes(amount);
                };
                logger.info("Parsed relative time '{}' to: {}", time, result);
                return result;
            }
        } catch (Exception e) {
            logger.debug("Failed to parse relative time (pattern 1): {}", time);
        }
        
        try {
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("当前时间[+\\-](\\d+)\s*(分钟|分|小时|时|天|日)");
            java.util.regex.Matcher matcher2 = pattern2.matcher(time);
            if (matcher2.find()) {
                int amount = Integer.parseInt(matcher2.group(1));
                String unit = matcher2.group(2);
                
                LocalDateTime result = switch (unit) {
                    case "分钟", "分" -> now.plusMinutes(amount);
                    case "小时", "时" -> now.plusHours(amount);
                    case "天", "日" -> now.plusDays(amount);
                    default -> now.plusMinutes(amount);
                };
                logger.info("Parsed relative time '{}' to: {}", time, result);
                return result;
            }
        } catch (Exception e) {
            logger.debug("Failed to parse relative time (pattern 2): {}", time);
        }
        
        try {
            return LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            logger.debug("Failed to parse ISO date time: {}", time);
        }
        
        try {
            LocalDate date = LocalDate.parse(time, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay();
        } catch (Exception e) {
            logger.debug("Failed to parse ISO date: {}", time);
        }
        
        try {
            java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})");
            java.util.regex.Matcher timeMatcher = timePattern.matcher(time);
            if (timeMatcher.find()) {
                int hour = Integer.parseInt(timeMatcher.group(1));
                int minute = Integer.parseInt(timeMatcher.group(2));
                LocalDateTime result = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), hour, minute);
                if (result.isBefore(now)) {
                    result = result.plusDays(1);
                }
                return result;
            }
        } catch (Exception e) {
            logger.debug("Failed to parse time of day: {}", time);
        }
        
        return null;
    }
}