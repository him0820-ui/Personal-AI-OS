package com.personalai.os.controller;

import com.personalai.os.dto.request.GoalRequest;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.service.MemoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: 目标记忆控制器，处理目标类型记忆的增删改查
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory/goal")
public class GoalController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping
    public ResponseEntity<List<MemoryGoal>> getGoals(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryGoal> goals = memoryService.getGoals(userId);
        return ResponseEntity.ok(goals);
    }

    @PostMapping
    public ResponseEntity<MemoryGoal> createGoal(HttpServletRequest request, @RequestBody GoalRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryGoal goal = memoryService.createGoal(userId, body.getTitle(), body.getDescription(), 
                body.getProgress(), body.getPriority(), null, "ACTIVE");
        return ResponseEntity.ok(goal);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryGoal> updateGoal(HttpServletRequest request, @PathVariable Long id, 
                                                  @RequestBody GoalRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryGoal goal = memoryService.updateGoal(userId, id, body.getTitle(), body.getDescription(), 
                body.getProgress(), body.getPriority(), null, "ACTIVE");
        return ResponseEntity.ok(goal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        memoryService.deleteGoal(userId, id);
        return ResponseEntity.ok().build();
    }
}
