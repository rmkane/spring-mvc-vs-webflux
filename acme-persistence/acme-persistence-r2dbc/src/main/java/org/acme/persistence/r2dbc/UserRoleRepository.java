package org.acme.persistence.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
public interface UserRoleRepository extends ReactiveCrudRepository<UserRole, Long> {

    Flux<UserRole> findByUserId(Long userId);
}
