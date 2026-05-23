package middleware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import utils.Logger;

import java.io.IOException;

/**
 * LoggingMiddleware - Logs all incoming requests with timing
 */
public class LoggingMiddleware implements HttpHandler {

    private final HttpHandler next;

    public LoggingMiddleware(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long start = System.currentTimeMillis();
        try {
            next.handle(exchange);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status    = exchange.getResponseCode();
            Logger.request(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                status < 0 ? 0 : status,
                duration
            );
        }
    }
}
