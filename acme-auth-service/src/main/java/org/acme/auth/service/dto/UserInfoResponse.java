package org.acme.auth.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponse {

    private final String dn;
    private final String givenName;
    private final String surname;
    private final List<String> roles;
}
