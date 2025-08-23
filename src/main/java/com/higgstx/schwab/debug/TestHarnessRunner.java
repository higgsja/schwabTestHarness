package com.higgstx.schwab.debug;

import com.higgstx.schwab.client.SchwabOAuthClient;
import com.higgstx.schwab.config.SchwabConfig;
import com.higgstx.schwab.config.SchwabSpringConfig;
import com.higgstx.schwab.model.TokenResponse;
import com.higgstx.schwab.service.TokenManager;
import com.higgstx.schwab.util.LoggingUtil;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * POJO Test Harness Runner with Lombok annotations and beautiful formatted output.
 * Provides an interactive console interface for testing the Schwab API wrapper.
 */
@Getter
@Setter
@SpringBootApplication
@ComponentScan(basePackages = "com.higgstx.schwab")
public class TestHarnessRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestHarnessRunner.class);
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SchwabSpringConfig schwabConfig;

    // Application state fields
    private boolean isRunning = true;
    private int totalOperations = 0;
    private String lastOperationTime = "";

    public static void main(String[] args) {
        // Initialize logging directory
        LoggingUtil.initializeLogging();
        
        // Show startup banner
        showStartupBanner();
        
        // Run the Spring Boot application
        SpringApplication app = new SpringApplication(TestHarnessRunner.class);
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        showWelcomeMessage();
        
        // Verify configuration
        if (!schwabConfig.isConfigSet()) {
            showConfigurationError();
            return;
        }
        
        showConfigurationSuccess();
        runInteractiveMenu();
    }

    /**
     * Displays the startup banner
     */
    private static void showStartupBanner() {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                 🏛️  SCHWAB API TEST HARNESS                  ║");
        System.out.println("║                      Professional Edition                     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Shows welcome message with system info
     */
    private void showWelcomeMessage() {
        String currentTime = LocalDateTime.now().format(TIME_FORMAT);
        
        System.out.println("┌─ 🚀 INITIALIZATION ──────────────────────────────────────┐");
        System.out.println("│ Starting Schwab API Test Harness...                     │");
        System.out.println("│ Time: " + String.format("%-48s", currentTime) + " │");
        System.out.println("│ Java Version: " + String.format("%-42s", System.getProperty("java.version")) + " │");
        System.out.println("└──────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    /**
     * Shows configuration error in formatted style
     */
    private void showConfigurationError() {
        System.out.println("╔═ ❌ CONFIGURATION ERROR ═══════════════════════════════════╗");
        System.out.println("║                                                           ║");
        System.out.println("║ Configuration not properly set in application.yml        ║");
        System.out.println("║                                                           ║");
        System.out.println("║ Required properties:                                      ║");
        System.out.println("║   • schwab.api.appKey                                     ║");
        System.out.println("║   • schwab.api.appSecret                                  ║");
        System.out.println("║                                                           ║");
        System.out.println("║ Please update your configuration and restart.            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }

    /**
     * Shows successful configuration load
     */
    private void showConfigurationSuccess() {
        String clientIdPreview = schwabConfig.getAppKey().substring(0, 8) + "...";
        
        System.out.println("┌─ ✅ CONFIGURATION LOADED ────────────────────────────────┐");
        System.out.println("│ Client ID: " + String.format("%-45s", clientIdPreview) + " │");
        System.out.println("│ Redirect URI: " + String.format("%-40s", SchwabConfig.REDIRECT_URI) + " │");
        System.out.println("│ Status: Ready for OAuth authorization                    │");
        System.out.println("└──────────────────────────────────────────────────────────┘");
        System.out.println();
        
        // Show log locations
        LoggingUtil.showLogLocations();
    }

    /**
     * Main interactive menu with beautiful formatting
     */
    private void runInteractiveMenu() {
        while (isRunning) {
            showMainMenu();
            String choice = getFormattedInput("Enter your choice");
            
            processMenuChoice(choice);
            
            if (isRunning) {
                showContinuePrompt();
            }
        }
    }

    /**
     * Displays the main menu with current status
     */
    private void showMainMenu() {
TokenResponse currentTokens = TokenManager.loadTokens(false);
        String tokenStatus = currentTokens != null ? currentTokens.getQuickStatus() : "❌ NO TOKENS";
        
        System.out.println("\n╔═ 🏛️  SCHWAB API TEST HARNESS MENU ═══════════════════════════╗");
        System.out.println("║                                                             ║");
        System.out.println("║ Current Status: " + String.format("%-42s", tokenStatus) + " ║");
        System.out.println("║ Operations Run: " + String.format("%-42s", String.valueOf(totalOperations)) + " ║");
        if (!lastOperationTime.isEmpty()) {
            System.out.println("║ Last Activity: " + String.format("%-43s", lastOperationTime) + " ║");
        }
        System.out.println("╠═════════════════════════════════════════════════════════════╣");
        System.out.println("║                        🔐 AUTHENTICATION                    ║");
        System.out.println("║  1. Authorize Application & Get New Tokens                 ║");
        System.out.println("║  2. Display Current Tokens                                 ║");
        System.out.println("║  3. Refresh Access Token                                   ║");
        System.out.println("╠═════════════════════════════════════════════════════════════╣");
        System.out.println("║                         🧪 API TESTING                     ║");
        System.out.println("║  4. Test API Endpoints                                     ║");
        System.out.println("╠═════════════════════════════════════════════════════════════╣");
        System.out.println("║                        ⚙️  CONFIGURATION                   ║");
        System.out.println("║  5. View Configuration                                     ║");
        System.out.println("║  6. Show Token Status                                      ║");
        System.out.println("║  7. Show Token File Paths                                  ║");
        System.out.println("║  8. Clear Token Files                                      ║");
        System.out.println("╠═════════════════════════════════════════════════════════════╣");
        System.out.println("║  0. 🚪 Exit Application                                     ║");
        System.out.println("╚═════════════════════════════════════════════════════════════╝");
    }

    /**
     * Processes menu choice with formatted error handling
     */
    private void processMenuChoice(String choice) {
        try {
            updateOperationTime();
            
            switch (choice.trim()) {
                case "1" -> authorizeApplication();
                case "2" -> displayCurrentTokens();
                case "3" -> refreshAccessToken();
                case "4" -> runApiEndpointTester();
                case "5" -> showConfiguration();
                case "6" -> showTokenStatus();
                case "7" -> showTokenFilePaths();
                case "8" -> clearTokenFiles();
                case "0" -> exitApplication();
                default -> showInvalidChoice(choice);
            }
        } catch (Exception e) {
            showErrorMessage("An unexpected error occurred", e.getMessage());
            logger.error("❌ Menu operation error: {}", e.getMessage(), e);
        }
    }
    
    /**
 * Processes the authorization code from the redirect URL
 */
private void processAuthorizationCode(SchwabOAuthClient client, String redirectUrl) {
    showProcessingMessage("Processing authorization URL...");
    
    String authCode = extractAuthCodeFromUrl(redirectUrl);
    
    if (authCode != null && !authCode.isEmpty()) {
        showSuccessMessage("Authorization code extracted successfully!");
        System.out.println("   Code preview: " + authCode.substring(0, Math.min(15, authCode.length())) + "...");
        
        showProcessingMessage("Exchanging authorization code for access tokens...");
        
        try {
            TokenResponse token = client.getTokens(authCode);
            
            if (token != null) {
                TokenManager.saveTokens(token);
                showSuccessMessage("Authorization successful! Tokens have been saved.");
                displayCurrentTokens();
            } else {
                showErrorMessage("Token exchange failed", "No response received from Schwab API");
            }
        } catch (Exception e) {
            showErrorMessage("Token exchange failed", e.getMessage());
            showTokenExchangeTroubleshooting();
        }
    } else {
        showErrorMessage("Could not extract authorization code", "Invalid or malformed URL");
        showAuthCodeTroubleshooting(redirectUrl);
    }
}

/**
 * Extracts authorization code from redirect URL
 */
private String extractAuthCodeFromUrl(String url) {
    try {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        url = url.trim();
        
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        
        if (!url.contains("code=")) {
            return null;
        }
        
        String query = url.contains("?") ? url.substring(url.indexOf("?") + 1) : null;
        if (query == null) return null;
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "code".equals(pair[0])) {
                return java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        
        return null;
        
    } catch (Exception e) {
        logger.error("Error extracting auth code from URL: {}", e.getMessage());
        return null;
    }
}

/**
 * Shows troubleshooting for token exchange issues
 */
private void showTokenExchangeTroubleshooting() {
    System.out.println("\n💡 Token Exchange Troubleshooting:");
    System.out.println("   • Authorization code may have expired (try again quickly)");
    System.out.println("   • Authorization code may have been used already");
    System.out.println("   • Check network connectivity to Schwab API");
}

/**
 * Shows troubleshooting for auth code extraction
 */
private void showAuthCodeTroubleshooting(String url) {
    System.out.println("\n🔍 URL Analysis:");
    System.out.println("   Provided: " + (url.length() > 80 ? url.substring(0, 80) + "..." : url));
    
    if (!url.contains("127.0.0.1")) {
        System.out.println("   ❌ URL doesn't contain '127.0.0.1'");
    } else if (!url.contains("code=")) {
        System.out.println("   ❌ URL doesn't contain 'code=' parameter");
    } else {
        System.out.println("   ⚠️ URL format appears correct but parsing failed");
    }
    
    System.out.println("\n💡 Troubleshooting:");
    System.out.println("   • Ensure you copied the COMPLETE URL from browser");
    System.out.println("   • URL should start with 'https://127.0.0.1/?code='");
    System.out.println("   • Don't include any extra characters or spaces");
}
    

    /**
     * Formatted authorization process
     */
    private void authorizeApplication() {
        showOperationHeader("🔐 OAuth Authorization", "Initiating OAuth 2.0 flow with Schwab API");
        
        try (SchwabOAuthClient client = new SchwabOAuthClient()) {
            String authUrl = client.getAuthorizationUrl();
            
            showAuthorizationInstructions(authUrl);
            
            if (openBrowserAutomatically(authUrl)) {
                showSuccessMessage("Browser opened successfully");
            } else {
                showWarningMessage("Please open the URL manually in your browser");
            }
            
            String redirectUrl = getFormattedInput("Paste the complete redirect URL here");
            
            if (redirectUrl.trim().isEmpty()) {
                showErrorMessage("Authorization cancelled", "No URL provided");
                return;
            }
            
            processAuthorizationCode(client, redirectUrl);
            
        } catch (Exception e) {
            showErrorMessage("Authorization failed", e.getMessage());
            logger.error("❌ Authorization error: {}", e.getMessage(), e);
        }
        
        incrementOperationCount();
    }

    /**
     * Shows detailed authorization instructions
     */
    /**
 * Shows detailed authorization instructions with easy-to-copy URL
 */
private void showAuthorizationInstructions(String authUrl) {
    System.out.println("╔═ 📱 AUTHORIZATION INSTRUCTIONS ═══════════════════════════════════════╗");
    System.out.println("║                                                                     ║");
    System.out.println("║ Schwab requires manual authorization due to redirect URI           ║");
    System.out.println("║ restrictions (must be exactly 'https://127.0.0.1')                 ║");
    System.out.println("║                                                                     ║");
    System.out.println("╠═ 🌐 STEP 1: AUTHORIZATION URL ═══════════════════════════════════════╣");
    System.out.println("║                                                                     ║");
    System.out.println("║ Please copy this URL (it's displayed without borders below):       ║");
    System.out.println("╚═════════════════════════════════════════════════════════════════════╝");
    System.out.println();
    
    // Display URL without any borders for easy copying
    System.out.println("COPY THIS URL:");
    System.out.println("─".repeat(80));
    System.out.println(authUrl);
    System.out.println("─".repeat(80));
    System.out.println();
    
    System.out.println("╔═ 📋 STEP 2: COMPLETE AUTHORIZATION ═══════════════════════════════════╗");
    System.out.println("║ • Paste the URL above into your web browser                        ║");
    System.out.println("║ • Sign in to your Schwab account                                   ║");
    System.out.println("║ • Approve the application access                                   ║");
    System.out.println("║                                                                     ║");
    System.out.println("╠═ 📋 STEP 3: COPY REDIRECT URL ═══════════════════════════════════════╣");
    System.out.println("║ After authorization, you'll be redirected to:                      ║");
    System.out.println("║ https://127.0.0.1/?code=AUTHORIZATION_CODE&session=...             ║");
    System.out.println("║                                                                     ║");
    System.out.println("║ ⚠️  Your browser will show 'This site can't be reached'            ║");
    System.out.println("║    or 'Connection refused' - THIS IS NORMAL!                       ║");
    System.out.println("║                                                                     ║");
    System.out.println("║ Copy the COMPLETE URL from your browser's address bar              ║");
    System.out.println("╚═════════════════════════════════════════════════════════════════════╝");
    System.out.println();
}

    /**
 * Refreshes access token with formatted output
 */
private void refreshAccessToken() {
    showOperationHeader("🔄 Token Refresh", "Refreshing expired access token");
    
    TokenResponse tokens = TokenManager.loadTokens(false);
    if (tokens == null || tokens.getRefreshToken() == null) {
        showErrorMessage("No refresh token found", "Please authorize the application first");
        return;
    }

    showProcessingMessage("Attempting to refresh access token...");
    
    try (SchwabOAuthClient client = new SchwabOAuthClient()) {
        TokenResponse refreshedTokens = client.refreshTokens(tokens.getRefreshToken());
        if (refreshedTokens != null) {
            try {
                TokenManager.saveTokens(refreshedTokens);
                showSuccessMessage("Access token refreshed successfully!");
                displayCurrentTokens();
            } catch (IOException saveException) {
                showErrorMessage("Token refresh succeeded but save failed", saveException.getMessage());
                logger.error("❌ Token save error: {}", saveException.getMessage(), saveException);
            }
        } else {
            showErrorMessage("Token refresh failed", "You may need to re-authorize the application");
        }
    } catch (Exception e) {
        showErrorMessage("Token refresh failed", e.getMessage());
        logger.error("❌ Token refresh error: {}", e.getMessage(), e);
    }
    
    incrementOperationCount();
}

    /**
     * Displays current tokens with beautiful formatting
     */
    private void displayCurrentTokens() {
        showOperationHeader("📋 Current Token Information", "Displaying stored authentication tokens");
        
TokenResponse tokens = TokenManager.loadTokens(false);
        if (tokens != null) {
            System.out.println(tokens.getDisplayInfo());
            System.out.println();
            System.out.println("📊 Quick Summary: " + tokens.getCompactInfo());
        } else {
            showInfoBox("No Tokens Found", 
                       "No authentication tokens are currently stored.",
                       "Please authorize the application first (option 1).");
        }
        
        incrementOperationCount();
    }



    /**
     * Runs the API endpoint tester
     */
    private void runApiEndpointTester() {
        showOperationHeader("🧪 API Endpoint Testing", "Interactive API endpoint testing suite");
        
        try {
            new ApiEndpointTester(scanner).run();
        } catch (Exception e) {
            showErrorMessage("API endpoint tester failed", e.getMessage());
            logger.error("❌ API tester error: {}", e.getMessage(), e);
        }
        
        incrementOperationCount();
    }

    /**
     * Shows configuration in formatted style
     */
    private void showConfiguration() {
        showOperationHeader("⚙️  Configuration Details", "Current API wrapper configuration");
        SchwabSpringConfig.showConfigInfo();
        incrementOperationCount();
    }

    /**
     * Shows token status
     */
    private void showTokenStatus() {
        showOperationHeader("📊 Token Status", "Detailed token file and validity information");
        TokenManager.showTokenStatus();
        incrementOperationCount();
    }

    /**
     * Shows token file paths
     */
    private void showTokenFilePaths() {
        showOperationHeader("🗂️  Token File Locations", "File system paths for token storage");
        TokenManager.showTokenFilePaths();
        incrementOperationCount();
    }

    /**
     * Clears token files with confirmation
     */
    private void clearTokenFiles() {
        showOperationHeader("🧹 Clear Token Files", "Remove all stored authentication tokens");
        
        String response = getFormattedInput("Are you sure you want to clear all token files? (yes/no)");
        
        if ("yes".equalsIgnoreCase(response.trim())) {
            TokenManager.clearTokenFiles();
            showSuccessMessage("Token files cleared successfully");
        } else {
            showInfoMessage("Operation cancelled");
        }
        
        incrementOperationCount();
    }

    /**
     * Exits the application gracefully
     */
    private void exitApplication() {
        showOperationHeader("🚪 Application Exit", "Shutting down test harness");
        
        System.out.println("┌─ 📊 SESSION SUMMARY ─────────────────────────────────────┐");
        System.out.println("│ Total Operations: " + String.format("%-40s", String.valueOf(totalOperations)) + " │");
        System.out.println("│ Session Duration: " + String.format("%-40s", getSessionDuration()) + " │");
        System.out.println("└──────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("👋 Thank you for using the Schwab API Test Harness!");
        
        isRunning = false;
        System.exit(0);
    }

    // Utility methods for formatting and UI

    /**
     * Shows an operation header
     */
    private void showOperationHeader(String title, String description) {
        System.out.println("\n╔═ " + title + " " + "═".repeat(Math.max(1, 60 - title.length() - 3)) + "╗");
        System.out.println("║ " + String.format("%-59s", description) + " ║");
        System.out.println("╚" + "═".repeat(61) + "╝");
        System.out.println();
    }

    /**
     * Shows a success message
     */
    private void showSuccessMessage(String message) {
        System.out.println("✅ " + message);
    }

    /**
     * Shows an error message with details
     */
    private void showErrorMessage(String title, String details) {
        System.out.println("❌ " + title + ": " + details);
    }

    /**
     * Shows a warning message
     */
    private void showWarningMessage(String message) {
        System.out.println("⚠️  " + message);
    }

    /**
     * Shows an info message
     */
    private void showInfoMessage(String message) {
        System.out.println("ℹ️  " + message);
    }

    /**
     * Shows a processing message
     */
    private void showProcessingMessage(String message) {
        System.out.println("🔄 " + message);
    }

    /**
     * Shows an info box with title, message, and suggestion
     */
    private void showInfoBox(String title, String message, String suggestion) {
        System.out.println("┌─ ℹ️  " + title + " " + "─".repeat(Math.max(1, 55 - title.length())) + "┐");
        System.out.println("│ " + String.format("%-59s", message) + " │");
        if (suggestion != null && !suggestion.isEmpty()) {
            System.out.println("│ " + String.format("%-59s", "") + " │");
            System.out.println("│ 💡 " + String.format("%-56s", suggestion) + " │");
        }
        System.out.println("└" + "─".repeat(61) + "┘");
    }

    /**
     * Gets formatted input from user
     */
    private String getFormattedInput(String prompt) {
        System.out.print("📝 " + prompt + ": ");
        return scanner.nextLine();
    }

    /**
     * Shows continue prompt
     */
    private void showContinuePrompt() {
        System.out.println("\n" + "─".repeat(63));
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    /**
     * Shows invalid choice message
     */
    private void showInvalidChoice(String choice) {
        showErrorMessage("Invalid choice", "'" + choice + "' is not a valid menu option");
    }

    /**
     * Opens browser automatically if possible
     */
    private boolean openBrowserAutomatically(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (IOException | URISyntaxException e) {
            logger.debug("Failed to open browser automatically: {}", e.getMessage());
        }
        return false;
    }

   

 

    /**
     * Updates the last operation time
     */
    private void updateOperationTime() {
        lastOperationTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Increments operation count
     */
    private void incrementOperationCount() {
        totalOperations++;
    }

    /**
     * Gets session duration (placeholder - could track actual start time)
     */
    private String getSessionDuration() {
        return "Current session"; // Could be enhanced to show actual duration
    }
}