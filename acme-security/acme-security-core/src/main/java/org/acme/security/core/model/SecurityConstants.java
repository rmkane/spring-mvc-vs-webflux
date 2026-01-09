package org.acme.security.core.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Security-related constants. This is a utility class with a private
 * constructor to prevent instantiation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityConstants {

    public static final String DN_HEADER = "x-dn";
    public static final String UNAUTHORIZED_MESSAGE = "Missing or invalid x-dn header";
    public static final String MISSING_DN_MESSAGE = "Missing or empty x-dn header";

    public static final String PROMETHEUS_ENDPOINT = "/actuator/prometheus";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/error",
            "/actuator/**"
    };
}
