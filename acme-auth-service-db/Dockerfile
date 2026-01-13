# Stage 1: Build
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POMs
COPY pom.xml .
COPY acme-pom/pom.xml ./acme-pom/
COPY acme-pom/acme-dependencies/pom.xml ./acme-pom/acme-dependencies/
COPY acme-pom/acme-starter-parent/pom.xml ./acme-pom/acme-starter-parent/

# Create stub directories and minimal POMs for parent POM modules (to satisfy parent POM validation)
RUN mkdir -p acme-test-integration-classic acme-test-integration-reactive \
    acme-auth-service-ldap acme-security acme-persistence-jpa acme-persistence-r2dbc \
    acme-api-mvc acme-api-webflux && \
    for dir in acme-test-integration-classic acme-test-integration-reactive \
    acme-auth-service-ldap acme-security acme-persistence-jpa acme-persistence-r2dbc \
    acme-api-mvc acme-api-webflux; do \
        echo '<?xml version="1.0" encoding="UTF-8"?><project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>org.acme</groupId><artifactId>'$dir'</artifactId><version>1.0.0-SNAPSHOT</version><packaging>pom</packaging></project>' > $dir/pom.xml; \
    done

# Copy auth client (dependency)
COPY acme-auth-client/pom.xml ./acme-auth-client/
COPY acme-auth-client ./acme-auth-client

# Copy auth utils (dependency)
COPY acme-auth-utils/pom.xml ./acme-auth-utils/
COPY acme-auth-utils ./acme-auth-utils

# Copy auth service DB
COPY acme-auth-service-db/pom.xml ./acme-auth-service-db/

# Copy source code
COPY acme-auth-service-db ./acme-auth-service-db

# Build
RUN mvn clean package -DskipTests -pl acme-auth-service-db -am

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-auth-service-db/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
