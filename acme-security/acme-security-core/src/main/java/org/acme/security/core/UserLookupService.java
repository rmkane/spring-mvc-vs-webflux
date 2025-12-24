package org.acme.security.core;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserLookupService {

    public UserPrincipal createUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        // Create user with dummy roles
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_OPERATOR");

        return UserPrincipal.builder()
                .username(username.trim())
                .roles(roles)
                .build();
    }
}

