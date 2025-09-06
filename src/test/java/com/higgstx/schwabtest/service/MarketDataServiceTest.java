package com.higgstx.schwabtest.service;

import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.exception.*;
import com.higgstx.schwabapi.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MarketDataService
 * These tests work without actual API credentials by ensuring no token files exist
 */
class MarketDataServiceTest
{

    private MarketDataService marketDataService;
    private boolean hadTokenFile = false;
    private String originalTokenContent = null;

    @BeforeEach
    void setUp() throws SchwabApiException
    {
        // Backup and remove token file to ensure clean test environment
        backupAndRemoveTokenFile();

        try
        {
            // Create with test properties since application.yml may not be available in test context
            SchwabApiProperties testProps = new SchwabApiProperties(
                    "https://api.schwabapi.com/v1/oauth/authorize",
                    "https://api.schwabapi.com/v1/oauth/token",
                    "https://api.schwabapi.com/marketdata/v1",
                    "https://127.0.0.1:8182",
                    "readonly",
                    30000
            );
            marketDataService = new MarketDataService(testProps);
        }
        catch (Exception e)
        {
            // Fallback to default constructor if properties fail
            try
            {
                marketDataService = new MarketDataService();
            }
            catch (Exception ex)
            {
                // If both fail, create with minimal test properties
                SchwabApiProperties fallbackProps = new SchwabApiProperties(
                        "https://test.api.com/auth",
                        "https://test.api.com/token",
                        "https://test.api.com/market",
                        "http://localhost:8080",
                        "readonly",
                        5000
                );
                marketDataService = new MarketDataService(fallbackProps);
            }
        }
    }

    @AfterEach
    void tearDown() throws Exception
    {
        if (marketDataService != null)
        {
            marketDataService.close();
        }

        // Restore token file if it existed
        restoreTokenFile();
    }

    private void backupAndRemoveTokenFile()
    {
        try
        {
            String tokenFile = "schwab-api.json";
            if (Files.exists(Paths.get(tokenFile)))
            {
                hadTokenFile = true;
                originalTokenContent = Files.readString(Paths.get(tokenFile));
                Files.delete(Paths.get(tokenFile));

                // Also clear any refresh token file
                String refreshTokenFile = "schwab-refresh-token.txt";
                if (Files.exists(Paths.get(refreshTokenFile)))
                {
                    Files.delete(Paths.get(refreshTokenFile));
                }
            }
        }
        catch (Exception e)
        {
            // Ignore errors during cleanup
        }
    }

    private void restoreTokenFile()
    {
        try
        {
            if (hadTokenFile && originalTokenContent != null)
            {
                Files.writeString(Paths.get("schwab-api.json"),
                        originalTokenContent);
            }
        }
        catch (Exception e)
        {
            // Ignore errors during restoration
        }
    }

    @Nested
    @DisplayName("Quote Operations")
    class QuoteOperationsTest
    {

        @Test
        @DisplayName(
                "Should handle quote request and return error due to no tokens")
        void testGetQuote_NoTokens_ReturnsError()
        {
            // Given
            String symbol = "AAPL";

            // When & Then - Should throw Exception due to no valid tokens
            assertThrows(Exception.class, () ->
            {
                marketDataService.getQuote(symbol);
            });
        }

        @Test
        @DisplayName("Should handle multiple quotes request")
        void testGetQuotes_NoTokens_ThrowsException()
        {
            // Given
            List<String> symbols = Arrays.asList("AAPL", "GOOGL", "MSFT");

            // When & Then - Should throw Exception due to no valid tokens
            assertThrows(Exception.class, () ->
            {
                marketDataService.getQuotes(symbols);
            });
        }

        @Test
        @DisplayName("Should handle empty symbol list")
        void testGetQuotes_EmptyList_ThrowsException()
        {
            // Given
            List<String> symbols = Arrays.asList();

            // When & Then - Should throw Exception due to no valid tokens or empty list
            assertThrows(Exception.class, () ->
            {
                marketDataService.getQuotes(symbols);
            });
        }

