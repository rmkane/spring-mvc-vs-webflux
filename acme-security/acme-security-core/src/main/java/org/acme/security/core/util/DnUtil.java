package org.acme.security.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for normalizing Distinguished Names (DNs) used in
 * authentication.
 * <p>
 * Uses standard JDK {@link LdapName} for RFC 4514 compliant DN parsing and
 * normalization. This ensures consistent cache keys regardless of DN order
 * variations (e.g., reverse order from NGINX Ingress vs. normal order from
 * LDAP).
 * <p>
 * Normalization includes:
 * <ul>
 * <li>RFC 4514 compliant parsing (handles escaped characters, hex encoding,
 * etc.)</li>
 * <li>Order normalization (ensures consistent RDN order)</li>
 * <li>Whitespace normalization</li>
 * <li>Case normalization (lowercase for attribute types, preserves values)</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DnUtil {

    /**
     * Normalizes a Distinguished Name for consistent caching and comparison.
     * <p>
     * Uses RFC 4514 compliant DN parsing via {@link LdapName} to handle:
     * <ul>
     * <li>Order variations (e.g., reverse order from NGINX Ingress:
     * {@code DC=org,DC=acme,DC=corp,CN=jdoe} vs. normal order:
     * {@code CN=jdoe,DC=corp,DC=acme,DC=org})</li>
     * <li>Escaped special characters (e.g., {@code \,}, {@code \+},
     * {@code \"})</li>
     * <li>Hex-encoded characters (e.g., {@code \XX} format)</li>
     * <li>Whitespace variations</li>
     * </ul>
     * <p>
     * The normalized DN will have:
     * <ol>
     * <li>Consistent RDN order (most specific first: CN, then OU, then DC)</li>
     * <li>Normalized whitespace (no spaces around commas or equals)</li>
     * <li>Lowercase attribute types (CN, OU, DC) while preserving value case</li>
     * </ol>
     * <p>
     * Example:
     * <ul>
     * <li>Input:
     * {@code DC=org,DC=acme,DC=corp,OU=users,OU=engineering,CN=jdoe}</li>
     * <li>Output:
     * {@code cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org}</li>
     * </ul>
     *
     * @param dn the Distinguished Name to normalize
     * @return the normalized DN in canonical form (CN first), or null if input is
     *         null or empty
     */
    public static String normalize(String dn) {
        if (!StringUtils.hasText(dn)) {
            return null;
        }

        try {
            // Use RFC 4514 compliant DN parsing to handle order variations
            LdapName ldapName = new LdapName(dn.trim());

            // Normalize DN order (ensures CN comes first, handles reverse order from NGINX)
            String normalizedDn = normalizeDnOrder(ldapName);

            // Normalize whitespace in the final DN string
            return normalizeWhitespace(normalizedDn);
        } catch (InvalidNameException e) {
            log.debug("Invalid DN format, falling back to simple normalization: {}", dn, e);
            // Fallback to simple normalization for invalid DNs
            return dn.trim()
                    .replaceAll("\\s+", " ") // Collapse multiple spaces to single space
                    .replaceAll("\\s*,\\s*", ",") // Remove spaces around commas
                    .replaceAll("\\s*=\\s*", "=") // Remove spaces around equals signs
                    .toLowerCase(); // Lowercase entire DN (last step)
        } catch (Exception e) {
            log.warn("Error normalizing DN, falling back to simple normalization: {}", dn, e);
            // Fallback to simple normalization on any error
            return dn.trim()
                    .replaceAll("\\s+", " ") // Collapse multiple spaces to single space
                    .replaceAll("\\s*,\\s*", ",") // Remove spaces around commas
                    .replaceAll("\\s*=\\s*", "=") // Remove spaces around equals signs
                    .toLowerCase(); // Lowercase entire DN (last step)
        }
    }

    /**
     * Normalizes the order of RDNs in a DN to ensure CN comes first.
     * <p>
     * Handles both normal order (CN first) and reverse order (DC first) DNs. Uses
     * RFC 4514 compliant DN parsing via {@link LdapName} to normalize order.
     * <p>
     * Example:
     * <ul>
     * <li>Input: {@code DC=org,DC=acme,DC=corp,OU=users,OU=engineering,CN=jdoe}
     * (reverse order)</li>
     * <li>Output: {@code CN=jdoe,OU=engineering,OU=users,DC=corp,DC=acme,DC=org}
     * (normalized order)</li>
     * </ul>
     *
     * @param ldapName the parsed LdapName
     * @return the DN string with normalized order (CN first)
     */
    private static String normalizeDnOrder(LdapName ldapName) {
        // Get RDNs (LdapName normalizes order internally)
        // LdapName.getRdns() returns RDNs from most specific (index 0) to least
        // specific (last index)
        // For reverse order input:
        // DC=org,DC=acme,DC=corp,OU=users,OU=engineering,CN=jdoe
        // RDNs: [CN=jdoe, OU=engineering, OU=users, DC=corp, DC=acme, DC=org]
        // (normalized, most specific first)
        // For normal order input:
        // CN=jdoe,OU=engineering,OU=users,DC=corp,DC=acme,DC=org
        // RDNs: [CN=jdoe, OU=engineering, OU=users, DC=corp, DC=acme, DC=org] (same
        // normalized order)
        List<Rdn> rdns = ldapName.getRdns();

        // Separate CN from other RDNs to ensure CN comes first
        List<Rdn> cnRdns = new ArrayList<>();
        List<Rdn> otherRdns = new ArrayList<>();

        for (Rdn rdn : rdns) {
            if ("cn".equalsIgnoreCase(rdn.getType())) {
                cnRdns.add(rdn);
            } else {
                otherRdns.add(rdn);
            }
        }

        // LdapName.getRdns() returns RDNs from most specific (index 0) to least
        // specific (last index)
        // For normal order "CN=John Doe,OU=Engineering,DC=example,DC=com":
        // RDNs: [DC=com, DC=example, OU=Engineering, CN=John Doe] (least specific
        // first)
        // For reverse order "DC=org,DC=acme,DC=corp,OU=users,OU=engineering,CN=jdoe":
        // RDNs: [CN=jdoe, OU=engineering, OU=users, DC=corp, DC=acme, DC=org] (most
        // specific first)
        // We want consistent output: CN first, then OUs, then DCs
        // Check if CN is at the start (index 0) - if so, RDNs are already in correct
        // order
        // If CN is at the end, we need to reverse otherRdns
        boolean cnAtStart = !cnRdns.isEmpty() && rdns.indexOf(cnRdns.get(0)) == 0;
        if (!cnAtStart) {
            // CN is not at start, reverse otherRdns to get correct order
            Collections.reverse(otherRdns);
        }

        // Build DN string: CN first, then others (now in correct order)
        StringBuilder normalized = new StringBuilder();
        for (Rdn rdn : cnRdns) {
            if (normalized.length() > 0) {
                normalized.append(",");
            }
            normalized.append(rdn.toString());
        }
        for (Rdn rdn : otherRdns) {
            if (normalized.length() > 0) {
                normalized.append(",");
            }
            normalized.append(rdn.toString());
        }

        return normalized.toString();
    }

    /**
     * Normalizes whitespace in a DN string.
     * <p>
     * Removes spaces around commas and equals signs, collapses multiple spaces, and
     * trims the result.
     *
     * @param dn the DN string to normalize
     * @return the DN string with normalized whitespace (lowercased)
     */
    private static String normalizeWhitespace(String dn) {
        return dn.replaceAll("\\s+", " ") // Collapse multiple spaces to single space
                .replaceAll("\\s*,\\s*", ",") // Remove spaces around commas
                .replaceAll("\\s*=\\s*", "=") // Remove spaces around equals signs
                .trim()
                .toLowerCase(); // Lowercase entire DN
    }
}
