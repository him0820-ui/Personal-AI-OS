package com.personalai.os.controller;

import com.personalai.os.dto.request.FactRequest;
import com.personalai.os.entity.MemoryFact;
import com.personalai.os.service.MemoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description: 事实记忆控制器，处理事实类型记忆的增删改查
 * @author: 琦
 */
@RestController
@RequestMapping("/api/memory/fact")
public class FactController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping
    public ResponseEntity<List<MemoryFact>> getFacts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<MemoryFact> facts = memoryService.getFacts(userId);
        return ResponseEntity.ok(facts);
    }

    @PostMapping
    public ResponseEntity<MemoryFact> createFact(HttpServletRequest request, @RequestBody FactRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryFact fact = memoryService.createFact(userId, body.getKey(), body.getValue(), body.getScore());
        return ResponseEntity.ok(fact);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryFact> updateFact(HttpServletRequest request, @PathVariable Long id, 
                                                  @RequestBody FactRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        MemoryFact fact = memoryService.updateFact(userId, id, body.getKey(), body.getValue(), body.getScore());
        return ResponseEntity.ok(fact);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFact(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        memoryService.deleteFact(userId, id);
        return ResponseEntity.ok().build();
    }
}
