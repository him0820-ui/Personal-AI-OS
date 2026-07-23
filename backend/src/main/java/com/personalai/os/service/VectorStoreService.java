package com.personalai.os.service;

import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 向量存储服务类，管理记忆的向量存储和语义检索
 * @author: 琦
 */
@Service
public class VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);

    @Autowired
    private VectorStore vectorStore;

    public void storeFact(MemoryFact fact) {
        String content = (fact.getKey() != null ? fact.getKey() : "") + " " + 
                         (fact.getValue() != null ? fact.getValue() : "");
        storeMemory(fact.getUserId(), fact.getId(), "Fact", content);
    }

    public void storeGoal(MemoryGoal goal) {
        String content = (goal.getTitle() != null ? goal.getTitle() : "") + " " + 
                         (goal.getDescription() != null ? goal.getDescription() : "");
        storeMemory(goal.getUserId(), goal.getId(), "Goal", content);
    }

    public void storeTimeline(MemoryTimeline timeline) {
        String content = (timeline.getTitle() != null ? timeline.getTitle() : "") + " " + 
                         (timeline.getDescription() != null ? timeline.getDescription() : "");
        storeMemory(timeline.getUserId(), timeline.getId(), "Timeline", content);
    }

    public void storeTodo(MemoryTodo todo) {
        storeMemory(todo.getUserId(), todo.getId(), "Todo", todo.getTitle());
    }

    public void storeAttribute(MemoryAttribute attribute) {
        String content = (attribute.getCategory() != null ? attribute.getCategory() : "") + " " + 
                         (attribute.getEntity() != null ? attribute.getEntity() : "") + " " + 
                         (attribute.getAttribute() != null ? attribute.getAttribute() : "") + " " + 
                         (attribute.getValue() != null ? attribute.getValue() : "");
        storeMemory(attribute.getUserId(), attribute.getId(), attribute.getCategory(), content);
    }

    private void storeMemory(Long userId, Long memoryId, String memoryType, String content) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", userId != null ? userId.toString() : "");
            metadata.put("memoryId", memoryId != null ? memoryId.toString() : "");
            metadata.put("memoryType", memoryType);

            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));

            logger.info("Stored memory in Qdrant: userId={}, memoryId={}, type={}, content={}", 
                    userId, memoryId, memoryType, content);
        } catch (Exception e) {
            logger.error("Failed to store memory in Qdrant: {}", e.getMessage(), e);
        }
    }

    public List<SearchResult> search(Long userId, String query, int topK) {
        try {
            logger.info("Vector search: userId={}, query='{}', topK={}", userId, query, topK);
            
            String filterExpression = "userId == '" + userId.toString() + "'";
            SearchRequest request = SearchRequest.query(query)
                    .withTopK(topK)
                    .withFilterExpression(filterExpression);

            List<Document> results = vectorStore.similaritySearch(request);
            List<SearchResult> searchResults = new ArrayList<>();

            logger.info("Vector search returned {} results", results.size());
            
            for (Document doc : results) {
                Map<String, Object> metadata = doc.getMetadata();
                Float score = metadata.containsKey("_score") ? ((Number) metadata.get("_score")).floatValue() : null;
                
                SearchResult searchResult = new SearchResult(
                        metadata.containsKey("memoryId") ? Long.parseLong((String) metadata.get("memoryId")) : null,
                        metadata.containsKey("memoryType") ? (String) metadata.get("memoryType") : "Unknown",
                        doc.getContent(),
                        score
                );
                searchResults.add(searchResult);
                
                logger.info("  Result: content='{}', type={}, score={}", 
                        doc.getContent(), searchResult.memoryType(), score);
            }

            return searchResults;
        } catch (Exception e) {
            logger.error("Vector search failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public void deleteMemory(Long userId, Long memoryId, String memoryType) {
        try {
            logger.debug("Delete memory from Qdrant: userId={}, memoryId={}, type={}", userId, memoryId, memoryType);
        } catch (Exception e) {
            logger.error("Failed to delete memory from Qdrant: {}", e.getMessage());
        }
    }

    public record SearchResult(Long memoryId, String memoryType, String content, Float score) {}
}
