package services;

import database.DatabaseConnection;
import utils.Logger;
import java.sql.*;
import java.util.*;

public class TimetableService {
    private final DatabaseConnection db = DatabaseConnection.getInstance();
    public List<Map<String, Object>> getTimetable(String courseId, String semester, String teacherId) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.id, t.day_of_week, t.start_time, t.end_time, t.room, t.semester,
                   sub.code AS subject_code, sub.name AS subject_name,
                   c.name AS course_name,
                   u.first_name || ' ' || u.last_name AS teacher_name
            FROM academic.timetables t
            JOIN academic.subjects sub ON t.subject_id = sub.id
            JOIN academic.courses c ON t.course_id = c.id
            LEFT JOIN academic.teachers tc ON t.teacher_id = tc.id
            LEFT JOIN auth.users u ON tc.user_id = u.id
            WHERE t.is_active=TRUE
            """);
        List<Object> params = new ArrayList<>();
        if (courseId  != null && !courseId.isBlank())  { sql.append(" AND t.course_id=?");  params.add(Integer.parseInt(courseId)); }
        if (semester  != null && !semester.isBlank())  { sql.append(" AND t.semester=?");   params.add(Integer.parseInt(semester)); }
        if (teacherId != null && !teacherId.isBlank()) { sql.append(" AND t.teacher_id=?"); params.add(Integer.parseInt(teacherId)); }
        sql.append(" ORDER BY CASE t.day_of_week WHEN 'Monday' THEN 1 WHEN 'Tuesday' THEN 2 WHEN 'Wednesday' THEN 3 WHEN 'Thursday' THEN 4 WHEN 'Friday' THEN 5 ELSE 6 END, t.start_time");
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
        } catch (SQLException e) { Logger.error("getTimetable: " + e.getMessage()); }
        return rows;
    }
}
