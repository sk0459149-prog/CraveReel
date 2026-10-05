package com.recipereels.controller;

import com.recipereels.dto.*;
import com.recipereels.entity.RecipeStatus;
import com.recipereels.entity.User;
import com.recipereels.repository.*;
import com.recipereels.security.UserPrincipal;
import com.recipereels.service.RecipeService;
import com.recipereels.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contributor")
public class ContributorController {

    private final RecipeService recipeService;
    private final StorageService storageService;
    private final RecipeRepository recipeRepository;
    private final RecipeReelRepository reelRepository;
    private final RatingRepository ratingRepository;
    private final RecipeLikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final DirectMessageRepository messageRepository;

    public ContributorController(RecipeService recipeService,
                                 StorageService storageService,
                                 RecipeRepository recipeRepository,
                                 RecipeReelRepository reelRepository,
                                 RatingRepository ratingRepository,
                                 RecipeLikeRepository likeRepository,
                                 CommentRepository commentRepository,
                                 DirectMessageRepository messageRepository) {
        this.recipeService = recipeService;
        this.storageService = storageService;
        this.recipeRepository = recipeRepository;
        this.reelRepository = reelRepository;
        this.ratingRepository = ratingRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getStats(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();

        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalRecipes(recipeRepository.countByUser(user));
        stats.setPendingRecipes(recipeRepository.findByUserAndStatus(user, RecipeStatus.PENDING).size());
        stats.setApprovedRecipes(recipeRepository.findByUserAndStatus(user, RecipeStatus.APPROVED).size());
        stats.setRejectedRecipes(recipeRepository.findByUserAndStatus(user, RecipeStatus.REJECTED).size());

        long views = recipeRepository.findByUser(user).stream()
                .mapToLong(r -> r.getViewCount() != null ? r.getViewCount() : 0L)
                .sum();
        stats.setTotalViews(views);

        Long likes = likeRepository.countLikesForContributor(user);
        stats.setTotalLikes(likes != null ? likes : 0L);

        Double avg = ratingRepository.getAverageRatingForContributor(user);
        stats.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);

        stats.setTotalComments(commentRepository.findCommentsOnContributorRecipes(user).size());
        stats.setUnreadMessages(messageRepository.countByRecipientAndIsReadFalse(user));

        return ResponseEntity.ok(ApiResponse.success("Contributor stats loaded", stats));
    }

    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse> getMyRecipes(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RecipeStatus recipeStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                recipeStatus = RecipeStatus.valueOf(status.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<RecipeDTO> recipes = recipeService.getContributorRecipes(userPrincipal.getUser(), recipeStatus);
        return ResponseEntity.ok(ApiResponse.success("My recipes retrieved", recipes));
    }

    @PostMapping("/recipes")
    public ResponseEntity<ApiResponse> createRecipe(
            @Valid @RequestBody RecipeCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RecipeDTO recipe = recipeService.createRecipe(createDTO, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Recipe submitted successfully", recipe));
    }

    @PutMapping("/recipes/{id}")
    public ResponseEntity<ApiResponse> updateRecipe(
            @PathVariable Long id,
            @Valid @RequestBody RecipeCreateDTO updateDTO,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RecipeDTO recipe = recipeService.updateRecipe(id, updateDTO, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Recipe updated successfully", recipe));
    }

    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<ApiResponse> deleteRecipe(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        recipeService.deleteRecipe(id, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Recipe deleted successfully"));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.storeImage(file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", Map.of("url", url)));
    }

    @PostMapping("/upload-video")
    public ResponseEntity<ApiResponse> uploadVideo(@RequestParam("file") MultipartFile file) {
        String url = storageService.storeVideo(file);
        return ResponseEntity.ok(ApiResponse.success("Video uploaded successfully", Map.of("url", url)));
    }

    @GetMapping("/interactions")
    public ResponseEntity<ApiResponse> getInteractions(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = userPrincipal.getUser();
        List<CommentDTO> comments = commentRepository.findCommentsOnContributorRecipes(user).stream().map(c -> {
            CommentDTO dto = new CommentDTO();
            dto.setId(c.getId());
            dto.setRecipeId(c.getRecipe().getId());
            dto.setContent(c.getContent());
            dto.setCreatedAt(c.getCreatedAt());
            dto.setUserId(c.getUser().getId());
            dto.setUserName(c.getUser().getName());
            dto.setUserAvatar(c.getUser().getAvatarUrl());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("comments", comments);
        data.put("inbox", messageRepository.findByRecipientOrderByCreatedAtDesc(user));
        data.put("sent", messageRepository.findBySenderOrderByCreatedAtDesc(user));

        return ResponseEntity.ok(ApiResponse.success("Interaction history retrieved", data));
    }
}
