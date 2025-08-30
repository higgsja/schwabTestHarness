// src/main/java/com/higgstx/schwabtest/debug/TestHarnessRunner.java
package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.config.SchwabOAuthClient;
import com.higgstx.schwabapi.exception.*;
import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import com.higgstx.schwabtest.util.LoggingUtil;
import com.higgstx.schwabtest.ui.UserInterface; // Import the new interface
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.awt.Desktop;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Interactive test harness for Schwab API operations with manual-only OAuth
 */
@SpringBootApplication
@ComponentScan(basePackages =
{
    "com.higgstx.schwabtest", "com.higgstx.schwabapi"
})
public class TestHarnessRunner implements CommandLineRunner
{

    // Removed the static Scanner, as it's now handled by ConsoleIOUserInterface
    private final SchwabTestConfig testConfig;
    private final UserInterface ui; // Inject the user interface

    @Autowired
    public TestHarnessRunner(SchwabTestConfig testConfig, UserInterface ui)
    {
        this.testConfig = testConfig;
        this.ui = ui; // Initialize the user interface
    }

    public static void main(String[] args)
    {
        LoggingUtil.initializeLogging(); // Assuming this sets up Logback
        SpringApplication.run(TestHarnessRunner.class, args);
    }

    @Override
    public void run(String... args) throws Exception
    {
        ui.displayHeader("SCHWAB API INTERACTIVE TEST HARNESS");

        try
        {
            testConfig.validateConfig();
        }
        catch (IllegalStateException e)
        {
            ui.
                    displayError("Configuration validation failed: " + e.
                            getMessage());
            ui.displayMessage(
                    "Please provide 'schwab.api.appKey' and 'schwab.api.appSecret' in your application.yml file.");
            ui.exit(1);
        }

        showMainMenu();
    }

