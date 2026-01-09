package org.acme.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BookNotFoundExceptionTest {

    @Test
    void constructorWithId_shouldSetCorrectMessage() {
        Long id = 123L;
        BookNotFoundException exception = new BookNotFoundException(id);

        assertEquals("Book not found with id: 123", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithIsbn_shouldSetCorrectMessage() {
        String isbn = "978-0-123456-78-9";
        BookNotFoundException exception = new BookNotFoundException(isbn);

        assertEquals("Book not found with ISBN: 978-0-123456-78-9", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithMessageAndCause_shouldSetBoth() {
        String message = "Custom error message";
        Throwable cause = new RuntimeException("Root cause");
        BookNotFoundException exception = new BookNotFoundException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void shouldBeThrowable() {
        assertThrows(BookNotFoundException.class, () -> {
            throw new BookNotFoundException(1L);
        });
    }
}
