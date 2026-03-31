# Stage 1: Build
FROM --platform=linux/amd64 maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app

ARG REVISION=1.0.0-SNAPSHOT

COPY acme-framework ./acme-framework
COPY acme-auth-utils ./acme-auth-utils
COPY acme-auth-client ./acme-auth-client
COPY acme-api-webflux ./acme-api-webflux

RUN mvn -Drevision=${REVISION} -f acme-framework/acme-pom/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-auth-utils/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-auth-client/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-framework/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-api-webflux/pom.xml clean package -DskipTests

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
