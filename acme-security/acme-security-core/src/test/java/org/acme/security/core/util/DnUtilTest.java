package org.acme.security.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DnUtilTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource(delimiter = '|', textBlock = """
            # Description                   | Input DN                                                            | Expected Output
            Valid DN                        | CN=John Doe,OU=Engineering,DC=example,DC=com                        | cn=john doe,ou=engineering,dc=example,dc=com
            Leading whitespace              | '  CN=John Doe,OU=Engineering,DC=example,DC=com'                    | cn=john doe,ou=engineering,dc=example,dc=com
            Trailing whitespace             | 'CN=John Doe,OU=Engineering,DC=example,DC=com  '                    | cn=john doe,ou=engineering,dc=example,dc=com
            Leading and trailing whitespace | '  CN=John Doe,OU=Engineering,DC=example,DC=com  '                  | cn=john doe,ou=engineering,dc=example,dc=com
            Mixed case                      | Cn=JoHn DoE,Ou=EnGiNeErInG,Dc=ExAmPlE,Dc=CoM                        | cn=john doe,ou=engineering,dc=example,dc=com
            Already normalized              | cn=john doe,ou=engineering,dc=example,dc=com                        | cn=john doe,ou=engineering,dc=example,dc=com
            Special characters              | CN=User+UID=123,OU=Test,DC=example,DC=com                           | cn=user+uid=123,ou=test,dc=example,dc=com
            Spaces around commas            | 'CN=John Doe , OU=Engineering , DC=example , DC=com'                | cn=john doe,ou=engineering,dc=example,dc=com
            Spaces before commas            | 'CN=John Doe ,OU=Engineering ,DC=example ,DC=com'                   | cn=john doe,ou=engineering,dc=example,dc=com
            Spaces after commas             | 'CN=John Doe, OU=Engineering, DC=example, DC=com'                   | cn=john doe,ou=engineering,dc=example,dc=com
            Multiple consecutive spaces     | 'CN=John    Doe,OU=Engineering,DC=example,DC=com'                   | cn=john doe,ou=engineering,dc=example,dc=com
            Tabs and newlines               | 'CN=John\tDoe,OU=Engineering\n,DC=example,DC=com'                   | cn=john doe,ou=engineering,dc=example,dc=com
            Spaces around equals            | 'CN = John Doe, OU = Engineering, DC = example, DC = com'           | cn=john doe,ou=engineering,dc=example,dc=com
            Spaces before equals            | 'CN =John Doe,OU =Engineering,DC =example,DC =com'                  | cn=john doe,ou=engineering,dc=example,dc=com
            Spaces after equals             | 'CN= John Doe,OU= Engineering,DC= example,DC= com'                  | cn=john doe,ou=engineering,dc=example,dc=com
            Complex whitespace              | '  CN = John    Doe , OU = Engineering , DC = example , DC = com  ' | cn=john doe,ou=engineering,dc=example,dc=com
            """)
    void normalize_variousFormats(String description, String input, String expected) {
        String result = DnUtil.normalize(input);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "null or empty: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t", "\n", "  \t\n  " })
    void normalize_nullOrBlank(String input) {
        String result = DnUtil.normalize(input);
        assertNull(result);
    }
}
