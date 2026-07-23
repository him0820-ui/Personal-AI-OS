package com.personalai.os.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @description: 聊天结束事件，用于触发记忆提取和更新流程
 * @author: 琦
 */
@Getter
public class ChatFinishedEvent extends ApplicationEvent {
    
    private final Long userId;
    private final List<com.personalai.os.entity.ChatMessage> conversation;
    
    public ChatFinishedEvent(Object source, Long userId, List<com.personalai.os.entity.ChatMessage> conversation) {
        super(source);
        this.userId = userId;
        this.conversation = conversation;
    }
}
