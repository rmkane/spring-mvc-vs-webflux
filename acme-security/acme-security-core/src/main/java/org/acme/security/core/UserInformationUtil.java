package org.acme.security.core;

import org.acme.auth.client.UserInfo;
import org.springframework.security.authentication.BadCredentialsException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for creating UserInformation objects from raw DN strings or
 * from UserInfo objects returned by the auth service.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserInformationUtil {

    /**
     * Marshals a raw DN string into a UserInformation object. Trims whitespace and
     * validates the DN.
     *
     * @param dn the raw Distinguished Name string
     * @return a UserInformation object with the normalized DN
     * @throws BadCredentialsException if the DN is null or empty after trimming
     */
    public static UserInformation fromDn(String dn) {
        if (dn == null) {
            throw new BadCredentialsException(SecurityConstants.MISSING_DN_MESSAGE);
        }

        String normalized = dn.trim();

        if (normalized.isEmpty()) {
            throw new BadCredentialsException(SecurityConstants.MISSING_DN_MESSAGE);
        }

        return UserInformation.builder()
                .dn(normalized)
                .build();
    }

    /**
     * Creates a UserInformation object from a UserInfo returned by the auth
     * service. This is a derivative object that extracts the DN from the UserInfo.
     *
     * @param userInfo the UserInfo from the auth service
     * @return a UserInformation object with the DN from UserInfo
     * @throws BadCredentialsException if the UserInfo is null or has no DN
     */
    public static UserInformation fromUserInfo(UserInfo userInfo) {
        if (userInfo == null) {
            throw new BadCredentialsException("UserInfo cannot be null");
        }

        // getUsername() returns the DN (for Spring Security compatibility)
        String dn = userInfo.getUsername();
        if (dn == null || dn.trim().isEmpty()) {
            throw new BadCredentialsException("UserInfo has no DN");
        }

        return UserInformation.builder()
                .dn(dn.trim())
                .build();
    }
}
