package org.acme.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.acme.test.request.RestRequestBuilder;

/** Test class to verify IntegrationTestSuite helper methods work correctly. */
class IntegrationTestSuiteTest extends IntegrationTestSuite {
    @Test
    void testGetBaseUrl() {
        String baseUrl = getBaseUrl();
        assertNotNull(baseUrl);
        assertTrue(baseUrl.startsWith("http://"));
        assertTrue(baseUrl.contains("localhost"));
    }

    @Test
    void testGetProtocol() {
        assertEquals("http", getProtocol());
    }

    @Test
    void testGetPort() {
        int port = getPort();
        assertTrue(port > 0 && port < 65536);
    }

    @Test
    void testRequest() {
        RestRequestBuilder builder = request("/api/test");
        assertNotNull(builder);
    }

    @Test
    void testGet() {
        RestRequestBuilder builder = get("/api/test");
        assertNotNull(builder);
        assertEquals(HttpMethod.GET, builder.build().getMethod());
    }

    @Test
    void testPost() {
        RestRequestBuilder builder = post("/api/test");
        assertNotNull(builder);
        assertEquals(HttpMethod.POST, builder.build().getMethod());
    }

    @Test
    void testPut() {
        RestRequestBuilder builder = put("/api/test");
        assertNotNull(builder);
        assertEquals(HttpMethod.PUT, builder.build().getMethod());
    }

    @Test
    void testDelete() {
        RestRequestBuilder builder = delete("/api/test");
        assertNotNull(builder);
        assertEquals(HttpMethod.DELETE, builder.build().getMethod());
    }

    @Test
    void testToJson() throws JsonProcessingException {
        TestObject obj = new TestObject("test", 123);
        String json = toJson(obj);
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }

    @Test
    void testParseJsonResponse() throws IOException {
        String jsonBody = "{\"name\":\"test\",\"value\":123}";
        ResponseEntity<String> response = new ResponseEntity<>(jsonBody, HttpStatus.OK);

        Map<String, Object> parsed = parseJsonResponse(response);
        assertNotNull(parsed);
        assertEquals("test", parsed.get("name"));
        assertEquals(123, parsed.get("value"));
    }

    @Test
    void testAssertStatus() {
        ResponseEntity<String> response = new ResponseEntity<>("body", HttpStatus.OK);
        assertStatus(response, HttpStatus.OK);
    }

    @Test
    void testAssertOk() {
        ResponseEntity<String> response = new ResponseEntity<>("body", HttpStatus.OK);
        assertOk(response);
    }

    @Test
    void testAssertOkFailure() {
        ResponseEntity<String> response = new ResponseEntity<>("body", HttpStatus.NOT_FOUND);
        assertThrows(AssertionError.class, () -> assertOk(response));
    }

    @Test
    void testAssertJsonPath() throws IOException {
        String jsonBody = "{\"user\":{\"name\":\"John\",\"age\":30}}";
        ResponseEntity<String> response = new ResponseEntity<>(jsonBody, HttpStatus.OK);

        assertJsonPath(response, "user.name", "John");
        assertJsonPath(response, "user.age", 30);
    }

    @Test
    void testGetDefaultHeaders() {
        HttpHeaders headers = getDefaultHeaders();
        assertNotNull(headers);
        assertEquals("cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org", headers.getFirst("x-dn"));
    }

    @Test
    void testFormatJsonObject() throws JsonProcessingException {
        TestObject obj = new TestObject("test", 123);
        String formatted = formatJson(obj);
        assertNotNull(formatted);
        assertTrue(formatted.contains("test"));
        assertTrue(formatted.contains("123"));
        // Should be pretty printed (contain newlines)
        assertTrue(formatted.contains("\n"));
    }

    @Test
    void testFormatJsonString() throws JsonProcessingException {
        String json = "{\"name\":\"test\",\"value\":123}";
        String formatted = formatJson(json);
        assertNotNull(formatted);
        assertTrue(formatted.contains("test"));
        assertTrue(formatted.contains("123"));
        // Should be pretty printed (contain newlines)
        assertTrue(formatted.contains("\n"));
    }

    @Test
    void testFormatXmlObject() throws Exception {
        TestObject obj = new TestObject("test", 123);
        String formatted = formatXml(obj);
        assertNotNull(formatted);
        assertTrue(formatted.contains("test"));
        assertTrue(formatted.contains("123"));
        // Should be pretty printed (contain newlines)
        assertTrue(formatted.contains("\n"));
    }

    @Test
    void testFormatXmlString() throws Exception {
        String xml = "<root><name>test</name><value>123</value></root>";
        String formatted = formatXml(xml);
        assertNotNull(formatted);
        assertTrue(formatted.contains("test"));
        assertTrue(formatted.contains("123"));
        // Should be pretty printed (contain newlines)
        assertTrue(formatted.contains("\n"));
    }

    // Helper class for testing
    record TestObject(String name, int value) {
    }
}
