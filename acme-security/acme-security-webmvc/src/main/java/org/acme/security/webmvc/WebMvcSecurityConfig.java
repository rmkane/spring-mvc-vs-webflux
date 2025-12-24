package org.acme.security.webmvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.acme.security.core.AuthenticationService;
import org.acme.security.core.SecurityConstants;
import org.acme.security.core.UserInformation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebMvcSecurityConfig {

    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(requestHeaderAuthenticationFilter(), RequestHeaderAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            writeUnauthorizedResponse(response);
                        }));

        return http.build();
    }

    @Bean
    public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader(SecurityConstants.USERNAME_HEADER);
        filter.setExceptionIfHeaderMissing(false);
        filter.setAuthenticationManager(authenticationManager());
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new AuthenticationManager() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                // Extract username from principal (should be String from header)
                Object principal = authentication.getPrincipal();
                String username;

                if (principal instanceof String principalString) {
                    username = principalString;
                } else if (principal instanceof UserInformation userInfo) {
                    username = userInfo.getUsername();
                } else {
                    throw new BadCredentialsException("Invalid principal type: " + principal.getClass().getName());
                }

                // Pass username to auth service, which will:
                // 1. Lookup UserPrincipal from auth layer
                // 2. Create UserInformation (derivative) from UserPrincipal
                // 3. Return authenticated Authentication with UserInformation as principal
                return authenticationService.createAuthenticatedAuthentication(username);
            }
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
