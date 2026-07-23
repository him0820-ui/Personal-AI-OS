package com.personalai.os.controller;

import com.personalai.os.entity.MemorySummary;
import com.personalai.os.service.MemorySummaryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @description: 记忆总结控制器，处理用户记忆总结的获取和刷新
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory/summary")
public class MemorySummaryController {

    @Autowired
    private MemorySummaryService memorySummaryService;

    @GetMapping
    public ResponseEntity<MemorySummary> getSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MemorySummary summary = memorySummaryService.getSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/refresh")
    public ResponseEntity<MemorySummary> refreshSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        MemorySummary summary = memorySummaryService.updateSummary(userId);
        return ResponseEntity.ok(summary);
    }
}
