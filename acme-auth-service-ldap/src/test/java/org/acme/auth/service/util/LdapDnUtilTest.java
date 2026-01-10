package org.acme.auth.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
