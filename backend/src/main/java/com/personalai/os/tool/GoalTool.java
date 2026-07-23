package com.personalai.os.tool;

import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 目标工具，AI可调用此工具查询用户目标数据
 * @author: 琦
 */
@Component
public class GoalTool {

    @Autowired
    private MemoryService memoryService;

    private Long currentUserId;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public String getName() {
        return "get_goals";
    }

    public String call() {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        List<MemoryGoal> goals = memoryService.getGoals(currentUserId);
        
        StringBuilder result = new StringBuilder();
        result.append("用户的目标：\n");
        for (MemoryGoal goal : goals) {
            result.append("- ").append(goal.getTitle())
                  .append(" (进度: ").append(goal.getProgress()).append("%)")
                  .append(" [").append(goal.getStatus()).append("]\n");
        }
        
        return result.toString();
    }
}
