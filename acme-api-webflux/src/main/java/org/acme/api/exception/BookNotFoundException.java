package org.acme.api.exception;

/**
 * Exception thrown when a book is not found by its ID.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }

    public BookNotFoundException(String isbn) {
        super("Book not found with ISBN: " + isbn);
    }

    public BookNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
