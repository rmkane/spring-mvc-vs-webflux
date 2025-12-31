package org.acme.test.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility for formatting JSON strings. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonFormatter {
    /**
     * Formats a JSON string with pretty printing.
     *
     * @param objectMapper the ObjectMapper to use for parsing and formatting
     * @param json         the JSON string to format
     * @return formatted JSON string
     * @throws JsonProcessingException if the JSON is invalid
     */
    public static String format(ObjectMapper objectMapper, String json)
            throws JsonProcessingException {
        return objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(objectMapper.readValue(json, Object.class));
    }
}
