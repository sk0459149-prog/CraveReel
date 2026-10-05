// Recipe Detail Page Controller

let recipeId = null;
let recipeData = null;
let isReelActive = false;

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  recipeId = params.get('id');

  if (!recipeId) {
    window.location.href = 'explore.html';
    return;
  }

  loadRecipeDetails(recipeId);
});

// Load Complete Recipe Details
async function loadRecipeDetails(id) {
  try {
    const res = await apiFetch(`/recipes/${id}`);
    if (res.success && res.data) {
      recipeData = res.data;
      renderRecipePage(recipeData);

      // Record view in background
      apiFetch(`/recipes/${id}/view`, { method: 'POST' }).catch(() => {});

      // Load comments & reviews
      loadReviews(id);
      loadComments(id);
      loadRelatedRecipes(recipeData.categoryId);
    }
  } catch (err) {
    document.getElementById('recipeLoadingSpinner').innerHTML = `
      <div class="text-danger py-5">
        <i class="bi bi-exclamation-octagon display-4"></i>
        <h4 class="mt-2 text-white">Recipe Not Found</h4>
        <p class="text-secondary">${err.message}</p>
        <a href="explore.html" class="btn btn-rr-primary">Back to Explore</a>
      </div>
    `;
  }
}

// Render Recipe DOM
function renderRecipePage(recipe) {
  document.getElementById('recipeLoadingSpinner').classList.add('d-none');
  document.getElementById('recipeContent').classList.remove('d-none');

  document.title = `${recipe.title} – CraveReel`;

  // Breadcrumbs & Title
  document.getElementById('breadcrumbCategory').innerText = recipe.categoryName || 'General';
  document.getElementById('recipeCategoryBadge').innerText = recipe.categoryName || 'General';
  document.getElementById('recipeTitle').innerText = recipe.title;
  document.getElementById('recipeDesc').innerText = recipe.description || '';

  // Badges
  document.getElementById('recipeDietBadge').innerHTML = recipe.vegetarian
    ? '<span class="badge-diet badge-veg"><i class="bi bi-circle-fill" style="font-size: 0.5rem;"></i> Vegetarian</span>'
    : '<span class="badge-diet badge-non-veg"><i class="bi bi-triangle-fill" style="font-size: 0.5rem;"></i> Non-Vegetarian</span>';
  document.getElementById('recipeDifficultyBadge').innerText = recipe.difficulty;

  // Author & Stats
  document.getElementById('recipeAuthorName').innerText = recipe.contributorName;
  if (recipe.contributorAvatar) {
    document.getElementById('recipeAuthorAvatar').src = recipe.contributorAvatar;
    document.getElementById('chefCardAvatar').src = recipe.contributorAvatar;
  }
  document.getElementById('chefCardName').innerText = recipe.contributorName;

  document.getElementById('recipePrepTime').innerText = `${recipe.prepTimeMinutes || 0} mins`;
  document.getElementById('recipeCookTime').innerText = `${recipe.cookTimeMinutes || 0} mins`;
  document.getElementById('recipeServings').innerText = `${recipe.servings || 2} Servings`;
  document.getElementById('recipeRatingNum').innerText = recipe.averageRating > 0 ? recipe.averageRating : 'New';
  document.getElementById('recipeRatingCount').innerText = `(${recipe.totalRatings})`;

  // CTAs
  const cookUrl = `cook.html?id=${recipe.id}`;
  document.getElementById('startCookingBtn').href = cookUrl;
  document.getElementById('startCookingBtn2').href = cookUrl;
  document.getElementById('startCookingBtn3').href = cookUrl;

  // Likes & Saves State
  updateLikeButtonUI(recipe.likedByCurrentUser, recipe.totalLikes);
  updateSaveButtonUI(recipe.savedByCurrentUser);

  // User's own rating if exists
  if (recipe.currentUserRating) {
    setRatingScore(recipe.currentUserRating);
  }

  // Media (Reel video vs Photo)
  const image = recipe.imageUrl || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800';
  document.getElementById('recipeMainImage').src = image;

  if (recipe.reel && recipe.reel.videoUrl) {
    document.getElementById('reelToggleBar').classList.remove('d-none');
    const video = document.getElementById('recipeDetailVideo');
    const url = recipe.reel.videoUrl;
    const youtubeId = getYouTubeIdFromUrl(url);
    if (youtubeId) {
      video.classList.add('d-none');
      const holder = document.getElementById('reelPlayerContainer');
      holder.insertAdjacentHTML('afterbegin', `<iframe id="recipeYouTubeVideo" src="https://www.youtube.com/embed/${youtubeId}?rel=0" title="${recipe.title} recipe video" style="width:100%;height:520px;border:0;" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>`);
    } else {
      video.classList.remove('d-none');
      video.src = url;
    }
  }

  // Dynamic Ingredients Checklist
  const ingList = document.getElementById('recipeIngredientsList');
  document.getElementById('ingredientsHeaderCount').innerText = `${recipe.ingredients.length} Items`;

  if (recipe.ingredients && recipe.ingredients.length > 0) {
    ingList.innerHTML = recipe.ingredients.map((ing, idx) => `
      <li class="list-group-item bg-transparent border-secondary border-opacity-10 py-2 px-1 d-flex align-items-center justify-content-between text-secondary" style="cursor: pointer;" onclick="toggleIngredientCheck(this)">
        <div class="d-flex align-items-center gap-2">
          <input class="form-check-input me-1" type="checkbox" id="ing-chk-${idx}">
          <label class="form-check-label mb-0" for="ing-chk-${idx}">${ing.name}</label>
        </div>
        <span class="badge bg-secondary bg-opacity-25 text-white fw-semibold">
          ${ing.quantity || ''} ${ing.unit || ''}
        </span>
      </li>
    `).join('');
  } else {
    ingList.innerHTML = '<li class="list-group-item bg-transparent text-muted">No specific ingredients listed.</li>';
  }

  // Step-by-Step Instructions
  const stepsList = document.getElementById('recipeStepsList');
  if (recipe.steps && recipe.steps.length > 0) {
    stepsList.innerHTML = recipe.steps.map(step => `
      <div class="p-3 rounded-3" style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--border-color);">
        <div class="d-flex justify-content-between align-items-center mb-2">
          <div class="d-flex align-items-center gap-2">
            <span class="badge bg-warning text-dark fw-bold rounded-circle d-flex align-items-center justify-content-center" style="width: 28px; height: 28px;">${step.stepNumber}</span>
            <span class="fw-bold text-white fs-6">${step.title || 'Step ' + step.stepNumber}</span>
          </div>
          ${step.timerMinutes ? `<span class="badge bg-danger bg-opacity-20 text-danger"><i class="bi bi-clock me-1"></i> ${step.timerMinutes} mins</span>` : ''}
        </div>
        <p class="text-secondary mb-2" style="font-size: 0.95rem;">${step.instruction}</p>
        ${step.tip ? `<div class="p-2 rounded bg-secondary bg-opacity-10 text-warning small"><i class="bi bi-lightbulb-fill me-1"></i> Chef's Tip: ${step.tip}</div>` : ''}
      </div>
    `).join('');
  } else {
    stepsList.innerHTML = '<div class="text-muted">No instructions provided yet.</div>';
  }
}

// Toggle Check on Ingredient
function toggleIngredientCheck(li) {
  const chk = li.querySelector('input[type="checkbox"]');
  chk.checked = !chk.checked;
  const label = li.querySelector('label');
  if (chk.checked) {
    label.style.textDecoration = 'line-through';
    label.style.opacity = '0.5';
  } else {
    label.style.textDecoration = 'none';
    label.style.opacity = '1';
  }
}

// Toggle Media View (Reel Video vs Picture)
function toggleMediaView() {
  isReelActive = !isReelActive;
  const videoCont = document.getElementById('reelPlayerContainer');
  const imgCont = document.getElementById('recipeImageContainer');
  const btn = document.getElementById('switchMediaBtn');
  const video = document.getElementById('recipeDetailVideo');

  if (isReelActive) {
    imgCont.classList.add('d-none');
    videoCont.classList.remove('d-none');
    btn.innerHTML = '<i class="bi bi-image"></i> Show Photo';
    if (!video.classList.contains('d-none')) video.play().catch(() => {});
  } else {
    video.pause();
    videoCont.classList.add('d-none');
    imgCont.classList.remove('d-none');
    btn.innerHTML = '<i class="bi bi-play-fill"></i> Watch Recipe Video';
  }
}

// Like Button
async function toggleRecipeLike() {
  if (!getAuthToken()) {
    showToast('Please log in to like this recipe', 'error');
    return;
  }

  try {
    const res = await apiFetch(`/recipes/${recipeId}/like`, { method: 'POST' });
    const isLiked = res.data && res.data.liked;
    let count = parseInt(document.getElementById('recipeLikeCount').innerText || '0', 10);
    count = isLiked ? count + 1 : Math.max(0, count - 1);
    updateLikeButtonUI(isLiked, count);
    showToast(isLiked ? 'Recipe liked' : 'Recipe unliked', 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function updateLikeButtonUI(isLiked, count) {
  const btn = document.getElementById('recipeLikeBtn');
  const icon = document.getElementById('recipeLikeIcon');
  const countSpan = document.getElementById('recipeLikeCount');
  countSpan.innerText = count;

  if (isLiked) {
    btn.classList.add('btn-outline-danger');
    btn.classList.remove('btn-rr-secondary');
    icon.className = 'bi bi-heart-fill text-danger';
  } else {
    btn.classList.remove('btn-outline-danger');
    btn.classList.add('btn-rr-secondary');
    icon.className = 'bi bi-heart';
  }
}

// Save to Collection
async function toggleRecipeSave() {
  if (!getAuthToken()) {
    showToast('Please log in to save recipes to your collection', 'error');
    return;
  }

  const isSaved = document.getElementById('recipeSaveIcon').classList.contains('bi-bookmark-fill');
  try {
    if (isSaved) {
      await apiFetch(`/recipes/${recipeId}/save`, { method: 'DELETE' });
      updateSaveButtonUI(false);
      showToast('Recipe collection updated successfully', 'success');
    } else {
      await apiFetch(`/recipes/${recipeId}/save?collectionName=Favorites`, { method: 'POST' });
      updateSaveButtonUI(true);
      showToast('Recipe collection updated successfully', 'success');
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function updateSaveButtonUI(isSaved) {
  const icon = document.getElementById('recipeSaveIcon');
  if (isSaved) {
    icon.className = 'bi bi-bookmark-fill text-warning';
  } else {
    icon.className = 'bi bi-bookmark';
  }
}

// Share Recipe
function shareRecipe() {
  const url = window.location.href;
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      showToast('Recipe link copied to clipboard!', 'success');
    });
  } else {
    showToast('Recipe URL: ' + url, 'success');
  }
}

// Interactive Star Picker
function setRatingScore(score) {
  document.getElementById('selectedStarScore').value = score;
  const stars = document.querySelectorAll('.star-rate');
  stars.forEach((s, idx) => {
    if (idx < score) {
      s.className = 'bi bi-star-fill text-warning star-rate';
    } else {
      s.className = 'bi bi-star text-secondary star-rate';
    }
  });
}

// Submit Rating & Review
async function handleRatingSubmit(event) {
  event.preventDefault();
  if (!getAuthToken()) {
    showToast('Please log in to submit a rating and review', 'error');
    return;
  }

  const stars = parseInt(document.getElementById('selectedStarScore').value, 10);
  if (!stars || stars < 1 || stars > 5) {
    showToast('Please select between 1 and 5 stars', 'error');
    return;
  }

  const reviewText = document.getElementById('reviewTextInput').value.trim();

  try {
    const res = await apiFetch(`/recipes/${recipeId}/ratings`, {
      method: 'POST',
      body: JSON.stringify({ stars, reviewText })
    });

    const msg = reviewText ? 'Review submitted successfully' : 'Rating submitted successfully';
    showToast(msg, 'success');
    loadRecipeDetails(recipeId);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Load Community Reviews
async function loadReviews(id) {
  const container = document.getElementById('recipeReviewsList');
  const countSpan = document.getElementById('reviewsCount');

  try {
    const res = await apiFetch(`/recipes/${id}/ratings`);
    if (res.success && res.data) {
      countSpan.innerText = res.data.length;
      if (res.data.length === 0) {
        container.innerHTML = '<div class="text-muted small py-2">No reviews yet. Be the first to share your experience!</div>';
      } else {
        container.innerHTML = res.data.map(r => `
          <div class="p-3 rounded-3" style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--border-color);">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <div class="d-flex align-items-center gap-2">
                <img src="${r.userAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150'}" style="width: 28px; height: 28px; border-radius: 50%; object-fit: cover;">
                <span class="fw-bold small text-white">${r.userName}</span>
              </div>
              <div class="small text-warning">${renderStars(r.stars)}</div>
            </div>
            ${r.reviewText ? `<p class="small text-secondary mb-0 mt-2">${r.reviewText}</p>` : ''}
          </div>
        `).join('');
      }
    }
  } catch (err) {
    console.error('Failed to load reviews', err);
  }
}

// Submit Comment
async function handleCommentSubmit(event) {
  event.preventDefault();
  if (!getAuthToken()) {
    showToast('Please log in to comment', 'error');
    return;
  }

  const input = document.getElementById('recipeCommentInput');
  const content = input.value.trim();
  if (!content) return;

  try {
    await apiFetch(`/recipes/${recipeId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content })
    });
    input.value = '';
    showToast('Comment added successfully', 'success');
    loadComments(recipeId);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Load Comments
