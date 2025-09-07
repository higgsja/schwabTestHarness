package com.higgstx.schwabtest.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Test harness application configuration - Spring Boot managed
 * Uses @ConfigurationProperties for automatic binding from application.yml
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "schwab.api")
public class SchwabTestConfig {
    
    private String appKey;
    private String appSecret;
    private String tokenPropertiesFile = "schwab-api.json";
    private String refreshTokenFile = "schwab-refresh-token.txt";
    
    // Nested configuration classes
    private Urls urls = new Urls();
    private Defaults defaults = new Defaults();
    
    @PostConstruct
    public void validateOnStartup() {
        if (!isValid()) {
            throw new IllegalStateException("Schwab API configuration incomplete. " +
                    "Please ensure 'schwab.api.appKey' and 'schwab.api.appSecret' are " +
                    "set in your application.yml file.");
        }
        log.info("Schwab test harness configuration loaded successfully");
    }
    
    public boolean isValid() {
        return appKey != null && !appKey.trim().isEmpty() &&
               appSecret != null && !appSecret.trim().isEmpty();
    }
    
    public void showConfig() {
        log.info("=== Schwab Test Configuration ===");
        log.info("App Key: {}", maskValue(appKey));
        log.info("App Secret: {}", maskValue(appSecret));
        log.info("Token Properties File: {}", tokenPropertiesFile);
        log.info("Refresh Token File: {}", refreshTokenFile);
        log.info("Redirect URI: {}", defaults.redirectUri);
        log.info("Auth URL: {}", urls.auth);
        log.info("Token URL: {}", urls.token);
        log.info("Market Data URL: {}", urls.marketData);
        log.info("HTTP Timeout: {}ms", defaults.httpTimeoutMs);
        log.info("Scope: {}", defaults.scope);
    }
    
    private String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    // Convenience method for TestHarnessRunner
    public String getRedirectUri() {
        return defaults.getRedirectUri();
    }
    
    @Data
    public static class Urls {
        private String auth = "https://api.schwabapi.com/v1/oauth/authorize";
        private String token = "https://api.schwabapi.com/v1/oauth/token";
        private String traderBase = "https://api.schwabapi.com/trader/v1";
        private String marketData = "https://api.schwabapi.com/marketdata/v1";
    }
    
    @Data
    public static class Defaults {
        private String redirectUri = "https://127.0.0.1:8182";
        private int httpTimeoutMs = 30000;
        private String scope = "readonly";
    }
}