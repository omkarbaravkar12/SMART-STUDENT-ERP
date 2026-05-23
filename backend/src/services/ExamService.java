package services;

import database.DatabaseConnection;
import utils.Logger;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class ExamService {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<Map<String, Object>> listExams(String subjectId, String semester) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.id, e.name, e.exam_type, e.academic_year, e.semester,
                   e.exam_date, e.start_time, e.end_time, e.room,
                   e.total_marks, e.passing_marks, e.is_published,
                   s.name AS subject_name, s.code AS subject_code
            FROM academic.exams e
            JOIN academic.subjects s ON e.subject_id = s.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (subjectId != null && !subjectId.isBlank()) { sql.append(" AND e.subject_id=?"); params.add(Integer.parseInt(subjectId)); }
        if (semester  != null && !semester.isBlank())  { sql.append(" AND e.semester=?::semester_type"); params.add(semester); }
        sql.append(" ORDER BY e.exam_date DESC");
        return query(sql.toString(), params);
    }

    public Map<String, Object> getExamById(String id) {
        List<Map<String,Object>> rows = query("SELECT e.*, s.name AS subject_name, s.code AS subject_code FROM academic.exams e JOIN academic.subjects s ON e.subject_id=s.id WHERE e.id=?", List.of(Integer.parseInt(id)));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> createExam(Map<String, Object> data) {
        String sql = """
            INSERT INTO academic.exams (name, subject_id, exam_type, academic_year, semester, exam_date, start_time, end_time, room, total_marks, passing_marks)
            VALUES (?, ?, ?::exam_type, ?, ?::semester_type, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, (String)data.get("name"));
            ps.setInt(2, Integer.parseInt(data.get("subjectId").toString()));
            ps.setString(3, (String)data.get("examType"));
            ps.setString(4, (String)data.get("academicYear"));
            ps.setString(5, (String)data.get("semester"));
            ps.setDate(6, Date.valueOf((String)data.get("examDate")));
            ps.setTime(7, Time.valueOf((String)data.get("startTime") + ":00"));
            ps.setTime(8, Time.valueOf((String)data.get("endTime") + ":00"));
            ps.setString(9, (String)data.getOrDefault("room","TBD"));
            ps.setInt(10, Integer.parseInt(data.getOrDefault("totalMarks","100").toString()));
            ps.setInt(11, Integer.parseInt(data.getOrDefault("passingMarks","40").toString()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return getExamById(rs.getString("id"));
        } catch (SQLException e) { Logger.error("createExam: " + e.getMessage()); }
        return null;
    }

    public boolean updateExam(String id, Map<String, Object> data) {
        String sql = "UPDATE academic.exams SET is_published=? WHERE id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, Boolean.parseBoolean(data.getOrDefault("isPublished","false").toString()));
            ps.setInt(2, Integer.parseInt(id));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { Logger.error("updateExam: " + e.getMessage()); return false; }
    }

    public List<Map<String, Object>> getResults(String examId, String studentId) {
        StringBuilder sql = new StringBuilder("""
            SELECT er.id, er.marks_obtained, er.grade, er.grade_points, er.is_absent, er.remarks,
                   s.student_id AS roll_no, u.first_name || ' ' || u.last_name AS student_name,
                   e.total_marks, e.name AS exam_name
            FROM academic.exam_results er
            JOIN academic.students s ON er.student_id = s.id
            JOIN auth.users u ON s.user_id = u.id
            JOIN academic.exams e ON er.exam_id = e.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (examId    != null && !examId.isBlank())    { sql.append(" AND er.exam_id=?");    params.add(Integer.parseInt(examId)); }
        if (studentId != null && !studentId.isBlank()) { sql.append(" AND er.student_id=?"); params.add(Integer.parseInt(studentId)); }
        sql.append(" ORDER BY student_name");
        return query(sql.toString(), params);
    }

    public boolean saveResults(Map<String, Object> data) {
        String sql = """
            INSERT INTO academic.exam_results (exam_id, student_id, marks_obtained, is_absent, remarks)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (exam_id, student_id) DO UPDATE
            SET marks_obtained=EXCLUDED.marks_obtained, is_absent=EXCLUDED.is_absent, remarks=EXCLUDED.remarks
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(data.get("examId").toString()));
            ps.setInt(2, Integer.parseInt(data.get("studentId").toString()));
            ps.setObject(3, data.get("marksObtained") != null ? Double.parseDouble(data.get("marksObtained").toString()) : null);
            ps.setBoolean(4, Boolean.parseBoolean(data.getOrDefault("isAbsent","false").toString()));
            ps.setString(5, (String) data.getOrDefault("remarks", null));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { Logger.error("saveResults: " + e.getMessage()); return false; }
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
        } catch (SQLException e) { Logger.error("ExamService query: " + e.getMessage()); }
        return rows;
    }
}
