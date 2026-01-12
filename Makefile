.DEFAULT_GOAL := help

.PHONY: help \
		infra-up db-jpa-up db-r2dbc-up db-auth-up \
		infra-down db-jpa-down db-r2dbc-down db-auth-down \
		infra-reset db-jpa-reset db-r2dbc-reset db-auth-reset \
		infra-logs db-jpa-logs db-r2dbc-logs db-auth-logs \
		db-jpa-exec db-r2dbc-exec db-auth-exec \
		ldap-up ldap-down ldap-reset ldap-logs ldap-exec \
		monitoring-up monitoring-down monitoring-logs prometheus-ui grafana-ui \
		build clean test format lint clean-logs \
		run-api-mvc run-api-webflux run-auth-ldap run-auth-db run-ui \
		stop-api-mvc stop-api-webflux stop-auth stop-ui stop-all \
		docker-build-api-mvc docker-build-api-webflux docker-build-auth-ldap docker-build-auth-db \
		docker-run-api-mvc docker-run-api-webflux docker-run-auth-ldap docker-run-auth-db \
		k8s-pods k8s-logs k8s-describe k8s-deploy k8s-redeploy k8s-delete k8s-stop k8s-start k8s-status k8s-setup k8s-port-forward

help:
	@echo "Available targets:"
	@echo ""
	@echo "Infrastructure Operations:"
	@echo "  infra-up       - Start all infrastructure (PostgreSQL databases and LDAP)"
	@echo "  infra-down     - Stop all infrastructure (PostgreSQL databases and LDAP)"
	@echo "  infra-reset    - Stop and remove all infrastructure volumes (purges all data, requires confirmation)"
	@echo "  infra-logs     - View logs for all infrastructure (databases and LDAP)"
	@echo ""
	@echo "Database Operations:"
	@echo "  db-jpa-up      - Start only JPA PostgreSQL database"
	@echo "  db-r2dbc-up    - Start only R2DBC PostgreSQL database"
	@echo "  db-auth-up     - Start only Auth PostgreSQL database"
	@echo "  db-jpa-down    - Stop only JPA PostgreSQL database"
	@echo "  db-r2dbc-down  - Stop only R2DBC PostgreSQL database"
	@echo "  db-auth-down   - Stop only Auth PostgreSQL database"
	@echo "  db-jpa-reset   - Stop and remove JPA database volume (purges data, requires confirmation)"
	@echo "  db-r2dbc-reset - Stop and remove R2DBC database volume (purges data, requires confirmation)"
	@echo "  db-auth-reset  - Stop and remove Auth PostgreSQL database volume (purges data, requires confirmation)"
	@echo "  db-jpa-logs    - View logs for JPA database"
	@echo "  db-r2dbc-logs  - View logs for R2DBC database"
	@echo "  db-auth-logs   - View logs for Auth PostgreSQL database"
	@echo "  db-jpa-exec    - Execute into JPA PostgreSQL database (psql)"
	@echo "  db-r2dbc-exec  - Execute into R2DBC PostgreSQL database (psql)"
	@echo "  db-auth-exec   - Execute into Auth PostgreSQL database (psql)"
	@echo ""
	@echo "LDAP Operations:"
	@echo "  ldap-up        - Start only LDAP server"
	@echo "  ldap-down      - Stop only LDAP server"
	@echo "  ldap-reset     - Stop and remove LDAP volume (purges data, requires confirmation)"
	@echo "  ldap-logs      - View logs for LDAP server"
	@echo "  ldap-exec      - Execute into LDAP container (bash shell)"
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
	@echo "  test           - Run all tests (Java and UI)"
	@echo "  format         - Format all code (Java with Spotless, UI with Prettier)"
	@echo "  lint           - Check code formatting (does not modify files)"
	@echo "  clean-logs     - Remove all log files from submodules"
	@echo ""
	@echo "Run Applications:"
	@echo "  run-api-mvc    - Build and run MVC API"
	@echo "  run-api-webflux - Build and run WebFlux API"
	@echo "  run-auth-ldap  - Build and run Auth Service (LDAP variant)"
	@echo "  run-auth-db    - Build and run Auth Service (Database variant)"
	@echo "  run-ui         - Run Next.js UI (port 3001)"
	@echo "  stop-api-mvc   - Stop MVC API"
	@echo "  stop-api-webflux - Stop WebFlux API"
	@echo "  stop-auth      - Stop Auth Service (either variant)"
	@echo "  stop-ui        - Stop Next.js UI"
	@echo "  stop-all       - Stop all APIs and UI"
	@echo ""
	@echo "Docker Operations:"
	@echo "  docker-build-api-mvc    - Build Docker image for MVC API"
	@echo "  docker-build-api-webflux - Build Docker image for WebFlux API"
	@echo "  docker-build-auth-ldap  - Build Docker image for Auth Service (LDAP variant)"
	@echo "  docker-build-auth-db    - Build Docker image for Auth Service (Database variant)"
	@echo "  docker-run-api-mvc      - Run MVC API in Docker container"
	@echo "  docker-run-api-webflux  - Run WebFlux API in Docker container"
	@echo "  docker-run-auth-ldap    - Run Auth Service (LDAP) in Docker container"
	@echo "  docker-run-auth-db      - Run Auth Service (Database) in Docker container"
	@echo ""
	@echo "Kubernetes Operations:"
	@echo "  k8s-pods        - List all pods in acme-apps namespace"
	@echo "  k8s-logs        - View logs for a pod (usage: make k8s-logs POD=<pod-name>)"
	@echo "  k8s-describe    - Describe a pod (usage: make k8s-describe POD=<pod-name>)"
	@echo "  k8s-deploy      - Deploy all services to Kubernetes (builds images and applies manifests)"
	@echo "  k8s-redeploy    - Rebuild and redeploy all services to Kubernetes"
	@echo "  k8s-delete      - Delete all deployments from Kubernetes"
	@echo "  k8s-stop        - Stop Minikube cluster"
	@echo "  k8s-start       - Start Minikube cluster"
	@echo "  k8s-status     - Show Minikube and Kubernetes status"
	@echo "  k8s-setup      - Initial setup of Minikube (namespaces, secrets, ingress)"
	@echo "  k8s-port-forward - Port-forward Ingress controller (for local access)"

