package com.personalai.os.controller;

import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.service.MemoryAttributeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 记忆属性控制器，处理记忆属性的增删改查和搜索
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory/attribute")
public class MemoryAttributeController {

    @Autowired
    private MemoryAttributeService memoryAttributeService;

    @GetMapping
    public ResponseEntity<List<MemoryAttribute>> getAttributes(HttpServletRequest request,
                                                               @RequestParam(required = false) String category) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryAttribute> attributes;
        if (category != null && !category.isEmpty()) {
            attributes = memoryAttributeService.getAttributesByCategory(userId, category);
        } else {
            attributes = memoryAttributeService.getAttributesByUserId(userId);
        }
        return ResponseEntity.ok(attributes);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> categories = memoryAttributeService.getCategories(userId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/entities")
    public ResponseEntity<List<String>> getEntities(HttpServletRequest request,
                                                    @RequestParam String category) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> entities = memoryAttributeService.getEntities(userId, category);
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MemoryAttribute>> search(HttpServletRequest request,
                                                        @RequestParam String keyword) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryAttribute> results = memoryAttributeService.search(userId, keyword);
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<MemoryAttribute> createAttribute(HttpServletRequest request,
                                                           @RequestBody AttributeRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        memoryAttributeService.writeAttribute(userId, body.getCategory(), body.getEntity(),
                body.getAttribute(), body.getValue(), body.getImportance(),
                body.getConfidence(), body.getSourceQuote());
        
        MemoryAttribute created = memoryAttributeService.getAttribute(userId, body.getCategory(),
                body.getEntity(), body.getAttribute());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryAttribute> updateAttribute(HttpServletRequest request,
                                                           @PathVariable Long id,
                                                           @RequestBody AttributeRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        memoryAttributeService.writeAttribute(userId, body.getCategory(), body.getEntity(),
                body.getAttribute(), body.getValue(), body.getImportance(),
                body.getConfidence(), body.getSourceQuote());
        
        MemoryAttribute updated = memoryAttributeService.getAttribute(userId, body.getCategory(),
                body.getEntity(), body.getAttribute());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttribute(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        memoryAttributeService.deleteAttribute(userId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> summary = new HashMap<>();
        
        List<String> categories = memoryAttributeService.getCategories(userId);
        for (String category : categories) {
            List<MemoryAttribute> attrs = memoryAttributeService.getAttributesByCategory(userId, category);
            summary.put(category.toLowerCase() + "Count", attrs.size());
        }
        
        return ResponseEntity.ok(summary);
    }

    public static class AttributeRequest {
        private String category;
        private String entity;
        private String attribute;
        private String value;
        private Integer importance;
        private Integer confidence;
        private String sourceQuote;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getEntity() { return entity; }
        public void setEntity(String entity) { this.entity = entity; }
        public String getAttribute() { return attribute; }
        public void setAttribute(String attribute) { this.attribute = attribute; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public Integer getImportance() { return importance; }
        public void setImportance(Integer importance) { this.importance = importance; }
        public Integer getConfidence() { return confidence; }
        public void setConfidence(Integer confidence) { this.confidence = confidence; }
        public String getSourceQuote() { return sourceQuote; }
        public void setSourceQuote(String sourceQuote) { this.sourceQuote = sourceQuote; }
    }
}