package org.acme.security.core;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Service for looking up users and creating UserPrincipal objects. Uses a
 * UserPrincipalRepository implementation (provided by API modules) to query the
 * database for user information and roles.
 */
@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserPrincipalRepository userPrincipalRepository;

    /**
     * Creates a UserPrincipal for the given username by looking up the user in the
     * database and retrieving their roles.
     *
     * @param username the username to lookup
     * @return a UserPrincipal with the user's roles from the database
     * @throws BadCredentialsException if the username is invalid or user not found
     */
    public UserPrincipal createUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
        }

        UserPrincipal userPrincipal = userPrincipalRepository.findByUsername(username.trim());

        if (userPrincipal == null) {
            throw new BadCredentialsException("User not found: " + username);
        }

        return userPrincipal;
    }
}
