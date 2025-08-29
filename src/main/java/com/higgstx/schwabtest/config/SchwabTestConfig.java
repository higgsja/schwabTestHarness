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
    }
    
    private String maskValue(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    public String getAppKey()
    {
        return appKey;
    }

    public String getAppSecret()
    {
        return appSecret;
    }

    public String getTokenPropertiesFile()
    {
        return tokenPropertiesFile;
    }

    public String getRefreshTokenFile()
    {
        return refreshTokenFile;
    }

    public void setAppKey(String appKey)
    {
        this.appKey = appKey;
    }

    public void setAppSecret(String appSecret)
    {
        this.appSecret = appSecret;
    }

    public void setTokenPropertiesFile(String tokenPropertiesFile)
    {
        this.tokenPropertiesFile = tokenPropertiesFile;
    }

    public void setRefreshTokenFile(String refreshTokenFile)
    {
        this.refreshTokenFile = refreshTokenFile;
    }
    
    
}