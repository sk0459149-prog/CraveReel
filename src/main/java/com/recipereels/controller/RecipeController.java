package com.recipereels.controller;

import com.recipereels.dto.*;
import com.recipereels.entity.User;
import com.recipereels.security.UserPrincipal;
import com.recipereels.service.InteractionService;
import com.recipereels.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final InteractionService interactionService;

    public RecipeController(RecipeService recipeService, InteractionService interactionService) {
        this.recipeService = recipeService;
        this.interactionService = interactionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getRecipes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isVegetarian,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer maxCookTime,
            @RequestParam(defaultValue = "newest") String sortBy,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        List<RecipeDTO> recipes = recipeService.getPublishedRecipes(query, categoryId, isVegetarian, difficulty, maxCookTime, sortBy, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Recipes retrieved successfully", recipes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getRecipeById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        RecipeDTO recipe = recipeService.getRecipeById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully", recipe));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse> getTrendingRecipes(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        List<RecipeDTO> trending = recipeService.getTrendingRecipes(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Trending recipes", trending));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse> getRecentRecipes(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        List<RecipeDTO> recent = recipeService.getRecentRecipes(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Recent recipes", recent));
    }

    @GetMapping("/quick")
    public ResponseEntity<ApiResponse> getQuickRecipes(
            @RequestParam(defaultValue = "30") int maxMinutes,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        List<RecipeDTO> quick = recipeService.getQuickRecipes(maxMinutes, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Quick recipes", quick));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse> recordView(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        recipeService.recordRecipeView(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("View recorded"));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse> toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        boolean liked = interactionService.toggleLike(id, userPrincipal.getUser());
        String msg = liked ? "Recipe liked" : "Recipe unliked";
        return ResponseEntity.ok(ApiResponse.success(msg, Map.of("liked", liked)));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<ApiResponse> saveRecipe(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Favorites") String collectionName,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        SavedRecipeDTO saved = interactionService.saveRecipe(id, collectionName, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Recipe collection updated successfully", saved));
    }

    @DeleteMapping("/{id}/save")
    public ResponseEntity<ApiResponse> removeSaved(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        interactionService.removeSavedRecipe(id, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Recipe collection updated successfully"));
    }

    @PostMapping("/{id}/ratings")
    public ResponseEntity<ApiResponse> rateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RatingDTO ratingDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RatingDTO result = interactionService.rateRecipe(id, ratingDTO.getStars(), ratingDTO.getReviewText(), userPrincipal.getUser());
        String msg = (ratingDTO.getReviewText() != null && !ratingDTO.getReviewText().trim().isEmpty())
                ? "Review submitted successfully"
                : "Rating submitted successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }

    @GetMapping("/{id}/ratings")
    public ResponseEntity<ApiResponse> getRatings(@PathVariable Long id) {
        List<RatingDTO> ratings = interactionService.getRecipeRatings(id);
        return ResponseEntity.ok(ApiResponse.success("Ratings retrieved", ratings));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateDTO commentDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CommentDTO comment = interactionService.addComment(id, commentDto, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Comment added successfully", comment));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse> getComments(@PathVariable Long id) {
        List<CommentDTO> comments = interactionService.getRecipeComments(id);
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved", comments));
    }
}
