package org.acme.api.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.acme.security.core.config.properties.HeadersProperties;

@Configuration
public class OpenApiConfig {
    private static final String SSL_CLIENT_SUBJECT_DN_ENV = "SSL_CLIENT_SUBJECT_DN";
    private static final String SSL_CLIENT_ISSUER_DN_ENV = "SSL_CLIENT_ISSUER_DN";

    private final HeadersProperties headersProperties;

    public OpenApiConfig(HeadersProperties headersProperties) {
        this.headersProperties = headersProperties;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        // Get default values from environment variables
        String defaultSubjectDn = System.getenv(SSL_CLIENT_SUBJECT_DN_ENV);
        String defaultIssuerDn = System.getenv(SSL_CLIENT_ISSUER_DN_ENV);

        // Create extensions map for default values (SpringDoc OpenAPI supports this)
        Map<String, Object> subjectDnExtensions = new HashMap<>();
        if (defaultSubjectDn != null && !defaultSubjectDn.isEmpty()) {
            subjectDnExtensions.put("x-default-value", defaultSubjectDn);
        }

        Map<String, Object> issuerDnExtensions = new HashMap<>();
        if (defaultIssuerDn != null && !defaultIssuerDn.isEmpty()) {
            issuerDnExtensions.put("x-default-value", defaultIssuerDn);
        }

        // Build description with default values if available
        String subjectDnDescription = "Subject Distinguished Name (DN) from X509 client certificate. " +
                "This header is required for all API endpoints. Click 'Authorize' button to set this value globally.";
        if (defaultSubjectDn != null && !defaultSubjectDn.isEmpty()) {
            subjectDnDescription += " Default value from " + SSL_CLIENT_SUBJECT_DN_ENV + ": " + defaultSubjectDn;
        }

        String issuerDnDescription = "Issuer Distinguished Name (DN) from X509 client certificate. " +
                "This header is required for all API endpoints. Click 'Authorize' button to set this value globally.";
        if (defaultIssuerDn != null && !defaultIssuerDn.isEmpty()) {
            issuerDnDescription += " Default value from " + SSL_CLIENT_ISSUER_DN_ENV + ": " + defaultIssuerDn;
        }

        return new OpenAPI()
                .info(new Info()
                        .title("Acme API - WebFlux")
                        .version("1.0.0")
                        .description("REST API for managing books using Spring WebFlux. " +
                                "Use the 'Authorize' button above to set the " + headersProperties.subjectDn() + " and "
                                + headersProperties.issuerDn() + " headers for authentication.")
                        .contact(new Contact()
                                .name("Acme Team")
                                .email("api@acme.org")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(headersProperties.subjectDn())
                        .addList(headersProperties.issuerDn()))
                .components(new Components()
                        .addSecuritySchemes(headersProperties.subjectDn(), new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(headersProperties.subjectDn())
                                .description(subjectDnDescription)
                                .extensions(subjectDnExtensions))
                        .addSecuritySchemes(headersProperties.issuerDn(), new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(headersProperties.issuerDn())
                                .description(issuerDnDescription)
                                .extensions(issuerDnExtensions)));
    }
}
