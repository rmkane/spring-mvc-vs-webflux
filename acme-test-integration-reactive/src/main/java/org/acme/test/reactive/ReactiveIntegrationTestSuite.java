package org.acme.test.reactive;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Base class for reactive integration tests using WebClient. Provides common
 * utilities for making HTTP requests to WebFlux endpoints.
 */
public abstract class ReactiveIntegrationTestSuite {

    // TODO: Should be a system property or environment variable
    private static final String DEFAULT_DN = "cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org";

    private static final int DEFAULT_PORT = 8081;
    private static final String PROTOCOL_HTTP = "http";
    private static final String PORT_PROPERTY = "test.server.port";
    private static final String PORT_ENV_VAR = "TEST_SERVER_PORT";

    protected static WebClient webClient;
    protected static ObjectMapper objectMapper;

    private final int port;

    /**
     * Default constructor that resolves the port from system properties or
     * environment variables. Falls back to the default port (8081) if not
     * specified.
     */
    public ReactiveIntegrationTestSuite() {
        this(resolvePort());
    }

    /**
     * Constructor that allows setting a specific port manually.
     *
     * @param port The port number (must be between 1 and 65535)
     * @throws IllegalArgumentException if the port is out of valid range
     */
    public ReactiveIntegrationTestSuite(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        this.port = port;
    }

    @BeforeAll
    static void setUp() {
        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + DEFAULT_PORT)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static int resolvePort() {
        // Check system property first
        String portProperty = System.getProperty(PORT_PROPERTY);
        if (portProperty != null && !portProperty.isEmpty()) {
            return Integer.parseInt(portProperty);
        }

        // Check environment variable
        String portEnvVar = System.getenv(PORT_ENV_VAR);
        if (portEnvVar != null && !portEnvVar.isEmpty()) {
            return Integer.parseInt(portEnvVar);
        }

        return DEFAULT_PORT;
    }

    protected String getBaseUrl() {
        return String.format("%s://localhost:%d", getProtocol(), getPort());
    }

    protected String getProtocol() {
        return PROTOCOL_HTTP;
    }

    protected int getPort() {
        return port;
    }

    protected WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl(getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    protected HttpHeaders getDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-dn", DEFAULT_DN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Converts an object to a JSON string.
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
     * Performs a GET request and returns the response body as a Mono.
     *
     * @param <T>          The response type
     * @param endpoint     The endpoint path
     * @param responseType The class of the response type
     * @param headers      Optional headers (if null, default headers are used)
     * @return A Mono containing the response body
     */
    protected <T> Mono<T> get(String endpoint, Class<T> responseType, HttpHeaders headers) {
        WebClient.RequestHeadersSpec<?> spec = getWebClient()
                .get()
                .uri(endpoint);

        if (headers != null) {
            spec.headers(httpHeaders -> httpHeaders.addAll(headers));
        } else {
            spec.headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()));
        }

        return spec.retrieve()
                .bodyToMono(responseType);
    }

    /**
     * Performs a GET request with default headers.
     */
    protected <T> Mono<T> get(String endpoint, Class<T> responseType) {
        return get(endpoint, responseType, null);
    }

    /**
     * Performs a POST request and returns the response body as a Mono.
     *
     * @param <T>          The response type
     * @param endpoint     The endpoint path
     * @param requestBody  The request body object
     * @param responseType The class of the response type
     * @param headers      Optional headers (if null, default headers are used)
     * @return A Mono containing the response body
     */
    protected <T> Mono<T> post(String endpoint, Object requestBody, Class<T> responseType, HttpHeaders headers)
            throws JsonProcessingException {
        WebClient.RequestBodySpec bodySpec = getWebClient()
                .post()
                .uri(endpoint);

        WebClient.RequestHeadersSpec<?> headersSpec = bodySpec.body(BodyInserters.fromValue(toJson(requestBody)));

        if (headers != null) {
            headersSpec = headersSpec.headers(httpHeaders -> httpHeaders.addAll(headers));
        } else {
            headersSpec = headersSpec.headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()));
        }

        return headersSpec.retrieve()
                .bodyToMono(responseType);
    }

    /**
     * Performs a POST request with default headers.
     */
    protected <T> Mono<T> post(String endpoint, Object requestBody, Class<T> responseType)
            throws JsonProcessingException {
        return post(endpoint, requestBody, responseType, null);
    }

    /**
     * Performs a POST request and returns the raw response body as a String. Useful
     * for handling different response types (e.g., ProblemDetail vs. BookResponse).
     *
     * @param endpoint    The endpoint path
     * @param requestBody The request body object
     * @param headers     Optional headers (if null, default headers are used)
     * @return A Mono containing the response body as String
     */
    protected Mono<String> postString(String endpoint, Object requestBody, HttpHeaders headers)
            throws JsonProcessingException {
        WebClient.RequestBodySpec bodySpec = getWebClient()
                .post()
                .uri(endpoint);

        WebClient.RequestHeadersSpec<?> headersSpec = bodySpec.body(BodyInserters.fromValue(toJson(requestBody)));

        if (headers != null) {
            headersSpec = headersSpec.headers(httpHeaders -> httpHeaders.addAll(headers));
        } else {
            headersSpec = headersSpec.headers(httpHeaders -> httpHeaders.addAll(getDefaultHeaders()));
        }

        return headersSpec.retrieve()
                .bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    // Extract response body from exception for error responses
                    String responseBody = ex.getResponseBodyAsString();
                    if (responseBody != null && !responseBody.isEmpty()) {
                        return Mono.just(responseBody);
                    }
                    return Mono.error(ex);
                });
    }

    /**
     * Performs a POST request with default headers and returns String.
     */
    protected Mono<String> postString(String endpoint, Object requestBody) throws JsonProcessingException {
        return postString(endpoint, requestBody, null);
    }

    /**
     * Asserts that a Mono emits a value with the expected status. For error
     * responses, this will verify the status code from WebClientResponseException.
     *
     * @param mono           The Mono to verify
     * @param expectedStatus The expected HTTP status
     */
    protected void assertStatus(Mono<?> mono, HttpStatus expectedStatus) {
        StepVerifier.create(mono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof WebClientResponseException ex) {
                        return ex.getStatusCode() == expectedStatus;
                    }
                    return false;
                })
                .verify();
    }

    /**
     * Asserts that a Mono completes successfully (status 200 OK).
     *
     * @param mono The Mono to verify
     */
    protected void assertOk(Mono<?> mono) {
        StepVerifier.create(mono)
                .expectNextCount(1)
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }
}
