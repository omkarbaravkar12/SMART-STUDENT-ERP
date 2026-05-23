package controllers;
import com.sun.net.httpserver.HttpExchange;
import services.TimetableService;
import utils.Logger;
import java.io.IOException;

public class TimetableController extends BaseController {
    private final TimetableService svc = new TimetableService();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        try {
            String courseId = getQueryParam(exchange, "courseId");
            String semester = getQueryParam(exchange, "semester");
            String teacherId = getQueryParam(exchange, "teacherId");
            sendSuccess(exchange, svc.getTimetable(courseId, semester, teacherId));
        } catch (Exception e) {
            Logger.error("TimetableController: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, e.getMessage());
        }
    }
}
