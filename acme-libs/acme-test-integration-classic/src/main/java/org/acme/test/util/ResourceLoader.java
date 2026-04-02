package org.acme.test.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility for loading test resources. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceLoader {
    /**
     * Loads a resource as a byte array.
     *
     * @param clazz    the class to use for resource loading (typically the test
     *                 class)
     * @param fileName the resource file name (e.g., "/files/test.txt")
     * @return the resource content as bytes
     * @throws IOException if the resource cannot be loaded
     */
    public static byte[] loadBytes(Class<?> clazz, String fileName) throws IOException {
        try (var inputStream = clazz.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + fileName);
            }
            return inputStream.readAllBytes();
        }
    }

    /**
     * Loads a resource as a string using UTF-8 encoding.
     *
     * @param clazz    the class to use for resource loading (typically the test
     *                 class)
     * @param fileName the resource file name (e.g., "/files/test.txt")
     * @return the resource content as a string
     * @throws IOException if the resource cannot be loaded
     */
    public static String loadString(Class<?> clazz, String fileName) throws IOException {
        return new String(loadBytes(clazz, fileName), StandardCharsets.UTF_8);
    }
}
