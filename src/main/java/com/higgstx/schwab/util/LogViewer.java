package com.higgstx.schwab.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

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
                case "tail" ->
                {
                    String logType = args.length > 1 ? args[1] : "main";
                    int lines = args.length > 2 ? Integer.parseInt(args[2]) : 50;
                    tailLog(logType, lines);
                }
                case "show" ->
                {
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
        System.out.println("=".repeat(60));
        System.out.println("           SCHWAB OAUTH CLIENT - LOG STATUS");
        System.out.println("=".repeat(60));
        
        Path logDir = Paths.get(LOG_DIR);
        
        if (!Files.exists(logDir)) {
            System.out.println("📁 Log directory does not exist yet: " + logDir.toAbsolutePath());
            System.out.println("   Logs will be created when the application runs.");
            return;
        }
        
        System.out.println("📁 Log Directory: " + logDir.toAbsolutePath());
        System.out.println("🕒 Generated at: " + LocalDateTime.now().format(DATE_FORMAT));
        System.out.println();
        
        // Check each log file
        checkLogFile("schwab-oauth-client.log", "Main Application Log");
        checkLogFile("oauth-operations.log", "OAuth Operations Log");
        checkLogFile("http-requests.log", "HTTP Requests Log");
        
        System.out.println("\n💡 Usage:");
        System.out.println("  ./build.sh run logs           # Show this status");
        System.out.println("  ./build.sh run logs list      # List all log files");
        System.out.println("  ./build.sh run logs tail      # Show last 50 lines of main log");
        System.out.println("  ./build.sh run logs tail http # Show last 50 lines of HTTP log");
        System.out.println("  ./build.sh run logs show main # Show full main log");
        System.out.println("  ./build.sh run logs clear     # Clear all log files");
    }
    
    /**
     * Check and display info about a specific log file
     */
    private static void checkLogFile(String filename, String description) {
        Path logFile = Paths.get(LOG_DIR, filename);
        
        if (Files.exists(logFile)) {
            try {
                long size = Files.size(logFile);
                String sizeStr = formatFileSize(size);
                
                // Get last modified time
                String lastModified = Files.getLastModifiedTime(logFile)
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(DATE_FORMAT);
                
                // Count lines
//                long lineCount = 0;
                long lineCount;
                try (Stream<String> lines = Files.lines(logFile)) {
                    lineCount = lines.count();
                }
                
                System.out.println("📄 " + description);
                System.out.println("   File: " + filename);
                System.out.println("   Size: " + sizeStr + " (" + lineCount + " lines)");
                System.out.println("   Last Modified: " + lastModified);
                
                // Show last few lines as preview
                if (lineCount > 0) {
                    System.out.println("   Recent Activity:");
                    showLastLines(logFile, 3, "     ");
                }
                
            } catch (IOException e) {
                System.out.println("📄 " + description + " - Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("📄 " + description + " - Not created yet");
        }
        
        System.out.println();
    }
    
    /**
     * List all log files in the log directory
     */
    private static void listLogFiles() {
        System.out.println("📋 Log Files:");
        System.out.println("-".repeat(50));
        
        Path logDir = Paths.get(LOG_DIR);
        
        if (!Files.exists(logDir)) {
            System.out.println("No log directory found.");
            return;
        }
        
        try (Stream<Path> files = Files.list(logDir)) {
            files.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".log"))
                 .sorted()
                 .forEach(path -> {
                     try {
                         String name = path.getFileName().toString();
                         long size = Files.size(path);
                         String sizeStr = formatFileSize(size);
                         System.out.println("  " + name + " (" + sizeStr + ")");
                     } catch (IOException e) {
                         System.out.println("  " + path.getFileName() + " (error reading)");
                     }
                 });
        } catch (IOException e) {
            System.out.println("Error listing log files: " + e.getMessage());
        }
    }
    
    /**
     * Show the last N lines of a log file
     */
    private static void tailLog(String logType, int lines) {
        String filename = getLogFilename(logType);
        Path logFile = Paths.get(LOG_DIR, filename);
        
        if (!Files.exists(logFile)) {
            System.out.println("Log file not found: " + filename);
            return;
        }
        
        System.out.println("📄 Last " + lines + " lines of " + filename + ":");
        System.out.println("─".repeat(80));
        
        showLastLines(logFile, lines, "");
    }
    
    /**
     * Show the full content of a log file
     */
    private static void showFullLog(String logType) {
        String filename = getLogFilename(logType);
        Path logFile = Paths.get(LOG_DIR, filename);
        
        if (!Files.exists(logFile)) {
            System.out.println("Log file not found: " + filename);
            return;
        }
        
        System.out.println("📄 Full content of " + filename + ":");
        System.out.println("─".repeat(80));
        
        try {
            List<String> lines = Files.readAllLines(logFile);
            lines.forEach(System.out::println);
        } catch (IOException e) {
            System.out.println("Error reading log file: " + e.getMessage());
        }
    }
    
    /**
     * Clear all log files
     */
    private static void clearLogs() {
        Path logDir = Paths.get(LOG_DIR);
        
        if (!Files.exists(logDir)) {
            System.out.println("No log directory found.");
            return;
        }
        
        System.out.print("Are you sure you want to clear all log files? (yes/no): ");
        
        try {
            String response = new java.util.Scanner(System.in).nextLine().trim().toLowerCase();
            
            if (response.equals("yes")) {
                try (Stream<Path> files = Files.list(logDir)) {
                    long deletedCount = files.filter(Files::isRegularFile)
                                           .filter(path -> path.toString().endsWith(".log"))
                                           .mapToLong(path -> {
                                               try {
                                                   Files.delete(path);
                                                   System.out.println("Deleted: " + path.getFileName());
                                                   return 1;
                                               } catch (IOException e) {
                                                   System.out.println("Failed to delete: " + path.getFileName() + " - " + e.getMessage());
                                                   return 0;
                                               }
                                           })
                                           .sum();
                    
                    System.out.println("\n✅ Cleared " + deletedCount + " log files.");
                }
            } else {
                System.out.println("Operation cancelled.");
            }
        } catch (IOException e) {
            System.out.println("Error clearing logs: " + e.getMessage());
        }
    }
    
    /**
     * Show the last N lines of a file
     */
    private static void showLastLines(Path file, int maxLines, String prefix) {
        try {
            List<String> lines = Files.readAllLines(file);
            int start = Math.max(0, lines.size() - maxLines);
            
            for (int i = start; i < lines.size(); i++) {
                System.out.println(prefix + lines.get(i));
            }
            
        } catch (IOException e) {
            System.out.println(prefix + "Error reading file: " + e.getMessage());
        }
    }
    
    /**
     * Get log filename for a given log type
     */
    private static String getLogFilename(String logType) {
        return switch (logType.toLowerCase())
        {
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
        System.out.println();
        System.out.println("Log Types:");
        System.out.println("  main/app       - Main application log");
        System.out.println("  oauth          - OAuth operations log");
        System.out.println("  http/requests  - HTTP requests log");
    }
}