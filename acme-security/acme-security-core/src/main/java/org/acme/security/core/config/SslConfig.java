package org.acme.security.core.config;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * SSL configuration for auth service client communication.
 * <p>
 * Configures SSL/TLS for RestClient to communicate with the auth service over
 * HTTPS. Loads keystore and truststore from configuration properties.
 * <p>
 * This configuration is only active when {@code auth.service.ssl.enabled=true}
 * is set.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "auth.service.ssl.enabled", havingValue = "true")
public class SslConfig {

    @Bean
    public ClientHttpRequestFactory sslClientHttpRequestFactory(
            @Value("${auth.service.ssl.truststore.path}") Resource truststoreResource,
            @Value("${auth.service.ssl.truststore.password}") String truststorePassword,
            @Value("${auth.service.ssl.truststore.type:JKS}") String truststoreType,
            @Value("${auth.service.ssl.keystore.path:#{null}}") Resource keystoreResource,
            @Value("${auth.service.ssl.keystore.password:#{null}}") String keystorePassword,
            @Value("${auth.service.ssl.keystore.type:JKS}") String keystoreType) throws Exception {

        log.info("Configuring SSL for auth service client");

        SSLContext sslContext = createSslContext(truststoreResource, truststorePassword, truststoreType,
                keystoreResource, keystorePassword, keystoreType);

        // Note: SSLConnectionSocketFactory and setSSLSocketFactory are deprecated in
        // HttpClient 5, but they are the only available APIs for configuring SSL. The
        // SSLContext creation uses standard Java APIs (non-deprecated), but the
        // HttpClient integration requires these deprecated methods until HttpClient 5
        // provides a replacement API.
        @SuppressWarnings("deprecation")
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext,
                new DefaultHostnameVerifier());

        @SuppressWarnings("deprecation")
        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    private SSLContext createSslContext(Resource truststoreResource, String truststorePassword, String truststoreType,
            Resource keystoreResource, String keystorePassword, String keystoreType) throws Exception {

        // Use standard Java SSLContext initialization (non-deprecated)
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Initialize KeyManagerFactory (for client certificates)
        KeyManagerFactory keyManagerFactory = null;
        if (keystoreResource != null && keystoreResource.exists() && keystorePassword != null) {
            log.debug("Loading keystore from: {}", keystoreResource);
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            try (InputStream is = keystoreResource.getInputStream()) {
                keystore.load(is, keystorePassword.toCharArray());
            }
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, keystorePassword.toCharArray());
            log.debug("Keystore loaded successfully");
        } else if (keystoreResource != null) {
            log.debug("Keystore not configured, skipping client certificate authentication");
        }

        // Initialize TrustManagerFactory (for server certificate verification)
        TrustManagerFactory trustManagerFactory = null;
        if (truststoreResource != null && truststoreResource.exists()) {
            log.debug("Loading truststore from: {}", truststoreResource);
            KeyStore truststore = KeyStore.getInstance(truststoreType);
            try (InputStream is = truststoreResource.getInputStream()) {
                truststore.load(is, truststorePassword.toCharArray());
            }
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            log.debug("Truststore loaded successfully");
        } else {
            log.warn("Truststore resource not found or not specified, using default truststore");
        }

        // Initialize SSLContext with KeyManagers and TrustManagers
        sslContext.init(
                keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null,
                new SecureRandom());

        return sslContext;
    }
}
