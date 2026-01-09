package org.acme.security.core.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PathMatcherUtilTest {

    @Test
    void isPublicEndpoint_shouldReturnTrue_forActuatorEndpoints() {
        assertTrue(PathMatcherUtil.isPublicEndpoint("/actuator/health"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/actuator/prometheus"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/actuator/info"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/actuator/metrics"));
        // Note: /actuator (without trailing slash) does not match /actuator/** pattern
        // Only paths starting with /actuator/ match
    }

    @Test
    void isPublicEndpoint_shouldReturnTrue_forSwaggerEndpoints() {
        assertTrue(PathMatcherUtil.isPublicEndpoint("/swagger-ui.html"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/swagger-ui/index.html"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/swagger-ui/swagger-ui.css"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/v3/api-docs"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/v3/api-docs/openapi.json"));
    }

    @Test
    void isPublicEndpoint_shouldReturnTrue_forErrorEndpoint() {
        assertTrue(PathMatcherUtil.isPublicEndpoint("/error"));
    }

    @Test
    void isPublicEndpoint_shouldReturnFalse_forProtectedEndpoints() {
        assertFalse(PathMatcherUtil.isPublicEndpoint("/api/v1/books"));
        assertFalse(PathMatcherUtil.isPublicEndpoint("/api/v1/books/1"));
        // Note: /actuator/private would match /actuator/** pattern, so it's considered
        // public
        // If you need to exclude specific actuator endpoints, configure them separately
        // Note: /swagger-ui matches /swagger-ui/** pattern (base path), so it's
        // considered public
        assertFalse(PathMatcherUtil.isPublicEndpoint("/v3/api-docs-private"));
        assertFalse(PathMatcherUtil.isPublicEndpoint("/swagger-ui.html/extra"));
    }

    @Test
    void isPublicEndpoint_shouldReturnFalse_forEmptyPath() {
        assertFalse(PathMatcherUtil.isPublicEndpoint(""));
        assertFalse(PathMatcherUtil.isPublicEndpoint("/"));
    }

    @Test
    void isPublicEndpoint_shouldHandleExactPatternMatching() {
        // Test exact pattern matching (no wildcard)
        assertTrue(PathMatcherUtil.isPublicEndpoint("/swagger-ui.html"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/error"));
        assertFalse(PathMatcherUtil.isPublicEndpoint("/swagger-ui.html/extra"));
        assertFalse(PathMatcherUtil.isPublicEndpoint("/error/extra"));
    }

    @Test
    void isPublicEndpoint_shouldHandleWildcardPatternMatching() {
        // Test wildcard pattern matching (/**)
        assertTrue(PathMatcherUtil.isPublicEndpoint("/actuator/health"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/actuator/prometheus"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/swagger-ui/index.html"));
        assertTrue(PathMatcherUtil.isPublicEndpoint("/v3/api-docs/openapi.json"));
    }
}
