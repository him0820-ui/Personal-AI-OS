package com.personalai.os.tool;

import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.mapper.MemoryTodoMapper;
import com.personalai.os.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @description: 待办工具，AI可调用此工具查询和操作用户待办事项
 * @author: 琦
 */
@Component
public class TodoTool {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private MemoryTodoMapper todoMapper;

    private Long currentUserId;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public String getName() {
        return "get_todos";
    }

    public String call() {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        List<MemoryTodo> todos = memoryService.getTodos(currentUserId);
        
        StringBuilder result = new StringBuilder();
        result.append("用户的待办事项：\n");
        for (MemoryTodo todo : todos) {
            result.append("- ").append(todo.getTitle())
                  .append(" [").append(todo.getCompleted() ? "已完成" : "待完成").append("]")
                  .append(" (优先级: ").append(todo.getPriority()).append(")\n");
        }
        
        return result.toString();
    }

    public String complete(Map<String, Object> args) {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        if (args == null) {
            return "缺少参数";
        }
        
        String title = (String) args.get("title");
        
        if (title == null || title.isEmpty()) {
            return "待办事项标题不能为空";
        }
        
        List<MemoryTodo> todos = memoryService.getTodos(currentUserId);
        for (MemoryTodo todo : todos) {
            if (todo.getTitle().equals(title)) {
                todo.setCompleted(true);
                todoMapper.updateById(todo);
                return "已完成待办事项：" + title;
            }
        }
        
        return "未找到待办事项：" + title;
    }

    public String delete(Map<String, Object> args) {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        if (args == null) {
            return "缺少参数";
        }
        
        String title = (String) args.get("title");
        
        if (title == null || title.isEmpty()) {
            return "待办事项标题不能为空";
        }
        
        List<MemoryTodo> todos = memoryService.getTodos(currentUserId);
        for (MemoryTodo todo : todos) {
            if (todo.getTitle().equals(title)) {
                todoMapper.deleteById(todo.getId());
                return "已删除待办事项：" + title;
            }
        }
        
        return "未找到待办事项：" + title;
    }
}
