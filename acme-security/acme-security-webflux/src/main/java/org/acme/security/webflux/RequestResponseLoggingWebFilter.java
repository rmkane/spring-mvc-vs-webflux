package org.acme.security.webflux;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;

import org.acme.security.core.model.SecurityConstants;
import org.acme.security.core.util.HttpHeaderFormatter;
import org.acme.security.webflux.util.HttpUtils;

/**
 * WebFilter that logs request and response headers for debugging and monitoring
 * purposes. This filter intercepts all requests and logs headers from both the
 * incoming request and the outgoing response in separate log statements in a
 * reactive, non-blocking manner.
 */
@Slf4j
public class RequestResponseLoggingWebFilter implements WebFilter {

    private static final String ALREADY_LOGGED_ATTRIBUTE = String.format("%s.ALREADY_LOGGED",
            RequestResponseLoggingWebFilter.class.getName());

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        if (!log.isDebugEnabled()) {
            return chain.filter(exchange);
        }

        // Skip logging for Prometheus endpoint
        if (request.getURI().getPath().equals(SecurityConstants.PROMETHEUS_ENDPOINT)) {
            return chain.filter(exchange);
        }

        // Ensure we only log once per request, even if this filter is called multiple
        // times (e.g., if multiple SecurityWebFilterChains are evaluated)
        Boolean alreadyLogged = exchange.getAttribute(ALREADY_LOGGED_ATTRIBUTE);
        if (Boolean.TRUE.equals(alreadyLogged)) {
            return chain.filter(exchange);
        }

        // Mark as logged for this exchange
        exchange.getAttributes().put(ALREADY_LOGGED_ATTRIBUTE, Boolean.TRUE);

        // Log request headers
        log.debug(formatRequestHeaders(request));

        // Wrap response to capture headers
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(response);

        return chain.filter(exchange.mutate().response(responseDecorator).build())
                .doFinally(signalType -> {
                    // Log response headers
                    log.debug(formatResponseHeaders(responseDecorator));
                });
    }

    private String formatRequestHeaders(ServerHttpRequest request) {
        return """
                Request Headers:
                Method: %s %s
                Headers:
                %s""".formatted(
                request.getMethod(),
                request.getURI().getPath(),
                HttpHeaderFormatter.formatHeaders(HttpUtils.getHeaders(request)));
    }

    private String formatResponseHeaders(ServerHttpResponse response) {
        return """
                Response Headers:
                Status: %s
                Headers:
                %s""".formatted(
                response.getStatusCode(),
                HttpHeaderFormatter.formatHeaders(HttpUtils.getHeaders(response)));
    }
}
