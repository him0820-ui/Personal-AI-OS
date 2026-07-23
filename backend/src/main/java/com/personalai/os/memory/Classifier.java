package com.personalai.os.memory;

import com.personalai.os.memory.dto.ExtractedInfo;
import org.springframework.stereotype.Component;

/**
 * @description: 记忆分类器，根据内容特征将记忆分类为事实、目标、时间线或待办
 * @author: 琦
 */
@Component
public class Classifier {

    public void classify(ExtractedInfo info) {
        String content = info.getContent().toLowerCase();
        String category = info.getCategory();
        
        if (category == null || category.isEmpty()) {
            if (content.contains("专业") || content.contains("学校") || content.contains("职业") || 
                content.contains("技能") || content.contains("姓名") || content.contains("年龄")) {
                info.setCategory("Fact");
            } else if (content.contains("学习") || content.contains("完成") || content.contains("面试") ||
                       content.contains("参加") || content.contains("获得") || content.contains("做了")) {
                info.setCategory("Timeline");
            } else if (content.contains("目标") || content.contains("计划") || content.contains("想要") ||
                       content.contains("打算") || content.contains("希望") || content.contains("争取")) {
                info.setCategory("Goal");
            } else if (content.contains("需要") || content.contains("应该") || content.contains("记得") ||
                       content.contains("待办") || content.contains("任务") || content.contains("事项")) {
                info.setCategory("Todo");
            } else {
                info.setCategory("Fact");
            }
        }
        
        if ("Fact".equals(info.getCategory()) && (info.getKey() == null || info.getKey().isEmpty())) {
            if (content.contains("专业")) {
                info.setKey("专业");
            } else if (content.contains("学校")) {
                info.setKey("学校");
            } else if (content.contains("职业")) {
                info.setKey("职业");
            } else if (content.contains("技能")) {
                info.setKey("技能");
            } else {
                info.setKey("其他");
            }
        }
    }
}
