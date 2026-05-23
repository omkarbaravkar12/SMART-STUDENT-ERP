/**
 * api.js — Smart Student ERP Frontend API Layer
 * All HTTP calls to the Java backend
 */

const API_BASE = 'http://localhost:8080/api';

/** Token management */
const Auth = {
  getToken:   () => localStorage.getItem('erp_token'),
  getUser:    () => { try { return JSON.parse(localStorage.getItem('erp_user') || 'null'); } catch { return null; } },
  setSession: (token, user) => { localStorage.setItem('erp_token', token); localStorage.setItem('erp_user', JSON.stringify(user)); },
  clear:      () => { localStorage.removeItem('erp_token'); localStorage.removeItem('erp_user'); },
  isLoggedIn: () => !!localStorage.getItem('erp_token'),
  getRole:    () => Auth.getUser()?.role || null,
};

/** Core HTTP client */
async function request(method, path, body = null, options = {}) {
  const token = Auth.getToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const config = { method, headers, ...options };
  if (body)  config.body = JSON.stringify(body);

  try {
    const res  = await fetch(`${API_BASE}${path}`, config);
    const data = await res.json().catch(() => ({}));
    if (res.status === 401) {
      Auth.clear();
      window.location.href = '/index.html';
      return null;
    }
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    console.error('API Error:', err);
    return { ok: false, status: 0, data: { error: 'Network error. Is the backend running?' } };
  }
}

const get    = (path) => request('GET',    path);
const post   = (path, body) => request('POST',   path, body);
const put    = (path, body) => request('PUT',    path, body);
const del    = (path) => request('DELETE', path);

/* ─── Auth ─────────────────────────────────────────────── */
const AuthAPI = {
  login:          (email, password) => post('/auth/login',          { email, password }),
  logout:         ()               => post('/auth/logout',          {}),
  me:             ()               => get('/auth/me'),
  forgotPassword: (email)          => post('/auth/forgot-password', { email }),
  verifyOtp:      (email, otp, newPassword) => post('/auth/verify-otp', { email, otp, newPassword }),
};

/* ─── Students ──────────────────────────────────────────── */
const StudentsAPI = {
  list:       (params = {}) => get('/students?' + new URLSearchParams(params)),
  get:        (id)          => get(`/students/${id}`),
  create:     (data)        => post('/students', data),
  update:     (id, data)    => put(`/students/${id}`, data),
  delete:     (id)          => del(`/students/${id}`),
  attendance: (id, params)  => get(`/students/${id}/attendance?` + new URLSearchParams(params || {})),
  results:    (id, params)  => get(`/students/${id}/results?` + new URLSearchParams(params || {})),
  timetable:  (id)          => get(`/students/${id}/timetable`),
};

/* ─── Teachers ──────────────────────────────────────────── */
const TeachersAPI = {
  list:   (params = {}) => get('/teachers?' + new URLSearchParams(params)),
  get:    (id)          => get(`/teachers/${id}`),
  create: (data)        => post('/teachers', data),
  update: (id, data)    => put(`/teachers/${id}`, data),
};

/* ─── Courses / Subjects / Depts ────────────────────────── */
const CoursesAPI = {
  list: () => get('/courses'),
};
const SubjectsAPI = {
  list: () => get('/subjects'),
};
const DepartmentsAPI = {
  list: () => get('/departments'),
};

/* ─── Attendance ────────────────────────────────────────── */
const AttendanceAPI = {
  list:    (params = {}) => get('/attendance?' + new URLSearchParams(params)),
  summary: (params = {}) => get('/attendance/summary?' + new URLSearchParams(params)),
  mark:    (data)        => post('/attendance', data),
};

/* ─── Exams ─────────────────────────────────────────────── */
const ExamsAPI = {
  list:        (params = {}) => get('/exams?' + new URLSearchParams(params)),
  get:         (id)          => get(`/exams/${id}`),
  create:      (data)        => post('/exams', data),
  update:      (id, data)    => put(`/exams/${id}`, data),
  getResults:  (params = {}) => get('/results?' + new URLSearchParams(params)),
  saveResult:  (data)        => post('/results', data),
};

/* ─── Fees ──────────────────────────────────────────────── */
const FeesAPI = {
  structures: (params = {}) => get('/fees?' + new URLSearchParams(params)),
  create:     (data)        => post('/fees', data),
  payments:   (params = {}) => get('/payments?' + new URLSearchParams(params)),
  pay:        (data)        => post('/payments', data),
  invoices:   (params = {}) => get('/invoices?' + new URLSearchParams(params)),
};

/* ─── Notifications ─────────────────────────────────────── */
const NotificationsAPI = {
  list:   () => get('/notifications'),
  create: (data) => post('/notifications', data),
};

/* ─── Timetable ─────────────────────────────────────────── */
const TimetableAPI = {
  list: (params = {}) => get('/timetable?' + new URLSearchParams(params)),
};

/* ─── Library ───────────────────────────────────────────── */
const LibraryAPI = {
  books:  (params = {}) => get('/library/books?' + new URLSearchParams(params)),
  addBook: (data)       => post('/library/books', data),
  borrow: (data)        => post('/library/borrow', data),
  return: (data)        => put('/library/borrow', data),
};

/* ─── Analytics ─────────────────────────────────────────── */
const AnalyticsAPI = {
  dashboard:  () => get('/analytics/dashboard'),
  students:   () => get('/analytics/students'),
  fees:       () => get('/analytics/fees'),
  attendance: () => get('/analytics/attendance'),
};

/** Toast notification helper */
window.Toast = {
  show(message, type = 'info', duration = 4000) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icons = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };
    toast.innerHTML = `<span style="font-size:1.1rem">${icons[type] || 'ℹ'}</span><span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.animation = 'toast-out 0.3s ease forwards';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  },
  success: (msg) => Toast.show(msg, 'success'),
  error:   (msg) => Toast.show(msg, 'error'),
  warning: (msg) => Toast.show(msg, 'warning'),
  info:    (msg) => Toast.show(msg, 'info'),
};

/** Redirect if not logged in */
function requireAuth() {
  if (!Auth.isLoggedIn()) { window.location.href = '/index.html'; return false; }
  return true;
}

/** Role guard */
function requireRole(...roles) {
  const userRole = Auth.getRole();
  if (!roles.includes(userRole)) {
    Toast.error('Access denied: insufficient permissions');
    return false;
  }
  return true;
}

/** Format currency (INR) */
function formatCurrency(amount) {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 0 }).format(amount);
}

/** Format date */
function formatDate(dateStr) {
  if (!dateStr) return '—';
  try { return new Date(dateStr).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' }); }
  catch { return dateStr; }
}

/** Animated counter */
function animateCounter(element, target, duration = 1200) {
  const start = 0, step = target / (duration / 16);
  let current = start;
  const tick = () => {
    current = Math.min(current + step, target);
    element.textContent = Math.floor(current).toLocaleString('en-IN');
    if (current < target) requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
}

/** Debounce */
function debounce(fn, delay) {
  let t; return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), delay); };
}
