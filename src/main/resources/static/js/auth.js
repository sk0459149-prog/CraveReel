// Authentication & Navigation State Handler

document.addEventListener('DOMContentLoaded', () => {
  initAuthState();
});

async function initAuthState() {
  const token = getAuthToken();
  const authNav = document.getElementById('auth-nav-section');
  if (!authNav) return;

  if (!token) {
    renderLoggedOutNav(authNav);
    return;
  }

  try {
    const res = await apiFetch('/auth/me');
    if (res.success && res.data) {
      setStoredUser(res.data);
      renderLoggedInNav(authNav, res.data);
    } else {
      removeAuthToken();
      renderLoggedOutNav(authNav);
    }
  } catch (err) {
    removeAuthToken();
    renderLoggedOutNav(authNav);
  }
}

function renderLoggedOutNav(container) {
  container.innerHTML = `
    <div class="d-flex align-items-center gap-2">
      <a href="login.html" class="btn btn-sm btn-rr-secondary">Log In</a>
      <a href="register.html" class="btn btn-sm btn-rr-primary">Get Started</a>
    </div>
  `;
}

function renderLoggedInNav(container, user) {
  let dashboardLink = 'explorer-dashboard.html';
  let dashboardLabel = 'My Dashboard';

  if (user.role === 'ROLE_ADMIN') {
    dashboardLink = 'admin-dashboard.html';
    dashboardLabel = 'Admin Dashboard';
  } else if (user.role === 'ROLE_CONTRIBUTOR') {
    dashboardLink = 'contributor-dashboard.html';
    dashboardLabel = 'Chef Dashboard';
  }

  const avatar = user.avatarUrl || 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150';

  container.innerHTML = `
    <div class="dropdown">
      <button class="btn btn-sm btn-rr-secondary dropdown-toggle d-flex align-items-center gap-2" type="button" data-bs-toggle="dropdown" aria-expanded="false">
        <img src="${avatar}" alt="${user.name}" style="width: 28px; height: 28px; border-radius: 50%; object-fit: cover; border: 1px solid var(--accent-primary);">
        <span class="d-none d-md-inline fw-semibold">${user.name.split(' ')[0]}</span>
      </button>
      <ul class="dropdown-menu dropdown-menu-end shadow-lg" style="background: var(--bg-card); border-color: var(--border-color); min-width: 220px;">
        <li class="px-3 py-2 border-bottom" style="border-color: var(--border-color) !important;">
          <div class="fw-bold text-white">${user.name}</div>
          <div class="small text-muted text-truncate">${user.email}</div>
          <span class="badge bg-secondary mt-1" style="font-size: 0.65rem;">${formatRole(user.role)}</span>
        </li>
        <li><a class="dropdown-item py-2 d-flex align-items-center gap-2" href="${dashboardLink}"><i class="bi bi-speedometer2 text-warning"></i> ${dashboardLabel}</a></li>
        <li><a class="dropdown-item py-2 d-flex align-items-center gap-2" href="saved.html"><i class="bi bi-bookmark-heart text-danger"></i> Saved Recipes</a></li>
        ${user.role === 'ROLE_CONTRIBUTOR' ? '<li><a class="dropdown-item py-2 d-flex align-items-center gap-2" href="contributor-dashboard.html?tab=add"><i class="bi bi-plus-circle text-success"></i> Add Recipe</a></li>' : ''}
        <li><hr class="dropdown-divider" style="border-color: var(--border-color);"></li>
        <li><button class="dropdown-item py-2 text-danger d-flex align-items-center gap-2" onclick="logout()"><i class="bi bi-box-arrow-right"></i> Log Out</button></li>
      </ul>
    </div>
  `;
}

function formatRole(role) {
  if (role === 'ROLE_ADMIN') return 'Administrator';
  if (role === 'ROLE_CONTRIBUTOR') return 'Chef / Creator';
  return 'Recipe Explorer';
}

function logout() {
  removeAuthToken();
  showToast('Logged out successfully', 'success');
  setTimeout(() => {
    window.location.href = 'index.html';
  }, 500);
}
