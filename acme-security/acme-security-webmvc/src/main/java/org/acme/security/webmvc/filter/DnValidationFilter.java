package org.acme.security.webmvc.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.security.core.config.properties.HeadersProperties;
import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.util.PathMatcherUtil;
import org.acme.security.webmvc.model.ErrorResponse;

/**
 * Filter to validate that both the client certificate subject and issuer
 * headers are present. This runs before the RequestHeaderAuthenticationFilter
 * to ensure both headers are required.
 */
@Component
@Order(2)
public class DnValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final HeadersProperties headersProperties;

    public DnValidationFilter(ObjectMapper objectMapper, HeadersProperties headersProperties) {
        this.objectMapper = objectMapper;
        this.headersProperties = headersProperties;
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
        String subjectDn = request.getHeader(headersProperties.subjectDn());
        if (subjectDn == null || subjectDn.trim().isEmpty()) {
            writeErrorResponse(response,
                    String.format(SecurityConstants.MISSING_HEADER_MESSAGE, headersProperties.subjectDn()));
            return;
        }

        // Validate Issuer DN header (required)
        String issuerDn = request.getHeader(headersProperties.issuerDn());
        if (issuerDn == null || issuerDn.trim().isEmpty()) {
            writeErrorResponse(response,
                    String.format(SecurityConstants.MISSING_HEADER_MESSAGE, headersProperties.issuerDn()));
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
