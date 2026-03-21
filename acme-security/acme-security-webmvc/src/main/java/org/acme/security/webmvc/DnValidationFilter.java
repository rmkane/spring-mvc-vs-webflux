package org.acme.security.webmvc;

import java.io.IOException;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.util.PathMatcherUtil;

/**
 * Filter to validate that both the client certificate subject and issuer
 * headers are present. This runs before the RequestHeaderAuthenticationFilter
 * to ensure both headers are required.
 */
@Component
@Order(1)
public class DnValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final String subjectDnHeader;
    private final String issuerDnHeader;

    public DnValidationFilter(
            ObjectMapper objectMapper,
            @Value("${acme.security.headers.subject-dn:#{null}}") String subjectDnHeader,
            @Value("${acme.security.headers.issuer-dn:#{null}}") String issuerDnHeader) {
        this.objectMapper = objectMapper;
        this.subjectDnHeader = Objects.requireNonNullElse(subjectDnHeader, SecurityConstants.SSL_CLIENT_SUBJECT_HEADER);
        this.issuerDnHeader = Objects.requireNonNullElse(issuerDnHeader, SecurityConstants.SSL_CLIENT_ISSUER_HEADER);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip validation for public endpoints
        String path = request.getRequestURI();
        if (PathMatcherUtil.isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate Subject DN header
        String subjectDn = request.getHeader(subjectDnHeader);
        if (subjectDn == null || subjectDn.trim().isEmpty()) {
            writeErrorResponse(response, String.format(SecurityConstants.MISSING_HEADER_MESSAGE, subjectDnHeader));
            return;
        }

        // Validate Issuer DN header (required)
        String issuerDn = request.getHeader(issuerDnHeader);
        if (issuerDn == null || issuerDn.trim().isEmpty()) {
            writeErrorResponse(response, String.format(SecurityConstants.MISSING_HEADER_MESSAGE, issuerDnHeader));
            return;
        }

        // Both headers are present, continue with the filter chain
        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse("Unauthorized", message);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
