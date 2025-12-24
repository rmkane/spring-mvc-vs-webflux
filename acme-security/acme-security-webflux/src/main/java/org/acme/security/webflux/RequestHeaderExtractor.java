package org.acme.security.webflux;

import org.acme.security.core.SecurityConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for extracting the username header from WebFlux requests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestHeaderExtractor {

    /**
     * Extracts the username from a WebFlux ServerHttpRequest.
     *
     * @param request the ServerHttpRequest
     * @return the username header value, or null if not present
     */
    public static String extractUsername(ServerHttpRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeaders().getFirst(SecurityConstants.USERNAME_HEADER);
    }
}
