package org.acme.auth.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration for AuthServiceClient bean. Provides a RestClient configured
 * with the auth service base URL.
 * <p>
 * If SSL is enabled (via {@code auth.service.ssl.enabled=true}), this will use
 * the SSL-configured ClientHttpRequestFactory provided by the security module's
 * SslConfig. Otherwise, it creates a default RestClient.
 */
@Configuration
public class AuthServiceClientConfig {

    @Bean
    public AuthServiceClient authServiceClient(
            @Value("${auth.service.base-url:http://localhost:8082}") String authServiceBaseUrl,
            @Autowired(required = false) Optional<ClientHttpRequestFactory> sslClientHttpRequestFactory) {

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(authServiceBaseUrl);

        sslClientHttpRequestFactory.ifPresent(builder::requestFactory);

        return new AuthServiceClient(builder.build());
    }
}
