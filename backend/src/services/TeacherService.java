package services;
import database.DatabaseConnection;
import utils.Logger;
import utils.PasswordUtil;
import java.sql.*;
import java.util.*;

public class TeacherService {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<Map<String, Object>> listTeachers(String deptId) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.employee_id, t.designation, t.qualification,
                   t.joining_date, t.is_active,
                   u.first_name, u.last_name, u.email, u.phone,
                   d.name AS department_name
            FROM academic.teachers t
            JOIN auth.users u ON t.user_id = u.id
            LEFT JOIN academic.departments d ON t.department_id = d.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (deptId != null && !deptId.isBlank()) { sql.append(" AND t.department_id=?"); params.add(Integer.parseInt(deptId)); }
        sql.append(" ORDER BY t.id");
        return query(sql.toString(), params);
    }

    public long totalTeachers() {
        try (PreparedStatement ps = db.getConnection().prepareStatement("SELECT COUNT(*) FROM academic.teachers WHERE is_active=TRUE");
             ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong(1); }
        catch (SQLException e) { Logger.error(e.getMessage()); } return 0;
    }

    public Map<String, Object> getTeacherById(String id) {
        List<Map<String,Object>> rows = query("""
            SELECT t.*, u.first_name, u.last_name, u.email, u.phone, d.name AS department_name
            FROM academic.teachers t JOIN auth.users u ON t.user_id=u.id
            LEFT JOIN academic.departments d ON t.department_id=d.id WHERE t.id=?
            """, List.of(Integer.parseInt(id)));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> createTeacher(Map<String, Object> data) {
        Connection conn = db.getConnection();
        try {
            db.beginTransaction();
            String userSql = "INSERT INTO auth.users (email,password_hash,role,first_name,last_name,phone,is_email_verified) VALUES (?,?,'teacher',?,?,?,TRUE) RETURNING id";
            String userId;
            try (PreparedStatement ps = conn.prepareStatement(userSql)) {
                ps.setString(1,(String)data.get("email"));
                ps.setString(2, PasswordUtil.hash("Password@123"));
                ps.setString(3,(String)data.get("firstName")); ps.setString(4,(String)data.get("lastName"));
                ps.setString(5,(String)data.getOrDefault("phone",null));
                ResultSet rs = ps.executeQuery(); rs.next(); userId = rs.getString("id");
            }
            String empId = "EMP" + String.format("%03d", totalTeachers() + 1);
            String tSql = "INSERT INTO academic.teachers (user_id,employee_id,department_id,designation,qualification,joining_date) VALUES (?::uuid,?,?,?,?,CURRENT_DATE) RETURNING id";
            try (PreparedStatement ps = conn.prepareStatement(tSql)) {
                ps.setString(1,userId); ps.setString(2,empId);
                ps.setObject(3,data.get("departmentId")!=null ? Integer.parseInt(data.get("departmentId").toString()) : null);
                ps.setString(4,(String)data.getOrDefault("designation","Lecturer"));
                ps.setString(5,(String)data.getOrDefault("qualification",null));
                ResultSet rs = ps.executeQuery(); rs.next();
                db.commit();
                return getTeacherById(rs.getString("id"));
            }
        } catch (SQLException e) { db.rollback(); Logger.error("createTeacher: " + e.getMessage()); return null; }
    }

    public boolean updateTeacher(String id, Map<String, Object> data) {
        String sql = "UPDATE academic.teachers SET designation=COALESCE(?,designation), qualification=COALESCE(?,qualification) WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1,(String)data.get("designation")); ps.setString(2,(String)data.get("qualification"));
            ps.setInt(3,Integer.parseInt(id)); return ps.executeUpdate() > 0;
        } catch (SQLException e) { Logger.error("updateTeacher: " + e.getMessage()); return false; }
    }

    private List<Map<String, Object>> query(String sql, List<Object> params) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i+1, params.get(i));
            ResultSet rs = ps.executeQuery(); ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
        } catch (SQLException e) { Logger.error("TeacherService query: " + e.getMessage()); }
        return rows;
    }
}
