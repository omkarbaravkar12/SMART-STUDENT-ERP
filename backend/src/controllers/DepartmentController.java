package controllers;
import com.sun.net.httpserver.HttpExchange;
import utils.Logger;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import database.DatabaseConnection;

public class DepartmentController extends BaseController {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        try {
            List<Map<String, Object>> depts = new ArrayList<>();
            String sql = """
                SELECT d.id, d.code, d.name, d.description, d.established_year, d.is_active,
                       COUNT(DISTINCT s.id) AS student_count,
                       COUNT(DISTINCT t.id) AS teacher_count
                FROM academic.departments d
                LEFT JOIN academic.students s ON d.id=s.department_id AND s.is_active=TRUE
                LEFT JOIN academic.teachers t ON d.id=t.department_id AND t.is_active=TRUE
                WHERE d.is_active=TRUE
                GROUP BY d.id ORDER BY d.name
                """;
            try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                    depts.add(row);
                }
            }
            sendSuccess(exchange, depts);
        } catch (Exception e) { Logger.error("DepartmentController: " + e.getMessage()); sendError(exchange, SERVER_ERROR, e.getMessage()); }
    }
}
