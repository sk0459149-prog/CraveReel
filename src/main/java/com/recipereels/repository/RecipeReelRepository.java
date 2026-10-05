package com.recipereels.repository;

import com.recipereels.entity.Recipe;
import com.recipereels.entity.RecipeReel;
import com.recipereels.entity.RecipeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeReelRepository extends JpaRepository<RecipeReel, Long> {
    Optional<RecipeReel> findByRecipe(Recipe recipe);
    List<RecipeReel> findByStatus(RecipeStatus status);
    long countByStatus(RecipeStatus status);


    @Modifying
    @Transactional
    @Query("UPDATE RecipeReel r SET r.viewCount = r.viewCount + 1 WHERE r.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Query("SELECT r FROM RecipeReel r WHERE r.status = 'APPROVED' ORDER BY r.viewCount DESC, r.createdAt DESC")
    List<RecipeReel> findTrendingReels();
}
