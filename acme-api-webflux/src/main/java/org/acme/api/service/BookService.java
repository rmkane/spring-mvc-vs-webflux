package org.acme.api.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.acme.api.model.BookResponse;
import org.acme.api.model.CreateBookRequest;
import org.acme.api.model.UpdateBookRequest;

public interface BookService {

    Mono<BookResponse> create(CreateBookRequest request);

    Flux<BookResponse> findAll();

    Mono<BookResponse> findById(Long id);

    Mono<BookResponse> update(Long id, UpdateBookRequest request);

    Mono<Void> delete(Long id);
}
