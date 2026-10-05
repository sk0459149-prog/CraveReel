package com.recipereels.repository;

import com.recipereels.entity.Recipe;
import com.recipereels.entity.RecipeViewHistory;
import com.recipereels.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeViewHistoryRepository extends JpaRepository<RecipeViewHistory, Long> {
    List<RecipeViewHistory> findByUserOrderByViewedAtDesc(User user);
    Optional<RecipeViewHistory> findByRecipeAndUser(Recipe recipe, User user);
    void deleteByUser(User user);
    void deleteByRecipe(Recipe recipe);
}
