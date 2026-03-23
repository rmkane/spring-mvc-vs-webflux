package org.acme.security.core.model;

import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Request-scoped marker for suppressing DEBUG request/response header dumps.
 * Uses the same logical prefix as {@code acme.security.header-filter} in
 * configuration.
 * <p>
 * Bound to the request or exchange (not {@link ThreadLocal}), so it is safe for
 * async servlet handling and WebFlux.
 * <p>
 * For servlet requests and WebFlux exchanges, use
 * {@code AcmeHeaderLoggingRequestAttributes} and
 * {@code AcmeHeaderLoggingExchangeAttributes} in the webmvc and webflux
 * modules.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmeHeaderLoggingAttributes {

    /**
     * Attribute key; value is {@link Boolean#TRUE} when header logging should be
     * treated as suppressed for this request.
     */
    public static final String ATTRIBUTE_NAME = "acme.security.header-filter.suppressed";

    /**
     * Sets or clears suppression on a generic attribute map (e.g. WebFlux
     * {@code ServerWebExchange#getAttributes()}).
     *
     * @param suppressed {@code true} to set the marker, {@code false} to remove it
     */
    public static void put(Map<String, Object> attributes, boolean suppressed) {
        if (suppressed) {
            attributes.put(ATTRIBUTE_NAME, Boolean.TRUE);
        } else {
            attributes.remove(ATTRIBUTE_NAME);
        }
    }

    public static void clear(Map<String, Object> attributes) {
        attributes.remove(ATTRIBUTE_NAME);
    }

    public static boolean isSuppressed(Map<String, Object> attributes) {
        return Boolean.TRUE.equals(attributes.get(ATTRIBUTE_NAME));
    }

    /**
     * Interprets a value returned from {@code getAttribute(ATTRIBUTE_NAME)}.
     */
    public static boolean isSuppressedValue(Object attributeValue) {
        return Boolean.TRUE.equals(attributeValue);
    }
}
