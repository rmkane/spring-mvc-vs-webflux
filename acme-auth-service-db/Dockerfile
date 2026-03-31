# Stage 1: Build
FROM --platform=linux/amd64 maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app

ARG REVISION=1.0.0-SNAPSHOT

COPY acme-framework/acme-pom ./acme-framework/acme-pom
COPY acme-auth-utils ./acme-auth-utils
COPY acme-auth-client ./acme-auth-client
COPY acme-auth-service-db ./acme-auth-service-db

RUN mvn -Drevision=${REVISION} -f acme-framework/acme-pom/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-auth-utils/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-auth-client/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-auth-service-db/pom.xml clean package -DskipTests

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-auth-service-db/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
