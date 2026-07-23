package com.personalai.os.controller;

import com.personalai.os.dto.response.DailySummaryResponse;
import com.personalai.os.dto.response.TaskRecommendationResponse;
import com.personalai.os.dto.response.TomorrowPlanResponse;
import com.personalai.os.service.PlannerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @description: 规划控制器，处理每日总结、明日计划和任务推荐
 * @author: 琦
 */
@RestController
@RequestMapping("/api/planner")
public class PlannerController {

    @Autowired
    private PlannerService plannerService;

    @GetMapping("/summary")
    public ResponseEntity<DailySummaryResponse> getSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        DailySummaryResponse summary = plannerService.generateSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/generate")
    public ResponseEntity<DailySummaryResponse> generateSummary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        DailySummaryResponse summary = plannerService.generateSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/tomorrow-plan")
    public ResponseEntity<TomorrowPlanResponse> getTomorrowPlan(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        TomorrowPlanResponse plan = plannerService.generateTomorrowPlan(userId);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/tomorrow-plan/generate")
    public ResponseEntity<TomorrowPlanResponse> generateTomorrowPlan(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        TomorrowPlanResponse plan = plannerService.generateTomorrowPlan(userId);
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<TaskRecommendationResponse> getRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        TaskRecommendationResponse recommendations = plannerService.generateTaskRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/recommendations/generate")
    public ResponseEntity<TaskRecommendationResponse> generateRecommendations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        TaskRecommendationResponse recommendations = plannerService.generateTaskRecommendations(userId);
        return ResponseEntity.ok(recommendations);
    }
}