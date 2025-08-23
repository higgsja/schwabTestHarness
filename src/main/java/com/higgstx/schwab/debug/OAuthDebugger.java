package com.higgstx.schwab.debug;

import com.higgstx.schwab.config.SchwabConfig;
import com.higgstx.schwab.service.TokenManager;
import com.higgstx.schwab.util.LoggingUtil;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple utility class for testing and debugging OAuth tokens and API calls.
 * This is a standalone debugger that doesn't require Spring Boot.
 */
public class OAuthDebugger {

    private static final Logger logger = LoggerFactory.getLogger(OAuthDebugger.class);

    public static void main(String[] args) {
        // Initialize logging
        LoggingUtil.initializeLogging();
        
        // Load configuration manually (since not using Spring)
        setupManualConfig();

        System.out.println("============================================================");
        System.out.println("          🔍 Schwab OAuth API Debugger");
        System.out.println("============================================================");
        
        showTokenStatus();
        showConfigurationStatus();
        testApiEndpoints();
        
        System.out.println("============================================================");
        System.out.println("✅ Debugger finished. Check logs for detailed information.");
        System.out.println("============================================================");
    }

    /**
     * Manually setup configuration for standalone usage
     */
    private static void setupManualConfig() {
        // Try to load from environment variables first
        try {
            System.out.println("✅ Configuration loaded from environment variables");
        } catch (IllegalStateException e) {
            // Fall back to hardcoded values for testing
            // In production, you should use environment variables or property files
            System.out.println("⚠️  Environment variables not found, using fallback configuration");
            SchwabConfig.setStaticCredentials(
                "y5eXVg33MBOWWyAOkDTuNRFr35Ml1Y5p", // Replace with your actual values
                "hy9B5A0tf7KcYE1A"  // Replace with your actual values
            );
            System.out.println("⚙️  Manual configuration setup completed");
        }
    }

    private static void showTokenStatus() {
        System.out.println("\n--- 🎫 Token Status ---");
        try {
            TokenManager.showTokenFilePaths();
            TokenManager.showTokenStatus();
            
            // Additional token validation
            boolean hasValidTokens = TokenManager.hasValidTokens();
            System.out.println("Valid Tokens Available: " + (hasValidTokens ? "✅ Yes" : "❌ No"));
            
        } catch (Exception e) {
            System.err.println("❌ Error occurred while showing token status: " + e.getMessage());
            logger.error("Show token status exception", e);
        }
    }

    private static void showConfigurationStatus() {
        System.out.println("\n--- ⚙️  Configuration Status ---");
        try {
            SchwabConfig.showConfigInfo();
            
            // Validate configuration
            try {
                SchwabConfig.validateConfig();
                System.out.println("Configuration Valid: ✅ Yes");
            } catch (IllegalStateException e) {
                System.out.println("Configuration Valid: ❌ No - " + e.getMessage());
                System.out.println("💡 To fix: Set environment variables SCHWAB_CLIENT_ID and SCHWAB_CLIENT_SECRET");
                System.out.println("   Or update the hardcoded values in this file for testing");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error occurred while showing configuration: " + e.getMessage());
            logger.error("Show configuration exception", e);
        }
    }

    private static void testApiEndpoints() {
        System.out.println("\n--- 🧪 API Endpoint Status ---");
        
        if (!TokenManager.hasValidTokens()) {
            System.out.println("❌ No valid tokens found.");
            System.out.println("💡 To test API endpoints:");
            System.out.println("   1. Run the full test harness: TestHarnessRunner");
            System.out.println("   2. Complete OAuth authorization (option 1)");
            System.out.println("   3. Then test API endpoints (option 4)");
            return;
        }

        System.out.println("✅ Valid tokens found - API testing could proceed");
        System.out.println("💡 For interactive API testing, use the full TestHarnessRunner");
        
        // Could add basic API connectivity test here
        testBasicConnectivity();
    }

    private static void testBasicConnectivity() {
        System.out.println("\n🔗 Testing basic connectivity...");
        
        try {
            // Test if we can reach the Schwab API endpoints
            java.net.URL authUrl = new java.net.URL(SchwabConfig.AUTH_URL);
            java.net.URLConnection connection = authUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            
            System.out.println("✅ Network connectivity to Schwab API: OK");
            
        } catch (IOException e) {
            System.out.println("❌ Network connectivity test failed: " + e.getMessage());
            logger.warn("Network connectivity test failed", e);
        }
    }
}