package org.acme.security.core;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserLookupService userLookupService;

    /**
     * Validates and normalizes a username from the authentication principal.
     *
     * @param principal the authentication principal (typically a String username)
     * @return the normalized username
     * @throws BadCredentialsException if the username is null or empty
     */
    public String validateAndNormalizeUsername(Object principal) {
        if (principal == null) {
            throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
        }

        String username = principal instanceof String ? (String) principal : principal.toString();
        String normalized = username.trim();

        if (normalized.isEmpty()) {
            throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
        }

        return normalized;
    }

    /**
     * Creates an authenticated Authentication object from a username. This is the
     * core authentication logic shared between MVC and WebFlux.
     *
     * @param principal the authentication principal (typically a String username)
     * @return an authenticated Authentication object
     */
    public Authentication createAuthenticatedAuthentication(Object principal) {
        String normalizedUsername = validateAndNormalizeUsername(principal);
        UserPrincipal userPrincipal = userLookupService.createUser(normalizedUsername);

        UserInformation userInformation = UserInformation.builder()
                .username(normalizedUsername)
                .build();

        return UsernamePasswordAuthenticationToken.authenticated(
                userInformation,
                null,
                userPrincipal.getAuthorities());
    }
}
