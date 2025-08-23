package com.higgstx.schwab.automation;

import com.higgstx.schwab.client.SchwabOAuthClient;
import com.higgstx.schwab.model.ApiResponse;
import com.higgstx.schwab.model.TokenResponse;
import com.higgstx.schwab.service.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Cron-job compatible Schwab API client that handles tokens automatically
 * without any user interaction required.
 */
public class CronCompatibleSchwabClient {
    
    private static final Logger logger = LoggerFactory.getLogger(CronCompatibleSchwabClient.class);
    // Configuration

    private int maxRetryAttempts = 2;
    
    /**
     * Main method for cron job execution
     */
    public static void main(String[] args) {
        CronCompatibleSchwabClient client = new CronCompatibleSchwabClient();
        
        try {
            // Configure for strict cron execution (fail fast)
            client.setFailOnTokenIssues(true);
            
            // Example: Get market data
            client.executeMarketDataCollection();
            
            System.exit(0); // Success
            
        } catch (Exception e) {
            logger.error("Cron job failed: {}", e.getMessage(), e);
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1); // Failure
        }
    }
    
    /**
     * Executes market data collection suitable for cron jobs
     */
    public void executeMarketDataCollection() throws Exception {
        logger.info("🔄 Starting automated market data collection");
        
        // Step 1: Ensure we have valid tokens
        String accessToken = getValidAccessTokenForCron();
        
        // Step 2: Collect market data
        try (SchwabOAuthClient client = new SchwabOAuthClient()) {
            
            // Example data collection - customize as needed
            collectQuoteData(client, accessToken);
            collectMarketHours(client, accessToken);
            
            logger.info("✅ Market data collection completed successfully");
        }
    }
    
    /**
     * Gets a valid access token for cron job execution
     * Throws exception if tokens cannot be obtained without user interaction
     */
    public String getValidAccessTokenForCron() throws Exception {
        logger.debug("Checking token status for cron execution");
        
        // Load tokens with auto-refresh enabled
        TokenResponse tokens = TokenManager.loadTokens(true);
        
        if (tokens == null) {
            throw new CronTokenException("No tokens found - manual authorization required");
        }
        
        // Check access token validity
        if (tokens.isAccessTokenValid()) {
            logger.info("✅ Access token is valid");
            return tokens.getAccessToken();
        }
        
        // Check if we can refresh
        if (!tokens.isRefreshTokenValid()) {
            throw new CronTokenException("Refresh token expired - manual re-authorization required");
        }
        
        // Attempt automatic refresh
        logger.info("🔄 Access token expired, attempting automatic refresh");
        
        try (SchwabOAuthClient client = new SchwabOAuthClient()) {
TokenResponse refreshed = client.refreshTokens(tokens.getRefreshToken());
            
            if (refreshed != null && refreshed.isAccessTokenValid()) {
                // Preserve refresh token if not provided
                if (refreshed.getRefreshToken() == null) {
                    refreshed.setRefreshToken(tokens.getRefreshToken());
                    refreshed.setRefreshTokenExpiresIn(tokens.getRefreshTokenExpiresIn());
                    refreshed.setRefreshTokenExpiresAt(tokens.getRefreshTokenExpiresAt());
                }
                
                TokenManager.saveTokens(refreshed);
                logger.info("✅ Access token refreshed successfully for cron execution");
                return refreshed.getAccessToken();
            } else {
                throw new CronTokenException("Token refresh failed - manual intervention required");
            }
        }
    }
    
    /**
     * Collects quote data with retry logic
     */
    private void collectQuoteData(SchwabOAuthClient client, String accessToken) throws Exception {
        logger.info("📈 Collecting quote data");
        
        String[] symbols = {"AAPL", "MSFT", "GOOGL", "TSLA", "NVDA"}; // Customize as needed
        
        for (String symbol : symbols) {
ApiResponse response = callWithRetry(() -> client.getQuotes(new String[]{symbol}, accessToken));
            
            if (response.getStatusCode() == 200) {
                logger.info("✅ Successfully collected quote for {}", symbol);
                // Process the quote data here
                processQuoteData(symbol, response.getBody());
            } else {
                logger.warn("⚠️ Failed to get quote for {}: HTTP {}", symbol, response.getStatusCode());
            }
            
            // Small delay to avoid rate limiting
            Thread.sleep(100);
        }
    }
    
    /**
     * Collects market hours data
     */
    private void collectMarketHours(SchwabOAuthClient client, String accessToken) throws Exception {
        logger.info("🕐 Collecting market hours");
        
        String[] marketTypes = {"EQUITY", "OPTION"};
        
        for (String marketType : marketTypes) {
            ApiResponse response = callWithRetry(() -> client.getMarketHours(marketType, accessToken));
            
            if (response.getStatusCode() == 200) {
                logger.info("✅ Successfully collected market hours for {}", marketType);
                processMarketHours(marketType, response.getBody());
            } else {
                logger.warn("⚠️ Failed to get market hours for {}: HTTP {}", marketType, response.getStatusCode());
            }
        }
    }
    
    /**
     * Calls API with retry logic for transient failures
     */
    private ApiResponse callWithRetry(ApiCall apiCall) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                return apiCall.execute();
            } catch (Exception e) {
                lastException = e;
                logger.warn("API call attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < maxRetryAttempts) {
                    // Exponential backoff
                    long delay = 1000L * attempt * attempt; // 1s, 4s, 9s...
                    logger.info("Retrying in {}ms...", delay);
                    Thread.sleep(delay);
                }
            }
        }
        
        throw new Exception("API call failed after " + maxRetryAttempts + " attempts", lastException);
    }
    
    /**
     * Process quote data - customize based on your needs
     */
    private void processQuoteData(String symbol, String jsonData) {
        // Example: Save to database, write to file, send to analytics system, etc.
        logger.debug("Processing quote data for {}: {} characters", symbol, jsonData.length());
        
        // TODO: Implement your data processing logic here
        // Examples:
        // - Parse JSON and save to database
        // - Write to CSV file
        // - Send to message queue
        // - Calculate metrics
    }
    
    /**
     * Process market hours data - customize based on your needs
     */
    private void processMarketHours(String marketType, String jsonData) {
        logger.debug("Processing market hours for {}: {} characters", marketType, jsonData.length());
        
        // TODO: Implement your market hours processing logic here
    }
    
    /**
     * Checks if tokens will expire soon and logs warnings
     */
    public void checkTokenHealth() {
        TokenResponse tokens = TokenManager.loadTokens(false);
        
        if (tokens == null) {
            logger.error("❌ No tokens available");
            return;
        }
        
        // Check access token
        if (tokens.getExpiresAt() != null) {
            long hoursUntilExpiry = ChronoUnit.HOURS.between(Instant.now(), tokens.getExpiresAt());
            
            if (hoursUntilExpiry <= 0) {
                logger.warn("⚠️ Access token expired");
            } else if (hoursUntilExpiry <= 1) {
                logger.warn("⚠️ Access token expires in {} hours", hoursUntilExpiry);
            }
        }
        
        // Check refresh token
        if (tokens.getRefreshTokenExpiresAt() != null) {
            long daysUntilRefreshExpiry = ChronoUnit.DAYS.between(Instant.now(), tokens.getRefreshTokenExpiresAt());
            
            if (daysUntilRefreshExpiry <= 0) {
                logger.error("❌ Refresh token expired - manual re-authorization required");
            } else if (daysUntilRefreshExpiry <= 2) {
                logger.warn("⚠️ Refresh token expires in {} days - consider re-authorizing soon", daysUntilRefreshExpiry);
            }
        }
    }
    
    // Configuration methods
    
    public void setFailOnTokenIssues(boolean failOnTokenIssues) {
    }
    
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = Math.max(1, Math.min(maxRetryAttempts, 5));
    }
    
    // Helper interfaces
    
    @FunctionalInterface
    private interface ApiCall {
        ApiResponse execute() throws Exception;
    }
    
    /**
     * Custom exception for cron job token issues
     */
    public static class CronTokenException extends Exception {
        public CronTokenException(String message) {
            super(message);
        }
        
        public CronTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}