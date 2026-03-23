package org.acme.security.core.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.security.core.config.properties.HeaderFilterProperties;

/**
 * Locks in header-filter suppression behavior aligned with
 * {@code application.yml} and {@code scripts/simulator/simulate-traffic.sh}.
 */
class AcmeHeaderLoggingPolicyTest {

    private static final String SAMPLE_IGNORE_HEADERS = "{\"user-agent\":[\"ELB-HealthChecker/*\",\"HealthChecker/*\",\"kube-probe/*\"]}";

    private static AcmeHeaderLoggingPolicy policy(boolean disabled, String ignoreHeadersJson) {
        HeaderFilterProperties props = new HeaderFilterProperties(disabled, ignoreHeadersJson);
        return new AcmeHeaderLoggingPolicy(props, new ObjectMapper());
    }

    private static Map<String, List<String>> ua(String userAgent) {
        return Map.of("user-agent", List.of(userAgent));
    }

    @Test
    void matchesIgnoreRules_shouldMatch_probeUserAgentsFromSimulator() {
        AcmeHeaderLoggingPolicy p = policy(false, SAMPLE_IGNORE_HEADERS);
        assertTrue(p.matchesIgnoreRules(ua("HealthChecker/1.0")));
        assertTrue(p.matchesIgnoreRules(ua("ELB-HealthChecker/2.0")));
        assertTrue(p.matchesIgnoreRules(ua("kube-probe/1.28")));
    }

    @Test
    void matchesIgnoreRules_shouldNotMatch_normalClientUserAgent() {
        AcmeHeaderLoggingPolicy p = policy(false, SAMPLE_IGNORE_HEADERS);
        assertFalse(p.matchesIgnoreRules(ua("curl/8.7.1")));
        assertFalse(p.matchesIgnoreRules(ua("Mozilla/5.0")));
    }

    @Test
    void shouldLog_whenDebugOn_shouldRespectIgnoreRules() {
        AcmeHeaderLoggingPolicy p = policy(false, SAMPLE_IGNORE_HEADERS);
        assertFalse(p.shouldLog(true, ua("kube-probe/1.28")), "probe traffic should not dump headers");
        assertTrue(p.shouldLog(true, ua("curl/8.7.1")), "normal client should dump header logs when DEBUG on");
    }

    @Test
    void shouldLog_whenDisabled_shouldNeverLog() {
        AcmeHeaderLoggingPolicy p = policy(true, SAMPLE_IGNORE_HEADERS);
        assertFalse(p.shouldLog(true, ua("curl/8.7.1")));
        assertFalse(p.shouldLog(true, ua("kube-probe/1.28")));
    }

    @Test
    void shouldLog_whenDebugOff_shouldSkipRegardlessOfHeaders() {
        AcmeHeaderLoggingPolicy p = policy(false, SAMPLE_IGNORE_HEADERS);
        assertFalse(p.shouldLog(false, ua("curl/8.7.1")));
    }
}
