package org.acme.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Acme API - MVC")
                        .version("1.0.0")
                        .description("REST API for managing books using Spring MVC. " +
                                "Use the 'Authorize' button above to set the x-username header for authentication.")
                        .contact(new Contact()
                                .name("Acme Team")
                                .email("api@acme.org")))
                .addSecurityItem(new SecurityRequirement().addList("x-username"))
                .components(new Components()
                        .addSecuritySchemes("x-username", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("x-username")
                                .description("Username for authentication. Enter any username (e.g., 'Bob', 'Alice') to authenticate. " +
                                        "This header is required for all API endpoints. Click 'Authorize' button to set this value globally.")));
    }
}

