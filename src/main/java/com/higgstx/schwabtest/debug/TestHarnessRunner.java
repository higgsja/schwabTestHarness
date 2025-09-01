package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.config.SchwabOAuthClient;
import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.exception.SchwabApiException;
import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabapi.service.MarketDataService;
import com.higgstx.schwabapi.model.market.QuoteData;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

@Component
public class TestHarnessRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestHarnessRunner.class);
    private final SchwabTestConfig config;
    private final TokenManager tokenManager;
    private final MarketDataService marketDataService;

    public TestHarnessRunner(SchwabTestConfig config, TokenManager tokenManager,
            MarketDataService marketDataService) {
        this.config = config;
        this.tokenManager = tokenManager;
        this.marketDataService = marketDataService;
    }

    @Override
    public void run(String... args) {
        System.out.println("============================================================");
        System.out.println("          SCHWAB API INTERACTIVE TEST HARNESS");
        System.out.println("============================================================");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            displayMenu();
            String choice = scanner.nextLine();
            try {
                switch (choice) {
                    case "1":
                        showConfigurationStatus();
                        break;
                    case "2":
                        manualOAuth(scanner);
                        break;
                    case "3":
                        tokenManager.showTokenStatus();
                        break;
                    case "4":
                        testAutomatedRefresh();
                        break;
                    case "5":
                        testMarketData();
                        break;
                    case "6":
                        testHistoricalData(scanner);
                        break;
                    case "7":
                        tokenManagementMenu(scanner);
                        break;
                    case "8":
                        System.out.println("Exiting. Goodbye!");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 8.");
                }
            } catch (Exception e) {
                System.err.println("An unexpected error occurred: " + e.getMessage());
                logger.error("Menu option failed", e);
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void displayMenu() {
        System.out.println("============================================================");
        System.out.println("MAIN MENU - Select an option:");
        System.out.println("============================================================");
        System.out.println("1. Configuration Status");
        System.out.println("2. Manual OAuth Authorization");
        System.out.println("3. Check Token Status");
        System.out.println("4. Test Automated Refresh");
        System.out.println("5. Test Market Data API");
        System.out.println("6. Test Historical Data");
        System.out.println("7. Token Management");
        System.out.println("8. Exit");
        System.out.println("============================================================");
        System.out.print("Enter your choice (1-8): ");
    }

    private void manualOAuth(Scanner scanner) {
        System.out.println("\n--- Manual OAuth Authorization ---");
        System.out.println("App Key: " + config.getAppKey());
        System.out.println("Redirect URI: " + config.getRedirectUri() +
                " (must be configured in Schwab Developer Portal)");

        System.out.println("\nThis process requires manual URL copying due to HTTPS certificate requirements.");

        System.out.print("\nProceed with manual OAuth authorization? (y/n): ");
        String response = scanner.nextLine();
        if (!response.equalsIgnoreCase("y")) {
            System.out.println("Authorization cancelled.");
            return;
        }

        SchwabOAuthClient client = null;
        try {
            // Create SchwabApiProperties manually to avoid YAML parsing issues
            System.out.println("Creating OAuth client...");
            SchwabApiProperties apiProperties = new SchwabApiProperties(
                config.getUrls().getAuth(),                    // Auth URL
                config.getUrls().getToken(),                   // Token URL  
                config.getUrls().getMarketData(),              // Market Data URL
                config.getDefaults().getRedirectUri(),         // Redirect URI
                config.getDefaults().getScope(),               // Scope
                config.getDefaults().getHttpTimeoutMs()        // Timeout
            );
            
            // Create client with explicit properties
            client = new SchwabOAuthClient(apiProperties);
            
            System.out.println("============================================================");
            System.out.println("STEP 1: Browser Authorization");
            System.out.println("============================================================");
            System.out.println("Building authorization URL...");
            
            String authUrl = client.buildAuthorizationUrl(config.getAppKey(), config.getRedirectUri());
            
            // Skip automatic browser opening to avoid Desktop issues
            System.out.println("\nPlease manually open this URL in your browser:");
            System.out.println("=".repeat(80));
            System.out.println(authUrl);
            System.out.println("=".repeat(80));
            
            System.out.println("\nInstructions:");
            System.out.println("1. Copy the URL above and paste it into your browser");
            System.out.println("2. Log into your Schwab account");
            System.out.println("3. Review and approve the API permissions");
            System.out.println("4. Schwab will redirect to: " + config.getRedirectUri() + "?code=...");
            System.out.println("5. Browser will show 'connection refused' - this is EXPECTED!");
            System.out.println("6. Copy the COMPLETE URL from your browser's address bar");

            System.out.println("\n============================================================");
            System.out.println("STEP 2: Paste Redirect URL");
            System.out.println("============================================================");
            System.out.print("Paste the full redirect URL here: ");
            String redirectUrl = scanner.nextLine().trim();

            if (redirectUrl.isEmpty()) {
                System.err.println("No URL provided.");
                return;
            }

            System.out.println("Extracting authorization code from URL...");
            String authCode = client.extractAuthorizationCode(redirectUrl);
            if (authCode == null) {
                System.err.println("Failed to extract authorization code from the URL.");
                System.err.println("Make sure you copied the complete URL including the ?code= parameter");
                System.err.println("Expected format: " + config.getRedirectUri() + "?code=XXXXXX");
                return;
            }
            System.out.println("Authorization code extracted successfully!");

            System.out.println("Exchanging authorization code for tokens...");
            TokenResponse tokens = client.getTokens(config.getAppKey(), config.getAppSecret(),
                    authCode, config.getRedirectUri());

            // Save all tokens to the main JSON file
            tokenManager.saveTokensInstance(tokens);

            // Save the refresh token separately for the refresher to find
            try {
                Path refreshTokenPath = Paths.get(tokenManager.getRefreshTokenFile());
                Files.writeString(refreshTokenPath, tokens.getRefreshToken());
            } catch (IOException e) {
                logger.error("Failed to write refresh token to file: {}", e.getMessage());
                System.err.println("Warning: Failed to save refresh token to file. Automated refresh may not work.");
            }

            System.out.println("\nSUCCESS! Tokens acquired and saved.");
            System.out.println("Access token expires at: " + tokens.getExpiresAt());
            System.out.println("You can now use the API endpoints (option 5).");

        } catch (Exception e) {
            System.err.println("OAuth error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            logger.error("OAuth process failed", e);
            
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    System.err.println("Warning: Error closing OAuth client: " + e.getMessage());
                }
            }
        }
    }

    private void testMarketData() {
        System.out.println("\n--- Testing Market Data API ---");
        
        // First check if we have valid tokens
        System.out.println("Checking token status before API calls...");
        String tokenStatus = marketDataService.getTokenStatus();
        System.out.println("Token Status: " + tokenStatus);
        
        if (!marketDataService.isReady()) {
            System.err.println("Service not ready - no valid tokens available.");
            System.out.println("Please run option 2 (Manual OAuth Authorization) first.");
            return;
        }
        
        // Test 1: Check market hours first
        System.out.println("\nTesting market hours...");
        try {
            var marketHours = marketDataService.getMarketHours("equity");
            System.out.println("Market hours request successful. Status: " + marketHours.getStatusCode());
            if (marketHours.getStatusCode() == 200) {
                System.out.println("Market hours data length: " + marketHours.getBody().length() + " characters");
                // Show first 200 characters of response for debugging
                String body = marketHours.getBody();
                if (body.length() > 200) {
                    System.out.println("Market hours sample: " + body.substring(0, 200) + "...");
                } else {
                    System.out.println("Market hours data: " + body);
                }
            } else {
                System.out.println("Market hours error: " + marketHours.getBody());
            }
        } catch (Exception e) {
            System.err.println("Market hours test failed: " + e.getMessage());
        }
        
        // Test 2: Try multiple symbols with different formats
        System.out.println("\nTesting quotes for multiple symbols...");
        String[] symbolsToTest = {"AAPL", "MSFT", "GOOGL", "TSLA", "SPY"};
        
        for (String symbol : symbolsToTest) {
            System.out.println("\nTesting symbol: " + symbol);
            try {
                QuoteData quote = marketDataService.getQuote(symbol);
                System.out.println("  Result: " + quote.getStatus());
                
                if (quote.isSuccess()) {
                    System.out.println("  SUCCESS - Close Price: $" + quote.getClosePrice());
                    System.out.println("  Volume: " + quote.getTotalVolume());
                    break; // Found a working symbol, stop testing
                } else {
                    System.out.println("  FAILED - " + quote.getErrorMessage());
                }
                
            } catch (Exception e) {
                System.err.println("  ERROR: " + e.getMessage());
            }
        }
        
        // Test 3: Try a batch quote request
        System.out.println("\nTesting batch quote request...");
        try {
            List<QuoteData> quotes = marketDataService.getQuotes(List.of("AAPL", "MSFT"));
            System.out.println("Batch quote request returned " + quotes.size() + " results:");
            for (QuoteData quote : quotes) {
                System.out.println("  " + quote.getSymbol() + ": " + quote.getStatus() + 
                                 (quote.isSuccess() ? " ($" + quote.getClosePrice() + ")" : " - " + quote.getErrorMessage()));
            }
        } catch (Exception e) {
            System.err.println("Batch quote test failed: " + e.getMessage());
        }
        
        System.out.println("\n--- Market Data Test Complete ---");
        System.out.println("Note: If all symbols return 'NOT_FOUND', this might be due to:");
        System.out.println("  • Market being closed (try during trading hours)");
        System.out.println("  • API endpoint requiring different symbol format");
        System.out.println("  • Sandbox vs production API differences");
        System.out.println("  • Insufficient API permissions in your Schwab app registration");
        System.out.println("  • Need to request 'market data' scope in addition to 'readonly'");
    }

    private void testHistoricalData(Scanner scanner) {
        System.out.println("\n--- Testing Historical Data API ---");
        
        // First check if we have valid tokens
        System.out.println("Checking token status before API calls...");
        String tokenStatus = marketDataService.getTokenStatus();
        System.out.println("Token Status: " + tokenStatus);
        
        if (!marketDataService.isReady()) {
            System.err.println("Service not ready - no valid tokens available.");
            System.out.println("Please run option 2 (Manual OAuth Authorization) first.");
            return;
        }
        
        // Get user preferences
        System.out.print("\nEnter number of days of historical data (1-365): ");
        String daysInput = scanner.nextLine().trim();
        int days;
        try {
            days = Integer.parseInt(daysInput);
            if (days < 1 || days > 365) {
                days = 30; // Default
                System.out.println("Invalid input, using default of 30 days");
            }
        } catch (NumberFormatException e) {
            days = 30; // Default
            System.out.println("Invalid input, using default of 30 days");
        }
        
        System.out.print("Enter ticker symbols (comma-separated, e.g., AAPL,MSFT,GOOGL): ");
        String symbolsInput = scanner.nextLine().trim();
        if (symbolsInput.isEmpty()) {
            symbolsInput = "AAPL,MSFT,GOOGL,TSLA,SPY"; // Default symbols
            System.out.println("No symbols entered, using default: " + symbolsInput);
        }
        
        String[] symbols = symbolsInput.split(",");
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = symbols[i].trim().toUpperCase();
        }
        
        System.out.println("\nFetching " + days + " days of historical data for " + symbols.length + " symbols...");
        System.out.println("Base Market Data URL: " + config.getUrls().getMarketData());
        
        // Test historical data for each symbol
        for (String symbol : symbols) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Historical Data for " + symbol + " (" + days + " days)");
            System.out.println("=".repeat(60));
            
            try {
                // Try different parameter combinations to find what works
                
                // Test 1: Basic daily data with period type "day"
                System.out.println("Test 1: Daily data with period type 'day'");
                String expectedUrl = String.format("%s/pricehistory?symbol=%s&periodType=day&period=1&frequencyType=daily&frequency=1&needPreviousClose=true",
                    config.getUrls().getMarketData(), symbol, days);
                System.out.println("Expected URL: " + expectedUrl);
                
                var response1 = marketDataService.getPriceHistory(
                    symbol, 
                    days,           // period (number of days)
                    "day",          // periodType 
                    1,              // frequency (1 = daily)
                    "daily"         // frequencyType
                );
                
                System.out.println("Response Status: " + response1.getStatusCode());
                if (response1.getStatusCode() != 200) {
                    System.out.println("Response Body: " + response1.getBody());
                }
                
                // Test 2: Try with different parameter format if first fails
                if (response1.getStatusCode() == 404) {
                    System.out.println("\nTest 2: Trying alternative parameters...");
                    
                    var response2 = marketDataService.getPriceHistory(
                        symbol, 
                        1,              // period = 1 month
                        "month",        // periodType 
                        1,              // frequency = 1 day  
                        "daily"         // frequencyType
                    );
                    
                    System.out.println("Alternative Response Status: " + response2.getStatusCode());
                    if (response2.getStatusCode() != 200) {
                        System.out.println("Alternative Response Body: " + response2.getBody());
                    } else {
                        response1 = response2; // Use the working response
                    }
                }
                
                // Test 3: Try minimal parameters if others fail
                if (response1.getStatusCode() == 404) {
                    System.out.println("\nTest 3: Trying minimal parameters...");
                    
                    var response3 = marketDataService.getPriceHistory(
                        symbol, 
                        10,             // period = 10 days (default)
                        "day",          // periodType 
                        1,              // frequency = 1
                        "minute"        // frequencyType (minute is default for day)
                    );
                    
                    System.out.println("Minimal Response Status: " + response3.getStatusCode());
                    if (response3.getStatusCode() != 200) {
                        System.out.println("Minimal Response Body: " + response3.getBody());
                    } else {
                        response1 = response3; // Use the working response
                    }
                }
                
                // Process successful response
                if (response1.getStatusCode() == 200) {
                    String responseBody = response1.getBody();
                    System.out.println("SUCCESS! Response Length: " + responseBody.length() + " characters");
                    
                    // Parse and display key information
                    if (responseBody.contains("candles")) {
                        // Count candles (data points)
                        int candleCount = countOccurrences(responseBody, "\"open\":");
                        System.out.println("Data Points Found: " + candleCount);
                        
                        // Show first few characters for inspection
                        if (responseBody.length() > 500) {
                            System.out.println("Sample Data (first 500 chars):");
                            System.out.println(responseBody.substring(0, 500) + "...");
                        } else {
                            System.out.println("Full Response:");
                            System.out.println(responseBody);
                        }
                        
                        // Extract some basic info if possible
                        if (responseBody.contains("\"symbol\":")) {
                            System.out.println("✓ Symbol confirmed in response");
                        }
                        if (responseBody.contains("\"empty\":false")) {
                            System.out.println("✓ Data available (not empty)");
                        }
                        if (responseBody.contains("\"empty\":true")) {
                            System.out.println("⚠ No data available (empty response)");
                        }
                        
                    } else {
                        System.out.println("⚠ Unexpected response format - no candles found");
                        System.out.println("Raw response: " + responseBody);
                    }
                } else {
                    System.out.println("❌ All parameter combinations failed with 404");
                    System.out.println("This suggests the endpoint URL structure might be wrong");
                }
                
            } catch (Exception e) {
                System.err.println("❌ Exception for " + symbol + ": " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("   Caused by: " + e.getCause().getMessage());
                }
            }
        }
        
        // Summary
        System.out.println("\n" + "=".repeat(60));
        System.out.println("HISTORICAL DATA TEST SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Requested: " + days + " days of data for " + symbols.length + " symbols");
        System.out.println("Symbols tested: " + String.join(", ", symbols));
        
        System.out.println("\nTroubleshooting Notes:");
        System.out.println("• 404 errors usually indicate wrong URL format or missing API permissions");
        System.out.println("• Check your Schwab Developer Portal app has 'Market Data Production' enabled");
        System.out.println("• Verify your app is fully approved (not just 'Approved - Pending')");
        System.out.println("• The endpoint might expect different URL structure than /pricehistory/{symbol}");
        System.out.println("• Try testing with different parameter combinations above");
        
        if (days > 90) {
            System.out.println("• Large date ranges may have slower response times");
        }
    }

    private int countOccurrences(String str, String substring) {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    private void tokenManagementMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n--- Token Management ---");
            System.out.println("1. Show file paths");
            System.out.println("2. Clear token files");
            System.out.println("3. Force token refresh");
            System.out.println("4. Back to main menu");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine();
            try {
                switch (choice) {
                    case "1":
                        tokenManager.showTokenFilePathsInstance();
                        break;
                    case "2":
                        tokenManager.clearTokenFilesInstance();
                        break;
                    case "3":
                        forceTokenRefresh();
                        break;
                    case "4":
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 4.");
                }
            } catch (Exception e) {
                System.err.println("An error occurred: " + e.getMessage());
                logger.error("Token management failed", e);
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void forceTokenRefresh() {
        System.out.println("Attempting to force a token refresh...");
        try {
            tokenManager.forceTokenRefreshInstance();
            System.out.println("Token refresh successful.");
        } catch (SchwabApiException e) {
            System.err.println("Failed to force token refresh: " + e.getMessage());
            logger.error("Force token refresh failed", e);
        }
    }

    private void testAutomatedRefresh() {
        System.out.println("\n--- Testing Automated Token Refresh ---");
        System.out.println("Testing automated token refresh...");
        try {
            // Load tokens with auto-refresh enabled
            TokenResponse tokens = tokenManager.loadTokensInstance(true);
            if (tokens != null) {
                System.out.println("Token manager successfully loaded and refreshed tokens automatically.");
                System.out.println("New Access Token Status: " + tokens.getQuickStatus());
                System.out.println("Access token expires at: " + tokens.getExpiresAt());
                System.out.println("Refresh token expires at: " + tokens.getRefreshTokenExpiresAt());
            } else {
                System.out.println("ERROR: No tokens found. Please run the Manual OAuth option (2) first to authorize the application.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred during automated refresh test: " + e.getMessage());
            logger.error("Automated refresh test failed", e);
        }
    }

    private void showConfigurationStatus() {
        System.out.println("\n--- Configuration Status ---");
        System.out.println("App Key Loaded: " + (config.getAppKey() != null && !config.getAppKey().isEmpty()));
        System.out.println("App Secret Loaded: " + (config.getAppSecret() != null && !config.getAppSecret().isEmpty()));
        System.out.println("Redirect URI Loaded: " + (config.getRedirectUri() != null && !config.getRedirectUri().isEmpty()));
        System.out.println("\nToken Files Status:");
        System.out.println("  " + tokenManager.getTokenPropertiesFile() + ": " +
                (new File(tokenManager.getTokenPropertiesFile()).exists() ? "EXISTS" : "MISSING"));
        System.out.println("  " + tokenManager.getRefreshTokenFile() + ": " +
                (new File(tokenManager.getRefreshTokenFile()).exists() ? "EXISTS" : "MISSING"));
        
        System.out.println("\nFull Configuration:");
        config.showConfig();
        
        System.out.println("\nURL Configuration:");
        try {
            System.out.println("  Auth URL: " + (config.getUrls() != null ? config.getUrls().getAuth() : "NULL"));
            System.out.println("  Token URL: " + (config.getUrls() != null ? config.getUrls().getToken() : "NULL"));
            System.out.println("  Market Data URL: " + (config.getUrls() != null ? config.getUrls().getMarketData() : "NULL"));
            System.out.println("  Redirect URI: " + (config.getDefaults() != null ? config.getDefaults().getRedirectUri() : "NULL"));
        } catch (Exception e) {
            System.err.println("Error accessing URL configuration: " + e.getMessage());
        }
        
        // Quick token status check
        System.out.println("\nQuick Token Status:");
        String tokenStatus = marketDataService.getTokenStatus();
        System.out.println("  Service Status: " + tokenStatus);
        System.out.println("  Service Ready: " + marketDataService.isReady());
    }
}