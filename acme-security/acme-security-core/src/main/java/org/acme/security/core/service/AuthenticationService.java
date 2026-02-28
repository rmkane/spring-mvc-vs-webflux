package org.acme.security.core.service;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.auth.client.UserInfo;
import org.acme.auth.utils.DnUtil;
import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.model.UserInformation;
import org.acme.security.core.util.UserInformationUtil;

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
            throw new BadCredentialsException(SecurityConstants.MISSING_SUBJECT_MESSAGE);
        }

        if (principal instanceof UserInformation userInfo) {
            String subjectDn = userInfo.getSubjectDn();
            if (!StringUtils.hasText(subjectDn)) {
                throw new BadCredentialsException(SecurityConstants.MISSING_SUBJECT_MESSAGE);
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
     * The flow is: 1. Look up the user in the auth service by DN to get UserInfo
     * (with roles) - cached to reduce calls to auth service 2. Create
     * UserInformation (derivative) from UserInfo 3. Use UserInformation as the
     * principal with roles from UserInfo
     *
     * @param dn the Distinguished Name from the request header
     * @return an authenticated Authentication object
     */
    public Authentication createAuthenticatedAuthentication(String dn) {
        if (!StringUtils.hasText(dn)) {
            throw new BadCredentialsException(SecurityConstants.MISSING_SUBJECT_MESSAGE);
        }

        // Normalize DN for consistent lookup and caching
        String normalizedDn = DnUtil.normalize(dn);
        if (normalizedDn == null) {
            throw new BadCredentialsException(SecurityConstants.MISSING_SUBJECT_MESSAGE);
        }

        // Look up user from auth service by DN to get UserInfo with roles (cached)
        UserInfo userInfo = cachedUserLookupService.lookupUser(normalizedDn);

        // Create UserInformation (derivative) from UserInfo
        UserInformation userInformation = UserInformationUtil.fromUserInfo(userInfo);

        // Filter roles to only include ACME roles (defense in depth - auth service is
        // agnostic)
        Collection<? extends GrantedAuthority> filteredAuthorities = userInfo.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith(SecurityConstants.ACME_GROUP_PREFIX))
                .toList();

        String roles = filteredAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));

        log.debug("User authenticated: subjectDn={}, issuerDn={}, roles=[{}]", userInformation.getSubjectDn(),
                userInformation.getIssuerDn(), roles);

        return UsernamePasswordAuthenticationToken.authenticated(
                userInformation,
                null,
                filteredAuthorities);
    }
}
