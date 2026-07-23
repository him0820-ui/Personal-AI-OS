package com.personalai.os.memory;

import com.personalai.os.entity.ChatMessage;
import com.personalai.os.memory.dto.ExtractResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 记忆引擎核心类，协调记忆提取、合并、存储和评分流程
 * @author: 琦
 */
@Component
public class MemoryEngine {

    private static final Logger logger = LoggerFactory.getLogger(MemoryEngine.class);

    @Autowired
    private Extractor extractor;

    @Autowired
    private Merger merger;

    @Autowired
    private Writer writer;

    public void process(Long userId, List<ChatMessage> conversation) {
        logger.info("MemoryEngine processing for user {}, {} messages", userId, conversation.size());
        
        try {
            ExtractResult result = extractor.extractDirect(conversation);
            
            logger.info("Extractor result: attributes={}, facts={}, timelines={}, goals={}, todos={}",
                result.getAttributes() != null ? result.getAttributes().size() : 0,
                result.getFacts() != null ? result.getFacts().size() : 0,
                result.getTimelines() != null ? result.getTimelines().size() : 0,
                result.getGoals() != null ? result.getGoals().size() : 0,
                result.getTodos() != null ? result.getTodos().size() : 0);
            
            ExtractResult merged = merger.mergeResult(result);
            
            StringBuilder conversationText = new StringBuilder();
            for (ChatMessage msg : conversation) {
                conversationText.append(msg.getContent()).append(" ");
            }
            
            writer.writeDirect(userId, merged, conversationText.toString());
            
            logger.info("MemoryEngine processing completed");
        } catch (Exception e) {
            logger.error("MemoryEngine processing failed for user {}: {}", userId, e.getMessage(), e);
        }
    }
}
