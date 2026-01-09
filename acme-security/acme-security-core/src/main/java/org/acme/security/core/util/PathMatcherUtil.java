package org.acme.security.core.util;

import java.util.Arrays;

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
        return Arrays.stream(SecurityConstants.PUBLIC_ENDPOINTS)
                .anyMatch(pattern -> {
                    if (pattern.endsWith("/**")) {
                        // For patterns ending with /**, check if path equals the prefix
                        // or starts with prefix + "/" (e.g., /v3/api-docs matches /v3/api-docs/**)
                        String prefix = pattern.substring(0, pattern.length() - 3);
                        return path.equals(prefix) || path.startsWith(prefix + "/");
                    }
                    // For exact patterns, check if path equals the pattern
                    return path.equals(pattern);
                });
    }
}
