package org.acme.security.webflux;

import org.acme.security.core.AuthenticationService;
import org.acme.security.core.UserInformationUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class WebFluxSecurityConfig {

    private final AuthenticationService authenticationService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager());
        authenticationWebFilter.setServerAuthenticationConverter(serverAuthenticationConverter());

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(auth -> auth
                        .anyExchange().authenticated())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authenticationManager(reactiveAuthenticationManager())
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .build();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return authentication -> {
            // Wrap blocking authentication in Mono.fromCallable() to run on blocking
            // scheduler
            // This prevents blocking the reactive event loop thread
            return Mono.fromCallable(() -> {
                try {
                    return authenticationService
                            .createAuthenticatedAuthentication(authentication.getPrincipal());
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
            String username = RequestHeaderExtractor.extractUsername(exchange.getRequest());

            try {
                var userInformation = UserInformationUtil.fromUsername(username);
                return Mono.just(UsernamePasswordAuthenticationToken.unauthenticated(
                        userInformation,
                        null));
            } catch (BadCredentialsException e) {
                return Mono.error(e);
            }
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
