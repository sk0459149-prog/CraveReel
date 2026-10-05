package com.recipereels.service.impl;

import com.recipereels.dto.ReelDTO;
import com.recipereels.entity.RecipeReel;
import com.recipereels.entity.RecipeStatus;
import com.recipereels.entity.User;
import com.recipereels.exception.ResourceNotFoundException;
import com.recipereels.repository.*;
import com.recipereels.service.ReelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReelServiceImpl implements ReelService {

    private final RecipeReelRepository reelRepository;
    private final RecipeLikeRepository likeRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final CommentRepository commentRepository;
    private final ReelViewTracker reelViewTracker;

    public ReelServiceImpl(RecipeReelRepository reelRepository,
                           RecipeLikeRepository likeRepository,
                           SavedRecipeRepository savedRecipeRepository,
                           CommentRepository commentRepository,
                           ReelViewTracker reelViewTracker) {
        this.reelRepository = reelRepository;
        this.likeRepository = likeRepository;
        this.savedRecipeRepository = savedRecipeRepository;
        this.commentRepository = commentRepository;
        this.reelViewTracker = reelViewTracker;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReelDTO> getApprovedReels(User currentUser) {
        List<RecipeReel> reels = reelRepository.findTrendingReels();
        return reels.stream()
                .filter(r -> r.getRecipe() != null && r.getRecipe().getStatus() == RecipeStatus.APPROVED)
                .map(r -> mapToReelDTO(r, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReelDTO getReelById(Long id, User currentUser) {
        RecipeReel reel = reelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reel not found with id: " + id));
        return mapToReelDTO(reel, currentUser);
    }

    @Override
    public void recordReelView(Long id) {
        if (!reelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reel not found with id: " + id);
        }
        reelViewTracker.recordView(id);
    }

    private ReelDTO mapToReelDTO(RecipeReel reel, User currentUser) {
        ReelDTO dto = new ReelDTO();
        dto.setId(reel.getId());
        dto.setRecipeId(reel.getRecipe().getId());
        dto.setRecipeTitle(reel.getRecipe().getTitle());
        dto.setRecipeDescription(reel.getRecipe().getDescription());
        dto.setCategoryName(reel.getRecipe().getCategory() != null ? reel.getRecipe().getCategory().getName() : "");
        dto.setVegetarian(reel.getRecipe().isVegetarian());
        dto.setVideoUrl(reel.getVideoUrl());
        dto.setThumbnailUrl(reel.getThumbnailUrl());
        dto.setDurationSeconds(reel.getDurationSeconds());
        dto.setAspectRatio(reel.getAspectRatio());
        dto.setViewCount(reel.getViewCount());
        dto.setCreatedAt(reel.getCreatedAt());

        if (reel.getRecipe().getUser() != null) {
            dto.setContributorId(reel.getRecipe().getUser().getId());
            dto.setContributorName(reel.getRecipe().getUser().getName());
            dto.setContributorAvatar(reel.getRecipe().getUser().getAvatarUrl());
        }

        dto.setLikeCount(likeRepository.countByRecipe(reel.getRecipe()));
        dto.setCommentCount(commentRepository.countByRecipe(reel.getRecipe()));

        if (currentUser != null) {
            dto.setLikedByCurrentUser(likeRepository.existsByRecipeAndUser(reel.getRecipe(), currentUser));
            dto.setSavedByCurrentUser(savedRecipeRepository.existsByRecipeAndUser(reel.getRecipe(), currentUser));
        }

        return dto;
    }
}
