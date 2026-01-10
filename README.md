<!-- omit in toc -->

# Acme Multi-Module Spring Boot Application

A multi-module Spring Boot application comparing MVC (blocking) and WebFlux (reactive) implementations with LDAP-like DN-based authentication and role-based access control.

<!-- omit in toc -->

## Table of Contents

- [Acme Multi-Module Spring Boot Application](#acme-multi-module-spring-boot-application)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Project Structure](#project-structure)
  - [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Building the Project](#building-the-project)
    - [Starting Databases and LDAP](#starting-databases-and-ldap)
    - [Running Applications](#running-applications)
  - [Architecture Overview](#architecture-overview)
    - [Authentication Service](#authentication-service)
      - [`acme-auth-service-ldap` (LDAP-based)](#acme-auth-service-ldap-ldap-based)
      - [`acme-auth-service-db` (PostgreSQL-based)](#acme-auth-service-db-postgresql-based)
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
    - [Infrastructure Operations](#infrastructure-operations)
    - [Database Operations](#database-operations)
    - [LDAP Operations](#ldap-operations)
    - [Build Operations](#build-operations)
    - [Run Applications](#run-applications)
    - [Docker Operations](#docker-operations)
  - [Project Structure Details](#project-structure-details)
    - [Module Organization](#module-organization)
    - [Dependency Relationships](#dependency-relationships)
    - [Architecture Flow](#architecture-flow)
  - [Key Components](#key-components)
    - [Authentication Service Components](#authentication-service-components)
    - [Authentication Client (`acme-auth-client`)](#authentication-client-acme-auth-client)
    - [Security (`acme-security`)](#security-acme-security)
    - [Persistence Components](#persistence-components)
    - [API](#api)
    - [UI (`acme-ui`)](#ui-acme-ui)
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
├── acme-auth-service-db/            # Authentication service (PostgreSQL-based)
├── acme-auth-service-ldap/          # Authentication service (LDAP-based)
├── acme-security/                   # Security layer
│   ├── acme-security-core/          # Core security logic
│   ├── acme-security-webmvc/        # MVC security configuration
│   └── acme-security-webflux/       # WebFlux security configuration
├── acme-persistence-jpa/            # JPA repositories and entities
├── acme-persistence-r2dbc/          # R2DBC repositories and entities
├── acme-api-mvc/                    # MVC REST API
├── acme-api-webflux/                # WebFlux REST API
├── acme-ui/                         # Next.js web UI for book management
├── acme-test-integration-classic/   # Integration test framework (RestTemplate-based)
└── acme-test-integration-reactive/  # Reactive integration test framework (WebClient-based)
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 20+ and pnpm 8+
- Docker and Docker Compose

### Building the Project

```bash
make build
```

### Starting Databases and LDAP

```bash
make db-jpa-up      # Start JPA database (port 5432)
make db-r2dbc-up    # Start R2DBC database (port 5433)
make db-auth-up     # Start Auth PostgreSQL database (port 5434)
make ldap-up        # Start LDAP server (port 389)
# Or start all infrastructure:
make infra-up
```

This starts:

- **postgres-jpa**: Port 5432 for MVC/JPA API
- **postgres-r2dbc**: Port 5433 for WebFlux/R2DBC API
- **postgres-auth**: Port 5434 for Auth Service (DB variant)
- **ldap**: Port 389 for Auth Service (LDAP variant)

### Running Applications

**Auth Service (must be started first):**

Both auth service variants are **interchangeable** and run on port 8082 (only one can run at a time):

```bash
# LDAP-based authentication service
make run-auth-ldap

# OR PostgreSQL-based authentication service
make run-auth-db
```

Both services:

- Run on port 8082
- Expose the same REST API: `/api/auth/users/{dn}`
- Return the same response format
- Use the same role names: `ACME_READ_WRITE`, `ACME_READ_ONLY`
- Are drop-in replacements for each other

The auth service must be running before starting the API applications.

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

**UI Application:**

```bash
make run-ui
```

Runs on port 3001

The UI provides a web interface for managing books. It communicates with the backend APIs (MVC or WebFlux) and automatically includes the `x-dn` header for authentication. For local development, configure the DN in `acme-ui/.env.local` (see `acme-ui/README.md` for details).

## Architecture Overview

### Authentication Service

The authentication service is available in **two interchangeable variants**, both running on port 8082:

#### `acme-auth-service-ldap` (LDAP-based)

- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **LDAP Directory**: Uses OpenLDAP server (port 389) for user storage
- **LDIF Data**: Users and roles are loaded from LDIF file on LDAP server startup
- **Spring LDAP**: Uses Spring LDAP for LDAP operations
- **LDAP Groups**: Roles are stored as LDAP groups (`ACME_READ_WRITE`, `ACME_READ_ONLY`) and used directly as Spring Security authorities
- **LDAP Structure**: Users stored as `inetOrgPerson` entries with DN, givenName, surname, and role attributes

#### `acme-auth-service-db` (PostgreSQL-based)

- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **PostgreSQL Database**: Uses PostgreSQL (port 5434) for user storage
- **Flyway Migrations**: Users and roles are loaded via database migrations
- **Spring Data JPA**: Uses JPA for database operations
- **Database Tables**: Users and roles stored in `users` and `user_roles` tables

**Both variants:**

- Run on port 8082 (only one can run at a time)
- Return `UserInfoResponse` with DN, givenName, surname, and roles (ACME_READ_ONLY, ACME_READ_WRITE)
- Are completely isolated from main application databases
- Are **drop-in replacements** - APIs work with either variant without code changes

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
- **UI**: 3001

## Security Implementation

### Header-Based Authentication

Both implementations extract the `x-dn` header (Distinguished Name) from HTTP requests. The flow is:

1. Security layer extracts DN from header
2. Security layer calls `CachedUserLookupService` which checks cache first
3. On cache miss, `CachedUserLookupService` calls `AuthServiceClient` which makes REST call to auth service (`http://localhost:8082/api/auth/users/{dn}`)
4. Auth service queries user store (LDAP directory or PostgreSQL database) for user by DN and roles
5. Auth service returns `UserInfoResponse` with DN, givenName, surname, and roles
6. `AuthServiceClient` converts response to `UserInfo`
7. Result is cached and returned to security layer
8. Security layer creates `UserInformation` (derivative) from `UserInfo`
9. `UserInformation` is stored as the principal in SecurityContext

### Missing Header

If the `x-dn` header is missing or empty, both implementations return `401 Unauthorized`.

### User Lookup

The auth service queries its user store (LDAP directory or PostgreSQL database) for users by DN and their roles:

- **Standalone Service**: Auth service runs on port 8082
- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **Two Variants**:
  - **LDAP variant**: Uses OpenLDAP server (port 389) for user storage
    - Users loaded from LDIF file (`01-users.ldif`) on LDAP server startup
    - Users stored as `inetOrgPerson` entries with DN, givenName, surname, and role attributes
  - **PostgreSQL variant**: Uses PostgreSQL (port 5434) for user storage
    - Users loaded via Flyway database migrations
    - Users stored in `users` and `user_roles` tables
- **Available users** (same users in both variants):
  - `cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org` - Member of `ACME_READ_WRITE` group
  - `cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org` - Member of `ACME_READ_WRITE` group
  - `cn=Brian Wilson,ou=Finance,ou=Users,dc=corp,dc=acme,dc=org` - Member of `ACME_READ_ONLY` group
  - `cn=Maria Garcia,ou=IT,ou=Users,dc=corp,dc=acme,dc=org` - Member of `ACME_READ_ONLY` group
  - `cn=Kevin Tran,ou=Security,ou=Users,dc=corp,dc=acme,dc=org` - No group membership (no roles)
- **LDAP Groups**: `ACME_READ_ONLY`, `ACME_READ_WRITE` (stored as `groupOfNames` in LDAP)
- **Spring Security Authorities**: Group names are used directly as authorities (e.g., `ACME_READ_WRITE`, `ACME_READ_ONLY`)
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
@PreAuthorize("hasAuthority('ACME_READ_WRITE')")  // Requires ACME_READ_WRITE group
@PreAuthorize("hasAnyAuthority('ACME_READ_ONLY', 'ACME_READ_WRITE')")  // Requires either group
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

Users are loaded from LDIF file into the LDAP directory:

- `cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org`: Member of `ACME_READ_WRITE` group
- `cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org`: Member of `ACME_READ_WRITE` group
- `cn=Brian Wilson,ou=Finance,ou=Users,dc=corp,dc=acme,dc=org`: Member of `ACME_READ_ONLY` group
- `cn=Maria Garcia,ou=IT,ou=Users,dc=corp,dc=acme,dc=org`: Member of `ACME_READ_ONLY` group
- `cn=Kevin Tran,ou=Security,ou=Users,dc=corp,dc=acme,dc=org`: No roles assigned

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

1. **Start databases and LDAP:**

   ```bash
   make db-jpa-up      # Start JPA database
   make db-r2dbc-up    # Start R2DBC database
   make db-auth-up     # Start Auth PostgreSQL database (for DB auth variant)
   make ldap-up        # Start LDAP server (for LDAP auth variant)
   # Or start all infrastructure:
   make infra-up
   ```

2. **Build project:**

   ```bash
   make build
   ```

3. **Start Auth Service (must be started first):**

   ```bash
   # Choose one variant (both run on port 8082):
   make run-auth-ldap  # LDAP-based authentication
   # OR
   make run-auth-db    # PostgreSQL-based authentication
   ```

4. **Run MVC API:**

   ```bash
   make run-mvc
   ```

5. **Run WebFlux API:**

   ```bash
   make run-webflux
   ```

6. **Run UI (optional):**

   ```bash
   make run-ui
   ```

   The UI runs on port 3001. Configure `acme-ui/.env.local` with your DN for local development (see `acme-ui/README.md`).

7. **Run tests:**

   ```bash
   make test
   ```

## Docker

### Build Docker Images

```bash
make docker-build-auth-ldap  # Build Auth Service (LDAP variant)
make docker-build-auth-db     # Build Auth Service (Database variant)
make docker-build-mvc
make docker-build-webflux
```

### Run in Docker

```bash
# Start all services with docker compose
docker compose up -d

# Or run individually:
make docker-run-auth-ldap  # Run Auth Service (LDAP variant)
make docker-run-auth-db    # Run Auth Service (Database variant)
make docker-run-mvc
make docker-run-webflux
```

## Makefile Commands

### Infrastructure Operations

- `make infra-up` - Start all infrastructure (PostgreSQL databases and LDAP)
- `make infra-down` - Stop all infrastructure (PostgreSQL databases and LDAP)
- `make infra-reset` - Stop and remove all infrastructure volumes (purges all data, requires confirmation)
- `make infra-logs` - View logs for all infrastructure (databases and LDAP)

### Database Operations

- `make db-jpa-up` - Start JPA PostgreSQL database
- `make db-r2dbc-up` - Start R2DBC PostgreSQL database
- `make db-auth-up` - Start Auth PostgreSQL database
- `make db-jpa-down` - Stop JPA PostgreSQL database
- `make db-r2dbc-down` - Stop R2DBC PostgreSQL database
- `make db-auth-down` - Stop Auth PostgreSQL database
- `make db-jpa-reset` - Stop and remove JPA database volume (purges data, requires confirmation)
- `make db-r2dbc-reset` - Stop and remove R2DBC database volume (purges data, requires confirmation)
- `make db-auth-reset` - Stop and remove Auth PostgreSQL database volume (purges data, requires confirmation)
- `make db-jpa-logs` - View logs for JPA database
- `make db-r2dbc-logs` - View logs for R2DBC database
- `make db-auth-logs` - View logs for Auth PostgreSQL database

### LDAP Operations

- `make ldap-up` - Start only LDAP server
- `make ldap-down` - Stop only LDAP server
- `make ldap-reset` - Stop and remove LDAP volume (purges data, requires confirmation)
- `make ldap-logs` - View logs for LDAP server

### Build Operations

- `make build` - Build all Maven modules
- `make clean` - Clean all Maven modules
- `make test` - Run all tests (Java and UI)
- `make format` - Format all code (Java with Spotless, UI with Prettier and ESLint)
- `make lint` - Check code formatting (does not modify files, includes UI linting)

### Run Applications

- `make run-auth-ldap` - Build and run Auth Service (LDAP variant) on port 8082
- `make run-auth-db` - Build and run Auth Service (PostgreSQL variant) on port 8082
- `make run-mvc` - Build and run MVC API
- `make run-webflux` - Build and run WebFlux API
- `make run-ui` - Start UI application on port 3001
- `make stop-auth` - Stop Auth Service (either variant)
- `make stop-mvc` - Stop MVC API
- `make stop-webflux` - Stop WebFlux API
- `make stop-ui` - Stop UI application
- `make stop-all` - Stop all applications

### Docker Operations

- `make docker-build-auth-ldap` - Build Docker image for Auth Service (LDAP variant)
- `make docker-build-auth-db` - Build Docker image for Auth Service (PostgreSQL variant)
- `make docker-build-mvc` - Build Docker image for MVC API
- `make docker-build-webflux` - Build Docker image for WebFlux API
- `make docker-run-auth-ldap` - Run Auth Service (LDAP) in Docker container
- `make docker-run-auth-db` - Run Auth Service (PostgreSQL) in Docker container
- `make docker-run-mvc` - Run MVC API in Docker container
- `make docker-run-webflux` - Run WebFlux API in Docker container

## Project Structure Details

### Module Organization

- **acme-pom**: Dependency management (BOM and parent POM)
- **acme-auth-client**: REST client wrapper for calling auth service
- **acme-auth-service-db**: Authentication service with PostgreSQL backend
- **acme-auth-service-ldap**: Authentication service with LDAP backend
- **acme-security**: Security layer with core logic and framework-specific configs
- **acme-persistence-jpa**: JPA data access layer
- **acme-persistence-r2dbc**: R2DBC data access layer
- **acme-api-mvc**: MVC REST API
- **acme-api-webflux**: WebFlux REST API
- **acme-ui**: Next.js web UI for book management
- **acme-test-integration-classic**: Integration test framework using RestTemplate (for MVC APIs)
- **acme-test-integration-reactive**: Reactive integration test framework using WebClient (for WebFlux APIs)

### Dependency Relationships

- `acme-api-mvc` depends on `acme-security-webmvc` and `acme-persistence-jpa`
- `acme-api-webflux` depends on `acme-security-webflux` and `acme-persistence-r2dbc`
- `acme-security-core` depends on `acme-auth-client` which provides `AuthServiceClient`
- `acme-auth-client` provides `AuthServiceClientConfig` which creates the REST client bean
- `acme-auth-service-db` and `acme-auth-service-ldap` are standalone Spring Boot applications (no dependencies on other modules)
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
6. Auth service queries LDAP directory for user by DN and roles
7. Auth service returns `UserInfoResponse` with DN, givenName, surname, and roles
8. `AuthServiceClient` converts response to `UserInfo`
9. Result is cached and returned to security layer
10. Security creates `UserInformation` (derivative) from `UserInfo`
11. `UserInformation` is stored as principal in SecurityContext

## Key Components

### Authentication Service Components

**acme-auth-service-ldap:**

- **Standalone Service**: Runs on port 8082 in its own container
- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **LDAP Directory**: OpenLDAP server (port 389) for user storage
- **LDAP Service**: `LdapUserService` queries LDAP by DN using Spring LDAP
- **LDIF Data**: Users loaded from `01-users.ldif` file on LDAP server startup
- **Response**: Returns `UserInfoResponse` with DN, givenName, surname, and roles

**acme-auth-service-db:**

- **Standalone Service**: Runs on port 8082 in its own container
- **REST API**: Exposes `/api/auth/users/{dn}` endpoint (DN = Distinguished Name)
- **PostgreSQL Database**: PostgreSQL (port 5434) for user storage
- **JPA Service**: `UserService` queries database by DN using Spring Data JPA
- **Flyway Migrations**: Users loaded via database migrations
- **Response**: Returns `UserInfoResponse` with DN, givenName, surname, and roles

**Both services are interchangeable** - they expose the same API contract and can be used as drop-in replacements.

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
- `RequestResponseLoggingFilter`: MVC filter that logs request and response headers (DEBUG level)
- `RequestResponseLoggingWebFilter`: WebFlux filter that logs request and response headers (DEBUG level)

### Persistence Components

- `Book`: Entity class (JPA and R2DBC versions) with `createdBy` and `updatedBy` audit fields
- `BookRepository`: Repository interface (JpaRepository and ReactiveCrudRepository)
- **Note**: User data is stored in LDAP directory, not in main application databases
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

### UI (`acme-ui`)

- **Next.js Application**: Web UI for book management built with Next.js 16.1 and React 19.2
- **Features**: Book listing, create, edit, and delete operations with alphabetical sorting (ignoring leading articles)
- **Authentication**: Automatically includes `x-dn` header from `LOCAL_DN` environment variable for backend API requests
- **Port**: Runs on port 3001 (development server)
- **Technology Stack**: Next.js, React, TypeScript, Tailwind CSS, pnpm
- **See `acme-ui/README.md` for detailed documentation**

## License

This is a demonstration project for comparing MVC and WebFlux implementations.
