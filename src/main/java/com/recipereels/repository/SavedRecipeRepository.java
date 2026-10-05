package com.recipereels.repository;

import com.recipereels.entity.Recipe;
import com.recipereels.entity.SavedRecipe;
import com.recipereels.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {
    List<SavedRecipe> findByUserOrderBySavedAtDesc(User user);
    List<SavedRecipe> findByUserAndCollectionNameOrderBySavedAtDesc(User user, String collectionName);
    Optional<SavedRecipe> findByRecipeAndUser(Recipe recipe, User user);
    boolean existsByRecipeAndUser(Recipe recipe, User user);
    void deleteByRecipeAndUser(Recipe recipe, User user);
    void deleteByRecipe(Recipe recipe);

    @Query("SELECT DISTINCT s.collectionName FROM SavedRecipe s WHERE s.user = :user")
    List<String> findDistinctCollectionsByUser(@Param("user") User user);
}
