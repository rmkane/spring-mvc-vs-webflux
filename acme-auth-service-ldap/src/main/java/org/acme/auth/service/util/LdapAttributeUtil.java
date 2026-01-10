package org.acme.auth.service.util;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for extracting values from LDAP attributes.
 * <p>
 * Provides safe extraction of attribute values from LDAP {@link Attributes}
 * objects, handling null checks and exceptions gracefully.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LdapAttributeUtil {

    /**
     * Extracts the value of an attribute from the LDAP attributes.
     * <p>
     * Returns the first value of the specified attribute as a string. If the
     * attribute is not found, is null, or an error occurs during extraction, null
     * is returned.
     *
     * @param attrs         the LDAP attributes
     * @param attributeName the name of the attribute to extract
     * @return the attribute value as a string, or null if not found or error occurs
     */
    public static String getAttributeValue(Attributes attrs, String attributeName) {
        if (attrs == null || !StringUtils.hasText(attributeName)) {
            return null;
        }
        try {
            Attribute attr = attrs.get(attributeName);
            if (attr == null || attr.get() == null) {
                return null;
            }
            return attr.get().toString();
        } catch (NamingException e) {
            log.debug("Error extracting attribute '{}' from LDAP attributes", attributeName, e);
            return null;
        }
    }

    /**
     * Extracts all group DNs from the memberOf attribute.
     * <p>
     * The memberOf attribute contains DNs of groups that the user belongs to. This
     * method extracts all group DNs without any filtering.
     * <p>
     * Note: memberOf is automatically maintained by memberOf overlay when user is
     * added to groups.
     *
     * @param attrs the LDAP attributes
     * @return list of group DNs, or empty list if memberOf attribute is not present
     */
    public static List<String> getGroupsFromMemberOf(Attributes attrs) {
        List<String> groupDns = new ArrayList<>();
        if (attrs == null) {
            return groupDns;
        }
        Attribute memberOfAttr = attrs.get("memberOf");
        if (memberOfAttr == null) {
            return groupDns;
        }
        try {
            for (int i = 0; i < memberOfAttr.size(); i++) {
                String groupDn = (String) memberOfAttr.get(i);
                if (groupDn != null) {
                    groupDns.add(groupDn);
                }
            }
        } catch (NamingException e) {
            log.error("Error extracting groups from memberOf attribute", e);
        }
        return groupDns;
    }
}
