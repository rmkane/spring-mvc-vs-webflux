package org.acme.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;

import com.fasterxml.jackson.core.JsonProcessingException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.test.reactive.ReactiveIntegrationTestSuite;

@Tag("integration")
public class BookControllerIntegrationTest extends ReactiveIntegrationTestSuite {

    private final HttpHeaders headers = getDefaultHeaders();

    public BookControllerIntegrationTest() {
        super(8081);
    }

    @Test
    void testGetBook() {
        Mono<BookResponse> responseMono = get("/api/books/1", BookResponse.class, headers);

        StepVerifier.create(responseMono)
                .assertNext(book -> {
                    assertNotNull(book);
                    assertEquals("The Great Gatsby", book.getTitle());
                    assertEquals("F. Scott Fitzgerald", book.getAuthor());
                    assertEquals("978-0-7432-7356-5", book.getIsbn());
                    assertEquals(1925, book.getPublicationYear());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testCreateBook() throws JsonProcessingException, IOException {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Test Book")
                .author("Test Author")
                .isbn("1234567890")
                .publicationYear(2024)
                .build();

        // First attempt: may succeed (201) if book doesn't exist, or fail (400) if it
        // already exists
        Mono<String> firstResponseMono = postString("/api/books", request, headers);

        StepVerifier.create(firstResponseMono)
                .assertNext(responseBody -> {
                    try {
                        // Try to parse as ProblemDetail first to check if it's an error
                        try {
                            ProblemDetail problemDetail = fromJson(responseBody, ProblemDetail.class);
                            // If it's a ProblemDetail, check the title
                            if ("Book Already Exists".equals(problemDetail.getTitle())) {
                                // Expected 400 - book already exists
                                assertNotNull(problemDetail.getDetail());
                                return;
                            } else if ("Internal Server Error".equals(problemDetail.getTitle())) {
                                // Unexpected 500 error - log and fail
                                throw new AssertionError("Unexpected 500 error: " + problemDetail.getDetail());
                            }
                        } catch (JsonProcessingException e) {
                            // Not a ProblemDetail, try BookResponse
                        }

                        // Try to parse as BookResponse (201 CREATED)
                        BookResponse bookResponse = fromJson(responseBody, BookResponse.class);
                        assertNotNull(bookResponse);
                        assertEquals("Test Book", bookResponse.getTitle());
                        assertEquals("Test Author", bookResponse.getAuthor());
                        assertEquals("1234567890", bookResponse.getIsbn());
                    } catch (JsonProcessingException e) {
                        throw new AssertionError("Failed to parse response: " + responseBody, e);
                    }
                })
                .expectComplete()
                .verify();

        // Second attempt: should always fail with 400 BAD_REQUEST (duplicate ISBN)
        Mono<String> secondResponseMono = postString("/api/books", request, headers);

        StepVerifier.create(secondResponseMono)
                .assertNext(responseBody -> {
                    try {
                        ProblemDetail problemDetail = fromJson(responseBody, ProblemDetail.class);
                        assertNotNull(problemDetail);
                        assertEquals("Book Already Exists", problemDetail.getTitle());
                        assertNotNull(problemDetail.getDetail());
                    } catch (JsonProcessingException e) {
                        throw new AssertionError("Failed to parse ProblemDetail: " + responseBody, e);
                    }
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testCreateBookWithDuplicateIsbn() throws JsonProcessingException {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Duplicate Book")
                .author("Duplicate Author")
                .isbn("978-0-7432-7356-5") // Using existing ISBN from seed data
                .publicationYear(2024)
                .build();

        Mono<String> responseMono = postString("/api/books", request, headers);

        StepVerifier.create(responseMono)
                .assertNext(responseBody -> {
                    try {
                        ProblemDetail problemDetail = fromJson(responseBody, ProblemDetail.class);
                        assertNotNull(problemDetail);
                        assertEquals("Book Already Exists", problemDetail.getTitle());
                        assertNotNull(problemDetail.getDetail());
                    } catch (JsonProcessingException e) {
                        throw new AssertionError("Failed to parse ProblemDetail: " + responseBody, e);
                    }
                })
                .expectComplete()
                .verify();
    }
}
