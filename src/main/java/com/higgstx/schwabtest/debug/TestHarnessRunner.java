package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.config.SchwabOAuthClient;
import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import com.higgstx.schwabtest.util.LoggingUtil;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Interactive test harness for Schwab API operations with manual-only OAuth
 */
public class TestHarnessRunner
{

    private static final Scanner scanner = new Scanner(System.in);
    private static SchwabTestConfig testConfig;

    public static void main(String[] args)
    {
        LoggingUtil.initializeLogging();
        setupConfiguration();

        System.out.println(
                "============================================================");
        System.out.println("          SCHWAB API INTERACTIVE TEST HARNESS");
        System.out.println(
                "============================================================");

        showMainMenu();
    }

    private static void setupConfiguration()
    {
        testConfig = new SchwabTestConfig();

        try
        {
            String appKey = null;
            String appSecret = null;

            // Read from application.yml
            String ymlPath = "src/main/resources/application.yml";
            if (Files.exists(Paths.get(ymlPath)))
            {
                String content = Files.readString(Paths.get(ymlPath));
                appKey = extractYmlValue(content, "appKey");
                appSecret = extractYmlValue(content, "appSecret");
            }
            else
            {
                // Try from classpath
                try (InputStream is = TestHarnessRunner.class.getClassLoader().
                        getResourceAsStream("application.yml"))
                {
                    if (is != null)
                    {
                        String content = new String(is.readAllBytes());
                        appKey = extractYmlValue(content, "appKey");
                        appSecret = extractYmlValue(content, "appSecret");
                    }
                }
            }

            if (appKey != null && appSecret != null)
            {
                testConfig.setAppKey(appKey);
                testConfig.setAppSecret(appSecret);
                System.out.println("Configuration loaded successfully");
            }
            else
            {
                System.out.println(
                        "WARNING: Could not load appKey/appSecret from application.yml");
            }

        }
        catch (IOException e)
        {
            System.err.println("Error loading configuration: " + e.getMessage());
        }
    }

