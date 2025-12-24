# Acme Multi-Module Spring Boot Application

A multi-module Spring Boot application comparing MVC (blocking) and WebFlux (reactive) implementations with header-based authentication and role-based access control.

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
│   ├── acme-dependencies/          # BOM for dependency versions
│   └── acme-starter-parent/        # Parent POM with plugin management
├── acme-security/                   # Security layer
│   ├── acme-security-core/         # Core security logic
│   ├── acme-security-webmvc/      # MVC security configuration
│   └── acme-security-webflux/      # WebFlux security configuration
├── acme-persistence/                # Persistence layer
│   ├── acme-persistence-jpa/       # JPA repositories and entities
│   └── acme-persistence-r2dbc/     # R2DBC repositories and entities
└── acme-api/                        # API layer
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
make databases-up
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

### Security Layer

Both implementations use header-based authentication:

- Extracts `x-username` header from HTTP requests
- Creates `UserPrincipal` with dummy roles (ROLE_USER, ROLE_ADMIN, ROLE_OPERATOR)
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
- Custom `AuthenticationManager` uses `UserLookupService`
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
- Reactive `AuthenticationManager` returns `Mono<Authentication>`
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
- **Same User Principal**: Both create `UserPrincipal` with same structure and roles
- **Same Role-Based Access Control**: Both use `@PreAuthorize` annotations
- **Same API Endpoints**: Both expose `/api/books` with same CRUD operations
- **Same Business Logic**: Same service layer functionality
- **Same Database Schema**: Both use identical `books` table structure
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

Both implementations extract the `x-username` header from HTTP requests. The header value is used to create a `UserPrincipal` with predefined roles.

### Missing Header

If the `x-username` header is missing or empty, both implementations return `401 Unauthorized`.

### User Creation

`UserLookupService` creates a `UserPrincipal` with:

- Username from header value
- Dummy roles: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_OPERATOR`

### Role-Based Access Control

Service methods can be protected with annotations like:

```java
@PreAuthorize("hasRole('ADMIN')")
```

### Deployment Context

Applications run in HTTP (no SSL/TLS). SSL/TLS termination is handled by the ingress layer above, which forwards headers (including `x-username`) to the applications.

## Testing the APIs

### Example Request (MVC)

```bash
curl -H "x-username: Bob" http://localhost:8080/api/books
```

### Example Request (WebFlux)

```bash
curl -H "x-username: Bob" http://localhost:8081/api/books
```

### Missing Header (Returns 401)

```bash
curl http://localhost:8080/api/books  # Returns 401 Unauthorized
```

### CRUD Operations

**Create Book:**

```bash
curl -X POST -H "x-username: Bob" -H "Content-Type: application/json" \
  -d '{"title":"Test Book","author":"Test Author","isbn":"123-456-789"}' \
  http://localhost:8080/api/books
```

**Get All Books:**

```bash
curl -H "x-username: Bob" http://localhost:8080/api/books
```

**Get Book by ID:**

```bash
curl -H "x-username: Bob" http://localhost:8080/api/books/1
```

**Update Book:**

```bash
curl -X PUT -H "x-username: Bob" -H "Content-Type: application/json" \
  -d '{"title":"Updated Title","author":"Updated Author"}' \
  http://localhost:8080/api/books/1
```

**Delete Book:**

```bash
curl -X DELETE -H "x-username: Bob" http://localhost:8080/api/books/1
```

## Development Workflow

1. **Start databases:**

   ```bash
   make databases-up
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

- `make databases-up` - Start both PostgreSQL databases
- `make databases-down` - Stop both PostgreSQL databases
- `make databases-logs` - View database logs
- `make build` - Build all Maven modules
- `make clean` - Clean all Maven modules
- `make test` - Run all tests
- `make run-mvc` - Build and run MVC API
- `make run-webflux` - Build and run WebFlux API
- `make docker-build-mvc` - Build Docker image for MVC API
- `make docker-build-webflux` - Build Docker image for WebFlux API
- `make docker-run-mvc` - Run MVC API in Docker container
- `make docker-run-webflux` - Run WebFlux API in Docker container

## Project Structure Details

### Module Organization

- **acme-pom**: Dependency management (BOM and parent POM)
- **acme-security**: Security layer with core logic and framework-specific configs
- **acme-persistence**: Data access layer with JPA and R2DBC implementations
- **acme-api**: API layer with MVC and WebFlux REST controllers

### Dependency Relationships

- `acme-api-mvc` depends on `acme-security-webmvc` and `acme-persistence-jpa`
- `acme-api-webflux` depends on `acme-security-webflux` and `acme-persistence-r2dbc`
- All modules inherit from `acme-starter-parent` which inherits from `acme-dependencies`

## Key Components

### Security

- `UserPrincipal`: Custom user principal with roles
- `UserLookupService`: Creates user from header value
- `WebMvcSecurityConfig`: MVC security configuration
- `WebFluxSecurityConfig`: WebFlux security configuration

### Persistence

- `Book`: Entity class (JPA and R2DBC versions)
- `BookRepository`: Repository interface (JpaRepository and ReactiveCrudRepository)
- Flyway migrations: V1 (schema) and V2 (seed data)

### API

- `BookController`: REST controller with CRUD endpoints
- `BookService`: Business logic layer
- `SecurityContextUtil` / `ReactiveSecurityContextUtil`: Utility classes for accessing principal

## License

This is a demonstration project for comparing MVC and WebFlux implementations.
