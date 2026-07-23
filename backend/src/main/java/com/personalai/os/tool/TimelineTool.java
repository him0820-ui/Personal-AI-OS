package com.personalai.os.tool;

import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 时间线工具，AI可调用此工具查询用户时间线数据
 * @author: 琦
 */
@Component
public class TimelineTool {

    @Autowired
    private MemoryService memoryService;

    private Long currentUserId;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public String getName() {
        return "get_timeline";
    }

    public String call() {
        if (currentUserId == null) {
            return "User not authenticated";
        }
        
        List<MemoryTimeline> timeline = memoryService.getTimeline(currentUserId);
        
        StringBuilder result = new StringBuilder();
        result.append("用户的成长轨迹：\n");
        for (MemoryTimeline event : timeline) {
            result.append("- ").append(event.getTimestamp().toLocalDate())
                  .append(": ").append(event.getTitle())
                  .append(" [").append(event.getDescription() != null ? event.getDescription() : "未分类").append("]\n");
        }
        
        return result.toString();
    }
}
