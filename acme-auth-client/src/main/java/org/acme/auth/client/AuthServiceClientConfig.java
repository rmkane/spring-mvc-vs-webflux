package org.acme.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for AuthServiceClient bean. Provides a RestClient configured
 * with the auth service base URL.
 */
@Configuration
public class AuthServiceClientConfig {

    @Bean
    public AuthServiceClient authServiceClient(
            @Value("${auth.service.base-url:http://localhost:8082}") String authServiceBaseUrl) {
        RestClient restClient = RestClient.builder()
                .baseUrl(authServiceBaseUrl)
                .build();

        return new AuthServiceClient(restClient);
    }
}
