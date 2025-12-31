package org.acme.test.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

class RestRequestBuilderTest {

    @Test
    void testCreateWithBaseUrl() {
        RestRequestBuilder builder = RestRequestBuilder.create("http://localhost:8080");
        RestRequest request = builder.build();

        assertEquals(URI.create("http://localhost:8080"), request.getURI());
        assertEquals(HttpMethod.GET, request.getMethod());
    }

    @Test
    void testCreateWithBaseUrlAndEndpoint() {
        RestRequestBuilder builder = RestRequestBuilder.create("http://localhost:8080", "/api/users");
        RestRequest request = builder.build();

        assertEquals(URI.create("http://localhost:8080/api/users"), request.getURI());
    }

    @Test
    void testMethod() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users")
                .method(HttpMethod.POST)
                .build();

        assertEquals(HttpMethod.POST, request.getMethod());
    }

    @Test
    void testEndpoint() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080").endpoint("/api/users").build();

        assertTrue(request.getURI().toString().endsWith("/api/users"));
    }

    @Test
    void testHeaders() {
        MultiValueMap<String, String> headers = new org.springframework.util.LinkedMultiValueMap<>();
        headers.add("X-Custom-Header", "value1");
        headers.add("X-Another-Header", "value2");

        RestRequest request = RestRequestBuilder.create("http://localhost:8080").headers(headers).build();

        assertEquals("value1", request.getHeaders().getFirst("X-Custom-Header"));
        assertEquals("value2", request.getHeaders().getFirst("X-Another-Header"));
    }

    @Test
    void testAddHeader() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .addHeader("X-Custom-Header", "value")
                .build();

        assertEquals("value", request.getHeaders().getFirst("X-Custom-Header"));
    }

    @Test
    void testQueryParam() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/search")
                .queryParam("q", "test")
                .queryParam("limit", "10")
                .build();

        URI uri = request.getURI();
        assertTrue(uri.getQuery().contains("q=test"));
        assertTrue(uri.getQuery().contains("limit=10"));
    }

    @Test
    void testQueryParams() {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("q", "test");
        params.put("limit", "10");

        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/search")
                .queryParams(params)
                .build();

        URI uri = request.getURI();
        assertTrue(uri.getQuery().contains("q=test"));
        assertTrue(uri.getQuery().contains("limit=10"));
    }

    @Test
    void testPathVar() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users/{id}")
                .pathVar("id", 123)
                .build();

        assertTrue(request.getURI().toString().contains("/api/users/123"));
    }

    @Test
    void testPathVars() {
        java.util.Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("id", 123);
        vars.put("action", "update");

        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users/{id}/{action}")
                .pathVars(vars)
                .build();

        String uri = request.getURI().toString();
        assertTrue(uri.contains("/api/users/123/update"));
    }

    @Test
    void testBodyWithBytes() {
        byte[] body = "test body".getBytes(StandardCharsets.UTF_8);

        RestRequest request = RestRequestBuilder.create("http://localhost:8080").body(body).build();

        assertEquals(body, request.getBody());
    }

    @Test
    void testBodyWithString() {
        String body = "test body";

        RestRequest request = RestRequestBuilder.create("http://localhost:8080").body(body).build();

        assertEquals(body, new String(request.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    void testBearerToken() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080").bearerToken("token123").build();

        assertEquals("Bearer token123", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void testBearerTokenNull() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080").bearerToken(null).build();

        assertNull(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void testBasicAuth() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080").basicAuth("user", "pass").build();

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(auth);
        assertTrue(auth.startsWith("Basic "));
    }

    @Test
    void testBasicAuthNull() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080").basicAuth(null, null).build();

        assertNull(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void testPart() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .part("field1", "value1")
                .part("field2", "value2")
                .build();

        assertTrue(request.isMultipart());
        assertNotNull(request.getMultipartBody());
    }

    @Test
    void testFileWithBytes(@TempDir File tempDir) throws IOException {
        byte[] content = "file content".getBytes(StandardCharsets.UTF_8);

        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .file("file", content, "test.txt")
                .build();

        assertTrue(request.isMultipart());
        assertNotNull(request.getMultipartBody());
    }

    @Test
    void testFileWithBytesAndContentType(@TempDir File tempDir) throws IOException {
        byte[] content = "file content".getBytes(StandardCharsets.UTF_8);

        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .file("file", content, "test.pdf", MediaType.APPLICATION_PDF)
                .build();

        assertTrue(request.isMultipart());
        assertNotNull(request.getMultipartBody());
    }

    @Test
    void testFileWithFileObject(@TempDir File tempDir) throws IOException {
        File testFile = new File(tempDir, "test.txt");
        Files.write(testFile.toPath(), "file content".getBytes(StandardCharsets.UTF_8));

        RestRequest request = RestRequestBuilder.create("http://localhost:8080").file("file", testFile).build();

        assertTrue(request.isMultipart());
        assertNotNull(request.getMultipartBody());
    }

    @Test
    void testFileWithFileObjectAndContentType(@TempDir File tempDir) throws IOException {
        File testFile = new File(tempDir, "test.pdf");
        Files.write(testFile.toPath(), "file content".getBytes(StandardCharsets.UTF_8));

        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .file("file", testFile, MediaType.APPLICATION_PDF)
                .build();

        assertTrue(request.isMultipart());
        assertNotNull(request.getMultipartBody());
    }

    @Test
    void testNullValuesIgnored() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .addHeader(null, null)
                .queryParam(null, null)
                .pathVar(null, null)
                .body((byte[]) null)
                .body((String) null)
                .build();

        assertNotNull(request);
        assertTrue(request.getHeaders().isEmpty());
    }

    @Test
    void testComplexRequest() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users/{id}/posts")
                .method(HttpMethod.POST)
                .pathVar("id", 123)
                .queryParam("sort", "date")
                .addHeader("Content-Type", "application/json")
                .bearerToken("token123")
                .body("{\"title\":\"Test\"}")
                .build();

        assertEquals(HttpMethod.POST, request.getMethod());
        assertTrue(request.getURI().toString().contains("/api/users/123/posts"));
        assertTrue(request.getURI().getQuery().contains("sort=date"));
        assertEquals("application/json", request.getHeaders().getFirst("Content-Type"));
        assertEquals("Bearer token123", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertNotNull(request.getBody());
    }
}
