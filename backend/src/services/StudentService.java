package services;

import database.DatabaseConnection;
import utils.Logger;
import utils.PasswordUtil;

import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * StudentService - Business logic for student management
 */
public class StudentService {

    private final DatabaseConnection db;

    public StudentService() {
        this.db = DatabaseConnection.getInstance();
    }

    /**
     * Get paginated list of students with filters
     */
    public Map<String, Object> getStudents(int page, int limit, String search,
                                            String departmentId, String courseId, String semester) {
        StringBuilder sql = new StringBuilder("""
            SELECT s.id, s.student_id, u.first_name, u.last_name, u.email, u.phone,
                   u.profile_photo, c.name AS course_name, d.name AS dept_name,
                   s.current_semester, s.admission_date, s.gender, s.is_active,
                   COUNT(*) OVER() AS total_count
            FROM academic.students s
            JOIN auth.users u ON s.user_id = u.id
            LEFT JOIN academic.courses c ON s.course_id = c.id
            LEFT JOIN academic.departments d ON s.department_id = d.id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(u.first_name || ' ' || u.last_name) LIKE ? OR s.student_id LIKE ? OR u.email LIKE ?)");
            String pattern = "%" + search.toLowerCase() + "%";
            params.add(pattern); params.add(pattern); params.add(pattern);
        }
        if (departmentId != null && !departmentId.isBlank()) {
            sql.append(" AND s.department_id = ?");
            params.add(Integer.parseInt(departmentId));
        }
        if (courseId != null && !courseId.isBlank()) {
            sql.append(" AND s.course_id = ?");
            params.add(Integer.parseInt(courseId));
        }
        if (semester != null && !semester.isBlank()) {
            sql.append(" AND s.current_semester = ?");
            params.add(Integer.parseInt(semester));
        }

        sql.append(" ORDER BY s.id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);

        List<Map<String, Object>> students = new ArrayList<>();
        long total = 0;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (total == 0) total = rs.getLong("total_count");
                students.add(mapStudentRow(rs));
            }
        } catch (SQLException e) {
            Logger.error("getStudents error: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("data", students);
        result.put("total", total);
        return result;
    }

    /**
     * Get student by ID with full profile
     */
    public Map<String, Object> getStudentById(String id) {
        String sql = """
            SELECT s.*, u.first_name, u.last_name, u.email, u.phone, u.profile_photo,
                   u.is_active, c.name AS course_name, d.name AS dept_name,
                   p.father_name, p.mother_name, p.father_phone, p.annual_income
            FROM academic.students s
            JOIN auth.users u ON s.user_id = u.id
            LEFT JOIN academic.courses c ON s.course_id = c.id
            LEFT JOIN academic.departments d ON s.department_id = d.id
            LEFT JOIN academic.parents p ON s.id = p.student_id
            WHERE s.id = ?
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> student = mapStudentRow(rs);
                student.put("dateOfBirth",       rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toString() : null);
                student.put("bloodGroup",        rs.getString("blood_group"));
                student.put("address",           rs.getString("address"));
                student.put("city",              rs.getString("city"));
                student.put("state",             rs.getString("state"));
                student.put("emergencyContact",  rs.getString("emergency_contact"));
                student.put("fatherName",        rs.getString("father_name"));
                student.put("motherName",        rs.getString("mother_name"));
                student.put("fatherPhone",       rs.getString("father_phone"));
                student.put("annualIncome",      rs.getObject("annual_income"));
                return student;
            }
        } catch (SQLException e) {
            Logger.error("getStudentById error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Create new student (creates user + student records)
     */
    public Map<String, Object> createStudent(Map<String, Object> data) {
        Connection conn = db.getConnection();
        try {
            db.beginTransaction();

            // Check email uniqueness
            String checkSql = "SELECT id FROM auth.users WHERE email = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, (String) data.get("email"));
                if (ps.executeQuery().next()) {
                    db.rollback();
                    return null;
                }
            }

            // Create user
            String defaultPassword = "Password@123";
            String passwordHash    = PasswordUtil.hash(defaultPassword);

            String userSql = """
                INSERT INTO auth.users (email, password_hash, role, first_name, last_name, phone, is_email_verified)
                VALUES (?, ?, 'student', ?, ?, ?, TRUE)
                RETURNING id
                """;

            String userId;
            try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                ps.setString(1, (String) data.get("email"));
                ps.setString(2, passwordHash);
                ps.setString(3, (String) data.get("firstName"));
                ps.setString(4, (String) data.get("lastName"));
                ps.setString(5, (String) data.getOrDefault("phone", null));
                ResultSet rs = ps.executeQuery();
                rs.next();
                userId = rs.getString("id");
            }

            // Generate student ID
            String studentId = generateStudentId();

            // Create student record
            String studentSql = """
                INSERT INTO academic.students
                  (user_id, student_id, course_id, department_id, current_semester,
                   admission_date, date_of_birth, gender, blood_group, address, city, state)
                VALUES (?, ?, ?, ?, 1, CURRENT_DATE, ?, ?::gender_type, ?, ?, ?, ?)
                RETURNING id
                """;

            int studentDbId;
            try (PreparedStatement ps = conn.prepareStatement(studentSql)) {
                ps.setObject(1, UUID.fromString(userId));
                ps.setString(2, studentId);
                ps.setObject(3, data.get("courseId") != null ? Integer.parseInt(data.get("courseId").toString()) : null);
                ps.setObject(4, data.get("departmentId") != null ? Integer.parseInt(data.get("departmentId").toString()) : null);
                ps.setObject(5, data.get("dateOfBirth") != null ? Date.valueOf((String) data.get("dateOfBirth")) : null);
                ps.setString(6, (String) data.get("gender"));
                ps.setString(7, (String) data.get("bloodGroup"));
                ps.setString(8, (String) data.get("address"));
                ps.setString(9, (String) data.get("city"));
                ps.setString(10, (String) data.get("state"));
                ResultSet rs = ps.executeQuery();
                rs.next();
                studentDbId = rs.getInt("id");
            }

            db.commit();
            Logger.info("Created student: " + studentId + " (id=" + studentDbId + ")");
            return getStudentById(String.valueOf(studentDbId));

        } catch (SQLException e) {
            db.rollback();
            Logger.error("createStudent error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Update student record
     */
    public boolean updateStudent(String id, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE academic.students SET ");
        List<Object> params = new ArrayList<>();

        if (data.containsKey("courseId")) {
            sql.append("course_id = ?, "); params.add(Integer.parseInt(data.get("courseId").toString()));
        }
        if (data.containsKey("currentSemester")) {
            sql.append("current_semester = ?, "); params.add(Integer.parseInt(data.get("currentSemester").toString()));
        }
        if (data.containsKey("address")) {
            sql.append("address = ?, "); params.add(data.get("address"));
        }
        if (data.containsKey("city")) {
            sql.append("city = ?, "); params.add(data.get("city"));
        }
        if (data.containsKey("bloodGroup")) {
            sql.append("blood_group = ?, "); params.add(data.get("bloodGroup"));
        }
        if (data.containsKey("isActive")) {
            sql.append("is_active = ?, "); params.add(Boolean.parseBoolean(data.get("isActive").toString()));
        }

        if (params.isEmpty()) return true;

        // Remove trailing comma
        String query = sql.toString().replaceAll(",\\s*$", "") + " WHERE id = ?";
        params.add(Integer.parseInt(id));

        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Logger.error("updateStudent error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete student (soft delete)
     */
    public boolean deleteStudent(String id) {
        String sql = """
            UPDATE auth.users SET is_active = FALSE
            WHERE id = (SELECT user_id FROM academic.students WHERE id = ?)
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            Logger.error("deleteStudent error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get student attendance records
     */
    public List<Map<String, Object>> getStudentAttendance(String studentId, String subjectId, String from, String to) {
        StringBuilder sql = new StringBuilder("""
            SELECT a.date, a.status, a.remarks,
                   sub.code AS subject_code, sub.name AS subject_name
            FROM academic.attendance a
            JOIN academic.subjects sub ON a.subject_id = sub.id
            WHERE a.student_id = ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(Integer.parseInt(studentId));

        if (subjectId != null && !subjectId.isBlank()) {
            sql.append(" AND a.subject_id = ?");
            params.add(Integer.parseInt(subjectId));
        }
        if (from != null && !from.isBlank()) {
            sql.append(" AND a.date >= ?");
            params.add(Date.valueOf(from));
        }
        if (to != null && !to.isBlank()) {
            sql.append(" AND a.date <= ?");
            params.add(Date.valueOf(to));
        }
        sql.append(" ORDER BY a.date DESC LIMIT 200");

        List<Map<String, Object>> records = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("date",        rs.getDate("date").toString());
                row.put("status",      rs.getString("status"));
                row.put("remarks",     rs.getString("remarks"));
                row.put("subjectCode", rs.getString("subject_code"));
                row.put("subjectName", rs.getString("subject_name"));
                records.add(row);
            }
        } catch (SQLException e) {
            Logger.error("getStudentAttendance error: " + e.getMessage());
        }
        return records;
    }

    /**
     * Get student exam results with GPA calculation
     */
    public Map<String, Object> getStudentResults(String studentId, String academicYear, String semester) {
        String sql = """
            SELECT er.marks_obtained, er.grade, er.grade_points, er.is_absent,
                   e.name AS exam_name, e.exam_type, e.total_marks, e.exam_date,
                   sub.code AS subject_code, sub.name AS subject_name, sub.credits
            FROM academic.exam_results er
            JOIN academic.exams e ON er.exam_id = e.id
            JOIN academic.subjects sub ON e.subject_id = sub.id
            WHERE er.student_id = ?
            AND (? IS NULL OR e.academic_year = ?)
            AND (? IS NULL OR e.semester = ?::semester_type)
            ORDER BY e.exam_date DESC
            """;

        List<Map<String, Object>> results = new ArrayList<>();
        double totalGradePoints = 0;
        int totalCredits = 0;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(studentId));
            ps.setString(2, academicYear); ps.setString(3, academicYear);
            ps.setString(4, semester);     ps.setString(5, semester);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("examName",    rs.getString("exam_name"));
                row.put("examType",    rs.getString("exam_type"));
                row.put("examDate",    rs.getDate("exam_date") != null ? rs.getDate("exam_date").toString() : null);
                row.put("subjectCode", rs.getString("subject_code"));
                row.put("subjectName", rs.getString("subject_name"));
                row.put("credits",     rs.getInt("credits"));
                row.put("marksObtained", rs.getObject("marks_obtained"));
                row.put("totalMarks",    rs.getInt("total_marks"));
                row.put("grade",         rs.getString("grade"));
                row.put("gradePoints",   rs.getObject("grade_points"));
                row.put("isAbsent",      rs.getBoolean("is_absent"));
                results.add(row);

                if (!rs.getBoolean("is_absent") && rs.getObject("grade_points") != null) {
                    totalGradePoints += rs.getDouble("grade_points") * rs.getInt("credits");
                    totalCredits     += rs.getInt("credits");
                }
            }
        } catch (SQLException e) {
            Logger.error("getStudentResults error: " + e.getMessage());
        }

        double gpa = totalCredits > 0 ? Math.round((totalGradePoints / totalCredits) * 100.0) / 100.0 : 0.0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("gpa",     gpa);
        response.put("totalCredits", totalCredits);
        return response;
    }

