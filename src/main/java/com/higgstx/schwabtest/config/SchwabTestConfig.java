package com.higgstx.schwabtest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Configuration properties for the Schwab API.
 * This class is managed by Spring Boot's dependency injection container
 * to automatically bind properties from application.yml.
 */
@Component
@ConfigurationProperties(prefix = "schwab.api")
public class SchwabTestConfig {
    
    private String appKey;
    private String appSecret;
    private String tokenPropertiesFile = "schwab-api.json";
    private String refreshTokenFile = "schwab-refresh-token.txt";
    
    // Add nested configuration classes to match application.yml structure
    private Urls urls = new Urls();
    private Defaults defaults = new Defaults();
    
    public boolean isValid() {
        return appKey != null && !appKey.trim().isEmpty() &&
               appSecret != null && !appSecret.trim().isEmpty();
    }
    
    @PostConstruct
    public void validateOnStartup() {
        validateConfig();
    }
    
    public void validateConfig() {
        if (!isValid()) {
            throw new IllegalStateException("Schwab API configuration incomplete. " +
                    "Please ensure 'schwab.api.appKey' and 'schwab.api.appSecret' are " +
                    "set in your application.yml file.");
        }
    }
    
    public void showConfig() {
        System.out.println("=== Schwab Test Configuration ===");
        System.out.println("App Key: " + maskValue(appKey));
        System.out.println("App Secret: " + maskValue(appSecret));
        System.out.println("Token Properties File: " + tokenPropertiesFile);
        System.out.println("Refresh Token File: " + refreshTokenFile);
        System.out.println("Redirect URI: " + (defaults != null ? defaults.redirectUri : "NULL"));
        System.out.println("Auth URL: " + (urls != null ? urls.auth : "NULL"));
        System.out.println("Token URL: " + (urls != null ? urls.token : "NULL"));
        System.out.println("Market Data URL: " + (urls != null ? urls.marketData : "NULL"));
        System.out.println("HTTP Timeout: " + (defaults != null ? defaults.httpTimeoutMs : "NULL") + "ms");
        System.out.println("Scope: " + (defaults != null ? defaults.scope : "NULL"));
    }
    
    private String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    // Convenience method for TestHarnessRunner
    public String getRedirectUri() {
        return defaults != null ? defaults.getRedirectUri() : "https://127.0.0.1:8182";
    }

    // Existing getters and setters
    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getTokenPropertiesFile() {
        return tokenPropertiesFile;
    }

    public String getRefreshTokenFile() {
        return refreshTokenFile;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public void setTokenPropertiesFile(String tokenPropertiesFile) {
        this.tokenPropertiesFile = tokenPropertiesFile;
    }

    public void setRefreshTokenFile(String refreshTokenFile) {
        this.refreshTokenFile = refreshTokenFile;
    }
    
    // New getters and setters for nested properties
    public Urls getUrls() {
        return urls != null ? urls : new Urls();
    }
    
    public void setUrls(Urls urls) {
        this.urls = urls;
    }
    
    public Defaults getDefaults() {
        return defaults != null ? defaults : new Defaults();
    }
    
    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }
    
    // Nested configuration classes
    public static class Urls {
        private String auth = "https://api.schwabapi.com/v1/oauth/authorize";
        private String token = "https://api.schwabapi.com/v1/oauth/token";
        private String traderBase = "https://api.schwabapi.com/trader/v1";
        private String marketData = "https://api.schwabapi.com/marketdata/v1";
        
        public String getAuth() {
            return auth;
        }
        
        public void setAuth(String auth) {
            this.auth = auth;
        }
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public String getTraderBase() {
            return traderBase;
        }
        
        public void setTraderBase(String traderBase) {
            this.traderBase = traderBase;
        }
        
        public String getMarketData() {
            return marketData;
        }
        
        public void setMarketData(String marketData) {
            this.marketData = marketData;
        }
    }
    
    public static class Defaults {
        private String redirectUri = "https://127.0.0.1:8182";
        private int httpTimeoutMs = 30000;
        private String scope = "readonly";
        
        public String getRedirectUri() {
            return redirectUri;
        }
        
        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
        
        public int getHttpTimeoutMs() {
            return httpTimeoutMs;
        }
        
        public void setHttpTimeoutMs(int httpTimeoutMs) {
            this.httpTimeoutMs = httpTimeoutMs;
        }
        
        public String getScope() {
            return scope;
        }
        
        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}