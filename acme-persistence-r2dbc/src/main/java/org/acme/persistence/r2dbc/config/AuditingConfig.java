package org.acme.persistence.r2dbc.config;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;

import reactor.core.publisher.Mono;

import org.acme.persistence.r2dbc.entity.Book;

/**
 * Configuration for automatic auditing of R2DBC entities. Provides lifecycle
 * callbacks similar to JPA's @PrePersist and @PreUpdate.
 */
@Configuration
public class AuditingConfig {

    /**
     * Callback to set createdAt timestamp before inserting new entities. Similar to
     * JPA's @PrePersist.
     */
    @Bean
    public BeforeConvertCallback<Book> bookAuditingCallback() {
        return (book, table) -> {
            // If createdAt is null, this is a new entity (insert operation)
            if (book.getCreatedAt() == null) {
                book.setCreatedAt(LocalDateTime.now());
            } else {
                // If createdAt exists, this is an update operation
                book.setUpdatedAt(LocalDateTime.now());
            }
            return Mono.just(book);
        };
    }
}
