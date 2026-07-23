package com.personalai.os.memory;

import com.personalai.os.memory.dto.ExtractResult;
import com.personalai.os.memory.dto.ExtractedInfo;
import com.personalai.os.memory.dto.Fact;
import com.personalai.os.memory.dto.Goal;
import com.personalai.os.memory.dto.Timeline;
import com.personalai.os.memory.dto.Todo;
import com.personalai.os.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 记忆合并器，处理语义去重和记忆合并
 * @author: 琦
 */
@Component
public class Merger {

    private static final Logger logger = LoggerFactory.getLogger(Merger.class);
    
    private static final float SIMILARITY_THRESHOLD = 0.80f;

    @Autowired
    private EmbeddingService embeddingService;

    public ExtractResult mergeResult(ExtractResult result) {
        if (result == null) {
            return new ExtractResult();
        }

        if (result.getFacts() != null) {
            result.setFacts(mergeFacts(result.getFacts()));
        }
        if (result.getTimelines() != null) {
            result.setTimelines(mergeTimelines(result.getTimelines()));
        }
        if (result.getGoals() != null) {
            result.setGoals(mergeGoals(result.getGoals()));
        }
        if (result.getTodos() != null) {
            result.setTodos(mergeTodos(result.getTodos()));
        }

        return result;
    }

    private List<Fact> mergeFacts(List<Fact> facts) {
        List<Fact> result = new ArrayList<>();
        for (Fact fact : facts) {
            boolean merged = false;
            for (Fact existing : result) {
                if (fact.getKey() != null && fact.getKey().equals(existing.getKey())) {
                    existing.setValue(fact.getValue());
                    if (fact.getImportance() != null && fact.getImportance() > (existing.getImportance() != null ? existing.getImportance() : 0)) {
                        existing.setImportance(fact.getImportance());
                    }
                    if (fact.getConfidence() != null && fact.getConfidence() > (existing.getConfidence() != null ? existing.getConfidence() : 0)) {
                        existing.setConfidence(fact.getConfidence());
                    }
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                result.add(fact);
            }
        }
        return result;
    }

    private List<Timeline> mergeTimelines(List<Timeline> timelines) {
        return mergeByEmbeddingSimilarity(timelines, Timeline::getTitle);
    }

    private List<Goal> mergeGoals(List<Goal> goals) {
        return mergeByEmbeddingSimilarity(goals, Goal::getTitle);
    }

    private List<Todo> mergeTodos(List<Todo> todos) {
        return mergeByEmbeddingSimilarity(todos, Todo::getTitle);
    }

    private <T> List<T> mergeByEmbeddingSimilarity(List<T> items, java.util.function.Function<T, String> getContent) {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            boolean merged = false;
            String newContent = getContent.apply(item);
            
            for (T existing : result) {
                String existingContent = getContent.apply(existing);
                
                boolean similar = embeddingService.isSimilar(newContent, existingContent, SIMILARITY_THRESHOLD);
                if (similar) {
                    logger.debug("Merging similar items: '{}' and '{}'", newContent, existingContent);
                    merged = true;
                    break;
                }
            }
            
            if (!merged) {
                result.add(item);
            }
        }
        return result;
    }

    @Deprecated
    public List<ExtractedInfo> merge(List<ExtractedInfo> infos) {
        List<ExtractedInfo> result = new ArrayList<>();
        
        for (ExtractedInfo info : infos) {
            boolean merged = false;
            
            for (ExtractedInfo existing : result) {
                if (shouldMerge(info, existing)) {
                    mergeInfo(existing, info);
                    merged = true;
                    break;
                }
            }
            
            if (!merged) {
                result.add(info);
            }
        }
        
        return result;
    }

    private boolean shouldMerge(ExtractedInfo newInfo, ExtractedInfo existing) {
        if (!newInfo.getCategory().equals(existing.getCategory())) {
            return false;
        }
        
        if ("Fact".equals(newInfo.getCategory()) && newInfo.getKey() != null && 
            newInfo.getKey().equals(existing.getKey())) {
            return true;
        }
        
        String newContent = newInfo.getContent().toLowerCase();
        String existingContent = existing.getContent().toLowerCase();
        
        if (newContent.contains(existingContent) || existingContent.contains(newContent)) {
            return true;
        }
        
        int minLength = Math.min(newContent.length(), existingContent.length());
        int matchCount = 0;
        
        for (int i = 0; i < minLength - 2; i++) {
            String sub = newContent.substring(i, i + 3);
            if (existingContent.contains(sub)) {
                matchCount++;
            }
        }
        
        return (double) matchCount / (minLength / 3) > 0.5;
    }

    private void mergeInfo(ExtractedInfo existing, ExtractedInfo newInfo) {
        existing.setContent(newInfo.getContent());
        
        if (newInfo.getScore() != null && newInfo.getScore() > existing.getScore()) {
            existing.setScore(newInfo.getScore());
        }
        
        if (newInfo.getKey() != null && !newInfo.getKey().isEmpty()) {
            existing.setKey(newInfo.getKey());
        }
    }
}
