package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger - Simple colored console logger
 */
public class Logger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String ts() {
        return LocalDateTime.now().format(FMT);
    }

    public static void info(String msg) {
        System.out.printf("\u001B[36m[%s] INFO  %s\u001B[0m%n", ts(), msg);
    }

    public static void warn(String msg) {
        System.out.printf("\u001B[33m[%s] WARN  %s\u001B[0m%n", ts(), msg);
    }

    public static void error(String msg) {
        System.out.printf("\u001B[31m[%s] ERROR %s\u001B[0m%n", ts(), msg);
    }

    public static void debug(String msg) {
        if ("true".equals(System.getenv("DEBUG"))) {
            System.out.printf("\u001B[90m[%s] DEBUG %s\u001B[0m%n", ts(), msg);
        }
    }

    public static void request(String method, String path, int status, long ms) {
        String color = status < 300 ? "\u001B[32m" : status < 400 ? "\u001B[33m" : "\u001B[31m";
        System.out.printf("%s[%s] %s %-35s %d (%dms)\u001B[0m%n",
            color, ts(), method, path, status, ms);
    }
}
