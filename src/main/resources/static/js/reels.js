// Dedicated Short-Video Reels Page Controller

let reelsData = [];
let activeIndex = 0;
let isGlobalMuted = true;
let currentRecipeDetail = null;

document.addEventListener('DOMContentLoaded', () => {
  loadReels();
  setupKeyboardNavigation();
});

// Load Reels from Backend API
async function loadReels() {
  const feed = document.getElementById('reelsFeed');
  if (!feed) return;

  try {
    const res = await apiFetch('/reels');
    if (res.success && res.data && res.data.length > 0) {
      reelsData = res.data;
      renderReelsFeed(reelsData);
      setupIntersectionObserver();

      // Check URL param for specific reel
      const urlParams = new URLSearchParams(window.location.search);
      const targetId = urlParams.get('id');
      if (targetId) {
        const idx = reelsData.findIndex(r => r.id.toString() === targetId || r.recipeId.toString() === targetId);
        if (idx !== -1) {
          scrollToReel(idx);
        } else {
          activateReel(0);
        }
      } else {
        activateReel(0);
      }
    } else {
      feed.innerHTML = `
        <div class="d-flex flex-column justify-content-center align-items-center h-100 text-center p-4">
          <i class="bi bi-camera-reels text-muted display-3 mb-3"></i>
          <h4 class="text-white">No Reels Available Yet</h4>
          <p class="text-secondary small">Be the first chef to share a short cooking reel!</p>
          <a href="contributor-dashboard.html" class="btn btn-rr-primary mt-2">Upload Reel</a>
        </div>
      `;
    }
  } catch (err) {
    feed.innerHTML = `
      <div class="text-center p-5 text-danger">
        <i class="bi bi-exclamation-triangle display-4"></i>
        <p class="mt-2">Failed to load cooking reels. Please try again.</p>
      </div>
    `;
  }
}

