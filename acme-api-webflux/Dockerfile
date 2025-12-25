# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POMs
COPY pom.xml .
COPY acme-pom/pom.xml ./acme-pom/
COPY acme-pom/acme-dependencies/pom.xml ./acme-pom/acme-dependencies/
COPY acme-pom/acme-starter-parent/pom.xml ./acme-pom/acme-starter-parent/

# Copy security module
COPY acme-security/pom.xml ./acme-security/
COPY acme-security/acme-security-core/pom.xml ./acme-security/acme-security-core/
COPY acme-security/acme-security-webflux/pom.xml ./acme-security/acme-security-webflux/

# Copy persistence module
COPY acme-persistence-r2dbc/pom.xml ./acme-persistence-r2dbc/

# Copy API module
COPY acme-api-webflux/pom.xml ./acme-api-webflux/

# Copy source code
COPY acme-security ./acme-security
COPY acme-persistence-r2dbc ./acme-persistence-r2dbc
COPY acme-api-webflux ./acme-api-webflux

# Build
RUN mvn clean package -DskipTests -pl acme-api-webflux -am

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-api-webflux/target/*.jar app.jar

EXPOSE 8081

ENV SPRING_R2DBC_URL=r2dbc:postgresql://postgres-r2dbc:5432/acme_r2dbc
ENV SPRING_R2DBC_USERNAME=acme_user
ENV SPRING_R2DBC_PASSWORD=acme_password

ENTRYPOINT ["java", "-jar", "app.jar"]

