package com.recipereels.service.impl;

import com.recipereels.dto.*;
import com.recipereels.entity.*;
import com.recipereels.exception.BadRequestException;
import com.recipereels.exception.ResourceNotFoundException;
import com.recipereels.repository.*;
import com.recipereels.service.RecipeService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final CategoryRepository categoryRepository;
    private final RecipeIngredientRepository ingredientRepository;
    private final RecipeStepRepository stepRepository;
    private final RecipeReelRepository reelRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final RecipeLikeRepository likeRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final RecipeViewHistoryRepository viewHistoryRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository,
                             CategoryRepository categoryRepository,
                             RecipeIngredientRepository ingredientRepository,
                             RecipeStepRepository stepRepository,
                             RecipeReelRepository reelRepository,
                             RatingRepository ratingRepository,
                             CommentRepository commentRepository,
                             RecipeLikeRepository likeRepository,
                             SavedRecipeRepository savedRecipeRepository,
                             RecipeViewHistoryRepository viewHistoryRepository) {
        this.recipeRepository = recipeRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.stepRepository = stepRepository;
        this.reelRepository = reelRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.savedRecipeRepository = savedRecipeRepository;
        this.viewHistoryRepository = viewHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDTO> getPublishedRecipes(String query, Long categoryId, Boolean isVegetarian, String difficultyStr, Integer maxCookTime, String sortBy, User currentUser) {
        Difficulty difficulty = null;
        if (difficultyStr != null && !difficultyStr.trim().isEmpty() && !difficultyStr.equalsIgnoreCase("ALL")) {
            try {
                difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<Recipe> recipes = recipeRepository.filterRecipes(RecipeStatus.APPROVED, query, categoryId, isVegetarian, difficulty, maxCookTime);

        List<RecipeDTO> dtos = recipes.stream()
                .map(r -> mapToDTO(r, currentUser))
                .collect(Collectors.toList());

        // Sorting
        if ("rating".equalsIgnoreCase(sortBy)) {
            dtos.sort(Comparator.comparing(RecipeDTO::getAverageRating).reversed());
        } else if ("popular".equalsIgnoreCase(sortBy) || "views".equalsIgnoreCase(sortBy)) {
            dtos.sort(Comparator.comparing(RecipeDTO::getViewCount).reversed());
        } else if ("quickest".equalsIgnoreCase(sortBy)) {
            dtos.sort(Comparator.comparing(RecipeDTO::getTotalTimeMinutes));
        } else {
            // Default newest
            dtos.sort(Comparator.comparing(RecipeDTO::getCreatedAt).reversed());
        }

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDTO getRecipeById(Long id, User currentUser) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));

        // If not approved, only author or admin can view
        if (recipe.getStatus() != RecipeStatus.APPROVED) {
            if (currentUser == null || (!currentUser.getId().equals(recipe.getUser().getId()) && currentUser.getRole() != Role.ROLE_ADMIN)) {
                throw new AccessDeniedException("This recipe is pending review and not publicly available");
            }
        }

        return mapToDTO(recipe, currentUser);
    }

    @Override
    public RecipeDTO createRecipe(RecipeCreateDTO createDTO, User contributor) {
        Category category = categoryRepository.findById(createDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + createDTO.getCategoryId()));

        Recipe recipe = new Recipe();
        recipe.setTitle(createDTO.getTitle().trim());
        recipe.setDescription(createDTO.getDescription());
        recipe.setCategory(category);
        recipe.setUser(contributor);
        recipe.setPrepTimeMinutes(createDTO.getPrepTimeMinutes() != null ? createDTO.getPrepTimeMinutes() : 10);
        recipe.setCookTimeMinutes(createDTO.getCookTimeMinutes() != null ? createDTO.getCookTimeMinutes() : 20);
        recipe.setServings(createDTO.getServings() != null ? createDTO.getServings() : 2);
        recipe.setVegetarian(createDTO.isVegetarian());

        try {
            recipe.setDifficulty(Difficulty.valueOf(createDTO.getDifficulty().toUpperCase()));
        } catch (Exception e) {
            recipe.setDifficulty(Difficulty.EASY);
        }

        recipe.setStatus(createDTO.isSubmitForReview() ? RecipeStatus.PENDING : RecipeStatus.DRAFT);
        recipe.setImageUrl(createDTO.getImageUrl() != null && !createDTO.getImageUrl().trim().isEmpty()
                ? createDTO.getImageUrl()
                : "https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800");

        // Save recipe first
        Recipe savedRecipe = recipeRepository.save(recipe);

        // Add ingredients
        if (createDTO.getIngredients() != null) {
            int order = 1;
            for (IngredientDTO ingDto : createDTO.getIngredients()) {
                if (ingDto.getName() != null && !ingDto.getName().trim().isEmpty()) {
                    RecipeIngredient ingredient = new RecipeIngredient(
                            ingDto.getName().trim(),
                            ingDto.getQuantity(),
                            ingDto.getUnit(),
                            order++
                    );
                    savedRecipe.addIngredient(ingredient);
                }
            }
        }

        // Add steps
        if (createDTO.getSteps() != null) {
            int stepNum = 1;
            for (StepDTO stepDto : createDTO.getSteps()) {
                if (stepDto.getInstruction() != null && !stepDto.getInstruction().trim().isEmpty()) {
                    RecipeStep step = new RecipeStep(
                            stepNum++,
                            stepDto.getTitle() != null ? stepDto.getTitle() : "Step " + (stepNum - 1),
                            stepDto.getInstruction().trim(),
                            stepDto.getTimerMinutes(),
                            stepDto.getTip()
                    );
                    savedRecipe.addStep(step);
                }
            }
        }

        // Attach video reel if provided
        if (createDTO.getVideoUrl() != null && !createDTO.getVideoUrl().trim().isEmpty()) {
            RecipeReel reel = new RecipeReel();
            reel.setRecipe(savedRecipe);
            reel.setVideoUrl(createDTO.getVideoUrl());
            reel.setThumbnailUrl(savedRecipe.getImageUrl());
            reel.setDurationSeconds(createDTO.getDurationSeconds() != null ? createDTO.getDurationSeconds() : 60);
            reel.setStatus(savedRecipe.getStatus());
            reel.setAspectRatio("9:16");
            savedRecipe.setReel(reel);
        }

        Recipe finalRecipe = recipeRepository.save(savedRecipe);
        return mapToDTO(finalRecipe, contributor);
    }

    @Override
    public RecipeDTO updateRecipe(Long id, RecipeCreateDTO updateDTO, User currentUser) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));

        // Only author or admin can update
        if (!recipe.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You do not have permission to edit this recipe");
        }

        Category category = categoryRepository.findById(updateDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        recipe.setTitle(updateDTO.getTitle().trim());
        recipe.setDescription(updateDTO.getDescription());
        recipe.setCategory(category);
        recipe.setPrepTimeMinutes(updateDTO.getPrepTimeMinutes());
        recipe.setCookTimeMinutes(updateDTO.getCookTimeMinutes());
        recipe.setServings(updateDTO.getServings());
        recipe.setVegetarian(updateDTO.isVegetarian());

        try {
            recipe.setDifficulty(Difficulty.valueOf(updateDTO.getDifficulty().toUpperCase()));
        } catch (Exception ignored) {}

        if (updateDTO.getImageUrl() != null && !updateDTO.getImageUrl().trim().isEmpty()) {
            recipe.setImageUrl(updateDTO.getImageUrl());
        }

        // Resubmit status if requested
        if (updateDTO.isSubmitForReview() && recipe.getStatus() == RecipeStatus.DRAFT) {
            recipe.setStatus(RecipeStatus.PENDING);
        }

        // Update ingredients: clear and re-add
        recipe.getIngredients().clear();
        if (updateDTO.getIngredients() != null) {
            int order = 1;
            for (IngredientDTO ingDto : updateDTO.getIngredients()) {
                if (ingDto.getName() != null && !ingDto.getName().trim().isEmpty()) {
                    RecipeIngredient ingredient = new RecipeIngredient(
                            ingDto.getName().trim(),
                            ingDto.getQuantity(),
                            ingDto.getUnit(),
                            order++
                    );
                    recipe.addIngredient(ingredient);
                }
            }
        }

        // Update steps: clear and re-add
        recipe.getSteps().clear();
        if (updateDTO.getSteps() != null) {
            int stepNum = 1;
            for (StepDTO stepDto : updateDTO.getSteps()) {
                if (stepDto.getInstruction() != null && !stepDto.getInstruction().trim().isEmpty()) {
                    RecipeStep step = new RecipeStep(
                            stepNum++,
                            stepDto.getTitle() != null ? stepDto.getTitle() : "Step " + (stepNum - 1),
                            stepDto.getInstruction().trim(),
                            stepDto.getTimerMinutes(),
                            stepDto.getTip()
                    );
                    recipe.addStep(step);
                }
            }
        }

        // Update or attach reel
        if (updateDTO.getVideoUrl() != null && !updateDTO.getVideoUrl().trim().isEmpty()) {
            if (recipe.getReel() != null) {
                recipe.getReel().setVideoUrl(updateDTO.getVideoUrl());
                recipe.getReel().setThumbnailUrl(recipe.getImageUrl());
                recipe.getReel().setDurationSeconds(updateDTO.getDurationSeconds() != null ? updateDTO.getDurationSeconds() : 60);
                recipe.getReel().setStatus(recipe.getStatus());
            } else {
                RecipeReel reel = new RecipeReel();
                reel.setRecipe(recipe);
                reel.setVideoUrl(updateDTO.getVideoUrl());
                reel.setThumbnailUrl(recipe.getImageUrl());
                reel.setDurationSeconds(updateDTO.getDurationSeconds() != null ? updateDTO.getDurationSeconds() : 60);
                reel.setStatus(recipe.getStatus());
                recipe.setReel(reel);
            }
        }

        Recipe updated = recipeRepository.save(recipe);
        return mapToDTO(updated, currentUser);
    }

    @Override
    public void deleteRecipe(Long id, User currentUser) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));

        if (!recipe.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You do not have permission to delete this recipe");
        }

        recipeRepository.delete(recipe);
    }

    @Override
    public void recordRecipeView(Long id, User currentUser) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + id));

        recipe.setViewCount(recipe.getViewCount() + 1);
        recipeRepository.save(recipe);

        // Record in user's browsing history if logged in
        if (currentUser != null) {
            RecipeViewHistory history = viewHistoryRepository.findByRecipeAndUser(recipe, currentUser)
                    .orElse(new RecipeViewHistory(recipe, currentUser));
            history.setViewedAt(java.time.LocalDateTime.now());
            viewHistoryRepository.save(history);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDTO> getTrendingRecipes(User currentUser) {
        return recipeRepository.findTrendingRecipes().stream()
                .limit(8)
                .map(r -> mapToDTO(r, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDTO> getRecentRecipes(User currentUser) {
        return recipeRepository.findRecentRecipes().stream()
                .limit(8)
                .map(r -> mapToDTO(r, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDTO> getQuickRecipes(int maxMinutes, User currentUser) {
        return recipeRepository.findQuickRecipes(maxMinutes).stream()
                .limit(8)
                .map(r -> mapToDTO(r, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeDTO> getContributorRecipes(User contributor, RecipeStatus status) {
        List<Recipe> recipes;
        if (status != null) {
            recipes = recipeRepository.findByUserAndStatus(contributor, status);
        } else {
            recipes = recipeRepository.findByUser(contributor);
        }

        return recipes.stream()
                .map(r -> mapToDTO(r, contributor))
                .collect(Collectors.toList());
    }

    private RecipeDTO mapToDTO(Recipe recipe, User currentUser) {
        RecipeDTO dto = new RecipeDTO();
        dto.setId(recipe.getId());
        dto.setTitle(recipe.getTitle());
        dto.setSlug(recipe.getSlug());
        dto.setDescription(recipe.getDescription());
        dto.setPrepTimeMinutes(recipe.getPrepTimeMinutes());
        dto.setCookTimeMinutes(recipe.getCookTimeMinutes());
        dto.setServings(recipe.getServings());
        dto.setDifficulty(recipe.getDifficulty().name());
        dto.setVegetarian(recipe.isVegetarian());
        dto.setStatus(recipe.getStatus().name());
        dto.setRejectionReason(recipe.getRejectionReason());
        dto.setImageUrl(recipe.getImageUrl());
        dto.setViewCount(recipe.getViewCount());
        dto.setCreatedAt(recipe.getCreatedAt());
        dto.setUpdatedAt(recipe.getUpdatedAt());

        // Contributor
        if (recipe.getUser() != null) {
            dto.setContributorId(recipe.getUser().getId());
            dto.setContributorName(recipe.getUser().getName());
            dto.setContributorAvatar(recipe.getUser().getAvatarUrl());
        }

        // Category
        if (recipe.getCategory() != null) {
            dto.setCategoryId(recipe.getCategory().getId());
            dto.setCategoryName(recipe.getCategory().getName());
            dto.setCategorySlug(recipe.getCategory().getSlug());
        }

        // Ingredients
        if (recipe.getIngredients() != null) {
            dto.setIngredients(recipe.getIngredients().stream()
                    .map(i -> new IngredientDTO(i.getId(), i.getName(), i.getQuantity(), i.getUnit(), i.getOrderIndex()))
                    .collect(Collectors.toList()));
        }

        // Steps
        if (recipe.getSteps() != null) {
            dto.setSteps(recipe.getSteps().stream()
                    .map(s -> new StepDTO(s.getId(), s.getStepNumber(), s.getTitle(), s.getInstruction(), s.getTimerMinutes(), s.getTip()))
                    .collect(Collectors.toList()));
        }

        // Reel
        if (recipe.getReel() != null) {
            ReelDTO reelDTO = new ReelDTO();
            reelDTO.setId(recipe.getReel().getId());
            reelDTO.setRecipeId(recipe.getId());
            reelDTO.setRecipeTitle(recipe.getTitle());
            reelDTO.setRecipeDescription(recipe.getDescription());
            reelDTO.setCategoryName(recipe.getCategory() != null ? recipe.getCategory().getName() : "");
            reelDTO.setVegetarian(recipe.isVegetarian());
            reelDTO.setVideoUrl(recipe.getReel().getVideoUrl());
            reelDTO.setThumbnailUrl(recipe.getReel().getThumbnailUrl());
            reelDTO.setDurationSeconds(recipe.getReel().getDurationSeconds());
            reelDTO.setAspectRatio(recipe.getReel().getAspectRatio());
            reelDTO.setViewCount(recipe.getReel().getViewCount());
            reelDTO.setCreatedAt(recipe.getReel().getCreatedAt());

            if (recipe.getUser() != null) {
                reelDTO.setContributorId(recipe.getUser().getId());
                reelDTO.setContributorName(recipe.getUser().getName());
                reelDTO.setContributorAvatar(recipe.getUser().getAvatarUrl());
            }

            dto.setReel(reelDTO);
        }

        // Ratings & counts
        Double avg = ratingRepository.getAverageRatingForRecipe(recipe);
        dto.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        Long ratingsCount = ratingRepository.getRatingCountForRecipe(recipe);
        dto.setTotalRatings(ratingsCount != null ? ratingsCount : 0L);
        dto.setTotalLikes(likeRepository.countByRecipe(recipe));
        dto.setTotalComments(commentRepository.countByRecipe(recipe));

        // User states
        if (currentUser != null) {
            dto.setLikedByCurrentUser(likeRepository.existsByRecipeAndUser(recipe, currentUser));
            dto.setSavedByCurrentUser(savedRecipeRepository.existsByRecipeAndUser(recipe, currentUser));
            ratingRepository.findByRecipeAndUser(recipe, currentUser)
                    .ifPresent(r -> dto.setCurrentUserRating(r.getStars()));

            if (dto.getReel() != null) {
                dto.getReel().setLikedByCurrentUser(dto.isLikedByCurrentUser());
                dto.getReel().setSavedByCurrentUser(dto.isSavedByCurrentUser());
                dto.getReel().setLikeCount(dto.getTotalLikes());
                dto.getReel().setCommentCount(dto.getTotalComments());
            }
        }

        return dto;
    }
}
