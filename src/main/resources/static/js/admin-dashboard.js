/* =============================================
   admin-dashboard.js – CraveReel Admin Panel
   ============================================= */

'use strict';

// ── State ──────────────────────────────────────
let allUsers = [];
let allRecipes = [];
let allComments = [];
let pendingFilter = 'PENDING';
let editingUserId = null;

// ── Init ───────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  const token = localStorage.getItem('rr_token');
  if (!token) { window.location.href = 'login.html'; return; }

  const user = JSON.parse(localStorage.getItem('rr_user') || '{}');
  if (user.role !== 'ROLE_ADMIN') {
    showToast('Access denied. Admin only.', 'error');
    setTimeout(() => window.location.href = 'index.html', 1500);
    return;
  }

  document.getElementById('adminName').textContent = user.name || 'Admin';

  // Tab routing
  const hash = location.hash || '#users';
  switchTab(hash.replace('#', ''));

  document.querySelectorAll('.admin-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  // Search users
  document.getElementById('userSearchInput')?.addEventListener('input', debounce(filterUsers, 300));
  // Recipe status filter
  document.getElementById('recipeStatusFilter')?.addEventListener('change', e => {
    pendingFilter = e.target.value;
    filterRecipesTable();
  });
  // Search comments
  document.getElementById('commentSearchInput')?.addEventListener('input', debounce(filterComments, 300));

  // Modal form
  document.getElementById('userModalForm')?.addEventListener('submit', handleSaveUser);
});

