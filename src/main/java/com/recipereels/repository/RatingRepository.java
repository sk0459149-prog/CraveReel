package com.recipereels.repository;

import com.recipereels.entity.Rating;
import com.recipereels.entity.Recipe;
import com.recipereels.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByRecipeOrderByCreatedAtDesc(Recipe recipe);
    List<Rating> findByUserOrderByCreatedAtDesc(User user);
    Optional<Rating> findByRecipeAndUser(Recipe recipe, User user);
    boolean existsByRecipeAndUser(Recipe recipe, User user);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.recipe = :recipe")
    Double getAverageRatingForRecipe(@Param("recipe") Recipe recipe);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.recipe = :recipe")
    Long getRatingCountForRecipe(@Param("recipe") Recipe recipe);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.recipe.user = :contributor")
    Double getAverageRatingForContributor(@Param("contributor") User contributor);
}