# Infrastructure Operations
infra-up:
	docker compose up -d postgres-jpa postgres-r2dbc postgres-auth ldap

infra-down:
	docker compose down

infra-reset:
	@echo "WARNING: This will delete ALL infrastructure data (databases and LDAP)!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	docker compose down -v

infra-logs:
	docker compose logs -f

# Database Operations
db-jpa-up:
	docker compose up -d postgres-jpa

db-jpa-down:
	docker compose stop postgres-jpa

db-jpa-reset:
	@echo "WARNING: This will delete all JPA database data!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	-docker compose stop postgres-jpa
	-docker compose rm -f postgres-jpa
	-docker volume rm $$(basename $$(pwd))_postgres-jpa-data 2>/dev/null
	-docker volume rm postgres-jpa-data 2>/dev/null

db-jpa-logs:
	docker compose logs -f postgres-jpa

db-r2dbc-up:
	docker compose up -d postgres-r2dbc

db-r2dbc-down:
	docker compose stop postgres-r2dbc

db-r2dbc-reset:
	@echo "WARNING: This will delete all R2DBC database data!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	-docker compose stop postgres-r2dbc
	-docker compose rm -f postgres-r2dbc
	-docker volume rm $$(basename $$(pwd))_postgres-r2dbc-data 2>/dev/null
	-docker volume rm postgres-r2dbc-data 2>/dev/null

db-r2dbc-logs:
	docker compose logs -f postgres-r2dbc

db-auth-up:
	docker compose up -d postgres-auth

db-auth-down:
	docker compose stop postgres-auth

db-auth-reset:
	@echo "WARNING: This will delete all Auth PostgreSQL database data!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	-docker compose stop postgres-auth
	-docker compose rm -f postgres-auth
	-docker volume rm $$(basename $$(pwd))_postgres-auth-data 2>/dev/null
	-docker volume rm postgres-auth-data 2>/dev/null

db-auth-logs:
	docker compose logs -f postgres-auth

db-jpa-exec:
	docker exec -it acme-postgres-jpa psql -U acme_user -d acme_jpa

db-r2dbc-exec:
	docker exec -it acme-postgres-r2dbc psql -U acme_user -d acme_r2dbc

