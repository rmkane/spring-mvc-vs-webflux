package org.acme.security.webmvc;

import jakarta.servlet.http.HttpServletRequest;

import org.acme.security.core.SecurityConstants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for extracting the username header from MVC requests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestHeaderExtractor {

    /**
     * Extracts the username from an MVC HttpServletRequest.
     *
     * @param request the HttpServletRequest
     * @return the username header value, or null if not present
     */
    public static String extractUsername(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader(SecurityConstants.USERNAME_HEADER);
    }
}
