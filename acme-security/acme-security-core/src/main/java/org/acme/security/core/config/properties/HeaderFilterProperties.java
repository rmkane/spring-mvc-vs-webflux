package org.acme.security.core.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Controls optional request/response header logging and path exclusions (e.g.
 * health probes).
 */
@ConfigurationProperties(prefix = "acme.security.header-filter")
public record HeaderFilterProperties(
        @DefaultValue("false") boolean disabled,
        /**
         * JSON object mapping header name (lowercase) to a set of exact or {@code *}
         * wildcard patterns; matching requests skip DEBUG header logging.
         */
        @DefaultValue("{}") String ignoreHeaders,
        /**
         * Additional path patterns (same rules as
         * {@link org.acme.security.core.util.PathMatcherUtil}) for which header logging
         * is skipped (e.g. {@code /actuator/health}).
         */
        @DefaultValue("[]") List<String> skipLoggingPaths) {

    public HeaderFilterProperties {
        skipLoggingPaths = skipLoggingPaths == null ? List.of() : List.copyOf(skipLoggingPaths);
    }
}
