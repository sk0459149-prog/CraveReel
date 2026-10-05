// Recipe Explorer Logic

let debounceTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
  await loadFilterCategories();
  syncFromUrlParams();
  triggerFilter();
});

// Load Categories into Sidebar Dropdown
async function loadFilterCategories() {
  const select = document.getElementById('filterCategory');
  if (!select) return;

  try {
    const res = await apiFetch('/categories');
    if (res.success && res.data) {
      res.data.forEach(c => {
        const opt = document.createElement('option');
        opt.value = c.id;
        opt.textContent = `${c.icon || '🍽️'} ${c.name}`;
        select.appendChild(opt);
      });
    }
  } catch (err) {
    console.error('Failed to load categories in filter', err);
  }
}

// Sync Form from Query Params (e.g. ?query=paneer or ?categoryId=1)
function syncFromUrlParams() {
  const params = new URLSearchParams(window.location.search);
  const q = params.get('query');
  const cat = params.get('categoryId');
  const sort = params.get('sort');
  const maxTime = params.get('maxCookTime');

  if (q) document.getElementById('exploreSearchInput').value = q;
  if (cat) document.getElementById('filterCategory').value = cat;
  if (sort) document.getElementById('filterSort').value = sort;
  if (maxTime) {
    document.getElementById('filterCookTime').value = maxTime;
    updateCookTimeLabel(maxTime);
  }
}

// Debounce Search Input
function handleSearchInput(e) {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    triggerFilter();
  }, 350);
}

// Update Max Cook Time display
function updateCookTimeLabel(val) {
  const display = document.getElementById('cookTimeDisplay');
  if (val >= 90) {
    display.innerText = 'Any';
  } else {
    display.innerText = `≤ ${val} mins`;
  }
}

// Trigger Filter Execution
async function triggerFilter() {
  const query = document.getElementById('exploreSearchInput').value.trim();
  const categoryId = document.getElementById('filterCategory').value;
  const difficulty = document.getElementById('filterDifficulty').value;
  const cookTimeVal = parseInt(document.getElementById('filterCookTime').value, 10);
  const sortBy = document.getElementById('filterSort').value;

  const dietVal = document.querySelector('input[name="dietRadio"]:checked')?.value;

  const params = new URLSearchParams();
  if (query) params.append('query', query);
  if (categoryId) params.append('categoryId', categoryId);
  if (dietVal !== undefined && dietVal !== '') params.append('isVegetarian', dietVal);
  if (difficulty && difficulty !== 'ALL') params.append('difficulty', difficulty);
  if (cookTimeVal < 90) params.append('maxCookTime', cookTimeVal);
  if (sortBy) params.append('sortBy', sortBy);

  const container = document.getElementById('exploreRecipesGrid');
  const countSpan = document.getElementById('resultsCount');

  container.innerHTML = `
    <div class="col-12 text-center py-5 text-muted">
      <div class="spinner-border text-warning spinner-border-sm" role="status"></div>
      <p class="mt-2 small">Finding matching recipes...</p>
    </div>
  `;

  try {
    const res = await apiFetch(`/recipes?${params.toString()}`);
    if (res.success && res.data) {
      countSpan.innerText = res.data.length;
      if (res.data.length === 0) {
        container.innerHTML = `
          <div class="col-12 text-center py-5">
            <i class="bi bi-search text-muted display-4 mb-3"></i>
            <h5 class="text-white">No Recipes Found</h5>
            <p class="text-secondary small">Try adjusting your filters, searching for an ingredient, or resetting filters.</p>
            <button class="btn btn-sm btn-rr-secondary" onclick="resetFilters()">Reset Filters</button>
          </div>
        `;
      } else {
        container.innerHTML = res.data.map(recipe => createRecipeCardHTML(recipe)).join('');
      }
    }
  } catch (err) {
    container.innerHTML = `<div class="col-12 text-danger text-center py-4">Failed to load recipes: ${err.message}</div>`;
  }
}

// Reset Filters
function resetFilters() {
  document.getElementById('exploreSearchInput').value = '';
  document.getElementById('filterCategory').value = '';
  document.getElementById('dietAll').checked = true;
  document.getElementById('filterDifficulty').value = 'ALL';
  document.getElementById('filterCookTime').value = 90;
  updateCookTimeLabel(90);
  document.getElementById('filterSort').value = 'newest';
  triggerFilter();
}
