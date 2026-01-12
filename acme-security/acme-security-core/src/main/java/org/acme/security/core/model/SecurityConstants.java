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
    public static final String SSL_CLIENT_SUBJECT_DN_HEADER = "ssl-client-subject-dn";
    public static final String SSL_CLIENT_ISSUER_DN_HEADER = "ssl-client-issuer-dn";

    // Error messages
    public static final String UNAUTHORIZED_MESSAGE = "Missing or invalid ssl-client-subject-dn or ssl-client-issuer-dn header";
    public static final String MISSING_DN_MESSAGE = "Missing or empty ssl-client-subject-dn header";
    public static final String MISSING_ISSUER_DN_MESSAGE = "Missing or empty ssl-client-issuer-dn header";

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
