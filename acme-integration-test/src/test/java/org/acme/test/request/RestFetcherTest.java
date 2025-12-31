package org.acme.test.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

class RestFetcherTest {

    private RestTemplate restTemplate;
    private RestFetcher restFetcher;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        restFetcher = new RestFetcher(restTemplate);
    }

    @Test
    void testFetchWithClass() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users")
                .method(HttpMethod.GET)
                .build();

        ResponseEntity<String> expectedResponse = new ResponseEntity<>("response body", HttpStatus.OK);

        when(restTemplate.exchange(
                eq(request.getURI()), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(expectedResponse);

        ResponseEntity<String> response = restFetcher.fetch(request, String.class);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("response body", response.getBody());
        verify(restTemplate)
                .exchange(
                        eq(request.getURI()), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testFetchWithParameterizedTypeReference() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users")
                .method(HttpMethod.GET)
                .build();

        ResponseEntity<java.util.List<String>> expectedResponse = new ResponseEntity<>(
                java.util.List.of("user1", "user2"), HttpStatus.OK);

        when(restTemplate.exchange(
                eq(request.getURI()),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        org.springframework.core.ParameterizedTypeReference<java.util.List<String>> typeRef = new org.springframework.core.ParameterizedTypeReference<java.util.List<String>>() {
        };

        ResponseEntity<java.util.List<String>> response = restFetcher.fetch(request, typeRef);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        java.util.List<String> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    @Test
    void testFetchWithPostAndBody() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users")
                .method(HttpMethod.POST)
                .body("{\"name\":\"test\"}")
                .build();

        ResponseEntity<String> expectedResponse = new ResponseEntity<>("created", HttpStatus.CREATED);

        when(restTemplate.exchange(
                eq(request.getURI()), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(expectedResponse);

        ResponseEntity<String> response = restFetcher.fetch(request, String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("created", response.getBody());
    }

    @Test
    void testFetchHandlesHttpClientErrorException() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users/999")
                .method(HttpMethod.GET)
                .build();

        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.NOT_FOUND, "Not Found", "User not found".getBytes(), null);

        when(restTemplate.exchange(
                eq(request.getURI()), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        ResponseEntity<String> response = restFetcher.fetch(request, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());
    }

    @Test
    void testFetchHandlesHttpServerErrorException() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/users")
                .method(HttpMethod.GET)
                .build();

        HttpServerErrorException exception = new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Server error".getBytes(),
                null);

        when(restTemplate.exchange(
                eq(request.getURI()), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(exception);

        ResponseEntity<String> response = restFetcher.fetch(request, String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Server error", response.getBody());
    }

    @Test
    void testFetchWithMultipart() {
        RestRequest request = RestRequestBuilder.create("http://localhost:8080")
                .endpoint("/api/upload")
                .method(HttpMethod.POST)
                .part("field", "value")
                .build();

        ResponseEntity<String> expectedResponse = new ResponseEntity<>("uploaded", HttpStatus.OK);

        when(restTemplate.exchange(
                eq(request.getURI()), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(expectedResponse);

        ResponseEntity<String> response = restFetcher.fetch(request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("uploaded", response.getBody());
    }

    @Test
    void testDefaultConstructor() {
        RestFetcher fetcher = new RestFetcher();
        assertNotNull(fetcher);
    }
}
