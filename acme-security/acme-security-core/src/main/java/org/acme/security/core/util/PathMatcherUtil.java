package org.acme.security.core.util;

import java.util.Arrays;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.security.core.model.SecurityConstants;

/**
 * Utility class for matching request paths against security endpoint patterns.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathMatcherUtil {

    /**
     * Checks if the given path matches any of the public endpoint patterns defined
     * in {@link SecurityConstants#PUBLIC_ENDPOINTS}.
     * <p>
     * Pattern matching rules:
     * <ul>
     * <li>For patterns ending with {@code /**}, checks if the path starts with the
     * prefix (without the {@code /**} suffix)</li>
     * <li>For exact patterns, checks if the path equals the pattern</li>
     * </ul>
     *
     * @param path the request path to check
     * @return true if the path matches a public endpoint pattern, false otherwise
     */
    public static boolean isPublicEndpoint(String path) {
        return matchesAnyPattern(path, SecurityConstants.PUBLIC_ENDPOINTS);
    }

    /**
     * Whether header DEBUG logging should be skipped for this path (public
     * endpoints, default health/metrics/probe paths, and optional extra patterns
     * from configuration).
     *
     * @param path          request path (no query string)
     * @param extraPatterns additional patterns from
     *                      {@code acme.security.header-filter.skip-logging-paths}
     */
    public static boolean shouldSkipHeaderLogging(String path, List<String> extraPatterns) {
        if (isPublicEndpoint(path)) {
            return true;
        }
        if (matchesAnyPattern(path, SecurityConstants.DEFAULT_LOGGING_SKIP_PATHS)) {
            return true;
        }
        if (extraPatterns != null && !extraPatterns.isEmpty()) {
            return matchesAnyPattern(path, extraPatterns.toArray(String[]::new));
        }
        return false;
    }

    /**
     * Returns true if {@code path} matches any of the patterns (same rules as
     * {@link #isPublicEndpoint(String)}).
     */
    public static boolean matchesAnyPattern(String path, String[] patterns) {
        return Arrays.stream(patterns).anyMatch(pattern -> matchesPattern(pattern, path));
    }

    static boolean matchesPattern(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return path.equals(pattern);
    }
}
