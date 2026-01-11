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

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Get default values from environment variables
        String defaultSubjectDn = System.getenv("SSL_CLIENT_SUBJECT_DN");
        String defaultIssuerDn = System.getenv("SSL_CLIENT_ISSUER_DN");

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
            subjectDnDescription += " Default value from SSL_CLIENT_SUBJECT_DN: " + defaultSubjectDn;
        }

        String issuerDnDescription = "Issuer Distinguished Name (DN) from X509 client certificate. " +
                "This header is required for all API endpoints. Click 'Authorize' button to set this value globally.";
        if (defaultIssuerDn != null && !defaultIssuerDn.isEmpty()) {
            issuerDnDescription += " Default value from SSL_CLIENT_ISSUER_DN: " + defaultIssuerDn;
        }

        return new OpenAPI()
                .info(new Info()
                        .title("Acme API - WebFlux")
                        .version("1.0.0")
                        .description("REST API for managing books using Spring WebFlux. " +
                                "Use the 'Authorize' button above to set the ssl-client-subject-dn and ssl-client-issuer-dn headers for authentication.")
                        .contact(new Contact()
                                .name("Acme Team")
                                .email("api@acme.org")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("ssl-client-subject-dn")
                        .addList("ssl-client-issuer-dn"))
                .components(new Components()
                        .addSecuritySchemes("ssl-client-subject-dn", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("ssl-client-subject-dn")
                                .description(subjectDnDescription)
                                .extensions(subjectDnExtensions))
                        .addSecuritySchemes("ssl-client-issuer-dn", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("ssl-client-issuer-dn")
                                .description(issuerDnDescription)
                                .extensions(issuerDnExtensions)));
    }
}
