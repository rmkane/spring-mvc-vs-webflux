package org.acme.api.service.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.acme.api.exception.BookAlreadyExistsException;
import org.acme.api.exception.BookNotFoundException;
import org.acme.api.mapper.BookMapper;
import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.api.model.UpdateBookRequest;
import org.acme.api.service.BookService;
import org.acme.api.util.ReactiveSecurityContextUtil;
import org.acme.persistence.r2dbc.entity.Book;
import org.acme.persistence.r2dbc.repository.BookRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @PreAuthorize("hasRole('READ_WRITE')")
    @Override
    public Mono<BookResponse> create(CreateBookRequest request) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing CREATE action for book: title={}, author={}, isbn={}",
                        user.getDn(), request.getTitle(), request.getAuthor(), request.getIsbn()))
                .flatMap(user -> {
                    // Check if book with same ISBN already exists
                    return bookRepository.findByIsbn(request.getIsbn())
                            .flatMap(existingBook -> Mono.<Book>error(new BookAlreadyExistsException(
                                    "Book with ISBN '" + request.getIsbn() + "' already exists")))
                            .switchIfEmpty(Mono.defer(() -> {
                                Book book = bookMapper.toEntity(request);
                                book.setCreatedBy(user.getDn());
                                // createdAt set automatically by AuditingConfig callback
                                // updatedAt and updatedBy are null on creation, set only on update
                                return bookRepository.save(book);
                            }))
                            .map(bookMapper::toResponse);
                });
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    @Override
    public Flux<BookResponse> findAll() {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing READ ALL action", user.getDn()))
                .flatMapMany(user -> bookRepository.findAll()
                        .map(bookMapper::toResponse));
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    @Override
    public Mono<BookResponse> findById(Long id) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing READ action for book id={}", user.getDn(), id))
                .flatMap(user -> bookRepository.findById(id)
                        .switchIfEmpty(Mono.error(new BookNotFoundException(id)))
                        .map(bookMapper::toResponse));
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    @Override
    public Mono<BookResponse> update(Long id, UpdateBookRequest request) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing UPDATE action for book id={}, title={}",
                        user.getDn(), id, request.getTitle()))
                .flatMap(user -> bookRepository.findById(id)
                        .switchIfEmpty(Mono.error(new BookNotFoundException(id)))
                        .flatMap(existingBook -> {
                            // Check if ISBN is being changed and if new ISBN already exists
                            if (!existingBook.getIsbn().equals(request.getIsbn())) {
                                return bookRepository.findByIsbn(request.getIsbn())
                                        .flatMap(book -> Mono.<Book>error(new BookAlreadyExistsException(
                                                "Book with ISBN '" + request.getIsbn() + "' already exists")))
                                        .switchIfEmpty(Mono.defer(() -> {
                                            existingBook.setTitle(request.getTitle());
                                            existingBook.setAuthor(request.getAuthor());
                                            existingBook.setIsbn(request.getIsbn());
                                            existingBook.setPublicationYear(request.getPublicationYear());
                                            existingBook.setUpdatedBy(user.getDn());
                                            // updatedAt set automatically by AuditingConfig callback
                                            return bookRepository.save(existingBook);
                                        }));
                            } else {
                                existingBook.setTitle(request.getTitle());
                                existingBook.setAuthor(request.getAuthor());
                                existingBook.setPublicationYear(request.getPublicationYear());
                                existingBook.setUpdatedBy(user.getDn());
                                // updatedAt set automatically by AuditingConfig callback
                                return bookRepository.save(existingBook);
                            }
                        })
                        .map(bookMapper::toResponse));
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    @Override
    public Mono<Void> delete(Long id) {
        return ReactiveSecurityContextUtil.getCurrentUserInformation()
                .doOnNext(user -> log.debug("User {} performing DELETE action for book id={}", user.getDn(), id))
                .flatMap(user -> bookRepository.existsById(id)
                        .flatMap(exists -> {
                            if (!exists) {
                                return Mono.error(new BookNotFoundException(id));
                            }
                            return bookRepository.deleteById(id);
                        }));
    }
}
