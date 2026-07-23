package com.personalai.os.agent;

import com.personalai.os.dto.response.DailySummaryResponse;
import com.personalai.os.dto.response.TaskRecommendationResponse;
import com.personalai.os.dto.response.TomorrowPlanResponse;
import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.service.AiService;
import com.personalai.os.service.MemoryService;
import com.personalai.os.service.MemoryAttributeService;
import com.personalai.os.service.PlannerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 规划智能体，负责处理用户的目标管理和任务规划请求
 * @author: 琦
 */
@Component
public class PlannerAgent {

    private static final Logger logger = LoggerFactory.getLogger(PlannerAgent.class);

    private final AiService aiService;
    private final String systemPrompt;
    private final MemoryService memoryService;
    private final MemoryAttributeService memoryAttributeService;
    private final PlannerService plannerService;
    
    @Autowired
    private ObjectMapper objectMapper;

    public PlannerAgent(AiService aiService, String systemPrompt, 
                       MemoryService memoryService, MemoryAttributeService memoryAttributeService,
                       PlannerService plannerService) {
        this.aiService = aiService;
        this.systemPrompt = systemPrompt;
        this.memoryService = memoryService;
        this.memoryAttributeService = memoryAttributeService;
        this.plannerService = plannerService;
        logger.info("PlannerAgent initialized");
    }

    public String execute(Long userId, String userRequest) {
        logger.info("PlannerAgent executing for user {}: {}", userId, userRequest);
        
        try {
            String lowerRequest = userRequest.toLowerCase();
            
            boolean hasChinese = containsChinese(userRequest);
            boolean hasJava = lowerRequest.contains("java");
            
            logger.info("User request analysis - hasChinese: {}, hasJava: {}", hasChinese, hasJava);
            
            if (hasChinese && hasJava) {
                logger.info("Detected goal planning request with Java");
                return decomposeGoal(userId, userRequest);
            }
            
            if (lowerRequest.contains("learn") || lowerRequest.contains("goal") || lowerRequest.contains("plan") || lowerRequest.contains("study")) {
                return decomposeGoal(userId, userRequest);
            } else if (lowerRequest.contains("schedule") || lowerRequest.contains("priorit") || lowerRequest.contains("arrange")) {
                return scheduleTasks(userId, userRequest);
            } else if (lowerRequest.contains("complete") || lowerRequest.contains("progress") || lowerRequest.contains("update")) {
                return updateProgress(userId, userRequest);
            } else if (lowerRequest.contains("adjust") || lowerRequest.contains("modify") || lowerRequest.contains("change")) {
                return adjustPlan(userId, userRequest);
            } else if (lowerRequest.contains("summary") || lowerRequest.contains("review") || lowerRequest.contains("today")) {
                return generateDailySummary(userId);
            } else if (lowerRequest.contains("tomorrow")) {
                return generateTomorrowPlan(userId);
            } else if (lowerRequest.contains("recommend") || lowerRequest.contains("suggest")) {
                return generateRecommendations(userId);
            } else {
                return decomposeGoal(userId, userRequest);
            }
        } catch (Exception e) {
            logger.error("PlannerAgent execution failed", e);
            return "抱歉，我在处理您的请求时遇到了一些问题，请稍后再试。";
        }
    }
    
