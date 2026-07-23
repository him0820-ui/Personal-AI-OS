package com.personalai.os.service;

import com.personalai.os.dto.response.DailySummaryResponse;
import com.personalai.os.dto.response.TomorrowPlanResponse;
import com.personalai.os.dto.response.TaskRecommendationResponse;
import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 规划服务类，生成每日总结、明日计划和任务推荐
 * @author: 琦
 */
@Service
public class PlannerService {

    private static final Logger logger = LoggerFactory.getLogger(PlannerService.class);

    @Autowired
    private AiService aiService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private MemoryAttributeService memoryAttributeService;

    @Autowired
    private ObjectMapper objectMapper;

    public DailySummaryResponse generateSummary(Long userId) {
        List<MemoryGoal> goals = memoryService.getGoals(userId);
        List<MemoryTodo> todos = memoryService.getTodos(userId);
        List<MemoryTimeline> timeline = memoryService.getTimeline(userId);
        List<MemoryFact> facts = memoryService.getFacts(userId);

        LocalDate today = LocalDate.now();
        
        List<MemoryTimeline> todayEvents = timeline.stream()
                .filter(t -> t.getTimestamp() != null && t.getTimestamp().toLocalDate().equals(today))
                .toList();

        long completedTodos = todos.stream().filter(MemoryTodo::getCompleted).count();
        long totalTodos = todos.size();
        double completionRate = totalTodos > 0 ? (double) completedTodos / totalTodos * 100 : 0;

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请根据以下信息生成用户的每日总结：\n\n");
        
        promptBuilder.append("【今日事件】\n");
        if (todayEvents.isEmpty()) {
            promptBuilder.append("今日暂无记录事件\n");
        } else {
            for (MemoryTimeline event : todayEvents) {
                promptBuilder.append("- ").append(event.getTitle()).append("\n");
            }
        }
        
        promptBuilder.append("\n【目标完成情况】\n");
        if (goals.isEmpty()) {
            promptBuilder.append("暂无目标\n");
        } else {
            for (MemoryGoal goal : goals) {
                promptBuilder.append("- ").append(goal.getTitle())
                          .append(": ").append(goal.getProgress()).append("%\n");
            }
        }
        
        promptBuilder.append("\n【待办完成情况】\n");
        promptBuilder.append("已完成: ").append(completedTodos).append("/").append(totalTodos)
                  .append(" (完成率: ").append(String.format("%.1f", completionRate)).append("%)\n");
        
        promptBuilder.append("\n【用户信息】\n");
        for (MemoryFact fact : facts) {
            promptBuilder.append("- ").append(fact.getKey()).append(": ").append(fact.getValue()).append("\n");
        }
        
        promptBuilder.append("\n请输出：\n");
        promptBuilder.append("1. 今日总结（50字以内）\n");
        promptBuilder.append("2. 明日建议（50字以内）\n");
        promptBuilder.append("格式：\n总结：xxx\n建议：xxx");
        
        String response = aiService.generateResponse(promptBuilder.toString());
        
        String summary = extractPart(response, "总结：", "建议：");
        String suggestions = extractPart(response, "建议：", null);
        
        if (summary.isEmpty() && suggestions.isEmpty()) {
            summary = "今日你专注于个人成长和认知管理，继续保持！";
            suggestions = "建议继续推进当前目标，保持良好的学习节奏。";
        }
        
        DailySummaryResponse result = new DailySummaryResponse();
        result.setDate(today);
        result.setSummary(summary);
        result.setCompletionRate(completionRate);
        result.setSuggestions(suggestions);
        
        return result;
    }

