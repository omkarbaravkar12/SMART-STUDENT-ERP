/**
 * dashboard.js — Shared dashboard utilities
 * Used across all role dashboards
 */

// ─── Auth Guard ──────────────────────────────────────────
(function () {
  if (!Auth.isLoggedIn()) { window.location.href = '../index.html'; }
  // Apply saved theme
  const theme = localStorage.getItem('erp_theme') || 'dark';
  document.documentElement.setAttribute('data-theme', theme);
})();

// ─── User Info ───────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  const user = Auth.getUser();
  if (!user) return;

  const name     = `${user.firstName} ${user.lastName}`;
  const initials = `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}`.toUpperCase();
  const role     = user.role;

  ['userNameSidebar', 'adminName', 'teacherName', 'studentName', 'accountantName']
    .forEach(id => { const el = document.getElementById(id); if (el) el.textContent = user.firstName; });
  ['userAvatarSidebar']
    .forEach(id => { const el = document.getElementById(id); if (el) el.textContent = initials; });
  ['userRoleSidebar']
    .forEach(id => { const el = document.getElementById(id); if (el) el.textContent = role; });

  // Set current date
  const dateEl = document.getElementById('currentDate');
  if (dateEl) {
    dateEl.textContent = new Date().toLocaleDateString('en-IN', { weekday:'long', year:'numeric', month:'long', day:'numeric' });
  }

  // Load notification count
  loadNotificationCount();
});

// ─── Sidebar Toggle ──────────────────────────────────────
function toggleSidebar() {
  const shell = document.getElementById('appShell');
  shell.classList.toggle('sidebar-collapsed');
  localStorage.setItem('sidebarCollapsed', shell.classList.contains('sidebar-collapsed'));
}

// Restore sidebar state
(function () {
  if (localStorage.getItem('sidebarCollapsed') === 'true') {
    const shell = document.getElementById('appShell');
    if (shell) shell.classList.add('sidebar-collapsed');
  }
})();

// ─── Theme Toggle ─────────────────────────────────────────
function toggleTheme() {
  const html = document.documentElement;
  const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', next);
  localStorage.setItem('erp_theme', next);
}

// ─── Logout ───────────────────────────────────────────────
async function logout() {
  await AuthAPI.logout().catch(() => {});
  Auth.clear();
  window.location.href = '../index.html';
}

// ─── Notification Count ───────────────────────────────────
async function loadNotificationCount() {
  const res = await NotificationsAPI.list().catch(() => null);
  if (!res?.ok) return;
  const notifs  = res.data?.data || [];
  const unread  = notifs.filter(n => !n.isRead).length;
  const dot     = document.getElementById('notifDot');
  const badge   = document.getElementById('notifBadge');
  if (dot)   { dot.style.display   = unread > 0 ? 'block' : 'none'; }
  if (badge) { badge.textContent   = unread > 0 ? unread : ''; badge.style.display = unread > 0 ? 'flex' : 'none'; }
}

// ─── Chart.js defaults ────────────────────────────────────
function getChartDefaults() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  return {
    textColor:   isDark ? '#a0a0b8' : '#4a4a60',
    gridColor:   isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)',
    bgSurface:   isDark ? '#1a1a24' : '#ffffff',
    primaryColor: '#6366f1',
    colors: ['#6366f1','#8b5cf6','#06b6d4','#10b981','#f59e0b','#ef4444','#3b82f6','#ec4899'],
  };
}

function buildLineChartOptions(yLabel = '') {
  const c = getChartDefaults();
  return {
    responsive: true,
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { color: c.gridColor }, ticks: { color: c.textColor, font: { size: 11 } } },
      y: { grid: { color: c.gridColor }, ticks: { color: c.textColor, font: { size: 11 } }, title: { display: !!yLabel, text: yLabel, color: c.textColor, font: { size: 11 } } },
    },
  };
}

