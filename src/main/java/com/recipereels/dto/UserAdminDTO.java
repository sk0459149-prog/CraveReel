package com.recipereels.dto;

import java.time.LocalDateTime;

public class UserAdminDTO {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private long recipeCount;

    public UserAdminDTO() {
    }

    public UserAdminDTO(Long id, String name, String email, String role, boolean enabled, LocalDateTime createdAt, long recipeCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.recipeCount = recipeCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getRecipeCount() {
        return recipeCount;
    }

    public void setRecipeCount(long recipeCount) {
        this.recipeCount = recipeCount;
    }
}
