package org.acme.security.core;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInformation {

    private final String username;
}
