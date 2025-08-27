package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import com.higgstx.schwabtest.util.LoggingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A simple utility class for testing and debugging OAuth tokens and API calls.
 * This version reads configuration from application.yml
 */
public class OAuthDebugger {

    private static final Logger logger = LoggerFactory.getLogger(OAuthDebugger.class);
    private static SchwabTestConfig testConfig;

    public static void main(String[] args) {
        // Initialize logging
        LoggingUtil.initializeLogging();
        
        // Load configuration from application.yml
        setupConfigFromApplicationYml();

        System.out.println("============================================================");
        System.out.println("          Schwab OAuth API Debugger");
        System.out.println("============================================================");
        
        showTokenStatus();
        showConfigurationStatus();
        testApiEndpoints();
        
        System.out.println("============================================================");
        System.out.println("Debugger finished. Check logs for detailed information.");
        System.out.println("============================================================");
    }

    /**
     * Setup configuration by reading from application.yml
     */
    private static void setupConfigFromApplicationYml() {
        testConfig = new SchwabTestConfig();
        
        try {
            String appKey = null;
            String appSecret = null;
            
            // Read the application.yml file
            String ymlPath = "src/main/resources/application.yml";
            if (Files.exists(Paths.get(ymlPath))) {
                String content = Files.readString(Paths.get(ymlPath));
                appKey = extractYmlValue(content, "appKey");
                appSecret = extractYmlValue(content, "appSecret");
            } else {
                // Try from classpath
                try (InputStream is = OAuthDebugger.class.getClassLoader().getResourceAsStream("application.yml")) {
                    if (is != null) {
                        String content = new String(is.readAllBytes());
                        appKey = extractYmlValue(content, "appKey");
                        appSecret = extractYmlValue(content, "appSecret");
                    }
                } catch (IOException ioException) {
                    System.out.println("Error reading application.yml from classpath: " + ioException.getMessage());
                    logger.error("Classpath configuration read error", ioException);
                }
            }
            
            if (appKey != null && appSecret != null) {
                System.out.println("Configuration loaded from application.yml");
                testConfig.setAppKey(appKey);
                testConfig.setAppSecret(appSecret);
            } else {
                System.out.println("Could not find appKey/appSecret in application.yml");
                System.out.println("Please ensure your application.yml contains:");
                System.out.println("schwab:");
                System.out.println("  api:");
                System.out.println("    appKey: \"your-client-id\"");
                System.out.println("    appSecret: \"your-client-secret\"");
            }
            
        } catch (IOException ioException) {
            System.out.println("Error reading application.yml: " + ioException.getMessage());
            logger.error("Configuration setup IO error", ioException);
        } catch (SecurityException securityException) {
            System.out.println("Security error reading application.yml: " + securityException.getMessage());
            logger.error("Configuration setup security error", securityException);
        }
    }
    
    /**
     * Simple YAML value extractor
     */
    private static String extractYmlValue(String yamlContent, String key) {
        try {
            String[] lines = yamlContent.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith(key + ":")) {
                    String value = trimmed.substring(key.length() + 1).trim();
                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            }
        } catch (StringIndexOutOfBoundsException indexException) {
            logger.debug("Error extracting {} from YAML - index out of bounds: {}", key, indexException.getMessage());
        } catch (NullPointerException nullException) {
            logger.debug("Error extracting {} from YAML - null pointer: {}", key, nullException.getMessage());
        }
        return null;
    }

    private static void showTokenStatus() {
        System.out.println("\n--- Token Status ---");
        try {
            TokenManager.showTokenFilePaths();
            
            // Create an instance to call the instance method
            TokenManager tokenManager = new TokenManager();
            tokenManager.showTokenStatus();
            
            // Additional token validation
            boolean hasValidTokens = TokenManager.hasValidTokens();
            System.out.println("Valid Tokens Available: " + (hasValidTokens ? "Yes" : "No"));
            
        } catch (RuntimeException runtimeException) {
            System.err.println("Runtime error occurred while showing token status: " + runtimeException.getMessage());
            logger.error("Show token status runtime exception", runtimeException);
        } 
    }

    private static void showConfigurationStatus() {
        System.out.println("\n--- Configuration Status ---");
        try {
            if (testConfig != null) {
                testConfig.showConfig();
                
                // Validate configuration
                try {
                    testConfig.validateConfig();
                    System.out.println("Configuration Valid: Yes");
                } catch (IllegalStateException stateException) {
                    System.out.println("Configuration Valid: No - " + stateException.getMessage());
                    System.out.println("Check your application.yml file:");
                    System.out.println("  schwab:");
                    System.out.println("    api:");
                    System.out.println("      appKey: \"your-client-id\"");
                    System.out.println("      appSecret: \"your-client-secret\"");
                }
            } else {
                System.out.println("Configuration not loaded");
            }
            
        } catch (RuntimeException runtimeException) {
            System.err.println("Runtime error occurred while showing configuration: " + runtimeException.getMessage());
            logger.error("Show configuration runtime exception", runtimeException);
        }
    }

    private static void testApiEndpoints() {
        System.out.println("\n--- API Endpoint Status ---");
        
        if (!TokenManager.hasValidTokens()) {
            System.out.println("No valid tokens found.");
            System.out.println("To test API endpoints:");
            System.out.println("   1. Run the full test harness: TestHarnessRunner");
            System.out.println("   2. Complete OAuth authorization (option 1)");
            System.out.println("   3. Then test API endpoints (option 4)");
            return;
        }

        System.out.println("Valid tokens found - API testing could proceed");
        System.out.println("For interactive API testing, use the full TestHarnessRunner");
        
        testBasicConnectivity();
    }

    private static void testBasicConnectivity() {
        System.out.println("\nTesting basic connectivity...");
        
        try {
            // Create an instance of SchwabApiProperties to get the auth URL
            SchwabApiProperties properties = new SchwabApiProperties();
            URL authUrl = new URL(properties.getAuthUrl());
            URLConnection connection = authUrl.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            
            System.out.println("Network connectivity to Schwab API: OK");
            
        } catch (IOException ioException) {
            System.out.println("Network connectivity test failed: " + ioException.getMessage());
            logger.warn("Network connectivity test failed", ioException);
        } catch (SecurityException securityException) {
            System.out.println("Security error during connectivity test: " + securityException.getMessage());
            logger.warn("Network connectivity security error", securityException);
        } catch (RuntimeException runtimeException) {
            System.out.println("Configuration error during connectivity test: " + runtimeException.getMessage());
            logger.warn("Network connectivity configuration error", runtimeException);
        }
    }
}