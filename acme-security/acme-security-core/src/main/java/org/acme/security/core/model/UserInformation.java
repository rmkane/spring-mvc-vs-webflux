package org.acme.security.core.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInformation {

    private final String dn;
    private final String givenName;
    private final String surname;
}
