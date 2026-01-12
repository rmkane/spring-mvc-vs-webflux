package org.acme.auth.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class LdapDnUtilTest {

    /* --------------------- extractCn() tests --------------------- */

    @ParameterizedTest
    @NullAndEmptySource
    @CsvSource({
            "'   '", // whitespace only
    })
    void extractCn_shouldReturnNull_forNullOrEmptyInput(String dn) {
        assertNull(LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldExtractCn_whenCnIsFirst() {
        String dn = "cn=John Doe,ou=Engineering,dc=corp,dc=acme,dc=org";
        assertEquals("John Doe", LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldExtractCn_whenCnIsNotFirst() {
        String dn = "ou=Engineering,cn=John Doe,dc=corp,dc=acme,dc=org";
        assertEquals("John Doe", LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldExtractCn_whenCnIsLast() {
        String dn = "ou=Engineering,dc=corp,dc=acme,dc=org,cn=John Doe";
        assertEquals("John Doe", LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldExtractCn_whenOnlyCn() {
        String dn = "cn=John Doe";
        assertEquals("John Doe", LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldExtractCn_caseInsensitive() {
        String dn = "CN=John Doe,OU=Engineering,DC=corp,DC=acme,DC=org";
        assertEquals("John Doe", LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldReturnNull_whenNoCn() {
        String dn = "ou=Engineering,dc=corp,dc=acme,dc=org";
        assertNull(LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldHandleEscapedCharacters() {
        String dn = "cn=John\\, Doe,ou=Engineering,dc=corp,dc=acme,dc=org";
        assertEquals("John, Doe", LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldReturnNull_forInvalidDn() {
        String dn = "invalid-dn-format";
        assertNull(LdapDnUtil.extractCn(dn));
    }

    @Test
    void extractCn_shouldExtractLastCn_whenMultipleCns() {
        // LdapName iterates RDNs from right to left (most specific to least specific)
        // So "cn=Second CN" comes before "cn=First CN" in iteration order
        String dn = "cn=First CN,cn=Second CN,ou=Engineering";
        String result = LdapDnUtil.extractCn(dn);
        // Should return the first CN found during iteration (rightmost in DN string)
        assertEquals("Second CN", result);
    }

    /* --------------------- extractAttribute() tests --------------------- */

    @ParameterizedTest
    @NullAndEmptySource
    @CsvSource({
            "'   '", // whitespace only
    })
    void extractAttribute_shouldReturnNull_forNullOrEmptyDn(String dn) {
        assertNull(LdapDnUtil.extractAttribute(dn, "cn"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @CsvSource({
            "'   '", // whitespace only
    })
    void extractAttribute_shouldReturnNull_forNullOrEmptyAttributeType(String attributeType) {
        assertNull(LdapDnUtil.extractAttribute("cn=John Doe,ou=Engineering", attributeType));
    }

    @Test
    void extractAttribute_shouldExtractCn() {
        String dn = "cn=John Doe,ou=Engineering,dc=corp,dc=acme,dc=org";
        assertEquals("John Doe", LdapDnUtil.extractAttribute(dn, "cn"));
    }

    @Test
    void extractAttribute_shouldExtractOu() {
        String dn = "cn=John Doe,ou=Engineering,dc=corp,dc=acme,dc=org";
        assertEquals("Engineering", LdapDnUtil.extractAttribute(dn, "ou"));
    }

    @Test
    void extractAttribute_shouldExtractDc() {
        String dn = "cn=John Doe,ou=Engineering,dc=corp,dc=acme,dc=org";
        // LdapName iterates RDNs from right to left (most specific to least specific)
        // So "dc=org" comes first in iteration order
        assertEquals("org", LdapDnUtil.extractAttribute(dn, "dc"));
    }

    @Test
    void extractAttribute_shouldExtractAttribute_caseInsensitive() {
        String dn = "CN=John Doe,OU=Engineering,DC=corp";
        assertEquals("John Doe", LdapDnUtil.extractAttribute(dn, "CN"));
        assertEquals("Engineering", LdapDnUtil.extractAttribute(dn, "ou"));
        assertEquals("corp", LdapDnUtil.extractAttribute(dn, "DC"));
    }

    @Test
    void extractAttribute_shouldReturnNull_whenAttributeNotFound() {
        String dn = "cn=John Doe,ou=Engineering,dc=corp,dc=acme,dc=org";
        assertNull(LdapDnUtil.extractAttribute(dn, "uid"));
    }

    @Test
    void extractAttribute_shouldReturnNull_forInvalidDn() {
        String dn = "invalid-dn-format";
        assertNull(LdapDnUtil.extractAttribute(dn, "cn"));
    }

    @Test
    void extractAttribute_shouldHandleEscapedCharacters() {
        String dn = "cn=John\\, Doe,ou=Engineering\\+Test";
        assertEquals("John, Doe", LdapDnUtil.extractAttribute(dn, "cn"));
        assertEquals("Engineering+Test", LdapDnUtil.extractAttribute(dn, "ou"));
    }

    /* --------------------- extractRelativeDn() tests --------------------- */

    @ParameterizedTest
    @NullAndEmptySource
    @CsvSource({
            "'   '", // whitespace only
    })
    void extractRelativeDn_shouldReturnDn_forNullOrEmptyDn(String dn) {
        String baseDn = "dc=corp,dc=acme,dc=org";
        assertEquals(dn, LdapDnUtil.extractRelativeDn(dn, baseDn));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @CsvSource({
            "'   '", // whitespace only
    })
    void extractRelativeDn_shouldReturnDn_forNullOrEmptyBaseDn(String baseDn) {
        String dn = "cn=John Doe,ou=Engineering,dc=corp,dc=acme,dc=org";
        assertEquals(dn, LdapDnUtil.extractRelativeDn(dn, baseDn));
    }

    @Test
    void extractRelativeDn_shouldExtractRelativeDn_normalOrder() {
        // Normal order: most specific first (CN) to least specific last (DC)
        String fullDn = "cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org";
        String baseDn = "dc=corp,dc=acme,dc=org";
        String expected = "cn=jdoe,ou=engineering,ou=users";
        String result = LdapDnUtil.extractRelativeDn(fullDn, baseDn);
        assertEquals(expected, result);
    }

    @Test
    void extractRelativeDn_shouldExtractRelativeDn_reverseOrder() {
        // Reverse order: least specific first (DC) to most specific last (CN)
        // This is the format NGINX sends from X.509 certificates:
        // DC=org,DC=acme,DC=corp,OU=users,OU=engineering,CN=jdoe
        // Note: Only certificate DNs might be in reverse order; normal LDAP DNs are in
        // standard order
        String fullDn = "dc=org,dc=acme,dc=corp,ou=users,ou=engineering,cn=jdoe";
        String baseDn = "dc=corp,dc=acme,dc=org";
        // Should extract the relative part regardless of order
        String result = LdapDnUtil.extractRelativeDn(fullDn, baseDn);
        // LdapName normalizes the order, so result should be in canonical form (most
        // specific first)
        assertNotNull(result);
        // Result should contain the relative part (CN and OUs) but not the base (DCs)
        assertTrue(result.contains("jdoe") || result.contains("jdoe"));
        assertTrue(result.contains("engineering") || result.contains("Engineering"));
        assertTrue(result.contains("users") || result.contains("Users"));
        assertFalse(result.toLowerCase().contains("dc=corp"));
        assertFalse(result.toLowerCase().contains("dc=acme"));
        assertFalse(result.toLowerCase().contains("dc=org"));
    }

    @Test
    void extractRelativeDn_shouldExtractRelativeDn_caseInsensitive() {
        // Test with mixed case (normal order)
        String fullDn = "CN=jdoe,OU=engineering,OU=users,DC=corp,DC=acme,DC=org";
        String baseDn = "dc=corp,dc=acme,dc=org";
        String result = LdapDnUtil.extractRelativeDn(fullDn, baseDn);
        assertNotNull(result);
        // Result should contain the relative part (CN and OUs) but not the base (DCs)
        assertTrue(result.contains("jdoe") || result.contains("jdoe"));
        assertTrue(result.contains("engineering") || result.contains("Engineering"));
        assertTrue(result.contains("users") || result.contains("Users"));
        // Should not contain any DC components from the base
        assertFalse(result.toLowerCase().contains("dc=corp"));
        assertFalse(result.toLowerCase().contains("dc=acme"));
        assertFalse(result.toLowerCase().contains("dc=org"));
    }

    @Test
    void extractRelativeDn_shouldReturnEmptyString_whenDnEqualsBaseDn() {
        String dn = "dc=corp,dc=acme,dc=org";
        String baseDn = "dc=corp,dc=acme,dc=org";
        assertEquals("", LdapDnUtil.extractRelativeDn(dn, baseDn));
    }

    @Test
    void extractRelativeDn_shouldReturnEmptyString_whenDnEqualsBaseDn_reverseOrder() {
        // Base DN in normal order, DN in reverse order
        String dn = "dc=org,dc=acme,dc=corp";
        String baseDn = "dc=corp,dc=acme,dc=org";
        assertEquals("", LdapDnUtil.extractRelativeDn(dn, baseDn));
    }

    @Test
    void extractRelativeDn_shouldReturnDn_whenDnDoesNotContainBaseDn() {
        String dn = "cn=John Doe,ou=Engineering,dc=example,dc=com";
        String baseDn = "dc=corp,dc=acme,dc=org";
        // Should return the DN as-is since it doesn't contain the base
        String result = LdapDnUtil.extractRelativeDn(dn, baseDn);
        assertEquals(dn, result);
    }

    @Test
    void extractRelativeDn_shouldReturnDn_whenDnIsShorterThanBaseDn() {
        String dn = "cn=John Doe";
        String baseDn = "dc=corp,dc=acme,dc=org";
        assertEquals(dn, LdapDnUtil.extractRelativeDn(dn, baseDn));
    }

    @Test
    void extractRelativeDn_shouldHandleSingleRdn() {
        String fullDn = "cn=jdoe,dc=corp,dc=acme,dc=org";
        String baseDn = "dc=corp,dc=acme,dc=org";
        String result = LdapDnUtil.extractRelativeDn(fullDn, baseDn);
        assertEquals("cn=jdoe", result);
    }

    @Test
    void extractRelativeDn_shouldHandleReverseOrderSingleRdn() {
        // Reverse order with single RDN before base
        String fullDn = "dc=org,dc=acme,dc=corp,cn=jdoe";
        String baseDn = "dc=corp,dc=acme,dc=org";
        String result = LdapDnUtil.extractRelativeDn(fullDn, baseDn);
        assertNotNull(result);
        assertTrue(result.contains("cn=jdoe") || result.contains("CN=jdoe"));
        assertFalse(result.contains("dc="));
    }

    @Test
    void extractRelativeDn_shouldHandleComplexReverseOrder() {
        // Complex reverse order DN matching the actual NGINX format from X.509
        // certificates
        // Note: Only certificate DNs might be in reverse order; normal LDAP DNs are in
        // standard order
        String fullDn = "DC=org,DC=acme,DC=corp,OU=users,OU=engineering,CN=jdoe";
        String baseDn = "dc=corp,dc=acme,dc=org";
        String result = LdapDnUtil.extractRelativeDn(fullDn, baseDn);
        // Should extract the relative part (CN and OUs) but not the base (DCs)
        assertNotNull(result);
        assertTrue(result.contains("jdoe") || result.contains("jdoe"));
        assertTrue(result.contains("engineering") || result.contains("Engineering"));
        assertTrue(result.contains("users") || result.contains("Users"));
        // Should not contain any DC components from the base
        assertFalse(result.toLowerCase().contains("dc=corp"));
        assertFalse(result.toLowerCase().contains("dc=acme"));
        assertFalse(result.toLowerCase().contains("dc=org"));
    }
}
