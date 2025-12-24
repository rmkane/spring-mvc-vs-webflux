package org.acme.api.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.acme.api",
        "org.acme.persistence.r2dbc",
        "org.acme.security"
})
@EnableR2dbcRepositories(basePackages = "org.acme.persistence.r2dbc")
public class AcmeApiWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcmeApiWebfluxApplication.class, args);
    }
}
