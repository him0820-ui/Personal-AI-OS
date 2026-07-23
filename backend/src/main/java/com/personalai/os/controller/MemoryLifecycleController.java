package com.personalai.os.controller;

import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.service.MemoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: 记忆生命周期控制器，处理记忆的归档、恢复和删除操作
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryLifecycleController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping("/fact/status/{status}")
    public ResponseEntity<List<MemoryFact>> getFactsByStatus(HttpServletRequest request, 
                                                              @PathVariable String status) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryFact> facts = memoryService.getFactsByStatus(userId, status.toUpperCase());
        return ResponseEntity.ok(facts);
    }

    @PostMapping("/fact/{id}/archive")
    public ResponseEntity<MemoryFact> archiveFact(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryFact fact = memoryService.archiveFact(userId, id);
        return ResponseEntity.ok(fact);
    }

    @PostMapping("/fact/{id}/restore")
    public ResponseEntity<MemoryFact> restoreFact(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryFact fact = memoryService.restoreFact(userId, id);
        return ResponseEntity.ok(fact);
    }

    @PostMapping("/fact/{id}/delete")
    public ResponseEntity<MemoryFact> softDeleteFact(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryFact fact = memoryService.softDeleteFact(userId, id);
        return ResponseEntity.ok(fact);
    }

    @GetMapping("/goal/status/{status}")
    public ResponseEntity<List<MemoryGoal>> getGoalsByStatus(HttpServletRequest request, 
                                                              @PathVariable String status) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryGoal> goals = memoryService.getGoalsByStatus(userId, status.toUpperCase());
        return ResponseEntity.ok(goals);
    }

    @PostMapping("/goal/{id}/archive")
    public ResponseEntity<MemoryGoal> archiveGoal(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryGoal goal = memoryService.archiveGoal(userId, id);
        return ResponseEntity.ok(goal);
    }

    @PostMapping("/goal/{id}/restore")
    public ResponseEntity<MemoryGoal> restoreGoal(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryGoal goal = memoryService.restoreGoal(userId, id);
        return ResponseEntity.ok(goal);
    }

    @PostMapping("/goal/{id}/delete")
    public ResponseEntity<MemoryGoal> softDeleteGoal(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryGoal goal = memoryService.softDeleteGoal(userId, id);
        return ResponseEntity.ok(goal);
    }

    @GetMapping("/timeline/status/{status}")
    public ResponseEntity<List<MemoryTimeline>> getTimelinesByStatus(HttpServletRequest request, 
                                                                      @PathVariable String status) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryTimeline> timelines = memoryService.getTimelinesByStatus(userId, status.toUpperCase());
        return ResponseEntity.ok(timelines);
    }

    @PostMapping("/timeline/{id}/archive")
    public ResponseEntity<MemoryTimeline> archiveTimeline(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTimeline timeline = memoryService.archiveTimeline(userId, id);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/timeline/{id}/restore")
    public ResponseEntity<MemoryTimeline> restoreTimeline(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTimeline timeline = memoryService.restoreTimeline(userId, id);
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/timeline/{id}/delete")
    public ResponseEntity<MemoryTimeline> softDeleteTimeline(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTimeline timeline = memoryService.softDeleteTimeline(userId, id);
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/todo/status/{status}")
    public ResponseEntity<List<MemoryTodo>> getTodosByStatus(HttpServletRequest request, 
                                                              @PathVariable String status) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryTodo> todos = memoryService.getTodosByStatus(userId, status.toUpperCase());
        return ResponseEntity.ok(todos);
    }

    @PostMapping("/todo/{id}/archive")
    public ResponseEntity<MemoryTodo> archiveTodo(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTodo todo = memoryService.archiveTodo(userId, id);
        return ResponseEntity.ok(todo);
    }

    @PostMapping("/todo/{id}/restore")
    public ResponseEntity<MemoryTodo> restoreTodo(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTodo todo = memoryService.restoreTodo(userId, id);
        return ResponseEntity.ok(todo);
    }

    @PostMapping("/todo/{id}/delete")
    public ResponseEntity<MemoryTodo> softDeleteTodo(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTodo todo = memoryService.softDeleteTodo(userId, id);
        return ResponseEntity.ok(todo);
    }
}