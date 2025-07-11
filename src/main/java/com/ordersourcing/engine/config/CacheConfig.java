package com.ordersourcing.engine.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "locations", 
            "inventories",
            "locationScores",
            "locationFilters",
            "carrierConfigs",
            "inventory",
            "scoringConfigs",
            "defaultScoringConfig",
            "scoringConfigsByCategory",
            "allActiveScoringConfigs",
                "scoringConfigForItem"
        ));
        return cacheManager;
    }
}