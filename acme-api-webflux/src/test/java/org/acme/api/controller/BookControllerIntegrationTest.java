package org.acme.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import com.fasterxml.jackson.core.JsonProcessingException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.api.model.UpdateBookRequest;
import org.acme.test.reactive.ReactiveIntegrationTestSuite;

@Tag("integration")
public class BookControllerIntegrationTest extends ReactiveIntegrationTestSuite {

    public BookControllerIntegrationTest() {
        super(8081);
    }

    @Test
    void testGetBook() {
        Mono<BookResponse> responseMono = getRequest("/api/v1/books/1")
                .retrieve(BookResponse.class);

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
    void testCreateBook() throws IOException {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("Test Book")
                .author("Test Author")
                .isbn("1234567890")
                .publicationYear(2024)
                .build();

        // First attempt: may succeed (201) if book doesn't exist, or fail (400) if it
        // already exists
        Mono<String> firstResponseMono = postRequest("/api/v1/books")
                .body(request)
                .retrieveString();

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
        Mono<String> secondResponseMono = postRequest("/api/v1/books")
                .body(request)
                .retrieveString();

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

        Mono<String> responseMono = postRequest("/api/v1/books")
                .body(request)
                .retrieveString();

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

    @Test
    void testFullCrudLifecycle() throws JsonProcessingException {
        // Use a unique ISBN to avoid conflicts with other test runs
        String uniqueIsbn = "978-0-TEST-" + System.currentTimeMillis();

        // 1. CREATE - Create a new book and capture the ID
        final Long[] bookIdHolder = new Long[1];

        CreateBookRequest createRequest = CreateBookRequest.builder()
                .title("Full CRUD Test Book")
                .author("Test Author")
                .isbn(uniqueIsbn)
                .publicationYear(2024)
                .build();

        Mono<BookResponse> createMono = postRequest("/api/v1/books")
                .body(createRequest)
                .retrieve(BookResponse.class)
                .doOnNext(book -> bookIdHolder[0] = book.getId());

        StepVerifier.create(createMono)
                .assertNext(book -> {
                    assertNotNull(book);
                    assertNotNull(book.getId());
                    assertEquals("Full CRUD Test Book", book.getTitle());
                    assertEquals("Test Author", book.getAuthor());
                    assertEquals(uniqueIsbn, book.getIsbn());
                    assertEquals(2024, book.getPublicationYear());
                })
                .expectComplete()
                .verify();

        Long bookId = bookIdHolder[0];
        assertNotNull(bookId);

        // 2. UPDATE - Update the book with new data
        UpdateBookRequest updateRequest = UpdateBookRequest.builder()
                .title("Updated CRUD Test Book")
                .author("Updated Test Author")
                .isbn(uniqueIsbn) // Keep same ISBN
                .publicationYear(2025)
                .build();

        Mono<BookResponse> updateMono = putRequest("/api/v1/books/" + bookId)
                .body(updateRequest)
                .retrieve(BookResponse.class);

        StepVerifier.create(updateMono)
                .assertNext(book -> {
                    assertNotNull(book);
                    assertEquals(bookId, book.getId());
                    assertEquals("Updated CRUD Test Book", book.getTitle());
                    assertEquals("Updated Test Author", book.getAuthor());
                    assertEquals(uniqueIsbn, book.getIsbn());
                    assertEquals(2025, book.getPublicationYear());
                })
                .expectComplete()
                .verify();

        // 3. DELETE - Delete the book
        Mono<Void> deleteMono = deleteRequest("/api/v1/books/" + bookId)
                .retrieve(Void.class);

        StepVerifier.create(deleteMono)
                .expectComplete()
                .verify();

        // 4. VERIFY DELETION - Try to get the deleted book, should get 404
        Mono<String> getMono = getRequest("/api/v1/books/" + bookId)
                .retrieveString();

        StepVerifier.create(getMono)
                .assertNext(responseBody -> {
                    try {
                        ProblemDetail problemDetail = fromJson(responseBody, ProblemDetail.class);
                        assertNotNull(problemDetail);
                        assertEquals("Book Not Found", problemDetail.getTitle());
                    } catch (JsonProcessingException e) {
                        throw new AssertionError("Failed to parse ProblemDetail: " + responseBody, e);
                    }
                })
                .expectComplete()
                .verify();
    }
}
