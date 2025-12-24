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
COPY acme-security/acme-security-webmvc/pom.xml ./acme-security/acme-security-webmvc/

# Copy persistence module
COPY acme-persistence/pom.xml ./acme-persistence/
COPY acme-persistence/acme-persistence-jpa/pom.xml ./acme-persistence/acme-persistence-jpa/

# Copy API module
COPY acme-api/pom.xml ./acme-api/
COPY acme-api/acme-api-mvc/pom.xml ./acme-api/acme-api-mvc/

# Copy source code
COPY acme-security ./acme-security
COPY acme-persistence/acme-persistence-jpa ./acme-persistence/acme-persistence-jpa
COPY acme-api/acme-api-mvc ./acme-api/acme-api-mvc

# Build
RUN mvn clean package -DskipTests -pl acme-api/acme-api-mvc -am

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-api/acme-api-mvc/target/*.jar app.jar

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-jpa:5432/acme_jpa
ENV SPRING_DATASOURCE_USERNAME=acme_user
ENV SPRING_DATASOURCE_PASSWORD=acme_password

ENTRYPOINT ["java", "-jar", "app.jar"]

