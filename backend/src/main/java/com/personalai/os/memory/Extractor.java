package com.personalai.os.memory;

import com.personalai.os.entity.ChatMessage;
import com.personalai.os.memory.dto.Attribute;
import com.personalai.os.memory.dto.ExtractedInfo;
import com.personalai.os.memory.dto.ExtractResult;
import com.personalai.os.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: 记忆提取器，从对话中提取事实、目标、时间线和待办等结构化信息
 * @author: 琦
 */
@Component
public class Extractor {

    private static final Logger logger = LoggerFactory.getLogger(Extractor.class);

    @Autowired
    private AiService aiService;

    public ExtractResult extractDirect(List<ChatMessage> conversation) {
        logger.info("Extractor.extractDirect called, conversation size: {}", conversation != null ? conversation.size() : 0);
        
        if (conversation == null || conversation.isEmpty()) {
            logger.warn("Extractor.extractDirect: conversation is null or empty");
            return new ExtractResult();
        }
        
        StringBuilder conversationText = new StringBuilder();
        for (ChatMessage msg : conversation) {
            conversationText.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
        }

        logger.info("Extractor processing conversation:\n{}", conversationText.toString());

        String promptText = "请从以下对话中提取用户明确声明或更新的个人信息，使用统一的知识图谱三元组格式（category-entity-attribute-value）。\n\n" +
            "必须严格按照以下JSON格式返回结果：\n\n" +
            "{\n" +
            "  \"attributes\": [\n" +
            "    {\n" +
            "      \"category\": \"类别\",\n" +
            "      \"entity\": \"实体\",\n" +
            "      \"attribute\": \"属性\",\n" +
            "      \"value\": \"值\",\n" +
            "      \"importance\": 100,\n" +
            "      \"confidence\": 100,\n" +
            "      \"sourceQuote\": \"用户原话引用\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"facts\": [],\n" +
            "  \"timelines\": [],\n" +
            "  \"goals\": [],\n" +
            "  \"todos\": []\n" +
            "}\n\n" +
            "统一知识模型说明：\n" +
            "- category（类别）：信息分类，只能是以下值之一：Person（个人信息）、Preference（偏好喜好）、Skill（技能能力）、Fact（事实）、Timeline（事件）、Goal（目标）、Todo（待办）、Relationship（关系）\n" +
            "- entity（实体）：信息描述的对象，如\"哈利波特\"、\"Java\"、\"张三\"、\"姓名\"\n" +
            "- attribute（属性）：实体的属性名称，如\"Interest\"、\"Level\"、\"Role\"、\"Value\"\n" +
            "- value（值）：属性的值，如\"Like\"、\"熟练\"、\"导师\"、\"唐琦\"\n\n" +
            "提取规则：\n" +
            "1. 只提取用户主动声明的信息（如\"我叫XXX\"、\"我改名字了\"、\"我学会了XXX\"）\n" +
            "2. 如果用户只是询问问题（如\"我叫什么来着\"、\"你知道XXX吗\"），不要提取任何信息\n" +
            "3. 如果AI回答中提到的信息不是用户明确声明的，不要提取\n" +
            "4. 用户表达喜好或厌恶的信息必须提取\n" +
            "5. 当用户更新已有信息时，必须提取新的信息\n" +
            "6. 每条提取的信息必须附带用户原话引用（sourceQuote）\n\n" +
            "字段说明：\n" +
            "- importance：重要性评分（0-100）\n" +
            "  - 90-100：核心身份信息（姓名、职业、专业、学校）\n" +
            "  - 70-89：重要目标和技能（秋招、掌握的技术栈）\n" +
            "  - 50-69：一般信息（兴趣爱好、日常习惯）\n" +
            "  - 30-49：临时信息（今天做了什么）\n" +
            "  - 0-29：不重要的闲聊\n" +
            "- confidence：置信度（0-100）\n" +
            "- sourceQuote：用户原话引用\n\n" +
            "类别分类规则：\n" +
            "- Person：个人身份信息（姓名、年龄、性别、专业、学校、职业）\n" +
            "- Preference：偏好喜好（喜欢/讨厌的事物、兴趣爱好）\n" +
            "- Skill：技能能力（掌握的技术、能力水平）\n" +
            "- Fact：一般事实（其他不适合以上类别的事实信息）\n" +
            "- Timeline：时间线事件（发生过的事情、学习经历）\n" +
            "- Goal：目标（计划达成的事情）\n" +
            "- Todo：待办事项（需要完成的任务）\n" +
            "- Relationship：关系（与他人的关系）\n\n" +
            "示例：\n" +
            "- 用户说\"我叫唐琦\" → 提取 {\"category\": \"Person\", \"entity\": \"姓名\", \"attribute\": \"Value\", \"value\": \"唐琦\", \"importance\": 100, \"confidence\": 100, \"sourceQuote\": \"我叫唐琦\"}\n" +
            "- 用户说\"我喜欢哈利波特\" → 提取 {\"category\": \"Preference\", \"entity\": \"哈利波特\", \"attribute\": \"Interest\", \"value\": \"Like\", \"importance\": 60, \"confidence\": 100, \"sourceQuote\": \"我喜欢哈利波特\"}\n" +
            "- 用户说\"我讨厌哈利波特\"（之前说过喜欢） → 提取 {\"category\": \"Preference\", \"entity\": \"哈利波特\", \"attribute\": \"Interest\", \"value\": \"Dislike\", \"importance\": 60, \"confidence\": 100, \"sourceQuote\": \"我讨厌哈利波特\"}\n" +
            "- 用户说\"我喜欢爬山\" → 提取 {\"category\": \"Preference\", \"entity\": \"爬山\", \"attribute\": \"Interest\", \"value\": \"Like\", \"importance\": 60, \"confidence\": 100, \"sourceQuote\": \"我喜欢爬山\"}\n" +
            "- 用户说\"我讨厌读书\" → 提取 {\"category\": \"Preference\", \"entity\": \"读书\", \"attribute\": \"Interest\", \"value\": \"Dislike\", \"importance\": 60, \"confidence\": 100, \"sourceQuote\": \"我讨厌读书\"}\n" +
            "- 用户说\"我熟练掌握Java\" → 提取 {\"category\": \"Skill\", \"entity\": \"Java\", \"attribute\": \"Level\", \"value\": \"熟练\", \"importance\": 80, \"confidence\": 100, \"sourceQuote\": \"我熟练掌握Java\"}\n" +
            "- 用户说\"张三是我的导师\" → 提取 {\"category\": \"Relationship\", \"entity\": \"张三\", \"attribute\": \"Role\", \"value\": \"导师\", \"importance\": 70, \"confidence\": 100, \"sourceQuote\": \"张三是我的导师\"}\n" +
            "- 用户说\"我叫什么来着\" → 不提取任何信息\n\n" +
            "如果没有新信息或用户只是询问，返回：{\"attributes\": [], \"facts\": [], \"timelines\": [], \"goals\": [], \"todos\": []}\n\n" +
            "对话内容：\n" + conversationText.toString();

        ExtractResult result = aiService.generateStructuredResponse(promptText, ExtractResult.class);

        if (result != null) {
            logger.info("Extractor result: attributes={}, facts={}, timelines={}, goals={}, todos={}",
                result.getAttributes() != null ? result.getAttributes().size() : 0,
                result.getFacts() != null ? result.getFacts().size() : 0,
                result.getTimelines() != null ? result.getTimelines().size() : 0,
                result.getGoals() != null ? result.getGoals().size() : 0,
                result.getTodos() != null ? result.getTodos().size() : 0);
            
            if (result.getAttributes() != null) {
                for (var attr : result.getAttributes()) {
                    logger.info("  Attribute: category={}, entity={}, attribute={}, value={}", 
                    attr.getCategory(), attr.getEntity(), attr.getAttribute(), attr.getValue());
                }
            }
        } else {
            logger.warn("Extractor returned null result");
        }

        return result != null ? result : new ExtractResult();
    }