db-auth-exec:
	docker exec -it acme-postgres-auth psql -U acme_user -d acme_auth

# LDAP Operations
ldap-up:
	docker compose up -d ldap

ldap-down:
	docker compose stop ldap

ldap-reset:
	@echo "WARNING: This will delete all LDAP data!"
	@read -p "Are you sure you want to continue? [y/N] " confirm && [ "$$confirm" = "y" ] || exit 1
	-docker compose stop ldap
	-docker compose rm -f ldap
	-docker volume rm $$(basename $$(pwd))_ldap-data 2>/dev/null
	-docker volume rm $$(basename $$(pwd))_ldap-config 2>/dev/null
	-docker volume rm ldap-data 2>/dev/null
	-docker volume rm ldap-config 2>/dev/null

ldap-logs:
	docker compose logs -f ldap

ldap-exec:
	docker exec -it acme-ldap bash

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
	@echo "Running UI tests..."
	@cd acme-ui && pnpm run test

# Java modules with source code (exclude POM-only modules)
JAVA_MODULES_LIST = \
	acme-api-mvc \
	acme-api-webflux \
	acme-auth-client \
	acme-auth-service-ldap \
	acme-auth-service-db \
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
	@echo "Formatting UI code..."
	@cd acme-ui && pnpm run format

lint:
	mvn spotless:check -pl $(JAVA_MODULES)
	@echo "Linting UI code..."
	@cd acme-ui && pnpm run lint

clean-logs:
	@echo "Cleaning log files..."
	@find . -type f -name "*.log" -not -path "*/target/*" -not -path "*/.git/*" -delete
	@find . -type d -name "logs" -not -path "*/target/*" -not -path "*/.git/*" -exec rm -rf {} + 2>/dev/null || true
	@echo "Log files cleaned"

run-api-mvc:
	mvn compile -DskipTests -pl acme-api-mvc -am \
	&& cd acme-api-mvc \
	&& SERVER_PORT=8080 mvn spring-boot:run \
	-Dspring-boot.run.fork=false \
	-Dspring-boot.run.addResources=false \
	-Dspring-boot.run.useTestClasspath=false \
	-Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8787" \
	-Dspring-boot.run.arguments="--spring.profiles.active=dev"

run-api-webflux:
	mvn compile -DskipTests -pl acme-api-webflux -am \
	&& cd acme-api-webflux \
	&& SERVER_PORT=8081 mvn spring-boot:run \
	-Dspring-boot.run.fork=false \
	-Dspring-boot.run.addResources=false \
	-Dspring-boot.run.useTestClasspath=false \
	-Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8788" \
	-Dspring-boot.run.arguments="--spring.profiles.active=dev"

run-auth-ldap:
	mvn compile -DskipTests -pl acme-auth-service-ldap -am \
	&& cd acme-auth-service-ldap \
	&& SERVER_PORT=8082 SERVER_SSL_ENABLED=true mvn spring-boot:run \
	-Dspring-boot.run.fork=false \
	-Dspring-boot.run.addResources=false \
	-Dspring-boot.run.useTestClasspath=false \
	-Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8789" \
	-Dspring-boot.run.arguments="--spring.profiles.active=dev"

run-auth-db:
	mvn compile -DskipTests -pl acme-auth-service-db -am \
	&& cd acme-auth-service-db \
	&& SERVER_PORT=8082 SERVER_SSL_ENABLED=true mvn spring-boot:run \
	-Dspring-boot.run.fork=false \
	-Dspring-boot.run.addResources=false \
	-Dspring-boot.run.useTestClasspath=false \
	-Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8789" \
	-Dspring-boot.run.arguments="--spring.profiles.active=dev"

run-ui:
	@if [ ! -d "acme-ui/node_modules" ]; then \
		echo "Installing UI dependencies..."; \
		cd acme-ui && pnpm install; \
	fi
	cd acme-ui && pnpm run dev

stop-api-mvc:
	@pkill -f "acme-api-mvc.*spring-boot:run" || pkill -f "AcmeApiMvcApplication" || echo "MVC API is not running"

stop-api-webflux:
	@pkill -f "acme-api-webflux.*spring-boot:run" || pkill -f "AcmeApiWebfluxApplication" || echo "WebFlux API is not running"

