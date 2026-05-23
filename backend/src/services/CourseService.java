package services;
import database.DatabaseConnection;
import utils.Logger;
import java.sql.*;
import java.util.*;

public class CourseService {
    private final DatabaseConnection db = DatabaseConnection.getInstance();
    public List<Map<String, Object>> getCourses() {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT c.*, d.name AS department_name FROM academic.courses c LEFT JOIN academic.departments d ON c.department_id=d.id WHERE c.is_active=TRUE ORDER BY c.name";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
        } catch (SQLException e) { Logger.error("getCourses: " + e.getMessage()); }
        return rows;
    }
}
