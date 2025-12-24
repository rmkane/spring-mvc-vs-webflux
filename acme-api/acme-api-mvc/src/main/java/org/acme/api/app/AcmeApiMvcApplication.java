package org.acme.api.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.acme.api",
        "org.acme.auth",
        "org.acme.persistence.jpa",
        "org.acme.security"
})
@EntityScan(basePackages = "org.acme.persistence.jpa")
@EnableJpaRepositories(basePackages = "org.acme.persistence.jpa")
public class AcmeApiMvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcmeApiMvcApplication.class, args);
    }
}
