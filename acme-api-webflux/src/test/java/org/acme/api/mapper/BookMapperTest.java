package org.acme.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.acme.api.app.AcmeApiWebFluxApplication;
import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.api.model.UpdateBookRequest;
import org.acme.persistence.r2dbc.entity.Book;

@SpringBootTest(classes = AcmeApiWebFluxApplication.class)
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    @Test
    void toEntity_fromCreateBookRequest_shouldMapCorrectly() {
        CreateBookRequest request = CreateBookRequest.builder()
                .title("The Great Gatsby")
                .author("F. Scott Fitzgerald")
                .isbn("978-0-7432-7356-5")
                .publicationYear(1925)
                .build();

        Book book = bookMapper.toEntity(request);

        assertEquals(request.getTitle(), book.getTitle());
        assertEquals(request.getAuthor(), book.getAuthor());
        assertEquals(request.getIsbn(), book.getIsbn());
        assertEquals(request.getPublicationYear(), book.getPublicationYear());
        assertNull(book.getId());
        assertNull(book.getCreatedAt());
        assertNull(book.getCreatedBy());
        assertNull(book.getUpdatedAt());
        assertNull(book.getUpdatedBy());
    }

    @Test
    void toEntity_fromUpdateBookRequest_shouldMapCorrectly() {
        UpdateBookRequest request = UpdateBookRequest.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn("978-0-123456-78-9")
                .publicationYear(2020)
                .build();

        Book book = bookMapper.toEntity(request);

        assertEquals(request.getTitle(), book.getTitle());
        assertEquals(request.getAuthor(), book.getAuthor());
        assertEquals(request.getIsbn(), book.getIsbn());
        assertEquals(request.getPublicationYear(), book.getPublicationYear());
        assertNull(book.getId());
        assertNull(book.getCreatedAt());
        assertNull(book.getCreatedBy());
        assertNull(book.getUpdatedAt());
        assertNull(book.getUpdatedBy());
    }

    @Test
    void toResponse_fromBook_shouldMapCorrectly() {
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now().plusHours(1);

        Book book = Book.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .isbn("978-0-123456-78-9")
                .publicationYear(2020)
                .createdAt(createdAt)
                .createdBy("user1")
                .updatedAt(updatedAt)
                .updatedBy("user2")
                .build();

        BookResponse response = bookMapper.toResponse(book);

        assertEquals(book.getId(), response.getId());
        assertEquals(book.getTitle(), response.getTitle());
        assertEquals(book.getAuthor(), response.getAuthor());
        assertEquals(book.getIsbn(), response.getIsbn());
        assertEquals(book.getPublicationYear(), response.getPublicationYear());
        assertEquals(book.getCreatedAt(), response.getCreatedAt());
        assertEquals(book.getCreatedBy(), response.getCreatedBy());
        assertEquals(book.getUpdatedAt(), response.getUpdatedAt());
        assertEquals(book.getUpdatedBy(), response.getUpdatedBy());
    }
}
