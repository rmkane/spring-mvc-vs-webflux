package org.acme.security.webflux.util;

import org.springframework.web.server.ServerWebExchange;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.acme.security.core.model.AcmeHeaderLoggingAttributes;

/**
 * {@link ServerWebExchange} attributes for
 * {@link AcmeHeaderLoggingAttributes#ATTRIBUTE_NAME}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmeHeaderLoggingExchangeAttributes {

    public static void put(ServerWebExchange exchange, boolean suppressed) {
        AcmeHeaderLoggingAttributes.put(exchange.getAttributes(), suppressed);
    }

    public static void clear(ServerWebExchange exchange) {
        AcmeHeaderLoggingAttributes.clear(exchange.getAttributes());
    }

    public static boolean isSuppressed(ServerWebExchange exchange) {
        return AcmeHeaderLoggingAttributes.isSuppressed(exchange.getAttributes());
    }
}
