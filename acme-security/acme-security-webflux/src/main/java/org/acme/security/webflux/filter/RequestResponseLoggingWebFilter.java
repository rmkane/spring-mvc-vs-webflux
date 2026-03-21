package org.acme.security.webflux.filter;

import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import reactor.core.publisher.Mono;

import org.acme.security.core.policy.AcmeHeaderLoggingPolicy;
import org.acme.security.core.util.HttpHeaderFormatter;
import org.acme.security.webflux.util.HttpUtils;

/**
 * WebFilter that logs request and response headers for debugging. Controlled by
 * {@code acme.security.header-filter}.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RequestResponseLoggingWebFilter implements WebFilter {

    private static final String ALREADY_LOGGED_KEY = RequestResponseLoggingWebFilter.class.getName();

    private final AcmeHeaderLoggingPolicy headerLoggingPolicy;

    @Override
    @NonNull
    public Mono<Void> filter(
            @NonNull ServerWebExchange exchange,
            @NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        Map<String, List<String>> normalizedHeaders = HttpUtils.collectNormalizedHeadersForLoggingPolicy(request);

        if (!headerLoggingPolicy.shouldLog(log.isDebugEnabled(), path, normalizedHeaders)) {
            return chain.filter(exchange);
        }

        Boolean alreadyLogged = exchange.getAttribute(ALREADY_LOGGED_KEY);
        if (Boolean.TRUE.equals(alreadyLogged)) {
            return chain.filter(exchange);
        }
        exchange.getAttributes().put(ALREADY_LOGGED_KEY, Boolean.TRUE);

        ServerHttpResponse response = exchange.getResponse();
        MultiValueMap<String, String> reqHeaders = HttpHeaderFormatter.redactSensitiveHeaders(
                HttpUtils.getHeaders(request));
        log.debug(formatRequestHeaders(request, reqHeaders));

        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(response);

        return chain.filter(exchange.mutate().response(responseDecorator).build())
                .doFinally(signalType -> {
                    MultiValueMap<String, String> respHeaders = HttpHeaderFormatter.redactSensitiveHeaders(
                            HttpUtils.getHeaders(responseDecorator));
                    log.debug(formatResponseHeaders(responseDecorator, respHeaders));
                });
    }

    private String formatRequestHeaders(ServerHttpRequest request, MultiValueMap<String, String> headers) {
        return HttpHeaderFormatter.formatRequest(
                "Dumping request info:",
                request.getMethod().name(),
                request.getURI().getPath(),
                request.getURI().getRawQuery(),
                headers);
    }

    private String formatResponseHeaders(ServerHttpResponse response, MultiValueMap<String, String> headers) {
        return HttpHeaderFormatter.formatResponse(
                "Dumping response info:",
                response.getStatusCode().toString(),
                headers);
    }
}
