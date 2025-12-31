package org.acme.api.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import reactor.core.publisher.Mono;

import org.acme.security.core.model.UserInformation;

public class ReactiveSecurityContextUtil {

    public static Mono<UserInformation> getCurrentUserInformation() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(UserInformation.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("No authenticated user found in SecurityContext")));
    }
}
