# Stage 1: Build
FROM --platform=linux/amd64 maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /app

ARG REVISION=1.0.0-SNAPSHOT

COPY acme-framework ./acme-framework
COPY acme-auth-utils ./acme-auth-utils
COPY acme-auth-client ./acme-auth-client
COPY acme-api-mvc ./acme-api-mvc

RUN mvn -Drevision=${REVISION} -f acme-framework/acme-pom/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-auth-utils/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-auth-client/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-framework/pom.xml clean install -DskipTests \
	&& mvn -Drevision=${REVISION} -f acme-api-mvc/pom.xml clean package -DskipTests

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/acme-api-mvc/target/acme-api-mvc-*.jar app.jar

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-jpa:5432/acme_jpa
ENV SPRING_DATASOURCE_USERNAME=acme_user
ENV SPRING_DATASOURCE_PASSWORD=acme_password

ENTRYPOINT ["java", "-jar", "app.jar"]
