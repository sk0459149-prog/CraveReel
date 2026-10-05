package com.recipereels.repository;

import com.recipereels.entity.Recipe;
import com.recipereels.entity.RecipeLike;
import com.recipereels.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeLikeRepository extends JpaRepository<RecipeLike, Long> {
    long countByRecipe(Recipe recipe);
    boolean existsByRecipeAndUser(Recipe recipe, User user);
    Optional<RecipeLike> findByRecipeAndUser(Recipe recipe, User user);
    void deleteByRecipeAndUser(Recipe recipe, User user);
    List<RecipeLike> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT COUNT(l) FROM RecipeLike l WHERE l.recipe.user = :contributor")
    Long countLikesForContributor(@Param("contributor") User contributor);
}