    /**
     * Get student timetable
     */
    public Map<String, Object> getStudentTimetable(String studentId) {
        String sql = """
            SELECT t.day_of_week, t.start_time, t.end_time, t.room,
                   sub.code AS subject_code, sub.name AS subject_name,
                   u.first_name || ' ' || u.last_name AS teacher_name
            FROM academic.timetables t
            JOIN academic.subjects sub ON t.subject_id = sub.id
            LEFT JOIN academic.teachers tc ON t.teacher_id = tc.id
            LEFT JOIN auth.users u ON tc.user_id = u.id
            WHERE t.course_id = (SELECT course_id FROM academic.students WHERE id = ?)
              AND t.semester = (SELECT current_semester FROM academic.students WHERE id = ?)
              AND t.is_active = TRUE
            ORDER BY CASE t.day_of_week
                WHEN 'Monday'    THEN 1 WHEN 'Tuesday'   THEN 2
                WHEN 'Wednesday' THEN 3 WHEN 'Thursday'  THEN 4
                WHEN 'Friday'    THEN 5 WHEN 'Saturday'  THEN 6
                ELSE 7 END, t.start_time
            """;

        Map<String, List<Map<String, Object>>> byDay = new LinkedHashMap<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(studentId));
            ps.setInt(2, Integer.parseInt(studentId));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String day = rs.getString("day_of_week");
                byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(Map.of(
                    "startTime",   rs.getTime("start_time").toString(),
                    "endTime",     rs.getTime("end_time").toString(),
                    "room",        Objects.toString(rs.getString("room"), "TBD"),
                    "subjectCode", rs.getString("subject_code"),
                    "subjectName", rs.getString("subject_name"),
                    "teacherName", Objects.toString(rs.getString("teacher_name"), "TBD")
                ));
            }
        } catch (SQLException e) {
            Logger.error("getStudentTimetable error: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timetable", byDay);
        return result;
    }

    // ─── Helpers ────────────────────────────────────────────

    private Map<String, Object> mapStudentRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id",              rs.getInt("id"));
        row.put("studentId",       rs.getString("student_id"));
        row.put("firstName",       rs.getString("first_name"));
        row.put("lastName",        rs.getString("last_name"));
        row.put("email",           rs.getString("email"));
        row.put("phone",           rs.getString("phone"));
        row.put("profilePhoto",    rs.getString("profile_photo"));
        row.put("courseName",      rs.getString("course_name"));
        row.put("departmentName",  rs.getString("dept_name"));
        row.put("currentSemester", rs.getInt("current_semester"));
        row.put("admissionDate",   rs.getDate("admission_date") != null ? rs.getDate("admission_date").toString() : null);
        try { row.put("gender", rs.getString("gender")); } catch (SQLException ignored) {}
        try { row.put("isActive", rs.getBoolean("is_active")); } catch (SQLException ignored) {}
        return row;
    }

    private String generateStudentId() throws SQLException {
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String sql = "SELECT COUNT(*) FROM academic.students WHERE admission_date >= DATE_TRUNC('year', CURRENT_DATE)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            rs.next();
            int seq = rs.getInt(1) + 1;
            return String.format("STU%d%03d", year, seq);
        }
    }
}
