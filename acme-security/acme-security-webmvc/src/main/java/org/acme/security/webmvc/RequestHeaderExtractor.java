package org.acme.security.webmvc;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.security.core.model.SecurityConstants;

/**
 * Utility class for extracting the DN header from MVC requests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestHeaderExtractor {

    /**
     * Extracts the Distinguished Name (DN) from an MVC HttpServletRequest.
     *
     * @param request the HttpServletRequest
     * @return the DN header value, or null if not present
     */
    public static String extractDn(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader(SecurityConstants.DN_HEADER);
    }
}
