package com.recipereels.repository;

import com.recipereels.entity.Recipe;
import com.recipereels.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipeOrderByOrderIndexAsc(Recipe recipe);
    void deleteByRecipe(Recipe recipe);
}
