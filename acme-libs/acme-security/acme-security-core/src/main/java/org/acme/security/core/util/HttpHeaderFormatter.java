package org.acme.security.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for formatting HTTP requests and responses into a cURL-like
 * readable format.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpHeaderFormatter {

    private static final String REQUEST_PREFIX = "> ";
    private static final String RESPONSE_PREFIX = "< ";
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "x-auth-token",
            "x-access-token",
            "x-refresh-token",
            "x-csrf-token",
            "x-xsrf-token");

    /**
     * Formats an HTTP request line in cURL style.
     *
     * @param message optional message to display before the request (may be null)
     * @param method  the HTTP method (GET, POST, etc.)
     * @param uri     the request URI
     * @param query   the query string (may be null)
     * @param headers the request headers
     * @return formatted request with method line and headers
     */
    public static String formatRequest(
            String message,
            String method,
            String uri,
            String query,
            MultiValueMap<String, String> headers) {
        String withQuery = query != null ? uri + "?" + query : uri;
        String requestLine = "%s%s %s %s".formatted(REQUEST_PREFIX, method, withQuery, HTTP_VERSION);
        String formattedRequest = "%s\n%s".formatted(requestLine, formatRequestHeaders(headers));
        if (message == null) {
            return formattedRequest;
        }
        return "%s\n%s".formatted(message, formattedRequest);
    }

    /**
     * Formats an HTTP response line in cURL style.
     *
     * @param message optional message to display before the response (may be null)
     * @param status  the HTTP status (e.g., "200 OK")
     * @param headers the response headers
     * @return formatted response with status line and headers
     */
    public static String formatResponse(
            String message, String status,
            MultiValueMap<String, String> headers) {
        String formattedResponse = "%s%s %s\n%s".formatted(
                RESPONSE_PREFIX, HTTP_VERSION, status, formatResponseHeaders(headers));
        if (message == null) {
            return formattedResponse;
        }
        return "%s\n%s".formatted(message, formattedResponse);
    }

    static String formatRequestHeaders(MultiValueMap<String, String> headers) {
        return formatHeaders(headers, REQUEST_PREFIX);
    }

    static String formatResponseHeaders(MultiValueMap<String, String> headers) {
        return formatHeaders(headers, RESPONSE_PREFIX);
    }

    /**
     * Formats a MultiValueMap of headers into a string where each header is on its
     * own line in the format "> name: value1, value2" or "< name: value1, value2".
     *
     * @param headers the headers to format
     * @param prefix  the prefix for each line ("> " for requests, "< " for
     *                responses)
     * @return formatted header string with each header on a new line
     */
    static String formatHeaders(MultiValueMap<String, String> headers, String prefix) {
        return headers.entrySet().stream()
                .map(entry -> "%s%s: %s".formatted(
                        prefix,
                        entry.getKey(),
                        String.join(", ", entry.getValue())))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Returns a copy of headers with values for sensitive security headers replaced
     * by {@code ***} (case-insensitive name match).
     */
    public static MultiValueMap<String, String> redactSensitiveHeaders(MultiValueMap<String, String> headers) {
        MultiValueMap<String, String> out = new LinkedMultiValueMap<>();
        headers.forEach((name, values) -> {
            if (isSensitiveHeaderName(name)) {
                out.put(name, new ArrayList<>(List.of("***")));
            } else {
                out.put(name, new ArrayList<>(values));
            }
        });
        return out;
    }

    private static boolean isSensitiveHeaderName(String headerName) {
        String normalized = headerName.toLowerCase();
        return SENSITIVE_HEADER_NAMES.contains(normalized)
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.endsWith("api-key");
    }
}
