package org.acme.persistence.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends ReactiveCrudRepository<Book, Long> {
}

