/**
 * students.js — Full student management page logic
 */

// Inject sidebar
document.getElementById('sidebar').innerHTML = `
<div class="sidebar-brand">
  <div class="brand-icon"><svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg"><rect width="48" height="48" rx="14" fill="url(#sg2)"/><path d="M24 10L38 17V25C38 33 31 39.5 24 42C17 39.5 10 33 10 25V17L24 10Z" fill="white" fill-opacity="0.9"/><defs><linearGradient id="sg2" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse"><stop stop-color="#6366f1"/><stop offset="1" stop-color="#8b5cf6"/></linearGradient></defs></svg></div>
  <span class="brand-name">Smart ERP</span>
</div>
<nav class="sidebar-nav">
  <a class="nav-item" href="admin-dashboard.html"><svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M3 4a1 1 0 011-1h12a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zM3 10a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H4a1 1 0 01-1-1v-6zM14 9a1 1 0 00-1 1v6a1 1 0 001 1h2a1 1 0 001-1v-6a1 1 0 00-1-1h-2z"/></svg><span class="nav-label">Dashboard</span></a>
  <a class="nav-item active" href="students.html"><svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M13 6a3 3 0 11-6 0 3 3 0 016 0zM18 8a2 2 0 11-4 0 2 2 0 014 0zM14 15a4 4 0 00-8 0v3h8v-3zM6 8a2 2 0 11-4 0 2 2 0 014 0zM16 18v-3a5.972 5.972 0 00-.75-2.906A3.005 3.005 0 0119 15v3h-3zM4.75 12.094A5.973 5.973 0 004 15v3H1v-3a3 3 0 013.75-2.906z"/></svg><span class="nav-label">Students</span></a>
  <a class="nav-item" href="teachers.html"><svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/></svg><span class="nav-label">Teachers</span></a>
  <a class="nav-item" href="attendance.html"><svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z" clip-rule="evenodd"/></svg><span class="nav-label">Attendance</span></a>
  <a class="nav-item" href="exams.html"><svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/></svg><span class="nav-label">Exams</span></a>
  <a class="nav-item" href="fees.html"><svg class="nav-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/><path fill-rule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clip-rule="evenodd"/></svg><span class="nav-label">Fees</span></a>
</nav>
<div class="sidebar-footer">
  <div class="user-card"><div class="user-avatar" id="userAvatarSidebar">A</div><div class="user-info"><div class="user-name" id="userNameSidebar">Admin</div><div class="user-role" id="userRoleSidebar">admin</div></div></div>
</div>`;

let currentPage  = 1;
let totalPages   = 1;
let currentView  = 'table';
let allStudents  = [];
let editingId    = null;
let sortField    = 'name';
let sortAsc      = true;

document.addEventListener('DOMContentLoaded', async () => {
  await loadFilters();
  loadStudents();
  loadStudentStats();
  // Check URL params for direct student view
  const params = new URLSearchParams(window.location.search);
  if (params.get('id'))   viewStudentProfile(params.get('id'));
  if (params.get('edit')) { editingId = params.get('edit'); openAddStudentModal(editingId); }
});

async function loadFilters() {
  const [depts, courses] = await Promise.all([DepartmentsAPI.list(), CoursesAPI.list()]);
  if (depts?.ok) {
    const sel = document.getElementById('deptFilter');
    const f_dept = document.getElementById('f_dept');
    (depts.data?.data || []).forEach(d => {
      sel.innerHTML    += `<option value="${d.id}">${d.name}</option>`;
      if (f_dept) f_dept.innerHTML += `<option value="${d.id}">${d.name}</option>`;
    });
  }
  if (courses?.ok) {
    const sel = document.getElementById('courseFilter');
    const f_course = document.getElementById('f_course');
    (courses.data?.data || []).forEach(c => {
      sel.innerHTML    += `<option value="${c.id}">${c.name}</option>`;
      if (f_course) f_course.innerHTML += `<option value="${c.id}">${c.name}</option>`;
    });
  }
}

