package org.acme.security.webmvc.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.security.core.config.properties.HeaderFilterProperties;
import org.acme.security.core.config.properties.HeadersProperties;
import org.acme.security.core.listener.AbstractAcmeSecurityStartupListener;
import org.acme.security.core.policy.AcmeHeaderLoggingPolicy;

/**
 * MVC: logs security header / header-logging policy at startup.
 */
@Component
public class WebMvcSecurityStartupListener extends AbstractAcmeSecurityStartupListener {

    private final AcmeHeaderLoggingPolicy headerLoggingPolicy;

    public WebMvcSecurityStartupListener(
            HeaderFilterProperties headerFilterProperties,
            HeadersProperties headersProperties,
            ObjectMapper objectMapper,
            AcmeHeaderLoggingPolicy headerLoggingPolicy) {
        super(headerFilterProperties, headersProperties, objectMapper);
        this.headerLoggingPolicy = headerLoggingPolicy;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logPolicySnapshotAtStartup() {
        logSecurityHeaderPolicySnapshot(headerLoggingPolicy.getIgnoredHeaderMatchers());
    }
}
