package org.acme.security.webmvc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for HTTP operations in the WebMVC context.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpUtils {

    /**
     * Extracts all headers from an HttpServletRequest into a MultiValueMap.
     *
     * @param request the HTTP servlet request
     * @return MultiValueMap containing all request headers
     */
    public static MultiValueMap<String, String> getHeaders(HttpServletRequest request) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            headers.put(headerName, Collections.list(headerValues));
        }
        return headers;
    }

    /**
     * Extracts all headers from an HttpServletResponse into a MultiValueMap.
     *
     * @param response the HTTP servlet response
     * @return MultiValueMap containing all response headers
     */
    public static MultiValueMap<String, String> getHeaders(HttpServletResponse response) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        headerNames.forEach(headerName -> headers.put(headerName, new ArrayList<>(response.getHeaders(headerName))));
        return headers;
    }
}
