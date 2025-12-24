package org.acme.api.config;

import java.util.List;

import org.acme.persistence.r2dbc.User;
import org.acme.persistence.r2dbc.UserRepository;
import org.acme.persistence.r2dbc.UserRoleRepository;
import org.acme.security.core.UserPrincipal;
import org.acme.security.core.UserPrincipalRepository;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * R2DBC implementation of UserPrincipalRepository for WebFlux security. Queries
 * the database reactively using Spring Data R2DBC to find users and their
 * roles.
 * <p>
 * Note: This implementation uses blocking operations on a blocking scheduler
 * since the UserPrincipalRepository interface is synchronous. The blocking
 * operations are executed on a separate thread pool to avoid blocking the
 * reactive event loop.
 */
@Component
@RequiredArgsConstructor
public class R2dbcUserPrincipalRepository implements UserPrincipalRepository {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserPrincipal findByUsername(String username) {
        // Direct blocking is safe here because this method is called from
        // Mono.fromCallable() in the security config, which runs on a blocking
        // scheduler
        User user = userRepository.findByUsername(username)
                .block();
        if (user == null) {
            return null;
        }

        List<String> roles = userRoleRepository.findByUserId(user.getId())
                .map(role -> role.getRoleName())
                .collectList()
                .block();

        return toUserPrincipal(user, roles != null ? roles : List.of());
    }

    private UserPrincipal toUserPrincipal(User user, List<String> roles) {
        return UserPrincipal.builder()
                .username(user.getUsername())
                .roles(roles)
                .build();
    }
}
