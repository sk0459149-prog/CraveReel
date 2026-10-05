package com.recipereels.repository;

import com.recipereels.entity.Recipe;
import com.recipereels.entity.RecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
    List<RecipeStep> findByRecipeOrderByStepNumberAsc(Recipe recipe);
    void deleteByRecipe(Recipe recipe);
}
