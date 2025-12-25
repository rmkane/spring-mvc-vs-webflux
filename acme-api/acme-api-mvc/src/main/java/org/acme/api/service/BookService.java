package org.acme.api.service;

import java.util.List;

import org.acme.api.util.SecurityContextUtil;
import org.acme.persistence.jpa.Book;
import org.acme.persistence.jpa.BookRepository;
import org.acme.security.core.UserInformation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;

    @PreAuthorize("hasRole('READ_WRITE')")
    public Book create(Book book) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing CREATE action for book: title={}, author={}",
                user.getDn(), book.getTitle(), book.getAuthor());
        book.setCreatedBy(user.getDn());
        book.setUpdatedBy(user.getDn());
        return bookRepository.save(book);
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    @Transactional(readOnly = true)
    public List<Book> findAll() {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing READ ALL action", user.getDn());
        return bookRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")
    @Transactional(readOnly = true)
    public Book findById(Long id) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing READ action for book id={}", user.getDn(), id);
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    public Book update(Long id, Book book) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing UPDATE action for book id={}, title={}",
                user.getDn(), id, book.getTitle());
        Book existingBook = findById(id);
        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setIsbn(book.getIsbn());
        existingBook.setPublicationYear(book.getPublicationYear());
        existingBook.setUpdatedBy(user.getDn());
        return bookRepository.save(existingBook);
    }

    @PreAuthorize("hasRole('READ_WRITE')")
    public void delete(Long id) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing DELETE action for book id={}", user.getDn(), id);
        bookRepository.deleteById(id);
    }
}
