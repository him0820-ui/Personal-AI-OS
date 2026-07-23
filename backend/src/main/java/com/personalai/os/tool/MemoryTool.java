package com.personalai.os.tool;

import com.personalai.os.entity.MemoryFact;
import com.personalai.os.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 记忆工具，AI可调用此工具查询用户记忆数据
 * @author: 琦
 */
@Component
public class MemoryTool {

    @Autowired
    private MemoryService memoryService;

    private Long currentUserId;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public String getName() {
        return "get_memory";
    }

    public String call() {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        List<MemoryFact> facts = memoryService.getFacts(currentUserId);
        
        StringBuilder result = new StringBuilder();
        result.append("用户的长期记忆：\n");
        for (MemoryFact fact : facts) {
            result.append("- ").append(fact.getKey()).append(": ").append(fact.getValue()).append("\n");
        }
        
        return result.toString();
    }
}
