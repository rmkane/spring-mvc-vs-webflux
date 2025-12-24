package org.acme.security.core;

import org.acme.auth.core.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for creating UserInformation objects from raw username strings
 * or from UserPrincipal objects returned by the auth layer.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserInformationUtil {

    /**
     * Marshals a raw username string into a UserInformation object. Trims
     * whitespace and validates the username.
     *
     * @param username the raw username string
     * @return a UserInformation object with the normalized username
     * @throws BadCredentialsException if the username is null or empty after
     *                                 trimming
     */
    public static UserInformation fromUsername(String username) {
        if (username == null) {
            throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
        }

        String normalized = username.trim();

        if (normalized.isEmpty()) {
            throw new BadCredentialsException(SecurityConstants.MISSING_USERNAME_MESSAGE);
        }

        return UserInformation.builder()
                .username(normalized)
                .build();
    }

    /**
     * Creates a UserInformation object from a UserPrincipal returned by the auth
     * layer. This is a derivative object that extracts the username from the
     * UserPrincipal.
     *
     * @param userPrincipal the UserPrincipal from the auth layer
     * @return a UserInformation object with the username from UserPrincipal
     * @throws BadCredentialsException if the UserPrincipal is null or has no
     *                                 username
     */
    public static UserInformation fromUserPrincipal(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new BadCredentialsException("UserPrincipal cannot be null");
        }

        String username = userPrincipal.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new BadCredentialsException("UserPrincipal has no username");
        }

        return UserInformation.builder()
                .username(username.trim())
                .build();
    }
}
