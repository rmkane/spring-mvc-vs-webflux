<!-- omit in toc -->
# Acme Multi-Module Spring Boot Application

A multi-module Spring Boot application comparing MVC (blocking) and WebFlux (reactive) implementations with header-based authentication and role-based access control.

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
  - [Authentication Layer (`acme-auth`)](#authentication-layer-acme-auth)
  - [Security Layer (`acme-security`)](#security-layer-acme-security)
  - [Persistence Layer](#persistence-layer)
- [MVC Route (Traditional/Blocking)](#mvc-route-traditionalblocking)
  - [Framework](#framework)
  - [Security](#security)
  - [Persistence](#persistence)
  - [Return Types](#return-types)
  - [Port](#port)
- [WebFlux Route (Reactive/Non-blocking)](#webflux-route-reactivenon-blocking)
  - [Framework](#framework-1)
  - [Security](#security-1)
  - [Persistence](#persistence-1)
  - [Return Types](#return-types-1)
  - [Port](#port-1)
- [How They Are The Same](#how-they-are-the-same)
- [How They Are Different](#how-they-are-different)
  - [Execution Model](#execution-model)
  - [Return Types](#return-types-2)
  - [Database Access](#database-access)
  - [Security Context Access](#security-context-access)
  - [Performance Characteristics](#performance-characteristics)
  - [Ports](#ports)
- [Security Implementation](#security-implementation)
  - [Header-Based Authentication](#header-based-authentication)
  - [Missing Header](#missing-header)
  - [User Lookup](#user-lookup)
  - [Role-Based Access Control](#role-based-access-control)
  - [Deployment Context](#deployment-context)
- [Testing the APIs](#testing-the-apis)
  - [Example Request (MVC)](#example-request-mvc)
  - [Example Request (WebFlux)](#example-request-webflux)
  - [Missing Header (Returns 401)](#missing-header-returns-401)
  - [CRUD Operations](#crud-operations)
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
  - [Authentication (`acme-auth`)](#authentication-acme-auth)
  - [Security (`acme-security`)](#security-acme-security)
  - [Persistence](#persistence-2)
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
├── pom.xml                         # Root aggregator
├── acme-pom/                       # Dependency management
│   ├── acme-dependencies/          # BOM for dependency versions
│   └── acme-starter-parent/        # Parent POM with plugin management
├── acme-auth/                      # Authentication layer
│   ├── acme-auth-core/             # Core auth interfaces and services
│   ├── acme-auth-jpa/              # JPA implementation of user lookup
│   └── acme-auth-r2dbc/            # R2DBC implementation of user lookup
├── acme-security/                  # Security layer
│   ├── acme-security-core/         # Core security logic
│   ├── acme-security-webmvc/       # MVC security configuration
│   └── acme-security-webflux/      # WebFlux security configuration
├── acme-persistence/               # Persistence layer
│   ├── acme-persistence-jpa/       # JPA repositories and entities
│   └── acme-persistence-r2dbc/     # R2DBC repositories and entities
└── acme-api/                       # API layer
    ├── acme-api-mvc/               # MVC REST API
    └── acme-api-webflux/           # WebFlux REST API
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
# Or start both:
make db-jpa-up db-r2dbc-up
```

This starts two PostgreSQL databases:

- **postgres-jpa**: Port 5432 for MVC/JPA API
- **postgres-r2dbc**: Port 5433 for WebFlux/R2DBC API

### Running Applications

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

### Authentication Layer (`acme-auth`)

The authentication layer handles all user lookup logic:

- **`acme-auth-core`**: Defines `UserPrincipal`, `UserPrincipalRepository` interface, and `UserLookupService`
- **`acme-auth-jpa`**: JPA implementation of `UserPrincipalRepository` for MVC
- **`acme-auth-r2dbc`**: R2DBC implementation of `UserPrincipalRepository` for WebFlux
- Queries database for users and their roles
- Returns `UserPrincipal` with roles from database (ROLE_READ_ONLY, ROLE_READ_WRITE)

### Security Layer (`acme-security`)

The security layer handles authentication mechanics:

- Extracts `x-username` header from HTTP requests
- Calls auth layer to lookup user and get `UserPrincipal`
- Creates `UserInformation` (derivative) from `UserPrincipal` as the principal
- Missing header returns `401 Unauthorized`
- Headers are supplied through ingress layer (SSL/TLS termination handled upstream)

### Persistence Layer

- **JPA**: Blocking database access using Spring Data JPA
- **R2DBC**: Reactive, non-blocking database access using Spring Data R2DBC
- Both use Flyway for database migrations
- Same database schema (`books` table)

## MVC Route (Traditional/Blocking)

### Framework

- Spring MVC (Servlet-based)

### Security

- `RequestHeaderAuthenticationFilter` extracts `x-username` header
- Custom `AuthenticationManager` calls auth layer's `UserLookupService`
- Creates `UserInformation` from `UserPrincipal` returned by auth layer
- `SecurityContextHolder` for accessing principal (thread-local)

### Persistence

- JPA/Spring Data JPA (blocking database operations)
- PostgreSQL on port 5432 (`acme_jpa` database)

### Return Types

- `ResponseEntity<T>`, standard Java objects
- Blocking, synchronous operations

### Port

- 8080

## WebFlux Route (Reactive/Non-blocking)

### Framework

- Spring WebFlux (Reactive)

### Security

- Custom `ServerHttpAuthenticationConverter` extracts `x-username` header
- Reactive `AuthenticationManager` calls auth layer's `UserLookupService`
- Creates `UserInformation` from `UserPrincipal` returned by auth layer
- `ReactiveSecurityContextHolder` or `@AuthenticationPrincipal` for accessing principal

### Persistence

- R2DBC (reactive, non-blocking database operations)
- PostgreSQL on port 5433 (`acme_r2dbc` database)

### Return Types

- `Mono<T>`, `Flux<T>` (reactive types)
- Non-blocking, reactive operations

### Port

- 8081

## How They Are The Same

- **Same Security Mechanism**: Both use `x-username` header for authentication
- **Same Authentication Flow**: Both call auth layer to lookup users from database
- **Same User Principal**: Both create `UserInformation` (derivative) from `UserPrincipal` with roles from database
- **Same Role-Based Access Control**: Both use `@PreAuthorize` annotations with database-backed roles
- **Same API Endpoints**: Both expose `/api/books` with same CRUD operations
- **Same Business Logic**: Same service layer functionality
- **Same Database Schema**: Both use identical `books`, `users`, and `user_roles` table structures
- **Same Error Handling**: Missing header returns `401 Unauthorized` in both

## How They Are Different

### Execution Model

- **MVC**: Blocking, thread-per-request model
- **WebFlux**: Non-blocking, event-loop model

### Return Types

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

## Security Implementation

### Header-Based Authentication

Both implementations extract the `x-username` header from HTTP requests. The flow is:

1. Security layer extracts username from header
2. Security layer calls auth layer's `UserLookupService` to query database
3. Auth layer returns `UserPrincipal` with roles from database
4. Security layer creates `UserInformation` (derivative) from `UserPrincipal`
5. `UserInformation` is stored as the principal in SecurityContext

### Missing Header

If the `x-username` header is missing or empty, both implementations return `401 Unauthorized`.

### User Lookup

The auth layer (`UserLookupService`) queries the database for users and their roles:

- **Database-backed users**: Three test users are seeded:
  - `noaccess`: No roles
  - `readonly`: `ROLE_READ_ONLY` role
  - `readwrite`: `ROLE_READ_ONLY` and `ROLE_READ_WRITE` roles
- **Roles**: `ROLE_READ_ONLY`, `ROLE_READ_WRITE` (from database, not hardcoded)

### Role-Based Access Control

Service methods are protected with `@PreAuthorize` annotations:

```java
@PreAuthorize("hasRole('READ_WRITE')")  // Requires ROLE_READ_WRITE
@PreAuthorize("hasAnyRole('READ_ONLY', 'READ_WRITE')")  // Requires either role
```

### Deployment Context

Applications run in HTTP (no SSL/TLS). SSL/TLS termination is handled by the ingress layer above, which forwards headers (including `x-username`) to the applications.

## Testing the APIs

### Example Request (MVC)

```bash
curl -H "x-username: readonly" http://localhost:8080/api/books
```

### Example Request (WebFlux)

```bash
curl -H "x-username: readwrite" http://localhost:8081/api/books
```

**Available test users:**

- `noaccess`: No roles (will fail on protected endpoints)
- `readonly`: Can read books (GET operations)
- `readwrite`: Can read and write books (all CRUD operations)

### Missing Header (Returns 401)

```bash
curl http://localhost:8080/api/books  # Returns 401 Unauthorized
```

### CRUD Operations

**Create Book (requires READ_WRITE role):**

```bash
curl -X POST -H "x-username: readwrite" -H "Content-Type: application/json" \
  -d '{"title":"Test Book","author":"Test Author","isbn":"123-456-789"}' \
  http://localhost:8080/api/books
```

**Get All Books (requires READ_ONLY or READ_WRITE role):**

```bash
curl -H "x-username: readonly" http://localhost:8080/api/books
```

**Get Book by ID (requires READ_ONLY or READ_WRITE role):**

```bash
curl -H "x-username: readonly" http://localhost:8080/api/books/1
```

**Update Book (requires READ_WRITE role):**

```bash
curl -X PUT -H "x-username: readwrite" -H "Content-Type: application/json" \
  -d '{"title":"Updated Title","author":"Updated Author"}' \
  http://localhost:8080/api/books/1
```

**Delete Book (requires READ_WRITE role):**

```bash
curl -X DELETE -H "x-username: readwrite" http://localhost:8080/api/books/1
```

**Note:** See `scripts/test-mvc.sh` and `scripts/test-webflux.sh` for comprehensive test scripts.

## Development Workflow

1. **Start databases:**

   ```bash
   make db-jpa-up      # Start JPA database
   make db-r2dbc-up    # Start R2DBC database
   ```

2. **Build project:**

   ```bash
   make build
   ```

3. **Run MVC API:**

   ```bash
   make run-mvc
   ```

4. **Run WebFlux API:**

   ```bash
   make run-webflux
   ```

5. **Run tests:**

   ```bash
   make test
   ```

## Docker

### Build Docker Images

```bash
make docker-build-mvc
make docker-build-webflux
```

### Run in Docker

```bash
make docker-run-mvc
make docker-run-webflux
```

## Makefile Commands

### Database Operations

- `make db-jpa-up` - Start JPA PostgreSQL database
- `make db-r2dbc-up` - Start R2DBC PostgreSQL database
- `make db-jpa-down` - Stop JPA database
- `make db-r2dbc-down` - Stop R2DBC database
- `make db-jpa-logs` - View JPA database logs
- `make db-r2dbc-logs` - View R2DBC database logs

### Build Operations

- `make build` - Build all Maven modules
- `make clean` - Clean all Maven modules
- `make test` - Run all tests
- `make format` - Format all Java code with Spotless
- `make lint` - Check code formatting (does not modify files)

### Run Applications

- `make run-mvc` - Build and run MVC API
- `make run-webflux` - Build and run WebFlux API
- `make stop-mvc` - Stop MVC API
- `make stop-webflux` - Stop WebFlux API

### Docker Operations

- `make docker-build-mvc` - Build Docker image for MVC API
- `make docker-build-webflux` - Build Docker image for WebFlux API
- `make docker-run-mvc` - Run MVC API in Docker container
- `make docker-run-webflux` - Run WebFlux API in Docker container

## Project Structure Details

### Module Organization

- **acme-pom**: Dependency management (BOM and parent POM)
- **acme-auth**: Authentication layer with user lookup logic (JPA and R2DBC implementations)
- **acme-security**: Security layer with core logic and framework-specific configs
- **acme-persistence**: Data access layer with JPA and R2DBC implementations
- **acme-api**: API layer with MVC and WebFlux REST controllers

### Dependency Relationships

- `acme-api-mvc` depends on `acme-security-webmvc` and `acme-persistence-jpa`
- `acme-api-webflux` depends on `acme-security-webflux` and `acme-persistence-r2dbc`
- `acme-security-webmvc` depends on `acme-auth-jpa` (not persistence directly)
- `acme-security-webflux` depends on `acme-auth-r2dbc` (not persistence directly)
- All modules inherit from `acme-starter-parent` which inherits from `acme-dependencies`

### Architecture Flow

```
Request → Security Layer → Auth Layer → Persistence Layer → Database
                ↓
         UserInformation (principal)
```

1. Security extracts `x-username` header
2. Security calls auth layer's `UserLookupService`
3. Auth layer queries persistence layer for user and roles
4. Auth layer returns `UserPrincipal` with roles
5. Security creates `UserInformation` (derivative) from `UserPrincipal`
6. `UserInformation` is stored as principal in SecurityContext

## Key Components

### Authentication (`acme-auth`)

- `UserPrincipal`: User principal with roles (from auth layer)
- `UserPrincipalRepository`: Interface for user lookup (JPA and R2DBC implementations)
- `UserLookupService`: Service that queries database for users and roles

### Security (`acme-security`)

- `UserInformation`: Principal object (derivative of `UserPrincipal`) stored in SecurityContext
- `AuthenticationService`: Creates authenticated `Authentication` from username
- `WebMvcSecurityConfig`: MVC security configuration
- `WebFluxSecurityConfig`: WebFlux security configuration

### Persistence

- `Book`: Entity class (JPA and R2DBC versions)
- `BookRepository`: Repository interface (JpaRepository and ReactiveCrudRepository)
- `User` and `UserRole`: Entities for RBAC
- Flyway migrations:
  - V1: Initial schema (books table)
  - V2: Seed data (books)
  - V3: User and role tables
  - V4: User and role seed data (noaccess, readonly, readwrite)

### API

- `BookController`: REST controller with CRUD endpoints
- `BookService`: Business logic layer with `@PreAuthorize` role checks
- `SecurityContextUtil` / `ReactiveSecurityContextUtil`: Utility classes for accessing `UserInformation` principal

## License

This is a demonstration project for comparing MVC and WebFlux implementations.
