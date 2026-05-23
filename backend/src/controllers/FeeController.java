package controllers;

import com.sun.net.httpserver.HttpExchange;
import services.FeeService;
import utils.JsonUtil;
import utils.Logger;
import java.io.IOException;
import java.util.*;

public class FeeController extends BaseController {
    private final FeeService feeService = new FeeService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();
        try {
            if (path.contains("/payments") && "GET".equals(method))
                sendSuccess(exchange, feeService.getPayments(getQueryParam(exchange,"studentId"), getQueryParam(exchange,"status")));
            else if (path.contains("/payments") && "POST".equals(method))
                sendCreated(exchange, feeService.recordPayment(JsonUtil.fromJson(readBody(exchange))));
            else if (path.contains("/invoices"))
                sendSuccess(exchange, feeService.getInvoices(getQueryParam(exchange,"studentId")));
            else if (path.contains("/fees") && "GET".equals(method))
                sendSuccess(exchange, feeService.getFeeStructures(getQueryParam(exchange,"courseId")));
            else if (path.contains("/fees") && "POST".equals(method))
                sendCreated(exchange, feeService.createFeeStructure(JsonUtil.fromJson(readBody(exchange))));
            else sendError(exchange, BAD_REQUEST, "Not found");
        } catch (Exception e) {
            Logger.error("FeeController: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, e.getMessage());
        }
    }
}
