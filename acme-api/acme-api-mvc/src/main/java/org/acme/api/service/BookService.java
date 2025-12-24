package org.acme.api.service;

import java.util.List;

import org.acme.api.util.SecurityContextUtil;
import org.acme.persistence.jpa.Book;
import org.acme.persistence.jpa.BookRepository;
import org.acme.security.core.UserInformation;
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

    public Book create(Book book) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing CREATE action for book: title={}, author={}",
                user.getUsername(), book.getTitle(), book.getAuthor());
        return bookRepository.save(book);
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing READ ALL action", user.getUsername());
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing READ action for book id={}", user.getUsername(), id);
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    public Book update(Long id, Book book) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing UPDATE action for book id={}, title={}",
                user.getUsername(), id, book.getTitle());
        Book existingBook = findById(id);
        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setIsbn(book.getIsbn());
        existingBook.setPublicationYear(book.getPublicationYear());
        return bookRepository.save(existingBook);
    }

    public void delete(Long id) {
        UserInformation user = SecurityContextUtil.getCurrentUserInformation();
        log.debug("User {} performing DELETE action for book id={}", user.getUsername(), id);
        bookRepository.deleteById(id);
    }
}
