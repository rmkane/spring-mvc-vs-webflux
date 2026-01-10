package org.acme.auth.service.service;

import org.acme.auth.service.dto.UserInfoResponse;

/**
 * Service interface for querying user information from LDAP.
 */
public interface LdapUserService {

    /**
     * Finds a user by DN (case-insensitive) and returns user information with
     * roles.
     * <p>
     * This method performs a case-insensitive search by trying multiple DN
     * variations and using LDAP search filters as fallback.
     *
     * @param dn the Distinguished Name to search for
     * @return UserInfoResponse if found
     * @throws org.acme.auth.service.exception.UserNotFoundException if user is not
     *                                                               found
     */
    UserInfoResponse findByDn(String dn);
}
