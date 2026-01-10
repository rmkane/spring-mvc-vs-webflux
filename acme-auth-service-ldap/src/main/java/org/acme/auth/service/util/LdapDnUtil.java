package org.acme.auth.service.util;

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
     * Extracts an ACME role name from a group DN if it's valid.
     * <p>
     * Uses standard LDAP DN parsing to extract the CN (Common Name) attribute
     * value. Validates that the group name starts with the ACME group prefix.
     *
     * @param groupDn the group Distinguished Name
     * @return the ACME role name if valid, null otherwise
     */
    public static String extractAcmeRoleName(String groupDn) {
        if (!StringUtils.hasText(groupDn)) {
            return null;
        }
        String cnValue = extractCn(groupDn);
        if (cnValue != null && cnValue.startsWith(LdapConstants.ACME_GROUP_PREFIX)) {
            return cnValue;
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
     * If the DN ends with the base DN, it's removed. Otherwise, the DN is returned
     * as-is (assuming it's already relative).
     * <p>
     * Example: extractRelativeDn("cn=john,ou=users,dc=corp,dc=acme,dc=org",
     * "dc=corp,dc=acme,dc=org") -> "cn=john,ou=users"
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