    @Deprecated
    public List<ExtractedInfo> extract(List<ChatMessage> conversation) {
        ExtractResult result = extractDirect(conversation);
        return convertToExtractedInfo(result);
    }

    private List<ExtractedInfo> convertToExtractedInfo(ExtractResult result) {
        List<ExtractedInfo> infos = new ArrayList<>();

        if (result == null) {
            return infos;
        }

        if (result.getAttributes() != null) {
            for (var attr : result.getAttributes()) {
                ExtractedInfo info = new ExtractedInfo();
                info.setContent(attr.getValue());
                info.setCategory(attr.getCategory());
                info.setKey(attr.getEntity() + ":" + attr.getAttribute());
                info.setScore(attr.getImportance() != null ? attr.getImportance() : 50);
                infos.add(info);
            }
        }

        if (result.getFacts() != null) {
            for (var fact : result.getFacts()) {
                ExtractedInfo info = new ExtractedInfo();
                info.setContent(fact.getValue());
                info.setCategory("Fact");
                info.setKey(fact.getKey() != null ? fact.getKey() : "其他");
                info.setScore(fact.getImportance() != null ? fact.getImportance() : 50);
                infos.add(info);
            }
        }

        if (result.getTimelines() != null) {
            for (var timeline : result.getTimelines()) {
                ExtractedInfo info = new ExtractedInfo();
                info.setContent(timeline.getTitle());
                info.setCategory("Timeline");
                info.setKey(timeline.getDescription());
                info.setScore(timeline.getImportance() != null ? timeline.getImportance() : 50);
                infos.add(info);
            }
        }

        if (result.getGoals() != null) {
            for (var goal : result.getGoals()) {
                ExtractedInfo info = new ExtractedInfo();
                info.setContent(goal.getTitle());
                info.setCategory("Goal");
                info.setKey(goal.getDescription());
                info.setScore(goal.getImportance() != null ? goal.getImportance() : 50);
                infos.add(info);
            }
        }

        if (result.getTodos() != null) {
            for (var todo : result.getTodos()) {
                ExtractedInfo info = new ExtractedInfo();
                info.setContent(todo.getTitle());
                info.setCategory("Todo");
                info.setKey(todo.getDescription());
                info.setScore(todo.getImportance() != null ? todo.getImportance() : 50);
                infos.add(info);
            }
        }

        return infos;
    }
}
