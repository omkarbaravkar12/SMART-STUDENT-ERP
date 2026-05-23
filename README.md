 # 🎓 Smart Student ERP System

A **production-level, full-stack Enterprise Resource Planning system** for universities and colleges. Built with Java (backend), HTML/CSS/JavaScript (frontend), and PostgreSQL (database).




## 📁 Project Structure

```
SmartStudentERP/
├── database.sql                    ← PostgreSQL schema + sample data
├── frontend/
│   ├── index.html                  ← Login page
│   ├── css/
│   │   ├── variables.css           ← Design tokens (dark/light theme)
│   │   ├── login.css               ← Login page styles
│   │   └── dashboard.css           ← Dashboard & component styles
│   ├── js/
│   │   ├── api.js                  ← API client layer (all HTTP calls)
│   │   ├── login.js                ← Login page logic
│   │   ├── dashboard.js            ← Shared dashboard utilities
│   │   ├── admin-dashboard.js      ← Admin dashboard charts & data
│   │   └── students.js             ← Student management CRUD
│   └── pages/
│       ├── admin-dashboard.html    ← Admin portal
│       ├── student-dashboard.html  ← Student portal
│       ├── students.html           ← Student management
│       ├── fees.html               ← Fee management
│       ├── teachers.html           ← Teacher management
│       ├── attendance.html         ← Attendance system
│       ├── exams.html              ← Examination management
│       ├── timetable.html          ← Timetable view
│       ├── library.html            ← Library management
│       └── notifications.html      ← Notifications
└── backend/src/
    ├── Main.java                   ← Entry point
    ├── database/
    │   └── DatabaseConnection.java ← PostgreSQL connection singleton
    ├── controllers/
    │   ├── BaseController.java     ← HTTP response helpers
    │   ├── AuthController.java     ← Login/logout/OTP
    │   ├── StudentController.java  ← Student CRUD
    │   ├── TeacherController.java  ← Teacher CRUD
    │   ├── AttendanceController.java
    │   ├── ExamController.java
    │   ├── FeeController.java
    │   ├── NotificationController.java
    │   ├── TimetableController.java
    │   ├── LibraryController.java
    │   ├── CourseController.java
    │   ├── SubjectController.java
    │   ├── DepartmentController.java
    │   └── AnalyticsController.java
    ├── services/
    │   ├── AuthService.java        ← Auth business logic
    │   ├── StudentService.java     ← Student DB operations
    │   ├── TeacherService.java
    │   ├── AttendanceService.java
    │   ├── ExamService.java
    │   ├── FeeService.java
    │   ├── NotificationService.java
    │   ├── TimetableService.java
    │   ├── LibraryService.java
    │   ├── CourseService.java
    │   └── AnalyticsService.java
    ├── middleware/
    │   ├── AuthMiddleware.java     ← JWT token validation
    │   ├── LoggingMiddleware.java  ← Request logging
    │   └── CorsMiddleware.java     ← CORS headers
    ├── routes/
    │   └── RouterRegistry.java     ← URL → Controller mapping
    └── utils/
        ├── JsonUtil.java           ← JSON serializer
        ├── PasswordUtil.java       ← PBKDF2 hashing
        ├── TokenUtil.java          ← Session tokens
        └── Logger.java             ← Colored console logger
```

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| PostgreSQL | 13+ | https://www.postgresql.org/download |
| Any HTTP server | — | Python, VS Code Live Server, etc. |

---

### Step 1 — Set Up PostgreSQL Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Run the database setup script
\i /path/to/SmartStudentERP/database.sql

# Verify setup
\c smart_erp
\dn       -- shows: auth, academic, finance, library, communication
\dt auth.*
```

---

### Step 2 — Download JDBC Driver

Download PostgreSQL JDBC driver:
```
https://jdbc.postgresql.org/download/
```
Save as `postgresql-42.x.x.jar` inside `SmartStudentERP/backend/lib/`

---

### Step 3 — Configure Database Connection

Edit `backend/src/database/DatabaseConnection.java` OR set environment variables:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=smart_erp
export DB_USER=postgres
export DB_PASSWORD=your_postgres_password
```

