package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.exception.*;
import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Simple token health checker for cron jobs - now Spring-aware
 */
@SpringBootApplication(scanBasePackages = "com.higgstx.schwabtest")
public class SimpleTokenCheckerMain {
    
    public static void main(String[] args) 
    throws SchwabApiException {
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.main.log-startup-info", "false");
        
        // Handle command line arguments
        if (args.length > 0 && "--check-health".equals(args[0])) {
            checkTokenHealthOnlyStandalone();
            return;
        }
        
        // Default: run the data collection simulation with Spring context
        ConfigurableApplicationContext context = SpringApplication.run(SimpleTokenCheckerMain.class, args);
        
        try {
            SchwabTestConfig config = context.getBean(SchwabTestConfig.class);
            TokenManager tokenManager = new TokenManager(
                config.getTokenPropertiesFile(),
                config.getRefreshTokenFile(),
                config.getAppKey(),
                config.getAppSecret()
            );
            
            System.out.println("Starting automated token validation");
            
            String accessToken = getValidAccessTokenForCron(tokenManager);
            
            if (accessToken != null) {
                System.out.println("Access token is valid - ready for API calls");
                simulateDataCollection();
                System.out.println("Simulated data collection completed");
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        } finally {
            context.close();
        }
        
        System.exit(0);
    }
    
    /**
     * Check token health without Spring context (for lightweight health checks)
     */
    public static void checkTokenHealthOnlyStandalone()
    throws SchwabApiException{
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
    public static String getValidAccessTokenForCron(TokenManager tokenManager) throws Exception {
        // Load tokens with auto-refresh enabled
        TokenResponse tokens = tokenManager.loadTokensInstance(true);
        
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