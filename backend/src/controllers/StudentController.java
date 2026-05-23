package controllers;

import com.sun.net.httpserver.HttpExchange;
import services.StudentService;
import utils.JsonUtil;
import utils.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * StudentController - Student CRUD operations
 * GET    /api/students           - List students (paginated)
 * POST   /api/students           - Create student
 * GET    /api/students/{id}      - Get student by ID
 * PUT    /api/students/{id}      - Update student
 * DELETE /api/students/{id}      - Delete student
 * GET    /api/students/{id}/attendance
 * GET    /api/students/{id}/results
 * GET    /api/students/{id}/timetable
 */
public class StudentController extends BaseController {

    private final StudentService studentService;

    public StudentController() {
        this.studentService = new StudentService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            // Sub-resource routing
            if (path.contains("/attendance")) {
                String id = extractIdFromPath(path);
                handleStudentAttendance(exchange, id);
            } else if (path.contains("/results")) {
                String id = extractIdFromPath(path);
                handleStudentResults(exchange, id);
            } else if (path.contains("/timetable")) {
                String id = extractIdFromPath(path);
                handleStudentTimetable(exchange, id);
            } else if (path.equals("/api/students") || path.equals("/api/students/")) {
                if ("GET".equals(method))  handleList(exchange);
                else if ("POST".equals(method)) handleCreate(exchange);
                else sendError(exchange, BAD_REQUEST, "Method not allowed");
            } else {
                String id = extractIdFromPath(path);
                if ("GET".equals(method))    handleGetById(exchange, id);
                else if ("PUT".equals(method))    handleUpdate(exchange, id);
                else if ("DELETE".equals(method)) handleDelete(exchange, id);
                else sendError(exchange, BAD_REQUEST, "Method not allowed");
            }
        } catch (Exception e) {
            Logger.error("StudentController error: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, "Internal server error: " + e.getMessage());
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        int page     = getIntParam(exchange, "page",   1);
        int limit    = getIntParam(exchange, "limit",  20);
        String search    = getQueryParam(exchange, "search");
        String deptId    = getQueryParam(exchange, "departmentId");
        String courseId  = getQueryParam(exchange, "courseId");
        String semester  = getQueryParam(exchange, "semester");

        Map<String, Object> result = studentService.getStudents(page, limit, search, deptId, courseId, semester);
        List<?> data  = (List<?>) result.get("data");
        long total    = ((Number) result.get("total")).longValue();
        sendPaginated(exchange, data, total, page, limit);
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.fromJson(body);

        // Validate required fields
        if (req.get("email") == null || req.get("firstName") == null ||
            req.get("lastName") == null || req.get("courseId") == null) {
            sendError(exchange, BAD_REQUEST, "email, firstName, lastName, courseId are required");
            return;
        }

        Map<String, Object> student = studentService.createStudent(req);
        if (student == null) {
            sendError(exchange, CONFLICT, "Student with this email already exists");
            return;
        }
        sendCreated(exchange, student);
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        if (id == null || id.isBlank()) {
            sendError(exchange, BAD_REQUEST, "Student ID is required");
            return;
        }
        Map<String, Object> student = studentService.getStudentById(id);
        if (student == null) {
            sendError(exchange, NOT_FOUND, "Student not found");
            return;
        }
        sendSuccess(exchange, student);
    }

    private void handleUpdate(HttpExchange exchange, String id) throws IOException {
        if (id == null || id.isBlank()) {
            sendError(exchange, BAD_REQUEST, "Student ID is required");
            return;
        }
        String body = readBody(exchange);
        Map<String, Object> req = JsonUtil.fromJson(body);
        boolean updated = studentService.updateStudent(id, req);
        if (!updated) {
            sendError(exchange, NOT_FOUND, "Student not found");
            return;
        }
        sendSuccess(exchange, "Student updated successfully", null);
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        if (id == null || id.isBlank()) {
            sendError(exchange, BAD_REQUEST, "Student ID is required");
            return;
        }
        // Check role - only admin can delete
        Object userRole = exchange.getAttribute("userRole");
        if (!"admin".equals(userRole)) {
            sendError(exchange, FORBIDDEN, "Only admins can delete students");
            return;
        }
        boolean deleted = studentService.deleteStudent(id);
        if (!deleted) {
            sendError(exchange, NOT_FOUND, "Student not found");
            return;
        }
        sendSuccess(exchange, "Student deleted successfully", null);
    }

    private void handleStudentAttendance(HttpExchange exchange, String id) throws IOException {
        String subjectId = getQueryParam(exchange, "subjectId");
        String from      = getQueryParam(exchange, "from");
        String to        = getQueryParam(exchange, "to");
        List<Map<String, Object>> attendance = studentService.getStudentAttendance(id, subjectId, from, to);
        sendSuccess(exchange, attendance);
    }

    private void handleStudentResults(HttpExchange exchange, String id) throws IOException {
        String academicYear = getQueryParam(exchange, "academicYear");
        String semester     = getQueryParam(exchange, "semester");
        Map<String, Object> results = studentService.getStudentResults(id, academicYear, semester);
        sendSuccess(exchange, results);
    }

    private void handleStudentTimetable(HttpExchange exchange, String id) throws IOException {
        Map<String, Object> timetable = studentService.getStudentTimetable(id);
        sendSuccess(exchange, timetable);
    }

    private String extractIdFromPath(String path) {
        // /api/students/42/attendance -> "42"
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("students".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
