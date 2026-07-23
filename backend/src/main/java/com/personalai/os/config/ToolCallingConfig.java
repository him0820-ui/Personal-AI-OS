package com.personalai.os.config;

import com.personalai.os.tool.GoalTool;
import com.personalai.os.tool.MemoryTool;
import com.personalai.os.tool.ReminderTool;
import com.personalai.os.tool.TimelineTool;
import com.personalai.os.tool.TodoTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @description: 工具调用配置类，统一管理AI工具调用路由
 * @author: 琦
 */
@Configuration
public class ToolCallingConfig {

    @Autowired
    private MemoryTool memoryTool;

    @Autowired
    private GoalTool goalTool;

    @Autowired
    private TimelineTool timelineTool;

    @Autowired
    private TodoTool todoTool;

    @Autowired
    private ReminderTool reminderTool;

    public String callTool(String toolName, Long userId) {
        return callTool(toolName, userId, null);
    }

    public String callTool(String toolName, Long userId, Map<String, Object> args) {
        memoryTool.setCurrentUserId(userId);
        goalTool.setCurrentUserId(userId);
        timelineTool.setCurrentUserId(userId);
        todoTool.setCurrentUserId(userId);
        reminderTool.setCurrentUserId(userId);
        
        return switch (toolName) {
            case "get_memory" -> memoryTool.call();
            case "get_goals" -> goalTool.call();
            case "get_timeline" -> timelineTool.call();
            case "get_todos" -> todoTool.call();
            case "complete_todo" -> todoTool.complete(args);
            case "delete_todo" -> todoTool.delete(args);
            case "get_reminders" -> reminderTool.call();
            case "add_reminder", "add_reminders" -> reminderTool.add(args);
            default -> "Unknown tool: " + toolName;
        };
    }
}
