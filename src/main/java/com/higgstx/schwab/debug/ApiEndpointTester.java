package com.higgstx.schwab.debug;

import com.fasterxml.jackson.core.*;
import com.higgstx.schwab.client.SchwabOAuthClient;
import com.higgstx.schwab.model.ApiResponse;
import com.higgstx.schwab.model.TokenResponse;
import com.higgstx.schwab.service.TokenManager;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import org.springframework.stereotype.*;

/**
 * POJO API Endpoint Tester with Lombok annotations and beautiful formatted output.
 * Provides interactive testing for all Schwab API market data endpoints.
 */
@Getter
@Setter
//@Component
public class ApiEndpointTester {

    private static final Logger logger = LoggerFactory.getLogger(ApiEndpointTester.class);
    
    private final Scanner scanner;
    private int testsRun = 0;
    private int successfulTests = 0;
    private String lastTestTime = "";

    public ApiEndpointTester(Scanner scanner) {
        this.scanner = scanner;
    }

    public void run() throws Exception {
        showTesterHeader();
        
        TokenResponse tokens = validateTokens();
        if (tokens == null) {
            return;
        }

        showTokenSummary(tokens);
        runTestingMenu(tokens.getAccessToken());
    }

    /**
     * Shows the API tester header
     */
    private void showTesterHeader() {
        System.out.println("╔═ 🧪 API ENDPOINT TESTING SUITE ══════════════════════════════════════╗");
        System.out.println("║                                                                      ║");
        System.out.println("║   This tool allows you to interactively test Schwab API endpoints.   ║");
        System.out.println("║                                                                      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Validates and manages tokens
     */
    private TokenResponse validateTokens() throws Exception {
        System.out.println("🔍 Validating tokens...");
        TokenResponse tokens = TokenManager.loadTokens(true);
        if (tokens == null || !tokens.isAccessTokenValid()) {
            System.out.println("❌ Token issue detected. Please run the OAuth authorization flow first.");
            return null;
        }
        System.out.println("✅ Tokens are valid.");
        return tokens;
    }
    
    /**
     * Shows a summary of the current token status
     */
    private void showTokenSummary(TokenResponse tokens) {
        System.out.println("┌───────────── Token Status ─────────────┐");
        System.out.printf("│ Access Token: %s%n", tokens.isAccessTokenValid() ? "✅ Valid" : "❌ Expired");
        System.out.printf("│ Refresh Token: %s%n", tokens.isRefreshTokenValid() ? "✅ Valid" : "❌ Expired");
        System.out.printf("│ Access expires in: %s%n", formatTokenDuration(tokens.getSecondsUntilAccessExpiry()));
        System.out.printf("│ Refresh expires in: %s%n", formatTokenDuration(tokens.getSecondsUntilRefreshExpiry()));
        System.out.println("└────────────────────────────────────────┘");
        System.out.println();
    }

    /**
     * Displays and handles the main testing menu
     */
    private void runTestingMenu(String accessToken) {
        System.out.println("Choose an API Endpoint to Test:");
        System.out.println("  1. Get Quotes");
        System.out.println("  2. Get Price History");
        System.out.println("  3. Get Market Hours");
        System.out.println("  4. Get Instruments");
        System.out.println("  5. Manual Token Refresh");
        System.out.println("  6. Show Token Status");
        System.out.println("  7. Exit");
        
        while (true) {
            System.out.print("\nEnter your choice (1-7): ");
            String choice = scanner.nextLine();
            
            try {
                switch (choice) {
                    case "1" -> testEndpoint("Get Quotes", () -> testQuotes(accessToken));
                    case "2" -> testEndpoint("Get Price History", () -> testPriceHistory(accessToken));
                    case "3" -> testEndpoint("Get Market Hours", () -> testMarketHours(accessToken));
                    case "4" -> testEndpoint("Get Instruments", () -> testInstruments(accessToken));
                    case "5" -> manualTokenRefresh();
                    case "6" -> showTokenSummary(TokenManager.loadTokens(false));
                    case "7" -> {
                        System.out.println("Exiting tester. Goodbye!");
                        return;
                    }
                    default -> System.out.println("❌ Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                showErrorMessage("Test failed due to an error: " + e.getMessage());
                logger.error("Test failed", e);
            }
        }
    }
    
    /**
     * Executes a test function and prints the result
     */
    private void testEndpoint(String name, ApiTestFunction function) throws Exception {
        System.out.println("Running test: " + name);
        testsRun++;
        Instant startTime = Instant.now();
        
        ApiResponse response = function.execute();
        
        long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
        
        if (response.getStatusCode() == 200) {
            showSuccessMessage(String.format("✅ %s successful! (took %d ms)", name, duration));
            System.out.println("Response Body (truncated): " + response.getBody().substring(0, Math.min(200, response.getBody().length())) + "...");
            successfulTests++;
        } else {
            showErrorMessage(String.format("❌ %s failed! HTTP %d", name, response.getStatusCode()));
            System.out.println("Response Body: " + response.getBody());
        }
        lastTestTime = Instant.now().toString();
    }

    // Replace the testQuotes method in ApiEndpointTester.java:

/**
 * Test case: Get Quotes
 */
private ApiResponse testQuotes(String accessToken) throws Exception {
    System.out.print("Enter symbols (e.g., AAPL,MSFT): ");
    String symbolsInput = scanner.nextLine();
    
    // Handle both single symbol and comma-separated symbols
    String[] symbols;
    if (symbolsInput.contains(",")) {
        symbols = symbolsInput.split(",");
        // Trim whitespace from each symbol
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = symbols[i].trim().toUpperCase();
        }
    } else {
        symbols = new String[]{symbolsInput.trim().toUpperCase()};
    }
    
    return new SchwabOAuthClient().getQuotes(symbols, accessToken);
}
    
    /**
     * Test case: Get Price History
     */
    private ApiResponse testPriceHistory(String accessToken) throws Exception {
        System.out.print("Enter symbol: ");
        String symbol = scanner.nextLine();
        System.out.print("Enter period (e.g., 1, 10): ");
        int period = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter period type (e.g., day, month): ");
        String periodType = scanner.nextLine();
        System.out.print("Enter frequency (e.g., 1, 5): ");
        int frequency = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter frequency type (e.g., minute, daily): ");
        String frequencyType = scanner.nextLine();

        return new SchwabOAuthClient().getPriceHistory(symbol, period, periodType, frequency, frequencyType, accessToken);
    }
    
    /**
     * Test case: Get Market Hours
     */
    private ApiResponse testMarketHours(String accessToken) throws Exception {
        System.out.print("Enter market type (e.g., EQUITY, FUTURE): ");
        String marketType = scanner.nextLine();
        return new SchwabOAuthClient().getMarketHours(marketType, accessToken);
    }
    
    /**
     * Test case: Get Instruments
     */
    private ApiResponse testInstruments(String accessToken) throws Exception {
        System.out.print("Enter search term (symbol or description): ");
        String searchTerm = scanner.nextLine();
        System.out.print("Enter projection (symbol-search, desc-search, fundamental): ");
        String projection = scanner.nextLine();
        return new SchwabOAuthClient().getInstruments(searchTerm, projection, accessToken);
    }
    
    /**
     * Test case: Manual Token Refresh
     */
    private void manualTokenRefresh() {
        showInfoMessage("Attempting a manual token refresh...");
        TokenResponse currentTokens = TokenManager.loadTokens(false);
        
        if (currentTokens == null) {
            showErrorMessage("No tokens found. Please authorize first.");
            return;
        }
        
        if (!currentTokens.isRefreshTokenValid()) {
            showErrorMessage("Cannot refresh: Refresh token is expired");
            return;
        }
        
        try {
            TokenResponse refreshed = TokenManager.forceTokenRefresh();
            
            if (refreshed != null && refreshed.isAccessTokenValid()) {
                showSuccessMessage("Manual refresh successful!");
                showTokenSummary(refreshed);
            } else {
                showErrorMessage("Manual refresh failed");
            }
        } catch (Exception e) {
            showErrorMessage("Manual refresh error: " + e.getMessage());
        }
    }
    
    /**
     * Formats duration for token display
     */
    private String formatTokenDuration(long seconds) {
        if (seconds < 0) {
            return "Expired";
        } else if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } else {
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            return days + "d " + hours + "h";
        }
    }

    /**
     * Functional interface for API test functions
     */
    @FunctionalInterface
    private interface ApiTestFunction {
        ApiResponse execute() throws Exception;
    }
    
    // --- Helper methods for formatted output ---
    private void showSuccessMessage(String message) {
        System.out.println("✨ " + message);
    }
    
    private void showErrorMessage(String message) {
        System.out.println("❌ " + message);
    }
    
    private void showInfoMessage(String message) {
        System.out.println("ℹ️  " + message);
    }
}