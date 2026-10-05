// Cooking Mode Assistant Controller

let recipeId = null;
let recipeData = null;
let currentStepIndex = 0;

// Kitchen Timer State
let timerInterval = null;
let timerSecondsRemaining = 0;
let timerInitialSeconds = 0;
let isTimerRunning = false;

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  recipeId = params.get('id');

  if (!recipeId) {
    window.location.href = 'explore.html';
    return;
  }

  loadCookingMode(recipeId);
  setupCookKeyboard();
});

// Load Recipe for Cooking Assistant
async function loadCookingMode(id) {
  try {
    const res = await apiFetch(`/recipes/${id}`);
    if (res.success && res.data) {
      recipeData = res.data;
      initCookingSession(recipeData);
    }
  } catch (err) {
    document.getElementById('cookLoading').innerHTML = `
      <div class="text-danger py-4">
        <i class="bi bi-exclamation-octagon display-4"></i>
        <h4 class="mt-2 text-white">Could not load recipe</h4>
        <p class="text-secondary">${err.message}</p>
        <a href="explore.html" class="btn btn-rr-primary">Return</a>
      </div>
    `;
  }
}

// Initialize Cooking Session
function initCookingSession(recipe) {
  document.getElementById('cookLoading').classList.add('d-none');
  document.getElementById('cookStepContainer').classList.remove('d-none');

  document.getElementById('cookRecipeTitle').innerText = recipe.title;
  document.getElementById('cookRecipeChef').innerText = `by ${recipe.contributorName}`;
  document.getElementById('exitCookingBtn').href = `recipe.html?id=${recipe.id}`;
  document.getElementById('rateFromCookBtn').href = `recipe.html?id=${recipe.id}#ratingForm`;

  // Populate Ingredients Offcanvas
  const ingList = document.getElementById('cookDrawerIngredientsList');
  if (recipe.ingredients && recipe.ingredients.length > 0) {
    ingList.innerHTML = recipe.ingredients.map((ing, idx) => `
      <li class="list-group-item bg-transparent text-secondary border-secondary border-opacity-10 py-2 px-0 d-flex justify-content-between align-items-center" onclick="toggleCookIngCheck(this)" style="cursor: pointer;">
        <div class="d-flex align-items-center gap-2">
          <input class="form-check-input me-1" type="checkbox" id="cook-ing-${idx}">
          <label class="form-check-label mb-0" for="cook-ing-${idx}">${ing.name}</label>
        </div>
        <span class="badge bg-secondary bg-opacity-25 text-white">${ing.quantity || ''} ${ing.unit || ''}</span>
      </li>
    `).join('');
  }

  // Display first step
  renderStep(0);
}

// Render Specific Step
function renderStep(index) {
  if (!recipeData.steps || recipeData.steps.length === 0) {
    document.getElementById('stepTitle').innerText = 'Cook and Enjoy';
    document.getElementById('stepInstruction').innerText = recipeData.description || 'Prepare according to taste.';
    return;
  }

  currentStepIndex = index;
  const total = recipeData.steps.length;
  const step = recipeData.steps[index];

  // Update Progress
  const pct = Math.round(((index + 1) / total) * 100);
  document.getElementById('cookProgressBar').style.width = `${pct}%`;

  // Step Badge & Text
  document.getElementById('stepBadge').innerText = `STEP ${index + 1} OF ${total}`;
  document.getElementById('stepTitle').innerText = step.title || `Step ${index + 1}`;
  document.getElementById('stepInstruction').innerText = step.instruction;

  // Tip
  const tipBox = document.getElementById('stepTipBox');
  if (step.tip) {
    document.getElementById('stepTipText').innerText = step.tip;
    tipBox.classList.remove('d-none');
  } else {
    tipBox.classList.add('d-none');
  }

  // Timer Setup: If step has timerMinutes, configure automatically
  clearInterval(timerInterval);
  isTimerRunning = false;
  const timerBtn = document.getElementById('timerStartBtn');
  timerBtn.innerHTML = '<i class="bi bi-play-fill"></i> Start';
  timerBtn.className = 'btn btn-sm btn-success px-3';

  if (step.timerMinutes && step.timerMinutes > 0) {
    timerInitialSeconds = step.timerMinutes * 60;
    timerSecondsRemaining = timerInitialSeconds;
  } else {
    timerInitialSeconds = 180; // 3 min default timer
    timerSecondsRemaining = 180;
  }
  updateTimerDisplay();

  // Navigation Buttons State
  document.getElementById('prevStepBtn').disabled = (index === 0);

  const nextBtn = document.getElementById('nextStepBtn');
  if (index === total - 1) {
    nextBtn.innerHTML = 'Finish Cooking 🎉';
    nextBtn.className = 'btn btn-success px-4 fw-bold';
  } else {
    nextBtn.innerHTML = 'Next Step <i class="bi bi-arrow-right"></i>';
    nextBtn.className = 'btn btn-cook-this px-4';
  }
}

