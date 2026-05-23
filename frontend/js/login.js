/**
 * login.js — Fixed version
 */

// Apply saved theme
const savedTheme = localStorage.getItem('erp_theme') || 'dark';
document.documentElement.setAttribute('data-theme', savedTheme);

/** Password toggle */
function togglePassword() {
  const input = document.getElementById('password');
  input.type = input.type === 'password' ? 'text' : 'password';
}

/** Theme toggle */
function toggleTheme() {
  const html = document.documentElement;
  const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', next);
  localStorage.setItem('erp_theme', next);
}

/** Fill demo credentials */
function fillDemo(email) {
  document.getElementById('email').value = email;
  document.getElementById('password').value = 'Password@123';
}

/** Redirect based on role */
function redirectToDashboard(role) {
  const routes = {
    admin:      'pages/admin-dashboard.html',
    teacher:    'pages/teacher-dashboard.html',
    student:    'pages/student-dashboard.html',
    accountant: 'pages/accountant-dashboard.html',
    parent:     'pages/student-dashboard.html',
  };
  window.location.replace(routes[role] || 'pages/admin-dashboard.html');
}

/** Login form submit */
document.addEventListener('DOMContentLoaded', function() {

  // Clear any old session
  localStorage.removeItem('erp_token');
  localStorage.removeItem('erp_user');

  document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const email    = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const btn      = document.getElementById('loginBtn');
    const alertBox = document.getElementById('alert-box');

    if (!email || !password) {
      alertBox.className = 'alert alert-error';
      alertBox.textContent = 'Please enter email and password';
      alertBox.classList.remove('hidden');
      return;
    }

    // Loading state
    btn.disabled = true;
    btn.querySelector('.btn-text').classList.add('hidden');
    btn.querySelector('.btn-loader').classList.remove('hidden');
    alertBox.className = 'alert hidden';

    try {
      const res = await AuthAPI.login(email, password);

      // Reset loading
      btn.disabled = false;
      btn.querySelector('.btn-text').classList.remove('hidden');
      btn.querySelector('.btn-loader').classList.add('hidden');

      if (!res || !res.ok) {
        alertBox.className = 'alert alert-error';
        alertBox.textContent = res?.data?.error || 'Invalid email or password';
        alertBox.classList.remove('hidden');
        return;
      }

      const { token, user } = res.data.data;
      Auth.setSession(token, user);

      alertBox.className = 'alert alert-success';
      alertBox.textContent = 'Login successful! Redirecting...';
      alertBox.classList.remove('hidden');

      setTimeout(function() {
        redirectToDashboard(user.role);
      }, 800);

    } catch(err) {
      btn.disabled = false;
      btn.querySelector('.btn-text').classList.remove('hidden');
      btn.querySelector('.btn-loader').classList.add('hidden');
      alertBox.className = 'alert alert-error';
      alertBox.textContent = 'Connection error. Is the backend running on port 8080?';
      alertBox.classList.remove('hidden');
    }
  });
});
```

Save and close.

---

## Now Refresh Browser:
```
http://localhost:3000/index.html