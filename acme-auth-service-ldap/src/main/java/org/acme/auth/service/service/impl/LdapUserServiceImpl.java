package org.acme.auth.service.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.auth.service.dto.UserInfoResponse;
import org.acme.auth.service.exception.UserNotFoundException;
import org.acme.auth.service.mapper.UserContextMapper;
import org.acme.auth.service.service.LdapUserService;
import org.acme.auth.service.util.LdapDnUtil;

/**
 * Implementation of {@link LdapUserService} for querying user information from
 * LDAP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LdapUserServiceImpl implements LdapUserService {

    private final LdapTemplate ldapTemplate;

    @Value("${spring.ldap.base}")
    private String ldapBase;

    @Override
    public UserInfoResponse findByDn(String dn) {
        log.debug("findByDn called with DN: {}", dn);
        if (!StringUtils.hasText(dn)) {
            throw new UserNotFoundException(dn);
        }

        // Try exact DN lookup first
        // Spring LDAP lookup expects a relative DN when base DN is configured
        UserInfoResponse user = null;
        String actualDn = null;
        try {
            String relativeDn = LdapDnUtil.extractRelativeDn(dn, ldapBase);
            log.debug("Attempting exact DN lookup with relative DN: {} (from full DN: {})", relativeDn, dn);
            user = ldapTemplate.lookup(relativeDn, new UserContextMapper(dn));
            if (user != null) {
                actualDn = dn;
                log.debug("Found user in LDAP by exact DN: {}", dn);
                // User found, skip fallback search
            }
        } catch (NameNotFoundException e) {
            log.debug("User not found by exact DN, trying case-insensitive search: {}", dn);
        } catch (Exception e) {
            log.warn("Error looking up user by exact DN: {}", dn, e);
        }

        // Fallback: case-insensitive search by extracting CN and searching all users
        // OpenLDAP's CN attribute matching is case-sensitive, so we search all users
        // and filter by case-insensitive CN match in Java
        if (user == null) {
            try {
                String cn = LdapDnUtil.extractCn(dn);
                if (cn == null) {
                    log.debug("Could not extract CN from DN: {}", dn);
                } else {
                    log.debug("Searching for user with CN '{}' (case-insensitive)", cn);

                    // Search all inetOrgPerson entries - use a mapper that extracts the entry DN
                    // Use empty string to search from context base (ldapBase is set in
                    // LdapContextSource)
                    // Spring LDAP will return full DNs when searching from the base
                    List<UserInfoResponse> users = ldapTemplate.search(
                            "",
                            "(objectClass=inetOrgPerson)",
                            (ContextMapper<UserInfoResponse>) ctx -> {
                                if (ctx instanceof DirContextAdapter adapter) {
                                    String entryDn = adapter.getDn().toString();
                                    // Spring LDAP should return full DN when searching from base
                                    // If it's relative, construct full DN by appending base
                                    String fullDn = LdapDnUtil.ensureFullDn(entryDn, ldapBase);
                                    log.debug("Mapped entry DN: {} -> {}", entryDn, fullDn);
                                    return new UserContextMapper(fullDn).mapFromContext(ctx);
                                }
                                return null;
                            });

                    log.debug("Found {} inetOrgPerson entries, filtering by CN '{}'", users.size(), cn);

                    // Filter by case-insensitive CN match
                    UserInfoResponse found = users.stream()
                            .filter(u -> {
                                String userCn = LdapDnUtil.extractCn(u.getDn());
                                boolean matches = userCn != null && userCn.equalsIgnoreCase(cn);
                                if (matches) {
                                    log.debug("Matched user: DN={}, CN={}", u.getDn(), userCn);
                                }
                                return matches;
                            })
                            .findFirst()
                            .orElse(null);

                    if (found != null) {
                        log.debug("Found user in LDAP via case-insensitive CN search: {} (matched CN: {})",
                                found.getDn(), cn);
                        user = found;
                        actualDn = found.getDn(); // Use the actual DN from LDAP for group queries
                    } else {
                        log.debug("No user found matching CN '{}' (case-insensitive) from DN: {}", cn, dn);
                    }
                }
            } catch (Exception e) {
                log.warn("Error during case-insensitive CN search for DN: {}", dn, e);
            }
        }

        if (user == null) {
            log.debug("User not found in LDAP for DN: {}", dn);
            throw new UserNotFoundException(dn);
        }

        // Query groups to find which groups contain this user as a member
        // (memberOf overlay is not enabled, so we query groups directly)
        String userDnForQuery = actualDn != null ? actualDn : user.getDn();
        List<String> roles = queryUserRoles(userDnForQuery);
        log.debug("Found {} roles for user {}: {}", roles.size(), user.getDn(), roles);

        // Return user with roles populated
        return UserInfoResponse.builder()
                .dn(dn) // Use original DN (may differ from actualDn in case of case-insensitive match)
                .givenName(user.getGivenName())
                .surname(user.getSurname())
                .roles(roles)
                .build();
    }

    /**
     * Queries LDAP groups to find which groups contain the given user DN as a
     * member.
     * <p>
     * Since the memberOf overlay is not enabled, we query groups directly using a
     * filter that matches groups where the member attribute equals the user DN.
     *
     * @param userDn the user's Distinguished Name
     * @return list of role names (group CNs starting with ACME_)
     */
    private List<String> queryUserRoles(String userDn) {
        List<String> roles = new ArrayList<>();
        try {
            // Search for groups that have this user as a member
            // Use empty string to search from context base (ldapBase is set in
            // LdapContextSource)
            String searchFilter = "(member=" + userDn + ")";
            List<String> groupDns = ldapTemplate.search(
                    "",
                    searchFilter,
                    (ContextMapper<String>) ctx -> {
                        if (ctx instanceof DirContextAdapter adapter) {
                            String groupDn = adapter.getDn().toString();
                            // Spring LDAP should return full DN when searching from base
                            // If it's relative, construct full DN by appending base
                            return LdapDnUtil.ensureFullDn(groupDn, ldapBase);
                        }
                        return null;
                    });

            log.debug("Found {} groups containing user {}: {}", groupDns.size(), userDn, groupDns);

            // Extract role names from group DNs
            for (String groupDn : groupDns) {
                String roleName = LdapDnUtil.extractAcmeRoleName(groupDn);
                if (roleName != null) {
                    roles.add(roleName);
                    log.debug("Added role '{}' from group DN: {}", roleName, groupDn);
                }
            }
        } catch (Exception e) {
            log.warn("Error querying user roles for DN: {}", userDn, e);
        }
        return roles;
    }
}
