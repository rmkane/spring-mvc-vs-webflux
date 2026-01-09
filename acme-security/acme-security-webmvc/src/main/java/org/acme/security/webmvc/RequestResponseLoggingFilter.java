package org.acme.security.webmvc;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import lombok.extern.slf4j.Slf4j;

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.util.HttpHeaderFormatter;
import org.acme.security.webmvc.util.HttpUtils;

/**
 * Filter that logs request and response headers for debugging and monitoring
 * purposes. This filter executes once per request and logs all headers from
 * both the incoming request and the outgoing response in separate log
 * statements.
 */
@Slf4j
@Component
@Order(1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!log.isDebugEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip logging for Prometheus endpoint
        if (request.getRequestURI().equals(SecurityConstants.PROMETHEUS_ENDPOINT)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Log request headers
        log.debug(formatRequestHeaders(request));

        // Wrap response to capture headers
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            // Log response headers
            log.debug(formatResponseHeaders(responseWrapper));
            // Copy cached response body to actual response
            responseWrapper.copyBodyToResponse();
        }
    }

    private String formatRequestHeaders(HttpServletRequest request) {
        return """
                Request Headers:
                Method: %s %s
                Headers:
                %s""".formatted(
                request.getMethod(),
                request.getRequestURI(),
                HttpHeaderFormatter.formatHeaders(HttpUtils.getHeaders(request)));
    }

    private String formatResponseHeaders(HttpServletResponse response) {
        return """
                Response Headers:
                Status: %d
                Headers:
                %s""".formatted(
                response.getStatus(),
                HttpHeaderFormatter.formatHeaders(HttpUtils.getHeaders(response)));
    }
}
