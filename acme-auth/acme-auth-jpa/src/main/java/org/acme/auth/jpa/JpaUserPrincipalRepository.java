package org.acme.auth.jpa;

import java.util.List;
import java.util.stream.Collectors;

import org.acme.auth.core.UserPrincipal;
import org.acme.auth.core.UserPrincipalRepository;
import org.acme.persistence.jpa.User;
import org.acme.persistence.jpa.UserRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * JPA implementation of UserPrincipalRepository. Queries the database using
 * Spring Data JPA to find users and their roles.
 */
@Component
@RequiredArgsConstructor
public class JpaUserPrincipalRepository implements UserPrincipalRepository {

    private final UserRepository userRepository;

    @Override
    public UserPrincipal findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toUserPrincipal)
                .orElse(null);
    }

    private UserPrincipal toUserPrincipal(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getRoleName())
                .collect(Collectors.toList());

        return UserPrincipal.builder()
                .username(user.getUsername())
                .roles(roles)
                .build();
    }
}
