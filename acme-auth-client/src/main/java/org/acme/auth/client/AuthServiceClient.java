package org.acme.auth.client;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST client for calling the standalone auth service to lookup users.
 */
@Slf4j
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestClient restClient;

    /**
     * Looks up a user by DN from the auth service.
     *
     * @param dn the Distinguished Name to lookup
     * @return UserInfo with DN, name, and roles
     * @throws BadCredentialsException if user not found or service unavailable
     */
    public UserInfo lookupUser(String dn) {
        log.debug("Calling auth service for user DN: {}", dn);

        try {
            UserInfoResponse response = restClient.get()
                    .uri("/api/auth/users/{dn}", dn)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response1) -> {
                        log.warn("User not found in auth service: {}", dn);
                        throw new BadCredentialsException("User not found: " + dn);
                    })
                    .body(UserInfoResponse.class);

            if (response == null) {
                log.warn("User not found in auth service: {}", dn);
                throw new BadCredentialsException("User not found: " + dn);
            }

            log.debug("User found: dn={}, givenName={}, surname={}, roles={}",
                    response.dn(), response.givenName(), response.surname(), response.roles());

            return UserInfo.builder()
                    .dn(response.dn())
                    .givenName(response.givenName())
                    .surname(response.surname())
                    .roles(response.roles())
                    .build();
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                throw e;
            }
            log.error("Error calling auth service for user DN: {}", dn, e);
            throw new BadCredentialsException("Authentication service error: " + e.getMessage(), e);
        }
    }

    /**
     * Response DTO from auth service. This matches the structure of
     * org.acme.auth.service.dto.UserInfoResponse but is defined here to avoid
     * creating a dependency from the client to the service module.
     */
    public record UserInfoResponse(String dn, String givenName, String surname, List<String> roles) {
    }
}
