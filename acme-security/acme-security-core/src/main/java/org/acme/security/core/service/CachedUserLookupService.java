package org.acme.security.core.service;

import org.acme.auth.client.AuthServiceClient;
import org.acme.auth.client.UserInfo;
import org.acme.security.core.model.SecurityConstants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for looking up users with caching.
 * <p>
 * Wraps the AuthServiceClient and provides caching to reduce calls to the auth
 * service. Cache is configured via Spring Boot's Caffeine auto-configuration in
 * application.yml.
 * <p>
 * Cache misses are logged when the method executes (cache hits don't execute
 * the method due to {@code @Cacheable}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedUserLookupService {

    private final AuthServiceClient authServiceClient;

    /**
     * Looks up a user by DN from the auth service. Results are cached to reduce
     * calls to the auth service.
     * <p>
     * User lookups are cached using the "users" cache keyed by DN. Cache
     * configuration is defined in application.yml via Spring Boot's Caffeine
     * auto-configuration.
     * <p>
     * This method only executes on cache misses, so logging here indicates a cache
     * miss.
     *
     * @param dn the Distinguished Name to lookup
     * @return UserInfo with DN, name, and roles
     * @throws BadCredentialsException if user not found or service unavailable
     */
    @Cacheable(value = "users", key = "#p0")
    public UserInfo lookupUser(String dn) {
        log.debug("Cache MISS: cache=users, key={}", dn);
        if (!StringUtils.hasText(dn)) {
            throw new BadCredentialsException(SecurityConstants.MISSING_DN_MESSAGE);
        }
        return authServiceClient.lookupUser(dn.trim());
    }
}
