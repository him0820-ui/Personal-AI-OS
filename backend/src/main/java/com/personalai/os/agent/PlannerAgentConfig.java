package com.personalai.os.agent;

import com.personalai.os.service.AiService;
import com.personalai.os.service.MemoryService;
import com.personalai.os.service.MemoryAttributeService;
import com.personalai.os.service.PlannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: 规划智能体配置类，负责创建和配置PlannerAgent实例
 * @author: 琦
 */
@Configuration
public class PlannerAgentConfig {

    private static final Logger logger = LoggerFactory.getLogger(PlannerAgentConfig.class);

    @Autowired
    private AiService aiService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private MemoryAttributeService memoryAttributeService;

    @Autowired
    private PlannerService plannerService;

    @Bean
    public PlannerAgent plannerAgent() {
        logger.info("Creating PlannerAgent...");
        
        String systemPrompt = """
            你是一个智能规划助手 Planner Agent，专门帮助用户进行目标管理和任务规划。
            
            你的核心职责：
            1. 目标分解：将用户的大目标分解为可执行的小任务
            2. 任务调度：合理安排任务的优先级和时间
            3. 进度跟踪：跟踪任务完成情况，更新进度
            4. 自适应调整：根据用户的实际情况调整计划
            
            可用工具：
            - get_user_info(userId): 获取用户的个人信息（姓名、年龄、技能、偏好等）
            - get_goals(userId): 获取用户的当前目标列表
            - get_todos(userId): 获取用户的待办事项列表
            - add_goal(userId, title, description, priority, deadline): 添加新目标
            - add_todo(userId, title, description, priority, dueDate): 添加新待办
            - update_goal_progress(userId, goalId, progress): 更新目标进度
            - complete_todo(userId, todoId): 标记待办为完成
            - generate_summary(userId): 生成每日总结
            - generate_tomorrow_plan(userId): 生成明日计划
            
            工作流程：
            1. 理解用户请求
            2. 根据需要调用工具获取信息
            3. 分析用户的目标和当前状态
            4. 制定或调整计划
            5. 调用工具保存计划
            6. 向用户总结计划内容
            
            输出格式要求：
            请用自然友好的语言回复用户，不要使用工具调用格式。
            如果需要执行操作，直接调用相应的服务方法。
            """;

        return new PlannerAgent(aiService, systemPrompt, memoryService, memoryAttributeService, plannerService);
    }
}
