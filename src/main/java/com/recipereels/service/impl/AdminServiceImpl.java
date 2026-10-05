package com.recipereels.service.impl;

import com.recipereels.dto.*;
import com.recipereels.entity.*;
import com.recipereels.exception.ResourceNotFoundException;
import com.recipereels.repository.*;
import com.recipereels.service.AdminService;
import com.recipereels.service.RecipeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeReelRepository reelRepository;
    private final CommentRepository commentRepository;
    private final RatingRepository ratingRepository;
    private final SystemSettingRepository settingRepository;
    private final RecipeService recipeService;

    public AdminServiceImpl(UserRepository userRepository,
                            RecipeRepository recipeRepository,
                            RecipeReelRepository reelRepository,
                            CommentRepository commentRepository,
                            RatingRepository ratingRepository,
                            SystemSettingRepository settingRepository,
                            RecipeService recipeService) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.reelRepository = reelRepository;
        this.commentRepository = commentRepository;
        this.ratingRepository = ratingRepository;
        this.settingRepository = settingRepository;
        this.recipeService = recipeService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDTO getAdminDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalContributors(userRepository.countByRole(Role.ROLE_CONTRIBUTOR));
        stats.setTotalRecipes(recipeRepository.count());
        stats.setPendingRecipes(recipeRepository.countByStatus(RecipeStatus.PENDING));
        stats.setApprovedRecipes(recipeRepository.countByStatus(RecipeStatus.APPROVED));
        stats.setRejectedRecipes(recipeRepository.countByStatus(RecipeStatus.REJECTED));
        stats.setTotalReels(reelRepository.count());

        long totalViews = recipeRepository.findAll().stream()
                .mapToLong(r -> r.getViewCount() != null ? r.getViewCount() : 0L)
                .sum();
        stats.setTotalViews(totalViews);
        stats.setTotalComments(commentRepository.count());

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDTO> getRecipesByStatus(RecipeStatus status, User admin) {
        List<Recipe> recipes = (status != null)
                ? recipeRepository.findByStatus(status)
                : recipeRepository.findAll();

        return recipes.stream()
                .map(r -> recipeService.getRecipeById(r.getId(), admin))
                .collect(Collectors.toList());
    }

    @Override
    public void approveRecipe(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        recipe.setStatus(RecipeStatus.APPROVED);
        recipe.setRejectionReason(null);

        if (recipe.getReel() != null) {
            recipe.getReel().setStatus(RecipeStatus.APPROVED);
        }

        recipeRepository.save(recipe);
    }

    @Override
    public void rejectRecipe(Long recipeId, String reason) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        recipe.setStatus(RecipeStatus.REJECTED);
        recipe.setRejectionReason(reason != null ? reason.trim() : "Does not meet community guidelines");

        if (recipe.getReel() != null) {
            recipe.getReel().setStatus(RecipeStatus.REJECTED);
        }

        recipeRepository.save(recipe);
    }

    @Override
    public void deleteRecipe(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        recipeRepository.delete(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getContentModerationQueue() {
        Map<String, Object> queue = new HashMap<>();

        // Pending recipes
        List<RecipeDTO> pendingRecipes = recipeRepository.findByStatus(RecipeStatus.PENDING).stream()
                .map(r -> recipeService.getRecipeById(r.getId(), null))
                .collect(Collectors.toList());
        queue.put("pendingRecipes", pendingRecipes);

        // Pending reels
        List<RecipeReel> pendingReels = reelRepository.findByStatus(RecipeStatus.PENDING);
        queue.put("pendingReelsCount", pendingReels.size());

        // Comments that are hidden or recent comments
        List<Comment> recentComments = commentRepository.findAll().stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .limit(20)
                .collect(Collectors.toList());
        queue.put("recentComments", recentComments.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("recipeId", c.getRecipe().getId());
            map.put("recipeTitle", c.getRecipe().getTitle());
            map.put("userName", c.getUser().getName());
            map.put("content", c.getContent());
            map.put("isHidden", c.isHidden());
            map.put("createdAt", c.getCreatedAt());
            return map;
        }).collect(Collectors.toList()));

        // Recent reviews
        List<Rating> recentReviews = ratingRepository.findAll().stream()
                .sorted(Comparator.comparing(Rating::getCreatedAt).reversed())
                .limit(20)
                .collect(Collectors.toList());
        queue.put("recentReviews", recentReviews.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("recipeId", r.getRecipe().getId());
            map.put("recipeTitle", r.getRecipe().getTitle());
            map.put("userName", r.getUser().getName());
            map.put("stars", r.getStars());
            map.put("reviewText", r.getReviewText());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).collect(Collectors.toList()));

        return queue;
    }

    @Override
    public void hideComment(Long commentId, boolean hide) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        comment.setHidden(hide);
        commentRepository.save(comment);
    }

    @Override
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingDTO getSystemSettings() {
        SystemSettingDTO dto = new SystemSettingDTO();
        settingRepository.findBySettingKey("website_name").ifPresent(s -> dto.setWebsiteName(s.getSettingValue()));
        settingRepository.findBySettingKey("max_video_size_mb").ifPresent(s -> {
            try { dto.setMaxVideoSizeMb(Integer.parseInt(s.getSettingValue())); } catch (Exception ignored) {}
        });
        settingRepository.findBySettingKey("max_reel_duration_sec").ifPresent(s -> {
            try { dto.setMaxReelDurationSec(Integer.parseInt(s.getSettingValue())); } catch (Exception ignored) {}
        });
        settingRepository.findBySettingKey("allow_comments").ifPresent(s -> dto.setAllowComments(Boolean.parseBoolean(s.getSettingValue())));
        settingRepository.findBySettingKey("allow_ratings").ifPresent(s -> dto.setAllowRatings(Boolean.parseBoolean(s.getSettingValue())));
        settingRepository.findBySettingKey("allow_new_contributors").ifPresent(s -> dto.setAllowNewContributors(Boolean.parseBoolean(s.getSettingValue())));
        settingRepository.findBySettingKey("content_moderation_mode").ifPresent(s -> dto.setContentModerationMode(s.getSettingValue()));
        return dto;
    }

    @Override
    public SystemSettingDTO updateSystemSettings(SystemSettingDTO dto) {
        saveOrUpdateSetting("website_name", dto.getWebsiteName(), "Website brand display name");
        saveOrUpdateSetting("max_video_size_mb", String.valueOf(dto.getMaxVideoSizeMb()), "Max video upload size in MB");
        saveOrUpdateSetting("max_reel_duration_sec", String.valueOf(dto.getMaxReelDurationSec()), "Max reel duration in seconds");
        saveOrUpdateSetting("allow_comments", String.valueOf(dto.isAllowComments()), "Allow users to post comments");
        saveOrUpdateSetting("allow_ratings", String.valueOf(dto.isAllowRatings()), "Allow users to rate and review recipes");
        saveOrUpdateSetting("allow_new_contributors", String.valueOf(dto.isAllowNewContributors()), "Allow registration as Contributor");
        saveOrUpdateSetting("content_moderation_mode", dto.getContentModerationMode(), "Content moderation mode");
        return dto;
    }

    private void saveOrUpdateSetting(String key, String value, String desc) {
        SystemSetting setting = settingRepository.findBySettingKey(key)
                .orElse(new SystemSetting(key, value, desc));
        setting.setSettingValue(value);
        settingRepository.save(setting);
    }
}
