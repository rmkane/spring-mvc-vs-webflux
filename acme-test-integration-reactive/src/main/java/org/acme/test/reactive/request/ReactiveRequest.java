package org.acme.test.reactive.request;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

/**
 * Immutable HTTP request for reactive integration tests. Encapsulates all
 * request data and provides execution methods that return Mono.
 */
public final class ReactiveRequest {

    @NonNull
    private final String uri;
    @NonNull
    private final HttpMethod method;
    @NonNull
    private final HttpHeaders headers;
    @Nullable
    private final Object body;
    @NonNull
    private final Map<String, String> queryParams;
    @NonNull
    private final Map<String, Object> pathVariables;
    @NonNull
    private final WebClient webClient;
    @NonNull
    private final ObjectMapper objectMapper;

    ReactiveRequest(
            @NonNull String uri,
            @NonNull HttpMethod method,
            @NonNull HttpHeaders headers,
            @Nullable Object body,
            @NonNull Map<String, String> queryParams,
            @NonNull Map<String, Object> pathVariables,
            @NonNull WebClient webClient,
            @NonNull ObjectMapper objectMapper) {
        this.uri = uri;
        this.method = method;
        this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
        this.body = body;
        this.queryParams = new HashMap<>(queryParams);
        this.pathVariables = new HashMap<>(pathVariables);
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @NonNull
    public HttpMethod getMethod() {
        return method;
    }

    @NonNull
    public String getUri() {
        return uri;
    }

    @NonNull
    public HttpHeaders getHeaders() {
        return headers;
    }

    @Nullable
    public Object getBody() {
        return body;
    }

    @NonNull
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @NonNull
    public Map<String, Object> getPathVariables() {
        return pathVariables;
    }

    /* --------------------- Execution methods --------------------- */

    /**
     * Executes the request and retrieves the response body as the specified type.
     *
     * @param <T>          The response type
     * @param responseType The class of the response type
     * @return A Mono containing the response body
     */
    public <T> Mono<T> retrieve(Class<T> responseType) {
        WebClient.RequestHeadersSpec<?> spec = buildRequest();

        return spec.retrieve()
                .bodyToMono(responseType);
    }

    /**
     * Executes the request and retrieves the response body as a String. Useful for
     * handling different response types (e.g., ProblemDetail vs. success response).
     *
     * @return A Mono containing the response body as String
     */
    public Mono<String> retrieveString() {
        WebClient.RequestHeadersSpec<?> spec = buildRequest();

        return spec.retrieve()
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
     * Executes the request and returns the full WebClient.ResponseSpec for advanced
     * handling.
     *
     * @return A WebClient.ResponseSpec for custom response handling
     */
    public WebClient.ResponseSpec exchange() {
        WebClient.RequestHeadersSpec<?> spec = buildRequest();
        return spec.retrieve();
    }

    /* --------------------- Private helpers --------------------- */

    private WebClient.RequestHeadersSpec<?> buildRequest() {
        // Create a new WebClient with the base URL from the request
        WebClient client = webClient.mutate()
                .baseUrl(extractBaseUrl(uri))
                .build();

        // Build the request based on method
        WebClient.RequestBodyUriSpec requestSpec = client.method(method);

        // Set URI
        WebClient.RequestBodySpec bodySpec = requestSpec.uri(extractPath(uri));

        // Add headers
        WebClient.RequestHeadersSpec<?> headersSpec;
        if (body != null) {
            // Serialize body to JSON if it's not already a String
            String bodyJson;
            if (body instanceof String) {
                bodyJson = (String) body;
            } else {
                try {
                    bodyJson = objectMapper.writeValueAsString(body);
                } catch (JsonProcessingException e) {
                    return bodySpec.headers(h -> h.addAll(headers));
                }
            }
            headersSpec = bodySpec.body(BodyInserters.fromValue(bodyJson));
        } else {
            headersSpec = bodySpec;
        }

        // Add headers
        headersSpec = headersSpec.headers(h -> h.addAll(headers));

        return headersSpec;
    }

    private String extractBaseUrl(String fullUri) {
        // Extract base URL (protocol + host + port)
        int pathStart = fullUri.indexOf('/', fullUri.indexOf("://") + 3);
        if (pathStart == -1) {
            return fullUri;
        }
        return fullUri.substring(0, pathStart);
    }

    private String extractPath(String fullUri) {
        // Extract path from full URI
        int pathStart = fullUri.indexOf('/', fullUri.indexOf("://") + 3);
        if (pathStart == -1) {
            return "/";
        }
        return fullUri.substring(pathStart);
    }
}
