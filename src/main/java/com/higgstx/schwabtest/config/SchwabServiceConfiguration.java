package com.higgstx.schwabtest.config;

import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabapi.service.MarketDataService;
import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.exception.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchwabServiceConfiguration {

    @Bean
    public SchwabApiProperties schwabApiProperties(SchwabTestConfig config) {
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
        return new TokenManager(
                config.getTokenPropertiesFile(),
                config.getAppKey(),
                config.getAppSecret()
        );
    }

    @Bean
    public MarketDataService marketDataService(SchwabApiProperties apiProperties, TokenManager tokenManager) 
            throws SchwabApiException {
        return new MarketDataService(apiProperties, tokenManager);
    }
}