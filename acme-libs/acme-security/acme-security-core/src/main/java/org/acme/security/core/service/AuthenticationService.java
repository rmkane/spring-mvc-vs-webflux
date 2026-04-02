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
import org.acme.security.core.model.HeaderCertificatePrincipal;
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

        if (principal instanceof HeaderCertificatePrincipal hcp) {
            if (!StringUtils.hasText(hcp.subjectDn())) {
                throw new BadCredentialsException(SecurityConstants.MISSING_SUBJECT_MESSAGE);
            }
            return UserInformation.builder()
                    .subjectDn(hcp.subjectDn())
                    .issuerDn(hcp.issuerDn())
                    .build();
        }

        // Fallback for String (backward compatibility)
        if (principal instanceof String dn) {
            return UserInformationUtil.fromDn(dn);
        }

        throw new BadCredentialsException("Invalid principal type: " + principal.getClass().getName());
    }

    /**
     * Authenticates using subject and issuer from headers (see
     * {@link HeaderCertificatePrincipal}). When the auth service returns an issuer
     * for the user, it must match the request issuer (after DN normalization).
     */
    public Authentication createAuthenticatedAuthentication(HeaderCertificatePrincipal clientPrincipal) {
        if (!StringUtils.hasText(clientPrincipal.subjectDn())) {
            throw new BadCredentialsException(SecurityConstants.MISSING_SUBJECT_MESSAGE);
        }

        String normalizedDn = DnUtil.normalize(clientPrincipal.subjectDn());
        if (normalizedDn == null) {
            throw new BadCredentialsException(SecurityConstants.MISSING_SUBJECT_MESSAGE);
        }

        UserInfo userInfo = cachedUserLookupService.lookupUser(normalizedDn);
        assertIssuerMatchesRequest(clientPrincipal.issuerDn(), userInfo);

        return buildAuthenticatedToken(userInfo);
    }

    /**
     * Creates an authenticated {@link Authentication} from a subject DN string.
     * Prefer {@link #createAuthenticatedAuthentication(HeaderCertificatePrincipal)}
     * when the request issuer is available.
     *
     * @param dn the Distinguished Name from the request header
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

        return buildAuthenticatedToken(userInfo);
    }

    private Authentication buildAuthenticatedToken(UserInfo userInfo) {
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

    private static void assertIssuerMatchesRequest(String headerIssuerDn, UserInfo userInfo) {
        if (!StringUtils.hasText(userInfo.getIssuerDn())) {
            return;
        }
        String normalizedExpected = DnUtil.normalize(userInfo.getIssuerDn());
        String normalizedActual = DnUtil.normalize(headerIssuerDn);
        if (normalizedExpected == null
                || normalizedActual == null
                || !normalizedExpected.equals(normalizedActual)) {
            throw new BadCredentialsException("Issuer DN does not match the registered certificate issuer");
        }
    }
}
