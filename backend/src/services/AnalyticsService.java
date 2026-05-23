package services;

import database.DatabaseConnection;
import utils.Logger;

import java.sql.*;
import java.util.*;

/**
 * AnalyticsService - Aggregated statistics for dashboards
 */
public class AnalyticsService {

    private final DatabaseConnection db;

    public AnalyticsService() {
        this.db = DatabaseConnection.getInstance();
    }

    /**
     * Master dashboard stats (admin)
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalStudents",       queryCount("SELECT COUNT(*) FROM academic.students WHERE is_active=TRUE"));
        stats.put("totalTeachers",       queryCount("SELECT COUNT(*) FROM academic.teachers WHERE is_active=TRUE"));
        stats.put("totalCourses",        queryCount("SELECT COUNT(*) FROM academic.courses WHERE is_active=TRUE"));
        stats.put("totalDepartments",    queryCount("SELECT COUNT(*) FROM academic.departments WHERE is_active=TRUE"));
        stats.put("totalSubjects",       queryCount("SELECT COUNT(*) FROM academic.subjects"));
        stats.put("pendingFees",         queryCount("SELECT COUNT(*) FROM finance.payments WHERE payment_status='pending'"));
        stats.put("todayAttendance",     queryCount("SELECT COUNT(*) FROM academic.attendance WHERE date=CURRENT_DATE AND status='present'"));
        stats.put("upcomingExams",       queryCount("SELECT COUNT(*) FROM academic.exams WHERE exam_date >= CURRENT_DATE AND is_published=TRUE"));
        stats.put("overdueBooks",        queryCount("SELECT COUNT(*) FROM library.borrow_records WHERE due_date < CURRENT_DATE AND status='borrowed'"));
        stats.put("unreadNotifications", queryCount("SELECT COUNT(*) FROM communication.user_notifications WHERE is_read=FALSE"));

        // Revenue this month
        String revenueSql = """
            SELECT COALESCE(SUM(amount_paid), 0) AS revenue
            FROM finance.payments
            WHERE DATE_TRUNC('month', payment_date) = DATE_TRUNC('month', CURRENT_DATE)
            AND payment_status IN ('paid','partial')
            """;
        stats.put("revenueThisMonth", queryDecimal(revenueSql));

        // Enrollment by department
        stats.put("enrollmentByDept",   getEnrollmentByDepartment());
        // Monthly enrollment trend
        stats.put("enrollmentTrend",    getEnrollmentTrend());
        // Fee collection summary
        stats.put("feeCollectionSummary", getFeeCollectionByStatus());
        // At-risk students
        stats.put("atRiskStudents",     getAtRiskCount());

        return stats;
    }

    /**
     * Student analytics
     */
    public Map<String, Object> getStudentAnalytics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Gender distribution
        String genderSql = """
            SELECT gender, COUNT(*) AS count
            FROM academic.students WHERE is_active=TRUE AND gender IS NOT NULL
            GROUP BY gender
            """;
        stats.put("genderDistribution", queryList(genderSql));

        // Semester-wise student count
        String semSql = """
            SELECT current_semester AS semester, COUNT(*) AS count
            FROM academic.students WHERE is_active=TRUE
            GROUP BY current_semester ORDER BY current_semester
            """;
        stats.put("semesterDistribution", queryList(semSql));

        // Performance distribution (grade distribution)
        String gradeSql = """
            SELECT er.grade, COUNT(*) AS count
            FROM academic.exam_results er
            WHERE er.grade IS NOT NULL
            GROUP BY er.grade ORDER BY er.grade
            """;
        stats.put("gradeDistribution", queryList(gradeSql));

        // Top 5 performing students by GPA
        String topSql = """
            SELECT s.student_id, u.first_name || ' ' || u.last_name AS name,
                   pa.gpa, pa.attendance_percent, pa.risk_level
            FROM academic.performance_analytics pa
            JOIN academic.students s ON pa.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            ORDER BY pa.gpa DESC NULLS LAST LIMIT 5
            """;
        stats.put("topStudents", queryList(topSql));

        // At-risk students
        String riskSql = """
            SELECT s.student_id, u.first_name || ' ' || u.last_name AS name,
                   pa.risk_score, pa.risk_level, pa.attendance_percent, pa.avg_marks
            FROM academic.performance_analytics pa
            JOIN academic.students s ON pa.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            WHERE pa.risk_level IN ('high','medium')
            ORDER BY pa.risk_score DESC
            """;
        stats.put("atRiskStudents", queryList(riskSql));