    private boolean containsChinese(String str) {
        if (str == null) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private String analyzeRequest(Long userId, String userRequest) {
        String prompt = """
            你是一个请求分类器。请分析用户的请求类型，从以下选项中选择最合适的一个：
            NEED_GOAL_DECOMPOSITION, NEED_TASK_SCHEDULING, NEED_PROGRESS_UPDATE, NEED_PLAN_ADJUSTMENT, NEED_SUMMARY, NEED_TOMORROW_PLAN, NEED_RECOMMENDATIONS, NEED_GOAL_CREATION, NEED_TODO_CREATION, GENERAL

            判断规则：
            - 包含"学习"、"目标"、"规划"、"计划"等词且涉及分解大目标为小任务 -> NEED_GOAL_DECOMPOSITION
            - 包含"安排"、"排序"、"优先级"、"时间"等词且涉及任务调度 -> NEED_TASK_SCHEDULING
            - 包含"完成"、"进度"、"更新"等词 -> NEED_PROGRESS_UPDATE
            - 包含"调整"、"修改"、"变更"等词且涉及计划调整 -> NEED_PLAN_ADJUSTMENT
            - 包含"总结"、"回顾"、"今日"等词 -> NEED_SUMMARY
            - 包含"明日"、"明天"、"下一天"等词 -> NEED_TOMORROW_PLAN
            - 包含"推荐"、"建议"等词 -> NEED_RECOMMENDATIONS
            - 包含"创建目标"、"添加目标"等词 -> NEED_GOAL_CREATION
            - 包含"创建待办"、"添加待办"等词 -> NEED_TODO_CREATION
            - 其他情况 -> GENERAL

            用户请求：%s

            请严格只返回一个类型名称，不要包含任何其他文字。
            """.formatted(userRequest);

        String response = aiService.generateResponse(prompt);
        logger.info("Request analysis result: '{}'", response);
        logger.info("User request contains '规划': {}, '目标': {}, '学习': {}", 
                userRequest.contains("规划"), userRequest.contains("目标"), userRequest.contains("学习"));
        
        if (response == null || response.trim().isEmpty()) {
            logger.warn("Request analysis returned empty result, checking keywords...");
            
            if (userRequest.contains("学习") || userRequest.contains("目标") || userRequest.contains("规划") || userRequest.contains("计划")) {
                logger.info("Matched keywords, returning NEED_GOAL_DECOMPOSITION");
                return "NEED_GOAL_DECOMPOSITION";
            } else if (userRequest.contains("总结") || userRequest.contains("回顾") || userRequest.contains("今日")) {
                logger.info("Matched keywords, returning NEED_SUMMARY");
                return "NEED_SUMMARY";
            } else if (userRequest.contains("明日") || userRequest.contains("明天")) {
                logger.info("Matched keywords, returning NEED_TOMORROW_PLAN");
                return "NEED_TOMORROW_PLAN";
            } else if (userRequest.contains("推荐") || userRequest.contains("建议")) {
                logger.info("Matched keywords, returning NEED_RECOMMENDATIONS");
                return "NEED_RECOMMENDATIONS";
            } else if (userRequest.contains("完成") || userRequest.contains("进度") || userRequest.contains("更新")) {
                logger.info("Matched keywords, returning NEED_PROGRESS_UPDATE");
                return "NEED_PROGRESS_UPDATE";
            }
            
            logger.warn("No keywords matched, defaulting to GENERAL");
            return "GENERAL";
        }
        
        String trimmedResponse = response.trim();
        
        if (trimmedResponse.contains("NEED_GOAL_DECOMPOSITION")) {
            return "NEED_GOAL_DECOMPOSITION";
        } else if (trimmedResponse.contains("NEED_TASK_SCHEDULING")) {
            return "NEED_TASK_SCHEDULING";
        } else if (trimmedResponse.contains("NEED_PROGRESS_UPDATE")) {
            return "NEED_PROGRESS_UPDATE";
        } else if (trimmedResponse.contains("NEED_PLAN_ADJUSTMENT")) {
            return "NEED_PLAN_ADJUSTMENT";
        } else if (trimmedResponse.contains("NEED_SUMMARY")) {
            return "NEED_SUMMARY";
        } else if (trimmedResponse.contains("NEED_TOMORROW_PLAN")) {
            return "NEED_TOMORROW_PLAN";
        } else if (trimmedResponse.contains("NEED_RECOMMENDATIONS")) {
            return "NEED_RECOMMENDATIONS";
        } else if (trimmedResponse.contains("NEED_GOAL_CREATION")) {
            return "NEED_GOAL_CREATION";
        } else if (trimmedResponse.contains("NEED_TODO_CREATION")) {
            return "NEED_TODO_CREATION";
        } else if (trimmedResponse.contains("GENERAL")) {
            return "GENERAL";
        }
        
        if (userRequest.contains("学习") || userRequest.contains("目标") || userRequest.contains("规划") || userRequest.contains("计划")) {
            return "NEED_GOAL_DECOMPOSITION";
        } else if (userRequest.contains("总结") || userRequest.contains("回顾") || userRequest.contains("今日")) {
            return "NEED_SUMMARY";
        } else if (userRequest.contains("明日") || userRequest.contains("明天")) {
            return "NEED_TOMORROW_PLAN";
        } else if (userRequest.contains("推荐") || userRequest.contains("建议")) {
            return "NEED_RECOMMENDATIONS";
        } else if (userRequest.contains("完成") || userRequest.contains("进度") || userRequest.contains("更新")) {
            return "NEED_PROGRESS_UPDATE";
        }
        
        return "GENERAL";
    }

    private String decomposeGoal(Long userId, String userRequest) {
        logger.info("Decomposing goal for user {}: {}", userId, userRequest);
        
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);
        List<MemoryGoal> existingGoals = memoryService.getGoals(userId);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个智能目标分解助手，请将用户的大目标分解为具体的、可执行的小任务。\n\n");
        promptBuilder.append("【用户信息】\n");
        for (MemoryAttribute attr : attributes.stream().filter(a -> "Person".equals(a.getCategory())).toList()) {
            promptBuilder.append("- ").append(attr.getEntity()).append(": ").append(attr.getValue()).append("\n");
        }
        
        promptBuilder.append("\n【技能能力】\n");
        for (MemoryAttribute attr : attributes.stream().filter(a -> "Skill".equals(a.getCategory())).toList()) {
            promptBuilder.append("- ").append(attr.getEntity()).append(": ").append(attr.getValue()).append("\n");
        }
        
        promptBuilder.append("\n【现有目标】\n");
        if (existingGoals.isEmpty()) {
            promptBuilder.append("暂无目标\n");
        } else {
            for (MemoryGoal goal : existingGoals) {
                promptBuilder.append("- ").append(goal.getTitle()).append(" (进度: ").append(goal.getProgress()).append("%)\n");
            }
        }
        
        promptBuilder.append("\n【用户目标】\n");
        promptBuilder.append(userRequest).append("\n\n");
        
        promptBuilder.append("请严格按照以下JSON格式输出分解结果：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"mainGoal\": \"主目标名称\",\n");
        promptBuilder.append("  \"description\": \"目标描述\",\n");
        promptBuilder.append("  \"priority\": \"高/中/低\",\n");
        promptBuilder.append("  \"deadline\": \"YYYY-MM-DD\",\n");
        promptBuilder.append("  \"subTasks\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"title\": \"子任务标题\",\n");
        promptBuilder.append("      \"description\": \"子任务描述\",\n");
        promptBuilder.append("      \"priority\": \"高/中/低\",\n");
        promptBuilder.append("      \"estimatedHours\": 2,\n");
        promptBuilder.append("      \"dependency\": \"依赖的子任务序号（可选）\",\n");
        promptBuilder.append("      \"suggestedDate\": \"建议完成日期\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"message\": \"给用户的说明\"\n");
        promptBuilder.append("}\n");

        String response = aiService.generateResponse(promptBuilder.toString());
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                String mainGoal = root.has("mainGoal") ? root.get("mainGoal").asText() : "";
                String description = root.has("description") ? root.get("description").asText() : "";
                String priority = root.has("priority") ? root.get("priority").asText() : "中";
                String deadline = root.has("deadline") ? root.get("deadline").asText() : "";
                
                if (!mainGoal.isEmpty()) {
                    memoryService.createGoal(
                        userId,
                        mainGoal,
                        description,
                        0,
                        mapPriorityToInt(priority),
                        deadline.isEmpty() ? null : LocalDate.parse(deadline),
                        "ACTIVE"
                    );
                    logger.info("Created main goal: {}", mainGoal);
                }
                
                if (root.has("subTasks")) {
                    for (JsonNode taskNode : root.get("subTasks")) {
                        String taskTitle = taskNode.has("title") ? taskNode.get("title").asText() : "";
                        String taskDesc = taskNode.has("description") ? taskNode.get("description").asText() : "";
                        String taskPriority = taskNode.has("priority") ? taskNode.get("priority").asText() : "中";
                        
                        if (!taskTitle.isEmpty()) {
                            memoryService.createTodo(
                                userId,
                                taskTitle,
                                mapPriorityToInt(taskPriority),
                                null
                            );
                            logger.info("Created subtask: {}", taskTitle);
                        }
                    }
                }
                
                String message = root.has("message") ? root.get("message").asText() : "";
                return message.isEmpty() ? "目标分解完成！已为您创建主目标和相关子任务。" : message;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse goal decomposition response", e);
        }
        
