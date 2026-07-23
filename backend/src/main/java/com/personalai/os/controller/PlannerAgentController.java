package com.personalai.os.controller;

import com.personalai.os.agent.PlannerAgent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @description: 规划代理控制器，处理智能规划代理的目标分解和计划生成
 * @author: 琦
 */
@RestController
@RequestMapping("/api/planner-agent")
public class PlannerAgentController {

    @Autowired
    private PlannerAgent plannerAgent;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> execute(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String userRequest = body.get("message");
        
        String result = plannerAgent.execute(userId, userRequest);
        
        Map<String, String> response = new HashMap<>();
        response.put("response", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/decompose-goal")
    public ResponseEntity<Map<String, String>> decomposeGoal(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String goal = body.get("goal");
        
        String result = plannerAgent.execute(userId, goal);
        
        Map<String, String> response = new HashMap<>();
        response.put("response", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update-progress")
    public ResponseEntity<Map<String, String>> updateProgress(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String updateInfo = body.get("update");
        
        String result = plannerAgent.execute(userId, updateInfo);
        
        Map<String, String> response = new HashMap<>();
        response.put("response", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-plan")
    public ResponseEntity<Map<String, String>> generatePlan(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        String result = plannerAgent.execute(userId, "生成明日计划");
        
        Map<String, String> response = new HashMap<>();
        response.put("response", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-summary")
    public ResponseEntity<Map<String, String>> generateSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        String result = plannerAgent.execute(userId, "生成每日总结");
        
        Map<String, String> response = new HashMap<>();
        response.put("response", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/get-recommendations")
    public ResponseEntity<Map<String, String>> getRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        String result = plannerAgent.execute(userId, "推荐下一步应该做的任务");
        
        Map<String, String> response = new HashMap<>();
        response.put("response", result);
        return ResponseEntity.ok(response);
    }
}
