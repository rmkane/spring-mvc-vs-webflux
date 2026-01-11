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
     * Extracts the Subject Distinguished Name (DN) from an MVC HttpServletRequest.
     *
     * @param request the HttpServletRequest
     * @return the Subject DN header value, or null if not present
     */
    public static String extractSubjectDn(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader(SecurityConstants.SSL_CLIENT_SUBJECT_DN_HEADER);
    }

    /**
     * Extracts the Issuer Distinguished Name (DN) from an MVC HttpServletRequest.
     *
     * @param request the HttpServletRequest
     * @return the Issuer DN header value, or null if not present
     */
    public static String extractIssuerDn(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader(SecurityConstants.SSL_CLIENT_ISSUER_DN_HEADER);
    }
}