        return "目标分解完成！已为您创建相关目标和任务。";
    }

    private String scheduleTasks(Long userId, String userRequest) {
        logger.info("Scheduling tasks for user {}: {}", userId, userRequest);
        
        List<MemoryTodo> pendingTodos = memoryService.getTodos(userId).stream()
                .filter(t -> !t.getCompleted())
                .toList();
        List<MemoryGoal> activeGoals = memoryService.getGoals(userId).stream()
                .filter(g -> g.getProgress() < 100)
                .toList();
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个智能任务调度助手，请根据用户的待办事项和目标，合理安排任务的优先级和时间。\n\n");
        promptBuilder.append("【用户信息】\n");
        for (MemoryAttribute attr : attributes.stream().filter(a -> "Person".equals(a.getCategory())).toList()) {
            promptBuilder.append("- ").append(attr.getEntity()).append(": ").append(attr.getValue()).append("\n");
        }
        
        promptBuilder.append("\n【进行中目标】\n");
        if (activeGoals.isEmpty()) {
            promptBuilder.append("暂无进行中目标\n");
        } else {
            for (MemoryGoal goal : activeGoals) {
                promptBuilder.append("- ").append(goal.getTitle())
                          .append(" (进度: ").append(goal.getProgress()).append("%, ")
                          .append("优先级: ").append(goal.getPriority()).append(")\n");
            }
        }
        
        promptBuilder.append("\n【未完成待办】\n");
        if (pendingTodos.isEmpty()) {
            promptBuilder.append("暂无未完成待办\n");
        } else {
            for (int i = 0; i < pendingTodos.size(); i++) {
                MemoryTodo todo = pendingTodos.get(i);
                promptBuilder.append(i + 1).append(". ").append(todo.getTitle())
                          .append(" (优先级: ").append(todo.getPriority()).append(")\n");
            }
        }
        
        promptBuilder.append("\n【用户请求】\n");
        promptBuilder.append(userRequest).append("\n\n");
        
        promptBuilder.append("请严格按照以下JSON格式输出调度结果：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"schedule\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"taskIndex\": 1,\n");
        promptBuilder.append("      \"taskTitle\": \"任务名称\",\n");
        promptBuilder.append("      \"priority\": \"高/中/低\",\n");
        promptBuilder.append("      \"timeSlot\": \"建议时间（如：上午9-11点）\",\n");
        promptBuilder.append("      \"estimatedHours\": 2,\n");
        promptBuilder.append("      \"reason\": \"安排理由\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"message\": \"给用户的说明\"\n");
        promptBuilder.append("}\n");

        String response = aiService.generateResponse(promptBuilder.toString());
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                if (root.has("schedule")) {
                    for (JsonNode scheduleNode : root.get("schedule")) {
                        int taskIndex = scheduleNode.has("taskIndex") ? scheduleNode.get("taskIndex").asInt() : 0;
                        String newPriority = scheduleNode.has("priority") ? scheduleNode.get("priority").asText() : "";
                        
                        if (taskIndex > 0 && taskIndex <= pendingTodos.size() && !newPriority.isEmpty()) {
                            MemoryTodo todo = pendingTodos.get(taskIndex - 1);
                            memoryService.updateTodo(
                                userId,
                                todo.getId(),
                                null,
                                null,
                                mapPriorityToInt(newPriority),
                                null
                            );
                        }
                    }
                }
                
                String message = root.has("message") ? root.get("message").asText() : "";
                return message.isEmpty() ? "任务调度完成！已更新任务优先级。" : message;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse task scheduling response", e);
        }
        
        return "任务调度完成！";
    }

    private String updateProgress(Long userId, String userRequest) {
        logger.info("Updating progress for user {}: {}", userId, userRequest);
        
        List<MemoryGoal> goals = memoryService.getGoals(userId);
        List<MemoryTodo> todos = memoryService.getTodos(userId);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请分析用户的进度更新请求，并确定需要更新的目标或待办事项。\n\n");
        promptBuilder.append("【当前目标】\n");
        if (goals.isEmpty()) {
            promptBuilder.append("暂无目标\n");
        } else {
            for (int i = 0; i < goals.size(); i++) {
                MemoryGoal goal = goals.get(i);
                promptBuilder.append(i + 1).append(". ").append(goal.getTitle())
                          .append(" (进度: ").append(goal.getProgress()).append("%)\n");
            }
        }
        
        promptBuilder.append("\n【待办事项】\n");
        if (todos.isEmpty()) {
            promptBuilder.append("暂无待办\n");
        } else {
            for (int i = 0; i < todos.size(); i++) {
                MemoryTodo todo = todos.get(i);
                promptBuilder.append(i + 1).append(". ").append(todo.getTitle())
                          .append(" (完成: ").append(todo.getCompleted() ? "是" : "否").append(")\n");
            }
        }
        
        promptBuilder.append("\n【用户请求】\n");
        promptBuilder.append(userRequest).append("\n\n");
        
        promptBuilder.append("请严格按照以下JSON格式输出更新结果：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"goalUpdates\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"goalIndex\": 1,\n");
        promptBuilder.append("      \"newProgress\": 50\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"todoCompletions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"todoIndex\": 1,\n");
        promptBuilder.append("      \"completed\": true\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"message\": \"给用户的说明\"\n");
        promptBuilder.append("}\n");

        String response = aiService.generateResponse(promptBuilder.toString());
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                if (root.has("goalUpdates")) {
                    for (JsonNode updateNode : root.get("goalUpdates")) {
                        int goalIndex = updateNode.has("goalIndex") ? updateNode.get("goalIndex").asInt() : 0;
                        int newProgress = updateNode.has("newProgress") ? updateNode.get("newProgress").asInt() : 0;
                        
                        if (goalIndex > 0 && goalIndex <= goals.size()) {
                            MemoryGoal goal = goals.get(goalIndex - 1);
                            memoryService.updateGoal(
                                userId,
                                goal.getId(),
                                null,
                                null,
                                Math.min(100, Math.max(0, newProgress)),
                                null,
                                null,
                                null
                            );
                            logger.info("Updated goal progress: {} -> {}%", goal.getTitle(), Math.min(100, Math.max(0, newProgress)));
                        }
                    }
                }
                
                if (root.has("todoCompletions")) {
                    for (JsonNode completionNode : root.get("todoCompletions")) {
                        int todoIndex = completionNode.has("todoIndex") ? completionNode.get("todoIndex").asInt() : 0;
                        boolean completed = completionNode.has("completed") ? completionNode.get("completed").asBoolean() : false;
                        
                        if (todoIndex > 0 && todoIndex <= todos.size()) {
                            MemoryTodo todo = todos.get(todoIndex - 1);
                            memoryService.updateTodo(
                                userId,
                                todo.getId(),
                                null,
                                completed,
                                null,
                                null
                            );
                            logger.info("Updated todo status: {} -> {}", todo.getTitle(), completed ? "COMPLETED" : "PENDING");
                        }
                    }
                }
                
                String message = root.has("message") ? root.get("message").asText() : "";
                return message.isEmpty() ? "进度更新完成！" : message;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse progress update response", e);
        }
        
        return "进度更新完成！";
    }

    private String adjustPlan(Long userId, String userRequest) {
        logger.info("Adjusting plan for user {}: {}", userId, userRequest);
        
        List<MemoryGoal> goals = memoryService.getGoals(userId);
        List<MemoryTodo> todos = memoryService.getTodos(userId);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个智能计划调整助手，请根据用户的请求调整现有计划。\n\n");
        promptBuilder.append("【当前目标】\n");
        if (goals.isEmpty()) {
            promptBuilder.append("暂无目标\n");
        } else {
            for (MemoryGoal goal : goals) {
                promptBuilder.append("- ").append(goal.getTitle())
                          .append(" (进度: ").append(goal.getProgress()).append("%, ")
                          .append("优先级: ").append(goal.getPriority()).append(")\n");
            }
        }
        
        promptBuilder.append("\n【待办事项】\n");
        if (todos.isEmpty()) {
            promptBuilder.append("暂无待办\n");
        } else {
            for (MemoryTodo todo : todos) {
                promptBuilder.append("- ").append(todo.getTitle())
                          .append(" (完成: ").append(todo.getCompleted() ? "是" : "否").append(", ")
                          .append("优先级: ").append(todo.getPriority()).append(")\n");
            }
        }
        
        promptBuilder.append("\n【用户请求】\n");
        promptBuilder.append(userRequest).append("\n\n");
        
        promptBuilder.append("请严格按照以下JSON格式输出调整建议：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"adjustments\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"type\": \"goal/todo\",\n");
        promptBuilder.append("      \"title\": \"目标或待办名称\",\n");
        promptBuilder.append("      \"action\": \"update/priority/delete\",\n");
        promptBuilder.append("      \"newValue\": \"新值（如：新优先级、新进度等）\",\n");
        promptBuilder.append("      \"reason\": \"调整理由\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"newGoals\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"title\": \"新目标名称\",\n");
        promptBuilder.append("      \"description\": \"目标描述\",\n");
        promptBuilder.append("      \"priority\": \"高/中/低\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"newTodos\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"title\": \"新待办名称\",\n");
        promptBuilder.append("      \"description\": \"待办描述\",\n");
        promptBuilder.append("      \"priority\": \"高/中/低\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"message\": \"给用户的说明\"\n");
        promptBuilder.append("}\n");

        String response = aiService.generateResponse(promptBuilder.toString());
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                if (root.has("newGoals")) {
                    for (JsonNode goalNode : root.get("newGoals")) {
                        String title = goalNode.has("title") ? goalNode.get("title").asText() : "";
                        String description = goalNode.has("description") ? goalNode.get("description").asText() : "";
                        String priority = goalNode.has("priority") ? goalNode.get("priority").asText() : "中";
                        
                        if (!title.isEmpty()) {
                            memoryService.createGoal(
                                userId,
                                title,
                                description,
                                0,
                                mapPriorityToInt(priority),
                                null,
                                "ACTIVE"
                            );
                            logger.info("Created new goal: {}", title);
                        }
                    }
                }
                
                if (root.has("newTodos")) {
                    for (JsonNode todoNode : root.get("newTodos")) {
                        String title = todoNode.has("title") ? todoNode.get("title").asText() : "";
                        String description = todoNode.has("description") ? todoNode.get("description").asText() : "";
                        String priority = todoNode.has("priority") ? todoNode.get("priority").asText() : "中";
                        
                        if (!title.isEmpty()) {
                            memoryService.createTodo(
                                userId,
                                title,
                                mapPriorityToInt(priority),
                                null
                            );
                            logger.info("Created new todo: {}", title);
                        }
                    }
                }
                
                String message = root.has("message") ? root.get("message").asText() : "";
                return message.isEmpty() ? "计划调整完成！" : message;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse plan adjustment response", e);
        }
        
        return "计划调整完成！";
    }

    private String createGoal(Long userId, String userRequest) {
        logger.info("Creating goal for user {}: {}", userId, userRequest);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请从用户的请求中提取目标信息。\n\n");
        promptBuilder.append("【用户请求】\n");
        promptBuilder.append(userRequest).append("\n\n");
        
        promptBuilder.append("请严格按照以下JSON格式输出：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"title\": \"目标名称\",\n");
        promptBuilder.append("  \"description\": \"目标描述\",\n");
        promptBuilder.append("  \"priority\": \"高/中/低\",\n");
        promptBuilder.append("  \"deadline\": \"YYYY-MM-DD（可选）\"\n");
        promptBuilder.append("}\n");

        String response = aiService.generateResponse(promptBuilder.toString());
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                String title = root.has("title") ? root.get("title").asText() : "";
                String description = root.has("description") ? root.get("description").asText() : "";
                String priority = root.has("priority") ? root.get("priority").asText() : "中";
                String deadline = root.has("deadline") ? root.get("deadline").asText() : "";
                
                if (!title.isEmpty()) {
                    memoryService.createGoal(
                        userId,
                        title,
                        description,
                        0,
                        mapPriorityToInt(priority),
                        deadline.isEmpty() ? null : LocalDate.parse(deadline),
                        "ACTIVE"
                    );
                    logger.info("Created goal: {}", title);
                    return "目标创建成功！\n\n目标名称：" + title + "\n优先级：" + priority;
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse goal creation response", e);
        }
        
        return "目标创建成功！";
    }

    private String createTodo(Long userId, String userRequest) {
        logger.info("Creating todo for user {}: {}", userId, userRequest);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请从用户的请求中提取待办事项信息。\n\n");
        promptBuilder.append("【用户请求】\n");
        promptBuilder.append(userRequest).append("\n\n");
        
        promptBuilder.append("请严格按照以下JSON格式输出：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"title\": \"待办名称\",\n");
        promptBuilder.append("  \"description\": \"待办描述\",\n");
        promptBuilder.append("  \"priority\": \"高/中/低\"\n");
        promptBuilder.append("}\n");

        String response = aiService.generateResponse(promptBuilder.toString());
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                String title = root.has("title") ? root.get("title").asText() : "";
                String description = root.has("description") ? root.get("description").asText() : "";
                String priority = root.has("priority") ? root.get("priority").asText() : "中";
                
                if (!title.isEmpty()) {
                    memoryService.createTodo(
                        userId,
                        title,
                        mapPriorityToInt(priority),
                        null
                    );
                    logger.info("Created todo: {}", title);
                    return "待办创建成功！\n\n待办名称：" + title + "\n优先级：" + priority;
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse todo creation response", e);
        }
        
        return "待办创建成功！";
    }

    private String generateDailySummary(Long userId) {
        logger.info("Generating daily summary for user {}", userId);
        DailySummaryResponse summary = plannerService.generateSummary(userId);
        
        return String.format("📅 每日总结\n\n日期：%s\n\n总结：%s\n\n完成率：%.1f%%\n\n建议：%s",
            summary.getDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
            summary.getSummary(),
            summary.getCompletionRate(),
            summary.getSuggestions());
    }

    private String generateTomorrowPlan(Long userId) {
        logger.info("Generating tomorrow plan for user {}", userId);
        TomorrowPlanResponse plan = plannerService.generateTomorrowPlan(userId);
        
        StringBuilder result = new StringBuilder();
        result.append("📋 明日计划\n\n");
        result.append("日期：").append(plan.getDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))).append("\n\n");
        result.append("概述：").append(plan.getOverview()).append("\n\n");
        
        if (plan.getFocusAreas() != null && !plan.getFocusAreas().isEmpty()) {
            result.append("重点领域：\n");
            for (String area : plan.getFocusAreas()) {
                result.append("• ").append(area).append("\n");
            }
            result.append("\n");
        }
        
        if (plan.getTasks() != null && !plan.getTasks().isEmpty()) {
            result.append("任务安排：\n");
            for (int i = 0; i < plan.getTasks().size(); i++) {
                TomorrowPlanResponse.Task task = plan.getTasks().get(i);
                result.append(i + 1).append(". ").append(task.getTitle())
                      .append(" (").append(task.getPriority()).append(")")
                      .append(" - ").append(task.getTimeSlot()).append("\n");
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    result.append("   ").append(task.getDescription()).append("\n");
                }
            }
            result.append("\n");
        }
        
        if (plan.getSuggestions() != null && !plan.getSuggestions().isEmpty()) {
            result.append("建议：").append(plan.getSuggestions()).append("\n");
        }
        
        return result.toString();
    }

    private String generateRecommendations(Long userId) {
        logger.info("Generating task recommendations for user {}", userId);
        TaskRecommendationResponse recommendations = plannerService.generateTaskRecommendations(userId);
        
        StringBuilder result = new StringBuilder();
        result.append("💡 任务推荐\n\n");
        
        if (recommendations.getRecommendations() != null && !recommendations.getRecommendations().isEmpty()) {
            for (TaskRecommendationResponse.Recommendation rec : recommendations.getRecommendations()) {
                result.append(rec.getRank()).append(". ").append(rec.getTask()).append("\n");
                result.append("   优先级：").append(rec.getPriority()).append("\n");
                result.append("   预计时间：").append(rec.getEstimatedHours()).append("小时\n");
                result.append("   推荐理由：").append(rec.getReason()).append("\n\n");
            }
        }
        
        if (recommendations.getMessage() != null && !recommendations.getMessage().isEmpty()) {
            result.append(recommendations.getMessage()).append("\n");
        }
        
        return result.toString();
    }

    private String respondToGeneralRequest(Long userId, String userRequest) {
        logger.info("Responding to general request for user {}: {}", userId, userRequest);
        
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个智能规划助手，请回答用户的问题。\n\n");
        promptBuilder.append("【用户信息】\n");
        for (MemoryAttribute attr : attributes) {
            promptBuilder.append("- ").append(attr.getCategory()).append(" | ").append(attr.getEntity())
                      .append(" | ").append(attr.getAttribute()).append(" | ").append(attr.getValue()).append("\n");
        }
        
        promptBuilder.append("\n【用户问题】\n");
        promptBuilder.append(userRequest).append("\n\n");
        promptBuilder.append("请用自然友好的语言回答用户的问题。");

        return aiService.generateResponse(promptBuilder.toString());
    }

    private Integer mapPriorityToInt(String priority) {
        return switch (priority) {
            case "高", "high", "HIGH", "High" -> 2;
            case "低", "low", "LOW", "Low" -> 0;
            default -> 1;
        };
    }
}
