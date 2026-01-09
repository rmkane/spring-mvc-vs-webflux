package org.acme.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.util.PathMatcherUtil;

@Configuration
@Profile("dev")
@Order(1)
public class DevSecurityConfig {

    @Bean
    public SecurityWebFilterChain devSecurityWebFilterChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher publicEndpointsMatcher = exchange -> {
            String path = exchange.getRequest().getURI().getPath();
            boolean matches = PathMatcherUtil.isPublicEndpoint(path);
            return matches
                    ? ServerWebExchangeMatcher.MatchResult.match()
                    : ServerWebExchangeMatcher.MatchResult.notMatch();
        };

        return http
                .securityMatcher(publicEndpointsMatcher)
                .authorizeExchange(auth -> auth
                        .pathMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyExchange().authenticated())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
