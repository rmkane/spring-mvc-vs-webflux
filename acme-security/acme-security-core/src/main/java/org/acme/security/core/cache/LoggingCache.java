package org.acme.security.core.cache;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cache wrapper that logs cache hits and misses.
 * <p>
 * Decorates a Spring Cache to add logging for cache operations while preserving
 * all cache functionality. This allows using {@code @Cacheable} annotations
 * while still logging cache hits and misses.
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingCache implements Cache {

    private final Cache delegate;

    @NonNull
    @Override
    public String getName() {
        return delegate.getName();
    }

    @NonNull
    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(@NonNull Object key) {
        ValueWrapper wrapper = delegate.get(key);
        logCacheHitOrMiss(getName(), key, wrapper != null);
        return wrapper;
    }

    @Override
    public <T> T get(@NonNull Object key, Class<T> type) {
        T value = delegate.get(key, type);
        logCacheHitOrMiss(getName(), key, value != null);
        return value;
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        // Check if value exists in cache first to determine hit/miss
        ValueWrapper wrapper = delegate.get(key);
        logCacheHitOrMiss(getName(), key, wrapper != null);
        if (wrapper != null) {
            @SuppressWarnings("unchecked")
            T value = (T) wrapper.get();
            return value;
        }
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(@NonNull Object key, Object value) {
        delegate.put(key, value);
    }

    @Override
    public void evict(@NonNull Object key) {
        delegate.evict(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    private void logCacheHitOrMiss(String cacheName, Object key, boolean isHit) {
        log.debug("Cache {}: cache={}, key={}", isHit ? "HIT" : "MISS", cacheName, key);
    }
}
