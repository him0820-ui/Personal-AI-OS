package com.personalai.os.memory;

import com.personalai.os.memory.dto.ExtractedInfo;
import org.springframework.stereotype.Component;

/**
 * @description: 记忆评分器，评估记忆的置信度和重要性
 * @author: 琦
 */
@Component
public class Scorer {

    public void score(ExtractedInfo info) {
        if (info.getScore() != null && info.getScore() > 0) {
            return;
        }
        
        String content = info.getContent();
        String category = info.getCategory();
        
        int score = 50;
        
        if ("Fact".equals(category)) {
            String[] highImportance = {"专业", "职业", "技能", "学历", "工作", "项目"};
            String[] lowImportance = {"爱好", "喜欢", "兴趣"};
            
            for (String keyword : highImportance) {
                if (content.contains(keyword)) {
                    score += 20;
                    break;
                }
            }
            
            for (String keyword : lowImportance) {
                if (content.contains(keyword)) {
                    score -= 15;
                    break;
                }
            }
        } else if ("Goal".equals(category)) {
            String[] highImportance = {"秋招", "面试", "工作", "考试", "项目"};
            for (String keyword : highImportance) {
                if (content.contains(keyword)) {
                    score += 25;
                    break;
                }
            }
        } else if ("Timeline".equals(category)) {
            String[] highImportance = {"完成", "获得", "面试", "录取"};
            for (String keyword : highImportance) {
                if (content.contains(keyword)) {
                    score += 15;
                    break;
                }
            }
        }
        
        if (content.length() > 50) {
            score += 5;
        }
        
        info.setScore(Math.min(100, Math.max(10, score)));
    }
}
