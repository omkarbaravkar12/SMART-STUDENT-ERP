package controllers;
import com.sun.net.httpserver.HttpExchange;
import services.CourseService;
import utils.Logger;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import database.DatabaseConnection;

public class SubjectController extends BaseController {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        try {
            List<Map<String, Object>> subjects = new ArrayList<>();
            String sql = "SELECT s.*, d.name AS department_name FROM academic.subjects s LEFT JOIN academic.departments d ON s.department_id=d.id ORDER BY s.code";
            try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                    subjects.add(row);
                }
            }
            sendSuccess(exchange, subjects);
        } catch (Exception e) { Logger.error("SubjectController: " + e.getMessage()); sendError(exchange, SERVER_ERROR, e.getMessage()); }
    }
}