async function loadComments(id) {
  const container = document.getElementById('recipeCommentsList');
  try {
    const res = await apiFetch(`/recipes/${id}/comments`);
    if (res.success && res.data) {
      if (res.data.length === 0) {
        container.innerHTML = '<div class="text-muted small py-2">No questions or comments yet.</div>';
      } else {
        container.innerHTML = res.data.map(c => `
          <div class="p-3 rounded-3" style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--border-color);">
            <div class="d-flex align-items-center gap-2 mb-1">
              <img src="${c.userAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150'}" style="width: 28px; height: 28px; border-radius: 50%; object-fit: cover;">
              <span class="fw-bold small text-white">${c.userName}</span>
              <span class="badge bg-secondary bg-opacity-25" style="font-size: 0.65rem;">${c.userRole === 'ROLE_CONTRIBUTOR' ? 'Chef' : 'Explorer'}</span>
            </div>
            <p class="small text-secondary mb-0 mt-1">${c.content}</p>
          </div>
        `).join('');
      }
    }
  } catch (err) {
    console.error('Failed to load comments', err);
  }
}

// Load Related Recipes
async function loadRelatedRecipes(categoryId) {
  const container = document.getElementById('relatedRecipesList');
  if (!container || !categoryId) return;

  try {
    const res = await apiFetch(`/recipes?categoryId=${categoryId}&sortBy=popular`);
    if (res.success && res.data) {
      const others = res.data.filter(r => r.id.toString() !== recipeId.toString()).slice(0, 3);
      if (others.length === 0) {
        container.innerHTML = '<div class="text-muted small">No other recipes in this category yet.</div>';
      } else {
        container.innerHTML = others.map(r => `
          <div class="d-flex align-items-center gap-3 p-2 rounded-3" style="background: rgba(0, 0, 0, 0.2); cursor: pointer;" onclick="window.location.href='recipe.html?id=${r.id}'">
            <img src="${r.imageUrl || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800'}" style="width: 60px; height: 60px; border-radius: var(--radius-sm); object-fit: cover;">
            <div class="flex-grow-1 overflow-hidden">
              <div class="fw-bold text-white small text-truncate">${r.title}</div>
              <div class="text-warning small" style="font-size: 0.75rem;">★ ${r.averageRating > 0 ? r.averageRating : 'New'}</div>
              <div class="text-muted small" style="font-size: 0.75rem;">${(r.prepTimeMinutes || 0) + (r.cookTimeMinutes || 0)} mins</div>
            </div>
          </div>
        `).join('');
      }
    }
  } catch (err) {
    console.error('Failed to load related recipes', err);
  }
}

// Direct Message Modal
function openDirectMessageModal() {
  if (!getAuthToken()) {
    showToast('Please log in to message the chef', 'error');
    return;
  }
  const modal = new bootstrap.Modal(document.getElementById('dmModal'));
  modal.show();
}

async function handleSendDM(event) {
  event.preventDefault();
  if (!recipeData || !recipeData.contributorId) return;

  const subject = document.getElementById('dmSubject').value.trim();
  const content = document.getElementById('dmContent').value.trim();

  try {
    await apiFetch('/messages', {
      method: 'POST',
      body: JSON.stringify({
        recipientId: recipeData.contributorId,
        subject,
        content
      })
    });
    bootstrap.Modal.getInstance(document.getElementById('dmModal')).hide();
    document.getElementById('dmSubject').value = '';
    document.getElementById('dmContent').value = '';
    showToast('Message sent successfully', 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}


function getYouTubeIdFromUrl(url) {
  const m = String(url).match(/(?:youtube\.com\/(?:watch\?v=|embed\/)|youtu\.be\/)([A-Za-z0-9_-]{11})/i);
  return m ? m[1] : '';
}
