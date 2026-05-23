package middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import services.AuthService;
import utils.JsonUtil;
import utils.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * AuthMiddleware - Validates JWT token and injects user info into exchange
 * Wraps any HttpHandler with authentication check
 */
public class AuthMiddleware implements HttpHandler {

    private final HttpHandler next;
    private final AuthService authService;

    // Routes that don't require authentication
    private static final String[] PUBLIC_PATHS = {
        "/api/auth/login",
        "/api/auth/forgot-password",
        "/api/auth/verify-otp",
        "/api/health"
    };

    public AuthMiddleware(HttpHandler next) {
        this.next = next;
        this.authService = new AuthService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Handle CORS preflight
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Check if path is public
        String path = exchange.getRequestURI().getPath();
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath)) {
                next.handle(exchange);
                return;
            }
        }

        // Extract and validate token
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(exchange, "Missing or invalid authorization header");
            return;
        }

        String token = authHeader.substring(7);
        Map<String, Object> user = authService.validateToken(token);

        if (user == null) {
            sendUnauthorized(exchange, "Invalid or expired token");
            return;
        }

        // Inject user info into exchange attributes
        exchange.setAttribute("currentUser", user);
        exchange.setAttribute("userId",      user.get("id"));
        exchange.setAttribute("userRole",    user.get("role"));
        exchange.setAttribute("userEmail",   user.get("email"));

        // Pass to next handler
        next.handle(exchange);
    }

    private void sendUnauthorized(HttpExchange exchange, String message) throws IOException {
        Map<String, Object> resp = Map.of(
            "success", false,
            "error",   message,
            "code",    401
        );
        String json  = JsonUtil.toJson(resp);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(401, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
