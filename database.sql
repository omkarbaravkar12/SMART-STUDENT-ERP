-- ============================================================
-- Smart Student ERP System - PostgreSQL Database
-- File: database.sql
-- Compatible with: PostgreSQL 13+
-- Run: psql -U postgres -f database.sql
-- ============================================================

\echo '===== Smart Student ERP System - Database Setup ====='

-- Drop and recreate database
--DROP DATABASE IF EXISTS smart_erp;
--CREATE DATABASE smart_erp
  --  WITH ENCODING = 'UTF8'
    --LC_COLLATE = 'en_US.UTF-8'
    --LC_CTYPE = 'en_US.UTF-8'
    --TEMPLATE = template0;

\c smart_erp;

\echo '--- Creating Extensions ---'
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- SCHEMA: auth
-- ============================================================
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS academic;
CREATE SCHEMA IF NOT EXISTS finance;
CREATE SCHEMA IF NOT EXISTS library;
CREATE SCHEMA IF NOT EXISTS communication;

-- ============================================================
-- ENUM TYPES
-- ============================================================
CREATE TYPE user_role AS ENUM ('admin', 'teacher', 'student', 'accountant', 'parent');
CREATE TYPE gender_type AS ENUM ('male', 'female', 'other');
CREATE TYPE attendance_status AS ENUM ('present', 'absent', 'late', 'excused');
CREATE TYPE payment_status AS ENUM ('pending', 'paid', 'overdue', 'waived', 'partial');
CREATE TYPE exam_type AS ENUM ('midterm', 'final', 'quiz', 'assignment', 'lab', 'practical');
CREATE TYPE notification_type AS ENUM ('announcement', 'exam_alert', 'fee_reminder', 'attendance_alert', 'system', 'event');
CREATE TYPE book_status AS ENUM ('available', 'borrowed', 'reserved', 'damaged', 'lost');
CREATE TYPE day_of_week AS ENUM ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday');
CREATE TYPE semester_type AS ENUM ('Fall', 'Spring', 'Summer');
CREATE TYPE grade_letter AS ENUM ('A+','A','A-','B+','B','B-','C+','C','C-','D+','D','F');

