package com.higgstx.schwabtest.config;

import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabapi.service.MarketDataService;
import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.exception.SchwabApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Schwab API services - simplified
 */
@Slf4j
@Configuration
public class SchwabServiceConfiguration {

    @Bean
    public SchwabApiProperties schwabApiProperties(SchwabTestConfig config) {
        log.debug("Creating SchwabApiProperties from test harness configuration");
        return new SchwabApiProperties(
                config.getUrls().getAuth(),
                config.getUrls().getToken(),
                config.getUrls().getMarketData(),
                config.getDefaults().getRedirectUri(),
                config.getDefaults().getScope(),
                config.getDefaults().getHttpTimeoutMs()
        );
    }

    @Bean
    public TokenManager tokenManager(SchwabTestConfig config) throws SchwabApiException {
        log.debug("Creating TokenManager with token file: {}", config.getTokenPropertiesFile());
        return new TokenManager(
                config.getTokenPropertiesFile(),
                config.getAppKey(),
                config.getAppSecret()
        );
    }

    @Bean
    public MarketDataService marketDataService(SchwabApiProperties apiProperties, 
                                             TokenManager tokenManager) throws SchwabApiException {
        log.debug("Creating MarketDataService");
        return new MarketDataService(apiProperties, tokenManager);
    }
}