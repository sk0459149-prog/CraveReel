package com.recipereels.repository;

import com.recipereels.entity.Comment;
import com.recipereels.entity.Recipe;
import com.recipereels.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByRecipeAndParentIsNullAndIsHiddenFalseOrderByCreatedAtDesc(Recipe recipe);
    List<Comment> findByRecipeAndParentIsNullOrderByCreatedAtDesc(Recipe recipe);
    List<Comment> findByUserOrderByCreatedAtDesc(User user);
    long countByRecipe(Recipe recipe);

    @Query("SELECT c FROM Comment c WHERE c.recipe.user = :contributor ORDER BY c.createdAt DESC")
    List<Comment> findCommentsOnContributorRecipes(@Param("contributor") User contributor);

    @Query("SELECT c FROM Comment c WHERE c.isHidden = true ORDER BY c.createdAt DESC")
    List<Comment> findHiddenComments();
}
