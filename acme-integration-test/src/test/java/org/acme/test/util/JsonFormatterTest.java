package org.acme.test.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class JsonFormatterTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testFormatSimpleJson() throws JsonProcessingException {
        String json = "{\"name\":\"John\",\"age\":30}";
        String formatted = JsonFormatter.format(objectMapper, json);

        assertNotNull(formatted);
        // Should contain newlines and indentation
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("name"));
        assertTrue(formatted.contains("John"));
    }

    @Test
    void testFormatNestedJson() throws JsonProcessingException {
        String json = "{\"user\":{\"name\":\"John\",\"address\":{\"city\":\"New York\"}}}";
        String formatted = JsonFormatter.format(objectMapper, json);

        assertNotNull(formatted);
        assertTrue(formatted.contains("user"));
        assertTrue(formatted.contains("name"));
        assertTrue(formatted.contains("address"));
        assertTrue(formatted.contains("city"));
    }

    @Test
    void testFormatArrayJson() throws JsonProcessingException {
        String json = "{\"items\":[{\"id\":1,\"name\":\"item1\"},{\"id\":2,\"name\":\"item2\"}]}";
        String formatted = JsonFormatter.format(objectMapper, json);

        assertNotNull(formatted);
        assertTrue(formatted.contains("items"));
        assertTrue(formatted.contains("item1"));
        assertTrue(formatted.contains("item2"));
    }

    @Test
    void testFormatAlreadyFormattedJson() throws JsonProcessingException {
        String json = "{\"name\":\"John\"}";
        String formatted1 = JsonFormatter.format(objectMapper, json);
        String formatted2 = JsonFormatter.format(objectMapper, formatted1);

        // Should handle already formatted JSON gracefully
        assertNotNull(formatted2);
    }

    @Test
    void testFormatInvalidJson() {
        String invalidJson = "{invalid json}";

        assertThrows(
                JsonProcessingException.class, () -> JsonFormatter.format(objectMapper, invalidJson));
    }

    @Test
    void testFormatEmptyObject() throws JsonProcessingException {
        String json = "{}";
        String formatted = JsonFormatter.format(objectMapper, json);

        assertNotNull(formatted);
        assertTrue(formatted.contains("{") && formatted.contains("}"));
    }

    @Test
    void testFormatEmptyArray() throws JsonProcessingException {
        String json = "[]";
        String formatted = JsonFormatter.format(objectMapper, json);

        assertNotNull(formatted);
        assertTrue(formatted.contains("[") && formatted.contains("]"));
    }

    @Test
    void testFormatWithSpecialCharacters() throws JsonProcessingException {
        String json = "{\"message\":\"Hello \\\"World\\\"\"}";
        String formatted = JsonFormatter.format(objectMapper, json);

        assertNotNull(formatted);
        assertTrue(formatted.contains("Hello"));
    }
}
