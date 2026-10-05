// Contributor Dashboard JavaScript Logic

let categoriesList = [];

document.addEventListener('DOMContentLoaded', async () => {
  if (!getAuthToken()) {
    showToast('Please log in as a Contributor to access this dashboard', 'error');
    setTimeout(() => window.location.href = 'login.html', 1200);
    return;
  }

  await loadCategories();
  loadStats();
  loadContributorRecipes();
  loadInteractions();
  loadChefProfile();

  // If initial new recipe rows empty, initialize defaults
  initDynamicFormDefaults();

  // Check URL query parameters (e.g. ?tab=add)
  const params = new URLSearchParams(window.location.search);
  if (params.get('tab') === 'add') {
    switchToAddRecipeTab();
  }
});

// Load Categories for select input
async function loadCategories() {
  const select = document.getElementById('formRecipeCategory');
  if (!select) return;

  try {
    const res = await apiFetch('/categories');
    if (res.success && res.data) {
      categoriesList = res.data;
      select.innerHTML = res.data.map(c => `
        <option value="${c.id}">${c.icon || '🍽️'} ${c.name}</option>
      `).join('');
    }
  } catch (err) {
    console.error('Failed to load categories', err);
  }
}

// 1. Load Contributor Metrics
async function loadStats() {
  try {
    const res = await apiFetch('/contributor/stats');
    if (res.success && res.data) {
      const s = res.data;
      document.getElementById('statMyRecipes').innerText = s.totalRecipes || 0;
      document.getElementById('statApproved').innerText = s.approvedRecipes || 0;
      document.getElementById('statPending').innerText = s.pendingRecipes || 0;
      document.getElementById('statViews').innerText = formatNumber(s.totalViews);
      document.getElementById('statLikes').innerText = formatNumber(s.totalLikes);
      document.getElementById('statRating').innerText = `${s.averageRating > 0 ? s.averageRating : '0.0'} ★`;
    }
  } catch (err) {
    console.error('Failed to load stats', err);
  }
}

// 2. Load Contributor Personal Recipes
async function loadContributorRecipes() {
  const status = document.getElementById('statusFilterSelect').value;
  const tbody = document.getElementById('contributorRecipesTable');

  tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted"><div class="spinner-border text-warning spinner-border-sm"></div> Loading your recipes...</td></tr>';

  try {
    const endpoint = status && status !== 'ALL' ? `/contributor/recipes?status=${status}` : '/contributor/recipes';
    const res = await apiFetch(endpoint);
    if (res.success && res.data) {
      if (res.data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No recipes found. Share your first recipe!</td></tr>';
      } else {
        tbody.innerHTML = res.data.map(r => {
          let statusBadge = '<span class="badge bg-secondary">Draft</span>';
          if (r.status === 'APPROVED') statusBadge = '<span class="badge bg-success">Approved / Live</span>';
          else if (r.status === 'PENDING') statusBadge = '<span class="badge bg-warning text-dark">Pending Review</span>';
          else if (r.status === 'REJECTED') statusBadge = `<span class="badge bg-danger" title="${r.rejectionReason || ''}">Rejected</span>`;

          const reelBadge = r.reel ? '<span class="badge bg-warning text-dark"><i class="bi bi-camera-reels-fill"></i> Attached</span>' : '<span class="text-muted small">None</span>';

          return `
            <tr>
              <td>
                <div class="d-flex align-items-center gap-2">
                  <img src="${r.imageUrl || 'https://images.unsplash.com/photo-1495521821757-a1efb6729352?w=800'}" style="width: 44px; height: 44px; border-radius: 8px; object-fit: cover;">
                  <div>
                    <div class="fw-bold text-white">${r.title}</div>
                    <div class="small text-muted">${(r.prepTimeMinutes || 0) + (r.cookTimeMinutes || 0)} mins · ${r.difficulty}</div>
                  </div>
                </div>
              </td>
              <td><span class="badge bg-warning bg-opacity-15 text-warning">${r.categoryName}</span></td>
              <td>${statusBadge}</td>
              <td class="text-white">${formatNumber(r.viewCount)}</td>
              <td class="text-white">${formatNumber(r.totalLikes)}</td>
              <td>${reelBadge}</td>
              <td>
                <div class="d-flex gap-2">
                  <button class="btn btn-sm btn-outline-warning" title="Edit Recipe" onclick="editRecipe(${r.id})">
                    <i class="bi bi-pencil-square"></i>
                  </button>
                  <a href="recipe.html?id=${r.id}" class="btn btn-sm btn-outline-info" title="Preview Recipe" target="_blank">
                    <i class="bi bi-eye"></i>
                  </a>
                  <button class="btn btn-sm btn-outline-danger" title="Delete Recipe" onclick="deleteContributorRecipe(${r.id})">
                    <i class="bi bi-trash"></i>
                  </button>
                </div>
              </td>
            </tr>
          `;
        }).join('');
      }
    }
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-danger text-center py-4">Failed to load recipes: ${err.message}</td></tr>`;
  }
}

