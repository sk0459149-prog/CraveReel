package com.recipereels.service;

import com.recipereels.dto.*;
import com.recipereels.entity.User;

import java.util.List;

public interface InteractionService {
    boolean toggleLike(Long recipeId, User user);
    SavedRecipeDTO saveRecipe(Long recipeId, String collectionName, User user);
    void removeSavedRecipe(Long recipeId, User user);

    RatingDTO rateRecipe(Long recipeId, Integer stars, String reviewText, User user);
    List<RatingDTO> getRecipeRatings(Long recipeId);

    CommentDTO addComment(Long recipeId, CommentCreateDTO commentDto, User user);
    List<CommentDTO> getRecipeComments(Long recipeId);

    DirectMessageDTO sendMessage(DirectMessageCreateDTO dto, User sender);
    List<DirectMessageDTO> getInbox(User recipient);
    List<DirectMessageDTO> getSentMessages(User sender);
    long getUnreadMessageCount(User user);
}
