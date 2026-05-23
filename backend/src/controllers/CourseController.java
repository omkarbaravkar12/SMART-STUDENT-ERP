package controllers;
import com.sun.net.httpserver.HttpExchange;
import services.CourseService;
import utils.Logger;
import java.io.IOException;

public class CourseController extends BaseController {
    private final CourseService svc = new CourseService();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        try { sendSuccess(exchange, svc.getCourses()); }
        catch (Exception e) { Logger.error("CourseController: " + e.getMessage()); sendError(exchange, SERVER_ERROR, e.getMessage()); }
    }
}
