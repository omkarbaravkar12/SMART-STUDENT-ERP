package controllers;

import com.sun.net.httpserver.HttpExchange;
import services.AttendanceService;
import utils.JsonUtil;
import utils.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AttendanceController
 * GET  /api/attendance?subjectId=&date=&classId=
 * POST /api/attendance          - Mark/bulk attendance
 * GET  /api/attendance/summary  - Attendance summary per student/subject
 */
public class AttendanceController extends BaseController {

    private final AttendanceService attendanceService;

    public AttendanceController() {
        this.attendanceService = new AttendanceService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if (path.contains("/summary")) {
                handleSummary(exchange);
            } else if ("GET".equals(method)) {
                handleList(exchange);
            } else if ("POST".equals(method)) {
                handleMark(exchange);
            } else {
                sendError(exchange, BAD_REQUEST, "Method not allowed");
            }
        } catch (Exception e) {
            Logger.error("AttendanceController: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, "Internal server error");
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        String subjectId = getQueryParam(exchange, "subjectId");
        String date      = getQueryParam(exchange, "date");
        String studentId = getQueryParam(exchange, "studentId");
        List<Map<String, Object>> records = attendanceService.getAttendance(subjectId, date, studentId);
        sendSuccess(exchange, records);
    }

    private void handleMark(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.fromJson(body);
        Object teacherId = exchange.getAttribute("userId");
        boolean ok = attendanceService.markAttendance(req, teacherId != null ? teacherId.toString() : null);
        if (ok) sendSuccess(exchange, "Attendance marked successfully", null);
        else    sendError(exchange, SERVER_ERROR, "Failed to mark attendance");
    }

    private void handleSummary(HttpExchange exchange) throws IOException {
        String studentId = getQueryParam(exchange, "studentId");
        String subjectId = getQueryParam(exchange, "subjectId");
        List<Map<String, Object>> summary = attendanceService.getAttendanceSummary(studentId, subjectId);
        sendSuccess(exchange, summary);
    }
}
