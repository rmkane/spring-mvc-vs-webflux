package org.acme.security.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class HttpHeaderFormatterTest {

    @Test
    void formatRequest_withQueryString() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.HOST, "localhost:8080");
        headers.add(HttpHeaders.ACCEPT, "application/json");

        String result = HttpHeaderFormatter.formatRequest(
                "Dumping request info:",
                "GET",
                "/api/books",
                "page=1&size=10",
                headers);

        assertTrue(result.contains("Dumping request info:"));
        assertTrue(result.contains("> GET /api/books?page=1&size=10 HTTP/1.1"));
        assertTrue(result.contains("> Host: localhost:8080"));
        assertTrue(result.contains("> Accept: application/json"));
    }

    @Test
    void formatRequest_withoutQueryString() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.HOST, "localhost:8080");

        String result = HttpHeaderFormatter.formatRequest(
                "Dumping request info:",
                "POST",
                "/api/books",
                null,
                headers);

        assertTrue(result.contains("Dumping request info:"));
        assertTrue(result.contains("> POST /api/books HTTP/1.1"));
        assertTrue(result.contains("> Host: localhost:8080"));
    }

    @Test
    void formatRequest_withoutMessage() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.ACCEPT, "text/plain");

        String result = HttpHeaderFormatter.formatRequest(
                null,
                "DELETE",
                "/api/books/123",
                null,
                headers);

        assertEquals("> DELETE /api/books/123 HTTP/1.1\n> Accept: text/plain", result);
    }

    @Test
    void formatResponse_withMessage() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.CONTENT_LENGTH, "150");

        String result = HttpHeaderFormatter.formatResponse(
                "Dumping response info:",
                "200 OK",
                headers);

        assertTrue(result.contains("Dumping response info:"));
        assertTrue(result.contains("< HTTP/1.1 200 OK"));
        assertTrue(result.contains("< Content-Type: application/json"));
        assertTrue(result.contains("< Content-Length: 150"));
    }

    @Test
    void formatResponse_withoutMessage() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.LOCATION, "/api/books/123");

        String result = HttpHeaderFormatter.formatResponse(
                null,
                "201 CREATED",
                headers);

        assertEquals("< HTTP/1.1 201 CREATED\n< Location: /api/books/123", result);
    }

    @Test
    void formatRequestHeaders_multipleValues() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add(HttpHeaders.ACCEPT, "application/xml");

        String result = HttpHeaderFormatter.formatRequestHeaders(headers);

        assertEquals("> Accept: application/json, application/xml", result);
    }

    @Test
    void formatResponseHeaders_multipleHeaders() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.SET_COOKIE, "session=abc123");
        headers.add(HttpHeaders.SET_COOKIE, "user=johndoe");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");

        String result = HttpHeaderFormatter.formatResponseHeaders(headers);

        assertTrue(result.contains("< Set-Cookie: session=abc123, user=johndoe"));
        assertTrue(result.contains("< Cache-Control: no-cache"));
    }

    @Test
    void formatRequestHeaders_emptyHeaders() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

        String result = HttpHeaderFormatter.formatRequestHeaders(headers);

        assertEquals("", result);
    }
}