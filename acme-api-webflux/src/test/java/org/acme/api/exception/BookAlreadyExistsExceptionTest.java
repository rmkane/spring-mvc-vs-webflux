package org.acme.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BookAlreadyExistsExceptionTest {

    @Test
    void constructor_shouldSetCorrectMessage() {
        String message = "Book with ISBN '978-0-123456-78-9' already exists";
        BookAlreadyExistsException exception = new BookAlreadyExistsException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void shouldBeThrowable() {
        assertThrows(BookAlreadyExistsException.class, () -> {
            throw new BookAlreadyExistsException("Book already exists");
        });
    }
}
