package org.acme.security.webflux.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.security.core.policy.AcmeHeaderLoggingPolicy;

/**
 * Utility class for HTTP operations in the WebFlux context.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpUtils {

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

    /**
     * Header map with lowercase keys for
     * {@link AcmeHeaderLoggingPolicy#shouldLog(boolean, Map)}.
     */
    public static Map<String, List<String>> collectNormalizedHeadersForLoggingPolicy(ServerHttpRequest request) {
        Map<String, List<String>> raw = new LinkedHashMap<>();
        request.getHeaders().forEach((name, values) -> raw.put(name, new ArrayList<>(values)));
        return AcmeHeaderLoggingPolicy.normalizeHeaderMap(raw);
    }
}
