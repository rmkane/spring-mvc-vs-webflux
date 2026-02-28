package org.acme.security.webflux;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.model.UserInformation;
import org.acme.security.core.service.AuthenticationService;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Order(2)
public class WebFluxSecurityConfig {

    private final AuthenticationService authenticationService;
    private final String subjectDnHeader;
    private final String issuerDnHeader;

    public WebFluxSecurityConfig(
            AuthenticationService authenticationService,
            @Value("${acme.security.headers.subject-dn:#{null}}") String subjectDnHeader,
            @Value("${acme.security.headers.issuer-dn:#{null}}") String issuerDnHeader) {
        this.authenticationService = authenticationService;
        this.subjectDnHeader = Objects.requireNonNullElse(subjectDnHeader, SecurityConstants.SSL_CLIENT_SUBJECT_HEADER);
        this.issuerDnHeader = Objects.requireNonNullElse(issuerDnHeader, SecurityConstants.SSL_CLIENT_ISSUER_HEADER);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager());
        authenticationWebFilter.setServerAuthenticationConverter(serverAuthenticationConverter());

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyExchange().authenticated())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authenticationManager(reactiveAuthenticationManager())
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .build();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return authentication -> {
            // Extract DN from principal (should be String from header)
            Object principal = authentication.getPrincipal();
            String dn;

            if (principal instanceof String principalString) {
                dn = principalString;
            } else if (principal instanceof UserInformation userInfo) {
                dn = userInfo.getSubjectDn();
            } else {
                return Mono.error(new BadCredentialsException(
                        "Invalid principal type: " + principal.getClass().getName()));
            }

            // Wrap blocking authentication in Mono.fromCallable() to run on blocking
            // scheduler
            // This prevents blocking the reactive event loop thread
            return Mono.fromCallable(() -> {
                try {
                    // Pass DN to auth service, which will:
                    // 1. Look up UserInfo from auth service by DN
                    // 2. Create UserInformation (derivative) from UserInfo
                    // 3. Return authenticated Authentication with UserInformation as principal
                    return authenticationService.createAuthenticatedAuthentication(dn);
                } catch (BadCredentialsException e) {
                    throw e;
                }
            })
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorMap(BadCredentialsException.class, e -> e);
        };
    }

    @Bean
    @SuppressWarnings("null") // getFirst() returns nullable; we validate before use
    public ServerAuthenticationConverter serverAuthenticationConverter() {
        return exchange -> {
            // Check if this is a public endpoint
            String path = exchange.getRequest().getPath().value();
            for (String publicEndpoint : SecurityConstants.PUBLIC_ENDPOINTS) {
                if (path.matches(publicEndpoint.replace("**", ".*"))) {
                    // Skip validation for public endpoints
                    return Mono.empty();
                }
            }

            // Validate Subject DN header
            String dnValue = exchange.getRequest().getHeaders().getFirst(subjectDnHeader);
            if (dnValue == null || dnValue.trim().isEmpty()) {
                return Mono.error(new BadCredentialsException(
                        String.format(SecurityConstants.MISSING_HEADER_MESSAGE, subjectDnHeader)));
            }
            String dn = dnValue.trim();

            // Validate Issuer DN header (required)
            String issuerDnValue = exchange.getRequest().getHeaders().getFirst(issuerDnHeader);
            if (issuerDnValue == null || issuerDnValue.trim().isEmpty()) {
                return Mono.error(new BadCredentialsException(
                        String.format(SecurityConstants.MISSING_HEADER_MESSAGE, issuerDnHeader)));
            }

            // Both headers are present, pass DN as String principal
            // Will be converted to UserInformation by AuthenticationService after looking
            // up UserInfo from auth service
            return Mono.just(UsernamePasswordAuthenticationToken.unauthenticated(
                    dn,
                    null));
        };
    }

    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }
}
