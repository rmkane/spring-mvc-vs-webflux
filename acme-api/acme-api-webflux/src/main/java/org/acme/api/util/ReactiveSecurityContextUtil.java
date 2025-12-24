package org.acme.api.util;

import org.acme.security.core.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import reactor.core.publisher.Mono;

public class ReactiveSecurityContextUtil {

    public static Mono<UserPrincipal> getCurrentUserPrincipal() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .cast(Authentication.class)
                .map(Authentication::getPrincipal)
                .cast(UserPrincipal.class)
                .switchIfEmpty(Mono.error(new IllegalStateException("No authenticated user found in SecurityContext")));
    }
}
