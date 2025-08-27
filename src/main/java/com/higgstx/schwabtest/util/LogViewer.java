package com.higgstx.schwabtest.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class for viewing and managing log files
 */
public class LogViewer {
    
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        if (args.length > 0) {
            String command = args[0].toLowerCase();
            switch (command) {
                case "list" -> listLogFiles();
                case "tail" -> {
                    String logType = args.length > 1 ? args[1] : "main";
                    int lines = args.length > 2 ? Integer.parseInt(args[2]) : 50;
                    tailLog(logType, lines);
                }
                case "show" -> {
                    String showType = args.length > 1 ? args[1] : "main";
                    showFullLog(showType);
                }
                case "clear" -> clearLogs();
                default -> showHelp();
            }
        } else {
            showLogStatus();
        }
    }
    
    /**
     * Show log file status and recent activity
     */
    public static void showLogStatus() {
        System.out.println("Current Log Status");
        System.out.println("-".repeat(40));
        
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                System.out.println("No logs directory found");
                return;
            }
            
            Files.list(logDir)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".log"))
                    .forEach(file -> {
                        try {
                            long size = Files.size(file);
                            String lastModified = DATE_FORMAT.format(
                                LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(file).toInstant(),
                                    java.time.ZoneId.systemDefault()
                                )
                            );
                            System.out.printf("  * %-25s %s (%s)%n", 
                                file.getFileName(), formatFileSize(size), lastModified);
                        } catch (IOException e) {
                            System.out.println("  * " + file.getFileName() + " (error reading)");
                        }
                    });
            
        } catch (IOException e) {
            System.err.println("Error reading logs directory: " + e.getMessage());
        }
        
        System.out.println("Use 'logviewer list' or 'logviewer tail [type]' for more.");
    }

    private static void listLogFiles() {
        System.out.println("Listing all log files in '" + LOG_DIR + "'");
        System.out.println("-".repeat(40));
        
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                System.out.println("No logs directory found");
                return;
            }
            
            Files.list(logDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            long size = Files.size(file);
                            System.out.printf("  * %-25s %s%n", file.getFileName(), formatFileSize(size));
                        } catch (IOException e) {
                            System.out.println("  * " + file.getFileName() + " (error reading)");
                        }
                    });
            
        } catch (IOException e) {
            System.err.println("Error reading logs directory: " + e.getMessage());
        }
        
        System.out.println("-".repeat(40));
    }
    
    /**
     * Shows the last N lines of a specified log file
     */
    private static void tailLog(String logType, int lines) {
        Path logFile = Paths.get(LOG_DIR, getLogFilename(logType));
        System.out.println("Showing last " + lines + " lines of " + logFile.getFileName());
        System.out.println("─".repeat(40));
        if (!Files.exists(logFile)) {
            System.out.println("File not found: " + logFile.getFileName());
            return;
        }
        
        try {
            List<String> allLines = Files.readAllLines(logFile);
            int start = Math.max(0, allLines.size() - lines);
            for (int i = start; i < allLines.size(); i++) {
                System.out.println(allLines.get(i));
            }
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
        System.out.println("─".repeat(40));
    }
    
    /**
     * Shows the full content of a specified log file
     */
    private static void showFullLog(String logType) {
        Path logFile = Paths.get(LOG_DIR, getLogFilename(logType));
        System.out.println("Showing full content of " + logFile.getFileName());
        System.out.println("─".repeat(40));
        if (!Files.exists(logFile)) {
            System.out.println("File not found: " + logFile.getFileName());
            return;
        }
        
        try {
            Files.lines(logFile).forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
        System.out.println("─".repeat(40));
    }
    
    private static void clearLogs() {
        System.out.println("Clearing all log files in '" + LOG_DIR + "'");
        System.out.println("-".repeat(40));
        
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (Files.exists(logDir)) {
                Files.list(logDir)
                        .filter(Files::isRegularFile)
                        .filter(file -> file.toString().endsWith(".log"))
                        .forEach(file -> {
                            try {
                                Files.delete(file);
                                System.out.println("Deleted: " + file.getFileName());
                            } catch (IOException e) {
                                System.out.println("Failed to delete: " + file.getFileName());
                            }
                        });
            }
        } catch (IOException e) {
            System.err.println("Error clearing logs: " + e.getMessage());
        }
        
        System.out.println("Logs cleared.");
        System.out.println("-".repeat(40));
    }
    
    /**
     * Gets log filename based on type
     */
    private static String getLogFilename(String logType) {
        return switch (logType.toLowerCase()) {
            case "main", "app" -> "schwab-oauth-client.log";
            case "oauth" -> "oauth-operations.log";
            case "http", "requests" -> "http-requests.log";
            default -> "schwab-oauth-client.log";
        };
    }
    
    /**
     * Format file size in human-readable format
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Show help information
     */
    private static void showHelp() {
        System.out.println("Log Viewer Commands:");
        System.out.println("  list           - List all log files");
        System.out.println("  tail [type] [n] - Show last n lines (default: 50)");
        System.out.println("  show [type]    - Show full log file");
        System.out.println("  clear          - Clear all log files");
        System.out.println("  status         - Show log file status (default)");
        System.out.println("\nLog Types:");
        System.out.println("  main (default), app, oauth, http, requests");
    }
}