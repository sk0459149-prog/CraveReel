package com.recipereels.service;

import com.recipereels.dto.RatingDTO;
import com.recipereels.dto.SavedRecipeDTO;
import com.recipereels.entity.RecipeViewHistory;
import com.recipereels.entity.User;

import java.util.List;
import java.util.Map;

public interface ExplorerService {
    List<SavedRecipeDTO> getSavedRecipes(User user, String collectionName);
    List<String> getUserCollections(User user);
    void createCollection(String collectionName, User user);
    void moveRecipeToCollection(Long recipeId, String collectionName, User user);

    List<RatingDTO> getUserReviews(User user);
    void deleteUserReview(Long ratingId, User user);

    List<SavedRecipeDTO> getUserBrowsingHistory(User user);
    void clearBrowsingHistory(User user);

    Map<String, Object> getExplorerDashboardSummary(User user);
}
