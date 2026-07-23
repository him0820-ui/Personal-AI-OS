package com.personalai.os.scheduler;

import com.personalai.os.dto.response.DailySummaryResponse;
import com.personalai.os.entity.User;
import com.personalai.os.mapper.UserMapper;
import com.personalai.os.service.MemorySummaryService;
import com.personalai.os.service.PlannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DailySummaryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DailySummaryScheduler.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private MemorySummaryService memorySummaryService;

    @Scheduled(cron = "0 0 22 * * ?")
    public void generateDailySummaryForAllUsers() {
        logger.info("Starting daily summary generation for all users...");
        
        List<User> users = userMapper.selectList(null);
        
        for (User user : users) {
            try {
                DailySummaryResponse summary = plannerService.generateSummary(user.getId());
                logger.info("Generated daily summary for user {}: {}", user.getUsername(), summary.getSummary());
            } catch (Exception e) {
                logger.error("Failed to generate daily summary for user {}: {}", user.getUsername(), e.getMessage());
            }
            
            try {
                memorySummaryService.updateSummary(user.getId());
                logger.info("Updated memory summary for user {}", user.getUsername());
            } catch (Exception e) {
                logger.error("Failed to update memory summary for user {}: {}", user.getUsername(), e.getMessage());
            }
        }
        
        logger.info("Daily summary generation completed.");
    }
}
