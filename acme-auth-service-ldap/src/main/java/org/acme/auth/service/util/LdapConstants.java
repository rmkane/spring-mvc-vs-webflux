package org.acme.auth.service.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants for LDAP operations and group naming.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LdapConstants {

    /**
     * Prefix for ACME role groups in LDAP.
     * <p>
     * Groups are named with this prefix (e.g., {@code ACME_READ_WRITE},
     * {@code ACME_READ_ONLY}) and are used directly as Spring Security authorities.
     */
    public static final String ACME_GROUP_PREFIX = "ACME_";
}
