package com.higgstx.schwab.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * Spring Boot configuration for the Schwab API client.
 * This extends the base SchwabConfig with Spring Boot integration.
 */
@Component
@ConfigurationProperties(prefix = "schwab.api")
@Data
@EqualsAndHashCode(callSuper = true)
public class SchwabSpringConfig extends SchwabConfig {

    @PostConstruct
    public void initSpringConfig() {
        // Initialize the static fields in the base SchwabConfig class
        super.init();
        System.out.println("✅ Schwab Spring configuration initialized");
    }

    /**
     * Override to provide Spring-specific validation
     */
    @Override
    public boolean isConfigSet() {
        boolean isSet = super.isConfigSet();
        if (!isSet) {
            System.err.println("❌ Schwab configuration not properly set in application.yml");
            System.err.println("Expected properties: schwab.api.appKey and schwab.api.appSecret");
        }
        return isSet;
    }
}