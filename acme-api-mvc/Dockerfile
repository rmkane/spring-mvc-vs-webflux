# Stage 1: Build
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POMs
COPY pom.xml .
COPY acme-pom/pom.xml ./acme-pom/
COPY acme-pom/acme-dependencies/pom.xml ./acme-pom/acme-dependencies/
COPY acme-pom/acme-starter-parent/pom.xml ./acme-pom/acme-starter-parent/

# Create stub directories and minimal POMs for parent POM modules (to satisfy parent POM validation)
RUN mkdir -p acme-test-integration-reactive \
    acme-auth-service-db acme-auth-service-ldap acme-persistence-r2dbc acme-api-webflux && \
    for dir in acme-test-integration-reactive \
    acme-auth-service-db acme-auth-service-ldap acme-persistence-r2dbc acme-api-webflux; do \
        echo '<?xml version="1.0" encoding="UTF-8"?><project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>org.acme</groupId><artifactId>'$dir'</artifactId><version>1.0.0-SNAPSHOT</version><packaging>pom</packaging></project>' > $dir/pom.xml; \
    done

# Copy auth client (dependency)
COPY acme-auth-client/pom.xml ./acme-auth-client/
COPY acme-auth-client ./acme-auth-client

# Copy auth utils (dependency)
COPY acme-auth-utils/pom.xml ./acme-auth-utils/
COPY acme-auth-utils ./acme-auth-utils

# Copy test-integration-classic (test dependency)
COPY acme-test-integration-classic/pom.xml ./acme-test-integration-classic/
COPY acme-test-integration-classic ./acme-test-integration-classic

# Copy security module
COPY acme-security/pom.xml ./acme-security/
COPY acme-security/acme-security-core/pom.xml ./acme-security/acme-security-core/
COPY acme-security/acme-security-webmvc/pom.xml ./acme-security/acme-security-webmvc/
COPY acme-security ./acme-security

# Copy persistence module
COPY acme-persistence-jpa/pom.xml ./acme-persistence-jpa/
COPY acme-persistence-jpa ./acme-persistence-jpa

# Copy API module
COPY acme-api-mvc/pom.xml ./acme-api-mvc/
COPY acme-api-mvc ./acme-api-mvc

# Build only the api-mvc and its dependencies
RUN mvn clean package -DskipTests -pl acme-api-mvc -am

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-api-mvc/target/acme-api-mvc-*.jar app.jar

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-jpa:5432/acme_jpa
ENV SPRING_DATASOURCE_USERNAME=acme_user
ENV SPRING_DATASOURCE_PASSWORD=acme_password

ENTRYPOINT ["java", "-jar", "app.jar"]

