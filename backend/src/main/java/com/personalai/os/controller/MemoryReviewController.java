package com.personalai.os.controller;

import com.personalai.os.entity.MemoryAttribute;
import com.personalai.os.entity.MemoryConflict;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryFactHistory;
import com.personalai.os.mapper.MemoryAttributeMapper;
import com.personalai.os.mapper.MemoryFactHistoryMapper;
import com.personalai.os.mapper.MemoryFactMapper;
import com.personalai.os.service.ConflictDetectionService;
import com.personalai.os.service.MemoryAttributeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description: 记忆审核控制器，处理冲突检测、低置信度记忆和来源异常审核
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory/review")
public class MemoryReviewController {

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    @Autowired
    private MemoryFactMapper factMapper;

    @Autowired
    private MemoryFactHistoryMapper factHistoryMapper;

    @Autowired
    private MemoryAttributeMapper attributeMapper;

    @Autowired
    private MemoryAttributeService memoryAttributeService;

    @GetMapping("/conflicts")
    public ResponseEntity<Map<String, Object>> getPendingConflicts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryConflict> conflicts = conflictDetectionService.getPendingConflicts(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("conflicts", conflicts);
        response.put("count", conflicts.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conflicts/all")
    public ResponseEntity<List<MemoryConflict>> getAllConflicts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryConflict> conflicts = conflictDetectionService.getAllConflicts(userId);
        return ResponseEntity.ok(conflicts);
    }

    @GetMapping("/conflicts/count")
    public ResponseEntity<Map<String, Integer>> getPendingCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = conflictDetectionService.countPendingConflicts(userId);
        
        Map<String, Integer> response = new HashMap<>();
        response.put("pendingCount", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conflicts/{id}/resolve")
    public ResponseEntity<MemoryConflict> resolveConflict(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        String resolution = body.get("resolution");
        
        conflictDetectionService.resolveConflict(id, resolution, userId);
        
        MemoryConflict conflict = conflictDetectionService.getAllConflicts(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
        
        return ResponseEntity.ok(conflict);
    }

    @PostMapping("/conflicts/{id}/overwrite")
    public ResponseEntity<MemoryConflict> overwriteFact(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        
        MemoryConflict conflict = conflictDetectionService.getAllConflicts(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
        
        if (conflict == null) {
            return ResponseEntity.notFound().build();
        }
        
        MemoryAttribute attr = attributeMapper.selectById(conflict.getFactId());
        if (attr != null && attr.getUserId().equals(userId)) {
            attr.setValue(conflict.getNewValue());
            attr.setConfidence(100);
            attributeMapper.updateById(attr);
        }
        
        conflictDetectionService.resolveConflict(id, "OVERWRITE", userId);
        return ResponseEntity.ok(conflict);
    }

    @PostMapping("/conflicts/{id}/keep-history")
    public ResponseEntity<MemoryConflict> keepHistory(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        
        MemoryConflict conflict = conflictDetectionService.getAllConflicts(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
        
        if (conflict == null) {
            return ResponseEntity.notFound().build();
        }
        
        conflictDetectionService.resolveConflict(id, "KEEP_HISTORY", userId);
        return ResponseEntity.ok(conflict);
    }

    @PostMapping("/conflicts/{id}/delete-new")
    public ResponseEntity<MemoryConflict> deleteNew(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        conflictDetectionService.resolveConflict(id, "DELETE_NEW", userId);
        
        MemoryConflict conflict = conflictDetectionService.getAllConflicts(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
        
        return ResponseEntity.ok(conflict);
    }

    @GetMapping("/low-confidence")
    public ResponseEntity<Map<String, Object>> getLowConfidenceAttributes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "50") Integer threshold) {
        
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);
        
        List<MemoryAttribute> lowConfidence = attributes.stream()
                .filter(a -> a.getConfidence() != null && a.getConfidence() < threshold)
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("attributes", lowConfidence);
        response.put("count", lowConfidence.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/source-anomaly")
    public ResponseEntity<Map<String, Object>> getSourceAnomalyAttributes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);
        
        List<MemoryAttribute> anomalies = attributes.stream()
                .filter(a -> a.getSourceQuote() == null || a.getSourceQuote().isEmpty())
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("attributes", anomalies);
        response.put("count", anomalies.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentAttributes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "24") Integer hours) {
        
        Long userId = (Long) request.getAttribute("userId");
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);
        
        List<MemoryAttribute> recent = attributes.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoffTime))
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("attributes", recent);
        response.put("count", recent.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/attribute/{id}/confirm")
    public ResponseEntity<MemoryAttribute> confirmAttribute(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        MemoryAttribute attr = attributeMapper.selectById(id);
        
        if (attr == null || !attr.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        attr.setConfidence(100);
        attr.setStatus("ACTIVE");
        attributeMapper.updateById(attr);
        
        return ResponseEntity.ok(attr);
    }

    @PostMapping("/attribute/{id}/reject")
    public ResponseEntity<MemoryAttribute> rejectAttribute(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        MemoryAttribute attr = attributeMapper.selectById(id);
        
        if (attr == null || !attr.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        attr.setStatus("DELETED");
        attributeMapper.updateById(attr);
        
        return ResponseEntity.ok(attr);
    }

    @PostMapping("/attribute/{id}/update")
    public ResponseEntity<MemoryAttribute> updateAttribute(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        MemoryAttribute attr = attributeMapper.selectById(id);
        
        if (attr == null || !attr.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        if (body.containsKey("value")) {
            attr.setValue((String) body.get("value"));
        }
        if (body.containsKey("importance")) {
            attr.setImportance(((Number) body.get("importance")).intValue());
        }
        if (body.containsKey("confidence")) {
            attr.setConfidence(((Number) body.get("confidence")).intValue());
        }
        if (body.containsKey("sourceQuote")) {
            attr.setSourceQuote((String) body.get("sourceQuote"));
        }
        
        attributeMapper.updateById(attr);
        
        return ResponseEntity.ok(attr);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getReviewSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("pendingConflicts", conflictDetectionService.countPendingConflicts(userId));
        
        List<MemoryAttribute> attributes = memoryAttributeService.getAttributesByUserId(userId);
        summary.put("lowConfidenceCount", attributes.stream()
                .filter(a -> a.getConfidence() != null && a.getConfidence() < 50)
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .count());
        
        summary.put("sourceAnomalyCount", attributes.stream()
                .filter(a -> a.getSourceQuote() == null || a.getSourceQuote().isEmpty())
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .count());
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        summary.put("recentCount", attributes.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(cutoffTime))
                .filter(a -> "ACTIVE".equals(a.getStatus()))
                .count());
        
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/fact/{factId}/history")
    public ResponseEntity<List<MemoryFactHistory>> getFactHistory(
            @PathVariable Long factId,
            HttpServletRequest request) {
        
        Long userId = (Long) request.getAttribute("userId");
        MemoryFact fact = factMapper.selectById(factId);
        
        if (fact == null || !fact.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<MemoryFactHistory> history = factHistoryMapper.selectByFactId(factId);
        return ResponseEntity.ok(history);
    }
}