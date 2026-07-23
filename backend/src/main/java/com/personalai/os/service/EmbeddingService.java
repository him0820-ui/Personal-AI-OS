package com.personalai.os.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 向量化服务类，调用Embedding模型生成文本向量并计算语义相似度
 * @author: 琦
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[0];
        }

        try {
            String url = ollamaBaseUrl + "/api/embeddings";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("prompt", text);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                JsonNode embeddingNode = jsonNode.get("embedding");
                
                if (embeddingNode != null && embeddingNode.isArray()) {
                    float[] vector = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        vector[i] = (float) embeddingNode.get(i).asDouble();
                    }
                    logger.debug("Generated embedding for text '{}': {} dimensions",
                            text.length() > 30 ? text.substring(0, 30) + "..." : text,
                            vector.length);
                    return vector;
                }
            } else {
                logger.error("Embedding API returned status code: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error generating embedding for text: {}", text, e);
            Thread.currentThread().interrupt();
        }

        return new float[0];
    }

    public float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0f;
        }

        int minLength = Math.min(a.length, b.length);
        float dotProduct = 0;
        float magnitudeA = 0;
        float magnitudeB = 0;

        for (int i = 0; i < minLength; i++) {
            dotProduct += a[i] * b[i];
            magnitudeA += a[i] * a[i];
            magnitudeB += b[i] * b[i];
        }

        float magnitude = (float) (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
        if (magnitude == 0) {
            return 0.0f;
        }

        return dotProduct / magnitude;
    }

    public boolean isSimilar(String text1, String text2, float threshold) {
        float[] embedding1 = getEmbedding(text1);
        float[] embedding2 = getEmbedding(text2);

        if (embedding1.length == 0 || embedding2.length == 0) {
            return false;
        }

        float similarity = cosineSimilarity(embedding1, embedding2);
        logger.debug("Similarity between '{}' and '{}': {} (threshold: {})",
                text1.length() > 20 ? text1.substring(0, 20) + "..." : text1,
                text2.length() > 20 ? text2.substring(0, 20) + "..." : text2,
                similarity, threshold);

        return similarity >= threshold;
    }
}