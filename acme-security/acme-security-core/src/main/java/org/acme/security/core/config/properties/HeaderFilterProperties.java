package org.acme.security.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import org.acme.security.core.model.AcmeHeaderLoggingAttributes;

/**
 * Controls optional request/response header logging; suppression is driven only
 * by {@link #ignoreHeaders()} (see
 * {@link AcmeHeaderLoggingAttributes#ATTRIBUTE_NAME}).
 */
@ConfigurationProperties(prefix = "acme.security.header-filter")
public record HeaderFilterProperties(
        @DefaultValue("false") boolean disabled,
        /**
         * JSON object mapping header name (lowercase) to a set of exact or {@code *}
         * wildcard patterns; matching requests skip DEBUG header logging.
         */
        @DefaultValue("{}") String ignoreHeaders) {
}