// Next / Previous Step
function nextStep() {
  if (currentStepIndex < recipeData.steps.length - 1) {
    renderStep(currentStepIndex + 1);
  } else {
    // Show celebration screen
    document.getElementById('cookStepContainer').classList.add('d-none');
    document.getElementById('cookCelebration').classList.remove('d-none');
    document.getElementById('cookProgressBar').style.width = '100%';
    playCelebrationChime();
  }
}

function prevStep() {
  if (currentStepIndex > 0) {
    document.getElementById('cookCelebration').classList.add('d-none');
    document.getElementById('cookStepContainer').classList.remove('d-none');
    renderStep(currentStepIndex - 1);
  }
}

// Kitchen Timer Functions
function toggleTimer() {
  const btn = document.getElementById('timerStartBtn');
  if (isTimerRunning) {
    // Pause
    clearInterval(timerInterval);
    isTimerRunning = false;
    btn.innerHTML = '<i class="bi bi-play-fill"></i> Resume';
    btn.className = 'btn btn-sm btn-success px-3';
  } else {
    // Start / Resume
    isTimerRunning = true;
    btn.innerHTML = '<i class="bi bi-pause-fill"></i> Pause';
    btn.className = 'btn btn-sm btn-warning px-3';

    timerInterval = setInterval(() => {
      if (timerSecondsRemaining > 0) {
        timerSecondsRemaining--;
        updateTimerDisplay();
      } else {
        clearInterval(timerInterval);
        isTimerRunning = false;
        btn.innerHTML = '<i class="bi bi-arrow-counterclockwise"></i> Done';
        btn.className = 'btn btn-sm btn-secondary px-3';
        playTimerAlarmChime();
        showToast('⏰ Kitchen Timer Finished!', 'success');
      }
    }, 1000);
  }
}

function resetTimer() {
  clearInterval(timerInterval);
  isTimerRunning = false;
  timerSecondsRemaining = timerInitialSeconds;
  updateTimerDisplay();
  const btn = document.getElementById('timerStartBtn');
  btn.innerHTML = '<i class="bi bi-play-fill"></i> Start';
  btn.className = 'btn btn-sm btn-success px-3';
}

function addTimerMinute(mins) {
  timerSecondsRemaining += mins * 60;
  updateTimerDisplay();
}

function updateTimerDisplay() {
  const m = Math.floor(timerSecondsRemaining / 60);
  const s = timerSecondsRemaining % 60;
  const pad = (n) => (n < 10 ? '0' + n : n);
  document.getElementById('timerDisplay').innerText = `${pad(m)}:${pad(s)}`;
}

// Web Audio API Synthesizer Chimes
function playTimerAlarmChime() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    [523.25, 659.25, 783.99, 1046.5].forEach((freq, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(freq, ctx.currentTime + idx * 0.15);
      gain.gain.setValueAtTime(0.3, ctx.currentTime + idx * 0.15);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + idx * 0.15 + 0.4);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(ctx.currentTime + idx * 0.15);
      osc.stop(ctx.currentTime + idx * 0.15 + 0.4);
    });
  } catch (e) {
    console.log('Audio chime not available in this environment');
  }
}

function playCelebrationChime() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    [440, 554.37, 659.25, 880].forEach((freq, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, ctx.currentTime + idx * 0.12);
      gain.gain.setValueAtTime(0.35, ctx.currentTime + idx * 0.12);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + idx * 0.12 + 0.5);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(ctx.currentTime + idx * 0.12);
      osc.stop(ctx.currentTime + idx * 0.12 + 0.5);
    });
  } catch (e) {}
}

// Keyboard navigation
function setupCookKeyboard() {
  window.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowRight') {
      nextStep();
    } else if (e.key === 'ArrowLeft') {
      prevStep();
    }
  });
}

function toggleCookIngCheck(li) {
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