        @Test
        @DisplayName("Should handle null symbol gracefully")
        void testGetQuote_NullSymbol_ThrowsException()
        {
            // When & Then
            assertThrows(Exception.class, () ->
            {
                marketDataService.getQuote(null);
            });
        }
    }

    @Nested
    @DisplayName("Price History Operations")
    class PriceHistoryOperationsTest
    {

        @Test
        @DisplayName("Should handle price history request")
        void testGetPriceHistory_NoTokens_ThrowsException()
        {
            // Given
            String symbol = "AAPL";
            String periodType = "month";
            int period = 1;
            String frequencyType = "daily";
            int frequency = 1;

            // When & Then - Should throw Exception due to no valid tokens
            assertThrows(Exception.class, () ->
            {
                marketDataService.getPriceHistory(symbol, periodType, period,
                        frequencyType,  frequency);
            });
        }
    }

    @Nested
    @DisplayName("Market Hours Operations")
    class MarketHoursOperationsTest
    {

        @Test
        @DisplayName("Should handle market hours request")
        void testGetMarketHours_NoTokens_ThrowsException()
        {
            // Given
            String marketType = "equity";

            // When & Then - Should throw Exception due to no valid tokens
            assertThrows(Exception.class, () ->
            {
                marketDataService.getMarketHours(marketType);
            });
        }
    }

    @Nested
    @DisplayName("Service Status Operations")
    class ServiceStatusOperationsTest
    {

        @Test
        @DisplayName("Should check if service is ready")
        void testIsReady_WithoutValidTokens()
        {
            // When
            boolean isReady = marketDataService.isReady();

            // Then - Should not be ready without valid tokens
            assertFalse(isReady,
                    "Service should not be ready without valid tokens");
        }

        @Test
        @DisplayName("Should get token status")
        void testGetTokenStatus_WithoutTokens()
        {
            // When
            String status = marketDataService.getTokenStatus();

            // Then
            assertNotNull(status);
            assertTrue(status.contains("NO TOKENS")
                    || status.contains("ERROR")
                    || status.contains("EXPIRED")
                    || status.contains("Token refresh failed")
                    || status.contains("Unable to get a valid access token"),
                    "Status should indicate no tokens available, but was: " + status);
        }

        @Test
        @DisplayName(
                "Should handle service construction with explicit properties")
        void testConstructor_WithExplicitProperties()
        {
            // Given
            SchwabApiProperties testProps = new SchwabApiProperties(
                    "https://api.schwabapi.com/v1/oauth/authorize",
                    "https://api.schwabapi.com/v1/oauth/token",
                    "https://api.schwabapi.com/marketdata/v1",
                    "https://127.0.0.1:8182",
                    "readonly",
                    30000
            );

            // When & Then - Should not throw
            assertDoesNotThrow(() ->
            {
                try (MarketDataService service = new MarketDataService(testProps))
                {
                    assertNotNull(service);
                    assertFalse(service.isReady(),
                            "Service should not be ready without valid tokens");
                }
            });
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodsTest
    {

        @Test
        @DisplayName("Should handle service close gracefully")
        void testClose_GracefulShutdown()
        {
            // When & Then
            assertDoesNotThrow(() ->
            {
                SchwabApiProperties testProps = new SchwabApiProperties(
                        "https://api.schwabapi.com/v1/oauth/authorize",
                        "https://api.schwabapi.com/v1/oauth/token",
                        "https://api.schwabapi.com/marketdata/v1",
                        "https://127.0.0.1:8182",
                        "readonly",
                        30000
                );
                MarketDataService service = new MarketDataService(testProps);
                service.close();
            });
        }

        @Test
        @DisplayName("Should handle multiple close calls")
        void testClose_MultipleCalls()
        {
            // When & Then
            assertDoesNotThrow(() ->
            {
                SchwabApiProperties testProps = new SchwabApiProperties(
                        "https://api.schwabapi.com/v1/oauth/authorize",
                        "https://api.schwabapi.com/v1/oauth/token",
                        "https://api.schwabapi.com/marketdata/v1",
                        "https://127.0.0.1:8182",
                        "readonly",
                        30000
                );
                MarketDataService service = new MarketDataService(testProps);
                service.close();
                service.close(); // Should not throw exception on second close
            });
        }
    }
}
