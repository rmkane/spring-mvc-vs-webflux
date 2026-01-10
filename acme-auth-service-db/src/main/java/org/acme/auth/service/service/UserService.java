package org.acme.auth.service.service;

import org.acme.auth.service.dto.UserInfoResponse;
import org.acme.auth.service.exception.UserNotFoundException;

/**
 * Service interface for querying user information from database.
 */
public interface UserService {

    /**
     * Finds a user by DN (case-insensitive) and returns user information with
     * roles.
     *
     * @param dn the Distinguished Name to search for
     * @return UserInfoResponse if found
     * @throws UserNotFoundException if user is not found
     */
    UserInfoResponse findByDn(String dn);
}
