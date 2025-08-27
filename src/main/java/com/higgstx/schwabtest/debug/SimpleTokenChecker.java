package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Simple token health checker for cron jobs
 */
public class SimpleTokenChecker {
    
    public static void main(String[] args) {
        // Handle command line arguments
        if (args.length > 0 && "--check-health".equals(args[0])) {
            checkTokenHealthOnly();
            return;
        }
        
        // Default: run the data collection simulation
        System.out.println("Starting automated token validation");
        
        try {
            String accessToken = getValidAccessTokenForCron();
            
            if (accessToken != null) {
                System.out.println("Access token is valid - ready for API calls");
                // Here you would do your actual data collection
                simulateDataCollection();
                System.out.println("Simulated data collection completed");
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Check token health and print status
     */
    public static void checkTokenHealthOnly() {
        TokenResponse tokens = TokenManager.loadTokens(false);
        
        if (tokens == null) {
            System.out.println("No tokens available - authorization required");
            return;
        }
        
        // Check access token
        if (tokens.getExpiresAt() != null) {
            long hoursUntilExpiry = ChronoUnit.HOURS.between(Instant.now(), tokens.getExpiresAt());
            
            if (hoursUntilExpiry <= 0) {
                System.out.println("Access token expired");
            } else if (hoursUntilExpiry <= 1) {
                System.out.println("Access token expires in " + hoursUntilExpiry + " hours");
            } else {
                System.out.println("Access token valid for " + hoursUntilExpiry + " hours");
            }
        }
        
        // Check refresh token
        if (tokens.getRefreshTokenExpiresAt() != null) {
            long daysUntilRefreshExpiry = ChronoUnit.DAYS.between(Instant.now(), tokens.getRefreshTokenExpiresAt());
            
            if (daysUntilRefreshExpiry <= 0) {
                System.out.println("Refresh token expired - manual re-authorization required");
            } else if (daysUntilRefreshExpiry <= 2) {
                System.out.println("Refresh token expires in " + daysUntilRefreshExpiry + " days - consider re-authorizing soon");
            } else {
                System.out.println("Refresh token valid for " + daysUntilRefreshExpiry + " days");
            }
        } else {
            System.out.println("Refresh token expiration unknown");
        }
    }
    
    /**
     * Gets a valid access token for cron job execution
     */
    public static String getValidAccessTokenForCron() throws Exception {
        // Load tokens with auto-refresh enabled
        TokenResponse tokens = TokenManager.loadTokens(true);
        
        if (tokens == null) {
            throw new Exception("No tokens found - manual authorization required");
        }
        
        // Check access token validity
        if (tokens.isAccessTokenValid()) {
            System.out.println("Access token is valid");
            return tokens.getAccessToken();
        }
        
        // Check if we can refresh
        if (!tokens.isRefreshTokenValid()) {
            throw new Exception("Refresh token expired - manual re-authorization required");
        }
        
        // If we get here, auto-refresh should have happened but failed
        throw new Exception("Token refresh failed - manual intervention required");
    }
    
    /**
     * Simulate data collection for testing
     */
    private static void simulateDataCollection() {
        System.out.println("Simulating quote data collection...");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        System.out.println("Simulating market hours collection...");
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        
        System.out.println("Simulating data processing...");
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }
}