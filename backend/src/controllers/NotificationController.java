package controllers;

import com.sun.net.httpserver.HttpExchange;
import services.NotificationService;
import utils.JsonUtil;
import utils.Logger;
import java.io.IOException;
import java.util.*;

public class NotificationController extends BaseController {
    private final NotificationService svc = new NotificationService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();
        String userId = Objects.toString(exchange.getAttribute("userId"), null);
        try {
            if ("GET".equals(method))  sendSuccess(exchange, svc.getUserNotifications(userId));
            else if ("POST".equals(method)) {
                Map<String,Object> body = JsonUtil.fromJson(readBody(exchange));
                body.put("createdBy", userId);
                sendCreated(exchange, svc.createNotification(body));
            } else sendError(exchange, BAD_REQUEST, "Method not allowed");
        } catch (Exception e) {
            Logger.error("NotificationController: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, e.getMessage());
        }
    }
}
