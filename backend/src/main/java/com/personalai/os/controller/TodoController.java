package com.personalai.os.controller;

import com.personalai.os.dto.request.TodoRequest;
import com.personalai.os.entity.MemoryTodo;
import com.personalai.os.service.MemoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: 待办记忆控制器，处理待办事项的增删改查
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory/todo")
public class TodoController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping
    public ResponseEntity<List<MemoryTodo>> getTodos(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryTodo> todos = memoryService.getTodos(userId);
        return ResponseEntity.ok(todos);
    }

    @PostMapping
    public ResponseEntity<MemoryTodo> createTodo(HttpServletRequest request, @RequestBody TodoRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTodo todo = memoryService.createTodo(userId, body.getTitle(), body.getPriority(), body.getDueDate());
        return ResponseEntity.ok(todo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryTodo> updateTodo(HttpServletRequest request, @PathVariable Long id, 
                                                  @RequestBody TodoRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryTodo todo = memoryService.updateTodo(userId, id, body.getTitle(), body.getCompleted(), 
                body.getPriority(), null);
        return ResponseEntity.ok(todo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        memoryService.deleteTodo(userId, id);
        return ResponseEntity.ok().build();
    }
}
