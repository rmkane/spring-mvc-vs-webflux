package org.acme.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.acme.test.request.RequestHeadersBuilder;
import org.acme.test.request.RestFetcher;
import org.acme.test.request.RestRequest;
import org.acme.test.request.RestRequestBuilder;
import org.acme.test.util.JsonFormatter;
import org.acme.test.util.ResourceLoader;
import org.acme.test.util.ResponseWriter;

/**
 * Base class for integration tests providing utilities for making HTTP requests
 * and assertions. Subclasses should extend this class to write integration
 * tests.
 */
public abstract class IntegrationTestSuite {

    // TODO: Should be a system property or environment variable
    private static final String DEFAULT_DN = "cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org";

    private static final int DEFAULT_PORT = 8080;
    private static final String PROTOCOL_HTTP = "http";
    private static final String PORT_PROPERTY = "test.server.port";
    private static final String PORT_ENV_VAR = "TEST_SERVER_PORT";

    /** REST client for making HTTP requests. */
    protected static RestFetcher restFetcher;
    /** JSON object mapper for serialization/deserialization. */
    protected static ObjectMapper objectMapper;
    /** XML mapper for XML processing. */
    protected static XmlMapper xmlMapper;
    /** XSLT transformer for XML transformations. */
    protected static Transformer transformer;

    private final int port;

    /**
     * Default constructor that resolves the port from system properties or
     * environment variables. Falls back to the default port (8080) if not
     * specified.
     */
    public IntegrationTestSuite() {
        this(resolvePort());
    }

    /**
     * Constructor that allows setting a specific port manually.
     *
     * @param port The port number (must be between 1 and 65535)
     * @throws IllegalArgumentException if the port is out of valid range
     */
    public IntegrationTestSuite(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        this.port = port;
    }

    @BeforeAll
    static void setUp() {
        restFetcher = new RestFetcher(new RestTemplate());

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer();
            // Configure for pretty printing
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            // Set indent amount (works with Xalan and Saxon)
            try {
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            } catch (IllegalArgumentException e) {
                // Ignore if not supported (some transformers don't support this property)
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create XML transformer", e);
        }
    }

    /**
     * Returns the base URL for the test server. Can be overridden by subclasses.
     *
     * @return The base URL as a string
     */
    protected String getBaseUrl() {
        return String.format("%s://localhost:%d", getProtocol(), getPort());
    }

    /**
     * Returns the protocol (http or https). Can be overridden by subclasses.
     *
     * @return The protocol string (http or https)
     */
    protected String getProtocol() {
        return PROTOCOL_HTTP;
    }

    /**
     * Returns the configured port. Can be overridden by subclasses.
     *
     * @return The port number
     */
    protected int getPort() {
        return port;
    }

    /**
     * Convenience method to create a RestRequestBuilder with the base URL and
     * endpoint.
     *
     * @param endpoint The API endpoint (e.g., "/api/v1/greeting")
     * @return A RestRequestBuilder configured with the base URL and endpoint
     */
    protected RestRequestBuilder request(String endpoint) {
        return RestRequestBuilder.create(getBaseUrl(), endpoint);
    }

    /**
     * Convenience method for GET requests.
     *
     * @param endpoint The API endpoint
     * @return A RestRequestBuilder configured for GET requests
     */
    protected RestRequestBuilder get(String endpoint) {
        return request(endpoint).method(HttpMethod.GET);
    }

    /**
     * Convenience method for POST requests.
     *
     * @param endpoint The API endpoint
     * @return A RestRequestBuilder configured for POST requests
     */
    protected RestRequestBuilder post(String endpoint) {
        return request(endpoint).method(HttpMethod.POST);
    }

    /**
     * Convenience method for PUT requests.
     *
     * @param endpoint The API endpoint
     * @return A RestRequestBuilder configured for PUT requests
     */
    protected RestRequestBuilder put(String endpoint) {
        return request(endpoint).method(HttpMethod.PUT);
    }

    /**
     * Convenience method for DELETE requests.
     *
     * @param endpoint The API endpoint
     * @return A RestRequestBuilder configured for DELETE requests
     */
    protected RestRequestBuilder delete(String endpoint) {
        return request(endpoint).method(HttpMethod.DELETE);
    }

