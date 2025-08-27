package com.higgstx.schwabtest.config;

import lombok.*;
import org.springframework.boot.context.properties.*;
import org.springframework.stereotype.*;

@Component
@ConfigurationProperties(prefix = "schwab.api")
@Data
public class SchwabTestConfig {
    
    private String appKey;
    private String appSecret;
    private String tokenPropertiesFile = "schwab-api.json";
    private String refreshTokenFile = "schwab-refresh-token.txt";
    
    public boolean isValid() {
        return appKey != null && !appKey.trim().isEmpty() &&
               appSecret != null && !appSecret.trim().isEmpty();
    }
    
    public void validateConfig() {
        if (!isValid()) {
            throw new IllegalStateException("Schwab API configuration incomplete");
        }
    }
    
    public void showConfig() {
        System.out.println("=== Schwab Test Configuration ===");
        System.out.println("App Key: " + maskValue(appKey));
        System.out.println("App Secret: " + maskValue(appSecret));
    }
    
    private String maskValue(String value) {
        if (value == null || value.length() <= 8) return "****";
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