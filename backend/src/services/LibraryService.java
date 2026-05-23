package services;
import database.DatabaseConnection;
import utils.Logger;
import java.sql.*;
import java.util.*;

public class LibraryService {
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public List<Map<String, Object>> searchBooks(String q, String category) {
        StringBuilder sql = new StringBuilder("SELECT b.*, s.name AS subject_name FROM library.books b LEFT JOIN academic.subjects s ON b.subject_id=s.id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (LOWER(b.title) LIKE ? OR LOWER(b.author) LIKE ? OR b.isbn LIKE ?)");
            String p = "%" + q.toLowerCase() + "%"; params.add(p); params.add(p); params.add(p);
        }
        if (category != null && !category.isBlank()) { sql.append(" AND b.category=?"); params.add(category); }
        sql.append(" ORDER BY b.title LIMIT 50");
        return query(sql.toString(), params);
    }

    public long totalBooks() {
        try (PreparedStatement ps = db.getConnection().prepareStatement("SELECT COUNT(*) FROM library.books");
             ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getLong(1); }
        catch (SQLException e) { Logger.error(e.getMessage()); }
        return 0;
    }

    public Map<String, Object> addBook(Map<String, Object> data) {
        String sql = "INSERT INTO library.books (isbn,title,author,publisher,published_year,category,total_copies,available_copies) VALUES (?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1,(String)data.get("isbn")); ps.setString(2,(String)data.get("title"));
            ps.setString(3,(String)data.get("author")); ps.setString(4,(String)data.getOrDefault("publisher",null));
            ps.setObject(5,data.get("publishedYear")); ps.setString(6,(String)data.getOrDefault("category","General"));
            int copies = Integer.parseInt(data.getOrDefault("totalCopies","1").toString());
            ps.setInt(7,copies); ps.setInt(8,copies);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Map.of("id", rs.getInt("id"), "message", "Book added");
        } catch (SQLException e) { Logger.error("addBook: " + e.getMessage()); }
        return null;
    }

    public Map<String, Object> borrowBook(Map<String, Object> data) {
        String sql = "INSERT INTO library.borrow_records (book_id,user_id,due_date) VALUES (?,?::uuid, CURRENT_DATE + 14) RETURNING id";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(data.get("bookId").toString()));
            ps.setString(2, (String) data.get("userId"));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Map.of("id", rs.getLong("id"), "dueDate", "14 days from today");
        } catch (SQLException e) { Logger.error("borrowBook: " + e.getMessage()); }
        return null;
    }

    public boolean returnBook(Map<String, Object> data) {
        String sql = "UPDATE library.borrow_records SET returned_at=CURRENT_DATE WHERE id=? AND status='borrowed'";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, Long.parseLong(data.get("borrowId").toString()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { Logger.error("returnBook: " + e.getMessage()); return false; }
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
        } catch (SQLException e) { Logger.error("LibraryService query: " + e.getMessage()); }
        return rows;
    }
}
