package com.personalai.os.service;

import com.personalai.os.config.ToolCallingConfig;
import com.personalai.os.entity.Conversation;
import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.event.ChatFinishedEvent;
import com.personalai.os.mapper.ConversationMapper;
import com.personalai.os.memory.ProgressUpdateService;
import com.personalai.os.memory.ReminderService;
import com.personalai.os.service.AiService.ToolCall;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import com.personalai.os.service.VectorStoreService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @description: 聊天服务类，处理对话管理、上下文构建、工具调用和流式响应
 * @author: 琦
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private AiService aiService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ToolCallingConfig toolCallingConfig;

    @Autowired
    private ProgressUpdateService progressUpdateService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private MemorySummaryService memorySummaryService;

    @Autowired
    private MemoryImportanceService memoryImportanceService;

    @Autowired
    private MemoryAttributeService memoryAttributeService;

    private static final int CONTEXT_WINDOW = 10;
    private static final int CACHE_TTL_HOURS = 1;
    private static final int MAX_TOOL_CALLS = 1;
    private static final int TOP_K_MEMORIES = 5;

    /** 兜底转换器：当缓存元素因缺少类型信息退化为 LinkedHashMap 时，把它转回 Conversation，避免对话历史再次被静默丢弃。 */
    private static final ObjectMapper CONV_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public void saveUserMessage(Long userId, Long sessionId, String content) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setContent(content);
        conversation.setSender("user");
        conversation.setType("TEXT");
        conversation.setCreatedAt(LocalDateTime.now());

        conversationMapper.insert(conversation);

        updateCache(sessionId);
        chatSessionService.updateSessionTime(sessionId);
    }

    public void saveAiMessage(Long userId, Long sessionId, String content) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setContent(content);
        conversation.setSender("ai");
        conversation.setType("TEXT");
        conversation.setCreatedAt(LocalDateTime.now());

        conversationMapper.insert(conversation);

        updateCache(sessionId);
        chatSessionService.updateSessionTime(sessionId);

        List<Conversation> recent = conversationMapper.findRecentBySessionId(sessionId, CONTEXT_WINDOW);
        logger.info("Publishing ChatFinishedEvent for user {}, session {}, {} messages", userId, sessionId, recent.size());
        eventPublisher.publishEvent(new ChatFinishedEvent(this, userId, convertToChatMessages(recent)));
        logger.info("ChatFinishedEvent published");
    }

    public void saveAiMessage(Long userId, Long sessionId, String content, String think) {
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setContent(content);
        conversation.setThink(think);
        conversation.setSender("ai");
        conversation.setType("TEXT");
        conversation.setCreatedAt(LocalDateTime.now());

        conversationMapper.insert(conversation);
        logger.info("AI message saved to database, id: {}", conversation.getId());

        updateCache(sessionId);
        chatSessionService.updateSessionTime(sessionId);

        List<Conversation> recent = conversationMapper.findRecentBySessionId(sessionId, CONTEXT_WINDOW);
        logger.info("Publishing ChatFinishedEvent for user {}, session {}, {} messages", userId, sessionId, recent.size());
        
        List<com.personalai.os.entity.ChatMessage> chatMessages = convertToChatMessages(recent);
        logger.info("Converted {} messages to ChatMessage format", chatMessages.size());
        
        eventPublisher.publishEvent(new ChatFinishedEvent(this, userId, chatMessages));
        logger.info("ChatFinishedEvent published successfully");
    }

    public List<Conversation> getRecentMessages(Long sessionId) {
        String cacheKey = "conversation:recent:" + sessionId;

        @SuppressWarnings("unchecked")
        List<Conversation> cached = (List<Conversation>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            logger.info("Cache hit for session {}, {} messages", sessionId, cached.size());
            return cached;
        }

        logger.info("Cache miss for session {}", sessionId);
        List<Conversation> recent = conversationMapper.findRecentBySessionId(sessionId, CONTEXT_WINDOW);
        logger.info("DB query returned {} messages for session {}", recent.size(), sessionId);
        redisTemplate.opsForValue().set(cacheKey, recent, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return recent;
    }

    public List<Conversation> getAllMessages(Long sessionId) {
        return conversationMapper.findBySessionIdOrderByCreatedAt(sessionId);
    }

    public String buildPrompt(Long userId, Long sessionId, String userMessage) {
        return buildPrompt(userId, sessionId, userMessage, null);
    }

    public String buildPrompt(Long userId, Long sessionId, String userMessage, String toolResult) {
        List<Conversation> recentMessages = getRecentMessages(sessionId);
        
        List<VectorStoreService.SearchResult> similarMemories = vectorStoreService.search(userId, userMessage, TOP_K_MEMORIES);
        
        List<MemoryAttribute> personAttrs = memoryAttributeService.getAttributesByCategory(userId, "Person");
        List<MemoryAttribute> preferenceAttrs = memoryAttributeService.getAttributesByCategory(userId, "Preference");
        List<MemoryAttribute> skillAttrs = memoryAttributeService.getAttributesByCategory(userId, "Skill");
        List<MemoryAttribute> goalAttrs = memoryAttributeService.getAttributesByCategory(userId, "Goal");
        List<MemoryAttribute> todoAttrs = memoryAttributeService.getAttributesByCategory(userId, "Todo");
        List<MemoryAttribute> timelineAttrs = memoryAttributeService.getAttributesByCategory(userId, "Timeline");

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你叫小小琦，是一个由张家界学院学生唐琦开发的个人AI助手。\n");
        promptBuilder.append("你帮助用户管理长期记忆、目标规划和成长轨迹。\n\n");

        promptBuilder.append("【系统时间】\n");
        promptBuilder.append("当前时间：").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        promptBuilder.append("时区：北京时间（UTC+8），以上时间已经是北京时间，不需要再加时区偏移\n\n");

        promptBuilder.append("【上下文理解规则】\n");
        promptBuilder.append("1. 对话历史优先级最高：当用户的问题简短或不完整时，必须优先参考对话历史来理解上下文。\n");
        promptBuilder.append("2. 长期记忆其次：只有当对话历史无法提供足够上下文时，才参考用户的长期记忆。\n");
        promptBuilder.append("3. 示例：如果之前讨论的是原子弹，用户问\"谁投放的\"，应该理解为\"谁投放了第一颗原子弹\"。\n");
        promptBuilder.append("4. 如果之前讨论的是原子弹，用户问\"飞行员是谁\"，应该理解为\"投放第一颗原子弹的飞行员是谁\"。\n\n");

        promptBuilder.append("【核心原则】\n");
        promptBuilder.append("1. 用户的问题是第一位的，必须优先回答用户的问题。\n");
        promptBuilder.append("2. 对于一般性知识问题（如历史、技术、科学等），直接回答，不要强行关联到用户的学习目标。\n");
        promptBuilder.append("3. 只有当用户明确询问或需要时，才主动提及用户的学习目标。\n\n");
        
        if (toolResult != null && !toolResult.isEmpty()) {
            promptBuilder.append("【工具执行结果】\n");
            promptBuilder.append(toolResult).append("\n\n");

            promptBuilder.append("请根据以上工具执行结果，直接用自然语言回答用户问题。不要再输出工具调用标签。\n\n");
        } else {
            promptBuilder.append("【工具调用说明】\n");
            promptBuilder.append("你是一个工具调用助手。当用户请求以下操作时，**必须**输出工具调用标签，绝对不能直接回答：\n");
            promptBuilder.append("=== 需要调用工具的场景 ===\n");
            promptBuilder.append("- 用户请求设置提醒：任何包含\"提醒\"、\"记得\"、\"记一下\"、\"到时候提醒\"等关键词的请求\n");
            promptBuilder.append("  → **必须**调用add_reminder工具\n");
            promptBuilder.append("  → 参数：title（标题，简短描述）、description（描述，详细说明）、time（时间，必须使用相对时间，如'1分钟后'、'3小时后'、'2天后'）\n");
            promptBuilder.append("- 用户询问\"我有哪些目标\"或\"我的目标\"→调用get_goals\n");
            promptBuilder.append("- 用户询问\"我有哪些记忆\"或\"我的个人信息\"→调用get_memory\n");
            promptBuilder.append("- 用户询问\"时间线\"或\"成长轨迹\"→调用get_timeline\n");
            promptBuilder.append("- 用户询问\"待办事项\"或\"我的任务\"→调用get_todos\n");
            promptBuilder.append("- 用户请求完成任务（如\"我已完成XXX\"、\"XXX已做完\"）→调用complete_todo\n");
            promptBuilder.append("- 用户请求删除任务（如\"删除XXX\"、\"移除XXX\"）→调用delete_todo\n");
            promptBuilder.append("- 用户询问\"提醒\"或\"待办提醒\"→调用get_reminders\n");
            promptBuilder.append("\n");
            promptBuilder.append("=== 工具调用格式 ===\n");
            promptBuilder.append("工具调用格式：<tool>{\"name\": \"工具名\", \"arguments\": {}}</tool>\n");
            promptBuilder.append("示例：\n");
            promptBuilder.append("用户：记得提醒我1分钟后测试\n");
            promptBuilder.append("正确回复：<tool>{\"name\": \"add_reminder\", \"arguments\": {\"title\": \"测试提醒\", \"description\": \"1分钟后测试提醒功能\", \"time\": \"1分钟后\"}}</tool>\n");
            promptBuilder.append("\n");
            promptBuilder.append("=== 绝对禁止 ===\n");
            promptBuilder.append("❌ 禁止直接回答用户的提醒请求，必须调用add_reminder工具\n");
            promptBuilder.append("❌ 禁止使用绝对日期时间格式，必须使用相对时间（如'1分钟后'、'2小时后'）\n");
            promptBuilder.append("❌ 禁止编造提醒时间，必须使用用户指定的相对时间\n");
            promptBuilder.append("\n");
            promptBuilder.append("=== 直接回答的场景 ===\n");
            promptBuilder.append("如果用户只是陈述信息（如\"我改名字了\"、\"我学会了XXX\"），请直接回应，不需要调用工具。\n");
            promptBuilder.append("对于一般性问题（如技术、知识、概念等），直接回答，不要调用工具。\n");
        }

        String userSummary = getUserSummary(userId);
        if (userSummary != null && !userSummary.isEmpty()) {
            promptBuilder.append("【用户画像】\n");
            promptBuilder.append(userSummary).append("\n\n");
        }

        if (recentMessages != null && !recentMessages.isEmpty()) {
            // 缓存/DB 查询返回的是“最新在前”(DESC)，反转成正序(最旧在前)，
            // 让 LLM 看到的对话历史时间顺序与真实一致。
            List<Conversation> orderedMessages = new ArrayList<>(recentMessages);
            Collections.reverse(orderedMessages);

            promptBuilder.append("【对话历史】\n");
            int size = orderedMessages.size();
            for (int i = 0; i < size; i++) {
                Conversation msg = toConversation(orderedMessages.get(i));
                if (msg == null) {
                    continue;
                }
                // 跳过刚保存的当前用户消息：saveUserMessage 已把它写入缓存，
                // 它会在 prompt 末尾以 “user: <消息>” 单独拼接，这里不再重复。
                if (i == size - 1
                        && "user".equals(msg.getSender())
                        && userMessage != null
                        && userMessage.equals(msg.getContent())) {
                    continue;
                }
                promptBuilder.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
            }
            promptBuilder.append("\n");
        }

        if (!similarMemories.isEmpty()) {
            promptBuilder.append("【相关记忆（语义检索）】\n");
            for (VectorStoreService.SearchResult memory : similarMemories) {
                promptBuilder.append("- ").append(memory.content())
                          .append(" (类型: ").append(memory.memoryType())
                          .append(", 相似度: ").append(String.format("%.2f", memory.score())).append(")\n");
            }
            promptBuilder.append("\n");
        }

        if (!personAttrs.isEmpty()) {
            promptBuilder.append("【个人信息】\n");
            for (MemoryAttribute attr : personAttrs) {
                promptBuilder.append("- ").append(attr.getEntity())
                          .append(": ").append(attr.getValue())
                          .append(" (重要性: ").append(attr.getImportance() != null ? attr.getImportance() : 50).append(")\n");
            }
            promptBuilder.append("\n");
        }

        if (!preferenceAttrs.isEmpty()) {
            promptBuilder.append("【兴趣偏好】\n");
            for (MemoryAttribute attr : preferenceAttrs) {
                promptBuilder.append("- ").append(attr.getEntity())
                          .append(": ").append(attr.getValue())
                          .append(" (重要性: ").append(attr.getImportance() != null ? attr.getImportance() : 50).append(")\n");
            }
            promptBuilder.append("\n");
        }

        if (!skillAttrs.isEmpty()) {
            promptBuilder.append("【技能能力】\n");
            for (MemoryAttribute attr : skillAttrs) {
                promptBuilder.append("- ").append(attr.getEntity())
                          .append(": ").append(attr.getValue())
                          .append(" (重要性: ").append(attr.getImportance() != null ? attr.getImportance() : 50).append(")\n");
            }
            promptBuilder.append("\n");
        }

        if (!goalAttrs.isEmpty()) {
            promptBuilder.append("【用户当前目标】\n");
            for (MemoryAttribute attr : goalAttrs) {
                promptBuilder.append("- ").append(attr.getEntity())
                          .append(": ").append(attr.getValue())
                          .append(" (重要性: ").append(attr.getImportance() != null ? attr.getImportance() : 50).append(")\n");
            }
            promptBuilder.append("\n");
        }

        if (!todoAttrs.isEmpty()) {
            promptBuilder.append("【待办事项】\n");
            for (MemoryAttribute attr : todoAttrs) {
                promptBuilder.append("- [ ] ").append(attr.getEntity())
                          .append(": ").append(attr.getValue())
                          .append(" (重要性: ").append(attr.getImportance() != null ? attr.getImportance() : 50).append(")\n");
            }
            promptBuilder.append("\n");
        }

        if (!timelineAttrs.isEmpty()) {
            promptBuilder.append("【成长轨迹】\n");
            for (MemoryAttribute attr : timelineAttrs) {
                promptBuilder.append("- ").append(attr.getEntity());
                if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                    promptBuilder.append(" - ").append(attr.getValue());
                }
                promptBuilder.append("\n");
            }
            promptBuilder.append("\n");
        }

        promptBuilder.append("user: ").append(userMessage);

        return promptBuilder.toString();
    }

    private void updateCache(Long sessionId) {
        String cacheKey = "conversation:recent:" + sessionId;
        List<Conversation> recent = conversationMapper.findRecentBySessionId(sessionId, CONTEXT_WINDOW);
        redisTemplate.opsForValue().set(cacheKey, recent, CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    public AiService.ThinkContent processWithToolCalling(Long userId, Long sessionId, String userMessage) {
        String currentUserMessage = userMessage;
        String toolResult = null;
        int toolCallCount = 0;

        List<MemoryAttribute> allAttributes = memoryAttributeService.getAttributesByUserId(userId);

        while (toolCallCount < MAX_TOOL_CALLS) {
            String prompt = buildPrompt(userId, sessionId, currentUserMessage, toolResult);
            AiService.ThinkContent thinkContent = aiService.generateResponseWithThinking(prompt);

            String response = thinkContent.content();
            logger.info("LLM response {} chars (think {} chars), contains tool call: {}",
                response != null ? response.length() : 0,
                thinkContent.think() != null ? thinkContent.think().length() : 0,
                aiService.containsToolCall(response));

            if (aiService.containsToolCall(response)) {
                ToolCall toolCall = aiService.parseToolCall(response);
                if (toolCall != null && toolCall.name() != null) {
                    logger.info("Executing tool call: {} with args: {}", toolCall.name(), toolCall.arguments());
                    
                    Map<String, Object> args = null;
                    if (toolCall.arguments() != null && !toolCall.arguments().isEmpty() && !"{}".equals(toolCall.arguments())) {
                        try {
                            args = aiService.getObjectMapper().readValue(toolCall.arguments(), 
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                        } catch (Exception e) {
                            logger.warn("Failed to parse tool arguments: {}", e.getMessage());
                        }
                    }
                    
                    String toolOutput = toolCallingConfig.callTool(toolCall.name(), userId, args);
                    if (toolOutput.startsWith("Unknown tool")) {
                        logger.warn("Unknown tool called: {}, cleaning tool tags from response", toolCall.name());
                        String cleanedResponse = aiService.removeToolCall(response);
                        progressUpdateService.analyzeConversationAndUpdate(userId, userMessage);
                        memoryImportanceService.processAttributeImportanceFeedback(userId, sessionId, allAttributes, cleanedResponse);
                        return new AiService.ThinkContent(thinkContent.think(), cleanedResponse);
                    }
                    
                    toolResult = "工具: " + toolCall.name() + "\n结果:\n" + toolOutput;
                    currentUserMessage = userMessage;
                    toolCallCount++;
                    continue;
                } else {
                    logger.warn("Failed to parse tool call from response: {}", response);
                }
            }

            progressUpdateService.analyzeConversationAndUpdate(userId, userMessage);
            memoryImportanceService.processAttributeImportanceFeedback(userId, sessionId, allAttributes, response);

            List<Map<String, Object>> pendingReminders = reminderService.getPendingReminders(userId);
            List<Map<String, Object>> urgentReminders = reminderService.getUrgentReminders(userId);
            
            if (!pendingReminders.isEmpty() || !urgentReminders.isEmpty()) {
                StringBuilder reminderText = new StringBuilder("\n\n【📅 待办提醒】\n");
                for (Map<String, Object> reminder : pendingReminders) {
                    reminderText.append("- ").append(reminder.get("message")).append("\n");
                }
                for (Map<String, Object> reminder : urgentReminders) {
                    reminderText.append("- ⚠️ ").append(reminder.get("message")).append("\n");
                }
                response = response + reminderText.toString();
                return new AiService.ThinkContent(thinkContent.think(), response);
            }

            return thinkContent;
        }

        logger.warn("Reached max tool call limit: {}, making final call with tool result", MAX_TOOL_CALLS);
        String finalPrompt = buildPrompt(userId, sessionId, userMessage, toolResult);
        AiService.ThinkContent finalResult = aiService.generateResponseWithThinking(finalPrompt);
        memoryImportanceService.processAttributeImportanceFeedback(userId, sessionId, allAttributes, finalResult.content());
        return finalResult;
    }

    /**
     * 流式处理带工具调用的聊天，使用 Ollama 流式 API 获取完整 think 和 content
     * 返回 Flux<String>，每个元素是 "think:xxx" 或 "content:xxx"
     */
    public Flux<String> processWithToolCallingStream(Long userId, Long sessionId, String userMessage) {
        String prompt = buildPrompt(userId, sessionId, userMessage, null);
        java.util.concurrent.atomic.AtomicReference<String> fullContentRef = new java.util.concurrent.atomic.AtomicReference<>("");
        java.util.concurrent.atomic.AtomicReference<String> fullThinkRef = new java.util.concurrent.atomic.AtomicReference<>("");

        return aiService.streamOllamaDirectly(prompt)
                .doOnNext(chunk -> {
                    logger.debug("Stream chunk received: type={}, length={}", 
                        chunk.startsWith("content:") ? "content" : (chunk.startsWith("think:") ? "think" : "unknown"),
                        chunk.length());
                    if (chunk.startsWith("content:")) {
                        fullContentRef.updateAndGet(s -> s + chunk.substring(8));
                    } else if (chunk.startsWith("think:")) {
                        fullThinkRef.updateAndGet(s -> s + chunk.substring(6));
                    }
                })
                .concatWith(Flux.defer(() -> {
                    String fullContent = fullContentRef.get();
                    String fullThink = fullThinkRef.get();
                    logger.info("Stream completed, checking for tool call. Content length: {}, think length: {}, content contains tool: {}, think contains tool: {}", 
                        fullContent.length(), fullThink.length(), 
                        aiService.containsToolCall(fullContent), aiService.containsToolCall(fullThink));
                    logger.debug("Full think content (first 500 chars): {}", fullThink.length() > 500 ? fullThink.substring(0, 500) : fullThink);
                    logger.debug("Full content (first 500 chars): {}", fullContent.length() > 500 ? fullContent.substring(0, 500) : fullContent);
                    
                    String combinedContent = fullContent + fullThink;
                    if (aiService.containsToolCall(combinedContent)) {
                        ToolCall toolCall = aiService.parseToolCall(combinedContent);
                        if (toolCall != null) {
                            logger.info("Executing tool call in stream: {} with args: {}", toolCall.name(), toolCall.arguments());
                            
                            Map<String, Object> args = null;
                            if (toolCall.arguments() != null && !toolCall.arguments().isEmpty() && !"{}".equals(toolCall.arguments())) {
                                try {
                                    args = aiService.getObjectMapper().readValue(toolCall.arguments(), 
                                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                                } catch (Exception e) {
                                    logger.warn("Failed to parse tool arguments in stream: {}", e.getMessage());
                                }
                            }
                            
                            String toolOutput = toolCallingConfig.callTool(toolCall.name(), userId, args);
                            String toolResult = "工具: " + toolCall.name() + "\n结果:\n" + toolOutput;
                            String finalPrompt = buildPrompt(userId, sessionId, userMessage, toolResult);
                            return aiService.streamOllamaDirectly(finalPrompt);
                        }
                    }
                    return Flux.empty();
                }));
    }

    public void saveStreamResult(Long userId, Long sessionId, String content, String think) {
        if ((content != null && !content.isEmpty()) || (think != null && !think.isEmpty())) {
            saveAiMessage(userId, sessionId,
                content != null ? content : "",
                think != null ? think : "");
        }
    }

    private List<com.personalai.os.entity.ChatMessage> convertToChatMessages(List<Conversation> conversations) {
        List<com.personalai.os.entity.ChatMessage> result = new ArrayList<>();
        for (Conversation c : conversations) {
            com.personalai.os.entity.ChatMessage msg = new com.personalai.os.entity.ChatMessage();
            msg.setContent(c.getContent());
            msg.setThink(c.getThink());
            msg.setSender(c.getSender());
            msg.setTimestamp(c.getCreatedAt());
            result.add(msg);
        }
        return result;
    }

    /**
     * 把缓存中的对话历史元素统一转成 Conversation。
     * 正常情况下（序列化器已开启类型保留）元素就是 Conversation；若残留旧的无类型缓存，
     * 元素会是 LinkedHashMap，这里兜底转换并打印警告，确保历史不再被静默丢弃。
     */
    private Conversation toConversation(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Conversation) {
            return (Conversation) obj;
        }
        try {
            logger.warn("Converting non-Conversation cache element {} to Conversation", obj.getClass().getName());
            return CONV_MAPPER.convertValue(obj, Conversation.class);
        } catch (Exception e) {
            logger.warn("Failed to convert {} to Conversation: {}", obj.getClass().getName(), e.getMessage());
            return null;
        }
    }

    private String getUserSummary(Long userId) {
        try {
            com.personalai.os.entity.MemorySummary summary = memorySummaryService.getSummary(userId);
            if (summary != null && summary.getSummary() != null) {
                return summary.getSummary();
            }
        } catch (Exception e) {
            logger.debug("Failed to get user summary for user {}: {}", userId, e.getMessage());
        }
        return null;
    }
}