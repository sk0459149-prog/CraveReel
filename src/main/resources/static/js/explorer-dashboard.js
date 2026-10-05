// Recipe Explorer Dashboard Logic

let currentCollectionFilter = 'ALL';

document.addEventListener('DOMContentLoaded', async () => {
  if (!getAuthToken()) {
    showToast('Please log in to view your Explorer Dashboard', 'error');
    setTimeout(() => window.location.href = 'login.html', 1200);
    return;
  }

  loadProfileInfo();
  loadDashboardSummary();
  loadBrowsingHistory();
  loadUserReviews();
  loadCollections();
});

// Load User Profile Info into Header & Form
async function loadProfileInfo() {
  try {
    const res = await apiFetch('/auth/me');
    if (res.success && res.data) {
      const u = res.data;
      document.getElementById('dashUserName').innerText = u.name;
      document.getElementById('dashUserBio').innerText = u.bio || 'Pantry master and enthusiastic home chef.';
      if (u.avatarUrl) {
        document.getElementById('dashAvatar').src = u.avatarUrl;
        document.getElementById('profAvatar').value = u.avatarUrl;
      }
      document.getElementById('profName').value = u.name;
      document.getElementById('profEmail').value = u.email;
      document.getElementById('profBio').value = u.bio || '';
      document.getElementById('profDiet').value = u.dietaryPreferences || '';
    }
  } catch (err) {
    console.error('Failed to load profile info', err);
  }
}

// Load Summary Stats
async function loadDashboardSummary() {
  try {
    const res = await apiFetch('/explorer/dashboard-summary');
    if (res.success && res.data) {
      document.getElementById('statSaved').innerText = res.data.savedCount || 0;
      document.getElementById('statReviews').innerText = res.data.reviewsCount || 0;
      document.getElementById('statHistory').innerText = res.data.historyCount || 0;
    }
  } catch (err) {
    console.error('Failed to load dashboard summary', err);
  }
}

// 1. Load Browsing History Table
async function loadBrowsingHistory() {
  const tbody = document.getElementById('historyTableBody');
  try {
    const res = await apiFetch('/explorer/history');
    if (res.success && res.data) {
      if (res.data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No browsing history yet. Explore some recipes!</td></tr>';
      } else {
        tbody.innerHTML = res.data.map(item => {
          const dateStr = new Date(item.savedAt).toLocaleDateString('en-US', {
            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
          });
          const totalMins = (item.prepTimeMinutes || 0) + (item.cookTimeMinutes || 0);

          return `
            <tr>
              <td>
                <div class="d-flex align-items-center gap-2">
                  <img src="${item.recipeImage || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800'}" style="width: 44px; height: 44px; border-radius: 8px; object-fit: cover;">
                  <span class="fw-bold text-white">${item.recipeTitle}</span>
                </div>
              </td>
              <td><span class="badge bg-warning bg-opacity-15 text-warning">${item.categoryName || 'General'}</span></td>
              <td>${formatDuration(totalMins)}</td>
              <td><span class="text-capitalize">${item.difficulty ? item.difficulty.toLowerCase() : 'Easy'}</span></td>
              <td class="small text-muted">${dateStr}</td>
              <td>
                <a href="recipe.html?id=${item.recipeId}" class="btn btn-sm btn-outline-warning">
                  <i class="bi bi-box-arrow-up-right"></i> View
                </a>
              </td>
            </tr>
          `;
        }).join('');
      }
    }
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-danger text-center py-3">Failed to load history: ${err.message}</td></tr>`;
  }
}

