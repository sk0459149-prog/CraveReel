package com.recipereels.controller;

import com.recipereels.dto.ApiResponse;
import com.recipereels.dto.ReelDTO;
import com.recipereels.entity.User;
import com.recipereels.security.UserPrincipal;
import com.recipereels.service.ReelService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reels")
public class ReelController {

    private final ReelService reelService;

    public ReelController(ReelService reelService) {
        this.reelService = reelService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getReels(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        List<ReelDTO> reels = reelService.getApprovedReels(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reels retrieved successfully", reels));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getReelById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User currentUser = userPrincipal != null ? userPrincipal.getUser() : null;
        ReelDTO reel = reelService.getReelById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Reel details", reel));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<ApiResponse> recordView(@PathVariable Long id) {
        reelService.recordReelView(id);
        return ResponseEntity.ok(ApiResponse.success("View recorded"));
    }
}
