package org.acme.api.config;

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
        return new OpenAPI()
                .info(new Info()
                        .title("Acme API - WebFlux")
                        .version("1.0.0")
                        .description("REST API for managing books using Spring WebFlux. " +
                                "Use the 'Authorize' button above to set the x-dn header for authentication.")
                        .contact(new Contact()
                                .name("Acme Team")
                                .email("api@acme.org")))
                .addSecurityItem(new SecurityRequirement().addList("x-dn"))
                .components(new Components()
                        .addSecuritySchemes("x-dn", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("x-dn")
                                .description(
                                        "Distinguished Name (DN) for authentication. Enter a user DN (e.g., 'cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org') to authenticate. "
                                                +
                                                "This header is required for all API endpoints. Click 'Authorize' button to set this value globally.")));
    }
}
