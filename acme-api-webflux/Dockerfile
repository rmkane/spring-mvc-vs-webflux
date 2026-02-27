# Stage 1: Build
FROM --platform=linux/amd64 maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent POMs
COPY pom.xml .
COPY acme-pom/pom.xml ./acme-pom/
COPY acme-pom/acme-dependencies/pom.xml ./acme-pom/acme-dependencies/
COPY acme-pom/acme-starter-parent/pom.xml ./acme-pom/acme-starter-parent/

# Create stub directories and minimal POMs for parent POM modules (to satisfy parent POM validation)
RUN mkdir -p acme-test-integration-classic \
    acme-auth-service-db acme-auth-service-ldap acme-persistence-jpa acme-api-mvc && \
    for dir in acme-test-integration-classic \
    acme-auth-service-db acme-auth-service-ldap acme-persistence-jpa acme-api-mvc; do \
        echo '<?xml version="1.0" encoding="UTF-8"?><project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>org.acme</groupId><artifactId>'$dir'</artifactId><version>1.0.0-SNAPSHOT</version><packaging>pom</packaging></project>' > $dir/pom.xml; \
    done

# Copy auth client (dependency)
COPY acme-auth-client/pom.xml ./acme-auth-client/
COPY acme-auth-client ./acme-auth-client

# Copy auth utils (dependency)
COPY acme-auth-utils/pom.xml ./acme-auth-utils/
COPY acme-auth-utils ./acme-auth-utils

# Copy test-integration-reactive (test dependency)
COPY acme-test-integration-reactive/pom.xml ./acme-test-integration-reactive/
COPY acme-test-integration-reactive ./acme-test-integration-reactive

# Copy security module
COPY acme-security/pom.xml ./acme-security/
COPY acme-security/acme-security-core/pom.xml ./acme-security/acme-security-core/
COPY acme-security/acme-security-webflux/pom.xml ./acme-security/acme-security-webflux/
COPY acme-security ./acme-security

# Copy persistence module
COPY acme-persistence-r2dbc/pom.xml ./acme-persistence-r2dbc/
COPY acme-persistence-r2dbc ./acme-persistence-r2dbc

# Copy API module
COPY acme-api-webflux/pom.xml ./acme-api-webflux/
COPY acme-api-webflux ./acme-api-webflux

# Build only the api-webflux and its dependencies
RUN mvn clean package -DskipTests -pl acme-api-webflux -am

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-api-webflux/target/acme-api-webflux-*.jar app.jar

EXPOSE 8081

ENV SPRING_R2DBC_URL=r2dbc:postgresql://postgres-r2dbc:5432/acme_r2dbc
ENV SPRING_R2DBC_USERNAME=acme_user
ENV SPRING_R2DBC_PASSWORD=acme_password

ENTRYPOINT ["java", "-jar", "app.jar"]

