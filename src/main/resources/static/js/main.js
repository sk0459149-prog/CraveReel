// Landing Page JavaScript Logic

document.addEventListener('DOMContentLoaded', () => {
  loadCategories();
  loadTrendingReels();
  loadTrendingRecipes();
  loadIndianRecipes();
  loadQuickRecipes();
});

// 1. Load Categories
async function loadCategories() {
  const container = document.getElementById('homeCategoriesList');
  if (!container) return;

  try {
    const res = await apiFetch('/categories');
    if (res.success && res.data) {
      const items = res.data.map(cat => `
        <div class="category-chip" onclick="filterByCategory('${cat.id}')">
          <span>${cat.icon || '🍽️'}</span> ${cat.name}
        </div>
      `).join('');
      container.innerHTML = `
        <div class="category-chip active" onclick="filterByCategory('')">
          <span>✨</span> All Categories
        </div>
        ${items}
      `;
    }
  } catch (err) {
    console.error('Failed to load categories', err);
  }
}

function filterByCategory(catId) {
  if (!catId) {
    window.location.href = 'explore.html';
  } else {
    window.location.href = `explore.html?categoryId=${catId}`;
  }
}

// 2. Load Trending Reels for Homepage Carousel
async function loadTrendingReels() {
  const container = document.getElementById('trendingReelsContainer');
  if (!container) return;

  try {
    const res = await apiFetch('/reels');
    if (res.success && res.data && res.data.length > 0) {
      container.innerHTML = res.data.map(reel => `
        <div class="reel-preview-card" onclick="window.location.href='reels.html?id=${reel.id}'">
          ${isYouTubeUrl(reel.videoUrl)
            ? `<img src="https://i.ytimg.com/vi/${getYouTubeId(reel.videoUrl)}/hqdefault.jpg" alt="${reel.recipeTitle}" loading="lazy" style="width:100%;height:100%;object-fit:cover;">`
            : `<video src="${reel.videoUrl}" muted loop preload="metadata" onmouseover="this.play()" onmouseout="this.pause()"></video>`}
          <div class="reel-preview-overlay">
            <div class="reel-preview-top">
              <span class="reel-play-chip"><i class="bi bi-play-fill text-warning"></i> Reel</span>
              <span class="badge bg-dark bg-opacity-75"><i class="bi bi-eye"></i> ${formatNumber(reel.viewCount)}</span>
            </div>
            <div class="reel-preview-bottom text-start">
              <div class="reel-preview-title">${reel.recipeTitle}</div>
              <div class="reel-preview-author">
                <img src="${reel.contributorAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150'}" style="width: 20px; height: 20px; border-radius: 50%;">
                <span class="text-truncate">${reel.contributorName}</span>
              </div>
              <a href="recipe.html?id=${reel.recipeId}" class="btn btn-sm btn-cook-this mt-2 py-1 px-3 w-100 justify-content-center" onclick="event.stopPropagation()">
                🍳 Cook This
              </a>
            </div>
          </div>
        </div>
      `).join('');
    } else {
      container.innerHTML = '<div class="text-muted p-4">No trending reels available.</div>';
    }
  } catch (err) {
    container.innerHTML = '<div class="text-muted p-4">Unable to load reels.</div>';
  }
}

// 3. Load Trending Recipes
async function loadTrendingRecipes() {
  const container = document.getElementById('trendingRecipesContainer');
  if (!container) return;

  try {
    const res = await apiFetch('/recipes/trending');
    if (res.success && res.data) {
      container.innerHTML = res.data.slice(0, 4).map(recipe => createRecipeCardHTML(recipe)).join('');
    }
  } catch (err) {
    container.innerHTML = '<div class="text-muted p-4">Failed to load recipes.</div>';
  }
}

// 3b. Indian Kitchen Spotlight
async function loadIndianRecipes() {
  const container = document.getElementById('indianRecipesContainer');
  if (!container) return;

  try {
    const res = await apiFetch('/recipes?query=Indian&sortBy=popular');
    if (res.success && res.data) {
      const indian = res.data.filter(r => (r.categoryName || '').toLowerCase() === 'indian');
      const recipes = (indian.length ? indian : res.data).slice(0, 6);
      container.innerHTML = recipes.length
        ? recipes.map(recipe => createRecipeCardHTML(recipe)).join('')
        : '<div class="col-12 text-muted">No Indian recipes available yet.</div>';
    }
  } catch (err) {
    container.innerHTML = '<div class="col-12 text-muted">Unable to load Indian recipes.</div>';
  }
}