    public TomorrowPlanResponse generateTomorrowPlan(Long userId) {
        List<MemoryGoal> goals = memoryService.getGoals(userId);
        List<MemoryTodo> todos = memoryService.getTodos(userId);
        List<MemoryTimeline> timeline = memoryService.getTimeline(userId);
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        
        List<MemoryTimeline> tomorrowEvents = timeline.stream()
                .filter(t -> t.getTimestamp() != null && t.getTimestamp().toLocalDate().equals(tomorrow))
                .toList();

        List<MemoryTodo> pendingTodos = todos.stream()
                .filter(t -> !t.getCompleted())
                .toList();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个智能规划助手，请根据用户的目标、待办和时间线，生成明日计划。\n\n");
        
        promptBuilder.append("【用户信息】\n");
        List<MemoryAttribute> personAttrs = attributes.stream()
                .filter(a -> "Person".equals(a.getCategory()))
                .toList();
        for (MemoryAttribute attr : personAttrs) {
            promptBuilder.append("- ").append(attr.getEntity()).append(": ").append(attr.getValue()).append("\n");
        }
        
        promptBuilder.append("\n【当前目标】\n");
        if (goals.isEmpty()) {
            promptBuilder.append("暂无目标\n");
        } else {
            for (MemoryGoal goal : goals) {
                String status = goal.getProgress() >= 100 ? "已完成" : "进行中";
                promptBuilder.append("- ").append(goal.getTitle())
                          .append(" (进度: ").append(goal.getProgress()).append("%, ")
                          .append("优先级: ").append(goal.getPriority()).append(", ")
                          .append("截止: ").append(formatDate(goal.getDeadline())).append(")\n");
            }
        }
        
        promptBuilder.append("\n【未完成待办】\n");
        if (pendingTodos.isEmpty()) {
            promptBuilder.append("暂无未完成待办\n");
        } else {
            for (MemoryTodo todo : pendingTodos) {
                promptBuilder.append("- ").append(todo.getTitle())
                          .append(" (优先级: ").append(todo.getPriority()).append(")\n");
            }
        }
        
        promptBuilder.append("\n【明日已安排事件】\n");
        if (tomorrowEvents.isEmpty()) {
            promptBuilder.append("明日暂无安排\n");
        } else {
            for (MemoryTimeline event : tomorrowEvents) {
                promptBuilder.append("- ").append(event.getTitle())
                          .append(" (时间: ").append(formatDateTime(event.getTimestamp())).append(")\n");
            }
        }

        promptBuilder.append("\n【偏好与技能】\n");
        List<MemoryAttribute> prefAttrs = attributes.stream()
                .filter(a -> "Preference".equals(a.getCategory()))
                .toList();
        for (MemoryAttribute attr : prefAttrs) {
            promptBuilder.append("- ").append(attr.getEntity()).append(": ").append(attr.getValue()).append("\n");
        }
        
        promptBuilder.append("\n请严格按照以下JSON格式输出明日计划：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"date\": \"").append(tomorrow.format(DateTimeFormatter.ISO_DATE)).append("\",\n");
        promptBuilder.append("  \"overview\": \"明日计划概述（50字以内）\",\n");
        promptBuilder.append("  \"focusAreas\": [\"重点领域1\", \"重点领域2\", \"重点领域3\"],\n");
        promptBuilder.append("  \"tasks\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"title\": \"任务标题\",\n");
        promptBuilder.append("      \"priority\": \"高/中/低\",\n");
        promptBuilder.append("      \"estimatedHours\": 2,\n");
        promptBuilder.append("      \"relatedGoal\": \"关联目标（可选）\",\n");
        promptBuilder.append("      \"description\": \"任务描述\",\n");
        promptBuilder.append("      \"timeSlot\": \"建议时间（如：上午9-11点）\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"suggestions\": \"额外建议（30字以内）\"\n");
        promptBuilder.append("}\n");
        
        String response = aiService.generateResponse(promptBuilder.toString());
        
        return parseTomorrowPlanResponse(response, tomorrow);
    }

    public TaskRecommendationResponse generateTaskRecommendations(Long userId) {
        List<MemoryGoal> goals = memoryService.getGoals(userId);
        List<MemoryTodo> todos = memoryService.getTodos(userId);
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);

        List<MemoryTodo> pendingTodos = todos.stream()
                .filter(t -> !t.getCompleted())
                .toList();

        List<MemoryGoal> activeGoals = goals.stream()
                .filter(g -> g.getProgress() < 100)
                .toList();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是一个智能任务推荐助手，请根据用户的目标和待办，推荐下一步应该做的任务。\n\n");
        
        promptBuilder.append("【用户信息】\n");
        List<MemoryAttribute> personAttrs = attributes.stream()
                .filter(a -> "Person".equals(a.getCategory()))
                .toList();
        for (MemoryAttribute attr : personAttrs) {
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
            for (MemoryTodo todo : pendingTodos) {
                promptBuilder.append("- ").append(todo.getTitle())
                          .append(" (优先级: ").append(todo.getPriority()).append(")\n");
            }
        }
        
        promptBuilder.append("\n【技能能力】\n");
        List<MemoryAttribute> skillAttrs = attributes.stream()
                .filter(a -> "Skill".equals(a.getCategory()))
                .toList();
        for (MemoryAttribute attr : skillAttrs) {
            promptBuilder.append("- ").append(attr.getEntity()).append(": ").append(attr.getValue()).append("\n");
        }
        
        promptBuilder.append("\n请严格按照以下JSON格式输出任务推荐：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"recommendations\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"rank\": 1,\n");
        promptBuilder.append("      \"task\": \"推荐任务描述\",\n");
        promptBuilder.append("      \"reason\": \"推荐理由\",\n");
        promptBuilder.append("      \"priority\": \"高/中/低\",\n");
        promptBuilder.append("      \"estimatedHours\": 1,\n");
        promptBuilder.append("      \"relatedGoal\": \"关联目标\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ],\n");
        promptBuilder.append("  \"totalTasks\": 3,\n");
        promptBuilder.append("  \"message\": \"给用户的鼓励语\"\n");
        promptBuilder.append("}\n");
        
        String response = aiService.generateResponse(promptBuilder.toString());
        
