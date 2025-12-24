package org.acme.api.service;

import org.acme.api.util.ReactiveSecurityContextUtil;
import org.acme.persistence.r2dbc.Book;
import org.acme.persistence.r2dbc.BookRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @PreAuthorize("hasRole('READ_WRITE')")
    public Mono<Book> create(Book book) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing CREATE action for book: title={}, author={}",
                        user.getUsername(), book.getTitle(), book.getAuthor()))
                .flatMap(user -> bookRepository.save(book));
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    public Flux<Book> findAll() {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing READ ALL action", user.getUsername()))
                .flatMapMany(user -> bookRepository.findAll());
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    public Mono<Book> findById(Long id) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing READ action for book id={}", user.getUsername(), id))
                .flatMap(user -> bookRepository.findById(id)
                        .switchIfEmpty(Mono.error(new RuntimeException("Book not found with id: " + id))));
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    public Mono<Book> update(Long id, Book book) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing UPDATE action for book id={}, title={}",
                        user.getUsername(), id, book.getTitle()))
                .flatMap(user -> findById(id)
                        .flatMap(existingBook -> {
                            existingBook.setTitle(book.getTitle());
                            existingBook.setAuthor(book.getAuthor());
                            existingBook.setIsbn(book.getIsbn());
                            existingBook.setPublicationYear(book.getPublicationYear());
                            return bookRepository.save(existingBook);
                        }));
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    public Mono<Void> delete(Long id) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing DELETE action for book id={}", user.getUsername(), id))
                .flatMap(user -> bookRepository.deleteById(id));
    }
}