// 3. Dynamic Ingredients Builder
function addIngredientRow(name = '', quantity = '', unit = 'pieces') {
  const container = document.getElementById('ingredientsContainer');
  const row = document.createElement('div');
  row.className = 'ingredient-row d-flex gap-2 align-items-center';
  row.innerHTML = `
    <input type="text" class="form-control form-control-sm ing-name" placeholder="Ingredient (e.g. Tomato)" value="${name}" required style="flex: 2;">
    <input type="text" class="form-control form-control-sm ing-qty" placeholder="Qty (e.g. 2)" value="${quantity}" style="flex: 1;">
    <input type="text" class="form-control form-control-sm ing-unit" placeholder="Unit (e.g. pieces)" value="${unit}" style="flex: 1;">
    <button type="button" class="btn btn-sm btn-outline-danger" onclick="this.parentElement.remove()" title="Remove">
      <i class="bi bi-x-lg"></i>
    </button>
  `;
  container.appendChild(row);
}

// 4. Dynamic Steps Builder
function addStepRow(title = '', instruction = '', timerMinutes = '') {
  const container = document.getElementById('stepsContainer');
  const stepNum = container.children.length + 1;
  const row = document.createElement('div');
  row.className = 'step-row p-3 rounded-3 position-relative';
  row.style.background = 'rgba(0, 0, 0, 0.3)';
  row.style.border = '1px solid var(--border-color)';

  row.innerHTML = `
    <div class="d-flex justify-content-between align-items-center mb-2">
      <span class="badge bg-warning text-dark fw-bold">Step ${stepNum}</span>
      <button type="button" class="btn btn-sm btn-outline-danger py-0 px-2" onclick="removeStepRow(this)" title="Remove Step">
        <i class="bi bi-trash"></i>
      </button>
    </div>
    <div class="row g-2 mb-2">
      <div class="col-md-8">
        <input type="text" class="form-control form-control-sm step-title-input" placeholder="Step Title (e.g. Saute Onions)" value="${title}">
      </div>
      <div class="col-md-4">
        <input type="number" class="form-control form-control-sm step-timer-input" placeholder="Timer (optional mins)" value="${timerMinutes}">
      </div>
    </div>
    <textarea class="form-control form-control-sm step-instruction-input" rows="2" placeholder="Instruction details..." required>${instruction}</textarea>
  `;
  container.appendChild(row);
}

function removeStepRow(btn) {
  btn.closest('.step-row').remove();
  // Renumber badges
  const container = document.getElementById('stepsContainer');
  Array.from(container.children).forEach((r, idx) => {
    const badge = r.querySelector('.badge');
    if (badge) badge.innerText = `Step ${idx + 1}`;
  });
}

