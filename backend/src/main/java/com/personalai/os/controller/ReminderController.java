package com.personalai.os.controller;

import com.personalai.os.memory.ReminderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @description: 提醒控制器，处理提醒的添加、查询和清除
 * @author: 琦
 */
@RestController
@RequestMapping("/api/reminder")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingReminders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> reminders = reminderService.getPendingReminders(userId);
        return ResponseEntity.ok(reminders);
    }

    @GetMapping("/urgent")
    public ResponseEntity<List<Map<String, Object>>> getUrgentReminders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> reminders = reminderService.getUrgentReminders(userId);
        return ResponseEntity.ok(reminders);
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addReminder(HttpServletRequest request, 
                                            @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String title = (String) body.get("title");
        String message = (String) body.get("message");
        String dueDateStr = (String) body.get("dueDate");
        
        LocalDate dueDate = LocalDate.parse(dueDateStr);
        reminderService.addManualReminder(userId, title, message, dueDate);
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearReminders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reminderService.clearReminders(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check")
    public ResponseEntity<List<Map<String, Object>>> checkReminders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        List<Map<String, Object>> pending = reminderService.getPendingReminders(userId);
        List<Map<String, Object>> urgent = reminderService.getUrgentReminders(userId);
        
        pending.addAll(urgent);
        
        return ResponseEntity.ok(pending);
    }
}