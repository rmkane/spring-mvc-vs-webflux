package org.acme.security.webflux.util;

import java.util.ArrayList;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Utility class for HTTP operations in the WebFlux context.
 */
public final class HttpUtils {

    private HttpUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts all headers from a ServerHttpRequest into a MultiValueMap.
     *
     * @param request the reactive HTTP request
     * @return MultiValueMap containing all request headers
     */
    public static MultiValueMap<String, String> getHeaders(ServerHttpRequest request) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        request.getHeaders().forEach((name, values) -> headers.put(name, new ArrayList<>(values)));
        return headers;
    }

    /**
     * Extracts all headers from a ServerHttpResponse into a MultiValueMap.
     *
     * @param response the reactive HTTP response
     * @return MultiValueMap containing all response headers
     */
    public static MultiValueMap<String, String> getHeaders(ServerHttpResponse response) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        response.getHeaders().forEach((name, values) -> headers.put(name, new ArrayList<>(values)));
        return headers;
    }
}