    private void showMainMenu()
    {
        while (true)
        {
            ui.displaySeparator(60);
            ui.displayMessage("MAIN MENU - Select an option:");
            ui.displaySeparator(60);
            ui.displayMessage("1. Manual OAuth Authorization");
            ui.displayMessage("2. Check Token Status");
            ui.displayMessage("3. Test Market Data API");
            ui.displayMessage("4. Token Management");
            ui.displayMessage("5. Configuration Status");
            ui.displayMessage("6. Exit");
            ui.displaySeparator(60);
            String choice = ui.getUserInput("Enter your choice (1-6): ");

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
                    ui.displayMessage("Exiting test harness. Goodbye!");
                    ui.exit(0);
                }
                default ->
                    ui.displayMessage("Invalid choice. Please enter 1-6.");
            }
        }
    }

    private void performManualOAuth()
    {
        ui.displaySubHeader("Manual OAuth Authorization");

        ui.displayMessage("App Key: " + maskValue(testConfig.getAppKey()));
        ui.displayMessage(
                "Redirect URI: https://127.0.0.1:8182 (must be configured in Schwab Developer Portal)");
        ui.displayMessage(
                "\nThis process requires manual URL copying due to HTTPS certificate requirements.");

        String proceed = ui.getUserInput(
                "\nProceed with manual OAuth authorization? (y/n): ");
        if (!proceed.toLowerCase().startsWith("y"))
        {
            return;
        }

        try (SchwabOAuthClient client = new SchwabOAuthClient())
        {
            String authUrl = client.
                    buildAuthorizationUrl(testConfig.getAppKey(), null);

            ui.displayHeader("STEP 1: Browser Authorization");
            ui.displayMessage("Opening Schwab authorization page...");

            if (Desktop.isDesktopSupported())
            {
                Desktop.getDesktop().browse(URI.create(authUrl));
            }
            else
            {
                ui.displayMessage("Could not open browser automatically.");
                ui.displayMessage("Please manually open this URL:");
                ui.displayMessage(authUrl);
            }

            ui.displayMessage("\nInstructions:");
            ui.displayMessage("1. Log into your Schwab account in the browser");
            ui.displayMessage("2. Review and approve the API permissions");
            ui.displayMessage(
                    "3. Schwab will redirect to: https://127.0.0.1:8182/?code=...");
            ui.displayMessage(
                    "4. Browser will show 'connection refused' - this is expected!");
            ui.displayMessage(
                    "5. Copy the COMPLETE URL from your browser's address bar");

            ui.displayHeader("STEP 2: Paste Redirect URL");
            String redirectUrl = ui.getUserInput(
                    "Paste the full redirect URL here: ");

            if (redirectUrl.isEmpty())
            {
                ui.displayError("No URL provided");
                return;
            }

            if (!redirectUrl.contains("code="))
            {
                ui.displayError("URL does not contain authorization code");
                ui.displayMessage("Make sure you copied the complete URL");
                return;
            }

            if (redirectUrl.contains("error="))
            {
                ui.displayError("Authorization failed - URL contains error");
                return;
            }

            String authCode = client.extractAuthorizationCode(redirectUrl);
            ui.displayMessage("Authorization code extracted successfully!");

            ui.displayMessage("Exchanging authorization code for tokens...");
            TokenResponse tokens = client.getTokens(
                    testConfig.getAppKey(),
                    testConfig.getAppSecret(),
                    authCode,
                    null
            );

            TokenManager.saveTokens(tokens);

            ui.displayMessage("\nSUCCESS! Tokens acquired and saved.");
            ui.displayMessage("You can now use the API endpoints (option 3).");

        }
        catch (Exception e)
        {
            ui.displayError("OAuth authorization failed: " + e.getMessage());
            if (e.getMessage().contains("expired"))
            {
                ui.displayMessage(
                        "The authorization code expired. Please try again faster.");
            }
        }

        ui.getUserInput("\nPress Enter to continue...");
    }

    private void checkTokenStatus()
    {
        ui.displaySubHeader("Token Status");
        try
        {
            TokenManager tokenManager = new TokenManager();
            tokenManager.showTokenStatus(); // This still uses System.out, needs modification in TokenManager
        }
        catch (Exception e)
        {
            ui.displayError("Error checking token status: " + e.getMessage());
        }
        ui.getUserInput("\nPress Enter to continue...");
    }

    private void testMarketDataAPI()
    {
        ui.displaySubHeader("Market Data API Test");
        if (!TokenManager.hasValidTokens())
        {
            ui.displayMessage(
                    "No valid tokens found. Please complete OAuth authorization first (option 1).");
            ui.getUserInput("Press Enter to continue...");
            return;
        }

        ui.displayMessage(
                "Enter stock symbols to get quotes (comma separated, e.g., AAPL,MSFT,GOOGL):");
        String input = ui.getUserInput("Symbols: ");

        if (input.isEmpty())
        {
            input = "AAPL";
        }

        String[] symbols = Arrays.stream(input.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toList())
                .toArray(new String[0]);

        try (SchwabOAuthClient client = new SchwabOAuthClient())
        {
            String accessToken = TokenManager.getValidAccessToken();
            ui.displayMessage("\nRetrieving quotes...");
            var response = client.getQuotes(symbols, accessToken);

            ui.displayMessage("\nResponse Status: " + response.getStatusCode());
            ui.displayMessage("Response Body:\n" + response.getBody());

        }
        catch (Exception e)
        {
            ui.displayError("API test failed: " + e.getMessage());
        }

        ui.getUserInput("\nPress Enter to continue...");
    }

    private void tokenManagement()
    {
        ui.displaySubHeader("Token Management");
        ui.displayMessage("1. Refresh tokens");
        ui.displayMessage("2. Clear all tokens");
        ui.displayMessage("3. Show token files location");
        ui.displayMessage("4. Back to main menu");

        String choice = ui.getUserInput("Enter your choice (1-4): ");

        switch (choice)
        {
            case "1" ->
            {
                try
                {
                    ui.displayMessage("Refreshing tokens...");
                    TokenManager tokenManager = new TokenManager(
                            "schwab-api.json", "schwab-refresh-token.txt",
                            testConfig.getAppKey(), testConfig.getAppSecret());
                    TokenResponse refreshed = tokenManager.
                            forceTokenRefreshInstance();
                    ui.displayMessage("Tokens refreshed successfully!");
                    ui.displayMessage("New token status: " + refreshed.
                            getQuickStatus());
                }
                catch (SchwabApiException e)
                {
                    ui.displayError("Token refresh failed: " + e.getMessage());
                }
            }
            case "2" ->
            {
                String confirm = ui.getUserInput(
                        "Are you sure you want to clear all tokens? (y/n): ");
                if (confirm.toLowerCase().startsWith("y"))
                {
                    TokenManager tokenManager = new TokenManager();
                    tokenManager.clearTokenFiles(); // This still uses System.out, needs modification
                    ui.displayMessage("Token files cleared.");
                }
                else
                {
                    ui.displayMessage("Token clearing cancelled.");
                }
            }
            case "3" ->
                TokenManager.showTokenFilePaths(); // This still uses System.out, needs modification
            case "4" ->
            {
                return;
            }
            default ->
                ui.displayMessage("Invalid choice.");
        }
        ui.getUserInput("\nPress Enter to continue...");
    }

    private void showConfigurationStatus()
    {
        ui.displaySubHeader("Configuration Status");
        if (testConfig != null)
        {
            testConfig.showConfig(); // This still uses System.out, needs modification in SchwabTestConfig
            try
            {
                testConfig.validateConfig();
                ui.displayMessage("Configuration: VALID");
            }
            catch (IllegalStateException e)
            {
                ui.displayMessage("Configuration: INVALID - " + e.getMessage());
            }
        }
        else
        {
            ui.displayMessage("Configuration not loaded");
        }
        ui.getUserInput("\nPress Enter to continue...");
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
