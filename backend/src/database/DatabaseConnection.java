package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * DatabaseConnection - Singleton connection pool manager for PostgreSQL
 * Smart Student ERP System
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    // DB Config - adjust these to your local setup
    private static final String DB_HOST     = System.getenv().getOrDefault("DB_HOST",     "localhost");
    private static final String DB_PORT     = System.getenv().getOrDefault("DB_PORT",     "5432");
    private static final String DB_NAME     = System.getenv().getOrDefault("DB_NAME",     "smart_erp");
    private static final String DB_USER     = System.getenv().getOrDefault("DB_USER",     "postgres");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    private static final String JDBC_URL = String.format(
        "jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME
    );

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        connect();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    private void connect() {
        try {
            Class.forName("org.postgresql.Driver");
            Properties props = new Properties();
            props.setProperty("user",     DB_USER);
            props.setProperty("password", DB_PASSWORD);
            props.setProperty("ssl",      "false");
            props.setProperty("ApplicationName", "SmartStudentERP");
            connection = DriverManager.getConnection(JDBC_URL, props);
            connection.setAutoCommit(true);
            LOGGER.info("Database connected successfully: " + JDBC_URL);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "PostgreSQL JDBC driver not found!", e);
            throw new RuntimeException("JDBC Driver not found", e);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Cannot connect to database: " + e.getMessage(), e);
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LOGGER.warning("Connection lost. Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    public void beginTransaction() throws SQLException {
        getConnection().setAutoCommit(false);
    }

    public void commit() throws SQLException {
        getConnection().commit();
        getConnection().setAutoCommit(true);
    }

    public void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Rollback failed", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing connection", e);
        }
    }

    /**
     * Helper: safely close resources
     */
    public static void closeResources(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r != null) {
                try { r.close(); } catch (Exception ignored) {}
            }
        }
    }
}
