package org.acme.security.core.policy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.security.core.config.properties.HeaderFilterProperties;
import org.acme.security.core.util.HeaderFilterConfigParser;
import org.acme.security.core.util.HeaderValuePatternMatcher;
import org.acme.security.core.util.PathMatcherUtil;

/**
 * Decides whether DEBUG request/response header logging should run, based on
 * {@link HeaderFilterProperties} and path rules.
 */
@Component
public class AcmeHeaderLoggingPolicy {

    private final boolean disabled;
    private final List<String> skipLoggingPaths;
    private final Map<String, List<HeaderValuePatternMatcher>> ignoredHeaderMatchers;

    public AcmeHeaderLoggingPolicy(HeaderFilterProperties properties, ObjectMapper objectMapper) {
        this.disabled = properties.disabled();
        this.skipLoggingPaths = properties.skipLoggingPaths();
        this.ignoredHeaderMatchers = compileIgnoredHeaders(
                HeaderFilterConfigParser.parseIgnoredHeaders(objectMapper, properties.ignoreHeaders()));
    }

    /**
     * @param headers header map with lowercase keys (see
     *                {@link #normalizeHeaderMap(Map)})
     */
    public boolean shouldLog(boolean debugEnabled, String path, Map<String, List<String>> headers) {
        if (disabled || !debugEnabled) {
            return false;
        }
        if (PathMatcherUtil.shouldSkipHeaderLogging(path, skipLoggingPaths)) {
            return false;
        }
        return !matchesIgnoreRules(headers);
    }

    /**
     * Whether the request matches
     * {@code acme.security.header-filter.ignore-headers} (e.g. kube probe
     * User-Agent). Independent of debug level.
     */
    public boolean matchesIgnoreRules(Map<String, List<String>> headers) {
        if (ignoredHeaderMatchers.isEmpty()) {
            return false;
        }
        return ignoredHeaderMatchers.entrySet().stream()
                .anyMatch(entry -> {
                    List<String> values = headers.get(entry.getKey());
                    if (values == null || values.isEmpty()) {
                        return false;
                    }
                    return values.stream()
                            .anyMatch(headerValue -> entry.getValue().stream()
                                    .anyMatch(matcher -> matcher.matches(headerValue)));
                });
    }

    public Map<String, List<HeaderValuePatternMatcher>> getIgnoredHeaderMatchers() {
        return ignoredHeaderMatchers;
    }

    /**
     * Normalizes keys to lowercase for use with
     * {@link #shouldLog(boolean, String, Map)}.
     */
    public static Map<String, List<String>> normalizeHeaderMap(Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> out = new LinkedHashMap<>();
        headers.forEach((k, v) -> out.put(k.toLowerCase(Locale.ROOT), v));
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, List<HeaderValuePatternMatcher>> compileIgnoredHeaders(
            Map<String, Set<String>> ignoredHeaders) {
        if (ignoredHeaders.isEmpty()) {
            return Map.of();
        }

        Map<String, List<HeaderValuePatternMatcher>> compiledMatchers = new LinkedHashMap<>();

        ignoredHeaders.forEach((headerName, ignoredValues) -> compiledMatchers.put(
                headerName,
                ignoredValues.stream()
                        .map(HeaderValuePatternMatcher::compile)
                        .toList()));

        return Map.copyOf(compiledMatchers);
    }
}