function switchTab(tab) {
  document.querySelectorAll('.admin-tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
  document.querySelectorAll('.admin-tab-panel').forEach(p => p.classList.toggle('active', p.id === `tab-${tab}`));
  location.hash = tab;
  if (tab === 'users') loadUsers();
  else if (tab === 'recipes') loadRecipes();
  else if (tab === 'moderation') loadComments();
  else if (tab === 'settings') loadSettings();
  else if (tab === 'dashboard') loadDashboard();
}

// ── DASHBOARD STATS ─────────────────────────────
async function loadDashboard() {
  try {
    const stats = await apiFetch('/api/admin/dashboard-stats');
    document.getElementById('statTotalUsers').textContent   = stats.totalUsers   ?? '-';
    document.getElementById('statTotalRecipes').textContent = stats.totalRecipes ?? '-';
    document.getElementById('statPending').textContent      = stats.pendingRecipes ?? '-';
    document.getElementById('statTotalReels').textContent   = stats.totalReels  ?? '-';
    document.getElementById('statTotalComments').textContent= stats.totalComments ?? '-';
    document.getElementById('statTotalRatings').textContent = stats.totalRatings ?? '-';
  } catch { /* silently skip */ }
}

// ══════════════════════════════════════════════
//  USER MANAGEMENT
// ══════════════════════════════════════════════
async function loadUsers() {
  const tbody = document.getElementById('usersTableBody');
  tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:2rem;color:var(--text-muted);">Loading…</td></tr>';
  try {
    allUsers = await apiFetch('/api/admin/users');
    renderUsersTable(allUsers);
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:#ff6b7a;padding:2rem;">${e.message}</td></tr>`;
  }
}

function filterUsers() {
  const q = document.getElementById('userSearchInput').value.toLowerCase();
  renderUsersTable(allUsers.filter(u =>
    u.name?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
  ));
}

function renderUsersTable(users) {
  const tbody = document.getElementById('usersTableBody');
  if (!users.length) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:2rem;color:var(--text-muted);">No users found.</td></tr>';
    return;
  }
  tbody.innerHTML = users.map(u => `
    <tr>
      <td><div style="display:flex;align-items:center;gap:0.75rem;">
        <div class="user-avatar-sm">${(u.name || '?')[0].toUpperCase()}</div>
        <div>
          <div style="font-weight:600;color:var(--text-primary);">${esc(u.name)}</div>
          <div style="font-size:0.78rem;color:var(--text-muted);">#${u.id}</div>
        </div>
      </div></td>
      <td style="color:var(--text-secondary);">${esc(u.email)}</td>
      <td><span class="role-badge role-${(u.role||'').toLowerCase().replace('role_','')}">${formatRole(u.role)}</span></td>
      <td style="text-align:center;">${u.recipesCount ?? 0}</td>
      <td><span class="status-dot ${u.enabled ? 'active' : 'inactive'}"></span>${u.enabled ? 'Active' : 'Disabled'}</td>
      <td style="font-size:0.8rem;color:var(--text-muted);">${u.createdAt ? formatDate(u.createdAt) : '-'}</td>
      <td>
        <div style="display:flex;gap:0.4rem;flex-wrap:wrap;">
          <button class="action-btn edit-btn" onclick="openEditUser(${u.id})">✏️ Edit</button>
          <button class="action-btn toggle-btn" onclick="toggleUser(${u.id}, ${u.enabled})">${u.enabled ? '🔒 Disable' : '🔓 Enable'}</button>
          <button class="action-btn delete-btn" onclick="deleteUser(${u.id}, '${esc(u.name)}')">🗑️ Delete</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function openCreateUser() {
  editingUserId = null;
  document.getElementById('userModalTitle').textContent = 'Create New User';
  document.getElementById('userModalForm').reset();
  document.getElementById('passwordField').style.display = 'block';
  document.getElementById('passwordInput').required = true;
  openModal('userModal');
}

function openEditUser(id) {
  const u = allUsers.find(x => x.id === id);
  if (!u) return;
  editingUserId = id;
  document.getElementById('userModalTitle').textContent = 'Edit User';
  document.getElementById('modalName').value  = u.name  || '';
  document.getElementById('modalEmail').value = u.email || '';
  document.getElementById('modalRole').value  = u.role  || 'ROLE_EXPLORER';
  document.getElementById('passwordField').style.display = 'none';
  document.getElementById('passwordInput').required = false;
  openModal('userModal');
}

async function handleSaveUser(e) {
  e.preventDefault();
  const btn = document.getElementById('userModalSubmitBtn');
  btn.disabled = true;
  btn.textContent = 'Saving…';

  const payload = {
    name:  document.getElementById('modalName').value.trim(),
    email: document.getElementById('modalEmail').value.trim(),
    role:  document.getElementById('modalRole').value,
  };
  if (!editingUserId) {
    payload.password = document.getElementById('passwordInput').value;
  }

  try {
    if (editingUserId) {
      await apiFetch(`/api/admin/users/${editingUserId}`, { method: 'PUT', body: JSON.stringify(payload) });
      showToast('User updated successfully', 'success');
    } else {
      await apiFetch('/api/admin/users', { method: 'POST', body: JSON.stringify(payload) });
      showToast('User created successfully', 'success');
    }
    closeModal('userModal');
    await loadUsers();
  } catch (err) {
    showToast(err.message || 'Operation failed', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Save';
  }
}

async function toggleUser(id, currentlyEnabled) {
  const action = currentlyEnabled ? 'disable' : 'enable';
  if (!confirm(`Are you sure you want to ${action} this user?`)) return;
  try {
    await apiFetch(`/api/admin/users/${id}/toggle`, { method: 'PATCH' });
    showToast(`User ${action}d successfully`, 'success');
    await loadUsers();
  } catch (err) {
    showToast(err.message || 'Operation failed', 'error');
  }
}

async function deleteUser(id, name) {
  if (!confirm(`Delete user "${name}"? This action cannot be undone.`)) return;
  try {
    await apiFetch(`/api/admin/users/${id}`, { method: 'DELETE' });
    showToast('User deleted successfully', 'success');
    await loadUsers();
  } catch (err) {
    showToast(err.message || 'Failed to delete user', 'error');
  }
}

// ══════════════════════════════════════════════
//  RECIPE APPROVAL
// ══════════════════════════════════════════════
async function loadRecipes() {
  const tbody = document.getElementById('recipesTableBody');
  tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:2rem;color:var(--text-muted);">Loading…</td></tr>';
  try {
    allRecipes = await apiFetch('/api/admin/recipes');
    renderRecipesTable(allRecipes.filter(r => r.status === pendingFilter));
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#ff6b7a;padding:2rem;">${e.message}</td></tr>`;
  }
}

function filterRecipesTable() {
  renderRecipesTable(allRecipes.filter(r => pendingFilter === 'ALL' || r.status === pendingFilter));
}