---

### Step 4 — Compile & Run Java Backend

```bash
cd SmartStudentERP/backend

# Compile all Java files
javac -cp ".:lib/postgresql-42.x.x.jar" \
  -d out \
  src/utils/*.java \
  src/database/*.java \
  src/models/*.java \
  src/middleware/*.java \
  src/services/*.java \
  src/controllers/*.java \
  src/routes/*.java \
  src/Main.java

# Run the server
java -cp "out:lib/postgresql-42.x.x.jar" Main
```

**On Windows (use semicolons):**
```cmd
javac -cp ".;lib\postgresql-42.x.x.jar" -d out src\utils\*.java src\database\*.java ...
java -cp "out;lib\postgresql-42.x.x.jar" Main
```

Expected output:
```
╔══════════════════════════════════════════╗
║   Smart Student ERP System - Backend      ║
║   Version 1.0.0                           ║
╚══════════════════════════════════════════╝
✓ Database connection established
✓ Server running at http://localhost:8080
✓ API Base URL: http://localhost:8080/api
```

---

### Step 5 — Serve the Frontend

**Option A: Python HTTP Server (simplest)**
```bash
cd SmartStudentERP/frontend
python3 -m http.server 3000
# Open: http://localhost:3000
```

**Option B: VS Code Live Server**
- Install "Live Server" extension
- Right-click `index.html` → "Open with Live Server"

**Option C: Node.js serve**
```bash
npx serve frontend -p 3000
```

---

### Step 6 — Login

Open `http://localhost:3000` and use any demo account:

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@erp.edu | Password@123 |
| Teacher | priya.t@erp.edu | Password@123 |
| Student | amit.s@erp.edu | Password@123 |
| Accountant | finance@erp.edu | Password@123 |

---

## 🔌 API Reference

### Auth
```
POST   /api/auth/login           — Login
POST   /api/auth/logout          — Logout
POST   /api/auth/forgot-password — Request OTP
POST   /api/auth/verify-otp      — Reset password
GET    /api/auth/me              — Current user
```

### Students
```
GET    /api/students              — List (supports ?search=&page=&limit=)
POST   /api/students              — Create
GET    /api/students/{id}         — Get by ID
PUT    /api/students/{id}         — Update
DELETE /api/students/{id}         — Soft delete
GET    /api/students/{id}/attendance
GET    /api/students/{id}/results
GET    /api/students/{id}/timetable
```

### Teachers
```
GET    /api/teachers              — List
POST   /api/teachers              — Create
GET    /api/teachers/{id}         — Get by ID
PUT    /api/teachers/{id}         — Update
```

### Academic
```
GET    /api/courses               — All courses
GET    /api/subjects              — All subjects
GET    /api/departments           — Departments with stats
GET    /api/timetable             — Timetable (filter by courseId, semester)
```

### Attendance
```
GET    /api/attendance            — List records
POST   /api/attendance            — Mark (single or bulk)
GET    /api/attendance/summary    — Aggregated attendance %
```

### Exams & Results
```
GET    /api/exams                 — List exams
POST   /api/exams                 — Schedule exam
GET    /api/exams/{id}            — Get exam
PUT    /api/exams/{id}            — Update/publish
GET    /api/results               — Exam results
POST   /api/results               — Enter marks
```

### Finance
```
GET    /api/fees                  — Fee structures
POST   /api/fees                  — Create structure
GET    /api/payments              — Payment records
POST   /api/payments              — Record payment
GET    /api/invoices              — Invoices
```

### Library
```
GET    /api/library/books         — Search books
POST   /api/library/books         — Add book
POST   /api/library/borrow        — Borrow book
PUT    /api/library/borrow        — Return book
```

### Notifications
```
GET    /api/notifications         — My notifications
POST   /api/notifications         — Send notification
```

