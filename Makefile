.DEFAULT_GOAL := help

.PHONY: help \
		dbs-up db-jpa-up db-r2dbc-up db-auth-up \
		dbs-down db-jpa-down db-r2dbc-down db-auth-down \
		dbs-reset db-jpa-reset db-r2dbc-reset db-auth-reset \
		dbs-logs db-jpa-logs db-r2dbc-logs db-auth-logs \
		monitoring-up monitoring-down monitoring-logs prometheus-ui grafana-ui \
		build clean test format lint \
		run-mvc run-webflux run-auth stop-mvc stop-webflux stop-auth stop-all \
		docker-build-mvc docker-build-webflux docker-build-auth \
		docker-run-mvc docker-run-webflux docker-run-auth

help:
	@echo "Available targets:"
	@echo ""
	@echo "Database Operations:"
	@echo "  dbs-up         - Start all PostgreSQL databases (JPA, R2DBC, Auth)"
	@echo "  db-jpa-up      - Start only JPA PostgreSQL database"
	@echo "  db-r2dbc-up    - Start only R2DBC PostgreSQL database"
	@echo "  db-auth-up     - Start only Auth PostgreSQL database"
	@echo "  dbs-down       - Stop all PostgreSQL databases"
	@echo "  db-jpa-down    - Stop only JPA PostgreSQL database"
	@echo "  db-r2dbc-down  - Stop only R2DBC PostgreSQL database"
	@echo "  db-auth-down   - Stop only Auth PostgreSQL database"
	@echo "  dbs-reset      - Stop and remove all database volumes (purges all data, requires confirmation)"
	@echo "  db-jpa-reset   - Stop and remove JPA database volume (purges data, requires confirmation)"
	@echo "  db-r2dbc-reset - Stop and remove R2DBC database volume (purges data, requires confirmation)"
	@echo "  db-auth-reset  - Stop and remove Auth database volume (purges data, requires confirmation)"
	@echo "  dbs-logs       - View logs for all databases"
	@echo "  db-jpa-logs    - View logs for JPA database"
	@echo "  db-r2dbc-logs  - View logs for R2DBC database"
	@echo "  db-auth-logs   - View logs for Auth database"
	@echo ""
	@echo "Monitoring Operations:"
	@echo "  monitoring-up   - Start Prometheus and Grafana"
	@echo "  monitoring-down - Stop Prometheus and Grafana"
	@echo "  monitoring-logs - View logs for monitoring stack"
	@echo "  prometheus-ui   - Open Prometheus UI in browser"
	@echo "  grafana-ui      - Open Grafana UI in browser"
	@echo ""
	@echo "Build Operations:"
	@echo "  build          - Build all Maven modules"
	@echo "  clean          - Clean all Maven modules"
	@echo "  test           - Run all tests"
	@echo "  format         - Format all Java code with Spotless"
	@echo "  lint           - Check code formatting (does not modify files)"
	@echo ""
	@echo "Run Applications:"
	@echo "  run-mvc        - Build and run MVC API"
	@echo "  run-webflux    - Build and run WebFlux API"
	@echo "  run-auth       - Build and run Auth Service"
	@echo "  stop-mvc       - Stop MVC API"
	@echo "  stop-webflux   - Stop WebFlux API"
	@echo "  stop-auth      - Stop Auth Service"
	@echo "  stop-all       - Stop all APIs"
	@echo ""
	@echo "Docker Operations:"
	@echo "  docker-build-mvc      - Build Docker image for MVC API"
	@echo "  docker-build-webflux  - Build Docker image for WebFlux API"
	@echo "  docker-build-auth     - Build Docker image for Auth Service"
	@echo "  docker-run-mvc        - Run MVC API in Docker container"
	@echo "  docker-run-webflux    - Run WebFlux API in Docker container"
	@echo "  docker-run-auth       - Run Auth Service in Docker container"

dbs-up:
	docker compose up -d postgres-jpa postgres-r2dbc postgres-auth

db-jpa-up:
	docker compose up -d postgres-jpa

db-r2dbc-up:
	docker compose up -d postgres-r2dbc

db-auth-up:
	docker compose up -d postgres-auth

dbs-down:
	docker compose down

db-jpa-down:
	docker compose stop postgres-jpa

db-r2dbc-down:
	docker compose stop postgres-r2dbc

db-auth-down:
	docker compose stop postgres-auth

dbs-reset:
	@echo "WARNING: This will delete ALL database data (JPA, R2DBC, and Auth)!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker compose down -v

db-jpa-reset:
	@echo "WARNING: This will delete all JPA database data!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	-docker compose stop postgres-jpa
	-docker compose rm -f postgres-jpa
	-docker volume rm $$(basename $$(pwd))_postgres-jpa-data 2>/dev/null
	-docker volume rm postgres-jpa-data 2>/dev/null

db-r2dbc-reset:
	@echo "WARNING: This will delete all R2DBC database data!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	-docker compose stop postgres-r2dbc
	-docker compose rm -f postgres-r2dbc
	-docker volume rm $$(basename $$(pwd))_postgres-r2dbc-data 2>/dev/null
	-docker volume rm postgres-r2dbc-data 2>/dev/null

