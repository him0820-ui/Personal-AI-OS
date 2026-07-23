package com.personalai.os.service;

import com.personalai.os.entity.MemoryConflict;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryFactHistory;
import com.personalai.os.mapper.MemoryConflictMapper;
import com.personalai.os.mapper.MemoryFactHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 冲突检测服务类，检测记忆数据中的语义冲突和矛盾
 * @author: 琦
 */
@Service
public class ConflictDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConflictDetectionService.class);

    private static final double CONFLICT_THRESHOLD = 0.3;
    private static final double SIMILAR_THRESHOLD = 0.8;
    private static final double TOPIC_SIMILARITY_THRESHOLD = 0.5;

    private static final Set<String> CONTRADICTION_PAIRS = new HashSet<>();
    static {
        CONTRADICTION_PAIRS.add("喜欢讨厌");
        CONTRADICTION_PAIRS.add("讨厌喜欢");
        CONTRADICTION_PAIRS.add("喜欢不喜欢");
        CONTRADICTION_PAIRS.add("不喜欢喜欢");
        CONTRADICTION_PAIRS.add("爱恨");
        CONTRADICTION_PAIRS.add("恨爱");
        CONTRADICTION_PAIRS.add("擅长不擅长");
        CONTRADICTION_PAIRS.add("不擅长擅长");
        CONTRADICTION_PAIRS.add("会不会");
        CONTRADICTION_PAIRS.add("不会会");
        CONTRADICTION_PAIRS.add("是不是");
        CONTRADICTION_PAIRS.add("不是是");
        CONTRADICTION_PAIRS.add("在不在");
        CONTRADICTION_PAIRS.add("不在在");
    }

    private static final Pattern INTEREST_PATTERN = Pattern.compile("(喜欢|讨厌|爱|恨|不喜欢|擅长|不擅长|会|不会)\\s*(.+)$");

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private MemoryConflictMapper conflictMapper;

    @Autowired
    private MemoryFactHistoryMapper factHistoryMapper;

    @Autowired
    private AiService aiService;

    public ConflictDetectionResult detectConflict(Long userId, Long factId, String key, String oldValue, String newValue) {
        if (oldValue == null || newValue == null) {
            return ConflictDetectionResult.noConflict();
        }

        oldValue = oldValue.trim();
        newValue = newValue.trim();

        if (oldValue.equalsIgnoreCase(newValue)) {
            return ConflictDetectionResult.noConflict();
        }

        logger.info("Conflict detection started - key={}, oldValue={}, newValue={}", key, oldValue, newValue);

        if (containsContradictionWords(oldValue, newValue)) {
            String oldTopic = extractTopic(oldValue);
            String newTopic = extractTopic(newValue);
            
            logger.info("Contradiction words detected - oldTopic={}, newTopic={}", oldTopic, newTopic);
            
            if (isSameTopic(oldTopic, newTopic)) {
                logger.info("Same topic detected, confirming conflict");
                return detectContradiction(userId, factId, key, oldValue, newValue, 0.0);
            } else {
                logger.info("Different topics, not a conflict");
                return ConflictDetectionResult.change(oldValue, newValue);
            }
        }

        double similarity = calculateSemanticSimilarity(oldValue, newValue);
        logger.info("Conflict detection - key={}, similarity={}", key, similarity);

        if (similarity < CONFLICT_THRESHOLD) {
            return detectContradiction(userId, factId, key, oldValue, newValue, similarity);
        } else if (similarity >= SIMILAR_THRESHOLD) {
            return ConflictDetectionResult.update();
        } else {
            return ConflictDetectionResult.change(oldValue, newValue);
        }
    }

    public void detectInterestConflict(Long userId, Long factId, String topic, String oldSentiment, String newSentiment) {
        logger.info("Interest conflict detection - topic={}, oldSentiment={}, newSentiment={}", topic, oldSentiment, newSentiment);
        
        String aiAnalysis = analyzeWithAI("兴趣爱好", oldSentiment + topic, newSentiment + topic);
        
        MemoryConflict conflict = new MemoryConflict();
        conflict.setUserId(userId);
        conflict.setFactId(factId);
        conflict.setKey("兴趣爱好");
        conflict.setOldValue(oldSentiment + topic);
        conflict.setNewValue(newSentiment + topic);
        conflict.setConflictScore(100);
        conflict.setConflictType(MemoryConflict.ConflictType.CONTRADICTION.name());
        conflict.setReviewStatus(MemoryConflict.ReviewStatus.PENDING.name());
        conflict.setAiAnalysis(aiAnalysis);
        conflict.setCreatedAt(LocalDateTime.now());
        
        conflictMapper.insert(conflict);
        logger.warn("Interest conflict detected and recorded - id={}, topic={}, oldSentiment={}, newSentiment={}", 
            conflict.getId(), topic, oldSentiment, newSentiment);
    }

    public ConflictDetectionResult detectAttributeConflict(Long userId, Long factId, String category, 
                                                          String entity, String attribute, 
                                                          String oldValue, String newValue) {
        logger.info("Attribute conflict detection - category={}, entity={}, attribute={}, oldValue={}, newValue={}", 
            category, entity, attribute, oldValue, newValue);

        if (oldValue.equals(newValue)) {
            return ConflictDetectionResult.noConflict();
        }

        if (isContradictoryValue(oldValue, newValue)) {
            String aiAnalysis = analyzeWithAI(category + ":" + entity + ":" + attribute, oldValue, newValue);
            
            MemoryConflict conflict = new MemoryConflict();
            conflict.setUserId(userId);
            conflict.setFactId(factId);
            conflict.setKey(category + ":" + entity + ":" + attribute);
            conflict.setOldValue(oldValue);
            conflict.setNewValue(newValue);
            conflict.setConflictScore(100);
            conflict.setConflictType(MemoryConflict.ConflictType.CONTRADICTION.name());
            conflict.setReviewStatus(MemoryConflict.ReviewStatus.PENDING.name());
            conflict.setAiAnalysis(aiAnalysis);
            conflict.setCreatedAt(LocalDateTime.now());
            
            conflictMapper.insert(conflict);
            logger.warn("Attribute conflict detected and recorded - id={}, category={}, entity={}, attribute={}", 
                conflict.getId(), category, entity, attribute);
            
            return ConflictDetectionResult.conflict(conflict);
        }

        double similarity = calculateSemanticSimilarity(oldValue, newValue);
        if (similarity < CONFLICT_THRESHOLD) {
            String aiAnalysis = analyzeWithAI(category + ":" + entity + ":" + attribute, oldValue, newValue);
            
            MemoryConflict conflict = new MemoryConflict();
            conflict.setUserId(userId);
            conflict.setFactId(factId);
            conflict.setKey(category + ":" + entity + ":" + attribute);
            conflict.setOldValue(oldValue);
            conflict.setNewValue(newValue);
            conflict.setConflictScore((int) (100 - similarity * 100));
            conflict.setConflictType(MemoryConflict.ConflictType.CONTRADICTION.name());
            conflict.setReviewStatus(MemoryConflict.ReviewStatus.PENDING.name());
            conflict.setAiAnalysis(aiAnalysis);
            conflict.setCreatedAt(LocalDateTime.now());
            
            conflictMapper.insert(conflict);
            logger.warn("Semantic conflict detected and recorded - id={}, category={}, entity={}, attribute={}", 
                conflict.getId(), category, entity, attribute);
            
            return ConflictDetectionResult.conflict(conflict);
        }

        return ConflictDetectionResult.update();
    }

    private boolean isContradictoryValue(String oldValue, String newValue) {
        String combined = oldValue + newValue;
        for (String pair : CONTRADICTION_PAIRS) {
            String word1 = pair.substring(0, pair.length() / 2);
            String word2 = pair.substring(pair.length() / 2);
            
            boolean hasWord1 = combined.contains(word1);
            boolean hasWord2 = combined.contains(word2);
            
            boolean oldHasWord1 = oldValue.contains(word1);
            boolean oldHasWord2 = oldValue.contains(word2);
            boolean newHasWord1 = newValue.contains(word1);
            boolean newHasWord2 = newValue.contains(word2);
            
            if ((oldHasWord1 && newHasWord2) || (oldHasWord2 && newHasWord1)) {
                return true;
            }
        }
        return false;
    }

    private String extractTopic(String value) {
        Matcher matcher = INTEREST_PATTERN.matcher(value);
        if (matcher.find()) {
            String topic = matcher.group(2).trim();
            if (!topic.isEmpty()) {
                return topic;
            }
        }
        return value;
    }

    private boolean isSameTopic(String topic1, String topic2) {
        if (topic1 == null || topic2 == null) {
            return false;
        }
        
        if (topic1.equals(topic2)) {
            return true;
        }
        
        if (topic1.contains(topic2) || topic2.contains(topic1)) {
            return true;
        }
        
        double similarity = calculateSemanticSimilarity(topic1, topic2);
        logger.info("Topic similarity - topic1={}, topic2={}, similarity={}", topic1, topic2, similarity);
        
        return similarity >= TOPIC_SIMILARITY_THRESHOLD;
    }

    private boolean containsContradictionWords(String oldValue, String newValue) {
        for (String pair : CONTRADICTION_PAIRS) {
            String word1 = pair.substring(0, pair.length() / 2);
            String word2 = pair.substring(pair.length() / 2);
            
            boolean oldHasWord1 = oldValue.contains(word1);
            boolean oldHasWord2 = oldValue.contains(word2);
            boolean newHasWord1 = newValue.contains(word1);
            boolean newHasWord2 = newValue.contains(word2);
            
            if ((oldHasWord1 && newHasWord2) || (oldHasWord2 && newHasWord1)) {
                return true;
            }
        }
        return false;
    }

    private double calculateSemanticSimilarity(String text1, String text2) {
        try {
            var embeddings1 = embeddingModel.embed(List.of(text1));
            var embeddings2 = embeddingModel.embed(List.of(text2));
            
            if (embeddings1.isEmpty() || embeddings2.isEmpty()) {
                logger.warn("Embedding returned empty result, falling back to string comparison");
                return calculateStringSimilarity(text1, text2);
            }

            float[] vector1 = embeddings1.get(0);
            float[] vector2 = embeddings2.get(0);

            return cosineSimilarity(vector1, vector2);
        } catch (Exception e) {
            logger.warn("Failed to calculate semantic similarity: {}, falling back to string comparison", e.getMessage());
            return calculateStringSimilarity(text1, text2);
        }
    }

    private double calculateStringSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        
        String t1 = text1.toLowerCase();
        String t2 = text2.toLowerCase();
        
        if (t1.equals(t2)) {
            return 1.0;
        }
        
        int maxLen = Math.max(t1.length(), t2.length());
        int commonChars = 0;
        
        for (char c : t1.toCharArray()) {
            if (t2.indexOf(c) >= 0) {
                commonChars++;
            }
        }
        
        return (double) commonChars / maxLen;
    }

    private double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private ConflictDetectionResult detectContradiction(Long userId, Long factId, String key, 
                                                        String oldValue, String newValue, double similarity) {
        String aiAnalysis = analyzeWithAI(key, oldValue, newValue);
        
        MemoryConflict conflict = new MemoryConflict();
        conflict.setUserId(userId);
        conflict.setFactId(factId);
        conflict.setKey(key);
        conflict.setOldValue(oldValue);
        conflict.setNewValue(newValue);
        conflict.setConflictScore((int) (100 - similarity * 100));
        conflict.setConflictType(MemoryConflict.ConflictType.CONTRADICTION.name());
        conflict.setReviewStatus(MemoryConflict.ReviewStatus.PENDING.name());
        conflict.setAiAnalysis(aiAnalysis);
        conflict.setCreatedAt(LocalDateTime.now());
        
        conflictMapper.insert(conflict);
        logger.warn("Conflict detected and recorded - id={}, key={}, oldValue={}, newValue={}", 
            conflict.getId(), key, oldValue, newValue);
        
        return ConflictDetectionResult.conflict(conflict);
    }

    private String analyzeWithAI(String key, String oldValue, String newValue) {
        try {
            String prompt = String.format("""
                你是一个记忆冲突分析助手。请分析以下两个关于用户的事实是否存在冲突：
                
                Key: %s
                旧值: %s
                新值: %s
                
                请判断：
                1. 是否存在语义冲突？（只有当两者描述同一主题时才可能冲突）
                2. 可能的原因是什么？（例如：用户改变主意、输入错误、信息更新等）
                3. 建议如何处理？
                
                请用简洁的中文回答。
                """, key, oldValue, newValue);

            return aiService.generateResponse(prompt);
        } catch (Exception e) {
            logger.warn("AI analysis failed: {}", e.getMessage());
            return "AI分析失败，请人工确认";
        }
    }

    @Transactional
    public void resolveConflict(Long conflictId, String resolution, Long userId) {
        MemoryConflict conflict = conflictMapper.selectById(conflictId);
        if (conflict == null || !conflict.getUserId().equals(userId)) {
            return;
        }

        conflict.setReviewStatus(MemoryConflict.ReviewStatus.RESOLVED.name());
        conflict.setReviewResult(resolution);
        conflict.setReviewedAt(LocalDateTime.now());
        conflictMapper.updateById(conflict);
        
        logger.info("Conflict resolved - id={}, result={}", conflictId, resolution);
    }

    @Transactional
    public void saveFactHistory(MemoryFact fact, String changeReason) {
        MemoryFactHistory history = new MemoryFactHistory();
        history.setFactId(fact.getId());
        history.setUserId(fact.getUserId());
        history.setKey(fact.getKey());
        history.setValue(fact.getValue());
        
        Integer maxVersion = factHistoryMapper.selectMaxVersion(fact.getId());
        history.setVersion(maxVersion == null ? 1 : maxVersion + 1);
        
        history.setImportance(fact.getImportance());
        history.setConfidence(fact.getConfidence());
        history.setSourceQuote(fact.getSourceQuote());
        history.setChangeReason(changeReason);
        history.setCreatedAt(LocalDateTime.now());
        
        factHistoryMapper.insert(history);
        logger.info("Fact history saved - factId={}, version={}", fact.getId(), history.getVersion());
    }

    public List<MemoryConflict> getPendingConflicts(Long userId) {
        return conflictMapper.selectPendingByUserId(userId);
    }

    public List<MemoryConflict> getAllConflicts(Long userId) {
        return conflictMapper.selectByUserId(userId);
    }

    public int countPendingConflicts(Long userId) {
        return conflictMapper.countPendingByUserId(userId);
    }

    public static class ConflictDetectionResult {
        private final boolean hasConflict;
        private final boolean shouldUpdate;
        private final MemoryConflict conflict;
        private final String reason;

        private ConflictDetectionResult(boolean hasConflict, boolean shouldUpdate, MemoryConflict conflict, String reason) {
            this.hasConflict = hasConflict;
            this.shouldUpdate = shouldUpdate;
            this.conflict = conflict;
            this.reason = reason;
        }

        public static ConflictDetectionResult noConflict() {
            return new ConflictDetectionResult(false, true, null, "无冲突");
        }

        public static ConflictDetectionResult conflict(MemoryConflict conflict) {
            return new ConflictDetectionResult(true, false, conflict, "检测到冲突");
        }

        public static ConflictDetectionResult update() {
            return new ConflictDetectionResult(false, true, null, "值相似，可更新");
        }

        public static ConflictDetectionResult change(String oldValue, String newValue) {
            return new ConflictDetectionResult(false, true, null, "值有变化");
        }

        public boolean hasConflict() {
            return hasConflict;
        }

        public boolean shouldUpdate() {
            return shouldUpdate;
        }

        public MemoryConflict getConflict() {
            return conflict;
        }

        public String getReason() {
            return reason;
        }
    }
}
