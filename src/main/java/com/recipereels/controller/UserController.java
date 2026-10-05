package com.recipereels.controller;

import com.recipereels.dto.ApiResponse;
import com.recipereels.dto.UserProfileDTO;
import com.recipereels.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileDTO profile = userService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile loaded", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfileDTO updateDto) {
        UserProfileDTO updated = userService.updateProfile(userDetails.getUsername(), updateDto);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }
}
