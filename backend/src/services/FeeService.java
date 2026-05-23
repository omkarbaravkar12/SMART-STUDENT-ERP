package services;

import database.DatabaseConnection;
import utils.Logger;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class FeeService {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<Map<String, Object>> getPayments(String studentId, String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT p.id, p.payment_ref, s.student_id AS roll_no,
                   u.first_name || ' ' || u.last_name AS student_name,
                   c.name AS course_name, p.amount_due, p.amount_paid,
                   p.late_fine, p.scholarship_discount, p.payment_status,
                   p.payment_method, p.transaction_id, p.payment_date, p.due_date
            FROM finance.payments p
            JOIN academic.students s ON p.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            LEFT JOIN academic.courses c ON s.course_id = c.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (studentId != null && !studentId.isBlank()) { sql.append(" AND p.student_id=?"); params.add(Integer.parseInt(studentId)); }
        if (status    != null && !status.isBlank())    { sql.append(" AND p.payment_status=?::payment_status"); params.add(status); }
        sql.append(" ORDER BY p.created_at DESC");
        return query(sql.toString(), params);
    }

    public Map<String, Object> recordPayment(Map<String, Object> data) {
        String sql = """
            UPDATE finance.payments
            SET amount_paid = ?, payment_status = ?::payment_status,
                payment_method = ?, transaction_id = ?, payment_date = CURRENT_DATE
            WHERE id = ?
            RETURNING id, payment_ref, payment_status, amount_paid
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            double amountPaid = Double.parseDouble(data.get("amountPaid").toString());
            String payId      = data.get("paymentId").toString();

            // Determine status
            List<Map<String,Object>> existing = query("SELECT amount_due FROM finance.payments WHERE id=?", List.of(Integer.parseInt(payId)));
            String newStatus = "paid";
            if (!existing.isEmpty()) {
                double amountDue = ((Number) existing.get(0).get("amount_due")).doubleValue();
                if (amountPaid < amountDue) newStatus = "partial";
            }

            ps.setDouble(1, amountPaid);
            ps.setString(2, newStatus);
            ps.setString(3, (String) data.getOrDefault("paymentMethod", "Cash"));
            ps.setString(4, (String) data.getOrDefault("transactionId", null));
            ps.setInt(5,    Integer.parseInt(payId));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Auto-generate invoice
                generateInvoice(rs.getInt("id"), Integer.parseInt(Objects.toString(data.get("studentId"), "0")), amountPaid);
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("paymentId",    rs.getInt("id"));
                resp.put("paymentRef",   rs.getString("payment_ref"));
                resp.put("status",       rs.getString("payment_status"));
                resp.put("amountPaid",   rs.getDouble("amount_paid"));
                return resp;
            }
        } catch (SQLException e) { Logger.error("recordPayment: " + e.getMessage()); }
        return null;
    }

    private void generateInvoice(int paymentId, int studentId, double amount) {
        String sql = "INSERT INTO finance.invoices (payment_id, student_id, total_amount) VALUES (?,?,?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            ps.setInt(2, studentId);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) { Logger.error("generateInvoice: " + e.getMessage()); }
    }

    public List<Map<String, Object>> getInvoices(String studentId) {
        StringBuilder sql = new StringBuilder("""
            SELECT i.id, i.invoice_number, i.invoice_date, i.total_amount,
                   p.payment_ref, p.payment_method, p.payment_status,
                   s.student_id AS roll_no, u.first_name || ' ' || u.last_name AS student_name
            FROM finance.invoices i
            JOIN finance.payments p ON i.payment_id = p.id
            JOIN academic.students s ON i.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (studentId != null && !studentId.isBlank()) { sql.append(" AND i.student_id=?"); params.add(Integer.parseInt(studentId)); }
        sql.append(" ORDER BY i.generated_at DESC");
        return query(sql.toString(), params);
    }

    public List<Map<String, Object>> getFeeStructures(String courseId) {
        StringBuilder sql = new StringBuilder("""
            SELECT fs.*, c.name AS course_name,
                   (fs.tuition_fee + fs.exam_fee + fs.library_fee + fs.lab_fee + fs.sports_fee + fs.misc_fee) AS total_fee
            FROM finance.fee_structures fs
            JOIN academic.courses c ON fs.course_id = c.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (courseId != null && !courseId.isBlank()) { sql.append(" AND fs.course_id=?"); params.add(Integer.parseInt(courseId)); }
        sql.append(" ORDER BY fs.academic_year DESC, fs.semester");
        return query(sql.toString(), params);
    }

    public Map<String, Object> createFeeStructure(Map<String, Object> data) {
        String sql = """
            INSERT INTO finance.fee_structures
              (course_id, academic_year, semester, tuition_fee, exam_fee, library_fee, lab_fee, sports_fee, misc_fee, due_date, late_fine_per_day)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (course_id, academic_year, semester) DO UPDATE SET tuition_fee=EXCLUDED.tuition_fee
            RETURNING id
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1,    Integer.parseInt(data.get("courseId").toString()));
            ps.setString(2, (String) data.get("academicYear"));
            ps.setInt(3,    Integer.parseInt(data.get("semester").toString()));
            ps.setDouble(4, Double.parseDouble(data.getOrDefault("tuitionFee","0").toString()));
            ps.setDouble(5, Double.parseDouble(data.getOrDefault("examFee","0").toString()));
            ps.setDouble(6, Double.parseDouble(data.getOrDefault("libraryFee","0").toString()));
            ps.setDouble(7, Double.parseDouble(data.getOrDefault("labFee","0").toString()));
            ps.setDouble(8, Double.parseDouble(data.getOrDefault("sportsFee","0").toString()));
            ps.setDouble(9, Double.parseDouble(data.getOrDefault("miscFee","0").toString()));
            ps.setDate(10,  Date.valueOf((String) data.get("dueDate")));
            ps.setDouble(11,Double.parseDouble(data.getOrDefault("lateFinePerDay","0").toString()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Map.of("id", rs.getInt("id"), "message", "Fee structure created");
        } catch (SQLException e) { Logger.error("createFeeStructure: " + e.getMessage()); }
        return null;
    }

    private List<Map<String, Object>> query(String sql, List<Object> params) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
        } catch (SQLException e) { Logger.error("FeeService query: " + e.getMessage()); }
        return rows;
    }
}
