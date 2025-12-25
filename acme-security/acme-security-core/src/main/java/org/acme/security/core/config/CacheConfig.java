package org.acme.security.core.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache configuration for user lookups in the security layer.
 * <p>
 * Configures a Caffeine cache named "users" with a configurable TTL from
 * application.yml. This cache stores UserInfo objects keyed by DN to avoid
 * repeated calls to the auth service.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.users.ttl:PT5M}")
    private Duration usersCacheTtl;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(usersCacheTtl)
                        .maximumSize(1000)
                        .recordStats());
        return cacheManager;
    }
}
