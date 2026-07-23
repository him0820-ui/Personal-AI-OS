package com.personalai.os.service;

import org.springframework.stereotype.Component;

/**
 * @description: Mock聊天客户端，用于测试环境下模拟AI响应
 * @author: 琦
 */
@Component
public class MockChatClient {

    public String generateResponse(String prompt) {
        if (prompt.contains("你好") || prompt.contains("Hello") || prompt.contains("hi")) {
            return "你好！我是你的个人AI助手。我可以帮助你管理长期记忆、目标规划和成长轨迹。有什么我可以帮你的吗？";
        }
        
        if (prompt.contains("记忆") || prompt.contains("memory") || prompt.contains("fact")) {
            return "我已经记住了你提到的信息。我的记忆引擎会自动分析和整理这些内容，帮助你更好地管理个人认知。";
        }
        
        if (prompt.contains("目标") || prompt.contains("goal") || prompt.contains("计划")) {
            return "好的，我来帮你规划这个目标。请告诉我更多细节，我会帮你制定详细的计划和追踪进度。";
        }
        
        if (prompt.contains("总结") || prompt.contains("summary") || prompt.contains("今日")) {
            return "今日总结：你今天完成了很多工作！继续保持，明天也会很棒。完成率：80%。";
        }
        
        if (prompt.contains("待办") || prompt.contains("todo") || prompt.contains("任务")) {
            return "我已经帮你记录了这个待办事项。记得按时完成哦！";
        }
        
        if (prompt.contains("时间线") || prompt.contains("timeline") || prompt.contains("轨迹")) {
            return "你的成长轨迹正在被记录。回顾过去，展望未来，你已经取得了很大的进步！";
        }
        
        return "这是一个很好的话题！让我帮你分析一下...\n\n" +
               "根据我们的对话，我了解到你正在关注个人成长和认知管理。\n" +
               "我可以帮助你：\n" +
               "1. 记住重要信息（长期记忆）\n" +
               "2. 规划目标和追踪进度\n" +
               "3. 记录成长轨迹\n" +
               "4. 管理待办事项\n" +
               "5. 生成每日总结\n\n" +
               "请问你想从哪个方面开始？";
    }
}