        return parseTaskRecommendationResponse(response);
    }

    private TomorrowPlanResponse parseTomorrowPlanResponse(String response, LocalDate date) {
        TomorrowPlanResponse result = new TomorrowPlanResponse();
        result.setDate(date);
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                if (root.has("overview")) {
                    result.setOverview(root.get("overview").asText());
                }
                
                if (root.has("focusAreas")) {
                    List<String> focusAreas = new ArrayList<>();
                    for (JsonNode area : root.get("focusAreas")) {
                        focusAreas.add(area.asText());
                    }
                    result.setFocusAreas(focusAreas);
                }
                
                if (root.has("tasks")) {
                    List<TomorrowPlanResponse.Task> tasks = new ArrayList<>();
                    for (JsonNode taskNode : root.get("tasks")) {
                        TomorrowPlanResponse.Task task = new TomorrowPlanResponse.Task();
                        if (taskNode.has("title")) task.setTitle(taskNode.get("title").asText());
                        if (taskNode.has("priority")) task.setPriority(taskNode.get("priority").asText());
                        if (taskNode.has("estimatedHours")) task.setEstimatedHours(taskNode.get("estimatedHours").asInt());
                        if (taskNode.has("relatedGoal")) task.setRelatedGoal(taskNode.get("relatedGoal").asText());
                        if (taskNode.has("description")) task.setDescription(taskNode.get("description").asText());
                        if (taskNode.has("timeSlot")) task.setTimeSlot(taskNode.get("timeSlot").asText());
                        tasks.add(task);
                    }
                    result.setTasks(tasks);
                }
                
                if (root.has("suggestions")) {
                    result.setSuggestions(root.get("suggestions").asText());
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse tomorrow plan response", e);
        }
        
        if (result.getOverview() == null || result.getOverview().isEmpty()) {
            result.setOverview("继续推进当前目标，保持良好的学习节奏");
        }
        
        if (result.getTasks() == null || result.getTasks().isEmpty()) {
            List<TomorrowPlanResponse.Task> defaultTasks = new ArrayList<>();
            TomorrowPlanResponse.Task task = new TomorrowPlanResponse.Task();
            task.setTitle("回顾今日进展");
            task.setPriority("中");
            task.setEstimatedHours(1);
            task.setDescription("整理今日完成的任务，规划明日工作");
            task.setTimeSlot("上午");
            defaultTasks.add(task);
            result.setTasks(defaultTasks);
        }
        
        return result;
    }

    private TaskRecommendationResponse parseTaskRecommendationResponse(String response) {
        TaskRecommendationResponse result = new TaskRecommendationResponse();
        
        try {
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            
            if (jsonStart != -1 && jsonEnd != -1) {
                String jsonString = response.substring(jsonStart, jsonEnd + 1);
                JsonNode root = objectMapper.readTree(jsonString);
                
                if (root.has("recommendations")) {
                    List<TaskRecommendationResponse.Recommendation> recommendations = new ArrayList<>();
                    for (JsonNode recNode : root.get("recommendations")) {
                        TaskRecommendationResponse.Recommendation rec = new TaskRecommendationResponse.Recommendation();
                        if (recNode.has("rank")) rec.setRank(recNode.get("rank").asInt());
                        if (recNode.has("task")) rec.setTask(recNode.get("task").asText());
                        if (recNode.has("reason")) rec.setReason(recNode.get("reason").asText());
                        if (recNode.has("priority")) rec.setPriority(recNode.get("priority").asText());
                        if (recNode.has("estimatedHours")) rec.setEstimatedHours(recNode.get("estimatedHours").asInt());
                        if (recNode.has("relatedGoal")) rec.setRelatedGoal(recNode.get("relatedGoal").asText());
                        recommendations.add(rec);
                    }
                    result.setRecommendations(recommendations);
                }
                
                if (root.has("totalTasks")) {
                    result.setTotalTasks(root.get("totalTasks").asInt());
                }
                
                if (root.has("message")) {
                    result.setMessage(root.get("message").asText());
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse task recommendation response", e);
        }
        
        if (result.getRecommendations() == null || result.getRecommendations().isEmpty()) {
            List<TaskRecommendationResponse.Recommendation> defaultRecs = new ArrayList<>();
            TaskRecommendationResponse.Recommendation rec = new TaskRecommendationResponse.Recommendation();
            rec.setRank(1);
            rec.setTask("查看并更新待办列表");
            rec.setReason("保持待办列表的时效性有助于更好地规划时间");
            rec.setPriority("中");
            rec.setEstimatedHours(1);
            defaultRecs.add(rec);
            result.setRecommendations(defaultRecs);
            result.setTotalTasks(1);
        }
        
        if (result.getMessage() == null || result.getMessage().isEmpty()) {
            result.setMessage("继续加油，你正在朝着目标前进！");
        }
        
        return result;
    }

    private String extractPart(String text, String startTag, String endTag) {
        int start = text.indexOf(startTag);
        if (start == -1) {
            return text;
        }
        start += startTag.length();
        
        if (endTag != null) {
            int end = text.indexOf(endTag, start);
            if (end != -1) {
                return text.substring(start, end).trim();
            }
        }
        
        return text.substring(start).trim();
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "无";
        return date.format(DateTimeFormatter.ISO_DATE);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "无";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}