function renderRecipesTable(recipes) {
  const tbody = document.getElementById('recipesTableBody');
  if (!recipes.length) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:2rem;color:var(--text-muted);">No ${pendingFilter !== 'ALL' ? pendingFilter.toLowerCase() : ''} recipes.</td></tr>`;
    return;
  }
  tbody.innerHTML = recipes.map(r => `
    <tr>
      <td>
        <div style="display:flex;align-items:center;gap:0.75rem;">
          <img src="${r.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=80&q=60'}"
               style="width:50px;height:50px;border-radius:8px;object-fit:cover;"
               onerror="this.src='https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=80&q=60'" />
          <div>
            <div style="font-weight:600;color:var(--text-primary);">${esc(r.title)}</div>
            <div style="font-size:0.78rem;color:var(--text-muted);">${esc(r.category || '')}</div>
          </div>
        </div>
      </td>
      <td style="color:var(--text-secondary);">${esc(r.authorName || r.author?.name || '-')}</td>
      <td>${r.submittedAt || r.createdAt ? formatDate(r.submittedAt || r.createdAt) : '-'}</td>
      <td><span class="status-badge status-${(r.status||'').toLowerCase()}">${r.status}</span></td>
      <td>${r.reelUrl || r.reel ? '🎬 Yes' : '—'}</td>
      <td>
        <div style="display:flex;gap:0.4rem;flex-wrap:wrap;">
          <a href="recipe.html?id=${r.id}" target="_blank" class="action-btn edit-btn">👁 View</a>
          ${r.status !== 'APPROVED' ? `<button class="action-btn approve-btn" onclick="approveRecipe(${r.id})">✅ Approve</button>` : ''}
          ${r.status !== 'REJECTED' ? `<button class="action-btn delete-btn" onclick="rejectRecipe(${r.id})">❌ Reject</button>` : ''}
        </div>
      </td>
    </tr>
  `).join('');
}

async function approveRecipe(id) {
  if (!confirm('Approve this recipe?')) return;
  try {
    await apiFetch(`/api/admin/recipes/${id}/approve`, { method: 'PUT' });
    showToast('Recipe approved successfully', 'success');
    await loadRecipes();
  } catch (err) {
    showToast(err.message || 'Failed to approve recipe', 'error');
  }
}

async function rejectRecipe(id) {
  const reason = prompt('Enter rejection reason (optional):') ?? '';
  try {
    await apiFetch(`/api/admin/recipes/${id}/reject`, {
      method: 'PUT',
      body: JSON.stringify({ reason })
    });
    showToast('Recipe rejected successfully', 'success');
    await loadRecipes();
  } catch (err) {
    showToast(err.message || 'Failed to reject recipe', 'error');
  }
}

// ══════════════════════════════════════════════
//  CONTENT MODERATION
// ══════════════════════════════════════════════
async function loadComments() {
  const container = document.getElementById('commentsContainer');
  container.innerHTML = '<div style="text-align:center;padding:2rem;color:var(--text-muted);">Loading comments…</div>';
  try {
    allComments = await apiFetch('/api/admin/moderation/comments');
    renderComments(allComments);
  } catch (e) {
    container.innerHTML = `<div style="color:#ff6b7a;padding:1rem;">${e.message}</div>`;
  }
}

function filterComments() {
  const q = document.getElementById('commentSearchInput').value.toLowerCase();
  renderComments(allComments.filter(c =>
    c.content?.toLowerCase().includes(q) || c.authorName?.toLowerCase().includes(q)
  ));
}

function renderComments(comments) {
  const container = document.getElementById('commentsContainer');
  if (!comments.length) {
    container.innerHTML = '<div style="text-align:center;padding:3rem;color:var(--text-muted);">No comments to moderate.</div>';
    return;
  }
  container.innerHTML = comments.map(c => `
    <div class="comment-mod-card ${c.hidden ? 'hidden-comment' : ''}" id="comment-${c.id}">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:1rem;">
        <div>
          <div style="font-weight:600;color:var(--text-primary);">${esc(c.authorName || 'Unknown')}</div>
          <div style="font-size:0.8rem;color:var(--text-muted);">on <a href="recipe.html?id=${c.recipeId}" target="_blank" style="color:var(--primary);">Recipe #${c.recipeId}</a> · ${c.createdAt ? formatDate(c.createdAt) : ''}</div>
        </div>
        <div style="display:flex;gap:0.5rem;">
          <button class="action-btn edit-btn" onclick="toggleHideComment(${c.id}, ${c.hidden})">
            ${c.hidden ? '👁 Unhide' : '🙈 Hide'}
          </button>
          <button class="action-btn delete-btn" onclick="deleteComment(${c.id})">🗑️ Delete</button>
        </div>
      </div>
      <p style="margin:0.75rem 0 0;color:${c.hidden ? 'var(--text-muted)' : 'var(--text-secondary)'};">"${esc(c.content)}"</p>
      ${c.hidden ? '<span style="font-size:0.75rem;color:var(--text-muted);font-style:italic;">Hidden from public</span>' : ''}
    </div>
  `).join('');
}

