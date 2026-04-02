package org.acme.security.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import org.acme.security.core.model.SecurityConstants;

/**
 * Bound from {@code acme.security.headers.*} (YAML keys {@code subject-dn},
 * {@code issuer-dn}).
 */
@ConfigurationProperties(prefix = "acme.security.headers")
public record HeadersProperties(
        @DefaultValue(SecurityConstants.SSL_CLIENT_SUBJECT_HEADER) String subjectDn,
        @DefaultValue(SecurityConstants.SSL_CLIENT_ISSUER_HEADER) String issuerDn) {
}
