package com.recipereels.service;

import com.recipereels.dto.DashboardStatsDTO;
import com.recipereels.dto.RecipeDTO;
import com.recipereels.dto.SystemSettingDTO;
import com.recipereels.entity.RecipeStatus;
import com.recipereels.entity.User;

import java.util.List;
import java.util.Map;

public interface AdminService {
    DashboardStatsDTO getAdminDashboardStats();
    List<RecipeDTO> getRecipesByStatus(RecipeStatus status, User admin);
    void approveRecipe(Long recipeId);
    void rejectRecipe(Long recipeId, String reason);
    void deleteRecipe(Long recipeId);

    // Content Moderation
    Map<String, Object> getContentModerationQueue();
    void hideComment(Long commentId, boolean hide);
    void deleteComment(Long commentId);

    // System Settings
    SystemSettingDTO getSystemSettings();
    SystemSettingDTO updateSystemSettings(SystemSettingDTO dto);
}
