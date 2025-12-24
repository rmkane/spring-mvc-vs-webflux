.DEFAULT_GOAL := help

.PHONY: help \
		dbs-up db-jpa-up db-r2dbc-up \
		dbs-down db-jpa-down db-r2dbc-down \
		dbs-logs db-jpa-logs db-r2dbc-logs \
		build clean test format lint \
		run-mvc run-webflux stop-mvc stop-webflux stop-all \
		docker-build-mvc docker-build-webflux \
		docker-run-mvc docker-run-webflux

help:
	@echo "Available targets:"
	@echo ""
	@echo "Database Operations:"
	@echo "  dbs-up         - Start both PostgreSQL databases"
	@echo "  db-jpa-up      - Start only JPA PostgreSQL database"
	@echo "  db-r2dbc-up    - Start only R2DBC PostgreSQL database"
	@echo "  dbs-down       - Stop both PostgreSQL databases"
	@echo "  db-jpa-down    - Stop only JPA PostgreSQL database"
	@echo "  db-r2dbc-down  - Stop only R2DBC PostgreSQL database"
	@echo "  dbs-logs       - View logs for both databases"
	@echo "  db-jpa-logs    - View logs for JPA database"
	@echo "  db-r2dbc-logs  - View logs for R2DBC database"
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
	@echo "  stop-mvc       - Stop MVC API"
	@echo "  stop-webflux   - Stop WebFlux API"
	@echo "  stop-all       - Stop both MVC and WebFlux APIs"
	@echo ""
	@echo "Docker Operations:"
	@echo "  docker-build-mvc     - Build Docker image for MVC API"
	@echo "  docker-build-webflux - Build Docker image for WebFlux API"
	@echo "  docker-run-mvc      - Run MVC API in Docker container"
	@echo "  docker-run-webflux  - Run WebFlux API in Docker container"

dbs-up:
	docker compose up -d

db-jpa-up:
	docker compose up -d postgres-jpa

db-r2dbc-up:
	docker compose up -d postgres-r2dbc

dbs-down:
	docker compose down

db-jpa-down:
	docker compose stop postgres-jpa

db-r2dbc-down:
	docker compose stop postgres-r2dbc

dbs-logs:
	docker compose logs -f

db-jpa-logs:
	docker compose logs -f postgres-jpa

db-r2dbc-logs:
	docker compose logs -f postgres-r2dbc

build:
	mvn clean install -DskipTests

clean:
	mvn clean

test:
	mvn test

# Java modules with source code (exclude POM-only modules)
JAVA_MODULES_LIST = \
	acme-api/acme-api-mvc \
	acme-api/acme-api-webflux \
	acme-auth/acme-auth-core \
	acme-auth/acme-auth-jpa \
	acme-auth/acme-auth-r2dbc \
	acme-security/acme-security-core \
	acme-security/acme-security-webmvc \
	acme-security/acme-security-webflux \
	acme-persistence/acme-persistence-jpa \
	acme-persistence/acme-persistence-r2dbc

# Convert space-separated list to comma-separated
JAVA_MODULES = $(subst $(space),$(comma),$(JAVA_MODULES_LIST))
space := $(empty) $(empty)
comma := ,

format:
	mvn spotless:apply -pl $(JAVA_MODULES)

lint:
	mvn spotless:check -pl $(JAVA_MODULES)

run-mvc: build
	cd acme-api/acme-api-mvc && mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev

run-webflux: build
	cd acme-api/acme-api-webflux && mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev

stop-mvc:
	@pkill -f "acme-api-mvc.*spring-boot:run" || pkill -f "AcmeApiMvcApplication" || echo "MVC API is not running"

stop-webflux:
	@pkill -f "acme-api-webflux.*spring-boot:run" || pkill -f "AcmeApiWebfluxApplication" || echo "WebFlux API is not running"

stop-all: stop-mvc stop-webflux

docker-build-mvc:
	cd acme-api/acme-api-mvc && docker build -t acme-api-mvc:latest .

docker-build-webflux:
	cd acme-api/acme-api-webflux && docker build -t acme-api-webflux:latest .

docker-run-mvc: docker-build-mvc
	docker run -p 8080:8080 --network spring-mvc-vs-webflux_acme-network --name acme-api-mvc acme-api-mvc:latest

docker-run-webflux: docker-build-webflux
	docker run -p 8081:8081 --network spring-mvc-vs-webflux_acme-network --name acme-api-webflux acme-api-webflux:latest

