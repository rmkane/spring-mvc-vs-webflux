package org.acme.security.core.util;

import java.util.stream.Collectors;

import org.springframework.util.MultiValueMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for formatting HTTP headers into a readable string format.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpHeaderFormatter {

    private static final String REQUEST_HEADER_PREFIX = "> ";
    private static final String RESPONSE_HEADER_PREFIX = "< ";

    public static String formatRequestHeaders(MultiValueMap<String, String> headers) {
        return formatHeaders(headers, REQUEST_HEADER_PREFIX);
    }

    public static String formatResponseHeaders(MultiValueMap<String, String> headers) {
        return formatHeaders(headers, RESPONSE_HEADER_PREFIX);
    }

    /**
     * Formats a MultiValueMap of headers into a string where each header is on its
     * own line in the format "- name: value1, value2".
     *
     * @param headers the headers to format
     * @return formatted header string with each header on a new line, prefixed with
     *         "- "
     */
    private static String formatHeaders(MultiValueMap<String, String> headers, String prefix) {
        return headers.entrySet().stream()
                .map(entry -> "%s%s: %s".formatted(
                        prefix,
                        entry.getKey(),
                        String.join(", ", entry.getValue())))
                .collect(Collectors.joining("\n"));
    }
}
