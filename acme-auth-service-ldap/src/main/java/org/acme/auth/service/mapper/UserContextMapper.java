package org.acme.auth.service.mapper;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.auth.service.dto.UserInfoResponse;
import org.acme.auth.service.util.LdapAttributeUtil;
import org.acme.auth.utils.LdapDnUtil;

/**
 * Context mapper that extracts user information from LDAP entry. The DN is
 * passed separately since it's the entry DN, not an attribute.
 */
@Slf4j
@RequiredArgsConstructor
public class UserContextMapper implements ContextMapper<UserInfoResponse> {

    private final String entryDn;

    @Override
    public UserInfoResponse mapFromContext(Object ctx) throws NamingException {
        if (!(ctx instanceof DirContextAdapter adapter)) {
            return null;
        }

        Attributes attrs = adapter.getAttributes();

        // Extract user attributes
        String givenName = LdapAttributeUtil.getAttributeValue(attrs, "givenName");
        String surname = LdapAttributeUtil.getAttributeValue(attrs, "sn");
        // Extract issuer DN from custom schema attribute
        String issuerDn = LdapAttributeUtil.getAttributeValue(attrs, "certificateIssuerDN");

        // Extract roles from memberOf attribute (LDAP groups)
        // Return all groups - auth service is agnostic about role naming
        var roles = LdapAttributeUtil.getGroupsFromMemberOf(attrs).stream()
                .map(LdapDnUtil::extractCn)
                .filter(cn -> cn != null)
                .toList();

        return UserInfoResponse.builder()
                .subjectDn(entryDn) // Use the entry DN
                .issuerDn(issuerDn)
                .givenName(givenName)
                .surname(surname)
                .roles(roles)
                .build();
    }
}