    /**
     * Fetches a response from the REST API.
     *
     * @param <T>          The response type
     * @param request      The REST request
     * @param responseType The response type class
     * @return The response entity
     */
    protected <T> ResponseEntity<T> fetch(RestRequest request, @NonNull Class<T> responseType) {
        return restFetcher.fetch(request, responseType);
    }

    /**
     * Fetches a response from the REST API with a parameterized type reference.
     *
     * @param <T>          The response type
     * @param request      The REST request
     * @param responseType The parameterized type reference
     * @return The response entity
     */
    protected <T> ResponseEntity<T> fetch(
            RestRequest request, @NonNull ParameterizedTypeReference<T> responseType) {
        return restFetcher.fetch(request, responseType);
    }

    /**
     * Parses a JSON response body into a Map. Useful for testing JSON responses.
     *
     * @param response The ResponseEntity containing the JSON response
     * @return A Map representation of the JSON body
     * @throws IOException if the JSON cannot be parsed
     */
    protected Map<String, Object> parseJsonResponse(ResponseEntity<String> response) throws IOException {
        return objectMapper.readValue(response.getBody(), new TypeReference<>() {
        });
    }

    /**
     * Converts an object to a JSON string. Useful for creating request bodies.
     *
     * @param object The object to convert to JSON
     * @return A JSON string representation of the object
     * @throws JsonProcessingException if the object cannot be serialized
     */
    protected String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Parses a JSON string into an object of the specified type.
     *
     * @param <T>   The type to deserialize to
     * @param json  The JSON string to parse
     * @param clazz The class of the type to deserialize to
     * @return The deserialized object
     * @throws JsonProcessingException if the JSON cannot be parsed
     */
    protected <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(json, clazz);
    }

    /**
     * Formats an object as JSON with pretty printing. Uses ObjectMapper to
     * serialize the object first.
     *
     * @param value The object to serialize and format as JSON
     * @return Formatted JSON string with indentation
     * @throws JsonProcessingException if the object cannot be serialized
     */
    protected String formatJson(Object value) throws JsonProcessingException {
        // Serialize object to JSON string using ObjectMapper
        String jsonString = objectMapper.writeValueAsString(value);
        // Format the JSON string with pretty printing
        return formatJsonString(jsonString);
    }

    /**
     * Formats a JSON string with pretty printing (indentation).
     *
     * @param json The JSON string to format
     * @return Formatted JSON string with indentation
     * @throws JsonProcessingException if the JSON is invalid
     */
    protected String formatJson(String json) throws JsonProcessingException {
        return formatJsonString(json);
    }

    /**
     * Internal helper method that performs the actual JSON formatting with pretty
     * printing.
     *
     * @param jsonString The JSON string to format
     * @return Formatted JSON string with indentation
     * @throws JsonProcessingException if the JSON is invalid
     */
    private String formatJsonString(String jsonString) throws JsonProcessingException {
        return JsonFormatter.format(objectMapper, jsonString);
    }

    /**
     * Asserts that the response has the expected status code.
     *
     * @param response       The response to check
     * @param expectedStatus The expected HTTP status
     * @throws AssertionError if the status codes don't match
     */
    protected void assertStatus(ResponseEntity<?> response, HttpStatus expectedStatus) {
        assertEquals(expectedStatus, response.getStatusCode(), "Response status code mismatch");
    }

    /**
     * Asserts that the response has status 200 OK.
     *
     * @param response The response to check
     * @throws AssertionError if the status is not OK
     */
    protected void assertOk(ResponseEntity<?> response) {
        assertStatus(response, HttpStatus.OK);
    }

    /**
     * Asserts that a JSON response contains a specific value at a given path.
     *
     * @param response      The JSON response
     * @param jsonPath      The JSON path (e.g., "query.term")
     * @param expectedValue The expected value
     * @throws IOException    if the JSON cannot be parsed
     * @throws AssertionError if the value doesn't match
     */
    protected void assertJsonPath(
            ResponseEntity<String> response, String jsonPath, Object expectedValue) throws IOException {
        Map<String, Object> json = parseJsonResponse(response);
        Object actualValue = getJsonPathValue(json, jsonPath);
        assertEquals(expectedValue, actualValue, "JSON path '" + jsonPath + "' mismatch");
    }

    private Object getJsonPathValue(Map<String, Object> json, String jsonPath) {
        String[] parts = jsonPath.split("\\.");
        Object current = json;
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * Returns the default Distinguished Name (DN) for requests. Can be overridden
     * by subclasses to provide custom authentication.
     *
     * @return The default DN string
     */
    protected String getDefaultDn() {
        return DEFAULT_DN;
    }

    /**
     * Returns the default HTTP headers for requests, including the x-dn header.
     *
     * @return The default HTTP headers
     */
    protected HttpHeaders getDefaultHeaders() {
        return RequestHeadersBuilder.create()
                .addHeader("x-dn", getDefaultDn())
                .build();
    }

    /**
     * Loads a resource file as a byte array.
     *
     * @param fileName The name of the resource file
     * @return The resource content as bytes
     * @throws IOException if the resource cannot be loaded
     */
    protected byte[] loadResource(String fileName) throws IOException {
        return ResourceLoader.loadBytes(getClass(), fileName);
    }

    /**
     * Loads a resource file as a string.
     *
     * @param fileName The name of the resource file
     * @return The resource content as a string
     * @throws IOException if the resource cannot be loaded
     */
    protected String loadResourceAsString(String fileName) throws IOException {
        return ResourceLoader.loadString(getClass(), fileName);
    }

    /**
     * Writes a JSON response string to a file.
     *
     * @param response The JSON response string
     * @param fileName The output file name
     * @throws IOException if the file cannot be written
     */
    protected void writeJsonResponse(String response, String fileName) throws IOException {
        ResponseWriter.writeJson(objectMapper, response, fileName);
    }

    /**
     * Writes a JSON response object to a file.
     *
     * @param response The response object to serialize as JSON
     * @param fileName The output file name
     * @throws IOException if the file cannot be written
     */
    protected void writeJsonResponse(Object response, String fileName) throws IOException {
        ResponseWriter.writeJson(objectMapper, response, fileName);
    }

    /**
     * Writes a response string to a file.
     *
     * @param response The response string
     * @param fileName The output file name
     * @throws IOException if the file cannot be written
     */
    protected void writeResponse(String response, String fileName) throws IOException {
        ResponseWriter.write(response, fileName);
    }

    /**
     * Formats an object as XML with pretty printing. Uses XmlMapper to serialize
     * the object first.
     *
     * @param value The object to serialize and format as XML
     * @return Formatted XML string with indentation
     * @throws JsonProcessingException if the object cannot be serialized
     * @throws TransformerException    if the XML cannot be formatted
     */
    protected String formatXml(Object value) throws JsonProcessingException, TransformerException {
        // Serialize object to XML string using XmlMapper
        String xmlString = xmlMapper.writeValueAsString(value);
        // Format the XML string with pretty printing
        return formatXmlString(xmlString);
    }

    /**
     * Formats an XML string with pretty printing (indentation).
     *
     * @param xml The XML string to format
     * @return Formatted XML string with indentation
     * @throws TransformerException if the XML cannot be transformed
     */
    protected String formatXml(String xml) throws TransformerException {
        return formatXmlString(xml);
    }

    /**
     * Internal helper method that performs the actual XML formatting with pretty
     * printing.
     *
     * @param xmlString The XML string to format
     * @return Formatted XML string with indentation
     * @throws TransformerException if the XML cannot be transformed
     */
    private String formatXmlString(String xmlString) throws TransformerException {
        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xmlString)), new StreamResult(writer));
        return writer.toString();
    }

    private static int resolvePort() {
        // Check system property first
        int portProperty = getSystemProperty(PORT_PROPERTY, DEFAULT_PORT);
        if (portProperty != DEFAULT_PORT) {
            return portProperty;
        }

        // Check environment variable
        int portEnv = getEnvironmentVariable(PORT_ENV_VAR, DEFAULT_PORT);
        if (portEnv != DEFAULT_PORT) {
            return portEnv;
        }

        return DEFAULT_PORT;
    }

    private static int getEnvironmentVariable(String variableName, int defaultValue) {
        String value = System.getenv(variableName);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid port value in environment variable '" + variableName + "': " + value, e);
            }
        }
        return defaultValue;
    }

    private static int getSystemProperty(String propertyName, int defaultValue) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid port value in system property '" + propertyName + "': " + value, e);
            }
        }
        return defaultValue;
    }
}
