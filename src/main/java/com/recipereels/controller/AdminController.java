package com.recipereels.controller;

import com.recipereels.dto.*;
import com.recipereels.entity.RecipeStatus;
import com.recipereels.security.UserPrincipal;
import com.recipereels.service.AdminService;
import com.recipereels.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getStats() {
        DashboardStatsDTO stats = adminService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Admin stats loaded", stats));
    }

    // 1. User Management
    @GetMapping("/users")
    public ResponseEntity<ApiResponse> getUsers(@RequestParam(required = false) String search) {
        List<UserAdminDTO> users = userService.getAllUsersForAdmin(search);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody UserAdminDTO userDto) {
        UserAdminDTO created = userService.createUserByAdmin(userDto);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", created));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable Long id, @RequestBody UserAdminDTO userDto) {
        UserAdminDTO updated = userService.updateUserByAdmin(id, userDto);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        userService.deleteUserByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse> toggleUserStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        userService.toggleUserStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully"));
    }

    // 2. Recipe Approval
    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse> getRecipes(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RecipeStatus recipeStatus = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                recipeStatus = RecipeStatus.valueOf(status.toUpperCase());
            } catch (Exception ignored) {}
        }
        List<RecipeDTO> recipes = adminService.getRecipesByStatus(recipeStatus, userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Recipes retrieved", recipes));
    }

    @PutMapping("/recipes/{id}/approve")
    public ResponseEntity<ApiResponse> approveRecipe(@PathVariable Long id) {
        adminService.approveRecipe(id);
        return ResponseEntity.ok(ApiResponse.success("Recipe approved successfully"));
    }

    @PutMapping("/recipes/{id}/reject")
    public ResponseEntity<ApiResponse> rejectRecipe(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        adminService.rejectRecipe(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Recipe rejected successfully"));
    }

    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<ApiResponse> deleteRecipe(@PathVariable Long id) {
        adminService.deleteRecipe(id);
        return ResponseEntity.ok(ApiResponse.success("Recipe deleted successfully"));
    }

    // 3. Content Moderation
    @GetMapping("/moderation")
    public ResponseEntity<ApiResponse> getModerationQueue() {
        Map<String, Object> queue = adminService.getContentModerationQueue();
        return ResponseEntity.ok(ApiResponse.success("Moderation queue loaded", queue));
    }

    @PatchMapping("/comments/{id}/hide")
    public ResponseEntity<ApiResponse> hideComment(@PathVariable Long id, @RequestParam boolean hide) {
        adminService.hideComment(id, hide);
        return ResponseEntity.ok(ApiResponse.success(hide ? "Comment hidden" : "Comment unhidden"));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse> deleteComment(@PathVariable Long id) {
        adminService.deleteComment(id);
        return ResponseEntity.ok(ApiResponse.success("Comment removed successfully"));
    }

    // 4. System Settings
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse> getSettings() {
        SystemSettingDTO settings = adminService.getSystemSettings();
        return ResponseEntity.ok(ApiResponse.success("Settings loaded", settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse> updateSettings(@RequestBody SystemSettingDTO settingsDTO) {
        SystemSettingDTO updated = adminService.updateSystemSettings(settingsDTO);
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", updated));
    }
}
