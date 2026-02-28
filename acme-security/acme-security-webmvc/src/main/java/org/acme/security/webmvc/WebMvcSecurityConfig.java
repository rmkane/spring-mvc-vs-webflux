package org.acme.security.webmvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
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

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.model.UserInformation;
import org.acme.security.core.service.AuthenticationService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebMvcSecurityConfig {

    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper;
    private final String subjectDnHeader;

    public WebMvcSecurityConfig(
            AuthenticationService authenticationService,
            ObjectMapper objectMapper,
            @Value("${acme.security.headers.subject-dn:#{null}}") String subjectDnHeader) {
        this.authenticationService = authenticationService;
        this.objectMapper = objectMapper;
        this.subjectDnHeader = Objects.requireNonNullElse(subjectDnHeader, SecurityConstants.SSL_CLIENT_SUBJECT_HEADER);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestResponseLoggingFilter requestResponseLoggingFilter,
            DnValidationFilter dnValidationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(dnValidationFilter, RequestHeaderAuthenticationFilter.class)
                .addFilterBefore(requestResponseLoggingFilter, RequestHeaderAuthenticationFilter.class)
                .addFilterBefore(requestHeaderAuthenticationFilter(), RequestHeaderAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, authException) -> writeUnauthorizedResponse(response)));

        return http.build();
    }

    @Bean
    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(subjectDnHeader);
        filter.setExceptionIfHeaderMissing(false);
        filter.setAuthenticationManager(authenticationManager());
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return (authentication) -> {
            // Extract DN from principal (should be String from header)
            Object principal = authentication.getPrincipal();
            String dn;

            if (principal instanceof String principalString) {
                dn = principalString;
            } else if (principal instanceof UserInformation userInfo) {
                dn = userInfo.getSubjectDn();
            } else {
                throw new BadCredentialsException("Invalid principal type: " + principal.getClass().getName());
            }

            // Note: Both Subject and Issuer DN validation is done in the filter before
            // reaching here
            // Pass DN to auth service, which will:
            // 1. Look up UserInfo from auth service by DN
            // 2. Create UserInformation (derivative) from UserInfo
            // 3. Return authenticated Authentication with UserInformation as principal
            return authenticationService.createAuthenticatedAuthentication(dn);
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
