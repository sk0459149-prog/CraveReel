package com.recipereels.service.impl;

import com.recipereels.dto.*;
import com.recipereels.entity.*;
import com.recipereels.exception.BadRequestException;
import com.recipereels.exception.ResourceNotFoundException;
import com.recipereels.repository.*;
import com.recipereels.service.InteractionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class InteractionServiceImpl implements InteractionService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final RecipeLikeRepository likeRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final DirectMessageRepository messageRepository;

    public InteractionServiceImpl(RecipeRepository recipeRepository,
                                  UserRepository userRepository,
                                  RecipeLikeRepository likeRepository,
                                  SavedRecipeRepository savedRecipeRepository,
                                  RatingRepository ratingRepository,
                                  CommentRepository commentRepository,
                                  DirectMessageRepository messageRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.savedRecipeRepository = savedRecipeRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public boolean toggleLike(Long recipeId, User user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        Optional<RecipeLike> existing = likeRepository.findByRecipeAndUser(recipe, user);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return false;
        } else {
            likeRepository.save(new RecipeLike(recipe, user));
            return true;
        }
    }

    @Override
    public SavedRecipeDTO saveRecipe(Long recipeId, String collectionName, User user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        String colName = (collectionName == null || collectionName.trim().isEmpty()) ? "Favorites" : collectionName.trim();

        SavedRecipe saved = savedRecipeRepository.findByRecipeAndUser(recipe, user)
                .orElse(new SavedRecipe(recipe, user, colName));
        saved.setCollectionName(colName);
        SavedRecipe result = savedRecipeRepository.save(saved);

        SavedRecipeDTO dto = new SavedRecipeDTO();
        dto.setId(result.getId());
        dto.setRecipeId(recipe.getId());
        dto.setRecipeTitle(recipe.getTitle());
        dto.setRecipeImage(recipe.getImageUrl());
        dto.setCollectionName(result.getCollectionName());
        dto.setSavedAt(result.getSavedAt());
        return dto;
    }

    @Override
    public void removeSavedRecipe(Long recipeId, User user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        savedRecipeRepository.deleteByRecipeAndUser(recipe, user);
    }

    @Override
    public RatingDTO rateRecipe(Long recipeId, Integer stars, String reviewText, User user) {
        if (stars == null || stars < 1 || stars > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars");
        }

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        Rating rating = ratingRepository.findByRecipeAndUser(recipe, user)
                .orElse(new Rating(recipe, user, stars, reviewText));

        rating.setStars(stars);
        rating.setReviewText(reviewText != null ? reviewText.trim() : null);
        Rating saved = ratingRepository.save(rating);

        RatingDTO dto = new RatingDTO();
        dto.setId(saved.getId());
        dto.setRecipeId(recipe.getId());
        dto.setRecipeTitle(recipe.getTitle());
        dto.setRecipeImage(recipe.getImageUrl());
        dto.setUserId(user.getId());
        dto.setUserName(user.getName());
        dto.setUserAvatar(user.getAvatarUrl());
        dto.setStars(saved.getStars());
        dto.setReviewText(saved.getReviewText());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setUpdatedAt(saved.getUpdatedAt());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingDTO> getRecipeRatings(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        return ratingRepository.findByRecipeOrderByCreatedAtDesc(recipe).stream().map(r -> {
            RatingDTO dto = new RatingDTO();
            dto.setId(r.getId());
            dto.setRecipeId(recipe.getId());
            dto.setRecipeTitle(recipe.getTitle());
            dto.setUserId(r.getUser().getId());
            dto.setUserName(r.getUser().getName());
            dto.setUserAvatar(r.getUser().getAvatarUrl());
            dto.setStars(r.getStars());
            dto.setReviewText(r.getReviewText());
            dto.setCreatedAt(r.getCreatedAt());
            dto.setUpdatedAt(r.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public CommentDTO addComment(Long recipeId, CommentCreateDTO commentDto, User user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        Comment parent = null;
        if (commentDto.getParentId() != null) {
            parent = commentRepository.findById(commentDto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));
        }

        Comment comment = new Comment(recipe, user, commentDto.getContent().trim(), parent);
        Comment saved = commentRepository.save(comment);

        return mapCommentToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> getRecipeComments(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));

        List<Comment> topLevelComments = commentRepository.findByRecipeAndParentIsNullAndIsHiddenFalseOrderByCreatedAtDesc(recipe);
        return topLevelComments.stream()
                .map(this::mapCommentToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DirectMessageDTO sendMessage(DirectMessageCreateDTO dto, User sender) {
        User recipient = userRepository.findById(dto.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found with id: " + dto.getRecipientId()));

        DirectMessage msg = new DirectMessage(sender, recipient, dto.getSubject(), dto.getContent().trim());
        DirectMessage saved = messageRepository.save(msg);

        return mapMessageToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectMessageDTO> getInbox(User recipient) {
        return messageRepository.findByRecipientOrderByCreatedAtDesc(recipient).stream()
                .map(this::mapMessageToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectMessageDTO> getSentMessages(User sender) {
        return messageRepository.findBySenderOrderByCreatedAtDesc(sender).stream()
                .map(this::mapMessageToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(User user) {
        return messageRepository.countByRecipientAndIsReadFalse(user);
    }

    private CommentDTO mapCommentToDTO(Comment c) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setRecipeId(c.getRecipe().getId());
        dto.setUserId(c.getUser().getId());
        dto.setUserName(c.getUser().getName());
        dto.setUserAvatar(c.getUser().getAvatarUrl());
        dto.setUserRole(c.getUser().getRole().name());
        dto.setContent(c.getContent());
        dto.setHidden(c.isHidden());
        dto.setCreatedAt(c.getCreatedAt());

        if (c.getParent() != null) {
            dto.setParentId(c.getParent().getId());
        }

        if (c.getReplies() != null && !c.getReplies().isEmpty()) {
            dto.setReplies(c.getReplies().stream()
                    .filter(r -> !r.isHidden())
                    .map(this::mapCommentToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private DirectMessageDTO mapMessageToDTO(DirectMessage m) {
        DirectMessageDTO dto = new DirectMessageDTO();
        dto.setId(m.getId());
        dto.setSenderId(m.getSender().getId());
        dto.setSenderName(m.getSender().getName());
        dto.setSenderAvatar(m.getSender().getAvatarUrl());
        dto.setSenderRole(m.getSender().getRole().name());
        dto.setRecipientId(m.getRecipient().getId());
        dto.setRecipientName(m.getRecipient().getName());
        dto.setRecipientAvatar(m.getRecipient().getAvatarUrl());
        dto.setRecipientRole(m.getRecipient().getRole().name());
        dto.setSubject(m.getSubject());
        dto.setContent(m.getContent());
        dto.setRead(m.isRead());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
