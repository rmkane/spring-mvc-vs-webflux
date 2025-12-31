package org.acme.api.service;

import java.util.List;

import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.api.model.UpdateBookRequest;

public interface BookService {

    BookResponse create(CreateBookRequest request);

    List<BookResponse> findAll();

    BookResponse findById(Long id);

    BookResponse update(Long id, UpdateBookRequest request);

    void delete(Long id);
}
