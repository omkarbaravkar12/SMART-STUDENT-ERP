/**
 * admin-dashboard.js — Admin dashboard data & charts
 */

let charts = {};
let studentsPage = 1;

document.addEventListener('DOMContentLoaded', () => {
  loadDashboardStats();
  loadStudentsTable();
  loadUpcomingExams();
});

// ─── Stats ───────────────────────────────────────────────
async function loadDashboardStats() {
  const res = await AnalyticsAPI.dashboard();
  if (!res?.ok) { Toast.error('Failed to load dashboard stats'); return; }

  const d = res.data.data;
  const statsConfig = [
    { icon: '🎓', label: 'Total Students', value: d.totalStudents, trend: '+12%', trendDir: 'up',   color: 'rgba(99,102,241,0.15)',  iconColor: '#6366f1' },
    { icon: '👩‍🏫', label: 'Teachers',      value: d.totalTeachers, trend: '+3%',  trendDir: 'up',   color: 'rgba(139,92,246,0.15)', iconColor: '#8b5cf6' },
    { icon: '📚', label: 'Courses',        value: d.totalCourses,  trend: 'Stable',trendDir: 'up',  color: 'rgba(6,182,212,0.15)',   iconColor: '#06b6d4' },
    { icon: '📅', label: 'Today Present',  value: d.todayAttendance, trend: 'Today', trendDir:'up', color: 'rgba(16,185,129,0.15)',  iconColor: '#10b981' },
    { icon: '⚠️', label: 'Pending Fees',   value: d.pendingFees,   trend: 'Unpaid', trendDir:'down',color: 'rgba(245,158,11,0.15)', iconColor: '#f59e0b' },
    { icon: '💰', label: 'Revenue (Month)', value: formatCurrency(d.revenueThisMonth), trend: '+8%', trendDir:'up', color: 'rgba(16,185,129,0.15)', iconColor: '#10b981', isText: true },
  ];

  const grid = document.getElementById('statsGrid');
  grid.innerHTML = statsConfig.map((s, i) => `
    <div class="stat-card" style="animation-delay:${0.05 * i}s">
      <div class="sc-top">
        <div class="sc-icon" style="background:${s.color}; font-size:1.3rem">${s.icon}</div>
        <div class="sc-trend ${s.trendDir}">
          ${s.trendDir === 'up' ? '↑' : '↓'} ${s.trend}
        </div>
      </div>
      <div class="sc-value" id="stat_${i}">${s.isText ? s.value : '—'}</div>
      <div class="sc-label">${s.label}</div>
    </div>
  `).join('');

  // Animate counters
  statsConfig.forEach((s, i) => {
    if (!s.isText) {
      const el = document.getElementById(`stat_${i}`);
      if (el) animateCounter(el, Number(s.value) || 0);
    }
  });

  // Load at-risk students
  if (d.atRiskStudents?.length) renderAtRiskList(d.atRiskStudents);
  else document.getElementById('atRiskList').innerHTML = '<p style="color:var(--text-secondary);font-size:0.85rem;padding:16px 0">No high-risk students detected ✓</p>';

  // Render charts after data loads
  renderCharts(d);
}

// ─── Charts ──────────────────────────────────────────────
function renderCharts(d) {
  renderEnrollmentChart(d.enrollmentTrend || []);
  renderDeptChart(d.enrollmentByDept || []);
  renderFeeChart(d.feeCollectionSummary || []);
  renderPaymentStatusChart(d.feeCollectionSummary || []);
}

function renderEnrollmentChart(trend) {
  const ctx = document.getElementById('enrollmentChart');
  if (!ctx) return;
  if (charts.enrollment) charts.enrollment.destroy();
  const c = getChartDefaults();
  charts.enrollment = new Chart(ctx, {
    type: 'line',
    data: {
      labels: trend.map(t => t.month),
      datasets: [{
        label: 'Enrollments',
        data: trend.map(t => t.enrollments),
        borderColor: c.primaryColor,
        backgroundColor: 'rgba(99,102,241,0.1)',
        tension: 0.4, fill: true,
        pointBackgroundColor: c.primaryColor,
        pointRadius: 4, pointHoverRadius: 6,
      }],
    },
    options: { ...buildLineChartOptions('Students'), plugins: { legend: { display: false } } },
  });
}

function renderDeptChart(depts) {
  const ctx = document.getElementById('deptChart');
  if (!ctx) return;
  if (charts.dept) charts.dept.destroy();
  const c = getChartDefaults();
  charts.dept = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: depts.map(d => d.department),
      datasets: [{ data: depts.map(d => d.students), backgroundColor: c.colors, borderWidth: 0 }],
    },
    options: buildDoughnutOptions(),
  });
}

