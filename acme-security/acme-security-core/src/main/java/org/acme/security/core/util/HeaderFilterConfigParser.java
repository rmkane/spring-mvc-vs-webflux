package org.acme.security.core.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HeaderFilterConfigParser {

    private static final String IGNORE_HEADERS_PROPERTY = "acme.security.header-filter.ignore-headers";
    private static final TypeReference<Map<String, Set<String>>> IGNORED_HEADERS_TYPE = new TypeReference<>() {
    };

    public static Map<String, Set<String>> parseIgnoredHeaders(ObjectMapper objectMapper, String ignoredHeadersJson) {
        if (ignoredHeadersJson == null || ignoredHeadersJson.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, Set<String>> parsedHeaders = objectMapper.copy()
                    .configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true)
                    .readValue(ignoredHeadersJson, IGNORED_HEADERS_TYPE);

            return normalizeHeaders(parsedHeaders);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse " + IGNORE_HEADERS_PROPERTY + " JSON", exception);
        }
    }

    private static Map<String, Set<String>> normalizeHeaders(Map<String, Set<String>> parsedHeaders) {
        if (parsedHeaders == null || parsedHeaders.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<String>> normalizedHeaders = new LinkedHashMap<>();

        parsedHeaders.forEach((headerName, headerValues) -> normalizedHeaders.put(
                headerName.toLowerCase(Locale.ROOT),
                normalizeValues(headerValues)));

        return Collections.unmodifiableMap(normalizedHeaders);
    }

    private static Set<String> normalizeValues(Set<String> headerValues) {
        if (headerValues == null || headerValues.isEmpty()) {
            return Collections.emptySet();
        }

        return headerValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());
    }
}
