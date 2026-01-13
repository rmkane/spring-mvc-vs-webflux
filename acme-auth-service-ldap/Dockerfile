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
    acme-auth-service-db acme-security acme-persistence-jpa acme-persistence-r2dbc \
    acme-api-mvc acme-api-webflux && \
    for dir in acme-test-integration-classic acme-test-integration-reactive \
    acme-auth-service-db acme-security acme-persistence-jpa acme-persistence-r2dbc \
    acme-api-mvc acme-api-webflux; do \
        echo '<?xml version="1.0" encoding="UTF-8"?><project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>org.acme</groupId><artifactId>'$dir'</artifactId><version>1.0.0-SNAPSHOT</version><packaging>pom</packaging></project>' > $dir/pom.xml; \
    done

# Copy auth utils (dependency)
COPY acme-auth-utils/pom.xml ./acme-auth-utils/
COPY acme-auth-utils ./acme-auth-utils

# Copy auth client (dependency)
COPY acme-auth-client/pom.xml ./acme-auth-client/
COPY acme-auth-client ./acme-auth-client

# Copy auth service LDAP
COPY acme-auth-service-ldap/pom.xml ./acme-auth-service-ldap/
COPY acme-auth-service-ldap ./acme-auth-service-ldap

# Build only the auth-service-ldap and its dependencies
RUN mvn clean package -DskipTests -pl acme-auth-service-ldap -am

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the Spring Boot executable JAR (Spring Boot plugin repackages it)
# The executable JAR is the one without .original suffix
COPY --from=build /app/acme-auth-service-ldap/target/acme-auth-service-ldap-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
