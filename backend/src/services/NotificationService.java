package services;

import database.DatabaseConnection;
import utils.Logger;
import java.sql.*;
import java.util.*;

public class NotificationService {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<Map<String, Object>> getUserNotifications(String userId) {
        String sql = """
            SELECT n.id, n.title, n.message, n.type, n.created_at,
                   un.is_read, un.read_at
            FROM communication.user_notifications un
            JOIN communication.notifications n ON un.notification_id = n.id
            WHERE un.user_id = ?::uuid
            ORDER BY n.created_at DESC LIMIT 50
            """;
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",        rs.getLong("id"));
                row.put("title",     rs.getString("title"));
                row.put("message",   rs.getString("message"));
                row.put("type",      rs.getString("type"));
                row.put("isRead",    rs.getBoolean("is_read"));
                row.put("readAt",    rs.getTimestamp("read_at") != null ? rs.getTimestamp("read_at").toInstant().toString() : null);
                row.put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
                rows.add(row);
            }
        } catch (SQLException e) { Logger.error("getUserNotifications: " + e.getMessage()); }
        return rows;
    }

    public Map<String, Object> createNotification(Map<String, Object> data) {
        String sql = """
            INSERT INTO communication.notifications
              (title, message, type, target_role, is_broadcast, created_by)
            VALUES (?, ?, ?::notification_type, ?::user_role, ?, ?::uuid)
            RETURNING id
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, (String) data.get("title"));
            ps.setString(2, (String) data.get("message"));
            ps.setString(3, Objects.toString(data.getOrDefault("type", "announcement")));
            ps.setString(4, (String) data.get("targetRole"));
            ps.setBoolean(5, Boolean.parseBoolean(Objects.toString(data.getOrDefault("isBroadcast","true"))));
            ps.setString(6, (String) data.get("createdBy"));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Map.of("id", rs.getLong("id"), "message", "Notification sent");
        } catch (SQLException e) { Logger.error("createNotification: " + e.getMessage()); }
        return null;
    }

    public boolean markAsRead(String userId, Long notificationId) {
        String sql = """
            UPDATE communication.user_notifications
            SET is_read=TRUE, read_at=NOW()
            WHERE user_id=?::uuid AND notification_id=?
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setLong(2, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { Logger.error("markAsRead: " + e.getMessage()); return false; }
    }
}