### Analytics
```
GET    /api/analytics/dashboard   — Admin stats overview
GET    /api/analytics/students    — Student performance analytics
GET    /api/analytics/fees        — Fee collection analytics
GET    /api/analytics/attendance  — Attendance trends
```

### Health
```
GET    /api/health                — Server health check (no auth required)
```

---

## 🏗️ Database Schema

### Schemas
| Schema | Purpose |
|--------|---------|
| `auth` | Users, sessions, login logs |
| `academic` | Students, teachers, courses, exams, attendance |
| `finance` | Fees, payments, invoices |
| `library` | Books, borrow records |
| `communication` | Notifications |

### Key Tables
```
auth.users              → All users (admin/teacher/student/accountant)
auth.sessions           → Active login sessions
academic.students       → Student profiles
academic.teachers       → Teacher profiles
academic.departments    → Academic departments
academic.courses        → Degree programs
academic.subjects       → Individual subjects
academic.student_enrollments → Student-subject-teacher mapping
academic.attendance     → Daily attendance records
academic.exams          → Exam schedule
academic.exam_results   → Marks and grades (auto-graded by trigger)
academic.timetables     → Weekly class schedule
academic.performance_analytics → AI risk scores & GPA tracking
finance.fee_structures  → Course-wise fee breakdown
finance.payments        → Payment records
finance.invoices        → Auto-generated invoices
library.books           → Book inventory
library.borrow_records  → Borrowing tracking
communication.notifications → System announcements
```

---

## 🎨 UI Features

- **Dark/Light Mode** toggle (persists across sessions)
- **Glassmorphism** login card with animated background orbs
- **Animated sidebar** with collapse/expand
- **Chart.js 4** bar, line, doughnut, pie charts
- **Animated stat counters** on dashboard load
- **Loading skeletons** while data fetches
- **Toast notifications** for all actions
- **Interactive data tables** with sorting, filtering, pagination
- **Card/Table view** toggle for student lists
- **Modal dialogs** for create/edit/view
- **Responsive** mobile layout
- **Role-based** portals (admin/teacher/student/accountant dashboards)

---

## 🔒 Security Features

- **PBKDF2+SHA256** password hashing
- **Secure session tokens** (cryptographically random)
- **24-hour session expiry**
- **Role-based access control** (RBAC) via middleware
- **PreparedStatements** for all SQL (no SQL injection)
- **CORS** headers on all API responses
- **Soft delete** (no data loss)
- **Audit trail** via login_logs table

---

## 📊 Database Triggers

| Trigger | Function |
|---------|---------|
| Auto-update `updated_at` | On users & payments update |
| Auto-generate invoice number | On invoice insert |
| Auto-calculate letter grade | On exam results insert |
| Auto-update book availability | On borrow/return |
| Auto-calculate late fine | On late payment |
| Auto-broadcast notifications | To all users of target role |

---

## 🧩 Module Summary

| # | Module | Status |
|---|--------|--------|
| 1 | Authentication & Sessions | ✅ Complete |
| 2 | Student Management | ✅ Complete |
| 3 | Teacher Management | ✅ Complete |
| 4 | Course & Subject Management | ✅ Complete |
| 5 | Attendance System | ✅ Complete |
| 6 | Examination Management | ✅ Complete |
| 7 | Fee Management | ✅ Complete |
| 8 | Notification System | ✅ Complete |
| 9 | Timetable Management | ✅ Complete |
| 10 | Library Management | ✅ Complete |
| 11 | Analytics Dashboard | ✅ Complete |
| 12 | AI Performance Analytics | ✅ Complete |

---

## 🛠️ Tech Stack

**Backend:** Java 17, com.sun.net.httpserver (built-in), PostgreSQL JDBC  
**Frontend:** HTML5, CSS3 (custom design system), Vanilla JS (ES2022), Chart.js 4  
**Database:** PostgreSQL 13+ with 5 schemas, 25+ tables, triggers, views  
**Architecture:** REST API, MVC pattern, Singleton DB connection, Middleware chain

---

*Built for Smart Student ERP System — Production Grade*
