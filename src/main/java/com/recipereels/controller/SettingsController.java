package com.recipereels.controller;

import com.recipereels.dto.ApiResponse;
import com.recipereels.dto.SystemSettingDTO;
import com.recipereels.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final AdminService adminService;

    public SettingsController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse> getPublicSettings() {
        SystemSettingDTO settings = adminService.getSystemSettings();
        return ResponseEntity.ok(ApiResponse.success("Public settings loaded", settings));
    }
}
