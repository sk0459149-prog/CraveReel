package com.recipereels.dto;

import java.util.HashMap;
import java.util.Map;

public class DashboardStatsDTO {
    // Admin & General metrics
    private long totalUsers;
    private long totalContributors;
    private long totalRecipes;
    private long pendingRecipes;
    private long approvedRecipes;
    private long rejectedRecipes;
    private long totalReels;
    private long totalViews;
    private long totalComments;

    // Contributor specific
    private Double averageRating = 0.0;
    private long totalLikes;
    private long unreadMessages;

    private Map<String, Object> extra = new HashMap<>();

    public DashboardStatsDTO() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalContributors() {
        return totalContributors;
    }

    public void setTotalContributors(long totalContributors) {
        this.totalContributors = totalContributors;
    }

    public long getTotalRecipes() {
        return totalRecipes;
    }

    public void setTotalRecipes(long totalRecipes) {
        this.totalRecipes = totalRecipes;
    }

    public long getPendingRecipes() {
        return pendingRecipes;
    }

    public void setPendingRecipes(long pendingRecipes) {
        this.pendingRecipes = pendingRecipes;
    }

    public long getApprovedRecipes() {
        return approvedRecipes;
    }

    public void setApprovedRecipes(long approvedRecipes) {
        this.approvedRecipes = approvedRecipes;
    }

    public long getRejectedRecipes() {
        return rejectedRecipes;
    }

    public void setRejectedRecipes(long rejectedRecipes) {
        this.rejectedRecipes = rejectedRecipes;
    }

    public long getTotalReels() {
        return totalReels;
    }

    public void setTotalReels(long totalReels) {
        this.totalReels = totalReels;
    }

    public long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public long getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(long unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
