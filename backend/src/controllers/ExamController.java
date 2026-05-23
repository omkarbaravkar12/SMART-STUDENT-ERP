package controllers;

import com.sun.net.httpserver.HttpExchange;
import services.ExamService;
import utils.JsonUtil;
import utils.Logger;
import java.io.IOException;
import java.util.*;

public class ExamController extends BaseController {
    private final ExamService examService = new ExamService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();
        try {
            if (path.contains("/results")) {
                if ("GET".equals(method))  sendSuccess(exchange, examService.getResults(getQueryParam(exchange,"examId"), getQueryParam(exchange,"studentId")));
                else if ("POST".equals(method)) { Map<String,Object> body = JsonUtil.fromJson(readBody(exchange)); sendSuccess(exchange, "Results saved", examService.saveResults(body)); }
                else sendError(exchange, BAD_REQUEST, "Method not allowed");
            } else {
                String id = extractId(path, "exams");
                if ("GET".equals(method) && id == null)  sendPaginated(exchange, examService.listExams(getQueryParam(exchange,"subjectId"), getQueryParam(exchange,"semester")), 0, 1, 20);
                else if ("GET".equals(method))            sendSuccess(exchange, examService.getExamById(id));
                else if ("POST".equals(method))           sendCreated(exchange, examService.createExam(JsonUtil.fromJson(readBody(exchange))));
                else if ("PUT".equals(method))            sendSuccess(exchange, "Updated", examService.updateExam(id, JsonUtil.fromJson(readBody(exchange))));
                else if ("DELETE".equals(method))         sendSuccess(exchange, "Deleted", null);
                else sendError(exchange, BAD_REQUEST, "Method not allowed");
            }
        } catch (Exception e) {
            Logger.error("ExamController: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, e.getMessage());
        }
    }

    private String extractId(String path, String resource) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++)
            if (resource.equals(parts[i]) && i + 1 < parts.length && !parts[i+1].isEmpty() && !"results".equals(parts[i+1]))
                return parts[i + 1];
        return null;
    }
}
