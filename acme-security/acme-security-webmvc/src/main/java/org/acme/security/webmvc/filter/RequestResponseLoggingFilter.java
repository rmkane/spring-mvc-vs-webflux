package org.acme.security.webmvc.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.acme.security.core.policy.AcmeHeaderLoggingPolicy;
import org.acme.security.core.util.HttpHeaderFormatter;
import org.acme.security.webmvc.util.HttpUtils;

/**
 * Filter that logs request and response headers for debugging and monitoring
 * purposes. Controlled by {@code acme.security.header-filter}.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private final AcmeHeaderLoggingPolicy headerLoggingPolicy;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        Map<String, List<String>> normalizedHeaders = HttpUtils.collectNormalizedHeadersForLoggingPolicy(request);

        if (!headerLoggingPolicy.shouldLog(log.isDebugEnabled(), path, normalizedHeaders)) {
            filterChain.doFilter(request, response);
            return;
        }

        MultiValueMap<String, String> requestHeaders = HttpHeaderFormatter.redactSensitiveHeaders(
                HttpUtils.getHeaders(request));
        log.debug(formatRequestHeaders(request, requestHeaders));

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            MultiValueMap<String, String> responseHeaders = HttpHeaderFormatter.redactSensitiveHeaders(
                    HttpUtils.getHeaders(responseWrapper));
            log.debug(formatResponseHeaders(responseWrapper, responseHeaders));
            responseWrapper.copyBodyToResponse();
        }
    }

    private String formatRequestHeaders(HttpServletRequest request, MultiValueMap<String, String> headers) {
        return HttpHeaderFormatter.formatRequest(
                "Dumping request info:",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                headers);
    }

    private String formatResponseHeaders(HttpServletResponse response, MultiValueMap<String, String> headers) {
        return HttpHeaderFormatter.formatResponse(
                "Dumping response info:",
                HttpStatus.valueOf(response.getStatus()).toString(),
                headers);
    }
}
