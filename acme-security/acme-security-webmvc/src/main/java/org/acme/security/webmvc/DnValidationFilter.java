package org.acme.security.webmvc;

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

import lombok.RequiredArgsConstructor;

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.util.PathMatcherUtil;

/**
 * Filter to validate that both ssl-client-subject-dn and ssl-client-issuer-dn
 * headers are present. This runs before the RequestHeaderAuthenticationFilter
 * to ensure both headers are required.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class DnValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

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
        String subjectDn = RequestHeaderExtractor.extractSubjectDn(request);
        if (subjectDn == null || subjectDn.trim().isEmpty()) {
            writeErrorResponse(response, SecurityConstants.MISSING_DN_MESSAGE);
            return;
        }

        // Validate Issuer DN header (required)
        String issuerDn = RequestHeaderExtractor.extractIssuerDn(request);
        if (issuerDn == null || issuerDn.trim().isEmpty()) {
            writeErrorResponse(response, SecurityConstants.MISSING_ISSUER_DN_MESSAGE);
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
