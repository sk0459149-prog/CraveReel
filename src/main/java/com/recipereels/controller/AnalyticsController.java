package com.recipereels.controller;

import com.recipereels.analytics.RecipeAnalyticsService;
import com.recipereels.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final RecipeAnalyticsService analyticsService;

    public AnalyticsController(RecipeAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/top-reels")
    public ResponseEntity<ApiResponse> getTopReels(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "Top reels retrieved successfully",
                analyticsService.getTopReels(limit)));
    }
}
