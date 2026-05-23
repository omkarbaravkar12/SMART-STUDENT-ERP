package controllers;

import com.sun.net.httpserver.HttpExchange;
import services.AuthService;
import utils.JsonUtil;
import utils.Logger;

import java.io.IOException;
import java.util.Map;

/**
 * AuthController - Handles authentication endpoints
 * POST /api/auth/login
 * POST /api/auth/logout
 * POST /api/auth/forgot-password
 * POST /api/auth/verify-otp
 * GET  /api/auth/me
 */
public class AuthController extends BaseController {

    private final String action;
    private final AuthService authService;

    public AuthController(String action) {
        this.action = action;
        this.authService = new AuthService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;

        try {
            switch (action) {
                case "login"          -> handleLogin(exchange);
                case "logout"         -> handleLogout(exchange);
                case "forgotPassword" -> handleForgotPassword(exchange);
                case "verifyOtp"      -> handleVerifyOtp(exchange);
                case "me"             -> handleMe(exchange);
                default               -> sendError(exchange, NOT_FOUND, "Unknown auth action");
            }
        } catch (Exception e) {
            Logger.error("AuthController error: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, "Internal server error");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, BAD_REQUEST, "Method not allowed");
            return;
        }
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.fromJson(body);

        String email    = (String) req.get("email");
        String password = (String) req.get("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            sendError(exchange, BAD_REQUEST, "Email and password are required");
            return;
        }

        Map<String, Object> result = authService.login(email.trim(), password,
            exchange.getRemoteAddress().getAddress().getHostAddress(),
            exchange.getRequestHeaders().getFirst("User-Agent"));

        if (result == null) {
            sendError(exchange, UNAUTHORIZED, "Invalid email or password");
        } else {
            sendSuccess(exchange, "Login successful", result);
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, BAD_REQUEST, "Method not allowed");
            return;
        }
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        sendSuccess(exchange, "Logged out successfully", null);
    }

    private void handleForgotPassword(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, BAD_REQUEST, "Method not allowed");
            return;
        }
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.fromJson(body);
        String email = (String) req.get("email");
        if (email == null || email.isBlank()) {
            sendError(exchange, BAD_REQUEST, "Email is required");
            return;
        }
        authService.sendOtp(email.trim());
        sendSuccess(exchange, "OTP sent to your email (check logs in dev mode)", null);
    }

    private void handleVerifyOtp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, BAD_REQUEST, "Method not allowed");
            return;
        }
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.fromJson(body);
        String email    = (String) req.get("email");
        String otp      = (String) req.get("otp");
        String newPass  = (String) req.get("newPassword");

        if (email == null || otp == null || newPass == null) {
            sendError(exchange, BAD_REQUEST, "Email, OTP, and new password are required");
            return;
        }
        boolean success = authService.verifyOtpAndResetPassword(email.trim(), otp.trim(), newPass);
        if (success) {
            sendSuccess(exchange, "Password reset successfully", null);
        } else {
            sendError(exchange, BAD_REQUEST, "Invalid or expired OTP");
        }
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, BAD_REQUEST, "Method not allowed");
            return;
        }
        // User info injected by AuthMiddleware via exchange attribute
        Object user = exchange.getAttribute("currentUser");
        if (user == null) {
            sendError(exchange, UNAUTHORIZED, "Not authenticated");
            return;
        }
        sendSuccess(exchange, user);
    }
}
