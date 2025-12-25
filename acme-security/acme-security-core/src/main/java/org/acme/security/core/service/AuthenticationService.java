package org.acme.security.core.service;

import java.util.stream.Collectors;

import org.acme.auth.client.UserInfo;
import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.model.UserInformation;
import org.acme.security.core.util.UserInformationUtil;
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

    private final CachedUserLookupService cachedUserLookupService;

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
            throw new BadCredentialsException(SecurityConstants.MISSING_DN_MESSAGE);
        }

        if (principal instanceof UserInformation userInfo) {
            String dn = userInfo.getDn();
            if (!StringUtils.hasText(dn)) {
                throw new BadCredentialsException(SecurityConstants.MISSING_DN_MESSAGE);
            }
            return userInfo;
        }

        // Fallback for String (backward compatibility)
        if (principal instanceof String dn) {
            return UserInformationUtil.fromDn(dn);
        }

        throw new BadCredentialsException("Invalid principal type: " + principal.getClass().getName());
    }

    /**
     * Creates an authenticated Authentication object from a DN string. This is the
     * core authentication logic shared between MVC and WebFlux.
     * <p>
     * The flow is: 1. Lookup the user in the auth service by DN to get UserInfo
     * (with roles) - cached to reduce calls to auth service 2. Create
     * UserInformation (derivative) from UserInfo 3. Use UserInformation as the
     * principal with roles from UserInfo
     *
     * @param dn the Distinguished Name from the request header
     * @return an authenticated Authentication object
     */
    public Authentication createAuthenticatedAuthentication(String dn) {
        if (!StringUtils.hasText(dn)) {
            throw new BadCredentialsException(SecurityConstants.MISSING_DN_MESSAGE);
        }

        // Lookup user from auth service by DN to get UserInfo with roles (cached)
        UserInfo userInfo = cachedUserLookupService.lookupUser(dn.trim());

        // Create UserInformation (derivative) from UserInfo
        UserInformation userInformation = UserInformationUtil.fromUserInfo(userInfo);

        String roles = userInfo.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        log.debug("User authenticated: dn={}, roles=[{}]", userInformation.getDn(), roles);

        return UsernamePasswordAuthenticationToken.authenticated(
                userInformation,
                null,
                userInfo.getAuthorities());
    }
}
