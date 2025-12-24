package org.acme.security.core;

import java.util.stream.Collectors;

import org.acme.auth.core.UserLookupService;
import org.acme.auth.core.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
            if (!StringUtils.hasText(username)) {
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
     * Creates an authenticated Authentication object from a username string. This
     * is the core authentication logic shared between MVC and WebFlux.
     * <p>
     * The flow is: 1. Lookup the user in the auth layer to get UserPrincipal (with
     * roles) 2. Create UserInformation (derivative) from UserPrincipal 3. Use
     * UserInformation as the principal with roles from UserPrincipal
     *
     * @param username the username from the request header
     * @return an authenticated Authentication object
     */
    public Authentication createAuthenticatedAuthentication(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
        }

        // Lookup user in auth layer to get UserPrincipal with roles
        UserPrincipal userPrincipal = userLookupService.lookupUser(username.trim());

        // Create UserInformation (derivative) from UserPrincipal
        UserInformation userInformation = UserInformationUtil.fromUserPrincipal(userPrincipal);

        String roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        log.debug("User authenticated: username={}, roles=[{}]", userInformation.getUsername(), roles);

        return UsernamePasswordAuthenticationToken.authenticated(
                userInformation,
                null,
                userPrincipal.getAuthorities());
    }
}
