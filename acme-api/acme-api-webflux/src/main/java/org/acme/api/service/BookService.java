package org.acme.api.service;

import org.acme.persistence.r2dbc.Book;
import org.acme.persistence.r2dbc.BookRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public Mono<Book> create(Book book) {
        return bookRepository.save(book);
    }

    public Flux<Book> findAll() {
        return bookRepository.findAll();
    }

    public Mono<Book> findById(Long id) {
        return bookRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Book not found with id: " + id)));
    }

    public Mono<Book> update(Long id, Book book) {
        return findById(id)
                .flatMap(existingBook -> {
                    existingBook.setTitle(book.getTitle());
                    existingBook.setAuthor(book.getAuthor());
                    existingBook.setIsbn(book.getIsbn());
                    existingBook.setPublicationYear(book.getPublicationYear());
                    return bookRepository.save(existingBook);
                });
    }

    public Mono<Void> delete(Long id) {
        return bookRepository.deleteById(id);
    }
}
