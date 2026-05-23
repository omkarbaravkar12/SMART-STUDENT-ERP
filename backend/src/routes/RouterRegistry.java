package routes;

import com.sun.net.httpserver.HttpServer;
import controllers.*;
import middleware.CorsMiddleware;
import middleware.AuthMiddleware;
import middleware.LoggingMiddleware;

/**
 * RouterRegistry - Central route registration
 * All API endpoints are registered here
 */
public class RouterRegistry {

    public static void register(HttpServer server) {

        // ─── Auth Routes ─────────────────────────────────
        server.createContext("/api/auth/login",          new LoggingMiddleware(new AuthController("login")));
        server.createContext("/api/auth/logout",         new LoggingMiddleware(new AuthMiddleware(new AuthController("logout"))));
        server.createContext("/api/auth/forgot-password",new LoggingMiddleware(new AuthController("forgotPassword")));
        server.createContext("/api/auth/verify-otp",     new LoggingMiddleware(new AuthController("verifyOtp")));
        server.createContext("/api/auth/me",             new LoggingMiddleware(new AuthMiddleware(new AuthController("me"))));

        // ─── Student Routes ───────────────────────────────
        server.createContext("/api/students",            new LoggingMiddleware(new AuthMiddleware(new StudentController())));
        server.createContext("/api/students/",           new LoggingMiddleware(new AuthMiddleware(new StudentController())));

        // ─── Teacher Routes ───────────────────────────────
        server.createContext("/api/teachers",            new LoggingMiddleware(new AuthMiddleware(new TeacherController())));
        server.createContext("/api/teachers/",           new LoggingMiddleware(new AuthMiddleware(new TeacherController())));

        // ─── Course & Subject Routes ──────────────────────
        server.createContext("/api/courses",             new LoggingMiddleware(new AuthMiddleware(new CourseController())));
        server.createContext("/api/subjects",            new LoggingMiddleware(new AuthMiddleware(new SubjectController())));
        server.createContext("/api/departments",         new LoggingMiddleware(new AuthMiddleware(new DepartmentController())));

        // ─── Attendance Routes ────────────────────────────
        server.createContext("/api/attendance",          new LoggingMiddleware(new AuthMiddleware(new AttendanceController())));
        server.createContext("/api/attendance/summary",  new LoggingMiddleware(new AuthMiddleware(new AttendanceController())));

        // ─── Examination Routes ───────────────────────────
        server.createContext("/api/exams",               new LoggingMiddleware(new AuthMiddleware(new ExamController())));
        server.createContext("/api/exams/",              new LoggingMiddleware(new AuthMiddleware(new ExamController())));
        server.createContext("/api/results",             new LoggingMiddleware(new AuthMiddleware(new ExamController())));

        // ─── Fee Management Routes ────────────────────────
        server.createContext("/api/fees",                new LoggingMiddleware(new AuthMiddleware(new FeeController())));
        server.createContext("/api/payments",            new LoggingMiddleware(new AuthMiddleware(new FeeController())));
        server.createContext("/api/invoices",            new LoggingMiddleware(new AuthMiddleware(new FeeController())));

        // ─── Notification Routes ──────────────────────────
        server.createContext("/api/notifications",       new LoggingMiddleware(new AuthMiddleware(new NotificationController())));

        // ─── Timetable Routes ─────────────────────────────
        server.createContext("/api/timetable",           new LoggingMiddleware(new AuthMiddleware(new TimetableController())));

        // ─── Library Routes ───────────────────────────────
        server.createContext("/api/library/books",       new LoggingMiddleware(new AuthMiddleware(new LibraryController())));
        server.createContext("/api/library/borrow",      new LoggingMiddleware(new AuthMiddleware(new LibraryController())));

        // ─── Analytics Routes ─────────────────────────────
        server.createContext("/api/analytics/dashboard", new LoggingMiddleware(new AuthMiddleware(new AnalyticsController())));
        server.createContext("/api/analytics/students",  new LoggingMiddleware(new AuthMiddleware(new AnalyticsController())));
        server.createContext("/api/analytics/fees",      new LoggingMiddleware(new AuthMiddleware(new AnalyticsController())));
        server.createContext("/api/analytics/attendance",new LoggingMiddleware(new AuthMiddleware(new AnalyticsController())));

        // ─── Health Check ─────────────────────────────────
        server.createContext("/api/health", exchange -> {
            String response = "{\"status\":\"UP\",\"service\":\"Smart Student ERP\",\"version\":\"1.0.0\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        });

        // ─── CORS preflight for all routes ────────────────
        server.createContext("/api/", new CorsMiddleware(null));
    }
}
