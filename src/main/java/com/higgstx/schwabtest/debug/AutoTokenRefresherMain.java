package com.higgstx.schwabtest.debug;

import com.higgstx.schwabapi.model.TokenResponse;
import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabtest.config.SchwabTestConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "com.higgstx.schwabtest")
public class AutoTokenRefresherMain {
    
    public static void main(String[] args) {
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.main.log-startup-info", "false");
        
        ConfigurableApplicationContext context = SpringApplication.run(AutoTokenRefresherMain.class, args);
        
        try {
            SchwabTestConfig config = context.getBean(SchwabTestConfig.class);
            refreshTokens(config);
        } catch (Exception e) {
            System.err.println("Error running token refresher: " + e.getMessage());
            System.exit(1);
        } finally {
            context.close();
        }
        
        System.exit(0);
    }
    
    private static void refreshTokens(SchwabTestConfig testConfig) {
        System.out.println("Schwab API Token Refresher is running...");

        try {
            TokenManager tokenManager = new TokenManager(
                    testConfig.getTokenPropertiesFile(), 
                    testConfig.getRefreshTokenFile(),
                    testConfig.getAppKey(), 
                    testConfig.getAppSecret());

            if (!tokenManager.hasRefreshToken()) {
                System.err.println("ERROR: No refresh token found. Please run the Manual OAuth option (1) first to authorize the application.");
                return;
            }

            System.out.println("Attempting to refresh tokens...");
            TokenResponse refreshedTokens = tokenManager.forceTokenRefreshInstance();
            System.out.println("SUCCESS! Tokens refreshed automatically.");
            System.out.println("New access token expires at: " + refreshedTokens.getExpiresAt());

        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE: Token refresh failed.");
            System.err.println("Reason: " + e.getMessage());
            System.err.println("Please re-run the Manual OAuth option (1) to re-authorize and get a new refresh token.");
        }
    }
}