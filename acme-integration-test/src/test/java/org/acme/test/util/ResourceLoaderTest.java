package org.acme.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ResourceLoaderTest {

    @Test
    void testLoadBytes() throws IOException {
        byte[] bytes = ResourceLoader.loadBytes(ResourceLoaderTest.class, "/test-resource.txt");

        assertNotNull(bytes);
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertEquals("Test resource content", content.trim());
    }

    @Test
    void testLoadString() throws IOException {
        String content = ResourceLoader.loadString(ResourceLoaderTest.class, "/test-resource.txt");

        assertNotNull(content);
        assertEquals("Test resource content", content.trim());
    }

    @Test
    void testLoadBytesWithNonExistentResource() {
        assertThrows(
                IOException.class,
                () -> ResourceLoader.loadBytes(ResourceLoaderTest.class, "/non-existent.txt"));
    }

    @Test
    void testLoadStringWithNonExistentResource() {
        assertThrows(
                IOException.class,
                () -> ResourceLoader.loadString(ResourceLoaderTest.class, "/non-existent.txt"));
    }

    @Test
    void testLoadBytesWithJsonResource() throws IOException {
        byte[] bytes = ResourceLoader.loadBytes(ResourceLoaderTest.class, "/test-resource.json");

        assertNotNull(bytes);
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("{") || content.contains("["));
    }

    @Test
    void testLoadStringWithJsonResource() throws IOException {
        String content = ResourceLoader.loadString(ResourceLoaderTest.class, "/test-resource.json");

        assertNotNull(content);
        assertTrue(content.contains("{") || content.contains("["));
    }
}
