package org.acme.security.webflux;

import org.acme.security.core.model.SecurityConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for extracting the DN header from WebFlux requests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestHeaderExtractor {

    /**
     * Extracts the Distinguished Name (DN) from a WebFlux ServerHttpRequest.
     *
     * @param request the ServerHttpRequest
     * @return the DN header value, or null if not present
     */
    public static String extractDn(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeaders().getFirst(SecurityConstants.DN_HEADER);
    }
}
