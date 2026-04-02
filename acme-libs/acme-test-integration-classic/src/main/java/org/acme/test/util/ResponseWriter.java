package org.acme.test.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility for writing test responses to files. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResponseWriter {
    private static final Path DEFAULT_OUTPUT_DIR = Path.of("target/integration");

    /**
     * Writes a string response to a file.
     *
     * @param response the response content to write
     * @param fileName the name of the file to write
     * @throws IOException if the file cannot be written
     */
    public static void write(String response, String fileName) throws IOException {
        write(response, fileName, DEFAULT_OUTPUT_DIR);
    }

    /**
     * Writes a string response to a file in the specified directory.
     *
     * @param response  the response content to write
     * @param fileName  the name of the file to write
     * @param outputDir the directory to write the file to
     * @throws IOException if the file cannot be written
     */
    public static void write(String response, String fileName, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve(fileName), response, StandardCharsets.UTF_8);
    }

    /**
     * Writes a JSON response to a file with formatting.
     *
     * @param objectMapper the ObjectMapper to use for formatting
     * @param jsonResponse the JSON string to write
     * @param fileName     the name of the file to write
     * @throws IOException if the file cannot be written
     */
    public static void writeJson(ObjectMapper objectMapper, String jsonResponse, String fileName)
            throws IOException {
        write(JsonFormatter.format(objectMapper, jsonResponse), fileName);
    }

    /**
     * Writes an object as JSON to a file with formatting.
     *
     * @param objectMapper the ObjectMapper to use for serialization and formatting
     * @param response     the object to serialize and write
     * @param fileName     the name of the file to write
     * @throws IOException if the file cannot be written
     */
    public static void writeJson(ObjectMapper objectMapper, Object response, String fileName)
            throws IOException {
        try {
            String json = objectMapper.writeValueAsString(response);
            write(JsonFormatter.format(objectMapper, json), fileName);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to serialize response to JSON", e);
        }
    }
}