async function loadStudentStats() {
  const [studRes, attendRes] = await Promise.all([
    StudentsAPI.list({ limit: 1 }),
    AttendanceAPI.summary(),
  ]);
  const total = studRes?.data?.meta?.total || 0;
  const statsEl = document.getElementById('studentStats');
  const avgAtt  = 82; // computed from summary
  statsEl.innerHTML = `
    <div class="stat-card"><div class="sc-top"><div class="sc-icon" style="background:rgba(99,102,241,0.15)">🎓</div></div>
      <div class="sc-value" id="sc_total">—</div><div class="sc-label">Total Students</div></div>
    <div class="stat-card"><div class="sc-top"><div class="sc-icon" style="background:rgba(16,185,129,0.15)">✓</div></div>
      <div class="sc-value">${avgAtt}%</div><div class="sc-label">Avg Attendance</div></div>
    <div class="stat-card"><div class="sc-top"><div class="sc-icon" style="background:rgba(245,158,11,0.15)">⚠</div></div>
      <div class="sc-value" id="sc_risk">—</div><div class="sc-label">At-Risk Students</div></div>
    <div class="stat-card"><div class="sc-top"><div class="sc-icon" style="background:rgba(6,182,212,0.15)">📅</div></div>
      <div class="sc-value" id="sc_new">—</div><div class="sc-label">New This Year</div></div>
  `;
  animateCounter(document.getElementById('sc_total'), total);
}

let filterTimeout;
function filterStudents() {
  clearTimeout(filterTimeout);
  filterTimeout = setTimeout(() => { currentPage = 1; loadStudents(); }, 300);
}

function sortBy(field) {
  if (sortField === field) sortAsc = !sortAsc;
  else { sortField = field; sortAsc = true; }
  loadStudents();
}

async function loadStudents() {
  const search   = document.getElementById('searchInput')?.value?.trim() || '';
  const deptId   = document.getElementById('deptFilter')?.value  || '';
  const courseId = document.getElementById('courseFilter')?.value || '';
  const semester = document.getElementById('semFilter')?.value   || '';

  const tbody = document.getElementById('studentsBody');
  if (tbody) tbody.innerHTML = `<tr><td colspan="10" style="text-align:center;padding:40px;color:var(--text-tertiary)">Loading...</td></tr>`;

  const res = await StudentsAPI.list({ page: currentPage, limit: 15, search, departmentId: deptId, courseId, semester });
  if (!res?.ok) { if (tbody) tbody.innerHTML = renderEmptyRow(10, 'Failed to load. Is the backend running?'); return; }

  allStudents = res.data?.data || [];
  const meta  = res.data?.meta || {};
  totalPages  = meta.totalPages || 1;

  document.getElementById('tableCount').textContent = `Students (${meta.total || 0} total)`;

  if (currentView === 'table') renderTable(allStudents);
  else renderCards(allStudents);

  renderPagination(document.getElementById('paginationContainer'), currentPage, totalPages, 'goToPage');
}

function goToPage(page) { currentPage = page; loadStudents(); }

