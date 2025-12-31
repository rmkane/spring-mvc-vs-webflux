package org.acme.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
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
    void testGetBook() throws JsonProcessingException, IOException {
        var request = get("/api/books/1")
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
    void testCreateBook() throws JsonProcessingException, IOException {
        var book = CreateBookRequest.builder()
                .title("Test Book")
                .author("Test Author")
                .isbn("1234567890")
                .publicationYear(2024)
                .build();

        var request = post("/api/books")
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
}
