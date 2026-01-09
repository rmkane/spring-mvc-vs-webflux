<!-- omit in toc -->
# Acme Multi-Module Spring Boot Application

A multi-module Spring Boot application comparing MVC (blocking) and WebFlux (reactive) implementations with LDAP-like DN-based authentication and role-based access control.

<!-- omit in toc -->
## Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Building the Project](#building-the-project)
  - [Starting Databases](#starting-databases)
  - [Running Applications](#running-applications)
- [Architecture Overview](#architecture-overview)
  - [Authentication Service (`acme-auth-service`)](#authentication-service-acme-auth-service)
  - [Security Layer (`acme-security`)](#security-layer-acme-security)
  - [Persistence Layer](#persistence-layer)
- [MVC Route (Traditional/Blocking)](#mvc-route-traditionalblocking)
  - [MVC Framework](#mvc-framework)
  - [MVC Security](#mvc-security)
  - [MVC Persistence](#mvc-persistence)
  - [MVC Return Types](#mvc-return-types)
  - [MVC Port](#mvc-port)
- [WebFlux Route (Reactive/Non-blocking)](#webflux-route-reactivenon-blocking)
  - [WebFlux Framework](#webflux-framework)
  - [WebFlux Security](#webflux-security)
  - [WebFlux Persistence](#webflux-persistence)
  - [WebFlux Return Types](#webflux-return-types)
  - [WebFlux Port](#webflux-port)
- [How They Are The Same](#how-they-are-the-same)
- [How They Are Different](#how-they-are-different)
  - [Execution Model](#execution-model)
  - [Return Types Comparison](#return-types-comparison)
  - [Database Access](#database-access)
  - [Security Context Access](#security-context-access)
  - [Performance Characteristics](#performance-characteristics)
  - [Ports](#ports)
- [Security Implementation](#security-implementation)
  - [Header-Based Authentication](#header-based-authentication)
  - [Missing Header](#missing-header)
  - [User Lookup](#user-lookup)
  - [User Lookup Caching](#user-lookup-caching)
  - [Role-Based Access Control](#role-based-access-control)
  - [Deployment Context](#deployment-context)
- [Testing the APIs](#testing-the-apis)
  - [Example Request (MVC)](#example-request-mvc)
  - [Example Request (WebFlux)](#example-request-webflux)
  - [Missing Header (Returns 401)](#missing-header-returns-401)
  - [CRUD Operations](#crud-operations)
  - [Integration Testing](#integration-testing)
- [Development Workflow](#development-workflow)
- [Docker](#docker)
  - [Build Docker Images](#build-docker-images)
  - [Run in Docker](#run-in-docker)
- [Makefile Commands](#makefile-commands)
  - [Database Operations](#database-operations)
  - [Build Operations](#build-operations)
  - [Run Applications](#run-applications)
  - [Docker Operations](#docker-operations)
- [Project Structure Details](#project-structure-details)
  - [Module Organization](#module-organization)
  - [Dependency Relationships](#dependency-relationships)
  - [Architecture Flow](#architecture-flow)
- [Key Components](#key-components)
  - [Authentication Service Components (`acme-auth-service`)](#authentication-service-components-acme-auth-service)
  - [Authentication Client (`acme-auth-client`)](#authentication-client-acme-auth-client)
  - [Security (`acme-security`)](#security-acme-security)
  - [Persistence Components](#persistence-components)
  - [API](#api)
- [License](#license)

## Overview

This project demonstrates two different approaches to building REST APIs with Spring Boot:

- **MVC Route**: Traditional blocking, servlet-based approach using Spring MVC and JPA
- **WebFlux Route**: Reactive, non-blocking approach using Spring WebFlux and R2DBC

Both implementations provide the same functionality but use different execution models and persistence layers.

## Project Structure

```none
spring-mvc-vs-webflux/
├── pom.xml                          # Root aggregator
├── acme-pom/                        # Dependency management
│   ├── acme-dependencies/           # BOM for dependency versions
│   └── acme-starter-parent/         # Parent POM with plugin management
├── acme-auth-client/                # REST client wrapper for auth service
├── acme-auth-service/               # Standalone authentication service
│   └── src/main/java/               # Spring Boot application with REST API
├── acme-security/                   # Security layer
│   ├── acme-security-core/          # Core security logic
│   ├── acme-security-webmvc/        # MVC security configuration
│   └── acme-security-webflux/       # WebFlux security configuration
├── acme-persistence-jpa/            # JPA repositories and entities
├── acme-persistence-r2dbc/          # R2DBC repositories and entities
├── acme-api-mvc/                    # MVC REST API
├── acme-api-webflux/                # WebFlux REST API
├── acme-test-integration-classic/   # Integration test framework (RestTemplate-based)
└── acme-test-integration-reactive/  # Reactive integration test framework (WebClient-based)
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker and Docker Compose

### Building the Project

```bash
make build
```

### Starting Databases

```bash
make db-jpa-up      # Start JPA database (port 5432)
make db-r2dbc-up    # Start R2DBC database (port 5433)
make db-auth-up     # Start Auth database (port 5434)
# Or start all:
make dbs-up
```

This starts three PostgreSQL databases:

- **postgres-jpa**: Port 5432 for MVC/JPA API
- **postgres-r2dbc**: Port 5433 for WebFlux/R2DBC API
- **postgres-auth**: Port 5434 for Auth Service

### Running Applications

**Auth Service (must be started first):**

```bash
make run-auth
```

Runs on port 8082. This service must be running before starting the API applications.

**MVC API:**

```bash
make run-mvc
```

Runs on port 8080

**WebFlux API:**

```bash
make run-webflux
```

Runs on port 8081

## Architecture Overview

### Authentication Service (`acme-auth-service`)

The authentication service is a **standalone Spring Boot application** that runs in its own container:

- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **Database**: Has its own PostgreSQL database (`acme_auth` on port 5434)
- **Flyway Migrations**: Manages user and role tables with seed data (LDAP-like structure)
- **Isolation**: Completely isolated from main application databases
- **Port**: Runs on port 8082
- Returns `UserInfoResponse` with DN, givenName, surname, and roles (ROLE_READ_ONLY, ROLE_READ_WRITE)
- **LDAP-like Structure**: Users have DN (Distinguished Name), givenName, and surname instead of username

### Security Layer (`acme-security`)

The security layer handles authentication mechanics:

- Extracts `x-dn` header from HTTP requests (DN = Distinguished Name)
- **Calls auth service via REST** using `AuthServiceClient` to look up user by DN
- Creates `UserInformation` (derivative) from `UserInfo` returned by auth service
- Missing header returns `401 Unauthorized`
- Headers are supplied through ingress layer (SSL/TLS termination handled upstream)
- **No direct database access** - all user lookups go through the auth service

### Persistence Layer

- **JPA**: Blocking database access using Spring Data JPA
- **R2DBC**: Reactive, non-blocking database access using Spring Data R2DBC
- Both use Flyway for database migrations
- Same database schema (`books` table)

## MVC Route (Traditional/Blocking)

### MVC Framework

- Spring MVC (Servlet-based)

### MVC Security

- `RequestHeaderAuthenticationFilter` extracts `x-dn` header
- Custom `AuthenticationManager` calls `AuthServiceClient` to lookup user by DN
- Creates `UserInformation` from `UserInfo` returned by auth service
- `SecurityContextHolder` for accessing principal (thread-local)

### MVC Persistence

- JPA/Spring Data JPA (blocking database operations)
- PostgreSQL on port 5432 (`acme_jpa` database)
- **Note**: User data is NOT stored here - users are managed by the auth service

### MVC Return Types

- `ResponseEntity<T>`, standard Java objects
- Blocking, synchronous operations

### MVC Port

- 8080

## WebFlux Route (Reactive/Non-blocking)

### WebFlux Framework

- Spring WebFlux (Reactive)

### WebFlux Security

- Custom `ServerHttpAuthenticationConverter` extracts `x-dn` header
- Reactive `AuthenticationManager` calls `AuthServiceClient` to lookup user by DN
- Creates `UserInformation` from `UserInfo` returned by auth service
- `ReactiveSecurityContextHolder` or `@AuthenticationPrincipal` for accessing principal

### WebFlux Persistence

- R2DBC (reactive, non-blocking database operations)
- PostgreSQL on port 5433 (`acme_r2dbc` database)

### WebFlux Return Types

- `Mono<T>`, `Flux<T>` (reactive types)
- Non-blocking, reactive operations

### WebFlux Port

- 8081

## How They Are The Same

- **Same Security Mechanism**: Both use `x-dn` header for authentication (DN = Distinguished Name)
- **Same Authentication Flow**: Both call auth service via REST to lookup users by DN
- **Same User Principal**: Both create `UserInformation` (derivative) from `UserInfo` with roles from auth service
- **Same Role-Based Access Control**: Both use `@PreAuthorize` annotations with database-backed roles
- **Same API Endpoints**: Both expose `/api/v1/books` with same CRUD operations
- **Same Business Logic**: Same service layer functionality
- **Same Database Schema**: Both use identical `books` table structure (users are in auth service database)
- **Same Error Handling**: Missing header returns `401 Unauthorized` in both

## How They Are Different

### Execution Model

- **MVC**: Blocking, thread-per-request model
- **WebFlux**: Non-blocking, event-loop model

### Return Types Comparison

- **MVC**: `ResponseEntity<Book>`, `List<Book>`, standard Java types
- **WebFlux**: `Mono<Book>`, `Flux<Book>`, reactive types

### Database Access

- **MVC**: JPA with blocking JDBC connections
- **WebFlux**: R2DBC with non-blocking reactive connections

### Security Context Access

- **MVC**: `SecurityContextHolder.getContext()` (thread-local)
- **WebFlux**: `ReactiveSecurityContextHolder.getContext()` (reactive) or `@AuthenticationPrincipal`

### Performance Characteristics

- **MVC**: Better for CPU-intensive, blocking I/O operations
- **WebFlux**: Better for high concurrency, I/O-bound operations

### Ports

- **MVC**: 8080
- **WebFlux**: 8081
- **Auth Service**: 8082

## Security Implementation

### Header-Based Authentication

Both implementations extract the `x-dn` header (Distinguished Name) from HTTP requests. The flow is:

1. Security layer extracts DN from header
2. Security layer calls `CachedUserLookupService` which checks cache first
3. On cache miss, `CachedUserLookupService` calls `AuthServiceClient` which makes REST call to auth service (`http://localhost:8082/api/auth/users/{dn}`)
4. Auth service queries its own database (`acme_auth`) for user by DN and roles
5. Auth service returns `UserInfoResponse` with DN, givenName, surname, and roles
6. `AuthServiceClient` converts response to `UserInfo`
7. Result is cached and returned to security layer
8. Security layer creates `UserInformation` (derivative) from `UserInfo`
9. `UserInformation` is stored as the principal in SecurityContext

### Missing Header

If the `x-dn` header is missing or empty, both implementations return `401 Unauthorized`.

### User Lookup

The auth service queries its own database (`acme_auth`) for users by DN and their roles:

- **Standalone Service**: Auth service runs on port 8082 with its own database (port 5434)
- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **LDAP-like Structure**: Users have DN, givenName, and surname (no username field)
- **Database-backed users**: Users are seeded in auth service database with LDAP-like DNs:
  - `cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org` - Has `ROLE_READ_WRITE` (full access)
  - `cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org` - Has `ROLE_READ_WRITE` (full access)
  - `cn=Brian Wilson,ou=Finance,ou=Users,dc=corp,dc=acme,dc=org` - Has `ROLE_READ_ONLY` (read-only access)
  - `cn=Maria Garcia,ou=IT,ou=Users,dc=corp,dc=example,dc=com` - Has `ROLE_READ_ONLY` (read-only access)
  - `cn=Kevin Tran,ou=Security,ou=Users,dc=corp,dc=example,dc=com` - No roles assigned
  - Users have DN (Distinguished Name), givenName, and surname fields
- **Roles**: `ROLE_READ_ONLY`, `ROLE_READ_WRITE` (from auth service database, not hardcoded)
- **Isolation**: User data is completely isolated from main application databases

### User Lookup Caching

User lookups are cached in the security layer to reduce calls to the auth service:

- **Cache Provider**: Caffeine (in-memory) configured via Spring Boot auto-configuration in `application.yml`
- **Cache Key**: DN (Distinguished Name)
- **Cache Name**: `users`
- **Configuration**: Configured in `application.yml` using Spring Boot's standard Caffeine properties:

  ```yaml
  spring:
    cache:
      type: caffeine
      cache-names: users
      caffeine:
        spec: >
          expireAfterWrite=5m,
          maximumSize=1000
  ```

- **Flexible**: Each API can override the cache provider by providing its own `CacheManager` bean
  - Default: Caffeine (in-memory, auto-configured)
  - Can use: Hazelcast, Redis, or any Spring Cache-compatible provider
  - Security layer is cache-provider agnostic - just needs a `CacheManager` bean
- **Transparent**: Caching is handled by the security layer - APIs don't need to know about it
- **Logging**: Cache misses are logged at DEBUG level when the lookup method executes

### Role-Based Access Control

Service methods are protected with `@PreAuthorize` annotations:

```java
@PreAuthorize("hasRole('READ_WRITE')")  // Requires ROLE_READ_WRITE
@PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")  // Requires either role
```

### Deployment Context

Applications run in HTTP (no SSL/TLS). SSL/TLS termination is handled by the ingress layer above, which forwards headers (including `x-dn`) to the applications.

## Testing the APIs

### Example Request (MVC)

```bash
curl -H "x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org" http://localhost:8080/api/v1/books
```

### Example Request (WebFlux)

```bash
curl -H "x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org" http://localhost:8081/api/v1/books
```

**Available test users:**

Users are seeded in the auth service database with LDAP-like DNs:

- `cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org`: Has `ROLE_READ_WRITE` (full access)
- `cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org`: Has `ROLE_READ_WRITE` (full access)
- `cn=Brian Wilson,ou=Finance,ou=Users,dc=corp,dc=acme,dc=org`: Has `ROLE_READ_ONLY` (read-only access)
- `cn=Maria Garcia,ou=IT,ou=Users,dc=corp,dc=example,dc=com`: Has `ROLE_READ_ONLY` (read-only access)
- `cn=Kevin Tran,ou=Security,ou=Users,dc=corp,dc=example,dc=com`: No roles assigned

### Missing Header (Returns 401)

```bash
curl http://localhost:8080/api/v1/books  # Returns 401 Unauthorized
```

### CRUD Operations

**Create Book (requires READ_WRITE role):**

```bash
curl -X POST -H "x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Book","author":"Test Author","isbn":"123-456-789","publicationYear":2024}' \
  http://localhost:8080/api/v1/books
```

**Note:** The API uses request/response DTOs (`CreateBookRequest`, `UpdateBookRequest`, `BookResponse`) and MapStruct for mapping between DTOs and entities. Duplicate ISBNs return a 400 Bad Request with RFC 9457 ProblemDetail response.

**Get All Books (requires READ_ONLY or READ_WRITE role):**

```bash
curl -H "x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org" \
  http://localhost:8080/api/v1/books
```

**Get Book by ID (requires READ_ONLY or READ_WRITE role):**

```bash
curl -H "x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org" \
  http://localhost:8080/api/v1/books/1
```

**Update Book (requires READ_WRITE role):**

```bash
curl -X PUT -H "x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org" \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated Title","author":"Updated Author"}' \
  http://localhost:8080/api/v1/books/1
```

**Delete Book (requires READ_WRITE role):**

```bash
curl -X DELETE -H "x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org" \
  http://localhost:8080/api/v1/books/1
```

**Note:** See `scripts/test-mvc.sh` and `scripts/test-webflux.sh` for comprehensive test scripts. Use the `X_DN` environment variable to set the DN.

### Integration Testing

Both APIs include integration tests using their respective test frameworks:

- **MVC API**: Uses `acme-test-integration-classic` framework with `RestTemplate` and `IntegrationTestSuite` base class
- **WebFlux API**: Uses `acme-test-integration-reactive` framework with `WebClient` and `ReactiveIntegrationTestSuite` base class

Integration tests are tagged with `@Tag("integration")` and are excluded from regular test runs via Maven Surefire plugin configuration. To run integration tests explicitly:

```bash
# Run integration tests for MVC API
cd acme-api-mvc && mvn test -Dgroups=integration

# Run integration tests for WebFlux API
cd acme-api-webflux && mvn test -Dgroups=integration
```

See `acme-test-integration-classic/README.md` and `acme-test-integration-reactive/README.md` for detailed usage instructions.

## Development Workflow

1. **Start databases:**

   ```bash
   make db-jpa-up      # Start JPA database
   make db-r2dbc-up    # Start R2DBC database
   make db-auth-up     # Start Auth database
   # Or start all:
   make dbs-up
   ```

2. **Build project:**

   ```bash
   make build
   ```

3. **Start Auth Service (must be started first):**

   ```bash
   make run-auth
   ```

4. **Run MVC API:**

   ```bash
   make run-mvc
   ```

5. **Run WebFlux API:**

   ```bash
   make run-webflux
   ```

6. **Run tests:**

   ```bash
   make test
   ```

## Docker

### Build Docker Images

```bash
make docker-build-auth
make docker-build-mvc
make docker-build-webflux
```

### Run in Docker

```bash
# Start all services with docker-compose
docker compose up -d

# Or run individually:
make docker-run-auth
make docker-run-mvc
make docker-run-webflux
```

## Makefile Commands

### Database Operations

- `make dbs-up` - Start all PostgreSQL databases
- `make db-jpa-up` - Start JPA PostgreSQL database
- `make db-r2dbc-up` - Start R2DBC PostgreSQL database
- `make db-auth-up` - Start Auth PostgreSQL database
- `make dbs-down` - Stop all databases
- `make db-jpa-down` - Stop JPA database
- `make db-r2dbc-down` - Stop R2DBC database
- `make db-auth-down` - Stop Auth database
- `make dbs-logs` - View all database logs
- `make db-jpa-logs` - View JPA database logs
- `make db-r2dbc-logs` - View R2DBC database logs
- `make db-auth-logs` - View Auth database logs

### Build Operations

- `make build` - Build all Maven modules
- `make clean` - Clean all Maven modules
- `make test` - Run all tests
- `make format` - Format all Java code with Spotless
- `make lint` - Check code formatting (does not modify files)

### Run Applications

- `make run-auth` - Build and run Auth Service (must be started first)
- `make run-mvc` - Build and run MVC API
- `make run-webflux` - Build and run WebFlux API
- `make stop-auth` - Stop Auth Service
- `make stop-mvc` - Stop MVC API
- `make stop-webflux` - Stop WebFlux API
- `make stop-all` - Stop all applications

### Docker Operations

- `make docker-build-auth` - Build Docker image for Auth Service
- `make docker-build-mvc` - Build Docker image for MVC API
- `make docker-build-webflux` - Build Docker image for WebFlux API
- `make docker-run-auth` - Run Auth Service in Docker container
- `make docker-run-mvc` - Run MVC API in Docker container
- `make docker-run-webflux` - Run WebFlux API in Docker container

## Project Structure Details

### Module Organization

- **acme-pom**: Dependency management (BOM and parent POM)
- **acme-auth-client**: REST client wrapper for calling acme-auth-service
- **acme-auth-service**: Standalone authentication service with REST API and its own database
- **acme-security**: Security layer with core logic and framework-specific configs
- **acme-persistence-jpa**: JPA data access layer
- **acme-persistence-r2dbc**: R2DBC data access layer
- **acme-api-mvc**: MVC REST API
- **acme-api-webflux**: WebFlux REST API
- **acme-test-integration-classic**: Integration test framework using RestTemplate (for MVC APIs)
- **acme-test-integration-reactive**: Reactive integration test framework using WebClient (for WebFlux APIs)

### Dependency Relationships

- `acme-api-mvc` depends on `acme-security-webmvc` and `acme-persistence-jpa`
- `acme-api-webflux` depends on `acme-security-webflux` and `acme-persistence-r2dbc`
- `acme-security-core` depends on `acme-auth-client` which provides `AuthServiceClient`
- `acme-auth-client` provides `AuthServiceClientConfig` which creates the REST client bean
- `acme-auth-service` is a standalone Spring Boot application (no dependencies on other modules)
- All modules inherit from `acme-starter-parent` which inherits from `acme-dependencies`

### Architecture Flow

```none
Request → Security Layer → CachedUserLookupService → [Cache Check] → AuthServiceClient → Auth Service (REST) → Auth Database
                ↓                                                              ↓
         UserInformation (principal)                                    Cache (on miss)
```

1. Security extracts `x-dn` header (Distinguished Name)
2. Security calls `CachedUserLookupService.lookupUser(dn)` (cached)
3. Cache is checked first - if hit, returns cached `UserInfo`
4. On cache miss, `CachedUserLookupService` calls `AuthServiceClient.lookupUser(dn)`
5. `AuthServiceClient` makes REST call to auth service: `GET /api/auth/users/{dn}`
6. Auth service queries its own database (`acme_auth`) for user by DN and roles
7. Auth service returns `UserInfoResponse` with DN, givenName, surname, and roles
8. `AuthServiceClient` converts response to `UserInfo`
9. Result is cached and returned to security layer
10. Security creates `UserInformation` (derivative) from `UserInfo`
11. `UserInformation` is stored as principal in SecurityContext

## Key Components

### Authentication Service Components (`acme-auth-service`)

- **Standalone Service**: Runs on port 8082 in its own container
- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **Database**: Own PostgreSQL database (`acme_auth` on port 5434)
- **Entities**: `User` (with DN, givenName, surname), `UserRole` for RBAC
- **Migrations**: Flyway migrations for users and roles tables with seed data (LDAP-like structure)
- **Response**: Returns `UserInfoResponse` with DN, givenName, surname, and roles

### Authentication Client (`acme-auth-client`)

- `UserInfo`: User information model with DN, givenName, surname, and roles (implements `UserDetails`)
- `AuthServiceClient`: REST client for calling acme-auth-service by DN
- `AuthServiceClientConfig`: Spring configuration for creating AuthServiceClient bean

### Security (`acme-security`)

- `UserInformation`: Principal object (derivative of `UserInfo`) stored in SecurityContext with DN
- `CachedUserLookupService`: Caches user lookups to reduce calls to auth service (uses `@Cacheable`)
- `AuthServiceClient`: REST client for calling auth service to lookup users by DN
- `AuthenticationService`: Creates authenticated `Authentication` from DN (uses `CachedUserLookupService`)
- `SslConfig`: SSL/TLS configuration for auth service client communication (mTLS)
- `WebMvcSecurityConfig`: MVC security configuration (extracts `x-dn` header)
- `WebFluxSecurityConfig`: WebFlux security configuration (extracts `x-dn` header)

### Persistence Components

- `Book`: Entity class (JPA and R2DBC versions) with `createdBy` and `updatedBy` audit fields
- `BookRepository`: Repository interface (JpaRepository and ReactiveCrudRepository)
- **Note**: `User` and `UserRole` entities are in `acme-auth-service`, not in main application databases
- Flyway migrations:
  - V1: Initial schema (books table with audit fields)
  - V2: Seed data (books)

### API

- `BookController`: REST controller with CRUD endpoints
- `BookService`: Service interface with `@PreAuthorize` role checks
- `BookServiceImpl`: Service implementation with business logic
- `BookMapper`: MapStruct mapper for DTO-to-entity conversion
- **Model Layer (DTOs)**:
  - `CreateBookRequest`: Request DTO for creating books
  - `UpdateBookRequest`: Request DTO for updating books
  - `BookResponse`: Response DTO for book data
- `GlobalExceptionHandler`: Centralized exception handling with RFC 9457 ProblemDetail responses
- `BookAlreadyExistsException`: Custom exception for duplicate ISBN validation
- `SecurityContextUtil` / `ReactiveSecurityContextUtil`: Utility classes for accessing `UserInformation` principal
- **Integration Tests**: Both APIs include integration tests using their respective test frameworks

## License

This is a demonstration project for comparing MVC and WebFlux implementations.