stop-auth:
	@pkill -f "acme-auth-service-ldap.*spring-boot:run" || \
	pkill -f "acme-auth-service-db.*spring-boot:run" || \
	pkill -f "AuthServiceApplication" || \
	echo "Auth Service is not running"

stop-ui:
	@pkill -f "next dev" || pkill -f "next-server" || echo "UI is not running"

stop-all: stop-api-mvc stop-api-webflux stop-auth stop-ui

docker-build-api-mvc:
	docker build -f acme-api-mvc/Dockerfile -t acme-api-mvc:latest .

docker-build-api-webflux:
	docker build -f acme-api-webflux/Dockerfile -t acme-api-webflux:latest .

docker-build-auth-ldap:
	docker build -f acme-auth-service-ldap/Dockerfile -t acme-auth-service-ldap:latest .

docker-build-auth-db:
	docker build -f acme-auth-service-db/Dockerfile -t acme-auth-service-db:latest .

docker-run-api-mvc: docker-build-api-mvc
	docker run -p 8080:8080 --network spring-mvc-vs-webflux_acme-network --name acme-api-mvc acme-api-mvc:latest

docker-run-api-webflux: docker-build-api-webflux
	docker run -p 8081:8081 --network spring-mvc-vs-webflux_acme-network --name acme-api-webflux acme-api-webflux:latest

docker-run-auth-ldap: docker-build-auth-ldap
	docker run -p 8082:8082 --network spring-mvc-vs-webflux_acme-network --name acme-auth-service-ldap acme-auth-service-ldap:latest

docker-run-auth-db: docker-build-auth-db
	docker run -p 8082:8082 --network spring-mvc-vs-webflux_acme-network --name acme-auth-service-db acme-auth-service-db:latest

# Kubernetes Operations
k8s-pods:
	@kubectl get pods -n acme-apps

k8s-logs:
	@if [ -z "$(POD)" ]; then \
		echo "Usage: make k8s-logs POD=<pod-name>"; \
		echo "Available pods:"; \
		kubectl get pods -n acme-apps -o name | sed 's|pod/||'; \
	else \
		kubectl logs -f $(POD) -n acme-apps; \
	fi

k8s-describe:
	@if [ -z "$(POD)" ]; then \
		echo "Usage: make k8s-describe POD=<pod-name>"; \
		echo "Available pods:"; \
		kubectl get pods -n acme-apps -o name | sed 's|pod/||'; \
	else \
		kubectl describe pod $(POD) -n acme-apps; \
	fi

k8s-deploy:
	@echo "🚀 Deploying to Kubernetes..."
	@bash acme-infrastructure/scripts/deploy.sh

k8s-redeploy: k8s-deploy

k8s-delete:
	@echo "🗑️  Deleting all deployments from Kubernetes..."
	@kubectl delete -f acme-infrastructure/deployments/ --ignore-not-found=true || true
	@echo "✅ Deployments deleted"

k8s-stop:
	@echo "🛑 Stopping Minikube cluster..."
	@minikube stop

k8s-start:
	@echo "▶️  Starting Minikube cluster..."
	@minikube start

k8s-status:
	@echo "📊 Minikube Status:"
	@minikube status || echo "Minikube is not running"
	@echo ""
	@echo "📊 Kubernetes Pods:"
	@kubectl get pods -n acme-apps 2>/dev/null || echo "Kubernetes is not accessible"
	@echo ""
	@echo "📊 Kubernetes Services:"
	@kubectl get svc -n acme-apps 2>/dev/null || echo "Kubernetes is not accessible"
	@echo ""
	@echo "📊 Kubernetes Ingress:"
	@kubectl get ingress -n acme-apps 2>/dev/null || echo "Kubernetes is not accessible"

k8s-setup:
	@echo "🔧 Setting up Minikube (namespaces, secrets, ingress)..."
	@bash acme-infrastructure/scripts/setup-minikube.sh

k8s-port-forward:
	@echo "🔌 Port-forwarding Ingress controller to localhost:8443..."
	@echo "Access via: https://acme.local:8443 (or https://localhost:8443)"
	@echo "Press Ctrl+C to stop"
	@bash acme-infrastructure/scripts/port-forward.sh
