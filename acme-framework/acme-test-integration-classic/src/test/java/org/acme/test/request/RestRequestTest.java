package org.acme.test.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class RestRequestTest {

    @Test
    void testBasicRequest() {
        URI uri = URI.create("http://localhost:8080/api/users");
        HttpHeaders headers = new HttpHeaders();
        byte[] body = "test".getBytes(StandardCharsets.UTF_8);

        RestRequest request = new RestRequest(uri, HttpMethod.GET, headers, body);

        assertEquals(uri, request.getURI());
        assertEquals(HttpMethod.GET, request.getMethod());
        assertEquals("GET", request.getMethodValue());
        assertEquals(body, request.getBody());
        assertFalse(request.isMultipart());
        assertNull(request.getMultipartBody());
    }

    @Test
    void testMultipartRequest() {
        URI uri = URI.create("http://localhost:8080/api/upload");
        HttpHeaders headers = new HttpHeaders();
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("field", "value");

        RestRequest request = new RestRequest(uri, HttpMethod.POST, headers, multipartBody);

        assertEquals(uri, request.getURI());
        assertEquals(HttpMethod.POST, request.getMethod());
        assertTrue(request.isMultipart());
        assertNotNull(request.getMultipartBody());
        assertEquals("value", multipartBody.getFirst("field"));
    }

    @Test
    void testRequestWithAttributes() {
        URI uri = URI.create("http://localhost:8080/api/users");
        HttpHeaders headers = new HttpHeaders();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        attributes.put("key2", 123);

        RestRequest request = new RestRequest(uri, HttpMethod.GET, headers, null, null, attributes);

        assertEquals("value1", request.getAttributes().get("key1"));
        assertEquals(123, request.getAttributes().get("key2"));
    }

    @Test
    void testGetBodyAsString() {
        String bodyText = "test body";
        byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
        RestRequest request = new RestRequest(
                URI.create("http://localhost:8080"), HttpMethod.POST, new HttpHeaders(), body);

        assertTrue(request.getBodyAsString(StandardCharsets.UTF_8).isPresent());
        assertEquals(bodyText, request.getBodyAsString(StandardCharsets.UTF_8).get());
    }

    @Test
    void testGetBodyAsStringWithNullBody() {
        RestRequest request = new RestRequest(
                URI.create("http://localhost:8080"), HttpMethod.GET, new HttpHeaders(), (byte[]) null);

        assertFalse(request.getBodyAsString(StandardCharsets.UTF_8).isPresent());
    }

    @Test
    void testIsMultipartWithEmptyMultipart() {
        URI uri = URI.create("http://localhost:8080/api/upload");
        HttpHeaders headers = new HttpHeaders();
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();

        RestRequest request = new RestRequest(uri, HttpMethod.POST, headers, multipartBody);

        assertFalse(request.isMultipart());
    }

    @Test
    void testHeadersAreReadOnly() {
        HttpHeaders originalHeaders = new HttpHeaders();
        originalHeaders.add("X-Test", "value");

        RestRequest request = new RestRequest(
                URI.create("http://localhost:8080"), HttpMethod.GET, originalHeaders, (byte[]) null);

        HttpHeaders headers = request.getHeaders();
        // Headers should be read-only (wrapped by HttpHeaders.readOnlyHttpHeaders)
        assertTrue(headers.getClass().getSimpleName().contains("ReadOnly"));
    }

    @Test
    void testAttributesAreMutable() {
        Map<String, Object> attributes = new HashMap<>();
        RestRequest request = new RestRequest(
                URI.create("http://localhost:8080"),
                HttpMethod.GET,
                new HttpHeaders(),
                null,
                null,
                attributes);

        request.getAttributes().put("newKey", "newValue");
        assertEquals("newValue", request.getAttributes().get("newKey"));
    }
}
