package org.acme.security.core.listener;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.acme.security.core.config.properties.HeaderFilterProperties;
import org.acme.security.core.config.properties.HeadersProperties;
import org.acme.security.core.util.CollectionWritingUtils;
import org.acme.security.core.util.HeaderValuePatternMatcher;

/**
 * Logs a snapshot of security header configuration at application startup.
 */
@Slf4j
public abstract class AbstractAcmeSecurityStartupListener {

    private final HeaderFilterProperties headerFilterProperties;
    private final HeadersProperties headersProperties;
    private final ObjectMapper objectMapper;

    protected AbstractAcmeSecurityStartupListener(
            HeaderFilterProperties headerFilterProperties,
            HeadersProperties headersProperties,
            ObjectMapper objectMapper) {
        this.headerFilterProperties = headerFilterProperties;
        this.headersProperties = headersProperties;
        this.objectMapper = objectMapper;
    }

    protected void logSecurityHeaderPolicySnapshot(Map<String, List<HeaderValuePatternMatcher>> ignoredHeaderMatchers) {
        log.debug(
                "Headers properties: subjectDn={}, issuerDn={}",
                headersProperties.subjectDn(),
                headersProperties.issuerDn());
        log.debug(
                "Request header logging policy snapshot: disabled={}, ignoreHeaders={}",
                headerFilterProperties.disabled(),
                serializeIgnoredHeadersAsJson(ignoredHeaderMatchers));
    }

    private String serializeIgnoredHeadersAsJson(Map<String, List<HeaderValuePatternMatcher>> ignoredMatchers) {
        Map<String, List<String>> stringLists = CollectionWritingUtils.mapToStringLists(
                ignoredMatchers, HeaderValuePatternMatcher::getOriginalValue);
        try {
            return objectMapper.writeValueAsString(stringLists);
        } catch (JsonProcessingException e) {
            log.error("Failed to write ignored headers to JSON", e);
            return "{}";
        }
    }
}
