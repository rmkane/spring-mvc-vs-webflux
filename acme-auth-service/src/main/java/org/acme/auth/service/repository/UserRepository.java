package org.acme.auth.service.repository;

import java.util.Optional;

import org.acme.auth.service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDn(String dn);
}
