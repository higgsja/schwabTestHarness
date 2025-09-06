package com.higgstx.schwabtest.config;

import com.higgstx.schwabapi.service.TokenManager;
import com.higgstx.schwabapi.service.MarketDataService;
import com.higgstx.schwabapi.config.SchwabApiProperties;
import com.higgstx.schwabapi.exception.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchwabServiceConfiguration
{

    @Bean
    public TokenManager tokenManager(SchwabTestConfig config)
            throws SchwabApiException
    {
        return new TokenManager(
                config.getTokenPropertiesFile(),
                config.getRefreshTokenFile(),
                config.getAppKey(),
                config.getAppSecret()
        );
    }

    @Bean
    public MarketDataService marketDataService(SchwabTestConfig config)
            throws SchwabApiException
    {
        // Create SchwabApiProperties from our Spring configuration
        SchwabApiProperties apiProperties = new SchwabApiProperties(
                config.getUrls().getAuth(),
                config.getUrls().getToken(),
                config.getUrls().getMarketData(),
                config.getDefaults().getRedirectUri(),
                config.getDefaults().getScope(),
                config.getDefaults().getHttpTimeoutMs()
        );

        return new MarketDataService(apiProperties);
    }
}
