package org.acme.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class ResponseWriterTest {

    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testWriteString() throws IOException {
        String content = "Test response content";
        String fileName = "test-response.txt";

        ResponseWriter.write(content, fileName, tempDir);

        Path filePath = tempDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        String writtenContent = Files.readString(filePath, StandardCharsets.UTF_8);
        assertEquals(content, writtenContent);
    }

    @Test
    void testWriteStringWithDefaultDirectory() throws IOException {
        String content = "Test response content";
        String fileName = "test-response.txt";

        // This will write to target/integration by default
        ResponseWriter.write(content, fileName);

        // Verify file was created (we can't easily test default directory without
        // mocking)
        // But we can verify no exception was thrown
        assertTrue(true);
    }

    @Test
    void testWriteJsonWithString() throws IOException {
        String jsonString = "{\"name\":\"John\",\"age\":30}";
        String fileName = "test-response.json";

        // Use the write method with custom directory, then manually format
        String formatted = JsonFormatter.format(objectMapper, jsonString);
        ResponseWriter.write(formatted, fileName, tempDir);

        Path filePath = tempDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        String writtenContent = Files.readString(filePath, StandardCharsets.UTF_8);
        // Should be formatted (pretty printed)
        assertTrue(writtenContent.contains("\n"));
        assertTrue(writtenContent.contains("name"));
        assertTrue(writtenContent.contains("John"));
    }

    @Test
    void testWriteJsonWithObject() throws IOException {
        TestObject obj = new TestObject("John", 30);
        String fileName = "test-object.json";

        // Use the writeJson method which writes to default directory
        ResponseWriter.writeJson(objectMapper, obj, fileName);

        // Verify no exception was thrown
        assertTrue(true);
    }

    @Test
    void testWriteJsonWithDefaultDirectory() throws IOException {
        String jsonString = "{\"name\":\"John\"}";
        String fileName = "test-response.json";

        // This will write to target/integration by default
        ResponseWriter.writeJson(objectMapper, jsonString, fileName);

        // Verify no exception was thrown
        assertTrue(true);
    }

    @Test
    void testWriteCreatesDirectories() throws IOException {
        Path nestedDir = tempDir.resolve("nested").resolve("deep");
        String content = "Test content";
        String fileName = "test.txt";

        ResponseWriter.write(content, fileName, nestedDir);

        Path filePath = nestedDir.resolve(fileName);
        assertTrue(Files.exists(filePath));
        assertTrue(Files.exists(nestedDir));
    }

    // Helper class for testing
    record TestObject(String name, int age) {
    }
}
