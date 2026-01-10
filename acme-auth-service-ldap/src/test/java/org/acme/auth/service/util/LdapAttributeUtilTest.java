package org.acme.auth.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class LdapAttributeUtilTest {

    @Test
    void getAttributeValue_shouldReturnValue_whenAttributeExists() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        Attribute attr = mock(Attribute.class);
        when(attrs.get("givenName")).thenReturn(attr);
        when(attr.get()).thenReturn("John");

        // When
        String result = LdapAttributeUtil.getAttributeValue(attrs, "givenName");

        // Then
        assertEquals("John", result);
    }

    @Test
    void getAttributeValue_shouldReturnNull_whenAttributesIsNull() {
        // When
        String result = LdapAttributeUtil.getAttributeValue(null, "givenName");

        // Then
        assertNull(result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getAttributeValue_shouldReturnNull_whenAttributeNameIsNullOrEmpty(String attributeName) {
        // Given
        Attributes attrs = mock(Attributes.class);

        // When
        String result = LdapAttributeUtil.getAttributeValue(attrs, attributeName);

        // Then
        assertNull(result);
    }

    @Test
    void getAttributeValue_shouldReturnNull_whenAttributeDoesNotExist() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        when(attrs.get("nonExistent")).thenReturn(null);

        // When
        String result = LdapAttributeUtil.getAttributeValue(attrs, "nonExistent");

        // Then
        assertNull(result);
    }

    @Test
    void getAttributeValue_shouldReturnNull_whenAttributeValueIsNull() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        Attribute attr = mock(Attribute.class);
        when(attrs.get("givenName")).thenReturn(attr);
        when(attr.get()).thenReturn(null);

        // When
        String result = LdapAttributeUtil.getAttributeValue(attrs, "givenName");

        // Then
        assertNull(result);
    }

    @Test
    void getAttributeValue_shouldReturnNull_whenNamingExceptionOccurs() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        Attribute attr = mock(Attribute.class);
        when(attrs.get("givenName")).thenReturn(attr);
        when(attr.get()).thenThrow(new NamingException("LDAP error"));

        // When
        String result = LdapAttributeUtil.getAttributeValue(attrs, "givenName");

        // Then
        assertNull(result);
    }

    @Test
    void getAttributeValue_shouldConvertValueToString() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        Attribute attr = mock(Attribute.class);
        when(attrs.get("uid")).thenReturn(attr);
        when(attr.get()).thenReturn(12345);

        // When
        String result = LdapAttributeUtil.getAttributeValue(attrs, "uid");

        // Then
        assertEquals("12345", result);
    }

    @Test
    void getAttributeValue_shouldHandleWhitespaceOnlyAttributeName() {
        // Given
        Attributes attrs = mock(Attributes.class);

        // When
        String result = LdapAttributeUtil.getAttributeValue(attrs, "   ");

        // Then
        assertNull(result);
    }

    /* --------------------- getGroupsFromMemberOf() tests --------------------- */

    @Test
    void getGroupsFromMemberOf_shouldReturnEmptyList_whenAttributesIsNull() {
        // When
        List<String> result = LdapAttributeUtil.getGroupsFromMemberOf(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getGroupsFromMemberOf_shouldReturnEmptyList_whenMemberOfAttributeDoesNotExist() {
        // Given
        Attributes attrs = mock(Attributes.class);
        when(attrs.get("memberOf")).thenReturn(null);

        // When
        List<String> result = LdapAttributeUtil.getGroupsFromMemberOf(attrs);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getGroupsFromMemberOf_shouldExtractAllGroupDns() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        Attribute memberOfAttr = mock(Attribute.class);
        when(attrs.get("memberOf")).thenReturn(memberOfAttr);
        when(memberOfAttr.size()).thenReturn(3);
        when(memberOfAttr.get(0)).thenReturn("cn=ACME_READ_WRITE,ou=roles,dc=corp,dc=acme,dc=org");
        when(memberOfAttr.get(1)).thenReturn("cn=OTHER_GROUP,ou=groups,dc=corp,dc=acme,dc=org");
        when(memberOfAttr.get(2)).thenReturn("cn=ACME_READ_ONLY,ou=roles,dc=corp,dc=acme,dc=org");

        // When
        List<String> result = LdapAttributeUtil.getGroupsFromMemberOf(attrs);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("cn=ACME_READ_WRITE,ou=roles,dc=corp,dc=acme,dc=org"));
        assertTrue(result.contains("cn=OTHER_GROUP,ou=groups,dc=corp,dc=acme,dc=org"));
        assertTrue(result.contains("cn=ACME_READ_ONLY,ou=roles,dc=corp,dc=acme,dc=org"));
    }

    @Test
    void getGroupsFromMemberOf_shouldHandleNamingException() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        Attribute memberOfAttr = mock(Attribute.class);
        when(attrs.get("memberOf")).thenReturn(memberOfAttr);
        when(memberOfAttr.size()).thenReturn(1);
        when(memberOfAttr.get(0)).thenThrow(new NamingException("LDAP error"));

        // When
        List<String> result = LdapAttributeUtil.getGroupsFromMemberOf(attrs);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getGroupsFromMemberOf_shouldSkipNullGroupDns() throws NamingException {
        // Given
        Attributes attrs = mock(Attributes.class);
        Attribute memberOfAttr = mock(Attribute.class);
        when(attrs.get("memberOf")).thenReturn(memberOfAttr);
        when(memberOfAttr.size()).thenReturn(2);
        when(memberOfAttr.get(0)).thenReturn("cn=ACME_READ_WRITE,ou=roles,dc=corp,dc=acme,dc=org");
        when(memberOfAttr.get(1)).thenReturn(null);

        // When
        List<String> result = LdapAttributeUtil.getGroupsFromMemberOf(attrs);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains("cn=ACME_READ_WRITE,ou=roles,dc=corp,dc=acme,dc=org"));
    }
}
