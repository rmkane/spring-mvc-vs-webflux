package org.acme.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

@Configuration
@Profile("dev")
@Order(1)
public class DevSecurityConfig {

    @Bean
    public SecurityWebFilterChain devSecurityWebFilterChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher swaggerMatcher = exchange -> {
            String path = exchange.getRequest().getURI().getPath();
            boolean matches = path.startsWith("/swagger-ui")
                    || path.startsWith("/v3/api-docs")
                    || path.equals("/swagger-ui.html")
                    || path.equals("/error");
            return matches
                    ? ServerWebExchangeMatcher.MatchResult.match()
                    : ServerWebExchangeMatcher.MatchResult.notMatch();
        };

        return http
                .securityMatcher(swaggerMatcher)
                .authorizeExchange(auth -> auth
                        .anyExchange().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
