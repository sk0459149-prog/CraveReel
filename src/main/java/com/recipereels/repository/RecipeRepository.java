package com.recipereels.repository;

import com.recipereels.entity.Category;
import com.recipereels.entity.Difficulty;
import com.recipereels.entity.Recipe;
import com.recipereels.entity.RecipeStatus;
import com.recipereels.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findByStatus(RecipeStatus status);

    boolean existsByTitle(String title);

    Optional<Recipe> findByTitle(String title);

    List<Recipe> findAllByTitle(String title);

    List<Recipe> findByUser(User user);

    List<Recipe> findByUserAndStatus(User user, RecipeStatus status);

    long countByStatus(RecipeStatus status);

    long countByUser(User user);

    @Query("SELECT r FROM Recipe r WHERE r.status = 'APPROVED' ORDER BY r.viewCount DESC")
    List<Recipe> findTrendingRecipes();

    @Query("SELECT r FROM Recipe r WHERE r.status = 'APPROVED' ORDER BY r.createdAt DESC")
    List<Recipe> findRecentRecipes();

    @Query("SELECT r FROM Recipe r WHERE r.status = 'APPROVED' AND (r.cookTimeMinutes + r.prepTimeMinutes) <= :maxMinutes ORDER BY (r.cookTimeMinutes + r.prepTimeMinutes) ASC")
    List<Recipe> findQuickRecipes(@Param("maxMinutes") int maxMinutes);

    @Query("SELECT DISTINCT r FROM Recipe r " +
           "LEFT JOIN r.ingredients i " +
           "LEFT JOIN r.user u " +
           "LEFT JOIN r.category c " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:query IS NULL OR :query = '' OR " +
           "     LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "     LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:categoryId IS NULL OR c.id = :categoryId) " +
           "AND (:isVegetarian IS NULL OR r.isVegetarian = :isVegetarian) " +
           "AND (:difficulty IS NULL OR r.difficulty = :difficulty) " +
           "AND (:maxCookTime IS NULL OR (r.cookTimeMinutes + r.prepTimeMinutes) <= :maxCookTime)")
    List<Recipe> filterRecipes(@Param("status") RecipeStatus status,
                              @Param("query") String query,
                              @Param("categoryId") Long categoryId,
                              @Param("isVegetarian") Boolean isVegetarian,
                              @Param("difficulty") Difficulty difficulty,
                              @Param("maxCookTime") Integer maxCookTime);
}
