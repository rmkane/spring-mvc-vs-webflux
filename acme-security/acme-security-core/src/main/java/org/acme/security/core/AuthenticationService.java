package org.acme.security.core;

import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserLookupService userLookupService;

    /**
     * Validates and normalizes a UserInformation object from the authentication
     * principal.
     *
     * @param principal the authentication principal (should be UserInformation)
     * @return the UserInformation object
     * @throws BadCredentialsException if the principal is null or invalid
     */
    public UserInformation validateUserInformation(Object principal) {
        if (principal == null) {
            throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
        }

        if (principal instanceof UserInformation userInfo) {
            String username = userInfo.getUsername();
            if (username == null || username.trim().isEmpty()) {
                throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
            }
            return userInfo;
        }

        // Fallback for String (backward compatibility)
        if (principal instanceof String username) {
            return UserInformationUtil.fromUsername(username);
        }

        throw new BadCredentialsException("Invalid principal type: " + principal.getClass().getName());
    }

    /**
     * Creates an authenticated Authentication object from UserInformation. This is
     * the core authentication logic shared between MVC and WebFlux.
     *
     * @param principal the authentication principal (should be UserInformation)
     * @return an authenticated Authentication object
     */
    public Authentication createAuthenticatedAuthentication(Object principal) {
        UserInformation userInformation = validateUserInformation(principal);
        String username = userInformation.getUsername();

        UserPrincipal userPrincipal = userLookupService.createUser(username);

        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        log.debug("User authenticated: username={}, roles=[{}]", username, roles);

        return UsernamePasswordAuthenticationToken.authenticated(
                userInformation,
                null,
                userPrincipal.getAuthorities());
    }
}
