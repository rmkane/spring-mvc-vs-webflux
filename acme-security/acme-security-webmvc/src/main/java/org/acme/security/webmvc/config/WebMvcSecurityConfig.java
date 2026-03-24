package org.acme.security.webmvc.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.security.core.config.properties.HeadersProperties;
import org.acme.security.core.model.HeaderCertificatePrincipal;
import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.model.UserInformation;
import org.acme.security.core.service.AuthenticationService;
import org.acme.security.webmvc.filter.DnValidationFilter;
import org.acme.security.webmvc.filter.HeaderCertificatePreAuthenticatedProcessingFilter;
import org.acme.security.webmvc.filter.RequestResponseLoggingFilter;
import org.acme.security.webmvc.model.ErrorResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebMvcSecurityConfig {

    private final AuthenticationService authenticationService;
    private final HeadersProperties headersProperties;
    private final ObjectMapper objectMapper;

    public WebMvcSecurityConfig(
            AuthenticationService authenticationService,
            HeadersProperties headersProperties,
            ObjectMapper objectMapper) {
        this.authenticationService = authenticationService;
        this.headersProperties = headersProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestResponseLoggingFilter requestResponseLoggingFilter,
            DnValidationFilter dnValidationFilter,
            HeaderCertificatePreAuthenticatedProcessingFilter headerCertificatePreAuthenticatedProcessingFilter)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(dnValidationFilter, RequestHeaderAuthenticationFilter.class)
                .addFilterBefore(requestResponseLoggingFilter, RequestHeaderAuthenticationFilter.class)
                .addFilterBefore(headerCertificatePreAuthenticatedProcessingFilter,
                        RequestHeaderAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, authException) -> writeUnauthorizedResponse(response)));

        return http.build();
    }

    @Bean
    public HeaderCertificatePreAuthenticatedProcessingFilter headerCertificatePreAuthenticatedProcessingFilter(
            AuthenticationManager authenticationManager) {
        HeaderCertificatePreAuthenticatedProcessingFilter filter = new HeaderCertificatePreAuthenticatedProcessingFilter(
                headersProperties);
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return (authentication) -> {
            // Extract DN from principal (should be String from header)
            Object principal = authentication.getPrincipal();

            if (principal instanceof HeaderCertificatePrincipal headerPrincipal) {
                return authenticationService.createAuthenticatedAuthentication(headerPrincipal);
            }
            if (principal instanceof String principalString) {
                return authenticationService.createAuthenticatedAuthentication(principalString);
            }
            if (principal instanceof UserInformation userInfo) {
                return authenticationService.createAuthenticatedAuthentication(userInfo.getSubjectDn());
            }
            throw new BadCredentialsException("Invalid principal type: " + principal.getClass().getName());
        };
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = new ErrorResponse("Unauthorized", SecurityConstants.UNAUTHORIZED_MESSAGE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
