package org.acme.security.core.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Default cache configuration for user lookups in the security layer.
 * <p>
 * Provides a default Caffeine cache configuration if no other CacheManager is
 * provided by the API application. This allows each API to override with its
 * own cache provider (e.g., Hazelcast, Redis) by providing its own CacheManager
 * bean.
 * <p>
 * The cache is named "users" and stores UserInfo objects keyed by DN to avoid
 * repeated calls to the auth service.
 * <p>
 * To use a different cache provider (e.g., Hazelcast, Redis), simply provide
 * your own CacheManager bean in the API module's configuration. This default
 * configuration will be skipped automatically.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.users.ttl:PT5M}")
    private Duration usersCacheTtl;

    /**
     * Default Caffeine cache manager. Only created if no other CacheManager bean is
     * provided by the application. This allows APIs to provide their own cache
     * implementation (e.g., Hazelcast, Redis) by defining their own CacheManager
     * bean.
     * <p>
     * Cache TTL is configurable via {@code cache.users.ttl} in application.yml.
     *
     * @return CaffeineCacheManager with "users" cache
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
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
