package com.personalai.os.event.listener;

import com.personalai.os.event.ChatFinishedEvent;
import com.personalai.os.memory.MemoryEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @description: 记忆监听器，监听聊天结束事件并触发记忆提取流程
 * @author: 琦
 */
@Component
public class MemoryListener {

    private static final Logger logger = LoggerFactory.getLogger(MemoryListener.class);

    @Autowired
    private MemoryEngine memoryEngine;

    @Async("taskExecutor")
    @EventListener
    public void onChatFinished(ChatFinishedEvent event) {
        try {
            logger.info("MemoryListener async processing for user {}, {} messages", 
                event.getUserId(), event.getConversation() != null ? event.getConversation().size() : 0);
            
            if (event.getConversation() == null || event.getConversation().isEmpty()) {
                logger.warn("MemoryListener: conversation is null or empty for user {}", event.getUserId());
                return;
            }
            
            memoryEngine.process(event.getUserId(), event.getConversation());
            logger.info("MemoryListener async processing completed");
        } catch (Exception e) {
            logger.error("MemoryListener async processing failed for user {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }
}
