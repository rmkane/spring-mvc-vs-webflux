package org.acme.api.service.impl;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.api.exception.BookAlreadyExistsException;
import org.acme.api.mapper.BookMapper;
import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.api.model.UpdateBookRequest;
import org.acme.api.service.BookService;
import org.acme.api.util.SecurityContextUtil;
import org.acme.persistence.jpa.Book;
import org.acme.persistence.jpa.BookRepository;
import org.acme.security.core.model.UserInformation;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @PreAuthorize("hasRole('READ_WRITE')")
    @Override
    public BookResponse create(CreateBookRequest request) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing CREATE action for book: title={}, author={}, isbn={}",
                user.getDn(), request.getTitle(), request.getAuthor(), request.getIsbn());

        // Check if book with same ISBN already exists
        bookRepository.findByIsbn(request.getIsbn())
                .ifPresent(book -> {
                    throw new BookAlreadyExistsException(
                            "Book with ISBN '" + request.getIsbn() + "' already exists");
                });

        Book book = bookMapper.toEntity(request);
        book.setCreatedBy(user.getDn());
        // updatedBy and updatedAt are null on creation, set only on update
        Book saved = bookRepository.save(book);
        return bookMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    @Transactional(readOnly = true)
    @Override
    public List<BookResponse> findAll() {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing READ ALL action", user.getDn());
        return bookRepository.findAll().stream()
                .map(bookMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    @Transactional(readOnly = true)
    @Override
    public BookResponse findById(Long id) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing READ action for book id={}", user.getDn(), id);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        return bookMapper.toResponse(book);
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    @Override
    public BookResponse update(Long id, UpdateBookRequest request) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing UPDATE action for book id={}, title={}",
                user.getDn(), id, request.getTitle());
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        // Check if ISBN is being changed and if new ISBN already exists
        if (!existingBook.getIsbn().equals(request.getIsbn())) {
            bookRepository.findByIsbn(request.getIsbn())
                    .ifPresent(book -> {
                        throw new BookAlreadyExistsException(
                                "Book with ISBN '" + request.getIsbn() + "' already exists");
                    });
        }

        existingBook.setTitle(request.getTitle());
        existingBook.setAuthor(request.getAuthor());
        existingBook.setIsbn(request.getIsbn());
        existingBook.setPublicationYear(request.getPublicationYear());
        existingBook.setUpdatedBy(user.getDn());
        Book saved = bookRepository.save(existingBook);
        return bookMapper.toResponse(saved);
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    @Override
    public void delete(Long id) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing DELETE action for book id={}", user.getDn(), id);
        bookRepository.deleteById(id);
    }
}
