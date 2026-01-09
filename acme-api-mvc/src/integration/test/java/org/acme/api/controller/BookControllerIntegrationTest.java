package org.acme.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;

import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.api.model.UpdateBookRequest;
import org.acme.test.IntegrationTestSuite;
import org.acme.test.request.RequestHeadersBuilder;

@Tag("integration")
public class BookControllerIntegrationTest extends IntegrationTestSuite {

    private static final int CREATED = 201; // HttpStatus.CREATED
    private static final int BAD_REQUEST = 400; // HttpStatus.BAD_REQUEST

    private final HttpHeaders headers = RequestHeadersBuilder.create()
            .addHeader("x-dn", "cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org")
            .addHeaderContentTypeJson()
            .build();

    public BookControllerIntegrationTest() {
        super(8080);
    }

    @Test
    void testGetBook() {
        var request = get("/api/v1/books/1")
                .headers(headers)
                .build();

        var response = fetch(request, BookResponse.class);

        assertNotNull(response.getBody());
        assertEquals("The Great Gatsby", response.getBody().getTitle());
        assertEquals("F. Scott Fitzgerald", response.getBody().getAuthor());
        assertEquals("978-0-7432-7356-5", response.getBody().getIsbn());
        assertEquals(1925, response.getBody().getPublicationYear());
    }

    @Test
    void testCreateBook() throws IOException {
        var book = CreateBookRequest.builder()
                .title("Test Book")
                .author("Test Author")
                .isbn("1234567890")
                .publicationYear(2024)
                .build();

        var request = post("/api/v1/books")
                .headers(headers)
                .body(toJson(book))
                .build();

        var response = fetch(request, String.class);

        switch (response.getStatusCode().value()) {
        case CREATED:
            BookResponse bookResponse = fromJson(response.getBody(), BookResponse.class);
            assertNotNull(bookResponse);
            assertEquals("Test Book", bookResponse.getTitle());
            assertEquals("Test Author", bookResponse.getAuthor());
            assertEquals("1234567890", bookResponse.getIsbn());
            break;
        case BAD_REQUEST:
            ProblemDetail problemDetail = fromJson(response.getBody(), ProblemDetail.class);
            assertNotNull(problemDetail);
            assertEquals("Book Already Exists", problemDetail.getTitle());
            assertNotNull(problemDetail.getDetail());
            break;
        default:
            throw new AssertionError("Unexpected response status: " + response.getStatusCode());
        }
    }

    @Test
    void testFullCrudLifecycle() throws IOException {
        // Use a unique ISBN to avoid conflicts with other test runs
        String uniqueIsbn = "978-0-TEST-" + System.currentTimeMillis();

        // 1. CREATE - Create a new book
        var createRequest = CreateBookRequest.builder()
                .title("Full CRUD Test Book")
                .author("Test Author")
                .isbn(uniqueIsbn)
                .publicationYear(2024)
                .build();

        var createHttpRequest = post("/api/v1/books")
                .headers(headers)
                .body(toJson(createRequest))
                .build();

        var createResponse = fetch(createHttpRequest, BookResponse.class);
        assertEquals(CREATED, createResponse.getStatusCode().value());
        assertNotNull(createResponse.getBody());
        assertEquals("Full CRUD Test Book", createResponse.getBody().getTitle());
        assertEquals("Test Author", createResponse.getBody().getAuthor());
        assertEquals(uniqueIsbn, createResponse.getBody().getIsbn());
        assertEquals(2024, createResponse.getBody().getPublicationYear());

        // Extract the ID from the created book
        Long bookId = createResponse.getBody().getId();
        assertNotNull(bookId);

        // 2. UPDATE - Update the book with new data
        var updateRequest = UpdateBookRequest.builder()
                .title("Updated CRUD Test Book")
                .author("Updated Test Author")
                .isbn(uniqueIsbn) // Keep same ISBN
                .publicationYear(2025)
                .build();

        var updateHttpRequest = put("/api/v1/books/" + bookId)
                .headers(headers)
                .body(toJson(updateRequest))
                .build();

        var updateResponse = fetch(updateHttpRequest, BookResponse.class);
        assertEquals(200, updateResponse.getStatusCode().value()); // OK
        assertNotNull(updateResponse.getBody());
        assertEquals(bookId, updateResponse.getBody().getId());
        assertEquals("Updated CRUD Test Book", updateResponse.getBody().getTitle());
        assertEquals("Updated Test Author", updateResponse.getBody().getAuthor());
        assertEquals(uniqueIsbn, updateResponse.getBody().getIsbn());
        assertEquals(2025, updateResponse.getBody().getPublicationYear());

        // 3. DELETE - Delete the book
        var deleteHttpRequest = delete("/api/v1/books/" + bookId)
                .headers(headers)
                .build();

        var deleteResponse = fetch(deleteHttpRequest, Void.class);
        assertEquals(204, deleteResponse.getStatusCode().value()); // NO_CONTENT

        // 4. VERIFY DELETION - Try to get the deleted book, should get 404
        var getHttpRequest = get("/api/v1/books/" + bookId)
                .headers(headers)
                .build();

        var getResponse = fetch(getHttpRequest, String.class);
        assertEquals(404, getResponse.getStatusCode().value()); // NOT_FOUND

        ProblemDetail problemDetail = fromJson(getResponse.getBody(), ProblemDetail.class);
        assertNotNull(problemDetail);
        assertEquals("Book Not Found", problemDetail.getTitle());
    }
}
