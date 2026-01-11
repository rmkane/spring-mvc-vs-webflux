package org.acme.security.webflux;

import org.springframework.http.server.reactive.ServerHttpRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.security.core.model.SecurityConstants;

/**
 * Utility class for extracting the DN header from WebFlux requests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestHeaderExtractor {

    /**
     * Extracts the Subject Distinguished Name (DN) from a WebFlux
     * ServerHttpRequest.
     *
     * @param request the ServerHttpRequest
     * @return the Subject DN header value, or null if not present
     */
    public static String extractSubjectDn(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeaders().getFirst(SecurityConstants.SSL_CLIENT_SUBJECT_DN_HEADER);
    }

    /**
     * Extracts the Issuer Distinguished Name (DN) from a WebFlux ServerHttpRequest.
     *
     * @param request the ServerHttpRequest
     * @return the Issuer DN header value, or null if not present
     */
    public static String extractIssuerDn(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeaders().getFirst(SecurityConstants.SSL_CLIENT_ISSUER_DN_HEADER);
    }
}
