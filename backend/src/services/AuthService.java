package services;

import database.DatabaseConnection;
import utils.Logger;
import utils.PasswordUtil;
import utils.TokenUtil;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * AuthService - Business logic for authentication
 */
public class AuthService {

    private final DatabaseConnection db;

    public AuthService() {
        this.db = DatabaseConnection.getInstance();
    }

    /**
     * Login: validate credentials, create session, log login
     * @return token + user info map, or null if invalid
     */
    public Map<String, Object> login(String email, String password, String ipAddress, String userAgent) {
        String sql = """
            SELECT id, email, password_hash, role, first_name, last_name,
                   phone, profile_photo, is_active, is_email_verified
            FROM auth.users
            WHERE email = ?
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                logLogin(null, ipAddress, userAgent, "failed_user_not_found");
                return null;
            }

            String userId       = rs.getString("id");
            String passwordHash = rs.getString("password_hash");
            boolean isActive    = rs.getBoolean("is_active");

            if (!isActive) {
                logLogin(userId, ipAddress, userAgent, "failed_account_disabled");
                return null;
            }

            if (!PasswordUtil.verify(password, passwordHash)) {
                logLogin(userId, ipAddress, userAgent, "failed_wrong_password");
                return null;
            }

            // Create session token
            String token = TokenUtil.generateToken(userId, rs.getString("role"));
            saveSession(userId, token, ipAddress);

            // Update last login
            updateLastLogin(userId);

            // Log success
            logLogin(userId, ipAddress, userAgent, "success");

            // Build response
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id",           userId);
            user.put("email",        rs.getString("email"));
            user.put("role",         rs.getString("role"));
            user.put("firstName",    rs.getString("first_name"));
            user.put("lastName",     rs.getString("last_name"));
            user.put("phone",        rs.getString("phone"));
            user.put("profilePhoto", rs.getString("profile_photo"));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("token", token);
            result.put("user",  user);
            return result;

        } catch (SQLException e) {
            Logger.error("Login error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Logout: invalidate session token
     */
    public void logout(String token) {
        String sql = "DELETE FROM auth.sessions WHERE token = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Logout error: " + e.getMessage());
        }
    }

    /**
     * Validate session token
     * @return user info map if valid, null if not
     */
    public Map<String, Object> validateToken(String token) {
        if (token == null || token.isBlank()) return null;

        String sql = """
            SELECT u.id, u.email, u.role, u.first_name, u.last_name,
                   u.is_active, s.expires_at
            FROM auth.sessions s
            JOIN auth.users u ON s.user_id = u.id
            WHERE s.token = ?
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            Timestamp expiresAt = rs.getTimestamp("expires_at");
            if (expiresAt != null && expiresAt.toInstant().isBefore(Instant.now())) {
                logout(token); // Clean up expired session
                return null;
            }

            if (!rs.getBoolean("is_active")) return null;

            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id",        rs.getString("id"));
            user.put("email",     rs.getString("email"));
            user.put("role",      rs.getString("role"));
            user.put("firstName", rs.getString("first_name"));
            user.put("lastName",  rs.getString("last_name"));
            return user;

        } catch (SQLException e) {
            Logger.error("Token validation error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generate and store OTP for password reset
     */
    public void sendOtp(String email) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(15, ChronoUnit.MINUTES));

        String sql = "UPDATE auth.users SET otp_code = ?, otp_expires_at = ? WHERE email = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setTimestamp(2, expiresAt);
            ps.setString(3, email);
            ps.executeUpdate();
            // In production: send via email
            Logger.info("[DEV] OTP for " + email + ": " + otp + " (expires 15min)");
        } catch (SQLException e) {
            Logger.error("Send OTP error: " + e.getMessage());
        }
    }

    /**
     * Verify OTP and reset password
     */
    public boolean verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        String verifySql = """
            SELECT id FROM auth.users
            WHERE email = ? AND otp_code = ? AND otp_expires_at > NOW()
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(verifySql)) {
            ps.setString(1, email);
            ps.setString(2, otp);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;

            String newHash = PasswordUtil.hash(newPassword);
            String updateSql = """
                UPDATE auth.users
                SET password_hash = ?, otp_code = NULL, otp_expires_at = NULL
                WHERE email = ?
                """;
            try (PreparedStatement upd = db.getConnection().prepareStatement(updateSql)) {
                upd.setString(1, newHash);
                upd.setString(2, email);
                upd.executeUpdate();
            }
            return true;

        } catch (SQLException e) {
            Logger.error("OTP verify error: " + e.getMessage());
            return false;
        }
    }

    // ─── Private helpers ──────────────────────────────────

    private void saveSession(String userId, String token, String ipAddress) throws SQLException {
        // Sessions expire in 24 hours
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS));
        String sql = """
            INSERT INTO auth.sessions (user_id, token, ip_address, expires_at)
            VALUES (?, ?, ?::inet, ?)
            ON CONFLICT (token) DO NOTHING
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            ps.setString(2, token);
            ps.setString(3, ipAddress != null ? ipAddress : "127.0.0.1");
            ps.setTimestamp(4, expiresAt);
            ps.executeUpdate();
        }
    }

    private void updateLastLogin(String userId) {
        String sql = "UPDATE auth.users SET last_login = NOW() WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(userId));
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Update last login error: " + e.getMessage());
        }
    }

    private void logLogin(String userId, String ipAddress, String userAgent, String status) {
        String sql = """
            INSERT INTO auth.login_logs (user_id, ip_address, user_agent, status)
            VALUES (?::uuid, ?::inet, ?, ?)
            """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, ipAddress != null ? ipAddress : "127.0.0.1");
            ps.setString(3, userAgent);
            ps.setString(4, status);
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Login log error: " + e.getMessage());
        }
    }
}
