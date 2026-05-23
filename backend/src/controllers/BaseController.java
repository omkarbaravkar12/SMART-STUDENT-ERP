package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * BaseController - Common response utilities for all controllers
 */
public abstract class BaseController implements HttpHandler {

    // HTTP status codes
    protected static final int OK         = 200;
    protected static final int CREATED    = 201;
    protected static final int BAD_REQUEST = 400;
    protected static final int UNAUTHORIZED = 401;
    protected static final int FORBIDDEN  = 403;
    protected static final int NOT_FOUND  = 404;
    protected static final int CONFLICT   = 409;
    protected static final int SERVER_ERROR = 500;

    /**
     * Send JSON response
     */
    protected void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JsonUtil.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendSuccess(HttpExchange exchange, Object data) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        sendJson(exchange, OK, resp);
    }

    protected void sendSuccess(HttpExchange exchange, String message, Object data) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        resp.put("data", data);
        sendJson(exchange, OK, resp);
    }

    protected void sendCreated(HttpExchange exchange, Object data) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        sendJson(exchange, CREATED, resp);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("error", message);
        resp.put("code", statusCode);
        sendJson(exchange, statusCode, resp);
    }

    protected void sendPaginated(HttpExchange exchange, Object data, long total, int page, int limit) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", data);
        Map<String, Object> meta = new HashMap<>();
        meta.put("total", total);
        meta.put("page", page);
        meta.put("limit", limit);
        meta.put("totalPages", (int) Math.ceil((double) total / limit));
        resp.put("meta", meta);
        sendJson(exchange, OK, resp);
    }

    /**
     * Read request body as String
     */
    protected String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parse query parameter from URI
     */
    protected String getQueryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    protected int getIntParam(HttpExchange exchange, String key, int defaultVal) {
        String val = getQueryParam(exchange, key);
        try { return val != null ? Integer.parseInt(val) : defaultVal; }
        catch (NumberFormatException e) { return defaultVal; }
    }

    /**
     * Extract path variable from URL
     * e.g. /api/students/42 -> "42"
     */
    protected String getPathVariable(HttpExchange exchange, String basePath) {
        String path = exchange.getRequestURI().getPath();
        if (path.length() > basePath.length()) {
            return path.substring(basePath.length()).replace("/", "");
        }
        return null;
    }

    /**
     * Handle CORS preflight
     */
    protected boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}
