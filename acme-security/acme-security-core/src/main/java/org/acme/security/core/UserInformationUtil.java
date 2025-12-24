package org.acme.security.core;

import org.springframework.security.authentication.BadCredentialsException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for creating UserInformation objects from raw username strings.
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
}
