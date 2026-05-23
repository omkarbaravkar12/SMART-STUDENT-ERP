import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import routes.RouterRegistry;
import database.DatabaseConnection;
import utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Main Entry Point - Smart Student ERP Backend Server
 * Starts embedded HTTP server on port 8080
 * Usage: java -cp .:lib/* Main
 */
public class Main {

    private static final int PORT = Integer.parseInt(
        System.getenv().getOrDefault("PORT", "8080")
    );

    public static void main(String[] args) throws IOException {
        Logger.info("╔══════════════════════════════════════════╗");
        Logger.info("║   Smart Student ERP System - Backend      ║");
        Logger.info("║   Version 1.0.0                           ║");
        Logger.info("╚══════════════════════════════════════════╝");

        // Test DB connection
        try {
            DatabaseConnection.getInstance().getConnection();
            Logger.info("✓ Database connection established");
        } catch (Exception e) {
            Logger.error("✗ Database connection failed: " + e.getMessage());
            System.exit(1);
        }

        // Start HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Register all route handlers
        RouterRegistry.register(server);

        // Thread pool for handling concurrent requests
        server.setExecutor(Executors.newFixedThreadPool(20));
        server.start();

        Logger.info("✓ Server running at http://localhost:" + PORT);
        Logger.info("✓ API Base URL: http://localhost:" + PORT + "/api");
        Logger.info("Press Ctrl+C to stop the server");

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Shutting down server...");
            server.stop(0);
            DatabaseConnection.getInstance().close();
            Logger.info("Server stopped. Goodbye!");
        }));
    }
}
