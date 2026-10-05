package com.recipereels.service.impl;

import com.recipereels.dto.RatingDTO;
import com.recipereels.dto.SavedRecipeDTO;
import com.recipereels.entity.*;
import com.recipereels.exception.BadRequestException;
import com.recipereels.exception.ResourceNotFoundException;
import com.recipereels.repository.*;
import com.recipereels.service.ExplorerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExplorerServiceImpl implements ExplorerService {

    private final SavedRecipeRepository savedRecipeRepository;
    private final RatingRepository ratingRepository;
    private final RecipeViewHistoryRepository viewHistoryRepository;
    private final RecipeRepository recipeRepository;

    public ExplorerServiceImpl(SavedRecipeRepository savedRecipeRepository,
                               RatingRepository ratingRepository,
                               RecipeViewHistoryRepository viewHistoryRepository,
                               RecipeRepository recipeRepository) {
        this.savedRecipeRepository = savedRecipeRepository;
        this.ratingRepository = ratingRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedRecipeDTO> getSavedRecipes(User user, String collectionName) {
        List<SavedRecipe> savedList;
        if (collectionName != null && !collectionName.trim().isEmpty() && !collectionName.equalsIgnoreCase("ALL")) {
            savedList = savedRecipeRepository.findByUserAndCollectionNameOrderBySavedAtDesc(user, collectionName.trim());
        } else {
            savedList = savedRecipeRepository.findByUserOrderBySavedAtDesc(user);
        }

        return savedList.stream().map(this::mapSavedToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserCollections(User user) {
        List<String> collections = savedRecipeRepository.findDistinctCollectionsByUser(user);
        if (collections == null || collections.isEmpty()) {
            return List.of("Favorites");
        }
        return collections;
    }

    @Override
    public void createCollection(String collectionName, User user) {
        if (collectionName == null || collectionName.trim().isEmpty()) {
            throw new BadRequestException("Collection name cannot be empty");
        }
    }

    @Override
    public void moveRecipeToCollection(Long recipeId, String collectionName, User user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found"));

        String col = (collectionName == null || collectionName.trim().isEmpty()) ? "Favorites" : collectionName.trim();

        SavedRecipe saved = savedRecipeRepository.findByRecipeAndUser(recipe, user)
                .orElse(new SavedRecipe(recipe, user, col));
        saved.setCollectionName(col);
        savedRecipeRepository.save(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingDTO> getUserReviews(User user) {
        return ratingRepository.findByUserOrderByCreatedAtDesc(user).stream().map(r -> {
            RatingDTO dto = new RatingDTO();
            dto.setId(r.getId());
            dto.setRecipeId(r.getRecipe().getId());
            dto.setRecipeTitle(r.getRecipe().getTitle());
            dto.setRecipeImage(r.getRecipe().getImageUrl());
            dto.setUserId(user.getId());
            dto.setUserName(user.getName());
            dto.setStars(r.getStars());
            dto.setReviewText(r.getReviewText());
            dto.setCreatedAt(r.getCreatedAt());
            dto.setUpdatedAt(r.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteUserReview(Long ratingId, User user) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + ratingId));

        if (!rating.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        ratingRepository.delete(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedRecipeDTO> getUserBrowsingHistory(User user) {
        List<RecipeViewHistory> history = viewHistoryRepository.findByUserOrderByViewedAtDesc(user);
        return history.stream().map(h -> {
            Recipe r = h.getRecipe();
            SavedRecipeDTO dto = new SavedRecipeDTO();
            dto.setId(h.getId());
            dto.setRecipeId(r.getId());
            dto.setRecipeTitle(r.getTitle());
            dto.setRecipeImage(r.getImageUrl());
            dto.setCategoryName(r.getCategory() != null ? r.getCategory().getName() : "");
            dto.setCookTimeMinutes(r.getCookTimeMinutes());
            dto.setPrepTimeMinutes(r.getPrepTimeMinutes());
            dto.setDifficulty(r.getDifficulty().name());
            dto.setVegetarian(r.isVegetarian());
            dto.setSavedAt(h.getViewedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void clearBrowsingHistory(User user) {
        viewHistoryRepository.deleteByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getExplorerDashboardSummary(User user) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("savedCount", savedRecipeRepository.findByUserOrderBySavedAtDesc(user).size());
        summary.put("reviewsCount", ratingRepository.findByUserOrderByCreatedAtDesc(user).size());
        summary.put("historyCount", viewHistoryRepository.findByUserOrderByViewedAtDesc(user).size());
        summary.put("collections", getUserCollections(user));
        return summary;
    }

    private SavedRecipeDTO mapSavedToDTO(SavedRecipe s) {
        Recipe r = s.getRecipe();
        SavedRecipeDTO dto = new SavedRecipeDTO();
        dto.setId(s.getId());
        dto.setRecipeId(r.getId());
        dto.setRecipeTitle(r.getTitle());
        dto.setRecipeImage(r.getImageUrl());
        dto.setCategoryName(r.getCategory() != null ? r.getCategory().getName() : "");
        dto.setCookTimeMinutes(r.getCookTimeMinutes());
        dto.setPrepTimeMinutes(r.getPrepTimeMinutes());
        dto.setDifficulty(r.getDifficulty().name());
        dto.setVegetarian(r.isVegetarian());
        dto.setCollectionName(s.getCollectionName());
        dto.setSavedAt(s.getSavedAt());

        Double avg = ratingRepository.getAverageRatingForRecipe(r);
        dto.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);

        return dto;
    }
}
