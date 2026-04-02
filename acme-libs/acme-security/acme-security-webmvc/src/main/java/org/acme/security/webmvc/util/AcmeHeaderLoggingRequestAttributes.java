package org.acme.security.webmvc.util;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.security.core.model.AcmeHeaderLoggingAttributes;

/**
 * Servlet request attributes for
 * {@link AcmeHeaderLoggingAttributes#ATTRIBUTE_NAME}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmeHeaderLoggingRequestAttributes {

    public static void put(HttpServletRequest request, boolean suppressed) {
        if (suppressed) {
            request.setAttribute(AcmeHeaderLoggingAttributes.ATTRIBUTE_NAME, Boolean.TRUE);
        } else {
            request.removeAttribute(AcmeHeaderLoggingAttributes.ATTRIBUTE_NAME);
        }
    }

    public static void clear(HttpServletRequest request) {
        request.removeAttribute(AcmeHeaderLoggingAttributes.ATTRIBUTE_NAME);
    }

    public static boolean isSuppressed(HttpServletRequest request) {
        return AcmeHeaderLoggingAttributes.isSuppressedValue(
                request.getAttribute(AcmeHeaderLoggingAttributes.ATTRIBUTE_NAME));
    }
}