async function toggleHideComment(id, isHidden) {
  try {
    if (isHidden) {
      await apiFetch(`/api/admin/comments/${id}/unhide`, { method: 'PATCH' });
      showToast('Comment is now visible', 'success');
    } else {
      await apiFetch(`/api/admin/comments/${id}/hide`, { method: 'PATCH' });
      showToast('Comment hidden successfully', 'success');
    }
    await loadComments();
  } catch (err) {
    showToast(err.message || 'Operation failed', 'error');
  }
}

async function deleteComment(id) {
  if (!confirm('Permanently delete this comment?')) return;
  try {
    await apiFetch(`/api/admin/comments/${id}`, { method: 'DELETE' });
    showToast('Comment deleted successfully', 'success');
    await loadComments();
  } catch (err) {
    showToast(err.message || 'Failed to delete comment', 'error');
  }
}

// ══════════════════════════════════════════════
//  SYSTEM SETTINGS
// ══════════════════════════════════════════════
async function loadSettings() {
  const form = document.getElementById('settingsForm');
  form.innerHTML = '<div style="text-align:center;padding:2rem;color:var(--text-muted);">Loading settings…</div>';
  try {
    const settings = await apiFetch('/api/admin/settings');
    renderSettings(settings);
  } catch (e) {
    form.innerHTML = `<div style="color:#ff6b7a;">${e.message}</div>`;
  }
}

function renderSettings(settings) {
  const form = document.getElementById('settingsForm');
  const knownKeys = {
    'site.name':            { label: 'Site Name', type: 'text' },
    'site.description':     { label: 'Site Description', type: 'textarea' },
    'recipes.require_approval': { label: 'Require Recipe Approval', type: 'toggle' },
    'recipes.max_images':   { label: 'Max Images per Recipe', type: 'number' },
    'reels.max_duration':   { label: 'Max Reel Duration (seconds)', type: 'number' },
    'comments.enabled':     { label: 'Comments Enabled', type: 'toggle' },
    'registration.enabled': { label: 'New Registrations Enabled', type: 'toggle' },
  };

  const settingsArray = Array.isArray(settings) ? settings : Object.entries(settings).map(([k,v]) => ({ key: k, value: v }));

  form.innerHTML = `
    <div class="settings-grid">
      ${settingsArray.map(s => {
        const meta = knownKeys[s.key] || { label: s.key, type: 'text' };
        const isToggle = meta.type === 'toggle';
        const isTextarea = meta.type === 'textarea';
        const val = s.value ?? '';
        return `
          <div class="setting-item">
            <label for="setting_${s.key}" class="setting-label">${meta.label}</label>
            ${isToggle ? `
              <label class="toggle-switch">
                <input type="checkbox" id="setting_${s.key}" data-key="${s.key}" ${val === 'true' ? 'checked' : ''} />
                <span class="toggle-slider"></span>
              </label>
            ` : isTextarea ? `
              <textarea id="setting_${s.key}" data-key="${s.key}" class="setting-input" rows="3">${esc(val)}</textarea>
            ` : `
              <input type="${meta.type}" id="setting_${s.key}" data-key="${s.key}" value="${esc(val)}" class="setting-input" />
            `}
            <p class="setting-key-hint">Key: <code>${s.key}</code></p>
          </div>
        `;
      }).join('')}
    </div>
    <button class="btn-primary" onclick="saveSettings()" style="margin-top:1.5rem;">💾 Save Settings</button>
  `;
}

async function saveSettings() {
  const inputs = document.querySelectorAll('#settingsForm [data-key]');
  const settings = {};
  inputs.forEach(el => {
    settings[el.dataset.key] = el.type === 'checkbox' ? String(el.checked) : el.value;
  });

  try {
    await apiFetch('/api/admin/settings', { method: 'PUT', body: JSON.stringify(settings) });
    showToast('Settings updated successfully', 'success');
  } catch (err) {
    showToast(err.message || 'Failed to save settings', 'error');
  }
}

// ── Modal helpers ──────────────────────────────
function openModal(id) { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

// Close modal on backdrop click
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-backdrop')) {
    e.target.classList.remove('open');
  }
});

// ── Utility ────────────────────────────────────
function esc(str) {
  if (str == null) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function debounce(fn, delay) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), delay); };
}

function formatRole(role) {
  const map = { ROLE_ADMIN: '👑 Admin', ROLE_CONTRIBUTOR: '👨‍🍳 Chef', ROLE_EXPLORER: '🧑 Explorer' };
  return map[role] || role;
}

function formatDate(dateStr) {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}
