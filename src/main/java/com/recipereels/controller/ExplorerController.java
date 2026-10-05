package com.recipereels.controller;

import com.recipereels.dto.ApiResponse;
import com.recipereels.dto.RatingDTO;
import com.recipereels.dto.SavedRecipeDTO;
import com.recipereels.security.UserPrincipal;
import com.recipereels.service.ExplorerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/explorer")
public class ExplorerController {

    private final ExplorerService explorerService;

    public ExplorerController(ExplorerService explorerService) {
        this.explorerService = explorerService;
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse> getDashboardSummary(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Map<String, Object> summary = explorerService.getExplorerDashboardSummary(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary loaded", summary));
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse> getSavedRecipes(
            @RequestParam(required = false) String collectionName,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<SavedRecipeDTO> saved = explorerService.getSavedRecipes(userPrincipal.getUser(), collectionName);
        return ResponseEntity.ok(ApiResponse.success("Saved recipes retrieved", saved));
    }

    @GetMapping("/collections")
    public ResponseEntity<ApiResponse> getCollections(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<String> collections = explorerService.getUserCollections(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Collections retrieved", collections));
    }

    @PostMapping("/collections/move")
    public ResponseEntity<ApiResponse> moveRecipeToCollection(
            @RequestParam Long recipeId,
            @RequestParam String collectionName,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        explorerService.moveRecipeToCollection(recipeId, collectionName, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Recipe collection updated successfully"));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse> getUserReviews(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<RatingDTO> reviews = explorerService.getUserReviews(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("My reviews retrieved", reviews));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        explorerService.deleteUserReview(id, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse> getBrowsingHistory(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<SavedRecipeDTO> history = explorerService.getUserBrowsingHistory(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Browsing history retrieved", history));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse> clearBrowsingHistory(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        explorerService.clearBrowsingHistory(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Browsing history cleared"));
    }
}
