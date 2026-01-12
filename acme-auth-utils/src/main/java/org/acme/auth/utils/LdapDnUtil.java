package org.acme.auth.utils;

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
 * Utility class for parsing Distinguished Names (DNs) and extracting attribute
 * values.
 * <p>
 * Uses standard JDK {@link LdapName} for RFC 4514 compliant DN parsing,
 * properly handling escaped characters and edge cases.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LdapDnUtil {

    /**
     * Extracts the CN (Common Name) value from a Distinguished Name.
     * <p>
     * Uses standard LDAP DN parsing to find the CN attribute in any RDN (not just
     * the first one).
     * <p>
     * Example: "cn=John Doe,ou=Engineering,dc=corp,dc=acme,dc=org" -> "John Doe"
     * <p>
     * Example: "ou=Engineering,cn=John Doe,dc=corp,dc=acme,dc=org" -> "John Doe"
     *
     * @param dn the Distinguished Name
     * @return the CN value, or null if not found or invalid DN format
     */
    public static String extractCn(String dn) {
        if (!StringUtils.hasText(dn)) {
            return null;
        }
        try {
            LdapName ldapName = new LdapName(dn);
            // Iterate through all RDNs to find CN (CN can be in any position)
            for (Rdn rdn : ldapName.getRdns()) {
                if ("cn".equalsIgnoreCase(rdn.getType())) {
                    Object value = rdn.getValue();
                    return value != null ? value.toString() : null;
                }
            }
        } catch (InvalidNameException e) {
            log.debug("Invalid DN format for CN extraction: {}", dn, e);
        } catch (Exception e) {
            log.debug("Error extracting CN from DN: {}", dn, e);
        }
        return null;
    }

    /**
     * Extracts an attribute value from a Distinguished Name by attribute type.
     * <p>
     * Uses standard LDAP DN parsing to find the specified attribute in any RDN.
     * <p>
     * Example: extractAttribute("cn=John Doe,ou=Engineering", "ou") ->
     * "Engineering"
     *
     * @param dn            the Distinguished Name
     * @param attributeType the attribute type to extract (e.g., "cn", "ou", "dc")
     * @return the attribute value, or null if not found or invalid DN format
     */
    public static String extractAttribute(String dn, String attributeType) {
        if (!StringUtils.hasText(dn) || !StringUtils.hasText(attributeType)) {
            return null;
        }
        try {
            LdapName ldapName = new LdapName(dn);
            // Iterate through all RDNs to find the specified attribute
            for (Rdn rdn : ldapName.getRdns()) {
                if (attributeType.equalsIgnoreCase(rdn.getType())) {
                    Object value = rdn.getValue();
                    return value != null ? value.toString() : null;
                }
            }
        } catch (InvalidNameException e) {
            log.debug("Invalid DN format for attribute extraction: {}", dn, e);
        } catch (Exception e) {
            log.debug("Error extracting attribute '{}' from DN: {}", attributeType, dn, e);
        }
        return null;
    }

    /**
     * Ensures a DN is a full DN by appending the base DN if it's not already
     * present.
     * <p>
     * If the DN already contains the base DN, it's returned as-is. Otherwise, the
     * base DN is appended with a comma separator.
     *
     * @param dn     the Distinguished Name (may be relative or absolute)
     * @param baseDn the base DN to append if not present
     * @return the full DN
     */
    public static String ensureFullDn(String dn, String baseDn) {
        if (!StringUtils.hasText(dn)) {
            return dn;
        }
        if (!StringUtils.hasText(baseDn)) {
            return dn;
        }
        // If DN already contains base, return as-is
        if (dn.contains(baseDn)) {
            return dn;
        }
        // Otherwise, append base DN
        return dn + "," + baseDn;
    }

    /**
     * Extracts the relative DN from a full DN by removing the base DN suffix.
     * <p>
     * Handles DN order differences (e.g.,
     * "CN=jdoe,OU=engineering,DC=corp,DC=acme,DC=org" vs
     * "DC=org,DC=acme,DC=corp,OU=engineering,CN=jdoe") by using RFC 4514 compliant
     * DN parsing to compare DNs regardless of component order.
     * <p>
     * Example: extractRelativeDn("cn=john,ou=users,dc=corp,dc=acme,dc=org",
     * "dc=corp,dc=acme,dc=org") -> "cn=john,ou=users"
     * <p>
     * Example: extractRelativeDn("DC=org,DC=acme,DC=corp,OU=users,CN=john",
     * "dc=corp,dc=acme,dc=org") -> "CN=john,OU=users" (handles reverse order)
     *
     * @param dn     the full Distinguished Name
     * @param baseDn the base DN to remove
     * @return the relative DN (without the base DN suffix)
     */
    public static String extractRelativeDn(String dn, String baseDn) {
        if (!StringUtils.hasText(dn)) {
            return dn;
        }
        if (!StringUtils.hasText(baseDn)) {
            return dn;
        }
        try {
            // Use RFC 4514 compliant DN parsing to handle order differences
            LdapName dnName = new LdapName(dn);
            LdapName baseName = new LdapName(baseDn);

            // LdapName normalizes DNs, so we can use equals() to check if DN ends with base
            // But we need to check if the base DN is a suffix of the full DN
            // LdapName.getRdns() returns RDNs from most specific (index 0) to least
            // specific (last index)
            int dnSize = dnName.size();
            int baseSize = baseName.size();

            if (dnSize < baseSize) {
                // DN is shorter than base, can't contain it
                log.debug("DN is shorter than base DN, returning full DN: dn={}, baseDn={}", dn, baseDn);
                return dn;
            }

            // Check if the base DN is a suffix of the full DN
            // LdapName.getRdns() returns RDNs from most specific (index 0) to least
            // specific (last index)
            // For DN: cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org
            // RDNs: [cn=jdoe, ou=engineering, ou=users, dc=corp, dc=acme, dc=org]
            // For Base: dc=corp,dc=acme,dc=org
            // RDNs: [dc=org, dc=acme, dc=corp] (normalized, most specific first)
            // The base DN RDNs are in reverse order compared to the DN's suffix RDNs
            // So we need to compare dnRdns[last baseSize] with baseRdns[reversed]
            List<Rdn> dnRdns = dnName.getRdns();
            List<Rdn> baseRdns = baseName.getRdns();

            // Check if the base DN is a suffix of the full DN
            // LdapName.getRdns() returns RDNs from most specific (index 0) to least
            // specific (last index)
            // For normal order DN: "cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org"
            // RDNs: [dc=org, dc=acme, dc=corp, ou=users, ou=engineering, cn=jdoe] (base is
            // at start)
            // For reverse order DN:
            // "dc=org,dc=acme,dc=corp,ou=users,ou=engineering,cn=jdoe"
            // RDNs: [cn=jdoe, ou=engineering, ou=users, dc=corp, dc=acme, dc=org] (base is
            // at end)
            // So we need to check both the first baseSize RDNs and the last baseSize RDNs

            boolean matches = false;
            boolean baseAtEnd = false;

            // Check if the last baseSize RDNs match the base (for reverse order DNs from
            // certificates)
            List<Rdn> dnSuffixRdns = dnRdns.subList(dnSize - baseSize, dnSize);
            LdapName dnSuffix = new LdapName(dnSuffixRdns);
            matches = dnSuffix.equals(baseName);
            if (matches) {
                baseAtEnd = true;
            }

            if (!matches) {
                // Try reversed base comparison
                List<Rdn> reversedBaseRdns = new ArrayList<>(baseRdns);
                Collections.reverse(reversedBaseRdns);
                LdapName reversedBase = new LdapName(reversedBaseRdns);
                matches = dnSuffix.equals(reversedBase);
                if (matches) {
                    baseAtEnd = true;
                }
            }

            if (!matches) {
                // Check if the first baseSize RDNs match the base (for normal order DNs)
                List<Rdn> dnPrefixRdns = dnRdns.subList(0, baseSize);
                LdapName dnPrefix = new LdapName(dnPrefixRdns);
                matches = dnPrefix.equals(baseName);
                if (matches) {
                    baseAtEnd = false;
                }

                if (!matches) {
                    // Try reversed base comparison
                    List<Rdn> reversedBaseRdns = new ArrayList<>(baseRdns);
                    Collections.reverse(reversedBaseRdns);
                    LdapName reversedBase = new LdapName(reversedBaseRdns);
                    matches = dnPrefix.equals(reversedBase);
                    if (matches) {
                        baseAtEnd = false;
                    }
                }
            }

            if (!matches) {
                log.debug("DN does not contain base DN: dn={}, baseDn={}", dn, baseDn);
            }

            log.debug("DN base match check: matches={}, baseAtEnd={}, dn={}, baseDn={}", matches, baseAtEnd, dn,
                    baseDn);

            if (matches) {
                // Extract the relative DN
                // If base matched at the end (reverse order), extract from the start
                // If base matched at the start (normal order), extract from the end

                List<Rdn> relativeRdns;
                if (baseAtEnd) {
                    // Base is at the end, extract from the start
                    relativeRdns = new ArrayList<>(dnRdns.subList(0, dnSize - baseSize));
                } else {
                    // Base is at the start, extract from the end
                    relativeRdns = new ArrayList<>(dnRdns.subList(baseSize, dnSize));
                }

                if (relativeRdns.isEmpty()) {
                    return ""; // Root entry
                }

                // Simple logic: If CN is at the end, reverse the entire list
                // This handles both normal order (CN at end) and reverse order (CN at start)
                Rdn lastRdn = relativeRdns.get(relativeRdns.size() - 1);
                if ("cn".equalsIgnoreCase(lastRdn.getType())) {
                    // CN is at the end, reverse to put it first
                    Collections.reverse(relativeRdns);
                }
                // Otherwise, CN should already be at the start (or we'll handle it below)

                // Build DN string manually to ensure CN comes first
                // Use Rdn.toString() to handle proper escaping
                StringBuilder dnBuilder = new StringBuilder();
                for (Rdn rdn : relativeRdns) {
                    if (dnBuilder.length() > 0) {
                        dnBuilder.append(",");
                    }
                    dnBuilder.append(rdn.toString());
                }

                return dnBuilder.toString();
            }
        } catch (InvalidNameException e) {
            log.debug("Invalid DN format for relative DN extraction: dn={}, baseDn={}", dn, baseDn, e);
            // Fall back to string-based matching for backward compatibility
        } catch (Exception e) {
            log.debug("Error extracting relative DN: dn={}, baseDn={}", dn, baseDn, e);
            // Fall back to string-based matching for backward compatibility
        }

        // Fallback: String-based matching (original logic for backward compatibility)
        // If DN ends with base DN (with comma separator), remove it
        String baseSuffix = "," + baseDn;
        if (dn.endsWith(baseSuffix)) {
            return dn.substring(0, dn.length() - baseSuffix.length());
        }
        // If DN equals base DN, return empty string (root entry)
        if (dn.equals(baseDn)) {
            return "";
        }
        // Otherwise, assume it's already relative
        return dn;
    }
}
