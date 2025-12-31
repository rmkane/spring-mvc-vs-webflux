# Acme Reactive Integration Test

This module provides a reusable test framework for reactive integration testing using Spring WebFlux's `WebClient`.

## Overview

The `ReactiveIntegrationTestSuite` base class provides utilities for making HTTP requests to WebFlux endpoints using the native reactive HTTP client (`WebClient`). This is the reactive equivalent of the `IntegrationTestSuite` in `acme-test-integration-classic`.

## Key Features

- **Native WebFlux Client**: Uses `WebClient` for reactive HTTP requests
- **Reactive Testing**: Uses `StepVerifier` from `reactor-test` for reactive assertions
- **Error Handling**: Automatically extracts response bodies from error responses (4xx/5xx)
- **JSON Utilities**: Built-in JSON serialization/deserialization with Jackson
- **Configurable Port**: Supports system properties and environment variables for port configuration

## Usage

Extend `ReactiveIntegrationTestSuite` in your test class:

```java
@Tag("integration")
public class BookControllerIntegrationTest extends ReactiveIntegrationTestSuite {

    private final HttpHeaders headers = getDefaultHeaders();

    public BookControllerIntegrationTest() {
        super(8081); // WebFlux API port
    }

    @Test
    void testGetBook() {
        Mono<BookResponse> responseMono = get("/api/books/1", BookResponse.class, headers);

        StepVerifier.create(responseMono)
                .assertNext(book -> {
                    assertNotNull(book);
                    assertEquals("The Great Gatsby", book.getTitle());
                })
                .expectComplete()
                .verify();
    }
}
```

## Available Methods

- `get(endpoint, responseType, headers)` - GET request returning `Mono<T>`
- `post(endpoint, requestBody, responseType, headers)` - POST request returning `Mono<T>`
- `postString(endpoint, requestBody, headers)` - POST request returning `Mono<String>` (for error handling)
- `toJson(object)` - Serialize object to JSON string
- `fromJson(json, clazz)` - Deserialize JSON string to object
- `getDefaultHeaders()` - Get default headers with `x-dn` authentication

## Dependencies

- `spring-webflux` - For `WebClient`
- `reactor-test` - For `StepVerifier` reactive testing
- `jackson-databind` - For JSON processing
- `spring-boot-starter-test` - For JUnit 5

## Comparison with `acme-test-integration-classic`

| Feature | `acme-test-integration-classic` | `acme-test-integration-reactive` |
| - | - | - |
| HTTP Client | `RestTemplate` (blocking) | `WebClient` (reactive) |
| Return Types | `ResponseEntity<T>` | `Mono<T>`, `Flux<T>` |
| Testing | JUnit assertions | `StepVerifier` |
| Use Case | MVC API tests | WebFlux API tests |