function renderTable(students) {
  const tbody = document.getElementById('studentsBody');
  if (!students.length) { tbody.innerHTML = renderEmptyRow(10); return; }
  tbody.innerHTML = students.map(s => `
    <tr>
      <td><input type="checkbox" value="${s.id}"/></td>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          ${createAvatar(`${s.firstName} ${s.lastName}`)}
          <div>
            <div style="font-weight:500;font-size:0.87rem">${s.firstName} ${s.lastName}</div>
            <div style="font-size:0.72rem;color:var(--text-tertiary)">${s.email}</div>
          </div>
        </div>
      </td>
      <td><span style="font-family:monospace;background:var(--bg-elevated);padding:2px 6px;border-radius:4px;font-size:0.78rem">${s.studentId}</span></td>
      <td><div style="font-size:0.82rem">${s.departmentName || '—'}</div></td>
      <td><div style="font-size:0.82rem">${s.courseName || '—'}</div></td>
      <td><span class="badge badge-primary">Sem ${s.currentSemester}</span></td>
      <td>
        <div style="display:flex;align-items:center;gap:8px">
          <div class="progress-bar" style="width:60px"><div class="progress-fill" style="width:82%;background:var(--brand-success)"></div></div>
          <span style="font-size:0.75rem">82%</span>
        </div>
      </td>
      <td><span style="font-weight:600;color:var(--brand-primary)">3.4</span></td>
      <td>${createStatusBadge(s.isActive !== false ? 'active' : 'inactive')}</td>
      <td>
        <div style="display:flex;gap:4px">
          <button class="btn btn-secondary btn-sm" onclick="viewStudentProfile(${s.id})" title="View">👁</button>
          <button class="btn btn-secondary btn-sm" onclick="openEditStudent(${s.id})" title="Edit">✏️</button>
          <button class="btn btn-danger btn-sm" onclick="deleteStudent(${s.id},'${s.firstName} ${s.lastName}')" title="Delete">🗑</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function renderCards(students) {
  const container = document.getElementById('studentsCards');
  if (!students.length) { container.innerHTML = `<div style="grid-column:1/-1;text-align:center;padding:40px;color:var(--text-secondary)">No students found</div>`; return; }
  container.innerHTML = students.map(s => `
    <div class="card" style="padding:20px;cursor:pointer;transition:var(--transition-normal)" onmouseenter="this.style.transform='translateY(-4px)'" onmouseleave="this.style.transform=''" onclick="viewStudentProfile(${s.id})">
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
        ${createAvatar(`${s.firstName} ${s.lastName}`, 'lg')}
        <div style="min-width:0">
          <div style="font-weight:600;font-size:0.95rem">${s.firstName} ${s.lastName}</div>
          <div style="font-size:0.75rem;color:var(--text-tertiary)">${s.studentId}</div>
        </div>
        ${createStatusBadge(s.isActive !== false ? 'active' : 'inactive')}
      </div>
      <div style="font-size:0.78rem;color:var(--text-secondary);margin-bottom:8px">${s.email}</div>
      <div style="display:flex;justify-content:space-between;font-size:0.78rem">
        <span style="color:var(--text-tertiary)">${s.courseName || '—'}</span>
        <span class="badge badge-primary">Sem ${s.currentSemester}</span>
      </div>
      <div style="margin-top:12px">
        <div style="display:flex;justify-content:space-between;font-size:0.72rem;color:var(--text-tertiary);margin-bottom:4px">
          <span>Attendance</span><span>82%</span>
        </div>
        <div class="progress-bar"><div class="progress-fill" style="width:82%"></div></div>
      </div>
    </div>
  `).join('');
}

function setView(view) {
  currentView = view;
  document.getElementById('tableView').style.display = view === 'table' ? 'block' : 'none';
  document.getElementById('cardView').style.display  = view === 'card'  ? 'block' : 'none';
  document.getElementById('btnTable').className = `btn btn-${view==='table'?'primary':'secondary'} btn-sm`;
  document.getElementById('btnCard').className  = `btn btn-${view==='card' ?'primary':'secondary'} btn-sm`;
  if (view === 'card') renderCards(allStudents);
  else renderTable(allStudents);
}

function openAddStudentModal(editId = null) {
  editingId = editId || null;
  document.getElementById('modalTitle').textContent = editId ? 'Edit Student' : 'Add New Student';
  document.getElementById('addStudentModal').style.display = 'flex';
}

function openEditStudent(id) { openAddStudentModal(id); }

async function saveStudent() {
  const btn = document.getElementById('saveBtn');
  btn.disabled = true; btn.textContent = 'Saving...';

  const data = {
    email:      document.getElementById('f_email')?.value?.trim(),
    firstName:  document.getElementById('f_firstName')?.value?.trim(),
    lastName:   document.getElementById('f_lastName')?.value?.trim(),
    phone:      document.getElementById('f_phone')?.value?.trim(),
    dateOfBirth:document.getElementById('f_dob')?.value,
    gender:     document.getElementById('f_gender')?.value,
    bloodGroup: document.getElementById('f_blood')?.value?.trim(),
    courseId:   document.getElementById('f_course')?.value,
    departmentId:document.getElementById('f_dept')?.value,
    address:    document.getElementById('f_address')?.value?.trim(),
    city:       document.getElementById('f_city')?.value?.trim(),
    state:      document.getElementById('f_state')?.value?.trim(),
  };

  if (!data.email || !data.firstName || !data.lastName) {
    Toast.error('First name, last name, and email are required');
    btn.disabled = false; btn.textContent = 'Save Student';
    return;
  }

  const res = editingId
    ? await StudentsAPI.update(editingId, data)
    : await StudentsAPI.create(data);

  btn.disabled = false; btn.textContent = 'Save Student';

  if (!res?.ok) {
    Toast.error(res?.data?.error || 'Failed to save student');
    return;
  }
  Toast.success(editingId ? 'Student updated!' : 'Student created! Default password: Password@123');
  document.getElementById('addStudentModal').style.display = 'none';
  loadStudents();
}

async function viewStudentProfile(id) {
  const modal   = document.getElementById('studentDetailModal');
  const content = document.getElementById('studentDetailContent');
  modal.style.display = 'flex';
  content.innerHTML   = '<div style="text-align:center;padding:32px;color:var(--text-secondary)">Loading profile...</div>';

  const [studentRes, attendanceRes, resultsRes] = await Promise.all([
    StudentsAPI.get(id),
    StudentsAPI.attendance(id, {}),
    StudentsAPI.results(id, {}),
  ]);

  if (!studentRes?.ok) { content.innerHTML = '<p style="color:var(--brand-danger)">Failed to load student profile</p>'; return; }
  const s = studentRes.data?.data;
  const att = attendanceRes?.data?.data || [];
  const results = resultsRes?.data?.data || {};

  const present = att.filter(a => a.status === 'present').length;
  const attPct  = att.length ? Math.round(present / att.length * 100) : 0;

  content.innerHTML = `
    <div style="display:flex;align-items:center;gap:20px;margin-bottom:24px;padding-bottom:20px;border-bottom:1px solid var(--border-subtle)">
      ${createAvatar(`${s.firstName} ${s.lastName}`, 'xl')}
      <div>
        <h3 style="font-family:var(--font-display);font-size:1.3rem;font-weight:700">${s.firstName} ${s.lastName}</h3>
        <div style="color:var(--text-secondary);font-size:0.85rem">${s.email}</div>
        <div style="display:flex;gap:8px;margin-top:8px">
          <span class="badge badge-primary">${s.studentId}</span>
          <span class="badge badge-info">${s.courseName || '—'}</span>
          <span class="badge badge-primary">Sem ${s.currentSemester}</span>
          ${createStatusBadge(s.isActive !== false ? 'active' : 'inactive')}
        </div>
      </div>
    </div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px">
      <div style="background:var(--bg-elevated);border-radius:var(--radius-md);padding:16px">
        <div style="font-size:0.72rem;color:var(--text-tertiary);text-transform:uppercase;letter-spacing:1px;margin-bottom:8px">Attendance</div>
        <div style="font-size:1.8rem;font-weight:800;font-family:var(--font-display);color:${attPct>=75?'var(--brand-success)':'var(--brand-danger)'}">${attPct}%</div>
        <div class="progress-bar" style="margin-top:8px"><div class="progress-fill" style="width:${attPct}%;background:${attPct>=75?'var(--brand-success)':'var(--brand-danger)'}"></div></div>
      </div>
      <div style="background:var(--bg-elevated);border-radius:var(--radius-md);padding:16px">
        <div style="font-size:0.72rem;color:var(--text-tertiary);text-transform:uppercase;letter-spacing:1px;margin-bottom:8px">GPA</div>
        <div style="font-size:1.8rem;font-weight:800;font-family:var(--font-display);color:var(--brand-primary)">${results.gpa || '—'}</div>
        <div style="font-size:0.75rem;color:var(--text-tertiary);margin-top:4px">${results.totalCredits || 0} credits completed</div>
      </div>
    </div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;font-size:0.83rem">
      ${[
        ['📞 Phone', s.phone || '—'],
        ['🩸 Blood Group', s.bloodGroup || '—'],
        ['📅 Admission', formatDate(s.admissionDate)],
        ['🏠 City', s.city || '—'],
        ['👤 Father', s.fatherName || '—'],
        ['📞 Parent Phone', s.fatherPhone || '—'],
      ].map(([label, val]) => `
        <div style="display:flex;gap:8px;padding:10px;background:var(--bg-elevated);border-radius:var(--radius-sm)">
          <span style="color:var(--text-secondary)">${label}:</span>
          <span style="font-weight:500;flex:1">${val}</span>
        </div>
      `).join('')}
    </div>
  `;
}

async function deleteStudent(id, name) {
  if (!confirm(`Are you sure you want to deactivate ${name}? This action can be reversed.`)) return;
  const res = await StudentsAPI.delete(id);
  if (res?.ok) { Toast.success(`${name} has been deactivated`); loadStudents(); }
  else Toast.error('Failed to delete student');
}

function selectAll(cb) {
  document.querySelectorAll('tbody input[type="checkbox"]').forEach(c => c.checked = cb.checked);
}

function exportStudents() {
  Toast.info('Generating CSV export...');
  const headers = ['ID','Name','Email','Course','Semester','Status'];
  const rows    = allStudents.map(s => [s.studentId, `${s.firstName} ${s.lastName}`, s.email, s.courseName, s.currentSemester, s.isActive ? 'Active' : 'Inactive']);
  const csv     = [headers, ...rows].map(r => r.join(',')).join('\n');
  const blob    = new Blob([csv], { type: 'text/csv' });
  const a       = document.createElement('a'); a.href = URL.createObjectURL(blob);
  a.download    = 'students.csv'; a.click();
}

function closeModal(e) {
  if (e.target.classList.contains('modal-overlay')) e.target.style.display = 'none';
}