-- ============================================================
-- TABLE: auth.roles
-- ============================================================
CREATE TABLE auth.roles (
    id          SERIAL PRIMARY KEY,
    name        user_role UNIQUE NOT NULL,
    description TEXT,
    permissions JSONB DEFAULT '{}',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: auth.users
-- ============================================================
CREATE TABLE auth.users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            user_role NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    profile_photo   VARCHAR(500),
    is_active       BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    otp_code        VARCHAR(10),
    otp_expires_at  TIMESTAMPTZ,
    last_login      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: auth.login_logs
-- ============================================================
CREATE TABLE auth.login_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    ip_address  INET,
    user_agent  TEXT,
    device_info JSONB DEFAULT '{}',
    status      VARCHAR(20) DEFAULT 'success',
    logged_at   TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: auth.sessions
-- ============================================================
CREATE TABLE auth.sessions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    token       VARCHAR(500) UNIQUE NOT NULL,
    ip_address  INET,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.departments
-- ============================================================
CREATE TABLE academic.departments (
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(10) UNIQUE NOT NULL,
    name        VARCHAR(150) NOT NULL,
    head_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    description TEXT,
    established_year INT,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.courses
-- ============================================================
CREATE TABLE academic.courses (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(20) UNIQUE NOT NULL,
    name            VARCHAR(200) NOT NULL,
    department_id   INT REFERENCES academic.departments(id) ON DELETE SET NULL,
    duration_years  NUMERIC(3,1) NOT NULL DEFAULT 4,
    total_credits   INT NOT NULL DEFAULT 120,
    description     TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.subjects
-- ============================================================
CREATE TABLE academic.subjects (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(20) UNIQUE NOT NULL,
    name            VARCHAR(200) NOT NULL,
    department_id   INT REFERENCES academic.departments(id) ON DELETE SET NULL,
    credits         INT NOT NULL DEFAULT 3,
    theory_hours    INT DEFAULT 3,
    lab_hours       INT DEFAULT 0,
    is_elective     BOOLEAN DEFAULT FALSE,
    description     TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.course_subjects (many-to-many)
-- ============================================================
CREATE TABLE academic.course_subjects (
    id          SERIAL PRIMARY KEY,
    course_id   INT REFERENCES academic.courses(id) ON DELETE CASCADE,
    subject_id  INT REFERENCES academic.subjects(id) ON DELETE CASCADE,
    semester    INT NOT NULL CHECK (semester BETWEEN 1 AND 10),
    is_mandatory BOOLEAN DEFAULT TRUE,
    UNIQUE(course_id, subject_id, semester)
);

-- ============================================================
-- TABLE: academic.teachers
-- ============================================================
CREATE TABLE academic.teachers (
    id              SERIAL PRIMARY KEY,
    user_id         UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    employee_id     VARCHAR(20) UNIQUE NOT NULL,
    department_id   INT REFERENCES academic.departments(id) ON DELETE SET NULL,
    designation     VARCHAR(100),
    qualification   VARCHAR(200),
    specialization  VARCHAR(200),
    joining_date    DATE NOT NULL,
    salary          NUMERIC(12,2),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.teacher_subjects
-- ============================================================
CREATE TABLE academic.teacher_subjects (
    id          SERIAL PRIMARY KEY,
    teacher_id  INT REFERENCES academic.teachers(id) ON DELETE CASCADE,
    subject_id  INT REFERENCES academic.subjects(id) ON DELETE CASCADE,
    academic_year VARCHAR(9) NOT NULL,
    semester    semester_type NOT NULL,
    is_active   BOOLEAN DEFAULT TRUE,
    UNIQUE(teacher_id, subject_id, academic_year, semester)
);

-- ============================================================
-- TABLE: academic.students
-- ============================================================
CREATE TABLE academic.students (
    id              SERIAL PRIMARY KEY,
    user_id         UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    student_id      VARCHAR(20) UNIQUE NOT NULL,
    course_id       INT REFERENCES academic.courses(id) ON DELETE SET NULL,
    department_id   INT REFERENCES academic.departments(id) ON DELETE SET NULL,
    current_semester INT NOT NULL DEFAULT 1,
    admission_date  DATE NOT NULL,
    date_of_birth   DATE,
    gender          gender_type,
    blood_group     VARCHAR(5),
    address         TEXT,
    city            VARCHAR(100),
    state           VARCHAR(100),
    country         VARCHAR(100) DEFAULT 'India',
    postal_code     VARCHAR(10),
    emergency_contact VARCHAR(20),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.parents
-- ============================================================
CREATE TABLE academic.parents (
    id              SERIAL PRIMARY KEY,
    student_id      INT REFERENCES academic.students(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    father_name     VARCHAR(150),
    mother_name     VARCHAR(150),
    father_phone    VARCHAR(20),
    mother_phone    VARCHAR(20),
    father_email    VARCHAR(255),
    mother_email    VARCHAR(255),
    father_occupation VARCHAR(150),
    mother_occupation VARCHAR(150),
    annual_income   NUMERIC(14,2),
    address         TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.student_enrollments
-- ============================================================
CREATE TABLE academic.student_enrollments (
    id              SERIAL PRIMARY KEY,
    student_id      INT REFERENCES academic.students(id) ON DELETE CASCADE,
    subject_id      INT REFERENCES academic.subjects(id) ON DELETE CASCADE,
    teacher_id      INT REFERENCES academic.teachers(id) ON DELETE SET NULL,
    academic_year   VARCHAR(9) NOT NULL,
    semester        semester_type NOT NULL,
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    is_active       BOOLEAN DEFAULT TRUE,
    UNIQUE(student_id, subject_id, academic_year, semester)
);

-- ============================================================
-- TABLE: academic.attendance
-- ============================================================
CREATE TABLE academic.attendance (
    id              BIGSERIAL PRIMARY KEY,
    student_id      INT REFERENCES academic.students(id) ON DELETE CASCADE,
    subject_id      INT REFERENCES academic.subjects(id) ON DELETE CASCADE,
    teacher_id      INT REFERENCES academic.teachers(id) ON DELETE SET NULL,
    date            DATE NOT NULL DEFAULT CURRENT_DATE,
    status          attendance_status NOT NULL DEFAULT 'present',
    remarks         TEXT,
    qr_token        VARCHAR(100),
    marked_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(student_id, subject_id, date)
);

-- ============================================================
-- TABLE: academic.exams
-- ============================================================
CREATE TABLE academic.exams (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    subject_id      INT REFERENCES academic.subjects(id) ON DELETE CASCADE,
    exam_type       exam_type NOT NULL,
    academic_year   VARCHAR(9) NOT NULL,
    semester        semester_type NOT NULL,
    exam_date       DATE NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    room            VARCHAR(50),
    total_marks     INT NOT NULL DEFAULT 100,
    passing_marks   INT NOT NULL DEFAULT 40,
    instructions    TEXT,
    is_published    BOOLEAN DEFAULT FALSE,
    created_by      UUID REFERENCES auth.users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: academic.exam_results
-- ============================================================
CREATE TABLE academic.exam_results (
    id              BIGSERIAL PRIMARY KEY,
    exam_id         INT REFERENCES academic.exams(id) ON DELETE CASCADE,
    student_id      INT REFERENCES academic.students(id) ON DELETE CASCADE,
    marks_obtained  NUMERIC(6,2),
    grade           grade_letter,
    grade_points    NUMERIC(4,2),
    is_absent       BOOLEAN DEFAULT FALSE,
    remarks         TEXT,
    entered_by      UUID REFERENCES auth.users(id),
    entered_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(exam_id, student_id)
);

-- ============================================================
-- TABLE: academic.timetables
-- ============================================================
CREATE TABLE academic.timetables (
    id              SERIAL PRIMARY KEY,
    course_id       INT REFERENCES academic.courses(id) ON DELETE CASCADE,
    subject_id      INT REFERENCES academic.subjects(id) ON DELETE CASCADE,
    teacher_id      INT REFERENCES academic.teachers(id) ON DELETE SET NULL,
    day_of_week     day_of_week NOT NULL,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    room            VARCHAR(50),
    semester        INT NOT NULL,
    academic_year   VARCHAR(9) NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    UNIQUE(course_id, semester, day_of_week, start_time, academic_year)
);

-- ============================================================
-- TABLE: finance.fee_structures
-- ============================================================
CREATE TABLE finance.fee_structures (
    id              SERIAL PRIMARY KEY,
    course_id       INT REFERENCES academic.courses(id) ON DELETE CASCADE,
    academic_year   VARCHAR(9) NOT NULL,
    semester        INT NOT NULL,
    tuition_fee     NUMERIC(12,2) NOT NULL DEFAULT 0,
    exam_fee        NUMERIC(12,2) NOT NULL DEFAULT 0,
    library_fee     NUMERIC(12,2) NOT NULL DEFAULT 0,
    lab_fee         NUMERIC(12,2) NOT NULL DEFAULT 0,
    sports_fee      NUMERIC(12,2) NOT NULL DEFAULT 0,
    misc_fee        NUMERIC(12,2) NOT NULL DEFAULT 0,
    due_date        DATE NOT NULL,
    late_fine_per_day NUMERIC(8,2) DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(course_id, academic_year, semester)
);

-- ============================================================
-- TABLE: finance.payments
-- ============================================================
CREATE TABLE finance.payments (
    id              BIGSERIAL PRIMARY KEY,
    payment_ref     VARCHAR(30) UNIQUE NOT NULL DEFAULT 'PAY-' || EXTRACT(EPOCH FROM NOW())::BIGINT,
    student_id      INT REFERENCES academic.students(id) ON DELETE CASCADE,
    fee_structure_id INT REFERENCES finance.fee_structures(id) ON DELETE SET NULL,
    amount_due      NUMERIC(12,2) NOT NULL,
    amount_paid     NUMERIC(12,2) NOT NULL DEFAULT 0,
    late_fine       NUMERIC(10,2) DEFAULT 0,
    scholarship_discount NUMERIC(10,2) DEFAULT 0,
    payment_status  payment_status NOT NULL DEFAULT 'pending',
    payment_method  VARCHAR(50),
    transaction_id  VARCHAR(100),
    payment_date    DATE,
    due_date        DATE NOT NULL,
    notes           TEXT,
    processed_by    UUID REFERENCES auth.users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: finance.invoices
-- ============================================================
CREATE TABLE finance.invoices (
    id              BIGSERIAL PRIMARY KEY,
    invoice_number  VARCHAR(30) UNIQUE NOT NULL,
    payment_id      BIGINT REFERENCES finance.payments(id) ON DELETE CASCADE,
    student_id      INT REFERENCES academic.students(id) ON DELETE CASCADE,
    invoice_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    total_amount    NUMERIC(12,2) NOT NULL,
    pdf_path        VARCHAR(500),
    generated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: finance.scholarships
-- ============================================================
CREATE TABLE finance.scholarships (
    id              SERIAL PRIMARY KEY,
    student_id      INT REFERENCES academic.students(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    discount_percent NUMERIC(5,2),
    discount_amount NUMERIC(12,2),
    academic_year   VARCHAR(9),
    description     TEXT,
    awarded_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: library.books
-- ============================================================
CREATE TABLE library.books (
    id              SERIAL PRIMARY KEY,
    isbn            VARCHAR(20) UNIQUE,
    title           VARCHAR(300) NOT NULL,
    author          VARCHAR(300) NOT NULL,
    publisher       VARCHAR(200),
    published_year  INT,
    category        VARCHAR(100),
    subject_id      INT REFERENCES academic.subjects(id) ON DELETE SET NULL,
    total_copies    INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    location_shelf  VARCHAR(50),
    status          book_status DEFAULT 'available',
    cover_image     VARCHAR(500),
    description     TEXT,
    added_at        TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- TABLE: library.borrow_records
-- ============================================================
CREATE TABLE library.borrow_records (
    id              BIGSERIAL PRIMARY KEY,
    book_id         INT REFERENCES library.books(id) ON DELETE CASCADE,
    user_id         UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    borrowed_at     DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE NOT NULL,
    returned_at     DATE,
    late_fine       NUMERIC(8,2) DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'borrowed',
    notes           TEXT
);

-- ============================================================
-- TABLE: communication.notifications
-- ============================================================
CREATE TABLE communication.notifications (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(300) NOT NULL,
    message         TEXT NOT NULL,
    type            notification_type NOT NULL DEFAULT 'announcement',
    target_role     user_role,
    target_user_id  UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    is_broadcast    BOOLEAN DEFAULT FALSE,
    created_by      UUID REFERENCES auth.users(id),
    send_email      BOOLEAN DEFAULT FALSE,
    email_sent      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    expires_at      TIMESTAMPTZ
);

-- ============================================================
-- TABLE: communication.user_notifications
-- ============================================================
CREATE TABLE communication.user_notifications (
    id                  BIGSERIAL PRIMARY KEY,
    notification_id     BIGINT REFERENCES communication.notifications(id) ON DELETE CASCADE,
    user_id             UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    is_read             BOOLEAN DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(notification_id, user_id)
);

-- ============================================================
-- TABLE: academic.performance_analytics (AI/ML data store)
-- ============================================================
CREATE TABLE academic.performance_analytics (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          INT REFERENCES academic.students(id) ON DELETE CASCADE,
    academic_year       VARCHAR(9) NOT NULL,
    semester            semester_type NOT NULL,
    attendance_percent  NUMERIC(5,2),
    avg_marks           NUMERIC(6,2),
    gpa                 NUMERIC(4,2),
    risk_score          NUMERIC(5,2),   -- 0-100, higher = more at risk
    risk_level          VARCHAR(20),    -- low, medium, high
    predicted_grade     grade_letter,
    performance_trend   VARCHAR(20),    -- improving, declining, stable
    behavior_score      NUMERIC(5,2),
    rank_in_class       INT,
    calculated_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(student_id, academic_year, semester)
);

-- ============================================================
-- TABLE: academic.teacher_attendance
-- ============================================================
CREATE TABLE academic.teacher_attendance (
    id          BIGSERIAL PRIMARY KEY,
    teacher_id  INT REFERENCES academic.teachers(id) ON DELETE CASCADE,
    date        DATE NOT NULL DEFAULT CURRENT_DATE,
    status      attendance_status NOT NULL DEFAULT 'present',
    check_in    TIME,
    check_out   TIME,
    remarks     TEXT,
    UNIQUE(teacher_id, date)
);

-- ============================================================
-- INDEXES
-- ============================================================
\echo '--- Creating Indexes ---'

CREATE INDEX idx_users_email ON auth.users(email);
CREATE INDEX idx_users_role ON auth.users(role);
CREATE INDEX idx_login_logs_user ON auth.login_logs(user_id, logged_at DESC);
CREATE INDEX idx_sessions_token ON auth.sessions(token);
CREATE INDEX idx_sessions_user ON auth.sessions(user_id);

CREATE INDEX idx_students_student_id ON academic.students(student_id);
CREATE INDEX idx_students_course ON academic.students(course_id);
CREATE INDEX idx_students_user ON academic.students(user_id);

CREATE INDEX idx_teachers_employee_id ON academic.teachers(employee_id);
CREATE INDEX idx_teachers_dept ON academic.teachers(department_id);

CREATE INDEX idx_attendance_student_date ON academic.attendance(student_id, date);
CREATE INDEX idx_attendance_subject_date ON academic.attendance(subject_id, date);
CREATE INDEX idx_attendance_date ON academic.attendance(date);

CREATE INDEX idx_exam_results_student ON academic.exam_results(student_id);
CREATE INDEX idx_exam_results_exam ON academic.exam_results(exam_id);

CREATE INDEX idx_payments_student ON finance.payments(student_id);
CREATE INDEX idx_payments_status ON finance.payments(payment_status);
CREATE INDEX idx_payments_due ON finance.payments(due_date);

CREATE INDEX idx_notifications_user ON communication.user_notifications(user_id, is_read);
CREATE INDEX idx_notifications_created ON communication.notifications(created_at DESC);

CREATE INDEX idx_borrow_user ON library.borrow_records(user_id, status);
CREATE INDEX idx_borrow_due ON library.borrow_records(due_date);

CREATE INDEX idx_timetable_course ON academic.timetables(course_id, academic_year);
CREATE INDEX idx_perf_student ON academic.performance_analytics(student_id, academic_year);

-- ============================================================
-- FUNCTIONS & TRIGGERS
-- ============================================================
\echo '--- Creating Functions and Triggers ---'

-- Auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON finance.payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Auto-generate invoice number
CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.invoice_number = 'INV-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(NEW.id::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_invoice_number
    BEFORE INSERT ON finance.invoices
    FOR EACH ROW EXECUTE FUNCTION generate_invoice_number();

-- Calculate grade letter from marks
CREATE OR REPLACE FUNCTION calculate_grade(marks NUMERIC, total NUMERIC)
RETURNS grade_letter AS $$
DECLARE
    percentage NUMERIC;
BEGIN
    IF total = 0 THEN RETURN 'F'; END IF;
    percentage := (marks / total) * 100;
    RETURN CASE
        WHEN percentage >= 97 THEN 'A+'::grade_letter
        WHEN percentage >= 93 THEN 'A'::grade_letter
        WHEN percentage >= 90 THEN 'A-'::grade_letter
        WHEN percentage >= 87 THEN 'B+'::grade_letter
        WHEN percentage >= 83 THEN 'B'::grade_letter
        WHEN percentage >= 80 THEN 'B-'::grade_letter
        WHEN percentage >= 77 THEN 'C+'::grade_letter
        WHEN percentage >= 73 THEN 'C'::grade_letter
        WHEN percentage >= 70 THEN 'C-'::grade_letter
        WHEN percentage >= 67 THEN 'D+'::grade_letter
        WHEN percentage >= 60 THEN 'D'::grade_letter
        ELSE 'F'::grade_letter
    END;
END;
$$ LANGUAGE plpgsql;

-- Auto-set grade on exam result insert
CREATE OR REPLACE FUNCTION auto_set_grade()
RETURNS TRIGGER AS $$
DECLARE
    v_total INT;
BEGIN
    SELECT total_marks INTO v_total FROM academic.exams WHERE id = NEW.exam_id;
    IF NEW.marks_obtained IS NOT NULL AND NOT NEW.is_absent THEN
        NEW.grade := calculate_grade(NEW.marks_obtained, v_total);
        NEW.grade_points := CASE NEW.grade
            WHEN 'A+' THEN 4.0 WHEN 'A' THEN 4.0 WHEN 'A-' THEN 3.7
            WHEN 'B+' THEN 3.3 WHEN 'B' THEN 3.0 WHEN 'B-' THEN 2.7
            WHEN 'C+' THEN 2.3 WHEN 'C' THEN 2.0 WHEN 'C-' THEN 1.7
            WHEN 'D+' THEN 1.3 WHEN 'D' THEN 1.0 ELSE 0.0
        END;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auto_grade
    BEFORE INSERT OR UPDATE ON academic.exam_results
    FOR EACH ROW EXECUTE FUNCTION auto_set_grade();

-- Update book available copies on borrow
CREATE OR REPLACE FUNCTION update_book_availability()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.status = 'borrowed' THEN
        UPDATE library.books SET available_copies = available_copies - 1
        WHERE id = NEW.book_id AND available_copies > 0;
    ELSIF TG_OP = 'UPDATE' AND OLD.status = 'borrowed' AND NEW.returned_at IS NOT NULL THEN
        UPDATE library.books SET available_copies = available_copies + 1
        WHERE id = NEW.book_id;
        NEW.status = 'returned';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_book_availability
    BEFORE INSERT OR UPDATE ON library.borrow_records
    FOR EACH ROW EXECUTE FUNCTION update_book_availability();

-- Late fine calculation for payments
CREATE OR REPLACE FUNCTION calculate_late_fine()
RETURNS TRIGGER AS $$
DECLARE
    v_fine_per_day NUMERIC;
    v_days_late INT;
BEGIN
    IF NEW.payment_status = 'paid' AND NEW.payment_date > NEW.due_date THEN
        SELECT late_fine_per_day INTO v_fine_per_day
        FROM finance.fee_structures WHERE id = NEW.fee_structure_id;
        v_days_late := NEW.payment_date - NEW.due_date;
        NEW.late_fine := v_fine_per_day * v_days_late;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_late_fine
    BEFORE UPDATE ON finance.payments
    FOR EACH ROW EXECUTE FUNCTION calculate_late_fine();

-- Broadcast notification to all role users
CREATE OR REPLACE FUNCTION broadcast_notification()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_broadcast = TRUE THEN
        INSERT INTO communication.user_notifications (notification_id, user_id)
        SELECT NEW.id, u.id FROM auth.users u
        WHERE (NEW.target_role IS NULL OR u.role = NEW.target_role)
          AND u.is_active = TRUE
        ON CONFLICT DO NOTHING;
    ELSIF NEW.target_user_id IS NOT NULL THEN
        INSERT INTO communication.user_notifications (notification_id, user_id)
        VALUES (NEW.id, NEW.target_user_id)
        ON CONFLICT DO NOTHING;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_broadcast_notification
    AFTER INSERT ON communication.notifications
    FOR EACH ROW EXECUTE FUNCTION broadcast_notification();

-- ============================================================
-- VIEWS
-- ============================================================
\echo '--- Creating Views ---'

CREATE OR REPLACE VIEW academic.student_overview AS
SELECT
    s.id,
    s.student_id,
    u.first_name || ' ' || u.last_name AS full_name,
    u.email,
    u.phone,
    u.profile_photo,
    c.name AS course_name,
    d.name AS department_name,
    s.current_semester,
    s.admission_date,
    s.gender,
    s.is_active
FROM academic.students s
JOIN auth.users u ON s.user_id = u.id
LEFT JOIN academic.courses c ON s.course_id = c.id
LEFT JOIN academic.departments d ON s.department_id = d.id;

CREATE OR REPLACE VIEW academic.teacher_overview AS
SELECT
    t.id,
    t.employee_id,
    u.first_name || ' ' || u.last_name AS full_name,
    u.email,
    u.phone,
    t.designation,
    t.qualification,
    d.name AS department_name,
    t.joining_date,
    t.is_active
FROM academic.teachers t
JOIN auth.users u ON t.user_id = u.id
LEFT JOIN academic.departments d ON t.department_id = d.id;

CREATE OR REPLACE VIEW academic.attendance_summary AS
SELECT
    s.id AS student_id,
    s.student_id AS roll_number,
    u.first_name || ' ' || u.last_name AS student_name,
    sub.code AS subject_code,
    sub.name AS subject_name,
    COUNT(*) AS total_classes,
    COUNT(*) FILTER (WHERE a.status = 'present') AS present_count,
    COUNT(*) FILTER (WHERE a.status = 'absent') AS absent_count,
    ROUND(COUNT(*) FILTER (WHERE a.status IN ('present','late'))::NUMERIC / NULLIF(COUNT(*),0) * 100, 2) AS attendance_percent
FROM academic.attendance a
JOIN academic.students s ON a.student_id = s.id
JOIN auth.users u ON s.user_id = u.id
JOIN academic.subjects sub ON a.subject_id = sub.id
GROUP BY s.id, s.student_id, u.first_name, u.last_name, sub.code, sub.name;

CREATE OR REPLACE VIEW finance.payment_overview AS
SELECT
    p.id,
    p.payment_ref,
    s.student_id,
    u.first_name || ' ' || u.last_name AS student_name,
    c.name AS course_name,
    p.amount_due,
    p.amount_paid,
    p.late_fine,
    p.scholarship_discount,
    p.payment_status,
    p.due_date,
    p.payment_date
FROM finance.payments p
JOIN academic.students s ON p.student_id = s.id
JOIN auth.users u ON s.user_id = u.id
LEFT JOIN academic.courses c ON s.course_id = c.id;

-- ============================================================
-- SAMPLE DATA
-- ============================================================
\echo '--- Inserting Sample Data ---'

-- Roles
INSERT INTO auth.roles (name, description, permissions) VALUES
('admin',     'System Administrator',  '{"all": true}'),
('teacher',   'Teaching Staff',        '{"attendance": true, "marks": true, "timetable": true}'),
('student',   'Enrolled Student',      '{"view_own": true, "attendance_view": true}'),
('accountant','Finance Staff',         '{"fees": true, "invoices": true, "reports": true}'),
('parent',    'Parent/Guardian',       '{"view_child": true}');

-- Users (password: "Password@123" -> bcrypt hash simulation)
INSERT INTO auth.users (id, email, password_hash, role, first_name, last_name, phone, is_active, is_email_verified) VALUES
-- Admins
('a0000000-0000-0000-0000-000000000001', 'admin@erp.edu',      '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'admin',     'Rahul',    'Sharma',    '9800000001', TRUE, TRUE),
-- Teachers
('a0000000-0000-0000-0000-000000000002', 'priya.t@erp.edu',    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'teacher',   'Priya',    'Mehta',     '9800000002', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000003', 'arjun.t@erp.edu',    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'teacher',   'Arjun',    'Patel',     '9800000003', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000004', 'sunita.t@erp.edu',   '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'teacher',   'Sunita',   'Reddy',     '9800000004', TRUE, TRUE),
-- Students
('a0000000-0000-0000-0000-000000000010', 'amit.s@erp.edu',     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'student',   'Amit',     'Kumar',     '9800000010', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000011', 'sneha.s@erp.edu',    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'student',   'Sneha',    'Joshi',     '9800000011', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000012', 'raj.s@erp.edu',      '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'student',   'Raj',      'Singh',     '9800000012', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000013', 'pooja.s@erp.edu',    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'student',   'Pooja',    'Verma',     '9800000013', TRUE, TRUE),
('a0000000-0000-0000-0000-000000000014', 'ravi.s@erp.edu',     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'student',   'Ravi',     'Gupta',     '9800000014', TRUE, TRUE),
-- Accountant
('a0000000-0000-0000-0000-000000000020', 'finance@erp.edu',    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TsuDevGDJaFEP0P5EZgZ8vF6WARO', 'accountant','Meena',    'Iyer',      '9800000020', TRUE, TRUE);

-- Departments
INSERT INTO academic.departments (code, name, head_user_id, description, established_year) VALUES
('CSE',  'Computer Science & Engineering',         'a0000000-0000-0000-0000-000000000002', 'Core CS department', 1998),
('ECE',  'Electronics & Communication Engineering','a0000000-0000-0000-0000-000000000003', 'ECE department',     2002),
('MECH', 'Mechanical Engineering',                 'a0000000-0000-0000-0000-000000000004', 'Mechanical dept',    1996),
('MBA',  'Master of Business Administration',      NULL, 'Management studies', 2005);

-- Courses
INSERT INTO academic.courses (code, name, department_id, duration_years, total_credits) VALUES
('BTech-CSE',  'B.Tech Computer Science',        1, 4.0, 160),
('BTech-ECE',  'B.Tech Electronics',             2, 4.0, 160),
('BTech-MECH', 'B.Tech Mechanical Engineering',  3, 4.0, 160),
('MBA-GEN',    'MBA General Management',         4, 2.0, 80);

-- Subjects
INSERT INTO academic.subjects (code, name, department_id, credits, theory_hours, lab_hours, is_elective) VALUES
('CS101', 'Introduction to Programming',  1, 4, 3, 2, FALSE),
('CS201', 'Data Structures & Algorithms', 1, 4, 3, 2, FALSE),
('CS301', 'Database Management Systems',  1, 3, 3, 2, FALSE),
('CS401', 'Software Engineering',         1, 3, 3, 0, FALSE),
('CS501', 'Machine Learning',             1, 3, 2, 2, TRUE),
('MA101', 'Engineering Mathematics I',    1, 4, 4, 0, FALSE),
('EC101', 'Circuit Theory',               2, 4, 3, 2, FALSE),
('ME101', 'Engineering Mechanics',        3, 4, 3, 2, FALSE),
('MB101', 'Organizational Behavior',      4, 3, 3, 0, FALSE),
('MB201', 'Financial Management',         4, 3, 3, 0, FALSE);

-- Course-Subject mappings
INSERT INTO academic.course_subjects (course_id, subject_id, semester, is_mandatory) VALUES
(1, 1, 1, TRUE), (1, 6, 1, TRUE), (1, 2, 2, TRUE),
(1, 3, 3, TRUE), (1, 4, 4, TRUE), (1, 5, 5, FALSE),
(2, 6, 1, TRUE), (2, 7, 1, TRUE),
(3, 8, 1, TRUE), (3, 6, 1, TRUE),
(4, 9, 1, TRUE), (4, 10, 1, TRUE);

-- Teachers
INSERT INTO academic.teachers (user_id, employee_id, department_id, designation, qualification, specialization, joining_date, salary) VALUES
('a0000000-0000-0000-0000-000000000002', 'EMP001', 1, 'Associate Professor', 'M.Tech Computer Science', 'Machine Learning & AI', '2018-07-15', 85000.00),
('a0000000-0000-0000-0000-000000000003', 'EMP002', 2, 'Assistant Professor',  'M.Tech ECE',              'VLSI Design',          '2020-08-01', 75000.00),
('a0000000-0000-0000-0000-000000000004', 'EMP003', 3, 'Professor',            'Ph.D Mechanical',         'Thermodynamics',       '2010-06-20', 110000.00);

-- Teacher-Subject assignments
INSERT INTO academic.teacher_subjects (teacher_id, subject_id, academic_year, semester) VALUES
(1, 1, '2024-25', 'Fall'), (1, 2, '2024-25', 'Fall'), (1, 5, '2024-25', 'Fall'),
(2, 7, '2024-25', 'Fall'), (2, 6, '2024-25', 'Fall'),
(3, 8, '2024-25', 'Fall');

-- Students
INSERT INTO academic.students (user_id, student_id, course_id, department_id, current_semester, admission_date, date_of_birth, gender, blood_group, address, city, state) VALUES
('a0000000-0000-0000-0000-000000000010', 'STU2024001', 1, 1, 3, '2022-08-01', '2004-03-15', 'male',   'O+',  '12, MG Road',     'Mumbai',   'Maharashtra'),
('a0000000-0000-0000-0000-000000000011', 'STU2024002', 1, 1, 3, '2022-08-01', '2004-07-22', 'female', 'B+',  '45, Anna Nagar',  'Chennai',  'Tamil Nadu'),
('a0000000-0000-0000-0000-000000000012', 'STU2024003', 2, 2, 2, '2023-08-01', '2005-01-10', 'male',   'A+',  '78, Park Street', 'Kolkata',  'West Bengal'),
('a0000000-0000-0000-0000-000000000013', 'STU2024004', 1, 1, 4, '2021-08-01', '2003-11-30', 'female', 'AB+', '3, Jubilee Hills','Hyderabad','Telangana'),
('a0000000-0000-0000-0000-000000000014', 'STU2024005', 3, 3, 1, '2024-08-01', '2006-05-17', 'male',   'B-',  '22, Civil Lines', 'Delhi',    'Delhi');

-- Parents
INSERT INTO academic.parents (student_id, father_name, mother_name, father_phone, mother_phone, father_email, annual_income, address) VALUES
(1, 'Suresh Kumar',   'Lata Kumar',   '9700000001', '9700000002', 'suresh.k@gmail.com', 900000,  '12, MG Road, Mumbai'),
(2, 'Vivek Joshi',    'Anita Joshi',  '9700000003', '9700000004', 'vivek.j@gmail.com',  1200000, '45, Anna Nagar, Chennai'),
(3, 'Deepak Singh',   'Rekha Singh',  '9700000005', '9700000006', 'deepak.s@gmail.com', 750000,  '78, Park Street, Kolkata'),
(4, 'Mohan Verma',    'Sushma Verma', '9700000007', '9700000008', 'mohan.v@gmail.com',  1500000, '3, Jubilee Hills, Hyderabad'),
(5, 'Sandeep Gupta',  'Kamla Gupta',  '9700000009', '9700000010', 'sandeep.g@gmail.com',650000,  '22, Civil Lines, Delhi');

-- Student Enrollments
INSERT INTO academic.student_enrollments (student_id, subject_id, teacher_id, academic_year, semester) VALUES
(1, 2, 1, '2024-25', 'Fall'), (1, 3, 1, '2024-25', 'Fall'),
(2, 2, 1, '2024-25', 'Fall'), (2, 3, 1, '2024-25', 'Fall'),
(3, 7, 2, '2024-25', 'Fall'), (3, 6, 2, '2024-25', 'Fall'),
(4, 4, 1, '2024-25', 'Fall'), (5, 8, 3, '2024-25', 'Fall');

-- Attendance (last 30 days sample)
INSERT INTO academic.attendance (student_id, subject_id, teacher_id, date, status) VALUES
(1, 2, 1, CURRENT_DATE - 1, 'present'), (1, 2, 1, CURRENT_DATE - 2, 'present'),
(1, 2, 1, CURRENT_DATE - 3, 'absent'),  (1, 2, 1, CURRENT_DATE - 4, 'present'),
(1, 2, 1, CURRENT_DATE - 5, 'present'), (1, 2, 1, CURRENT_DATE - 7, 'present'),
(1, 3, 1, CURRENT_DATE - 1, 'present'), (1, 3, 1, CURRENT_DATE - 2, 'late'),
(2, 2, 1, CURRENT_DATE - 1, 'present'), (2, 2, 1, CURRENT_DATE - 2, 'present'),
(2, 2, 1, CURRENT_DATE - 3, 'present'), (2, 2, 1, CURRENT_DATE - 4, 'absent'),
(3, 7, 2, CURRENT_DATE - 1, 'present'), (3, 7, 2, CURRENT_DATE - 2, 'present'),
(4, 4, 1, CURRENT_DATE - 1, 'absent'),  (4, 4, 1, CURRENT_DATE - 2, 'present'),
(5, 8, 3, CURRENT_DATE - 1, 'present'), (5, 8, 3, CURRENT_DATE - 2, 'present');

-- Exams
INSERT INTO academic.exams (name, subject_id, exam_type, academic_year, semester, exam_date, start_time, end_time, room, total_marks, passing_marks, is_published, created_by) VALUES
('DSA Midterm',     2, 'midterm', '2024-25', 'Fall', CURRENT_DATE + 7,  '10:00', '13:00', 'R101', 100, 40, TRUE,  'a0000000-0000-0000-0000-000000000001'),
('DBMS Midterm',    3, 'midterm', '2024-25', 'Fall', CURRENT_DATE + 10, '10:00', '13:00', 'R102', 100, 40, TRUE,  'a0000000-0000-0000-0000-000000000001'),
('DSA Quiz 1',      2, 'quiz',    '2024-25', 'Fall', CURRENT_DATE - 5,  '09:00', '10:00', 'R101', 25,  10, TRUE,  'a0000000-0000-0000-0000-000000000002'),
('ML Assignment 1', 5, 'assignment','2024-25','Fall', CURRENT_DATE - 3,  '00:00', '23:59', 'Online',50, 20, TRUE, 'a0000000-0000-0000-0000-000000000002');

-- Exam Results
INSERT INTO academic.exam_results (exam_id, student_id, marks_obtained, entered_by) VALUES
(3, 1, 20.5, 'a0000000-0000-0000-0000-000000000002'),
(3, 2, 18.0, 'a0000000-0000-0000-0000-000000000002'),
(4, 1, 42.0, 'a0000000-0000-0000-0000-000000000002'),
(4, 2, 38.5, 'a0000000-0000-0000-0000-000000000002');

-- Timetable
INSERT INTO academic.timetables (course_id, subject_id, teacher_id, day_of_week, start_time, end_time, room, semester, academic_year) VALUES
(1, 2, 1, 'Monday',    '09:00', '10:30', 'R201', 3, '2024-25'),
(1, 3, 1, 'Monday',    '11:00', '12:30', 'R201', 3, '2024-25'),
(1, 2, 1, 'Wednesday', '09:00', '10:30', 'R201', 3, '2024-25'),
(1, 3, 1, 'Wednesday', '11:00', '12:30', 'R202', 3, '2024-25'),
(1, 4, 1, 'Friday',    '10:00', '11:30', 'R203', 4, '2024-25'),
(2, 7, 2, 'Tuesday',   '09:00', '10:30', 'R301', 2, '2024-25'),
(3, 8, 3, 'Thursday',  '14:00', '15:30', 'R401', 1, '2024-25');

-- Fee Structures
INSERT INTO finance.fee_structures (course_id, academic_year, semester, tuition_fee, exam_fee, library_fee, lab_fee, sports_fee, misc_fee, due_date, late_fine_per_day) VALUES
(1, '2024-25', 1, 45000, 3000, 1500, 5000, 1000, 2000, CURRENT_DATE + 30, 50),
(1, '2024-25', 2, 45000, 3000, 1500, 5000, 1000, 2000, CURRENT_DATE + 60, 50),
(2, '2024-25', 1, 42000, 3000, 1500, 4500, 1000, 2000, CURRENT_DATE + 30, 50),
(3, '2024-25', 1, 42000, 3000, 1500, 4500, 1000, 2000, CURRENT_DATE + 30, 50);

-- Payments
INSERT INTO finance.payments (payment_ref, student_id, fee_structure_id, amount_due, amount_paid, payment_status, payment_method, transaction_id, payment_date, due_date, processed_by) VALUES
('PAY-2024-001', 1, 1, 57500, 57500, 'paid',    'Online',   'TXN123456', CURRENT_DATE - 10, CURRENT_DATE + 30, 'a0000000-0000-0000-0000-000000000020'),
('PAY-2024-002', 2, 1, 57500, 57500, 'paid',    'UPI',      'TXN789012', CURRENT_DATE - 5,  CURRENT_DATE + 30, 'a0000000-0000-0000-0000-000000000020'),
('PAY-2024-003', 3, 3, 52000, 0,     'pending', NULL,       NULL,        NULL,               CURRENT_DATE + 30, NULL),
('PAY-2024-004', 4, 1, 57500, 30000, 'partial', 'NEFT',     'TXN345678', CURRENT_DATE - 2,  CURRENT_DATE + 30, 'a0000000-0000-0000-0000-000000000020'),
('PAY-2024-005', 5, 4, 52000, 0,     'pending', NULL,       NULL,        NULL,               CURRENT_DATE + 30, NULL);

-- Library Books
INSERT INTO library.books (isbn, title, author, publisher, published_year, category, subject_id, total_copies, available_copies, location_shelf) VALUES
('978-0262033848', 'Introduction to Algorithms (CLRS)',   'Cormen, Leiserson, Rivest, Stein', 'MIT Press',    2022, 'Computer Science', 2, 5, 4, 'CS-A1'),
('978-0596007126', 'Head First Design Patterns',          'Freeman & Freeman',                 'OReilly Media', 2021, 'Software',        4, 3, 3, 'CS-B2'),
('978-0201633610', 'Design Patterns: GoF',                'Gamma, Helm, Johnson, Vlissides',  'Addison-Wesley',1994, 'Software',        4, 2, 2, 'CS-B3'),
('978-1491910399', 'Database Design for Mere Mortals',   'Michael J. Hernandez',              'Addison-Wesley',2020, 'Database',        3, 4, 3, 'CS-C1'),
('978-0134494166', 'Clean Code',                         'Robert C. Martin',                  'Prentice Hall', 2008, 'Software',        4, 6, 5, 'CS-D1'),
('978-0136082675', 'Operating System Concepts',          'Silberschatz, Galvin, Gagne',       'Wiley',         2018, 'Computer Science', 2, 3, 3, 'CS-A2');

-- Borrow Records
INSERT INTO library.borrow_records (book_id, user_id, borrowed_at, due_date, status) VALUES
(1, 'a0000000-0000-0000-0000-000000000010', CURRENT_DATE - 5, CURRENT_DATE + 9,  'borrowed'),
(4, 'a0000000-0000-0000-0000-000000000011', CURRENT_DATE - 8, CURRENT_DATE + 6,  'borrowed');

-- Notifications
INSERT INTO communication.notifications (title, message, type, is_broadcast, target_role, created_by) VALUES
('Welcome to Smart ERP!',           'The new Student ERP System is now live. Explore all features.', 'announcement', TRUE, NULL, 'a0000000-0000-0000-0000-000000000001'),
('Mid-Term Exams Schedule Released','Mid-term exams start next week. Check your exam schedule.',      'exam_alert',   TRUE, 'student', 'a0000000-0000-0000-0000-000000000001'),
('Fee Payment Reminder',            'Semester fee is due in 30 days. Pay before the due date.',       'fee_reminder', TRUE, 'student', 'a0000000-0000-0000-0000-000000000020'),
('Independence Day Holiday',        'College will remain closed on 15th August for Independence Day.','event',        TRUE, NULL, 'a0000000-0000-0000-0000-000000000001');

-- Performance Analytics (sample)
INSERT INTO academic.performance_analytics (student_id, academic_year, semester, attendance_percent, avg_marks, gpa, risk_score, risk_level, predicted_grade, performance_trend) VALUES
(1, '2024-25', 'Fall', 87.5, 78.3, 3.30, 18.5, 'low',    'B+', 'improving'),
(2, '2024-25', 'Fall', 75.0, 72.1, 3.00, 30.0, 'medium', 'B',  'stable'),
(3, '2024-25', 'Fall', 90.0, 85.0, 3.70, 12.0, 'low',    'A-', 'improving'),
(4, '2024-25', 'Fall', 62.5, 61.0, 2.30, 58.0, 'high',   'C',  'declining'),
(5, '2024-25', 'Fall', 95.0, 88.5, 3.80, 8.0,  'low',    'A',  'improving');

\echo '===== Database Setup Complete! ====='
\echo 'Schemas: auth, academic, finance, library, communication'
\echo 'Default password for all users: Password@123'
\echo 'Admin email: admin@erp.edu'
