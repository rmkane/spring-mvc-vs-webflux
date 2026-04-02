package org.acme.security.core.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ HeaderFilterProperties.class, HeadersProperties.class })
public class AcmeSecurityPropertiesConfiguration {
}