db-auth-reset:
	@echo "WARNING: This will delete all Auth database data!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	-docker compose stop postgres-auth
	-docker compose rm -f postgres-auth
	-docker volume rm $$(basename $$(pwd))_postgres-auth-data 2>/dev/null
	-docker volume rm postgres-auth-data 2>/dev/null

dbs-logs:
	docker compose logs -f

db-jpa-logs:
	docker compose logs -f postgres-jpa

db-r2dbc-logs:
	docker compose logs -f postgres-r2dbc

db-auth-logs:
	docker compose logs -f postgres-auth

monitoring-up:
	docker compose up -d prometheus grafana

monitoring-down:
	docker compose stop prometheus grafana

monitoring-logs:
	docker compose logs -f prometheus grafana

prometheus-ui:
	@echo "Opening Prometheus UI at http://localhost:9090"
	@open http://localhost:9090 || xdg-open http://localhost:9090 || echo "Please open http://localhost:9090 in your browser"

grafana-ui:
	@echo "Opening Grafana UI at http://localhost:3000"
	@echo "Default credentials: admin/admin"
	@open http://localhost:3000 || xdg-open http://localhost:3000 || echo "Please open http://localhost:3000 in your browser"

build:
	mvn clean install -DskipTests

clean:
	mvn clean

test:
	mvn test

# Java modules with source code (exclude POM-only modules)
JAVA_MODULES_LIST = \
	acme-api-mvc \
	acme-api-webflux \
	acme-auth-client \
	acme-auth-service \
	acme-security/acme-security-core \
	acme-security/acme-security-webmvc \
	acme-security/acme-security-webflux \
	acme-persistence-jpa \
	acme-persistence-r2dbc \
	acme-test-integration-classic \
	acme-test-integration-reactive

# Convert space-separated list to comma-separated
JAVA_MODULES = $(subst $(space),$(comma),$(JAVA_MODULES_LIST))
space := $(empty) $(empty)
comma := ,

format:
	mvn spotless:apply -pl $(JAVA_MODULES)

lint:
	mvn spotless:check -pl $(JAVA_MODULES)

run-mvc:
	mvn compile -DskipTests -pl acme-api-mvc -am \
	&& cd acme-api-mvc \
	&& SERVER_PORT=8080 mvn spring-boot:run \
	-Dspring-boot.run.fork=false \
	-Dspring-boot.run.addResources=false \
	-Dspring-boot.run.useTestClasspath=false \
	-Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8787" \
	-Dspring-boot.run.arguments="--spring.profiles.active=dev"

run-webflux:
	mvn compile -DskipTests -pl acme-api-webflux -am \
	&& cd acme-api-webflux \
	&& SERVER_PORT=8081 mvn spring-boot:run \
	-Dspring-boot.run.fork=false \
	-Dspring-boot.run.addResources=false \
	-Dspring-boot.run.useTestClasspath=false \
	-Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8788" \
	-Dspring-boot.run.arguments="--spring.profiles.active=dev"

run-auth:
	mvn compile -DskipTests -pl acme-auth-service -am \
	&& cd acme-auth-service \
	&& SERVER_PORT=8082 SERVER_SSL_ENABLED=true mvn spring-boot:run \
	-Dspring-boot.run.fork=false \
	-Dspring-boot.run.addResources=false \
	-Dspring-boot.run.useTestClasspath=false \
	-Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8789" \
	-Dspring-boot.run.arguments="--spring.profiles.active=dev"

stop-mvc:
	@pkill -f "acme-api-mvc.*spring-boot:run" || pkill -f "AcmeApiMvcApplication" || echo "MVC API is not running"

stop-webflux:
	@pkill -f "acme-api-webflux.*spring-boot:run" || pkill -f "AcmeApiWebfluxApplication" || echo "WebFlux API is not running"

stop-auth:
	@pkill -f "acme-auth-service.*spring-boot:run" || pkill -f "AuthServiceApplication" || echo "Auth Service is not running"

stop-all: stop-mvc stop-webflux stop-auth

docker-build-mvc:
	cd acme-api-mvc && docker build -t acme-api-mvc:latest .

docker-build-webflux:
	cd acme-api-webflux && docker build -t acme-api-webflux:latest .

docker-build-auth:
	cd acme-auth-service && docker build -t acme-auth-service:latest .

docker-run-mvc: docker-build-mvc
	docker run -p 8080:8080 --network spring-mvc-vs-webflux_acme-network --name acme-api-mvc acme-api-mvc:latest

docker-run-webflux: docker-build-webflux
	docker run -p 8081:8081 --network spring-mvc-vs-webflux_acme-network --name acme-api-webflux acme-api-webflux:latest

docker-run-auth: docker-build-auth
	docker run -p 8082:8082 --network spring-mvc-vs-webflux_acme-network --name acme-auth-service acme-auth-service:latest