function initDynamicFormDefaults() {
  const ingCont = document.getElementById('ingredientsContainer');
  if (ingCont && ingCont.children.length === 0) {
    addIngredientRow('Ripe Tomatoes', '4', 'pieces');
    addIngredientRow('Onion', '1', 'piece');
    addIngredientRow('Olive Oil', '2', 'tbsp');
  }

  const stepCont = document.getElementById('stepsContainer');
  if (stepCont && stepCont.children.length === 0) {
    addStepRow('Prep Ingredients', 'Chop the vegetables into uniform pieces.');
    addStepRow('Cook & Simmer', 'Heat oil in pan, sauté aromatics, and simmer until tender.', '8');
  }
}

// 5. File Upload Handlers (Image & Video)
async function uploadRecipePhoto(input) {
  if (!input.files || input.files.length === 0) return;
  const file = input.files[0];

  const formData = new FormData();
  formData.append('file', file);

  try {
    showToast('Uploading recipe image...', 'success');
    const res = await apiFetch('/contributor/upload-image', {
      method: 'POST',
      body: formData
    });
    if (res.success && res.data && res.data.url) {
      document.getElementById('formPhotoUrl').value = res.data.url;
      showToast('Image uploaded successfully', 'success');
    }
  } catch (err) {
    showToast(err.message || 'Image upload failed', 'error');
  }
}

async function uploadRecipeVideo(input) {
  if (!input.files || input.files.length === 0) return;
  const file = input.files[0];

  const progress = document.getElementById('videoUploadProgress');
  progress.classList.remove('d-none');

  const formData = new FormData();
  formData.append('file', file);

  try {
    showToast('Uploading cooking reel video...', 'success');
    const res = await apiFetch('/contributor/upload-video', {
      method: 'POST',
      body: formData
    });
    progress.classList.add('d-none');
    if (res.success && res.data && res.data.url) {
      document.getElementById('formVideoUrl').value = res.data.url;
      showToast('Video uploaded successfully', 'success');
    }
  } catch (err) {
    progress.classList.add('d-none');
    showToast(err.message || 'Video upload failed', 'error');
  }
}

// 6. Submit Recipe Form (Create or Update)
async function handleRecipeFormSubmit(event) {
  event.preventDefault();
  await submitRecipe(true);
}

async function submitRecipeAsDraft() {
  await submitRecipe(false);
}