async function clearHistory() {
  if (!confirm('Are you sure you want to clear your browsing history?')) return;
  try {
    await apiFetch('/explorer/history', { method: 'DELETE' });
    showToast('Browsing history cleared', 'success');
    loadBrowsingHistory();
    loadDashboardSummary();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// 2. Load User Reviews
async function loadUserReviews() {
  const container = document.getElementById('userReviewsList');
  try {
    const res = await apiFetch('/explorer/reviews');
    if (res.success && res.data) {
      if (res.data.length === 0) {
        container.innerHTML = '<div class="text-center text-muted py-5">You haven\'t rated any recipes yet. Browse recipes to leave reviews!</div>';
      } else {
        container.innerHTML = res.data.map(r => `
          <div class="p-3 rounded-3" style="background: rgba(0, 0, 0, 0.25); border: 1px solid var(--border-color);">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <div>
                <a href="recipe.html?id=${r.recipeId}" class="fw-bold text-white fs-6 text-decoration-none">
                  ${r.recipeTitle}
                </a>
                <div class="small text-warning mt-1">${renderStars(r.stars)}</div>
              </div>
              <button class="btn btn-sm btn-outline-danger" onclick="deleteReview(${r.id})">
                <i class="bi bi-trash"></i> Delete
              </button>
            </div>
            ${r.reviewText ? `<p class="small text-secondary mb-0">${r.reviewText}</p>` : ''}
          </div>
        `).join('');
      }
    }
  } catch (err) {
    container.innerHTML = `<div class="text-danger small py-3">Failed to load reviews: ${err.message}</div>`;
  }
}

async function deleteReview(reviewId) {
  if (!confirm('Are you sure you want to delete this review?')) return;
  try {
    await apiFetch(`/explorer/reviews/${reviewId}`, { method: 'DELETE' });
    showToast('Review deleted successfully', 'success');
    loadUserReviews();
    loadDashboardSummary();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// 3. Load Collections
async function loadCollections(collectionName = '') {
  currentCollectionFilter = collectionName;
  const grid = document.getElementById('collectionsGrid');
  const filtersBar = document.getElementById('collectionFiltersBar');

  // Load collection filter buttons
  try {
    const colsRes = await apiFetch('/explorer/collections');
    if (colsRes.success && colsRes.data) {
      filtersBar.innerHTML = `
        <button class="btn btn-sm ${!collectionName ? 'btn-warning' : 'btn-rr-secondary'}" onclick="loadCollections('')">All</button>
        ${colsRes.data.map(c => `
          <button class="btn btn-sm ${collectionName === c ? 'btn-warning' : 'btn-rr-secondary'}" onclick="loadCollections('${c}')">${c}</button>
        `).join('')}
      `;
    }

    const endpoint = collectionName ? `/explorer/saved?collectionName=${encodeURIComponent(collectionName)}` : '/explorer/saved';
    const res = await apiFetch(endpoint);
    if (res.success && res.data) {
      if (res.data.length === 0) {
        grid.innerHTML = '<div class="col-12 text-center text-muted py-5">No recipes found in this collection. Click the bookmark icon on any recipe to save it here!</div>';
      } else {
        grid.innerHTML = res.data.map(recipe => {
          const totalMins = (recipe.prepTimeMinutes || 0) + (recipe.cookTimeMinutes || 0);
          return `
            <div class="col-sm-6 col-lg-4">
              <div class="recipe-card">
                <div class="recipe-card-media">
                  <img src="${recipe.recipeImage || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800'}" alt="${recipe.recipeTitle}">
                  <span class="badge bg-primary position-absolute top-0 start-0 m-2">${recipe.collectionName || 'Favorites'}</span>
                  <button class="card-save-btn saved" title="Remove from Collection" onclick="removeCollectionItem(event, ${recipe.recipeId})">
                    <i class="bi bi-bookmark-fill text-warning"></i>
                  </button>
                </div>
                <div class="recipe-card-body">
                  <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="small text-warning fw-semibold">${recipe.categoryName || 'General'}</span>
                    <span class="small text-white">★ ${recipe.averageRating > 0 ? recipe.averageRating : 'New'}</span>
                  </div>
                  <h5 class="recipe-card-title">${recipe.recipeTitle}</h5>
                  <div class="recipe-meta-row mt-auto">
                    <span><i class="bi bi-clock me-1"></i> ${formatDuration(totalMins)}</span>
                    <a href="recipe.html?id=${recipe.recipeId}" class="btn btn-sm btn-outline-warning py-0 px-2" style="font-size: 0.75rem;">View</a>
                  </div>
                </div>
              </div>
            </div>
          `;
        }).join('');
      }
    }
  } catch (err) {
    grid.innerHTML = `<div class="col-12 text-danger text-center py-4">Failed to load saved collection: ${err.message}</div>`;
  }
}

async function removeCollectionItem(event, recipeId) {
  event.stopPropagation();
  try {
    await apiFetch(`/recipes/${recipeId}/save`, { method: 'DELETE' });
    showToast('Recipe collection updated successfully', 'success');
    loadCollections(currentCollectionFilter);
    loadDashboardSummary();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// 4. Profile Management Form Submission
async function handleProfileUpdate(event) {
  event.preventDefault();
  const name = document.getElementById('profName').value.trim();
  const bio = document.getElementById('profBio').value.trim();
  const avatarUrl = document.getElementById('profAvatar').value.trim();
  const dietaryPreferences = document.getElementById('profDiet').value.trim();

  try {
    const res = await apiFetch('/users/profile', {
      method: 'PUT',
      body: JSON.stringify({ name, bio, avatarUrl, dietaryPreferences })
    });

    if (res.success && res.data) {
      setStoredUser(res.data);
      document.getElementById('dashUserName').innerText = res.data.name;
      document.getElementById('dashUserBio').innerText = res.data.bio || '';
      if (res.data.avatarUrl) {
        document.getElementById('dashAvatar').src = res.data.avatarUrl;
      }
      showToast('Profile updated successfully', 'success');
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}
