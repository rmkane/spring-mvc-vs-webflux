package org.acme.test.reactive.request;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/**
 * Fluent builder for reactive HTTP requests in integration tests. Provides a
 * clean API for building and executing WebClient requests.
 */
public final class ReactiveRequestBuilder {

    @NonNull
    private final WebClient webClient;
    @NonNull
    private final ObjectMapper objectMapper;
    @NonNull
    private String baseUrl;
    @Nullable
    private String endpoint;
    @NonNull
    private HttpMethod method = HttpMethod.GET;
    @NonNull
    private final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    @NonNull
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    @NonNull
    private final Map<String, Object> pathVariables = new LinkedHashMap<>();
    @Nullable
    private Object body;

    private ReactiveRequestBuilder(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = Objects.requireNonNull(webClient, "webClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * Creates a new ReactiveRequestBuilder with the specified base URL, WebClient,
     * and ObjectMapper.
     *
     * @param baseUrl      The base URL for the request
     * @param webClient    The WebClient to use
     * @param objectMapper The ObjectMapper for JSON serialization
     * @return A new ReactiveRequestBuilder
     */
    public static ReactiveRequestBuilder create(
            String baseUrl, WebClient webClient, ObjectMapper objectMapper) {
        ReactiveRequestBuilder builder = new ReactiveRequestBuilder(webClient, objectMapper);
        builder.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        return builder;
    }

    /**
     * Creates a new ReactiveRequestBuilder with the specified base URL, endpoint,
     * WebClient, and ObjectMapper.
     *
     * @param baseUrl      The base URL for the request
     * @param endpoint     The API endpoint
     * @param webClient    The WebClient to use
     * @param objectMapper The ObjectMapper for JSON serialization
     * @return A new ReactiveRequestBuilder
     */
    public static ReactiveRequestBuilder create(
            String baseUrl, String endpoint, WebClient webClient, ObjectMapper objectMapper) {
        return create(baseUrl, webClient, objectMapper).endpoint(endpoint);
    }

    /**
     * Sets the HTTP method for the request.
     *
     * @param method The HTTP method
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder method(HttpMethod method) {
        this.method = Objects.requireNonNull(method, "method");
        return this;
    }

    /**
     * Sets the endpoint path for the request.
     *
     * @param endpoint The API endpoint
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder endpoint(String endpoint) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        return this;
    }

    /**
     * Sets the headers for the request.
     *
     * @param headers The headers to set
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder headers(HttpHeaders headers) {
        if (headers != null) {
            this.headers.addAll(headers);
        }
        return this;
    }

    /**
     * Sets the headers for the request.
     *
     * @param headers The headers to set
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder headers(MultiValueMap<String, String> headers) {
        if (headers != null) {
            this.headers.addAll(headers);
        }
        return this;
    }

    /**
     * Adds a header to the request.
     *
     * @param name  The header name
     * @param value The header value
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder header(String name, String value) {
        if (name != null && value != null) {
            this.headers.add(name, value);
        }
        return this;
    }

    /**
     * Sets the Content-Type header.
     *
     * @param mediaType The media type
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder contentType(MediaType mediaType) {
        if (mediaType != null) {
            this.headers.set(HttpHeaders.CONTENT_TYPE, mediaType.toString());
        }
        return this;
    }

    /**
     * Sets the Content-Type header to application/json.
     *
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder contentTypeJson() {
        return contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Sets the Content-Type header to application/xml.
     *
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder contentTypeXml() {
        return contentType(MediaType.APPLICATION_XML);
    }

    /**
     * Sets the Accept header.
     *
     * @param mediaType The media type
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder accept(MediaType mediaType) {
        if (mediaType != null) {
            this.headers.set(HttpHeaders.ACCEPT, mediaType.toString());
        }
        return this;
    }

    /**
     * Sets the Accept header to application/json.
     *
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder acceptJson() {
        return accept(MediaType.APPLICATION_JSON);
    }

    /**
     * Sets the Accept header to application/xml.
     *
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder acceptXml() {
        return accept(MediaType.APPLICATION_XML);
    }

    /**
     * Adds a query parameter to the request.
     *
     * @param name  The query parameter name
     * @param value The query parameter value
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder queryParam(String name, String value) {
        if (name != null && value != null) {
            this.queryParams.put(name, value);
        }
        return this;
    }

    /**
     * Adds multiple query parameters to the request.
     *
     * @param params The query parameters map
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder queryParams(Map<String, String> params) {
        if (params != null) {
            this.queryParams.putAll(params);
        }
        return this;
    }

    /**
     * Adds a path variable for URI template expansion.
     *
     * @param name  The path variable name
     * @param value The path variable value
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder pathVar(String name, Object value) {
        if (name != null && value != null) {
            this.pathVariables.put(name, value);
        }
        return this;
    }

    /**
     * Adds multiple path variables for URI template expansion.
     *
     * @param variables The path variables map
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder pathVars(Map<String, ?> variables) {
        if (variables != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> castVariables = (Map<String, Object>) variables;
            this.pathVariables.putAll(castVariables);
        }
        return this;
    }

    /**
     * Sets the request body.
     *
     * @param body The request body object
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder body(Object body) {
        this.body = body;
        return this;
    }

    /**
     * Sets the request body as a JSON string.
     *
     * @param json The JSON string
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder bodyJson(String json) {
        this.body = json;
        return this;
    }

    /* --------------------- Authentication helpers --------------------- */

    /**
     * Adds a Bearer token to the Authorization header.
     *
     * @param token The bearer token
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder bearerToken(String token) {
        if (token != null) {
            this.headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return this;
    }

    /**
     * Adds Basic authentication to the Authorization header.
     *
     * @param username The username
     * @param password The password
     * @return This builder for method chaining
     */
    public ReactiveRequestBuilder basicAuth(String username, String password) {
        if (username != null && password != null) {
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            this.headers.add(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
        return this;
    }

    /* --------------------- Terminal operations --------------------- */

    /**
     * Builds the request object.
     *
     * @return An immutable ReactiveRequest
     */
    public ReactiveRequest build() {
        String uri = buildUri();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.addAll(this.headers);

        return new ReactiveRequest(
                uri, method, httpHeaders, body, queryParams, pathVariables, webClient, objectMapper);
    }

    /**
     * Executes the request and retrieves the response body as the specified type.
     *
     * @param <T>          The response type
     * @param responseType The class of the response type
     * @return A Mono containing the response body
     */
    public <T> Mono<T> retrieve(Class<T> responseType) {
        return build().retrieve(responseType);
    }

    /**
     * Executes the request and retrieves the response body as a String.
     *
     * @return A Mono containing the response body as String
     */
    public Mono<String> retrieveString() {
        return build().retrieveString();
    }

    /**
     * Executes the request and returns the full WebClient.ResponseSpec for advanced
     * handling.
     *
     * @return A WebClient.ResponseSpec for custom response handling
     */
    public WebClient.ResponseSpec exchange() {
        return build().exchange();
    }

    /* --------------------- Private helpers --------------------- */

    private String buildUri() {
        StringBuilder uri = new StringBuilder(baseUrl);

        if (endpoint != null && !endpoint.isEmpty()) {
            if (!endpoint.startsWith("/")) {
                uri.append("/");
            }
            uri.append(endpoint);
        }

        // Replace path variables
        String uriString = uri.toString();
        for (Map.Entry<String, Object> entry : pathVariables.entrySet()) {
            uriString = uriString.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        // Add query parameters
        if (!queryParams.isEmpty()) {
            uriString += "?";
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) {
                    uriString += "&";
                }
                uriString += entry.getKey() + "=" + entry.getValue();
                first = false;
            }
        }

        return uriString;
    }
}
