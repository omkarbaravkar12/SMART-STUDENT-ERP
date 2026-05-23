package controllers;

import com.sun.net.httpserver.HttpExchange;
import services.AnalyticsService;
import utils.Logger;

import java.io.IOException;
import java.util.Map;

/**
 * AnalyticsController - Dashboard analytics endpoints
 * GET /api/analytics/dashboard  - Admin overview stats
 * GET /api/analytics/students   - Student performance stats
 * GET /api/analytics/fees       - Fee collection stats
 * GET /api/analytics/attendance - Attendance stats
 */
public class AnalyticsController extends BaseController {

    private final AnalyticsService analyticsService;

    public AnalyticsController() {
        this.analyticsService = new AnalyticsService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, BAD_REQUEST, "Method not allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        try {
            if (path.contains("/dashboard")) {
                sendSuccess(exchange, analyticsService.getDashboardStats());
            } else if (path.contains("/students")) {
                sendSuccess(exchange, analyticsService.getStudentAnalytics());
            } else if (path.contains("/fees")) {
                sendSuccess(exchange, analyticsService.getFeeAnalytics());
            } else if (path.contains("/attendance")) {
                sendSuccess(exchange, analyticsService.getAttendanceAnalytics());
            } else {
                sendError(exchange, NOT_FOUND, "Analytics endpoint not found");
            }
        } catch (Exception e) {
            Logger.error("AnalyticsController error: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, "Failed to fetch analytics");
        }
    }
}
