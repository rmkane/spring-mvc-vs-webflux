package org.acme.auth.service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.acme.auth.service.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by DN with roles eagerly fetched using JOIN FETCH to avoid N+1
     * queries. Since roles are now LAZY, we need to explicitly fetch them when
     * needed.
     *
     * @param dn the Distinguished Name
     * @return Optional containing User if found, empty otherwise
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE LOWER(u.dn) = LOWER(:dn)")
    Optional<User> findByDnIgnoreCase(@Param("dn") String dn);
}