// 4. Load Quick Recipes
async function loadQuickRecipes() {
  const container = document.getElementById('quickRecipesContainer');
  if (!container) return;

  try {
    const res = await apiFetch('/recipes/quick?maxMinutes=30');
    if (res.success && res.data) {
      container.innerHTML = res.data.slice(0, 4).map(recipe => createRecipeCardHTML(recipe)).join('');
    }
  } catch (err) {
    console.error('Failed to load quick recipes', err);
  }
}

// Shared Recipe Card Component Generator
function createRecipeCardHTML(recipe) {
  const isVeg = recipe.vegetarian;
  const dietBadge = isVeg
    ? '<span class="badge-diet badge-veg"><i class="bi bi-circle-fill" style="font-size: 0.5rem;"></i> Veg</span>'
    : '<span class="badge-diet badge-non-veg"><i class="bi bi-triangle-fill" style="font-size: 0.5rem;"></i> Non-Veg</span>';

  const reelBadge = recipe.reel
    ? '<span class="badge-reel"><i class="bi bi-camera-reels-fill"></i> Video</span>'
    : '';

  const savedClass = recipe.savedByCurrentUser ? 'saved' : '';
  const totalMins = (recipe.prepTimeMinutes || 0) + (recipe.cookTimeMinutes || 0);

  return `
    <div class="col-sm-6 col-lg-3">
      <div class="recipe-card" onclick="window.location.href='recipe.html?id=${recipe.id}'" style="cursor: pointer;">
        <div class="recipe-card-media">
          <img src="${recipe.imageUrl || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800'}" alt="${recipe.title}" loading="lazy">
          <div class="position-absolute top-0 start-0 m-2 d-flex gap-1 z-1">
            ${dietBadge}
            ${reelBadge}
          </div>
          <button class="card-save-btn ${savedClass}" title="Save to Collection" onclick="handleCardSave(event, ${recipe.id}, this)">
            <i class="bi ${recipe.savedByCurrentUser ? 'bi-bookmark-fill' : 'bi-bookmark'}"></i>
          </button>
        </div>

        <div class="recipe-card-body">
          <div class="d-flex align-items-center justify-content-between mb-1">
            <span class="small text-warning fw-semibold">${recipe.categoryName || 'General'}</span>
            <div class="small text-muted d-flex align-items-center gap-1">
              <i class="bi bi-star-fill text-warning"></i>
              <span class="fw-bold text-white">${recipe.averageRating > 0 ? recipe.averageRating : 'New'}</span>
              <span class="text-muted">(${recipe.totalRatings})</span>
            </div>
          </div>

          <h5 class="recipe-card-title">${recipe.title}</h5>
          <p class="recipe-card-desc">${recipe.description || ''}</p>

          <div class="recipe-meta-row">
            <span title="Total Cook Time"><i class="bi bi-clock me-1"></i> ${formatDuration(totalMins)}</span>
            <span title="Difficulty level" class="text-capitalize"><i class="bi bi-bar-chart me-1"></i> ${recipe.difficulty.toLowerCase()}</span>
            <span title="Contributor"><i class="bi bi-person me-1"></i> ${recipe.contributorName ? recipe.contributorName.split(' ')[0] : 'Chef'}</span>
          </div>
        </div>
      </div>
    </div>
  `;
}

// Save Recipe from card
async function handleCardSave(event, recipeId, btn) {
  event.stopPropagation();
  if (!getAuthToken()) {
    showToast('Please log in to save recipes to your collection', 'error');
    setTimeout(() => window.location.href = 'login.html', 1200);
    return;
  }

  const isSaved = btn.classList.contains('saved');
  try {
    if (isSaved) {
      await apiFetch(`/recipes/${recipeId}/save`, { method: 'DELETE' });
      btn.classList.remove('saved');
      btn.innerHTML = '<i class="bi bi-bookmark"></i>';
      showToast('Recipe collection updated successfully', 'success');
    } else {
      await apiFetch(`/recipes/${recipeId}/save?collectionName=Favorites`, { method: 'POST' });
      btn.classList.add('saved');
      btn.innerHTML = '<i class="bi bi-bookmark-fill"></i>';
      showToast('Recipe collection updated successfully', 'success');
    }
  } catch (err) {
    showToast(err.message || 'Failed to update recipe collection', 'error');
  }
}


function isYouTubeUrl(url) {
  return typeof url === 'string' && /(?:youtube\.com\/(?:watch\?v=|embed\/)|youtu\.be\/)/i.test(url);
}

function getYouTubeId(url) {
  const m = String(url).match(/(?:youtube\.com\/(?:watch\?v=|embed\/)|youtu\.be\/)([A-Za-z0-9_-]{11})/i);
  return m ? m[1] : '';
}