// Render Reels in Feed
function renderReelsFeed(reels) {
  const feed = document.getElementById('reelsFeed');
  feed.innerHTML = reels.map((reel, index) => {
    const likedClass = reel.likedByCurrentUser ? 'liked' : '';
    const savedClass = reel.savedByCurrentUser ? 'saved' : '';

    return `
      <div class="reel-item" data-index="${index}" id="reel-item-${index}">
        ${isYouTubeReel(reel.videoUrl)
          ? `<iframe class="reel-youtube-frame" src="https://www.youtube.com/embed/${getYouTubeReelId(reel.videoUrl)}?rel=0" title="${reel.recipeTitle} recipe video" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>`
          : `<video
              id="reel-video-${index}"
              src="${reel.videoUrl}"
              loop muted playsinline preload="metadata"
              onclick="toggleVideoPlay(${index})"
              ontimeupdate="updateReelProgress(${index}, this)"
            ></video>`}

        <!-- Progress Bar -->
        <div class="reel-progress-bar-container" onclick="seekReel(event, ${index})">
          <div class="reel-progress-bar" id="reel-progress-${index}"></div>
        </div>

        <!-- Floating Overlay -->
        <div class="reel-overlay">
          <!-- Top Row -->
          <div class="reel-overlay-top">
            <div class="reel-play-chip">
              <i class="bi bi-camera-reels-fill text-warning"></i>
              <span>${reel.categoryName || 'Cooking Reel'}</span>
            </div>
            <button class="reel-audio-toggle" onclick="toggleAudio()" title="Mute / Unmute">
              <i class="bi ${isGlobalMuted ? 'bi-volume-mute-fill' : 'bi-volume-up-fill'}" id="audioIcon"></i>
            </button>
          </div>

          <!-- Bottom Row & Details -->
          <div class="reel-overlay-bottom">
            <div class="reel-creator-row">
              <img src="${reel.contributorAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150'}" class="reel-creator-avatar">
              <div>
                <div class="reel-creator-name">${reel.contributorName}</div>
                <span class="badge ${reel.vegetarian ? 'bg-success' : 'bg-danger'} bg-opacity-75" style="font-size: 0.65rem;">
                  ${reel.vegetarian ? '🌱 Veg' : '🍗 Non-Veg'}
                </span>
              </div>
            </div>

            <div class="reel-recipe-title">${reel.recipeTitle}</div>
            <div class="reel-recipe-desc">${reel.recipeDescription || ''}</div>

            <!-- "Cook This" & "View Recipe" CTA -->
            <div class="reel-cta-bar">
              <a href="recipe.html?id=${reel.recipeId}" class="btn btn-sm btn-cook-this flex-grow-1 justify-content-center">
                🍳 Cook This
              </a>
              <a href="cook.html?id=${reel.recipeId}" class="btn btn-sm btn-rr-primary" title="Launch Cooking Assistant">
                <i class="bi bi-stopwatch"></i> Cook Mode
              </a>
            </div>
          </div>
        </div>

        <!-- Right Action Rail -->
        <div class="reel-action-rail">
          <!-- Like Button -->
          <div class="d-flex flex-column align-items-center">
            <button class="reel-action-btn ${likedClass}" id="like-btn-${reel.recipeId}" onclick="handleReelLike(${reel.recipeId}, this)">
              <i class="bi ${reel.likedByCurrentUser ? 'bi-heart-fill' : 'bi-heart'}"></i>
            </button>
            <span class="reel-action-count" id="like-count-${reel.recipeId}">${formatNumber(reel.likeCount)}</span>
          </div>

          <!-- Comment Button -->
          <div class="d-flex flex-column align-items-center">
            <button class="reel-action-btn" onclick="openCommentDrawer(${reel.recipeId})">
              <i class="bi bi-chat-dots-fill"></i>
            </button>
            <span class="reel-action-count" id="comment-count-${reel.recipeId}">${formatNumber(reel.commentCount)}</span>
          </div>

          <!-- Save Button -->
          <div class="d-flex flex-column align-items-center">
            <button class="reel-action-btn ${savedClass}" id="save-btn-${reel.recipeId}" onclick="handleReelSave(${reel.recipeId}, this)">
              <i class="bi ${reel.savedByCurrentUser ? 'bi-bookmark-fill' : 'bi-bookmark'}"></i>
            </button>
            <span class="reel-action-count">Save</span>
          </div>

          <!-- Share Button -->
          <div class="d-flex flex-column align-items-center">
            <button class="reel-action-btn" onclick="handleReelShare(${reel.id})">
              <i class="bi bi-share-fill"></i>
            </button>
            <span class="reel-action-count">Share</span>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

// Intersection Observer for autoplay when visible
function setupIntersectionObserver() {
  const options = {
    root: document.getElementById('reelsFeed'),
    threshold: 0.65
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      const idx = parseInt(entry.target.getAttribute('data-index'), 10);
      const video = document.getElementById(`reel-video-${idx}`);

      if (entry.isIntersecting) {
        activeIndex = idx;
        activateReel(idx);
        if (video) {
          video.muted = isGlobalMuted;
          video.play().catch(() => {});
        }
      } else {
        if (video) {
          video.pause();
        }
      }
    });
  }, options);

  document.querySelectorAll('.reel-item').forEach(item => observer.observe(item));
}

// Activate Reel: update side panel and record view
async function activateReel(index) {
  if (!reelsData[index]) return;
  const reel = reelsData[index];

  // Record reel view
  apiFetch(`/reels/${reel.id}/view`, { method: 'POST' }).catch(() => {});

  // Update Desktop Side Panel
  updateSidePanel(reel);
}

// Update Desktop Companion Panel with Full Details
async function updateSidePanel(reel) {
  const title = document.getElementById('sidePanelRecipeTitle');
  const cat = document.getElementById('sidePanelCategory');
  const prep = document.getElementById('sidePanelPrep');
  const cook = document.getElementById('sidePanelCook');
  const diff = document.getElementById('sidePanelDifficulty');
  const cookBtn = document.getElementById('sidePanelCookThisBtn');
  const cookModeBtn = document.getElementById('sidePanelCookingModeBtn');
  const ingList = document.getElementById('sidePanelIngredientsList');
  const ingCount = document.getElementById('sidePanelIngCount');

  if (!title) return;

  title.innerText = reel.recipeTitle;
  cat.innerText = reel.categoryName || 'General';
  cookBtn.href = `recipe.html?id=${reel.recipeId}`;
  cookModeBtn.href = `cook.html?id=${reel.recipeId}`;

  // Fetch full recipe for ingredients
  try {
    const res = await apiFetch(`/recipes/${reel.recipeId}`);
    if (res.success && res.data) {
      currentRecipeDetail = res.data;
      prep.innerText = `${res.data.prepTimeMinutes || 0} mins`;
      cook.innerText = `${res.data.cookTimeMinutes || 0} mins`;
      diff.innerText = res.data.difficulty || 'Easy';

      if (res.data.ingredients && res.data.ingredients.length > 0) {
        ingCount.innerText = `${res.data.ingredients.length} items`;
        ingList.innerHTML = res.data.ingredients.map(ing => `
          <li class="list-group-item bg-transparent text-secondary border-secondary border-opacity-10 px-0 d-flex justify-content-between align-items-center">
            <span><i class="bi bi-check2 text-success me-1"></i> ${ing.name}</span>
            <span class="badge bg-secondary bg-opacity-25 text-white">${ing.quantity || ''} ${ing.unit || ''}</span>
          </li>
        `).join('');
      } else {
        ingList.innerHTML = '<li class="list-group-item bg-transparent text-muted px-0">Ingredients ready in full recipe.</li>';
      }
    }
  } catch (err) {
    console.error('Failed to load side panel recipe detail', err);
  }
}

// Video Play/Pause Toggle
function toggleVideoPlay(index) {
  const video = document.getElementById(`reel-video-${index}`);
  if (!video) return;

  if (video.paused) {
    video.play();
  } else {
    video.pause();
  }
}

// Audio Toggle (Mute / Unmute)
function toggleAudio() {
  isGlobalMuted = !isGlobalMuted;
  const icon = document.getElementById('audioIcon');

  document.querySelectorAll('video').forEach(v => {
    v.muted = isGlobalMuted;
  });

  if (icon) {
    icon.className = `bi ${isGlobalMuted ? 'bi-volume-mute-fill' : 'bi-volume-up-fill'}`;
  }

  showToast(isGlobalMuted ? 'Audio muted' : 'Audio unmuted', 'success');
}

// Progress Bar Update & Scrubbing
function updateReelProgress(index, video) {
  const bar = document.getElementById(`reel-progress-${index}`);
  if (bar && video.duration) {
    const pct = (video.currentTime / video.duration) * 100;
    bar.style.width = `${pct}%`;
  }
}

function seekReel(event, index) {
  const video = document.getElementById(`reel-video-${index}`);
  if (!video || !video.duration) return;

  const rect = event.currentTarget.getBoundingClientRect();
  const clickX = event.clientX - rect.left;
  const pct = clickX / rect.width;
  video.currentTime = pct * video.duration;
}

// Keyboard Navigation (Arrow Up / Down)
function setupKeyboardNavigation() {
  window.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (activeIndex < reelsData.length - 1) {
        scrollToReel(activeIndex + 1);
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (activeIndex > 0) {
        scrollToReel(activeIndex - 1);
      }
    } else if (e.key === ' ' || e.key === 'k') {
      e.preventDefault();
      toggleVideoPlay(activeIndex);
    } else if (e.key === 'm') {
      toggleAudio();
    }
  });
}

function scrollToReel(index) {
  const target = document.getElementById(`reel-item-${index}`);
  if (target) {
    target.scrollIntoView({ behavior: 'smooth' });
  }
}

// Like Handler
async function handleReelLike(recipeId, btn) {
  if (!getAuthToken()) {
    showToast('Please log in to like this recipe', 'error');
    return;
  }

  try {
    const res = await apiFetch(`/recipes/${recipeId}/like`, { method: 'POST' });
    const isLiked = res.data && res.data.liked;
    const countSpan = document.getElementById(`like-count-${recipeId}`);

    if (isLiked) {
      btn.classList.add('liked');
      btn.innerHTML = '<i class="bi bi-heart-fill"></i>';
      if (countSpan) countSpan.innerText = parseInt(countSpan.innerText || '0', 10) + 1;
      showToast('Recipe liked', 'success');
    } else {
      btn.classList.remove('liked');
      btn.innerHTML = '<i class="bi bi-heart"></i>';
      if (countSpan) countSpan.innerText = Math.max(0, parseInt(countSpan.innerText || '1', 10) - 1);
      showToast('Recipe unliked', 'success');
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Save Handler
async function handleReelSave(recipeId, btn) {
  if (!getAuthToken()) {
    showToast('Please log in to save this recipe', 'error');
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
    showToast(err.message, 'error');
  }
}

// Share Handler
function handleReelShare(reelId) {
  const url = `${window.location.origin}/reels.html?id=${reelId}`;
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      showToast('Reel link copied to clipboard!', 'success');
    });
  } else {
    showToast('Reel URL: ' + url, 'success');
  }
}

// Comments Drawer Handler
let currentDrawerRecipeId = null;

async function openCommentDrawer(recipeId) {
  currentDrawerRecipeId = recipeId;
  const drawer = document.getElementById('reelCommentDrawer');
  const body = document.getElementById('drawerCommentsBody');
  const countSpan = document.getElementById('drawerCommentCount');

  drawer.classList.add('open');
  body.innerHTML = '<div class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-warning"></div> Loading comments...</div>';

  try {
    const res = await apiFetch(`/recipes/${recipeId}/comments`);
    if (res.success && res.data) {
      countSpan.innerText = res.data.length;
      if (res.data.length === 0) {
        body.innerHTML = '<div class="text-center text-muted py-5">No comments yet. Be the first to ask!</div>';
      } else {
        body.innerHTML = res.data.map(c => `
          <div class="d-flex gap-2">
            <img src="${c.userAvatar || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150'}" style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover;">
            <div class="flex-grow-1">
              <div class="d-flex align-items-center gap-2">
                <span class="fw-bold small text-white">${c.userName}</span>
                <span class="small text-muted" style="font-size: 0.7rem;">${c.userRole === 'ROLE_CONTRIBUTOR' ? 'Chef' : ''}</span>
              </div>
              <div class="small text-secondary mt-1">${c.content}</div>
            </div>
          </div>
        `).join('');
      }
    }
  } catch (err) {
    body.innerHTML = '<div class="text-danger small">Failed to load comments.</div>';
  }
}

function closeCommentDrawer() {
  document.getElementById('reelCommentDrawer').classList.remove('open');
}

async function handleDrawerCommentSubmit(event) {
  event.preventDefault();
  if (!getAuthToken()) {
    showToast('Please log in to comment', 'error');
    return;
  }

  const input = document.getElementById('drawerCommentInput');
  const content = input.value.trim();
  if (!content || !currentDrawerRecipeId) return;

  try {
    await apiFetch(`/recipes/${currentDrawerRecipeId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content })
    });
    input.value = '';
    showToast('Comment added successfully', 'success');
    openCommentDrawer(currentDrawerRecipeId);
  } catch (err) {
    showToast(err.message, 'error');
  }
}


function isYouTubeReel(url) {
  return typeof url === 'string' && /(?:youtube\.com\/(?:watch\?v=|embed\/)|youtu\.be\/)/i.test(url);
}

function getYouTubeReelId(url) {
  const m = String(url).match(/(?:youtube\.com\/(?:watch\?v=|embed\/)|youtu\.be\/)([A-Za-z0-9_-]{11})/i);
  return m ? m[1] : '';
}
