package org.acme.api.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import org.acme.security.core.model.SecurityConstants;

@Configuration
@Profile("dev")
@Order(1)
public class DevSecurityConfig {

    @Bean
    public SecurityWebFilterChain devSecurityWebFilterChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher publicEndpointsMatcher = exchange -> {
            String path = exchange.getRequest().getURI().getPath();
            boolean matches = Arrays.stream(SecurityConstants.PUBLIC_ENDPOINTS)
                    .anyMatch(pattern -> {
                        if (pattern.endsWith("/**")) {
                            return path.startsWith(pattern.substring(0, pattern.length() - 3));
                        }
                        return path.equals(pattern);
                    });
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
