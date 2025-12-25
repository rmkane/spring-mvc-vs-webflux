# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POMs
COPY pom.xml .
COPY acme-pom/pom.xml ./acme-pom/
COPY acme-pom/acme-dependencies/pom.xml ./acme-pom/acme-dependencies/
COPY acme-pom/acme-starter-parent/pom.xml ./acme-pom/acme-starter-parent/

# Copy auth service
COPY acme-auth-service/pom.xml ./acme-auth-service/

# Copy source code
COPY acme-auth-service ./acme-auth-service

# Build
RUN mvn clean package -DskipTests -pl acme-auth-service -am

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-auth-service/target/*.jar app.jar

EXPOSE 8082

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-auth:5432/acme_auth
ENV SPRING_DATASOURCE_USERNAME=acme_user
ENV SPRING_DATASOURCE_PASSWORD=acme_password

ENTRYPOINT ["java", "-jar", "app.jar"]

