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

    /**
     * Formats a MultiValueMap of headers into a string where each header is on its
     * own line in the format "- name: value1, value2".
     *
     * @param headers the headers to format
     * @return formatted header string with each header on a new line, prefixed with
     *         "- "
     */
    public static String formatHeaders(MultiValueMap<String, String> headers) {
        return headers.entrySet().stream()
                .map(entry -> "- %s: %s".formatted(
                        entry.getKey(),
                        String.join(", ", entry.getValue())))
                .collect(Collectors.joining("\n"));
    }
}
