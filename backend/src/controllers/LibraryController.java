package controllers;
import com.sun.net.httpserver.HttpExchange;
import services.LibraryService;
import utils.JsonUtil;
import utils.Logger;
import java.io.IOException;
import java.util.*;

public class LibraryController extends BaseController {
    private final LibraryService svc = new LibraryService();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (handleCors(exchange)) return;
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();
        try {
            if (path.contains("/borrow") && "POST".equals(method))
                sendCreated(exchange, svc.borrowBook(JsonUtil.fromJson(readBody(exchange))));
            else if (path.contains("/borrow") && "PUT".equals(method))
                sendSuccess(exchange, "Returned", svc.returnBook(JsonUtil.fromJson(readBody(exchange))));
            else if ("GET".equals(method))
                sendPaginated(exchange, svc.searchBooks(getQueryParam(exchange,"q"), getQueryParam(exchange,"category")),
                    svc.totalBooks(), getIntParam(exchange,"page",1), getIntParam(exchange,"limit",20));
            else if ("POST".equals(method))
                sendCreated(exchange, svc.addBook(JsonUtil.fromJson(readBody(exchange))));
            else sendError(exchange, BAD_REQUEST, "Method not allowed");
        } catch (Exception e) {
            Logger.error("LibraryController: " + e.getMessage());
            sendError(exchange, SERVER_ERROR, e.getMessage());
        }
    }
}
