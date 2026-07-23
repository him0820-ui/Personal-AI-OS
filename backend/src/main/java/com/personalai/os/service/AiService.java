package com.personalai.os.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: AI服务类，封装与大语言模型的交互，包括流式响应、工具调用和结构化输出
 * @author: 琦
 */
@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private static final String THINK_OPEN = new String(new char[]{'<', 't', 'h', 'i', 'n', 'k', '>'});
    private static final String THINK_CLOSE = new String(new char[]{'<', '/', 't', 'h', 'i', 'n', 'k', '>'});
    private static final String TOOL_OPEN = new String(new char[]{'<', 't', 'o', 'o', 'l', '>'});
    private static final String TOOL_CLOSE = new String(new char[]{'<', '/', 't', 'o', 'o', 'l', '>'});

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
        "(?:<tool>|\\(tool\\))([\\s\\S]*?)(?:</tool>|\\))", Pattern.DOTALL);
    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:deepseek-r1:8b}")
    private String modelName;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ObjectMapper objectMapper;

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private ChatClient chatClient;

    @Autowired
    public void init() {
        this.chatClient = ChatClient.create(chatModel);
        logger.info("AiService initialized with model: {}, ollamaBaseUrl: {}", chatModel.getClass().getName(), ollamaBaseUrl);
    }

    /**
     * 直接调用 Ollama API，获取包含 thinking 字段的完整响应
     * Spring AI 的 content() 方法会过滤掉 thinking 字段，所以需要直接调用
     */
    public ThinkContent generateResponseWithThinking(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.7);

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", List.of(message));

            RestClient restClient = RestClient.builder()
                    .baseUrl(ollamaBaseUrl)
                    .build();

            String responseJson = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode messageNode = root.path("message");

            String thinkContent = messageNode.has("thinking") ? messageNode.get("thinking").asText("") : "";
            String content = messageNode.has("content") ? messageNode.get("content").asText("") : "";

            logger.info("Ollama direct call - think: {} chars, content: {} chars",
                    thinkContent.length(), content.length());

            return new ThinkContent(thinkContent.trim(), content.trim());
        } catch (Exception e) {
            logger.error("Error calling Ollama API directly, falling back to Spring AI", e);
            String response = generateResponse(prompt);
            return new ThinkContent("", response);
        }
    }

    /**
     * 调用大语言模型生成普通文本响应
     * @param prompt 输入提示词
     * @return LLM生成的文本响应
     */
    public String generateResponse(String prompt) {
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        return response;
    }

    /**
     * 调用大语言模型生成结构化响应，自动解析JSON并转换为指定类型
     * @param prompt 输入提示词，应包含JSON格式要求
     * @param targetClass 目标类型，用于JSON反序列化
     * @return 解析后的结构化对象
     */
    public <T> T generateStructuredResponse(String prompt, Class<T> targetClass) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.0);

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", List.of(message));

            logger.info("generateStructuredResponse - model: {}, prompt length: {}", modelName, prompt.length());

            RestClient restClient = RestClient.builder()
                    .baseUrl(ollamaBaseUrl)
                    .build();

            String responseJson = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            logger.info("generateStructuredResponse - raw responseJson length: {}", responseJson != null ? responseJson.length() : 0);
            logger.info("generateStructuredResponse - responseJson: {}", responseJson);

            if (responseJson == null || responseJson.isEmpty()) {
                logger.error("generateStructuredResponse - responseJson is null or empty");
                return null;
            }

            JsonNode root = objectMapper.readTree(responseJson);
            logger.info("generateStructuredResponse - root node keys: {}", root.fieldNames());
            JsonNode messageNode = root.path("message");
            logger.info("generateStructuredResponse - messageNode is null: {}", messageNode.isNull());
            logger.info("generateStructuredResponse - messageNode has content: {}", messageNode.has("content"));
            logger.info("generateStructuredResponse - messageNode content type: {}", messageNode.has("content") ? messageNode.get("content").getNodeType() : "N/A");
            String response = messageNode.has("content") ? messageNode.get("content").asText("") : "";

            logger.info("LLM raw response for {}: {}", targetClass.getSimpleName(), response);
            logger.info("LLM raw response length: {}", response != null ? response.length() : 0);

            String jsonStr = extractJsonFromResponse(response);
            logger.info("Extracted JSON string: {}", jsonStr);
            
            if (jsonStr != null && !jsonStr.isEmpty()) {
                logger.info("Extracted JSON for {}: {}", targetClass.getSimpleName(), jsonStr);
                T result = objectMapper.readValue(jsonStr, targetClass);
                logger.info("Parsed {} successfully", targetClass.getSimpleName());
                return result;
            } else {
                logger.warn("No JSON extracted from LLM response for {}", targetClass.getSimpleName());
                logger.warn("Trying to parse raw response as JSON...");
                try {
                    T result = objectMapper.readValue(response, targetClass);
                    logger.info("Successfully parsed raw response as {} directly", targetClass.getSimpleName());
                    return result;
                } catch (Exception e) {
                    logger.warn("Failed to parse raw response as {}: {}", targetClass.getSimpleName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error generating structured response for class: {}", targetClass.getName(), e);
        }
        return null;
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        String cleanedResponse = response.trim();
        
        if (cleanedResponse.startsWith("```json")) {
            int endCodeBlock = cleanedResponse.indexOf("```", 7);
            if (endCodeBlock != -1) {
                cleanedResponse = cleanedResponse.substring(7, endCodeBlock).trim();
            } else {
                cleanedResponse = cleanedResponse.substring(7).trim();
            }
        } else if (cleanedResponse.startsWith("```")) {
            int endCodeBlock = cleanedResponse.indexOf("```", 3);
            if (endCodeBlock != -1) {
                cleanedResponse = cleanedResponse.substring(3, endCodeBlock).trim();
            } else {
                cleanedResponse = cleanedResponse.substring(3).trim();
            }
        }

        int firstBrace = cleanedResponse.indexOf('{');
        int lastBrace = cleanedResponse.lastIndexOf('}');
        
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return cleanedResponse.substring(firstBrace, lastBrace + 1);
        }

        int firstBracket = cleanedResponse.indexOf('[');
        int lastBracket = cleanedResponse.lastIndexOf(']');
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return "{\"data\":" + cleanedResponse.substring(firstBracket, lastBracket + 1) + "}";
        }

        return null;
    }

    /**
     * 使用Spring AI流式API生成响应，支持think和content分离
     * @param prompt 输入提示词
     * @return 流式响应Flux，每个元素为"think:xxx"或"content:xxx"格式
     */
    public Flux<String> streamResponse(String prompt) {
        try {
            if (chatModel instanceof StreamingChatModel) {
                StreamingChatModel streamingChatModel = (StreamingChatModel) chatModel;
                Prompt chatPrompt = new Prompt(prompt);
                AtomicBoolean inThinkBlock = new AtomicBoolean(false);
                return streamingChatModel.stream(chatPrompt)
                        .map(ChatResponse::getResult)
                        .map(result -> result.getOutput().getContent())
                        .doOnNext(content -> logger.debug("Received chunk: {} chars, starts with: {}",
                            content != null ? content.length() : 0,
                            content != null && content.length() > 20 ? content.substring(0, 20) : content))
                        .flatMap(content -> {
                            return extractThinkAndContent(content, inThinkBlock);
                        });
            }
        } catch (Exception e) {
            logger.error("Error streaming response", e);
        }
        logger.warn("Falling back to non-streaming response");
        String response = generateResponse(prompt);
        return Flux.just("content:" + response);
    }

    /**
     * 使用 Ollama 流式 API 直接调用，获取完整的 think 和 content
     * Spring AI 的 stream() 会过滤 thinking 字段，需要直接调用 Ollama API
     */
    public Flux<String> streamOllamaDirectly(String prompt) {
        return Flux.create(sink -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", modelName);
                requestBody.put("stream", true);
                requestBody.put("temperature", 0.7);

                Map<String, String> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);
                requestBody.put("messages", List.of(message));

                String bodyJson = objectMapper.writeValueAsString(requestBody);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ollamaBaseUrl + "/api/chat"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                        .thenAccept(response -> {
                            response.body().forEach(line -> {
                                try {
                                    JsonNode node = objectMapper.readTree(line);
                                    JsonNode messageNode = node.path("message");
                                    String think = messageNode.has("thinking") ? messageNode.get("thinking").asText("") : "";
                                    String content = messageNode.has("content") ? messageNode.get("content").asText("") : "";

                                    logger.debug("Stream line raw (first 200 chars): {}", line.length() > 200 ? line.substring(0, 200) : line);
                                    logger.debug("Stream line - think length: {}, content length: {}", think.length(), content.length());

                                    if (!think.isEmpty()) {
                                        sink.next("think:" + think);
                                    }
                                    if (!content.isEmpty()) {
                                        sink.next("content:" + content);
                                    }
                                } catch (Exception e) {
                                    logger.error("Error parsing stream line: {}", e.getMessage());
                                }
                            });
                            sink.complete();
                        })
                        .exceptionally(e -> {
                            logger.error("Error in Ollama stream: {}", e.getMessage());
                            sink.error(e);
                            return null;
                        });
            } catch (Exception e) {
                logger.error("Error setting up Ollama stream: {}", e.getMessage());
                sink.error(e);
            }
        });
    }

    /**
     * 从LLM响应中解析工具调用信息
     * @param response LLM生成的响应文本
     * @return 解析后的ToolCall对象，包含工具名称和参数
     */
    public ToolCall parseToolCall(String response) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        if (matcher.find()) {
            String toolCallJson = matcher.group(1);
            try {
                JsonNode node = objectMapper.readTree(toolCallJson);
                String toolName = node.has("name") ? node.get("name").asText() : null;
                JsonNode argsNode = node.has("arguments") ? node.get("arguments") : null;
                String args = argsNode != null ? argsNode.toString() : "{}";

                if (toolName != null && !toolName.isEmpty()) {
                    logger.info("Parsed tool call: {}", toolName);
                    return new ToolCall(toolName, args);
                }
            } catch (Exception e) {
                logger.error("Failed to parse tool call JSON", e);
            }
        }

        return null;
    }

    /**
     * 检查响应中是否包含工具调用标记
     * @param response LLM生成的响应文本
     * @return 是否包含工具调用
     */
    public boolean containsToolCall(String response) {
        return response != null && TOOL_CALL_PATTERN.matcher(response).find();
    }

    /**
     * 从响应中移除工具调用标记，返回纯文本内容
     * @param response LLM生成的响应文本
     * @return 移除工具调用后的纯文本
     */
    public String removeToolCall(String response) {
        if (response == null) {
            return null;
        }
        return TOOL_CALL_PATTERN.matcher(response).replaceAll("").trim();
    }

    private Flux<String> extractThinkAndContent(String content, AtomicBoolean inThinkBlock) {
        if (content == null) {
            return Flux.empty();
        }

        StringBuilder result = new StringBuilder();
        StringBuilder thinkBuilder = new StringBuilder();
        int i = 0;

        while (i < content.length()) {
            if (!inThinkBlock.get()) {
                int startIndex = content.indexOf(THINK_OPEN, i);
                if (startIndex == -1) {
                    result.append(content.substring(i));
                    break;
                }
                result.append(content.substring(i, startIndex));
                inThinkBlock.set(true);
                i = startIndex + THINK_OPEN.length();
            } else {
                int endIndex = content.indexOf(THINK_CLOSE, i);
                if (endIndex == -1) {
                    thinkBuilder.append(content.substring(i));
                    i = content.length();
                } else {
                    thinkBuilder.append(content.substring(i, endIndex));
                    inThinkBlock.set(false);
                    i = endIndex + THINK_CLOSE.length();
                }
            }
        }

        Flux<String> flux = Flux.empty();

        if (thinkBuilder.length() > 0) {
            flux = flux.concatWith(Flux.just("think:" + thinkBuilder.toString()));
        }

        if (result.length() > 0) {
            flux = flux.concatWith(Flux.just("content:" + result.toString()));
        }

        return flux;
    }

    public record ToolCall(String name, String arguments) {}

    public record ThinkContent(String think, String content) {}

    /**
     * 从响应中分离think和content内容
     * @param response LLM生成的响应文本，可能包含<think>标签
     * @return 分离后的ThinkContent对象
     */
    public ThinkContent separateThinkAndContent(String response) {
        if (response == null || response.isEmpty()) {
            return new ThinkContent("", "");
        }

        int thinkStart = response.indexOf(THINK_OPEN);
        if (thinkStart != -1) {
            int thinkEnd = response.indexOf(THINK_CLOSE, thinkStart);
            if (thinkEnd != -1) {
                String thinkContent = response.substring(thinkStart + THINK_OPEN.length(), thinkEnd).trim();
                String contentPart = (thinkEnd + THINK_CLOSE.length() < response.length())
                        ? response.substring(thinkEnd + THINK_CLOSE.length()).trim()
                        : "";
                return new ThinkContent(thinkContent, contentPart);
            }
        }

        return new ThinkContent("", response);
    }
}
