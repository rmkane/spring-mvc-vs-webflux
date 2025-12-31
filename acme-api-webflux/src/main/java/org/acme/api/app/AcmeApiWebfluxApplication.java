package org.acme.api.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.acme.api",
        "org.acme.auth",
        "org.acme.persistence.r2dbc",
        "org.acme.security.core",
        "org.acme.security.webflux"
}, exclude = { SecurityAutoConfiguration.class })
@EnableR2dbcRepositories(basePackages = "org.acme.persistence.r2dbc")
@EnableCaching
public class AcmeApiWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcmeApiWebfluxApplication.class, args);
    }
}
