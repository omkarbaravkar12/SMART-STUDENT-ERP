package services;

import database.DatabaseConnection;
import utils.Logger;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class AttendanceService {

    private final DatabaseConnection db;
    public AttendanceService() { this.db = DatabaseConnection.getInstance(); }

    public List<Map<String, Object>> getAttendance(String subjectId, String date, String studentId) {
        StringBuilder sql = new StringBuilder("""
            SELECT a.id, a.date, a.status, a.remarks,
                   s.student_id AS roll_no,
                   u.first_name || ' ' || u.last_name AS student_name,
                   sub.name AS subject_name, sub.code AS subject_code
            FROM academic.attendance a
            JOIN academic.students s ON a.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            JOIN academic.subjects sub ON a.subject_id = sub.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (subjectId != null && !subjectId.isBlank()) {
            sql.append(" AND a.subject_id = ?"); params.add(Integer.parseInt(subjectId));
        }
        if (date != null && !date.isBlank()) {
            sql.append(" AND a.date = ?"); params.add(Date.valueOf(date));
        }
        if (studentId != null && !studentId.isBlank()) {
            sql.append(" AND a.student_id = ?"); params.add(Integer.parseInt(studentId));
        }
        sql.append(" ORDER BY a.date DESC, student_name LIMIT 500");

        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(Map.of(
                    "id",          rs.getInt("id"),
                    "date",        rs.getDate("date").toString(),
                    "status",      rs.getString("status"),
                    "remarks",     Objects.toString(rs.getString("remarks"), ""),
                    "rollNo",      rs.getString("roll_no"),
                    "studentName", rs.getString("student_name"),
                    "subjectName", rs.getString("subject_name"),
                    "subjectCode", rs.getString("subject_code")
                ));
            }
        } catch (SQLException e) { Logger.error("getAttendance: " + e.getMessage()); }
        return rows;
    }

    @SuppressWarnings("unchecked")
    public boolean markAttendance(Map<String, Object> data, String teacherUserId) {
        // Supports bulk: data.records = [{studentId, status, remarks}, ...]
        // Or single: data.studentId, data.subjectId, data.status, data.date
        String subjectId = Objects.toString(data.get("subjectId"), null);
        String dateStr   = Objects.toString(data.get("date"), null);
        if (subjectId == null || dateStr == null) return false;

        String sql = """
            INSERT INTO academic.attendance (student_id, subject_id, teacher_id, date, status, remarks)
            VALUES (?, ?, (SELECT id FROM academic.teachers WHERE user_id = ?::uuid), ?, ?::attendance_status, ?)
            ON CONFLICT (student_id, subject_id, date) DO UPDATE
            SET status = EXCLUDED.status, remarks = EXCLUDED.remarks
            """;

        Object records = data.get("records");
        if (records instanceof String recStr) {
            // It's JSON array string — parse manually
            records = parseRecordsList(recStr);
        }

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            if (records instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> rec) {
                        ps.setInt(1,    Integer.parseInt(Objects.toString(rec.get("studentId"))));
                        ps.setInt(2,    Integer.parseInt(subjectId));
                        ps.setString(3, teacherUserId);
                        ps.setDate(4,   Date.valueOf(dateStr));
                       ps.setString(5, Objects.toString(rec.get("status") != null ? rec.get("status") : "present"));
ps.setString(6, rec.get("remarks") != null ? rec.get("remarks").toString() : "");
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            } else {
                // Single record
                ps.setInt(1,    Integer.parseInt(Objects.toString(data.get("studentId"))));
                ps.setInt(2,    Integer.parseInt(subjectId));
                ps.setString(3, teacherUserId);
                ps.setDate(4,   Date.valueOf(dateStr));
                ps.setString(5, Objects.toString(data.getOrDefault("status", "present")));
                ps.setString(6, Objects.toString(data.getOrDefault("remarks", ""), ""));
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            Logger.error("markAttendance: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getAttendanceSummary(String studentId, String subjectId) {
        StringBuilder sql = new StringBuilder("""
            SELECT asv.*
            FROM academic.attendance_summary asv
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (studentId != null && !studentId.isBlank()) {
            sql.append(" AND asv.student_id = ?"); params.add(Integer.parseInt(studentId));
        }
        if (subjectId != null && !subjectId.isBlank()) {
            sql.append(" AND asv.subject_code = (SELECT code FROM academic.subjects WHERE id = ?)");
            params.add(Integer.parseInt(subjectId));
        }
        sql.append(" ORDER BY attendance_percent ASC");

        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
        } catch (SQLException e) { Logger.error("getAttendanceSummary: " + e.getMessage()); }
        return rows;
    }

    private List<Map<String, Object>> parseRecordsList(String json) {
        List<Map<String, Object>> list = new ArrayList<>();
        // Simple array parser: [{...},{...}]
        json = json.trim();
        if (!json.startsWith("[")) return list;
        json = json.substring(1, json.length() - 1).trim();
        for (String item : json.split("\\},\\s*\\{")) {
            item = item.replace("{", "").replace("}", "").trim();
            Map<String, Object> map = new LinkedHashMap<>();
            for (String kv : item.split(",")) {
                String[] parts = kv.split(":", 2);
                if (parts.length == 2) {
                    String k = parts[0].trim().replace("\"", "");
                    String v = parts[1].trim().replace("\"", "");
                    map.put(k, v);
                }
            }
            list.add(map);
        }
        return list;
    }
}
