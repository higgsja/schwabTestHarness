//v21
package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.config.SchwabOAuthClient;
import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.exception.SchwabApiException;
import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabapi.service.MarketDataService;
import com.higgstx.schwabapi.model.market.QuoteData;
import com.higgstx.schwabapi.server.OkHttpSSLServer;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TestHarnessRunner implements CommandLineRunner
{

    private static final Logger logger = LoggerFactory.getLogger(
            TestHarnessRunner.class);
    private final SchwabTestConfig config;
    private final TokenManager tokenManager;
    private final MarketDataService marketDataService;

    public TestHarnessRunner(SchwabTestConfig config, TokenManager tokenManager,
            MarketDataService marketDataService)
    {
        this.config = config;
        this.tokenManager = tokenManager;
        this.marketDataService = marketDataService;
    }

    @Override
    public void run(String... args)
    {
        System.out.println(
                "============================================================");
        System.out.println("          SCHWAB API INTERACTIVE TEST HARNESS");
        System.out.println(
                "============================================================");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        while (true)
        {
            displayMenu();
            String choice = scanner.nextLine();
            try
            {
                switch (choice)
                {
                    case "1":
                        showConfigurationStatus();
                        break;
                    case "2":
                        automaticOAuth(scanner);
                        break;
                    case "3":
                        manualOAuth(scanner);
                        break;
                    case "4":
                        tokenManager.showTokenStatus();
                        break;
                    case "5":
                        testAutomatedRefresh();
                        break;
                    case "6":
                        testMarketData();
                        break;
                    case "7":
                        testHistoricalData(scanner);
                        break;
                    case "8":
                        System.out.println("Exiting. Goodbye!");
                        scanner.close();
                        return;
                    default:
                        System.out.println(
                                "Invalid choice. Please enter a number between 1 and 9.");
                }
            }
            catch (Exception e)
            {
                System.err.println("An unexpected error occurred: " + e.
                        getMessage());
                logger.error("Menu option failed", e);
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void displayMenu()
{
    System.out.println("============================================================");
    System.out.println("MAIN MENU - Select an option:");
    System.out.println("============================================================");
    System.out.println("1. Configuration Status");
    System.out.println("2. Automatic OAuth Authorization (HTTPS)");
    System.out.println("3. Manual OAuth Authorization (fallback)");
    System.out.println("4. Check Token Status");
    System.out.println("5. Test Automated Refresh (Forced)");
    System.out.println("6. Test Market Data API");
    System.out.println("7. Test Historical Data");
    System.out.println("8. Exit");
    System.out.println("============================================================");
    System.out.print("Enter your choice (1-9): ");
}

    private void automaticOAuth(Scanner scanner)
    {
        System.out.println("\n--- Automatic OAuth Authorization ---");
        System.out.println(
                "This will start a local web server to handle the OAuth callback automatically.");
        System.out.println(
                "Your browser will open automatically to complete the authorization.");

        // Use HTTPS with OkHttp's SSL support
        String redirectUri = "https://127.0.0.1:8182";

        OkHttpSSLServer callbackServer = null;
        try
        {
            callbackServer = new OkHttpSSLServer();

            // Start server and wait for callback
            CompletableFuture<String> authCodeFuture = callbackServer.
                    startAndWaitForCode(300, TimeUnit.SECONDS);

            System.out.println("\nHTTPS server started successfully!");
            System.out.println("Server URL: " + redirectUri);
            System.out.println(
                    "\nIMPORTANT: Configure this redirect URI in your Schwab Developer Portal:");
            System.out.println("  Redirect URI: " + redirectUri);

            // Test the server first
            System.out.println("\nTesting server accessibility...");
            System.out.println(
                    "Opening " + redirectUri + " should show a styled success page.");
            System.out.println(
                    "You'll see a browser security warning - this is expected for self-signed certificates.");
            System.out.println(
                    "Click 'Advanced' then 'Proceed to 127.0.0.1 (unsafe)' to continue.");

            System.out.print(
                    "\nDid the test page load successfully after bypassing the warning? (y/n): ");
            String testResult = scanner.nextLine();

            if (!testResult.equalsIgnoreCase("y"))
            {
                System.out.println("\nServer test failed. Troubleshooting:");
                System.out.println("1. Port 8182 might be blocked by firewall");
                System.out.println(
                        "2. Another application might be using port 8182");
                System.out.println(
                        "3. Browser might be blocking self-signed certificates completely");
                System.out.println(
                        "4. Try option 3 (Manual OAuth) as a reliable fallback");
                return;
            }

            System.out.println(
                    "Server test successful! The HTTPS server is working properly.");

            // Create OAuth client with HTTPS redirect URI
            SchwabApiProperties apiProperties = new SchwabApiProperties(
                    config.getUrls().getAuth(),
                    config.getUrls().getToken(),
                    config.getUrls().getMarketData(),
                    redirectUri, // HTTPS redirect URI
                    config.getDefaults().getScope(),
                    config.getDefaults().getHttpTimeoutMs()
            );

            try (SchwabOAuthClient client = new SchwabOAuthClient(apiProperties))
            {

                // Build authorization URL
                String authUrl = client.
                        buildAuthorizationUrl(config.getAppKey(), redirectUri);

                System.out.println("\n" + "=".repeat(80));
                System.out.println("STEP 1: Opening Browser for Authorization");
                System.out.println("=".repeat(80));

                // Try to open browser automatically
                if (openBrowser(authUrl))
                {
                    System.out.println(
                            "Browser opened automatically. Please complete the authorization in your browser.");
                }
                else
                {
                    System.out.println(
                            "Could not open browser automatically. Please manually open this URL:");
                    System.out.println(authUrl);
                }

                System.out.println("\nInstructions:");
                System.out.println(
                        "1. Log into your Schwab account in the opened browser");
                System.out.println("2. Review and approve the API permissions");
                System.out.println(
                        "3. Schwab will redirect back to the local server automatically");
                System.out.println(
                        "4. The authorization code will be captured automatically");

                System.out.println("\n" + "=".repeat(80));
                System.out.println("STEP 2: Waiting for Authorization...");
                System.out.println("=".repeat(80));
                System.out.println(
                        "Waiting for authorization callback (timeout: 5 minutes)...");

                // Wait for the authorization code
                String authCode;
                try
                {
                    authCode = authCodeFuture.get(); // This blocks until callback received or timeout
                    System.out.println(
                            "Authorization code received successfully!");

                }
                catch (Exception e)
                {
                    System.err.println(
                            "Failed to receive authorization code: " + e.
                                    getMessage());
                    if (e.getCause() instanceof java.util.concurrent.TimeoutException)
                    {
                        System.err.println(
                                "Timeout waiting for authorization. Please try again.");
                    }
                    return;
                }

                System.out.println("\n" + "=".repeat(80));
                System.out.println("STEP 3: Exchanging Code for Tokens");
                System.out.println("=".repeat(80));

                // Exchange code for tokens
                System.out.println(
                        "Exchanging authorization code for access tokens...");
                TokenResponse tokens = client.getTokens(
                        config.getAppKey(),
                        config.getAppSecret(),
                        authCode,
                        redirectUri
                );

                // Save tokens
                tokenManager.saveTokensInstance(tokens);

                // Save refresh token separately
                try
                {
                    Path refreshTokenPath = Paths.get(tokenManager.
                            getRefreshTokenFile());
                    Files.
                            writeString(refreshTokenPath, tokens.
                                    getRefreshToken());
                }
                catch (IOException e)
                {
                    logger.error("Failed to write refresh token to file: {}", e.
                            getMessage());
                    System.err.println(
                            "Warning: Failed to save refresh token to file. Automated refresh may not work.");
                }

                System.out.println("\n" + "=".repeat(80));
                System.out.println("SUCCESS! OAuth Authorization Complete");
                System.out.println("=".repeat(80));
                System.out.println("Access token expires at: " + tokens.
                        getExpiresAt());
                System.out.println("Refresh token expires at: " + tokens.
                        getRefreshTokenExpiresAt());
                System.out.println(
                        "You can now use the API endpoints (option 6).");

            }
            catch (Exception e)
            {
                System.err.println("OAuth error: " + e.getClass().
                        getSimpleName() + ": " + e.getMessage());
                logger.error("OAuth process failed", e);
            }

        }
        catch (IOException e)
        {
            System.err.println("Failed to create OAuth callback server: " + e.
                    getMessage());
            logger.error("Callback server creation failed", e);
        }
        finally
        {
            if (callbackServer != null)
            {
                try
                {
                    callbackServer.close();
                }
                catch (Exception e)
                {
                    System.err.println(
                            "Warning: Error closing callback server: " + e.
                                    getMessage());
                }
            }
        }
    }

    /**
     * Attempts to open the URL in the default browser
     */
    private boolean openBrowser(String url)
    {
        try
        {
            if (Desktop.isDesktopSupported())
            {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE))
                {
                    desktop.browse(new URI(url));
                    return true;
                }
            }

            // Fallback: try OS-specific commands
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win"))
            {
                pb = new ProcessBuilder("cmd", "/c", "start", url);
            }
            else if (os.contains("mac"))
            {
                pb = new ProcessBuilder("open", url);
            }
            else if (os.contains("nix") || os.contains("nux"))
            {
                pb = new ProcessBuilder("xdg-open", url);
            }
            else
            {
                return false;
            }

            pb.start();
            return true;

        }
        catch (Exception e)
        {
            logger.debug("Failed to open browser: {}", e.getMessage());
            return false;
        }
    }

    private void manualOAuth(Scanner scanner)
    {
        System.out.println("\n--- Manual OAuth Authorization ---");
        System.out.println("App Key: " + config.getAppKey());
        System.out.println("Redirect URI: " + config.getRedirectUri()
                + " (must be configured in Schwab Developer Portal)");

        System.out.println("\nThis process requires manual URL copying.");

        System.out.print("\nProceed with manual OAuth authorization? (y/n): ");
        String response = scanner.nextLine();
        if (!response.equalsIgnoreCase("y"))
        {
            System.out.println("Authorization cancelled.");
            return;
        }

        SchwabOAuthClient client = null;
        try
        {
            // Create SchwabApiProperties manually
            System.out.println("Creating OAuth client...");
            SchwabApiProperties apiProperties = new SchwabApiProperties(
                    config.getUrls().getAuth(), // Auth URL
                    config.getUrls().getToken(), // Token URL  
                    config.getUrls().getMarketData(), // Market Data URL
                    config.getDefaults().getRedirectUri(), // HTTPS Redirect URI
                    config.getDefaults().getScope(), // Scope
                    config.getDefaults().getHttpTimeoutMs() // Timeout
            );

            // Create client with explicit properties
            client = new SchwabOAuthClient(apiProperties);

            System.out.println(
                    "============================================================");
            System.out.println("STEP 1: Browser Authorization");
            System.out.println(
                    "============================================================");
            System.out.println("Building authorization URL...");

            String authUrl = client.buildAuthorizationUrl(config.getAppKey(),
                    config.getRedirectUri());

            System.out.println(
                    "\nPlease manually open this URL in your browser:");
            System.out.println("=".repeat(80));
            System.out.println(authUrl);
            System.out.println("=".repeat(80));

            System.out.println("\nInstructions:");
            System.out.println(
                    "1. Copy the URL above and paste it into your browser");
            System.out.println("2. Log into your Schwab account");
            System.out.println("3. Review and approve the API permissions");
            System.out.println("4. Schwab will redirect to: " + config.
                    getRedirectUri() + "?code=...");
            System.out.println(
                    "5. Browser will show 'connection refused' or security warning - this is EXPECTED!");
            System.out.println(
                    "6. Copy the COMPLETE URL from your browser's address bar");

            System.out.println(
                    "\n============================================================");
            System.out.println("STEP 2: Paste Redirect URL");
            System.out.println(
                    "============================================================");
            System.out.print("Paste the full redirect URL here: ");
            String redirectUrl = scanner.nextLine().trim();

            if (redirectUrl.isEmpty())
            {
                System.err.println("No URL provided.");
                return;
            }

            System.out.println("Extracting authorization code from URL...");
            String authCode = client.extractAuthorizationCode(redirectUrl);
            if (authCode == null)
            {
                System.err.println(
                        "Failed to extract authorization code from the URL.");
                System.err.println(
                        "Make sure you copied the complete URL including the ?code= parameter");
                System.err.println(
                        "Expected format: " + config.getRedirectUri() + "?code=XXXXXX");
                return;
            }
            System.out.println("Authorization code extracted successfully!");

            System.out.println("Exchanging authorization code for tokens...");
            TokenResponse tokens = client.getTokens(config.getAppKey(), config.
                    getAppSecret(),
                    authCode, config.getRedirectUri());

            // Save all tokens to the main JSON file
            tokenManager.saveTokensInstance(tokens);

            // Save the refresh token separately for the refresher to find
            try
            {
                Path refreshTokenPath = Paths.get(tokenManager.
                        getRefreshTokenFile());
                Files.writeString(refreshTokenPath, tokens.getRefreshToken());
            }
            catch (IOException e)
            {
                logger.error("Failed to write refresh token to file: {}", e.
                        getMessage());
                System.err.println(
                        "Warning: Failed to save refresh token to file. Automated refresh may not work.");
            }

            System.out.println("\nSUCCESS! Tokens acquired and saved.");
            System.out.println("Access token expires at: " + tokens.
                    getExpiresAt());
            System.out.println("You can now use the API endpoints (option 6).");

        }
        catch (Exception e)
        {
            System.err.println(
                    "OAuth error: " + e.getClass().getSimpleName() + ": " + e.
                    getMessage());
            logger.error("OAuth process failed", e);

        }
        finally
        {
            if (client != null)
            {
                try
                {
                    client.close();
                }
                catch (Exception e)
                {
                    System.err.println(
                            "Warning: Error closing OAuth client: " + e.
                                    getMessage());
                }
            }
        }
    }

    private void testMarketData()
    {
        System.out.println("\n--- Testing Market Data API ---");

        // First check if we have valid tokens
        System.out.println("Checking token status before API calls...");
        String tokenStatus = marketDataService.getTokenStatus();
        System.out.println("Token Status: " + tokenStatus);

        if (!marketDataService.isReady())
        {
            System.err.println("Service not ready - no valid tokens available.");
            System.out.println(
                    "Please run option 2 (Automatic OAuth Authorization) first.");
            return;
        }

        // Test 1: Check market hours first
        System.out.println("\nTesting market hours...");
        try
        {
            var marketHours = marketDataService.getMarketHours("equity");
            System.out.println(
                    "Market hours request successful. Status: " + marketHours.
                            getStatusCode());
            if (marketHours.getStatusCode() == 200)
            {
                System.out.println("Market hours data length: " + marketHours.
                        getBody().length() + " characters");
                // Show first 200 characters of response for debugging
                String body = marketHours.getBody();
                if (body.length() > 200)
                {
                    System.out.println("Market hours sample: " + body.substring(
                            0, 200) + "...");
                }
                else
                {
                    System.out.println("Market hours data: " + body);
                }
            }
            else
            {
                System.out.println("Market hours error: " + marketHours.
                        getBody());
            }
        }
        catch (Exception e)
        {
            System.err.println("Market hours test failed: " + e.getMessage());
        }

        // Test 2: Try multiple symbols with different formats
        System.out.println("\nTesting quotes for multiple symbols...");
        String[] symbolsToTest =
        {
            "AAPL", "MSFT", "GOOGL", "TSLA", "SPY"
        };

        for (String symbol : symbolsToTest)
        {
            System.out.println("\nTesting symbol: " + symbol);
            try
            {
                QuoteData quote = marketDataService.getQuote(symbol);
                System.out.println("  Result: " + quote.getStatus());

                if (quote.isSuccess())
                {
                    System.out.println("  SUCCESS - Close Price: $" + quote.
                            getClosePrice());
                    System.out.println("  Volume: " + quote.getTotalVolume());
                    break; // Found a working symbol, stop testing
                }
                else
                {
                    System.out.println("  FAILED - " + quote.getErrorMessage());
                }

            }
            catch (Exception e)
            {
                System.err.println("  ERROR: " + e.getMessage());
            }
        }

        // Test 3: Try a batch quote request
        System.out.println("\nTesting batch quote request...");
        try
        {
            List<QuoteData> quotes = marketDataService.getQuotes(List.of("AAPL",
                    "MSFT"));
            System.out.println(
                    "Batch quote request returned " + quotes.size() + " results:");
            for (QuoteData quote : quotes)
            {
                System.out.println("  " + quote.getSymbol() + ": " + quote.
                        getStatus()
                        + (quote.isSuccess() ? " ($" + quote.getClosePrice() + ")" : " - " + quote.
                        getErrorMessage()));
            }
        }
        catch (Exception e)
        {
            System.err.println("Batch quote test failed: " + e.getMessage());
        }

        System.out.println("\n--- Market Data Test Complete ---");
        System.out.println(
                "Note: If all symbols return 'NOT_FOUND', this might be due to:");
        System.out.println("  • Market being closed (try during trading hours)");
        System.out.println("  • API endpoint requiring different symbol format");
        System.out.println("  • Sandbox vs production API differences");
        System.out.println(
                "  • Insufficient API permissions in your Schwab app registration");
        System.out.println(
                "  • Need to request 'market data' scope in addition to 'readonly'");
    }

    private void testHistoricalData(Scanner scanner)
    {
        System.out.println("\n--- Testing Historical Data API ---");

        // First check if we have valid tokens
        System.out.println("Checking token status before API calls...");
        String tokenStatus = marketDataService.getTokenStatus();
        System.out.println("Token Status: " + tokenStatus);

        if (!marketDataService.isReady())
        {
            System.err.println("Service not ready - no valid tokens available.");
            System.out.println(
                    "Please run option 2 (Automatic OAuth Authorization) first.");
            return;
        }

        System.out.print(
                "Enter ticker symbols (comma-separated, e.g., AAPL,MSFT,GOOGL): ");
        String symbolsInput = scanner.nextLine().trim();
        if (symbolsInput.isEmpty())
        {
            symbolsInput = "AAPL,MSFT,GOOGL,TSLA,SPY"; // Default symbols
            System.out.println(
                    "No symbols entered, using default: " + symbolsInput);
        }

        String[] symbols = symbolsInput.split(",");
        for (int i = 0; i < symbols.length; i++)
        {
            symbols[i] = symbols[i].trim().toUpperCase();
        }

        System.out.println(
                "\nFetching 30 days of historical data for " + symbols.length + " symbols...");
        System.out.println("Base Market Data URL: " + config.getUrls().
                getMarketData());

        // Test historical data for each symbol
        for (String symbol : symbols)
        {
            System.out.println("\n" + "=".repeat(60));
            System.out.println(
                    "Historical Data for " + symbol + "30 days)");
            System.out.println("=".repeat(60));

            try
            {
                String expectedUrl = String.format(
                        "%s/pricehistory?symbol=%s&periodType=month&period=1&frequencyType=daily&frequency=1&needPreviousClose=true",
                        config.getUrls().getMarketData(), symbol
                );
                System.out.println("Expected URL: " + expectedUrl);

                var response1 = marketDataService.getPriceHistory(
                        symbol,
                        "month", // period type
                        1, // number of periods
                        "daily", // frequencyType
                        1 // frequency
                );

                System.out.println("Response Status: " + response1.
                        getStatusCode());
                if (response1.getStatusCode() != 200)
                {
                    System.out.println("Response Body: " + response1.getBody());
                }

                // Process successful response
                if (response1.getStatusCode() == 200)
                {
                    String responseBody = response1.getBody();
                    System.out.println(
                            "SUCCESS! Response Length: " + responseBody.
                                    length() + " characters");

                    // Parse and display key information
                    if (responseBody.contains("candles"))
                    {
                        // Count candles (data points)
                        int candleCount = countOccurrences(responseBody,
                                "\"open\":");
                        System.out.println("Data Points Found: " + candleCount);

                        // Show first few characters for inspection
                        if (responseBody.length() > 500)
                        {
                            System.out.println("Sample Data (first 500 chars):");
                            System.out.println(
                                    responseBody.substring(0, 500) + "...");
                        }
                        else
                        {
                            System.out.println("Full Response:");
                            System.out.println(responseBody);
                        }

                        // Extract some basic info if possible
                        if (responseBody.contains("\"symbol\":"))
                        {
                            System.out.println("✓ Symbol confirmed in response");
                        }
                        if (responseBody.contains("\"empty\":false"))
                        {
                            System.out.println("✓ Data available (not empty)");
                        }
                        if (responseBody.contains("\"empty\":true"))
                        {
                            System.out.println(
                                    "⚠ No data available (empty response)");
                        }

                    }
                    else
                    {
                        System.out.println(
                                "⚠ Unexpected response format - no candles found");
                        System.out.println("Raw response: " + responseBody);
                    }
                }
                else
                {
                    System.out.println(
                            "⚠ All parameter combinations failed with 404");
                    System.out.println(
                            "This suggests the endpoint URL structure might be wrong");
                }

            }
            catch (SchwabApiException | IOException e)
            {
                System.err.println("⚠ Exception for " + symbol + ": " + e.
                        getMessage());
                if (e.getCause() != null)
                {
                    System.err.println("   Caused by: " + e.getCause().
                            getMessage());
                }
            }
        }

        // Summary
        System.out.println(
                "\n" + "=".repeat(60));
        System.out.println(
                "HISTORICAL DATA TEST SUMMARY");
        System.out.println(
                "=".repeat(60));
        System.out.println(
                "Requested: 30 days of data for " + symbols.length + " symbols");
        System.out.println(
                "Symbols tested: " + String.join(", ", symbols));

        System.out.println(
                "\nTroubleshooting Notes:");
        System.out.println(
                "• 404 errors usually indicate wrong URL format or missing API permissions");
        System.out.println(
                "• Check your Schwab Developer Portal app has 'Market Data Production' enabled");
        System.out.println(
                "• Verify your app is fully approved (not just 'Approved - Pending')");
        System.out.println(
                "• The endpoint might expect different URL structure than /pricehistory/{symbol}");
        System.out.println(
                "• Try testing with different parameter combinations above");

    }

    private int countOccurrences(String str, String substring)
    {
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(substring, index)) != -1)
        {
            count++;
            index += substring.length();
        }
        return count;
    }

   

    private void testAutomatedRefresh()
{
    System.out.println("\n--- Testing Automated Token Refresh (Forced) ---");
    System.out.println("This option will force a token refresh regardless of current token validity.");
    
    // First, show current token status
    System.out.println("STEP 1: Current token status before forced refresh...");
    try
    {
        TokenResponse currentTokens = tokenManager.loadTokensInstance(false);
        if (currentTokens == null)
        {
            System.out.println("ERROR: No tokens found. Please run OAuth authorization first (option 2).");
            return;
        }
        
        System.out.println("Current token details:");
        System.out.println("  Access token valid: " + currentTokens.isAccessTokenValid());
        System.out.println("  Refresh token valid: " + currentTokens.isRefreshTokenValid());
        System.out.println("  Access expires in: " + currentTokens.getSecondsUntilAccessExpiry() + " seconds");
        System.out.println("  Refresh expires in: " + currentTokens.getSecondsUntilRefreshExpiry() + " seconds");
        System.out.println("  Current expires at: " + currentTokens.getExpiresAt());
        
        if (!currentTokens.isRefreshTokenValid())
        {
            System.out.println("ERROR: Refresh token is invalid - cannot force refresh");
            System.out.println("Please run OAuth authorization first (option 2)");
            return;
        }
        
    }
    catch (Exception e)
    {
        System.err.println("Error checking current token status: " + e.getMessage());
        return;
    }
    
    System.out.println("\nSTEP 2: Forcing token refresh...");
    try
    {
        // Use the existing force refresh method
        TokenResponse refreshedTokens = tokenManager.forceTokenRefreshInstance();
        
        if (refreshedTokens != null)
        {
            System.out.println("SUCCESS: Forced token refresh completed");
            System.out.println("Post-refresh token details:");
            System.out.println("  New access token valid: " + refreshedTokens.isAccessTokenValid());
            System.out.println("  New refresh token valid: " + refreshedTokens.isRefreshTokenValid());
            System.out.println("  New access expires at: " + refreshedTokens.getExpiresAt());
            System.out.println("  New refresh expires at: " + refreshedTokens.getRefreshTokenExpiresAt());
            System.out.println("  New expires in: " + refreshedTokens.getSecondsUntilAccessExpiry() + " seconds");
            System.out.println("  Token source: " + refreshedTokens.getSource());
            
            // Verify the refreshed token works with API
            System.out.println("\nSTEP 3: Verifying refreshed tokens work with API...");
            String marketServiceStatus = marketDataService.getTokenStatus();
            System.out.println("Market service status: " + marketServiceStatus);
            System.out.println("Market service ready: " + marketDataService.isReady());
            
        }
        else
        {
            System.out.println("ERROR: Force refresh returned null");
        }
        
    }
    catch (Exception e)
    {
        System.err.println("FAILURE: Forced refresh failed");
        System.err.println("Error: " + e.getMessage());
        System.err.println("Error type: " + e.getClass().getSimpleName());
        
        if (e instanceof SchwabApiException)
        {
            SchwabApiException apiEx = (SchwabApiException) e;
            System.err.println("Status Code: " + apiEx.getStatusCode());
            System.err.println("Error Code: " + apiEx.getErrorCode());
            System.err.println("Recommended Action: " + apiEx.getRecommendedAction());
        }
        
        logger.error("Forced refresh test failed", e);
        
        System.out.println("\nTroubleshooting:");
        System.out.println("1. Check appKey/appSecret in application.yml");
        System.out.println("2. Verify refresh token hasn't expired");
        System.out.println("3. Check network connectivity to Schwab API");
        System.out.println("4. If all else fails, run OAuth authorization again (option 2)");
    }
}

    private void showConfigurationStatus()
    {
        System.out.println("\n--- Configuration Status ---");
        System.out.println(
                "App Key Loaded: " + (config.getAppKey() != null && !config.
                getAppKey().isEmpty()));
        System.out.println(
                "App Secret Loaded: " + (config.getAppSecret() != null && !config.
                getAppSecret().isEmpty()));
        System.out.println(
                "Redirect URI Loaded: " + (config.getRedirectUri() != null && !config.
                getRedirectUri().isEmpty()));
        System.out.println("\nToken Files Status:");
        System.out.println("  " + tokenManager.getTokenPropertiesFile() + ": "
                + (new File(tokenManager.getTokenPropertiesFile()).exists() ? "EXISTS" : "MISSING"));
        System.out.println("  " + tokenManager.getRefreshTokenFile() + ": "
                + (new File(tokenManager.getRefreshTokenFile()).exists() ? "EXISTS" : "MISSING"));

        System.out.println("\nFull Configuration:");
        config.showConfig();

        System.out.println("\nURL Configuration:");
        try
        {
            System.out.println(
                    "  Auth URL: " + (config.getUrls() != null ? config.
                    getUrls().getAuth() : "NULL"));
            System.out.println(
                    "  Token URL: " + (config.getUrls() != null ? config.
                    getUrls().getToken() : "NULL"));
            System.out.println(
                    "  Market Data URL: " + (config.getUrls() != null ? config.
                    getUrls().getMarketData() : "NULL"));
            System.out.println(
                    "  Redirect URI: " + (config.getDefaults() != null ? config.
                    getDefaults().getRedirectUri() : "NULL"));
        }
        catch (Exception e)
        {
            System.err.println("Error accessing URL configuration: " + e.
                    getMessage());
        }

        // Quick token status check
        System.out.println("\nQuick Token Status:");
        String tokenStatus = marketDataService.getTokenStatus();
        System.out.println("  Service Status: " + tokenStatus);
        System.out.println("  Service Ready: " + marketDataService.isReady());
    }
}