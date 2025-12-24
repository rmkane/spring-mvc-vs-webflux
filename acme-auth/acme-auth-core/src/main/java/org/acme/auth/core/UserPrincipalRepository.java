package org.acme.auth.core;

/**
 * Abstraction for user lookup that returns a UserPrincipal with roles.
 * Implementations will use their respective persistence layers (JPA or R2DBC).
 */
public interface UserPrincipalRepository {

    /**
     * Finds a user by username and returns their roles.
     *
     * @param username the username to lookup
     * @return a UserPrincipal with the user's roles, or null if not found
     */
    UserPrincipal findByUsername(String username);
}
