package com.personalai.os.controller;

import com.personalai.os.dto.request.TimelineRequest;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.service.MemoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: 时间线记忆控制器，处理时间线事件的增删改查
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory/timeline")
public class TimelineController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping
    public ResponseEntity<List<MemoryTimeline>> getTimeline(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryTimeline> timeline = memoryService.getTimeline(userId);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping
    public ResponseEntity<MemoryTimeline> createTimeline(HttpServletRequest request, @RequestBody TimelineRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTimeline timeline = memoryService.createTimeline(userId, body.getTitle(), body.getDescription(), body.getTimestamp());
        return ResponseEntity.ok(timeline);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryTimeline> updateTimeline(HttpServletRequest request, @PathVariable Long id,
                                                         @RequestBody TimelineRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTimeline timeline = memoryService.updateTimeline(userId, id, body.getTitle(), body.getDescription(), body.getTimestamp());
        return ResponseEntity.ok(timeline);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeline(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        memoryService.deleteTimeline(userId, id);
        return ResponseEntity.ok().build();
    }
}