function buildBarChartOptions(stacked = false) {
  const c = getChartDefaults();
  return {
    responsive: true,
    plugins: { legend: { labels: { color: c.textColor, font: { size: 11 } } } },
    scales: {
      x: { stacked, grid: { color: c.gridColor }, ticks: { color: c.textColor, font: { size: 11 } } },
      y: { stacked, grid: { color: c.gridColor }, ticks: { color: c.textColor, font: { size: 11 } } },
    },
  };
}

function buildDoughnutOptions() {
  const c = getChartDefaults();
  return {
    responsive: true,
    cutout: '68%',
    plugins: {
      legend: { position: 'bottom', labels: { color: c.textColor, font: { size: 11 }, padding: 12 } },
    },
  };
}

// ─── Table Helpers ────────────────────────────────────────
function createStatusBadge(status) {
  const map = {
    active: 'badge-success', inactive: 'badge-danger',
    paid: 'badge-success', pending: 'badge-warning', overdue: 'badge-danger', partial: 'badge-info',
    present: 'badge-success', absent: 'badge-danger', late: 'badge-warning',
    low: 'badge-success', medium: 'badge-warning', high: 'badge-danger',
    published: 'badge-success', draft: 'badge-warning',
    available: 'badge-success', borrowed: 'badge-warning',
  };
  const cls = map[status?.toLowerCase()] || 'badge-info';
  return `<span class="badge ${cls}">${status || '—'}</span>`;
}

function createAvatar(name, size = 'sm') {
  const initials = name?.split(' ').map(w => w[0]).join('').substring(0,2).toUpperCase() || '?';
  const colors   = ['#6366f1','#8b5cf6','#06b6d4','#10b981','#f59e0b','#ef4444'];
  const color    = colors[initials.charCodeAt(0) % colors.length];
  return `<div class="avatar avatar-${size}" style="background:${color}">${initials}</div>`;
}

function renderEmptyRow(colSpan, message = 'No data found') {
  return `<tr><td colspan="${colSpan}" style="text-align:center;padding:40px;color:var(--text-tertiary)">
    <div style="font-size:2rem;margin-bottom:8px">📭</div>
    <div>${message}</div>
  </td></tr>`;
}

// ─── Pagination ───────────────────────────────────────────
function renderPagination(container, currentPage, totalPages, onPageChange) {
  if (totalPages <= 1) { container.innerHTML = ''; return; }
  let html = '<div style="display:flex;align-items:center;gap:6px;padding:14px 20px;border-top:1px solid var(--border-subtle)">';
  html += `<button class="btn btn-secondary btn-sm" ${currentPage===1?'disabled':''} onclick="${onPageChange}(${currentPage-1})">‹ Prev</button>`;
  for (let i = 1; i <= Math.min(totalPages, 7); i++) {
    html += `<button class="btn btn-${i===currentPage?'primary':'secondary'} btn-sm" onclick="${onPageChange}(${i})">${i}</button>`;
  }
  if (totalPages > 7) html += `<span style="color:var(--text-tertiary);padding:0 4px">...</span>`;
  html += `<button class="btn btn-secondary btn-sm" ${currentPage===totalPages?'disabled':''} onclick="${onPageChange}(${currentPage+1})">Next ›</button>`;
  html += `<span style="color:var(--text-tertiary);font-size:0.78rem;margin-left:8px">Page ${currentPage} of ${totalPages}</span>`;
  html += '</div>';
  container.innerHTML = html;
}

// ─── Export helper ────────────────────────────────────────
function exportReport() {
  Toast.info('Report export coming soon! Generating PDF...');
}

// ─── Mobile Sidebar ───────────────────────────────────────
document.addEventListener('click', (e) => {
  const shell   = document.getElementById('appShell');
  const sidebar = document.getElementById('sidebar');
  if (!shell || !sidebar) return;
  if (window.innerWidth <= 768) {
    if (!sidebar.contains(e.target) && !e.target.closest('.sidebar-toggle')) {
      shell.classList.remove('mobile-open');
    }
  }
});

// Override toggleSidebar for mobile
const _origToggle = toggleSidebar;
window.toggleSidebar = function() {
  if (window.innerWidth <= 768) {
    document.getElementById('appShell')?.classList.toggle('mobile-open');
  } else {
    _origToggle();
  }
};
