package org.acme.test.request;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/** Utility to execute {@link RestRequest} using {@link RestTemplate}. */
public final class RestFetcher {
    private final RestTemplate restTemplate;

    /**
     * Creates a RestFetcher with a default RestTemplate.
     */
    public RestFetcher() {
        this(new RestTemplate());
    }

    /**
     * Creates a RestFetcher with the specified RestTemplate.
     *
     * @param restTemplate The RestTemplate to use for HTTP requests
     */
    public RestFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Executes a REST request and returns the response.
     *
     * @param <T>          The response type
     * @param request      The REST request to execute
     * @param responseType The expected response type class
     * @return The response entity
     */
    public <T> ResponseEntity<T> fetch(RestRequest request, @NonNull Class<T> responseType) {
        return executeRequest(request, responseType);
    }

    /**
     * Executes a REST request with a parameterized type reference and returns the
     * response.
     *
     * @param <T>          The response type
     * @param request      The REST request to execute
     * @param responseType The parameterized type reference
     * @return The response entity
     */
    public <T> ResponseEntity<T> fetch(
            RestRequest request, @NonNull ParameterizedTypeReference<T> responseType) {
        return executeRequest(request, responseType);
    }

    private <T> ResponseEntity<T> executeRequest(RestRequest request, Object responseType) {
        try {
            HttpEntity<?> entity = createHttpEntity(request);

            if (responseType instanceof ParameterizedTypeReference<?>) {
                @SuppressWarnings("unchecked")
                ParameterizedTypeReference<T> typeRef = (ParameterizedTypeReference<T>) responseType;
                return restTemplate.exchange(request.getURI(), request.getMethod(), entity, typeRef);
            }

            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) responseType;
            @SuppressWarnings("null")
            ResponseEntity<T> result = restTemplate.exchange(request.getURI(), request.getMethod(), entity, clazz);
            return result;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return createErrorResponse(e);
        }
    }

    private HttpEntity<?> createHttpEntity(RestRequest request) {
        return request.isMultipart()
                ? new HttpEntity<>(request.getMultipartBody(), request.getHeaders())
                : new HttpEntity<>(request.getBody(), request.getHeaders());
    }

    @SuppressWarnings("null")
    private <T> ResponseEntity<T> createErrorResponse(Exception e) {
        @SuppressWarnings("unchecked")
        T body = (T) getResponseBodyAsString(e);
        return ResponseEntity.status(getStatusCode(e)).headers(getResponseHeaders(e)).body(body);
    }

    private String getResponseBodyAsString(Exception e) {
        if (e instanceof HttpClientErrorException httpClientErrorException) {
            return httpClientErrorException.getResponseBodyAsString();
        } else if (e instanceof HttpServerErrorException httpServerErrorException) {
            return httpServerErrorException.getResponseBodyAsString();
        }
        return null;
    }

    private HttpStatusCode getStatusCode(Exception e) {
        if (e instanceof HttpClientErrorException httpClientErrorException) {
            return httpClientErrorException.getStatusCode();
        } else if (e instanceof HttpServerErrorException httpServerErrorException) {
            return httpServerErrorException.getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private HttpHeaders getResponseHeaders(Exception e) {
        if (e instanceof HttpClientErrorException httpClientErrorException) {
            return httpClientErrorException.getResponseHeaders();
        } else if (e instanceof HttpServerErrorException httpServerErrorException) {
            return httpServerErrorException.getResponseHeaders();
        }
        return null;
    }
}