async function submitRecipe(submitForReview) {
  const editId = document.getElementById('editingRecipeId').value;
  const title = document.getElementById('formRecipeTitle').value.trim();
  const categoryId = document.getElementById('formRecipeCategory').value;
  const description = document.getElementById('formRecipeDesc').value.trim();
  const prepTimeMinutes = parseInt(document.getElementById('formPrepTime').value, 10) || 10;
  const cookTimeMinutes = parseInt(document.getElementById('formCookTime').value, 10) || 20;
  const servings = parseInt(document.getElementById('formServings').value, 10) || 2;
  const difficulty = document.getElementById('formDifficulty').value;
  const isVegetarian = document.getElementById('formIsVegetarian').checked;
  const imageUrl = document.getElementById('formPhotoUrl').value.trim();
  const videoUrl = document.getElementById('formVideoUrl').value.trim();

  // Gather Ingredients
  const ingredients = [];
  document.querySelectorAll('#ingredientsContainer .ingredient-row').forEach((row, idx) => {
    const name = row.querySelector('.ing-name').value.trim();
    const quantity = row.querySelector('.ing-qty').value.trim();
    const unit = row.querySelector('.ing-unit').value.trim();
    if (name) {
      ingredients.push({ name, quantity, unit, orderIndex: idx + 1 });
    }
  });

  // Gather Steps
  const steps = [];
  document.querySelectorAll('#stepsContainer .step-row').forEach((row, idx) => {
    const sTitle = row.querySelector('.step-title-input').value.trim();
    const sTimer = parseInt(row.querySelector('.step-timer-input').value, 10) || null;
    const sInst = row.querySelector('.step-instruction-input').value.trim();
    if (sInst) {
      steps.push({
        stepNumber: idx + 1,
        title: sTitle || `Step ${idx + 1}`,
        instruction: sInst,
        timerMinutes: sTimer
      });
    }
  });

  const payload = {
    title,
    categoryId: parseInt(categoryId, 10),
    description,
    prepTimeMinutes,
    cookTimeMinutes,
    servings,
    difficulty,
    isVegetarian,
    imageUrl,
    videoUrl,
    ingredients,
    steps,
    submitForReview
  };

  try {
    let res;
    if (editId) {
      // Update Recipe
      res = await apiFetch(`/contributor/recipes/${editId}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      });
      showToast('Recipe updated successfully', 'success');
    } else {
      // Create Recipe
      res = await apiFetch('/contributor/recipes', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      showToast('Recipe submitted successfully', 'success');
    }

    resetRecipeForm();
    loadStats();
    loadContributorRecipes();

    // Switch back to Recipe Management tab
    const tabBtn = document.getElementById('tab-recipes-btn');
    const tab = new bootstrap.Tab(tabBtn);
    tab.show();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Edit Recipe Prep
async function editRecipe(recipeId) {
  try {
    const res = await apiFetch(`/recipes/${recipeId}`);
    if (res.success && res.data) {
      const r = res.data;
      document.getElementById('editingRecipeId').value = r.id;
      document.getElementById('recipeFormHeader').innerHTML = `<i class="bi bi-pencil-square text-warning me-2"></i> Update Recipe: ${r.title}`;
      document.getElementById('formRecipeTitle').value = r.title;
      document.getElementById('formRecipeCategory').value = r.categoryId;
      document.getElementById('formRecipeDesc').value = r.description || '';
      document.getElementById('formPrepTime').value = r.prepTimeMinutes || 10;
      document.getElementById('formCookTime').value = r.cookTimeMinutes || 20;
      document.getElementById('formServings').value = r.servings || 2;
      document.getElementById('formDifficulty').value = r.difficulty || 'EASY';
      document.getElementById('formIsVegetarian').checked = r.vegetarian;
      document.getElementById('formPhotoUrl').value = r.imageUrl || '';
      document.getElementById('formVideoUrl').value = (r.reel && r.reel.videoUrl) ? r.reel.videoUrl : '';

      // Populate ingredients
      const ingCont = document.getElementById('ingredientsContainer');
      ingCont.innerHTML = '';
      if (r.ingredients && r.ingredients.length > 0) {
        r.ingredients.forEach(i => addIngredientRow(i.name, i.quantity, i.unit));
      } else {
        addIngredientRow();
      }

      // Populate steps
      const stepCont = document.getElementById('stepsContainer');
      stepCont.innerHTML = '';
      if (r.steps && r.steps.length > 0) {
        r.steps.forEach(s => addStepRow(s.title, s.instruction, s.timerMinutes || ''));
      } else {
        addStepRow();
      }

      switchToAddRecipeTab();
    }
  } catch (err) {
    showToast('Failed to load recipe for editing', 'error');
  }
}

// Delete Recipe
async function deleteContributorRecipe(id) {
  if (!confirm('Are you sure you want to delete this recipe?')) return;
  try {
    await apiFetch(`/contributor/recipes/${id}`, { method: 'DELETE' });
    showToast('Recipe deleted successfully', 'success');
    loadStats();
    loadContributorRecipes();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function resetRecipeForm() {
  document.getElementById('recipeSubmissionForm').reset();
  document.getElementById('editingRecipeId').value = '';
  document.getElementById('recipeFormHeader').innerHTML = '<i class="bi bi-cloud-arrow-up-fill text-success me-2"></i> Submit New Recipe &amp; Reel';
  document.getElementById('ingredientsContainer').innerHTML = '';
  document.getElementById('stepsContainer').innerHTML = '';
  initDynamicFormDefaults();
}

function switchToAddRecipeTab() {
  const tabBtn = document.getElementById('tab-add-btn');
  const tab = new bootstrap.Tab(tabBtn);
  tab.show();
}

// 7. Load Interaction History (Messages & Comments)
async function loadInteractions() {
  try {
    const res = await apiFetch('/contributor/interactions');
    if (res.success && res.data) {
      const { inbox, sent, comments } = res.data;

      // Inbox
      const inboxList = document.getElementById('contributorInboxList');
      if (inbox && inbox.length > 0) {
        inboxList.innerHTML = inbox.map(m => `
          <div class="p-3 rounded-3" style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--border-color);">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <span class="fw-bold small text-white">${m.sender ? m.sender.name : 'Home Cook'}</span>
              <span class="small text-muted" style="font-size: 0.7rem;">${new Date(m.createdAt).toLocaleDateString()}</span>
            </div>
            <div class="small fw-semibold text-warning">${m.subject || 'Recipe Inquiry'}</div>
            <div class="small text-secondary mt-1">${m.content}</div>
          </div>
        `).join('');
      } else {
        inboxList.innerHTML = '<div class="text-muted small p-3">No messages received yet.</div>';
      }

      // Sent
      const sentList = document.getElementById('contributorSentList');
      if (sent && sent.length > 0) {
        sentList.innerHTML = sent.map(m => `
          <div class="p-3 rounded-3" style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--border-color);">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <span class="fw-bold small text-white">To: ${m.recipient ? m.recipient.name : 'User'}</span>
              <span class="small text-muted" style="font-size: 0.7rem;">${new Date(m.createdAt).toLocaleDateString()}</span>
            </div>
            <div class="small fw-semibold text-warning">${m.subject || 'Message'}</div>
            <div class="small text-secondary mt-1">${m.content}</div>
          </div>
        `).join('');
      } else {
        sentList.innerHTML = '<div class="text-muted small p-3">No sent messages.</div>';
      }

      // Comments Received
      const commentsList = document.getElementById('contributorCommentsList');
      if (comments && comments.length > 0) {
        commentsList.innerHTML = comments.map(c => `
          <div class="p-3 rounded-3" style="background: rgba(0, 0, 0, 0.2); border: 1px solid var(--border-color);">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <span class="fw-bold small text-white">${c.userName}</span>
              <span class="small text-muted">${new Date(c.createdAt).toLocaleDateString()}</span>
            </div>
            <p class="small text-secondary mb-0">${c.content}</p>
          </div>
        `).join('');
      } else {
        commentsList.innerHTML = '<div class="text-muted small p-3">No comments on your recipes yet.</div>';
      }
    }
  } catch (err) {
    console.error('Failed to load interactions', err);
  }
}

// 8. Chef Profile Management
async function loadChefProfile() {
  try {
    const res = await apiFetch('/auth/me');
    if (res.success && res.data) {
      const u = res.data;
      document.getElementById('chefDashName').innerText = u.name;
      document.getElementById('chefDashBio').innerText = u.bio || 'Craft recipes, upload reels, and interact with home cooks.';
      if (u.avatarUrl) {
        document.getElementById('chefDashAvatar').src = u.avatarUrl;
        document.getElementById('chefAvatar').value = u.avatarUrl;
      }
      document.getElementById('chefName').value = u.name;
      document.getElementById('chefEmail').value = u.email;
      document.getElementById('chefBio').value = u.bio || '';
    }
  } catch (err) {
    console.error('Failed to load chef profile', err);
  }
}

async function handleChefProfileUpdate(event) {
  event.preventDefault();
  const name = document.getElementById('chefName').value.trim();
  const bio = document.getElementById('chefBio').value.trim();
  const avatarUrl = document.getElementById('chefAvatar').value.trim();

  try {
    const res = await apiFetch('/users/profile', {
      method: 'PUT',
      body: JSON.stringify({ name, bio, avatarUrl })
    });
    if (res.success && res.data) {
      setStoredUser(res.data);
      document.getElementById('chefDashName').innerText = res.data.name;
      document.getElementById('chefDashBio').innerText = res.data.bio || '';
      if (res.data.avatarUrl) {
        document.getElementById('chefDashAvatar').src = res.data.avatarUrl;
      }
      showToast('Profile updated successfully', 'success');
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}