    private static String extractYmlValue(String yamlContent, String key)
    {
        try
        {
            String[] lines = yamlContent.split("\n");
            for (String line : lines)
            {
                String trimmed = line.trim();
                if (trimmed.startsWith(key + ":"))
                {
                    String value = trimmed.substring(key.length() + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\""))
                    {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            }
        }
        catch (Exception e)
        {
            // Ignore parsing errors
        }
        return null;
    }

    private static void showMainMenu()
    {
        while (true)
        {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("MAIN MENU - Select an option:");
            System.out.println("=".repeat(60));
            System.out.println("1. Manual OAuth Authorization");
            System.out.println("2. Check Token Status");
            System.out.println("3. Test Market Data API");
            System.out.println("4. Token Management");
            System.out.println("5. Configuration Status");
            System.out.println("6. Exit");
            System.out.println("=".repeat(60));
            System.out.print("Enter your choice (1-6): ");

            String choice = scanner.nextLine().trim();

            switch (choice)
            {
                case "1" ->
                    performManualOAuth();
                case "2" ->
                    checkTokenStatus();
                case "3" ->
                    testMarketDataAPI();
                case "4" ->
                    tokenManagement();
                case "5" ->
                    showConfigurationStatus();
                case "6" ->
                {
                    System.out.println("Exiting test harness. Goodbye!");
                    System.exit(0);
                }
                default ->
                    System.out.println("Invalid choice. Please enter 1-6.");
            }
        }
    }

    private static void performManualOAuth()
    {
        System.out.println("\n--- Manual OAuth Authorization ---");

        // Validate configuration
        if (testConfig == null || !testConfig.isValid())
        {
            System.out.println(
                    "ERROR: Invalid configuration - missing appKey or appSecret");
            System.out.println("Please check your application.yml file");
            return;
        }

        System.out.println("App Key: " + maskValue(testConfig.getAppKey()));
        System.out.println(
                "Redirect URI: https://127.0.0.1:8182 (must be configured in Schwab Developer Portal)");
        System.out.println(
                "\nThis process requires manual URL copying due to HTTPS certificate requirements.");

        System.out.print("\nProceed with manual OAuth authorization? (y/n): ");
        if (!scanner.nextLine().trim().toLowerCase().startsWith("y"))
        {
            return;
        }

        try (SchwabOAuthClient client = new SchwabOAuthClient())
        {

            // Build authorization URL
            String authUrl = client.
                    buildAuthorizationUrl(testConfig.getAppKey(), null);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("STEP 1: Browser Authorization");
            System.out.println("=".repeat(60));
            System.out.println("Opening Schwab authorization page...");

            // Open browser
            if (Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().browse(URI.create(authUrl));
            }
            else
            {
                System.out.println("Could not open browser automatically.");
                System.out.println("Please manually open this URL:");
                System.out.println(authUrl);
            }

            System.out.println("\nInstructions:");
            System.out.println("1. Log into your Schwab account in the browser");
            System.out.println("2. Review and approve the API permissions");
            System.out.println(
                    "3. Schwab will redirect to: https://127.0.0.1:8182/?code=...");
            System.out.println(
                    "4. Browser will show 'connection refused' - this is expected!");
            System.out.println(
                    "5. Copy the COMPLETE URL from your browser's address bar");

            System.out.println("\n" + "=".repeat(60));
            System.out.println("STEP 2: Paste Redirect URL");
            System.out.println("=".repeat(60));
            System.out.print("Paste the full redirect URL here: ");

            String redirectUrl = scanner.nextLine().trim();

            if (redirectUrl.isEmpty())
            {
                System.out.println("ERROR: No URL provided");
                return;
            }

            if (!redirectUrl.contains("code="))
            {
                System.out.println(
                        "ERROR: URL does not contain authorization code");
                System.out.println("Make sure you copied the complete URL");
                return;
            }

            if (redirectUrl.contains("error="))
            {
                System.out.println(
                        "ERROR: Authorization failed - URL contains error");
                return;
            }

            // Extract authorization code
            String authCode = client.extractAuthorizationCode(redirectUrl);
            System.out.println("Authorization code extracted successfully!");

            // Exchange for tokens
            System.out.println("Exchanging authorization code for tokens...");
            TokenResponse tokens = client.getTokens(
                    testConfig.getAppKey(),
                    testConfig.getAppSecret(),
                    authCode,
                    null
            );

            // Save tokens
            TokenManager.saveTokens(tokens);

            System.out.println("\nSUCCESS! Tokens acquired and saved.");
            System.out.println("You can now use the API endpoints (option 3).");

        }
        catch (Exception e)
        {
            System.err.println("OAuth authorization failed: " + e.getMessage());
            if (e.getMessage().contains("expired"))
            {
                System.out.println(
                        "The authorization code expired. Please try again faster.");
            }
        }

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void checkTokenStatus()
    {
        System.out.println("\n--- Token Status ---");

        try
        {
            TokenManager tokenManager = new TokenManager();
            tokenManager.showTokenStatus();
        }
        catch (Exception e)
        {
            System.err.println("Error checking token status: " + e.getMessage());
        }

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void testMarketDataAPI()
    {
        System.out.println("\n--- Market Data API Test ---");

        if (!TokenManager.hasValidTokens())
        {
            System.out.println(
                    "No valid tokens found. Please complete OAuth authorization first (option 1).");
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
            return;
        }

        System.out.println(
                "Enter stock symbols to get quotes (comma separated, e.g., AAPL,MSFT,GOOGL):");
        System.out.print("Symbols: ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty())
        {
            input = "AAPL"; // Default
        }

        String[] symbols = input.split(",");
        for (int i = 0; i < symbols.length; i++)
        {
            symbols[i] = symbols[i].trim().toUpperCase();
        }

        try (SchwabOAuthClient client = new SchwabOAuthClient())
        {
            String accessToken = TokenManager.getValidAccessToken();

            System.out.println("\nRetrieving quotes...");
            var response = client.getQuotes(symbols, accessToken);

            System.out.println("\nResponse Status: " + response.getStatusCode());
            System.out.println("Response Body:");
            System.out.println(response.getBody());

        }
        catch (Exception e)
        {
            System.err.println("API test failed: " + e.getMessage());
        }

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void tokenManagement()
    {
        System.out.println("\n--- Token Management ---");
        System.out.println("1. Refresh tokens");
        System.out.println("2. Clear all tokens");
        System.out.println("3. Show token files location");
        System.out.println("4. Back to main menu");

        System.out.print("Enter your choice (1-4): ");
        String choice = scanner.nextLine().trim();

        switch (choice)
        {
            case "1" ->
            {
                try
                {
                    System.out.println("Refreshing tokens...");
                    // Create TokenManager with credentials for refresh
                    TokenManager tokenManager = new TokenManager(
                            "schwab-api.json", "schwab-refresh-token.txt",
                            testConfig.getAppKey(), testConfig.getAppSecret());
                    TokenResponse refreshed = tokenManager.
                            forceTokenRefreshInstance();
                    System.out.println("Tokens refreshed successfully!");
                    System.out.println("New token status: " + refreshed.
                            getQuickStatus());
                }
                catch (Exception e)
                {
                    System.err.
                            println("Token refresh failed: " + e.getMessage());
                }
            }
            case "2" ->
            {
                System.out.print(
                        "Are you sure you want to clear all tokens? (y/n): ");
                if (scanner.nextLine().trim().toLowerCase().startsWith("y"))
                {
                    TokenManager tokenManager = new TokenManager();
                    tokenManager.clearTokenFiles();
                }
            }
            case "3" ->
                TokenManager.showTokenFilePaths();
            case "4" ->
            {
                return;
            }
            default ->
                System.out.println("Invalid choice.");
        }

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static void showConfigurationStatus()
    {
        System.out.println("\n--- Configuration Status ---");
        if (testConfig != null)
        {
            testConfig.showConfig();

            try
            {
                testConfig.validateConfig();
                System.out.println("Configuration: VALID");
            }
            catch (IllegalStateException e)
            {
                System.out.println("Configuration: INVALID - " + e.getMessage());
            }
        }
        else
        {
            System.out.println("Configuration not loaded");
        }

        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private static String maskValue(String value)
    {
        if (value == null || value.length() <= 8)
        {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(
                value.length() - 4);
    }
}
