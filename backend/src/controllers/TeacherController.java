package controllers;
import com.sun.net.httpserver.HttpExchange;
import services.TeacherService;
import utils.JsonUtil;
import utils.Logger;
import java.io.IOException;
import java.util.Map;

public class TeacherController extends BaseController {
    private final TeacherService svc = new TeacherService();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();
        String path   = exchange.getRequestURI().getPath();
        try {
            String[] parts = path.split("/");
            String id = null;
            for (int i = 0; i < parts.length; i++)
                if ("teachers".equals(parts[i]) && i+1 < parts.length && !parts[i+1].isEmpty()) id = parts[i+1];
            if ("GET".equals(method) && id == null)
                sendPaginated(exchange, svc.listTeachers(getQueryParam(exchange,"departmentId")), svc.totalTeachers(), getIntParam(exchange,"page",1), 20);
            else if ("GET".equals(method))
                sendSuccess(exchange, svc.getTeacherById(id));
            else if ("POST".equals(method))
                sendCreated(exchange, svc.createTeacher(JsonUtil.fromJson(readBody(exchange))));
            else if ("PUT".equals(method))
                sendSuccess(exchange, "Updated", svc.updateTeacher(id, JsonUtil.fromJson(readBody(exchange))));
            else sendError(exchange, BAD_REQUEST, "Method not allowed");
        } catch (Exception e) {
            Logger.error("TeacherController: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, e.getMessage());
        }
    }
}
