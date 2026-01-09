package org.acme.security.webflux;

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

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.model.UserInformation;
import org.acme.security.core.service.AuthenticationService;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
@Order(2)
public class WebFluxSecurityConfig {

    private final AuthenticationService authenticationService;

    @Bean
    public RequestResponseLoggingWebFilter requestResponseLoggingWebFilter() {
        return new RequestResponseLoggingWebFilter();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            RequestResponseLoggingWebFilter requestResponseLoggingWebFilter) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager());
        authenticationWebFilter.setServerAuthenticationConverter(serverAuthenticationConverter());

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .addFilterBefore(requestResponseLoggingWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
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
                dn = userInfo.getDn();
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
    public ServerAuthenticationConverter serverAuthenticationConverter() {
        return exchange -> {
            String dn = RequestHeaderExtractor.extractDn(exchange.getRequest());

            if (dn == null || dn.trim().isEmpty()) {
                return Mono.error(new BadCredentialsException("Missing or empty x-dn header"));
            }

            // Pass DN as String principal - will be converted to UserInformation
            // by AuthenticationService after looking up UserInfo from auth service
            return Mono.just(UsernamePasswordAuthenticationToken.unauthenticated(
                    dn.trim(),
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