        return stats;
    }

    /**
     * Fee analytics
     */
    public Map<String, Object> getFeeAnalytics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Total expected vs collected
        String totalSql = """
            SELECT
                SUM(amount_due) AS total_expected,
                SUM(amount_paid) AS total_collected,
                SUM(amount_due - amount_paid) AS total_pending
            FROM finance.payments
            """;
        List<Map<String, Object>> totals = queryList(totalSql);
        if (!totals.isEmpty()) stats.putAll(totals.get(0));

        // Monthly collection trend (last 6 months)
        String monthlySql = """
            SELECT TO_CHAR(payment_date, 'Mon YYYY') AS month,
                   SUM(amount_paid) AS collected
            FROM finance.payments
            WHERE payment_date >= CURRENT_DATE - INTERVAL '6 months'
              AND payment_status IN ('paid','partial')
            GROUP BY DATE_TRUNC('month', payment_date), TO_CHAR(payment_date, 'Mon YYYY')
            ORDER BY DATE_TRUNC('month', payment_date)
            """;
        stats.put("monthlyTrend", queryList(monthlySql));

        // Payment status distribution
        stats.put("statusDistribution", getFeeCollectionByStatus());

        // Top defaulters
        String defaulterSql = """
            SELECT s.student_id, u.first_name || ' ' || u.last_name AS name,
                   p.amount_due - p.amount_paid AS outstanding, p.due_date
            FROM finance.payments p
            JOIN academic.students s ON p.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            WHERE p.payment_status = 'pending'
            ORDER BY outstanding DESC LIMIT 10
            """;
        stats.put("topDefaulters", queryList(defaulterSql));

        return stats;
    }

    /**
     * Attendance analytics
     */
    public Map<String, Object> getAttendanceAnalytics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Average attendance by subject
        String subjectSql = """
            SELECT sub.name AS subject,
                   ROUND(COUNT(*) FILTER(WHERE a.status='present')::NUMERIC / NULLIF(COUNT(*),0)*100,1) AS avg_percent
            FROM academic.attendance a
            JOIN academic.subjects sub ON a.subject_id = sub.id
            WHERE a.date >= CURRENT_DATE - 30
            GROUP BY sub.name ORDER BY avg_percent DESC
            """;
        stats.put("bySubject", queryList(subjectSql));

        // Daily trend last 14 days
        String dailySql = """
            SELECT date, COUNT(*) AS total,
                   COUNT(*) FILTER(WHERE status='present') AS present,
                   COUNT(*) FILTER(WHERE status='absent') AS absent
            FROM academic.attendance
            WHERE date >= CURRENT_DATE - 14
            GROUP BY date ORDER BY date
            """;
        stats.put("dailyTrend", queryList(dailySql));

        // Students with < 75% attendance
        String lowAttSql = """
            SELECT s.student_id, u.first_name || ' ' || u.last_name AS name,
                   asv.attendance_percent, asv.subject_name
            FROM academic.attendance_summary asv
            JOIN academic.students s ON asv.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            WHERE asv.attendance_percent < 75
            ORDER BY asv.attendance_percent
            """;
        stats.put("lowAttendance", queryList(lowAttSql));

        return stats;
    }

    // ─── Private helpers ────────────────────────────────────

    private long queryCount(String sql) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            Logger.error("queryCount error for: " + sql.substring(0, Math.min(50, sql.length())) + " - " + e.getMessage());
        }
        return 0L;
    }

    private double queryDecimal(String sql) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            Logger.error("queryDecimal error: " + e.getMessage());
        }
        return 0.0;
    }

    private List<Map<String, Object>> queryList(String sql) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            Logger.error("queryList error: " + e.getMessage());
        }
        return rows;
    }

    private List<Map<String, Object>> getEnrollmentByDepartment() {
        return queryList("""
            SELECT d.name AS department, COUNT(s.id) AS students
            FROM academic.departments d
            LEFT JOIN academic.students s ON d.id = s.department_id AND s.is_active=TRUE
            GROUP BY d.name ORDER BY students DESC
            """);
    }

    private List<Map<String, Object>> getEnrollmentTrend() {
        return queryList("""
            SELECT TO_CHAR(admission_date,'Mon YYYY') AS month, COUNT(*) AS enrollments
            FROM academic.students
            WHERE admission_date >= CURRENT_DATE - INTERVAL '12 months'
            GROUP BY DATE_TRUNC('month', admission_date), TO_CHAR(admission_date,'Mon YYYY')
            ORDER BY DATE_TRUNC('month', admission_date)
            """);
    }

    private List<Map<String, Object>> getFeeCollectionByStatus() {
        return queryList("""
            SELECT payment_status AS status, COUNT(*) AS count, SUM(amount_due) AS total_amount
            FROM finance.payments GROUP BY payment_status
            """);
    }

    private long getAtRiskCount() {
        return queryCount("SELECT COUNT(*) FROM academic.performance_analytics WHERE risk_level='high'");
    }
}
