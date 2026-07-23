package com.personalai.os.service;

import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryReference;
import com.personalai.os.mapper.MemoryAttributeMapper;
import com.personalai.os.mapper.MemoryFactMapper;
import com.personalai.os.mapper.MemoryReferenceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 记忆重要性服务类，根据用户反馈动态调整记忆的重要性评分
 * @author: 琦
 */
@Service
public class MemoryImportanceService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryImportanceService.class);

    private static final int IMPORTANCE_INCREMENT = 1;
    private static final int MAX_IMPORTANCE = 100;

    @Autowired
    private MemoryFactMapper factMapper;

    @Autowired
    private MemoryAttributeMapper attributeMapper;

    @Autowired
    private MemoryReferenceMapper referenceMapper;

    @Async("taskExecutor")
    @Transactional
    public void processImportanceFeedback(Long userId, Long sessionId, List<MemoryFact> providedFacts, String aiResponse) {
        if (providedFacts == null || providedFacts.isEmpty() || aiResponse == null) {
            return;
        }

        logger.info("Processing importance feedback for user {}, session {}, {} facts provided", userId, sessionId, providedFacts.size());

        for (MemoryFact fact : providedFacts) {
            if (isFactReferenced(fact, aiResponse)) {
                updateFactImportance(fact);
                recordReference(userId, sessionId, fact, aiResponse);
                logger.info("Fact referenced and importance updated - key={}, newImportance={}", fact.getKey(), fact.getImportance());
            }
        }
    }

    private boolean isFactReferenced(MemoryFact fact, String aiResponse) {
        String factText = fact.getKey() + " " + fact.getValue();
        String responseLower = aiResponse.toLowerCase();
        String factLower = factText.toLowerCase();

        String[] factWords = factLower.split("[\\s,，。！？、；:：]+");
        
        int matchedWords = 0;
        int significantWords = 0;
        
        for (String word : factWords) {
            if (word.length() >= 2) {
                significantWords++;
                if (responseLower.contains(word)) {
                    matchedWords++;
                }
            }
        }
        
        if (significantWords == 0) {
            return false;
        }
        
        double matchRatio = (double) matchedWords / significantWords;
        logger.debug("Fact reference check - key={}, matchRatio={}", fact.getKey(), matchRatio);
        
        if (matchRatio >= 0.5) {
            return true;
        }
        
        Pattern pattern = Pattern.compile(Pattern.quote(fact.getKey()), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            return true;
        }
        
        return false;
    }

    @Transactional
    public void updateFactImportance(MemoryFact fact) {
        int newImportance = fact.getImportance() + IMPORTANCE_INCREMENT;
        if (newImportance > MAX_IMPORTANCE) {
            newImportance = MAX_IMPORTANCE;
        }
        
        fact.setImportance(newImportance);
        factMapper.updateById(fact);
        
        logger.info("Fact importance updated - key={}, importance={}", fact.getKey(), newImportance);
    }

    private void recordReference(Long userId, Long sessionId, MemoryFact fact, String aiResponse) {
        MemoryReference reference = new MemoryReference();
        reference.setUserId(userId);
        reference.setSessionId(sessionId);
        reference.setFactId(fact.getId());
        reference.setKey(fact.getKey());
        reference.setReferencedContent(fact.getValue());
        reference.setAiResponse(aiResponse.length() > 500 ? aiResponse.substring(0, 500) : aiResponse);
        reference.setImportanceGain(IMPORTANCE_INCREMENT);
        reference.setReferencedAt(LocalDateTime.now());
        
        referenceMapper.insert(reference);
        logger.info("Memory reference recorded - factId={}, referenceId={}", fact.getId(), reference.getId());
    }

    public List<MemoryReference> getReferencesByUserId(Long userId) {
        return referenceMapper.selectByUserId(userId);
    }

    public List<MemoryReference> getReferencesByFactId(Long factId) {
        return referenceMapper.selectByFactId(factId);
    }

    public int getReferenceCount(Long factId) {
        Integer count = referenceMapper.countReferencesByFactId(factId);
        return count != null ? count : 0;
    }

    public int getTotalImportanceGain(Long factId) {
        Integer gain = referenceMapper.sumImportanceGainByFactId(factId);
        return gain != null ? gain : 0;
    }

    @Async("taskExecutor")
    @Transactional
    public void processAttributeImportanceFeedback(Long userId, Long sessionId, List<MemoryAttribute> providedAttributes, String aiResponse) {
        if (providedAttributes == null || providedAttributes.isEmpty() || aiResponse == null) {
            return;
        }

        logger.info("Processing attribute importance feedback for user {}, session {}, {} attributes provided", userId, sessionId, providedAttributes.size());

        for (MemoryAttribute attr : providedAttributes) {
            if (isAttributeReferenced(attr, aiResponse)) {
                updateAttributeImportance(attr);
                recordAttributeReference(userId, sessionId, attr, aiResponse);
                logger.info("Attribute referenced and importance updated - category={}, entity={}, attribute={}, newImportance={}", 
                    attr.getCategory(), attr.getEntity(), attr.getAttribute(), attr.getImportance());
            }
        }
    }

    private boolean isAttributeReferenced(MemoryAttribute attr, String aiResponse) {
        String attrText = (attr.getCategory() != null ? attr.getCategory() : "") + " " + 
                          (attr.getEntity() != null ? attr.getEntity() : "") + " " + 
                          (attr.getAttribute() != null ? attr.getAttribute() : "") + " " + 
                          (attr.getValue() != null ? attr.getValue() : "");
        String responseLower = aiResponse.toLowerCase();
        String attrLower = attrText.toLowerCase();

        String[] attrWords = attrLower.split("[\\s,，。！？、；:：]+");
        
        int matchedWords = 0;
        int significantWords = 0;
        
        for (String word : attrWords) {
            if (word.length() >= 2) {
                significantWords++;
                if (responseLower.contains(word)) {
                    matchedWords++;
                }
            }
        }
        
        if (significantWords == 0) {
            return false;
        }
        
        double matchRatio = (double) matchedWords / significantWords;
        logger.debug("Attribute reference check - entity={}, matchRatio={}", attr.getEntity(), matchRatio);
        
        if (matchRatio >= 0.5) {
            return true;
        }
        
        if (attr.getEntity() != null) {
            Pattern pattern = Pattern.compile(Pattern.quote(attr.getEntity()), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(aiResponse);
            if (matcher.find()) {
                return true;
            }
        }
        
        if (attr.getValue() != null) {
            Pattern pattern = Pattern.compile(Pattern.quote(attr.getValue()), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(aiResponse);
            if (matcher.find()) {
                return true;
            }
        }
        
        return false;
    }

    @Transactional
    public void updateAttributeImportance(MemoryAttribute attr) {
        int newImportance = attr.getImportance() != null ? attr.getImportance() + IMPORTANCE_INCREMENT : IMPORTANCE_INCREMENT;
        if (newImportance > MAX_IMPORTANCE) {
            newImportance = MAX_IMPORTANCE;
        }
        
        attr.setImportance(newImportance);
        attributeMapper.updateById(attr);
        
        logger.info("Attribute importance updated - category={}, entity={}, attribute={}, importance={}", 
            attr.getCategory(), attr.getEntity(), attr.getAttribute(), newImportance);
    }

    private void recordAttributeReference(Long userId, Long sessionId, MemoryAttribute attr, String aiResponse) {
        MemoryReference reference = new MemoryReference();
        reference.setUserId(userId);
        reference.setSessionId(sessionId);
        reference.setFactId(attr.getId());
        reference.setKey(attr.getCategory() + ":" + attr.getEntity() + ":" + attr.getAttribute());
        reference.setReferencedContent(attr.getValue());
        reference.setAiResponse(aiResponse.length() > 500 ? aiResponse.substring(0, 500) : aiResponse);
        reference.setImportanceGain(IMPORTANCE_INCREMENT);
        reference.setReferencedAt(LocalDateTime.now());
        
        referenceMapper.insert(reference);
        logger.info("Attribute reference recorded - attrId={}, referenceId={}", attr.getId(), reference.getId());
    }
}
