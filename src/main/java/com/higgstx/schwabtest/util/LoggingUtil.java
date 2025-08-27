package com.higgstx.schwabtest.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for logging setup
 */
public class LoggingUtil {
    
    private static final String LOGS_DIR = "logs";
    
    /**
     * Initialize logging directory
     */
    public static void initializeLogging() {
        try {
            Path logsPath = Paths.get(LOGS_DIR);
            if (!Files.exists(logsPath)) {
                Files.createDirectories(logsPath);
                System.out.println("Created logs directory: " + logsPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create logs directory: " + e.getMessage());
        }
    }
    
    /**
     * Get the logs directory path
     */
    public static String getLogsDirectory() {
        return Paths.get(LOGS_DIR).toAbsolutePath().toString();
    }
    
    /**
 * Show log file locations
 */
public static void showLogLocations() {
    Path logsPath = Paths.get(LOGS_DIR).toAbsolutePath();
    System.out.println("\nLog File Locations:");
    System.out.println("  Directory: " + logsPath);
    System.out.println("  Application Log: " + logsPath.resolve("schwab-oauth-client.log"));
    System.out.println("  Debug Log: " + logsPath.resolve("schwab-debug.log"));
    System.out.println("  OAuth Log: " + logsPath.resolve("oauth-operations.log"));
    System.out.println("  HTTP Requests Log: " + logsPath.resolve("http-requests.log"));
}
}