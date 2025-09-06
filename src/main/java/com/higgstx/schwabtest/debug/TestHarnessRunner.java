package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.config.SchwabOAuthClient;
import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.exception.SchwabApiException;
import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabapi.service.MarketDataService;
import com.higgstx.schwabapi.model.market.QuoteData;
import com.higgstx.schwabapi.model.market.DailyPriceData;
import com.higgstx.schwabapi.server.OkHttpSSLServer;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.stream.Collectors;

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
                handleMenuChoice(choice, scanner);
            } catch (SchwabApiException e) {
                handleApiException(e);
            } catch (Exception e) {
                handleGenericException(e);
            }
            
            if ("9".equals(choice)) {
                scanner.close();
                return;
            }
            
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void handleMenuChoice(String choice, Scanner scanner) throws SchwabApiException {
        switch (choice) {
            case "1" -> showConfigurationStatus();
            case "2" -> automaticOAuth(scanner);
            case "3" -> manualOAuth(scanner);
            case "4" -> tokenManager.showTokenStatus();
            case "5" -> testAutomatedRefresh();
            case "6" -> testMarketData();
            case "7" -> testHistoricalData(scanner);
            case "8" -> testBulkHistoricalData(scanner);
            case "9" -> {
                System.out.println("Exiting. Goodbye!");
                return;
            }
            default -> System.out.println("Invalid choice. Please enter a number between 1 and 9.");
        }
    }

    private void handleApiException(SchwabApiException e) {
        System.err.println("\nAPI Error Details:");
        System.err.println("  Message: " + e.getDisplayMessage());
        System.err.println("  Category: " + e.getErrorCategory().getDescription());
        System.err.println("  Status Code: " + e.getStatusCode());
        System.err.println("  Error Code: " + e.getErrorCode());
        System.err.println("  Recommended Action: " + e.getRecommendedAction());
        
        if (e.shouldAlert()) {
            System.err.println("  Severity: " + e.getSeverity());
        }
        
        if (e.isRetryable()) {
            System.err.println("  Note: This error is retryable. Wait " + e.getRetryAfterSeconds() + " seconds before retrying.");
        }
        
        logger.error("Menu option failed with API exception", e);
    }

    private void handleGenericException(Exception e) {
        System.err.println("An unexpected error occurred: " + e.getMessage());
        logger.error("Menu option failed", e);
    }

    private void displayMenu() {
        System.out.println("============================================================");
        System.out.println("MAIN MENU - Select an option:");
        System.out.println("============================================================");
        System.out.println("1. Configuration Status");
        System.out.println("2. Automatic OAuth Authorization (HTTPS)");
        System.out.println("3. Manual OAuth Authorization (fallback)");
        System.out.println("4. Check Token Status");
        System.out.println("5. Test Automated Refresh (Forced)");
        System.out.println("6. Test Market Data API");
        System.out.println("7. Test Historical Data (Individual)");
        System.out.println("8. Test Bulk Historical Data");
        System.out.println("9. Exit");
        System.out.println("============================================================");
        System.out.print("Enter your choice (1-9): ");
    }

    private void testBulkHistoricalData(Scanner scanner) throws SchwabApiException {
        System.out.println("\n--- Testing Bulk Historical Data API ---");
        System.out.println("This test uses the getBulkHistoricalData method to fetch 30 days of data for multiple symbols.");

        if (!marketDataService.ensureServiceReady("testBulkHistoricalData")) {
            System.out.println("Service not ready. Please ensure tokens are valid and try again.");
            return;
        }

        System.out.print("Enter ticker symbols (comma-separated, e.g., AAPL,MSFT,GOOGL): ");
        String symbolsInput = scanner.nextLine().trim();
        if (symbolsInput.isEmpty()) {
            symbolsInput = "AAPL,MSFT,GOOGL,TSLA,SPY";
            System.out.println("No symbols entered, using default: " + symbolsInput);
        }

        String[] symbols = symbolsInput.split(",");
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = symbols[i].trim().toUpperCase();
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("BULK HISTORICAL DATA TEST");
        System.out.println("=".repeat(70));
        System.out.println("Symbols to fetch: " + String.join(", ", symbols));
        System.out.println("Period: 30 days (1 month of daily data)");

        long startTime = System.currentTimeMillis();

        try {
            System.out.println("\nCalling getBulkHistoricalData...");
            List<DailyPriceData> bulkData = marketDataService.getBulkHistoricalData(symbols);

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            displayBulkResults(symbols, bulkData, totalTime);

        } catch (SchwabApiException e) {
            System.err.println("API Error during bulk fetch:");
            System.err.println("  " + e.getDisplayMessage());
            System.err.println("  Recommended Action: " + e.getRecommendedAction());
            throw e;
        }
    }

    private void displayBulkResults(String[] symbols, List<DailyPriceData> bulkData, long totalTime) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("BULK FETCH RESULTS");
        System.out.println("=".repeat(70));
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Total data points returned: " + bulkData.size());
        System.out.println("Symbols requested: " + symbols.length);

        Map<String, List<DailyPriceData>> dataBySymbol = bulkData.stream()
                .collect(Collectors.groupingBy(DailyPriceData::getSymbol));

        System.out.println("\nResults by symbol:");
        System.out.println("-".repeat(50));

        for (String symbol : symbols) {
            displaySymbolResults(symbol, dataBySymbol.get(symbol));
        }

        displayPerformanceAnalysis(symbols, bulkData, totalTime);
        displayDataQualityAnalysis(bulkData);
        displayRecentSamples(bulkData);
        displayTestNotes();
    }

    private void displaySymbolResults(String symbol, List<DailyPriceData> symbolData) {
        if (symbolData == null || symbolData.isEmpty()) {
            System.out.println(symbol + ": No data returned");
            return;
        }

        long successCount = symbolData.stream().filter(DailyPriceData::isSuccess).count();
        long errorCount = symbolData.size() - successCount;

        System.out.println(symbol + ":");
        System.out.println("  Total data points: " + symbolData.size());
        System.out.println("  Successful: " + successCount);
        System.out.println("  Errors: " + errorCount);

        if (successCount > 0) {
            displaySuccessfulDataSample(symbolData);
            displayDateRange(symbolData);
        } else if (errorCount > 0) {
            displayErrorSample(symbolData);
        }
        System.out.println();
    }

    private void displaySuccessfulDataSample(List<DailyPriceData> symbolData) {
        DailyPriceData sample = symbolData.stream()
                .filter(DailyPriceData::isSuccess)
                .findFirst().orElse(null);

        if (sample != null) {
            System.out.println("  Sample data:");
            System.out.println("    Date: " + sample.getLocalDate());
            System.out.println("    Open: $" + sample.getOpen());
            System.out.println("    High: $" + sample.getHigh());
            System.out.println("    Low: $" + sample.getLow());
            System.out.println("    Close: $" + sample.getClose());
            System.out.println("    Volume: " + formatVolume(sample.getVolume()));
        }
    }

    private void displayDateRange(List<DailyPriceData> symbolData) {
        List<DailyPriceData> successfulData = symbolData.stream()
                .filter(DailyPriceData::isSuccess)
                .sorted((a, b) -> a.getLocalDate().compareTo(b.getLocalDate()))
                .collect(Collectors.toList());

        if (successfulData.size() > 1) {
            System.out.println("  Date range: "
                    + successfulData.get(0).getLocalDate() + " to "
                    + successfulData.get(successfulData.size() - 1).getLocalDate());
        }
    }

    private void displayErrorSample(List<DailyPriceData> symbolData) {
        DailyPriceData errorSample = symbolData.stream()
                .filter(data -> !data.isSuccess())
                .findFirst().orElse(null);

        if (errorSample != null && errorSample.getErrorMessage() != null) {
            System.out.println("  Error: " + errorSample.getErrorMessage());
        }
    }

    private void displayPerformanceAnalysis(String[] symbols, List<DailyPriceData> bulkData, long totalTime) {
        System.out.println("PERFORMANCE ANALYSIS:");
        System.out.println("-".repeat(50));
        System.out.println("Average time per symbol: " + (totalTime / symbols.length) + "ms");
        System.out.println("Data points per second: " + (bulkData.size() * 1000 / Math.max(totalTime, 1)));
    }

    private void displayDataQualityAnalysis(List<DailyPriceData> bulkData) {
        long totalSuccessful = bulkData.stream().filter(DailyPriceData::isSuccess).count();
        long totalErrors = bulkData.size() - totalSuccessful;
        double successRate = (double) totalSuccessful / bulkData.size() * 100;

        System.out.println("\nDATA QUALITY:");
        System.out.println("-".repeat(50));
        System.out.println("Success rate: " + String.format("%.1f%%", successRate));
        System.out.println("Successful data points: " + totalSuccessful);
        System.out.println("Failed data points: " + totalErrors);
    }

    private void displayRecentSamples(List<DailyPriceData> bulkData) {
        long totalSuccessful = bulkData.stream().filter(DailyPriceData::isSuccess).count();
        
        if (totalSuccessful > 0) {
            System.out.println("\nMOST RECENT DATA SAMPLES:");
            System.out.println("-".repeat(50));

            bulkData.stream()
                    .filter(DailyPriceData::isSuccess)
                    .sorted((a, b) -> b.getLocalDate().compareTo(a.getLocalDate()))
                    .limit(5)
                    .forEach(data -> {
                        System.out.println(data.getSymbol() + " (" + data.getLocalDate() + "): "
                                + "Close $" + data.getClose() + ", Volume " + formatVolume(data.getVolume()));
                    });
        }
    }

    private void displayTestNotes() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("BULK HISTORICAL DATA TEST COMPLETE");
        System.out.println("=".repeat(70));

        System.out.println("Notes about this test:");
        System.out.println("• getBulkHistoricalData() makes individual API calls for each symbol");
        System.out.println("• Includes 100ms delay between requests to respect API rate limits");
        System.out.println("• Returns all data in a single List<DailyPriceData>");
        System.out.println("• Gracefully handles errors by returning error data objects");
        System.out.println("• Fetches 30 days (1 month) of daily OHLCV data per symbol");

        String currentTokenStatus = marketDataService.getTokenStatus();
        if (currentTokenStatus.contains("ERROR") || !marketDataService.isReady()) {
            System.out.println("\nTroubleshooting:");
            System.out.println("• Token issues detected - try option 4 to check token status");
            System.out.println("• If tokens expired, run option 5 (force refresh) or option 2 (re-auth)");
        }
    }

    private String formatVolume(Long volume) {
        if (volume == null) return "N/A";

        if (volume >= 1_000_000_000) {
            return String.format("%.1fB", volume / 1_000_000_000.0);
        } else if (volume >= 1_000_000) {
            return String.format("%.1fM", volume / 1_000_000.0);
        } else if (volume >= 1_000) {
            return String.format("%.1fK", volume / 1_000.0);
        } else {
            return volume.toString();
        }
    }

    private void automaticOAuth(Scanner scanner) throws SchwabApiException {
        System.out.println("\n--- Automatic OAuth Authorization ---");
        System.out.println("This will start a local HTTPS server to handle the OAuth callback automatically.");

        String redirectUri = "https://127.0.0.1:8182";
        OkHttpSSLServer callbackServer = null;

        try {
            callbackServer = new OkHttpSSLServer();
            CompletableFuture<String> authCodeFuture = callbackServer.startAndWaitForCode(300, TimeUnit.SECONDS);

            System.out.println("\nHTTPS server started successfully on: " + redirectUri);
            System.out.println("\nIMPORTANT: Ensure this redirect URI is configured in your Schwab Developer Portal");

            if (!testServerAccessibility(scanner, redirectUri)) {
                return;
            }

            SchwabApiProperties apiProperties = createApiProperties(redirectUri);
            
            try (SchwabOAuthClient client = new SchwabOAuthClient(apiProperties)) {
                String authUrl = client.buildAuthorizationUrl(config.getAppKey(), redirectUri);
                
                performAutomaticOAuthFlow(scanner, authUrl, authCodeFuture, client, redirectUri);
            }

        } catch (Exception e) {
            if (e instanceof SchwabApiException) {
                throw (SchwabApiException) e;
            }
            throw SchwabApiException.networkError("automatic OAuth flow", e);
        } finally {
            closeServerSafely(callbackServer);
        }
    }

    private boolean testServerAccessibility(Scanner scanner, String redirectUri) {
        System.out.println("\nTesting server accessibility...");
        System.out.println("You'll see a browser security warning - this is expected for self-signed certificates.");
        System.out.println("Click 'Advanced' then 'Proceed to 127.0.0.1 (unsafe)' to continue.");

        System.out.print("\nDid the test page load successfully after bypassing the warning? (y/n): ");
        String testResult = scanner.nextLine();

        if (!testResult.equalsIgnoreCase("y")) {
            System.out.println("\nServer test failed. Try option 3 (Manual OAuth) as a fallback.");
            return false;
        }

        System.out.println("Server test successful!");
        return true;
    }

    private SchwabApiProperties createApiProperties(String redirectUri) throws SchwabApiException {
        return new SchwabApiProperties(
                config.getUrls().getAuth(),
                config.getUrls().getToken(),
                config.getUrls().getMarketData(),
                redirectUri,
                config.getDefaults().getScope(),
                config.getDefaults().getHttpTimeoutMs()
        );
    }

    private void performAutomaticOAuthFlow(Scanner scanner, String authUrl, CompletableFuture<String> authCodeFuture, 
                                         SchwabOAuthClient client, String redirectUri) throws SchwabApiException {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("STEP 1: Opening Browser for Authorization");
        System.out.println("=".repeat(80));

        if (openBrowser(authUrl)) {
            System.out.println("Browser opened automatically.");
        } else {
            System.out.println("Please manually open this URL: " + authUrl);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("STEP 2: Waiting for Authorization...");
        System.out.println("=".repeat(80));

        try {
            String authCode = authCodeFuture.get();
            System.out.println("Authorization code received successfully!");

            System.out.println("\n" + "=".repeat(80));
            System.out.println("STEP 3: Exchanging Code for Tokens");
            System.out.println("=".repeat(80));

            TokenResponse tokens = client.getTokens(
                    config.getAppKey(),
                    config.getAppSecret(),
                    authCode,
                    redirectUri
            );

            saveTokensAndComplete(tokens);

        } catch (Exception e) {
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                throw SchwabApiException.timeout("Authorization timed out after 5 minutes");
            }
            throw SchwabApiException.networkError("OAuth authorization", e);
        }
    }

    private boolean openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    return true;
                }
            }

            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", url);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else if (os.contains("nix") || os.contains("nux")) {
                pb = new ProcessBuilder("xdg-open", url);
            } else {
                return false;
            }

            pb.start();
            return true;

        } catch (Exception e) {
            logger.debug("Failed to open browser: {}", e.getMessage());
            return false;
        }
    }

    private void saveTokensAndComplete(TokenResponse tokens) throws SchwabApiException {
        // Use the correct method names from TokenManager
        tokenManager.saveTokens(tokens);

        try {
            // Write refresh token to file manually since there's no getter for the file path
            Path refreshTokenPath = Paths.get("schwab-refresh-token.txt");
            Files.writeString(refreshTokenPath, tokens.getRefreshToken());
        } catch (IOException e) {
            logger.error("Failed to write refresh token to file: {}", e.getMessage());
            System.err.println("Warning: Failed to save refresh token to file.");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("SUCCESS! OAuth Authorization Complete");
        System.out.println("=".repeat(80));
        System.out.println("Access token expires at: " + tokens.getExpiresAt());
        System.out.println("Refresh token expires at: " + tokens.getRefreshTokenExpiresAt());
        System.out.println("You can now use the API endpoints (options 6, 7, or 8).");
    }

    private void closeServerSafely(OkHttpSSLServer callbackServer) {
        if (callbackServer != null) {
            try {
                callbackServer.close();
            } catch (Exception e) {
                System.err.println("Warning: Error closing callback server: " + e.getMessage());
            }
        }
    }

    private void manualOAuth(Scanner scanner) throws SchwabApiException {
        System.out.println("\n--- Manual OAuth Authorization ---");
        System.out.println("This process requires manual URL copying and pasting.");

        System.out.print("\nProceed with manual OAuth authorization? (y/n): ");
        if (!scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.println("Authorization cancelled.");
            return;
        }

        SchwabApiProperties apiProperties = new SchwabApiProperties(
                config.getUrls().getAuth(),
                config.getUrls().getToken(),
                config.getUrls().getMarketData(),
                config.getDefaults().getRedirectUri(),
                config.getDefaults().getScope(),
                config.getDefaults().getHttpTimeoutMs()
        );

        try (SchwabOAuthClient client = new SchwabOAuthClient(apiProperties)) {
            performManualOAuthFlow(scanner, client);
        }
    }

    private void performManualOAuthFlow(Scanner scanner, SchwabOAuthClient client) throws SchwabApiException {
        System.out.println("============================================================");
        System.out.println("STEP 1: Browser Authorization");
        System.out.println("============================================================");

        String authUrl = client.buildAuthorizationUrl(config.getAppKey(), config.getRedirectUri());

        System.out.println("\nPlease manually open this URL in your browser:");
        System.out.println("=".repeat(80));
        System.out.println(authUrl);
        System.out.println("=".repeat(80));

        System.out.println("\nInstructions:");
        System.out.println("1. Copy the URL above and paste it into your browser");
        System.out.println("2. Log into your Schwab account");
        System.out.println("3. Review and approve the API permissions");
        System.out.println("4. Copy the COMPLETE redirect URL from your browser's address bar");

        System.out.println("\n============================================================");
        System.out.println("STEP 2: Paste Redirect URL");
        System.out.println("============================================================");
        System.out.print("Paste the full redirect URL here: ");
        String redirectUrl = scanner.nextLine().trim();

        if (redirectUrl.isEmpty()) {
            throw SchwabApiException.validationError("No URL provided");
        }

        String authCode = client.extractAuthorizationCode(redirectUrl);
        if (authCode == null) {
            throw SchwabApiException.validationError(
                "Failed to extract authorization code. Ensure you copied the complete URL with ?code= parameter");
        }

        System.out.println("Authorization code extracted successfully!");
        System.out.println("Exchanging authorization code for tokens...");

        TokenResponse tokens = client.getTokens(
                config.getAppKey(), 
                config.getAppSecret(),
                authCode, 
                config.getRedirectUri()
        );

        saveTokensAndComplete(tokens);
    }

    private void testMarketData() throws SchwabApiException {
        System.out.println("\n--- Testing Market Data API ---");

        if (!marketDataService.ensureServiceReady("testMarketData")) {
            System.out.println("Service not ready. Please ensure tokens are valid.");
            return;
        }

        testMarketHours();
        testQuoteSymbols();
        testBatchQuotes();
        displayMarketDataSummary();
    }

    private void testMarketHours() {
        System.out.println("\nTesting market hours...");
        try {
            var marketHours = marketDataService.getMarketHours("equity");
            System.out.println("Market hours request successful. Status: " + marketHours.getStatusCode());
            
            if (marketHours.getStatusCode() == 200) {
                String body = marketHours.getBody();
                System.out.println("Market hours data length: " + body.length() + " characters");
                
                if (body.length() > 200) {
                    System.out.println("Sample: " + body.substring(0, 200) + "...");
                } else {
                    System.out.println("Data: " + body);
                }
            } else {
                System.out.println("Error: " + marketHours.getBody());
            }
        } catch (SchwabApiException e) {
            System.err.println("Market hours error: " + e.getDisplayMessage());
        } catch (Exception e) {
            System.err.println("Market hours test failed: " + e.getMessage());
        }
    }

    private void testQuoteSymbols() throws SchwabApiException {
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
                    break; // Found working symbol
                } else {
                    System.out.println("  FAILED - " + quote.getErrorMessage());
                }
            } catch (SchwabApiException e) {
                System.err.println("  API ERROR: " + e.getDisplayMessage());
            }
        }
    }

    private void testBatchQuotes() throws SchwabApiException {
        System.out.println("\nTesting batch quote request...");
        try {
            List<QuoteData> quotes = marketDataService.getQuotes(List.of("AAPL", "MSFT"));
            System.out.println("Batch quote request returned " + quotes.size() + " results:");
            
            for (QuoteData quote : quotes) {
                String result = quote.getSymbol() + ": " + quote.getStatus();
                if (quote.isSuccess()) {
                    result += " ($" + quote.getClosePrice() + ")";
                } else {
                    result += " - " + quote.getErrorMessage();
                }
                System.out.println("  " + result);
            }
        } catch (SchwabApiException e) {
            System.err.println("Batch quote error: " + e.getDisplayMessage());
        }
    }

    private void displayMarketDataSummary() {
        System.out.println("\n--- Market Data Test Complete ---");
        System.out.println("Note: If symbols return 'NOT_FOUND', check:");
        System.out.println("  • Market hours (try during trading hours)");
        System.out.println("  • API permissions in Schwab Developer Portal");
        System.out.println("  • App approval status");
    }

    private void testHistoricalData(Scanner scanner) throws SchwabApiException {
        System.out.println("\n--- Testing Historical Data API ---");

        if (!marketDataService.ensureServiceReady("testHistoricalData")) {
            return;
        }

        System.out.print("Enter ticker symbol (default: AAPL): ");
        String symbolInput = scanner.nextLine().trim();
        if (symbolInput.isEmpty()) {
            symbolInput = "AAPL";
        }

        String symbol = symbolInput.toUpperCase();
        System.out.println("\nFetching 30 days of historical data for: " + symbol);

        try {
            var response = marketDataService.getPriceHistory(symbol, "month", 1, "daily", 1);
            
            System.out.println("Response Status: " + response.getStatusCode());
            
            if (response.getStatusCode() == 200) {
                analyzeHistoricalResponse(symbol, response.getBody());
            } else {
                System.out.println("Error Response: " + response.getBody());
            }
            
        } catch (SchwabApiException e) {
            System.err.println("Historical data error: " + e.getDisplayMessage());
            System.err.println("Recommended Action: " + e.getRecommendedAction());
        }
    }

    private void analyzeHistoricalResponse(String symbol, String responseBody) {
        System.out.println("SUCCESS! Response Length: " + responseBody.length() + " characters");

        if (responseBody.contains("candles")) {
            int candleCount = countOccurrences(responseBody, "\"open\":");
            System.out.println("Data Points Found: " + candleCount);

            if (responseBody.length() > 500) {
                System.out.println("Sample Data: " + responseBody.substring(0, 500) + "...");
            } else {
                System.out.println("Full Response: " + responseBody);
            }

            if (responseBody.contains("\"empty\":false")) {
                System.out.println("✓ Data available");
            } else if (responseBody.contains("\"empty\":true")) {
                System.out.println("⚠ No data available");
            }
        } else {
            System.out.println("⚠ Unexpected response format");
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

    private void testAutomatedRefresh() throws SchwabApiException {
        System.out.println("\n--- Testing Automated Token Refresh (Forced) ---");

        TokenResponse currentTokens = tokenManager.loadTokens(false);
        if (currentTokens == null) {
            throw SchwabApiException.notFound("No tokens found. Please run OAuth authorization first (option 2).");
        }

        System.out.println("STEP 1: Current token status");
        displayTokenStatus(currentTokens);

        if (!currentTokens.isRefreshTokenValid()) {
            throw SchwabApiException.tokenError("Refresh token is invalid - cannot force refresh");
        }

        System.out.println("\nSTEP 2: Forcing token refresh...");
        
        TokenResponse refreshedTokens = tokenManager.forceTokenRefresh();
        
        System.out.println("SUCCESS: Token refresh completed");
        displayTokenStatus(refreshedTokens);

        System.out.println("\nSTEP 3: Verifying refreshed tokens...");
        String marketServiceStatus = marketDataService.getTokenStatus();
        System.out.println("Market service status: " + marketServiceStatus);
        System.out.println("Market service ready: " + marketDataService.isReady());
    }

    private void displayTokenStatus(TokenResponse tokens) {
        System.out.println("  Access token valid: " + tokens.isAccessTokenValid());
        System.out.println("  Refresh token valid: " + tokens.isRefreshTokenValid());
        System.out.println("  Access expires in: " + tokens.getSecondsUntilAccessExpiry() + " seconds");
        System.out.println("  Access expires at: " + tokens.getExpiresAt());
        if (tokens.getSource() != null) {
            System.out.println("  Token source: " + tokens.getSource());
        }
    }

    private void showConfigurationStatus() {
        System.out.println("\n--- Configuration Status ---");
        
        System.out.println("Credentials:");
        System.out.println("  App Key: " + (config.getAppKey() != null ? "LOADED" : "MISSING"));
        System.out.println("  App Secret: " + (config.getAppSecret() != null ? "LOADED" : "MISSING"));
        
        System.out.println("\nToken Files:");
        System.out.println("  " + tokenManager.getTokenFilePath() + ": " + 
            (Files.exists(Paths.get(tokenManager.getTokenFilePath())) ? "EXISTS" : "MISSING"));
        // Hard-coded refresh token file since we don't have a getter
        System.out.println("  schwab-refresh-token.txt: " + 
            (Files.exists(Paths.get("schwab-refresh-token.txt")) ? "EXISTS" : "MISSING"));

        System.out.println("\nAPI Endpoints:");
        System.out.println("  Auth URL: " + config.getUrls().getAuth());
        System.out.println("  Token URL: " + config.getUrls().getToken());
        System.out.println("  Market Data URL: " + config.getUrls().getMarketData());
        System.out.println("  Redirect URI: " + config.getDefaults().getRedirectUri());

        System.out.println("\nService Status:");
        System.out.println("  Token Status: " + marketDataService.getTokenStatus());
        System.out.println("  Service Ready: " + marketDataService.isReady());
    }
}