package org.acme.security.core.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Security-related constants. This is a utility class with a private
 * constructor to prevent instantiation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

    // Header names
    public static final String SSL_CLIENT_SUBJECT_HEADER = "x-amzn-mtls-clientcert-subject";
    public static final String SSL_CLIENT_ISSUER_HEADER = "x-amzn-mtls-clientcert-issuer";

    // Error messages
    public static final String UNAUTHORIZED_MESSAGE = "Missing or invalid client subject or issuer header";
    public static final String MISSING_HEADER_MESSAGE = "Missing or empty header: %s";
    public static final String MISSING_SUBJECT_MESSAGE = "Missing or empty subject DN";

    /**
     * Prefix for ACME role groups.
     * <p>
     * Groups are named with this prefix (e.g., {@code ACME_READ_WRITE},
     * {@code ACME_READ_ONLY}) and are used directly as Spring Security authorities.
     */
    public static final String ACME_GROUP_PREFIX = "ACME_";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/**",
            "/error",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
    };
}
