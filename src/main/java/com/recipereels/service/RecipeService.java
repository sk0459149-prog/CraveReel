package com.recipereels.service;

import com.recipereels.dto.RecipeCreateDTO;
import com.recipereels.dto.RecipeDTO;
import com.recipereels.entity.RecipeStatus;
import com.recipereels.entity.User;

import java.util.List;

public interface RecipeService {
    List<RecipeDTO> getPublishedRecipes(String query, Long categoryId, Boolean isVegetarian, String difficulty, Integer maxCookTime, String sortBy, User currentUser);
    RecipeDTO getRecipeById(Long id, User currentUser);
    RecipeDTO createRecipe(RecipeCreateDTO createDTO, User contributor);
    RecipeDTO updateRecipe(Long id, RecipeCreateDTO updateDTO, User currentUser);
    void deleteRecipe(Long id, User currentUser);
    void recordRecipeView(Long id, User currentUser);

    List<RecipeDTO> getTrendingRecipes(User currentUser);
    List<RecipeDTO> getRecentRecipes(User currentUser);
    List<RecipeDTO> getQuickRecipes(int maxMinutes, User currentUser);

    List<RecipeDTO> getContributorRecipes(User contributor, RecipeStatus status);
}
