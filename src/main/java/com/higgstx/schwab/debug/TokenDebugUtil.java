package com.higgstx.schwab.debug;

import com.higgstx.schwab.config.SchwabConfig;
import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Debug utility for token files
 */
public class TokenDebugUtil {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("           🔍 TOKEN FILE DEBUGGER");
        System.out.println("=".repeat(60));
        System.out.println("🕐 Generated at: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println();
        
        debugTokenFiles();
    }
    
    /**
     * Debug token files
     */
    private static void debugTokenFiles() {
        // Check JSON token file (current format)
        Path jsonPath = Paths.get(SchwabConfig.TOKEN_PROPERTIES_FILE);
        System.out.println("📄 Current Token File: " + jsonPath.toAbsolutePath());
        checkTokenFile(jsonPath, "Current JSON Token File");
        
        // Check refresh token file
        Path refreshPath = Paths.get(SchwabConfig.REFRESH_TOKEN_FILE);
        System.out.println("\n📄 Refresh Token File: " + refreshPath.toAbsolutePath());
        checkTokenFile(refreshPath, "Refresh Token File");
        
        // Check for legacy properties file
        Path legacyPropsPath = Paths.get("schwab_tokens.properties");
        System.out.println("\n📄 Legacy Properties File: " + legacyPropsPath.toAbsolutePath());
        checkTokenFile(legacyPropsPath, "Legacy Properties File");
        
        // Provide recommendations
        provideRecommendations(jsonPath, refreshPath, legacyPropsPath);
    }
    
    private static void checkTokenFile(Path filePath, String description) {
        if (Files.exists(filePath)) {
            try {
                String content = Files.readString(filePath);
                long size = Files.size(filePath);
                String lastModified = Files.getLastModifiedTime(filePath)
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                System.out.println("✅ File exists");
                System.out.println("📏 Size: " + formatFileSize(size));
                System.out.println("🕐 Last Modified: " + lastModified);
                
                if (content.trim().isEmpty()) {
                    System.out.println("⚠️  File is empty");
                } else {
                    System.out.println("📝 Content preview:");
                    
                    // Show different previews based on file type
                    if (filePath.toString().endsWith(".json")) {
                        analyzeJsonContent(content);
                    } else {
                        // For refresh token or properties files
                        String preview = content.length() > 100 ? 
                            content.substring(0, 100) + "..." : content;
                        System.out.println("   " + preview.replace("\n", "\\n"));
                    }
                }
                
            } catch (IOException e) {
                System.out.println("❌ Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("❌ File does not exist");
        }
    }
    
    private static void analyzeJsonContent(String content) {
        try {
            // Basic JSON structure analysis
            if (content.contains("\"access_token\"")) {
                System.out.println("   ✅ Contains access_token field");
            } else {
                System.out.println("   ❌ Missing access_token field");
            }
            
            if (content.contains("\"refresh_token\"")) {
                System.out.println("   ✅ Contains refresh_token field");
            } else {
                System.out.println("   ❌ Missing refresh_token field");
            }
            
            if (content.contains("\"expiresAt\"")) {
                System.out.println("   ✅ Contains expiresAt field");
            } else {
                System.out.println("   ⚠️  Missing expiresAt field");
            }
            
            // Check for problematic fields
            if (content.contains("\"displayInfo\"")) {
                System.out.println("   ⚠️  Contains problematic 'displayInfo' field");
                System.out.println("       This field should not be serialized to JSON");
            }
            
            // Show a safe preview (first 200 chars, hiding sensitive data)
            String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            preview = preview.replaceAll("\"[a-zA-Z0-9+/=]{20,}\"", "\"[REDACTED_TOKEN]\"");
            System.out.println("   📄 Structure preview:");
            System.out.println("   " + preview.replace("\n", "\n   "));
            
        } catch (Exception e) {
            System.out.println("   ❌ Error analyzing JSON: " + e.getMessage());
        }
    }
    
    private static void provideRecommendations(Path jsonPath, Path refreshPath, Path legacyPath) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔧 RECOMMENDATIONS:");
        System.out.println("=".repeat(60));
        
        boolean hasJsonTokens = Files.exists(jsonPath);
        boolean hasRefreshToken = Files.exists(refreshPath);
        boolean hasLegacyFile = Files.exists(legacyPath);
        
        if (!hasJsonTokens && !hasRefreshToken && !hasLegacyFile) {
            System.out.println("📋 No token files found - this is normal for first run");
            System.out.println("💡 Next steps:");
            System.out.println("   1. Run the test harness: java -jar schwab-test-harness.jar");
            System.out.println("   2. Choose option 1 to authorize the application");
            System.out.println("   3. Complete the OAuth flow in your browser");
            
        } else if (hasJsonTokens) {
            try {
                String content = Files.readString(jsonPath);
                if (content.contains("\"displayInfo\"")) {
                    System.out.println("🔧 Issue detected: JSON file contains problematic fields");
                    System.out.println("💡 Fix: Delete the file and re-run OAuth authorization");
                    System.out.println("   Command: rm \"" + jsonPath.toAbsolutePath() + "\"");
                } else {
                    System.out.println("✅ JSON token file looks good");
                    System.out.println("💡 You can test API endpoints using the test harness");
                }
            } catch (IOException e) {
                System.out.println("🔧 Issue: Cannot read JSON file properly");
                System.out.println("💡 Fix: Delete and recreate the token file");
            }
            
        } else if (hasLegacyFile) {
            System.out.println("🔧 Legacy properties file detected");
            System.out.println("💡 Migrate to new format:");
            System.out.println("   1. Delete legacy file: rm \"" + legacyPath.toAbsolutePath() + "\"");
            System.out.println("   2. Re-run OAuth authorization to create new JSON format");
        }
        
        // General recommendations
        System.out.println("\n📚 General Tips:");
        System.out.println("   • Token files are stored in the current working directory");
        System.out.println("   • Access tokens expire after 30 minutes");
        System.out.println("   • Refresh tokens expire after 7 days");
        System.out.println("   • Use the test harness option 3 to refresh expired access tokens");
        System.out.println("   • Check logs/ directory for detailed error information");
        
        System.out.println("\n🛠️  Troubleshooting:");
        System.out.println("   • If OAuth fails: Check your client credentials in application.yml");
        System.out.println("   • If API calls fail: Verify tokens exist and are not expired");
        System.out.println("   • If network issues: Check firewall and proxy settings");
    }
    
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}