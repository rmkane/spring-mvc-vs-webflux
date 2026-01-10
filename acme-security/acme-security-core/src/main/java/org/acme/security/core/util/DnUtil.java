package org.acme.security.core.util;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for normalizing Distinguished Names (DNs) used in
 * authentication.
 * <p>
 * This implementation follows a pragmatic approach inspired by RFC 4514 but
 * does not claim full compliance. It handles common DN formatting variations
 * for caching optimization purposes.
 * <p>
 * <strong>Limitations:</strong>
 * <ul>
 * <li>Does not handle escaped special characters (e.g., {@code \,}, {@code \+},
 * {@code \"})</li>
 * <li>Does not handle hex-encoded characters (e.g., {@code \XX} format)</li>
 * <li>Lowercases entire DN including values (may not match case-sensitive LDAP
 * directories)</li>
 * </ul>
 * <p>
 * If full RFC 4514 compliance is required, consider using a dedicated LDAP
 * library such as UnboundID LDAP SDK.
 * <p>
 * TODO: Consider replacing with RFC 4514 compliant implementation using
 * UnboundID LDAP SDK (com.unboundid:unboundid-ldapsdk:7.0.4) if edge cases are
 * encountered in production. The current implementation avoids adding an
 * external dependency and potential CVE exposure, but may not handle all DN
 * formats correctly. Monitor authentication logs for DN parsing issues.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DnUtil {

    /**
     * Normalizes a Distinguished Name for consistent caching and comparison.
     * <p>
     * Normalization steps:
     * <ol>
     * <li>Trim leading and trailing whitespace</li>
     * <li>Collapse multiple consecutive whitespace characters to a single
     * space</li>
     * <li>Remove whitespace around commas</li>
     * <li>Remove whitespace around equals signs</li>
     * <li>Convert entire DN to lowercase</li>
     * </ol>
     *
     * @param dn the Distinguished Name to normalize
     * @return the normalized DN, or null if input is null or empty
     */
    public static String normalize(String dn) {
        if (!StringUtils.hasText(dn)) {
            return null;
        }
        return dn.trim()
                .replaceAll("\\s+", " ") // Collapse multiple spaces to single space
                .replaceAll("\\s*,\\s*", ",") // Remove spaces around commas
                .replaceAll("\\s*=\\s*", "=") // Remove spaces around equals signs
                .toLowerCase(); // Lowercase entire DN (last step)
    }
}