function renderFeeChart(fees) {
  const ctx = document.getElementById('feeChart');
  if (!ctx) return;
  if (charts.fee) charts.fee.destroy();
  const paid    = fees.find(f => f.status === 'paid');
  const pending = fees.find(f => f.status === 'pending');
  const partial = fees.find(f => f.status === 'partial');
  const c = getChartDefaults();
  charts.fee = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Collected', 'Pending', 'Partial'],
      datasets: [{
        data: [paid?.total_amount || 0, pending?.total_amount || 0, partial?.total_amount || 0],
        backgroundColor: ['#10b981', '#f59e0b', '#3b82f6'],
        borderRadius: 6, borderSkipped: false,
      }],
    },
    options: { ...buildBarChartOptions(), plugins: { legend: { display: false } } },
  });
}

function renderPaymentStatusChart(fees) {
  const ctx = document.getElementById('paymentStatusChart');
  if (!ctx) return;
  if (charts.paymentStatus) charts.paymentStatus.destroy();
  const c = getChartDefaults();
  charts.paymentStatus = new Chart(ctx, {
    type: 'pie',
    data: {
      labels: fees.map(f => f.status),
      datasets: [{ data: fees.map(f => f.count), backgroundColor: c.colors, borderWidth: 0 }],
    },
    options: buildDoughnutOptions(),
  });
}

function refreshCharts() { loadDashboardStats(); }

// ─── At-Risk Students ─────────────────────────────────────
function renderAtRiskList(students) {
  const el = document.getElementById('atRiskList');
  if (!el) return;
  el.innerHTML = students.slice(0, 5).map(s => `
    <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid var(--border-subtle)">
      <div style="display:flex;align-items:center;gap:10px">
        ${createAvatar(s.name)}
        <div>
          <div style="font-size:0.85rem;font-weight:500">${s.name}</div>
          <div style="font-size:0.72rem;color:var(--text-tertiary)">${s.student_id} · Att: ${s.attendance_percent}%</div>
        </div>
      </div>
      <span class="badge ${s.risk_level === 'high' ? 'badge-danger' : 'badge-warning'}">${s.risk_level} risk</span>
    </div>
  `).join('');
}

// ─── Upcoming Exams ───────────────────────────────────────
async function loadUpcomingExams() {
  const res = await ExamsAPI.list({ semester: 'Fall' });
  const el  = document.getElementById('upcomingExamsList');
  if (!el) return;
  if (!res?.ok || !res.data?.data?.length) {
    el.innerHTML = '<p style="color:var(--text-secondary);font-size:0.85rem;padding:16px 0">No upcoming exams scheduled</p>';
    return;
  }
  const exams = res.data.data.slice(0, 4);
  el.innerHTML = exams.map(e => `
    <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid var(--border-subtle)">
      <div>
        <div style="font-size:0.85rem;font-weight:500">${e.exam_name || e.name}</div>
        <div style="font-size:0.72rem;color:var(--text-tertiary)">${e.subject_name} · ${formatDate(e.exam_date)}</div>
      </div>
      <span class="badge badge-info">${e.exam_type}</span>
    </div>
  `).join('');
}

// ─── Students Table ───────────────────────────────────────
let searchTimeout;
function searchStudents(q) {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => { studentsPage = 1; loadStudentsTable(q); }, 350);
}

async function loadStudentsTable(search = '') {
  const tbody = document.getElementById('studentsTableBody');
  if (!tbody) return;
  tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:32px;color:var(--text-tertiary)">
    <svg style="width:20px;height:20px;animation:spin 0.8s linear infinite" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" stroke-dasharray="60" stroke-dashoffset="30"/></svg>
  </td></tr>`;

  const res = await StudentsAPI.list({ page: studentsPage, limit: 10, search });
  if (!res?.ok) {
    tbody.innerHTML = renderEmptyRow(7, 'Failed to load students');
    return;
  }

  const students = res.data?.data || [];
  if (!students.length) {
    tbody.innerHTML = renderEmptyRow(7, 'No students found');
    return;
  }

  tbody.innerHTML = students.map(s => `
    <tr>
      <td>
        <div style="display:flex;align-items:center;gap:10px">
          ${createAvatar(`${s.firstName} ${s.lastName}`)}
          <div>
            <div style="font-weight:500">${s.firstName} ${s.lastName}</div>
            <div style="font-size:0.72rem;color:var(--text-tertiary)">${s.email}</div>
          </div>
        </div>
      </td>
      <td><span style="font-family:monospace;font-size:0.82rem">${s.studentId}</span></td>
      <td>${s.courseName || '—'}</td>
      <td><span class="badge badge-primary">Sem ${s.currentSemester}</span></td>
      <td>${formatDate(s.admissionDate)}</td>
      <td>${createStatusBadge(s.isActive ? 'active' : 'inactive')}</td>
      <td>
        <div style="display:flex;gap:6px">
          <button class="btn btn-secondary btn-sm" onclick="viewStudent(${s.id})">View</button>
          <button class="btn btn-secondary btn-sm" onclick="editStudent(${s.id})">Edit</button>
        </div>
      </td>
    </tr>
  `).join('');
}

function viewStudent(id) { window.location.href = `students.html?id=${id}`; }
function editStudent(id) { window.location.href = `students.html?edit=${id}`; }
