# Architecture Overview

This document provides a comprehensive architectural overview of the Acme multi-module Spring Boot application, which demonstrates and compares blocking (MVC) and reactive (WebFlux) implementations.

## Table of Contents

- [System Overview](#system-overview)
- [High-Level Architecture](#high-level-architecture)
- [Module Organization](#module-organization)
- [Data Flow](#data-flow)
- [Security Architecture](#security-architecture)
- [Persistence Layer](#persistence-layer)
- [Testing Architecture](#testing-architecture)
- [Deployment Architecture](#deployment-architecture)
- [Technology Stack](#technology-stack)
- [Design Patterns](#design-patterns)
- [Key Architectural Decisions](#key-architectural-decisions)

## System Overview

### Purpose

The Acme application is a demonstration project that implements identical REST API functionality using two different Spring paradigms:

1. **Spring MVC (Blocking)**: Traditional servlet-based, blocking I/O approach
2. **Spring WebFlux (Reactive)**: Modern reactive, non-blocking I/O approach

Both implementations share common security and authentication infrastructure while using paradigm-specific persistence layers.

### Core Functionality

- **Book Management API**: CRUD operations for book resources
- **DN-Based Authentication**: LDAP-like Distinguished Name authentication
- **Role-Based Access Control**: User roles with permission validation
- **External Auth Service**: Centralized authentication service
- **Caching**: User lookup caching to reduce auth service calls
- **Monitoring**: Prometheus metrics and Grafana dashboards

## High-Level Architecture

```none
┌──────────────────────────────────────────┐
│            Web UI (Port 3001)            │
│         Next.js (React/TypeScript)       │
│  - Server-side rendering                 │
│  - API routes for backend communication  │
└───────┬──────────────────────────────────┘
        │
        │ HTTP (x-dn header)
        │
┌───────▼──────────────────────────────────┐
│         Load Balancer / Gateway          │
│         (mTLS with client certs)         │
└───────┬─────────────────────────┬────────┘
        │                         │
┌───────▼─────────┐      ┌────────▼────────┐
│     MVC API     │      │  WebFlux API    │
│   (Port 8080)   │      │  (Port 8081)    │
│                 │      │                 │
│ Spring MVC      │      │ Spring WebFlux  │
│ Servlet Stack   │      │ Netty Stack     │
│ Blocking I/O    │      │ Non-blocking IO │
└───────┬─────────┘      └────────┬────────┘
        │                         │
┌───────▼─────────────────────────▼────────┐
│      Shared Security Infrastructure      │
│  - Header-based Authentication           │
│  - DN Validation                         │
│  - RBAC                                  │
└───────┬─────────────────────────┬────────┘
        │                         │
┌───────▼─────────┐      ┌────────▼────────┐
│   JPA Layer     │      │   R2DBC Layer   │
│  (Hibernate)    │      │    (Reactive)   │
│   Blocking      │      │      Async      │
└───────┬─────────┘      └────────┬────────┘
        │                         │
┌───────▼─────────┐      ┌────────▼────────┐
│   PostgreSQL    │      │   PostgreSQL    │
│    (JPA DB)     │      │   (R2DBC DB)    │
└─────────────────┘      └─────────────────┘
┌──────────────────────────────────────────┐
│      Authentication Service (Port 8082)  │
│  - User Management                       │
│  - DN Lookup                             │
│  - Role Assignment                       │
│  - Two interchangeable variants:         │
│    • LDAP-based (Spring LDAP)            │
│    • PostgreSQL-based (Spring Data JPA)  │
└───────┬─────────────────────────┬────────┘
        │                         │
┌───────▼────────┐      ┌────────▼────────┐
│    OpenLDAP    │      │   PostgreSQL    │
│   (Port 389)   │      │   (Port 5434)   │
│ LDAP Directory │      │  Auth Database  │
└────────────────┘      └─────────────────┘
┌──────────────────────────────────────────┐
│            Monitoring Stack              │
│  - Prometheus (scrapes metrics)          │
│  - Grafana (visualization)               │
│  - mTLS secured endpoints                │
└──────────────────────────────────────────┘
```

## Module Organization

### Module Hierarchy

The project is organized as a Maven multi-module project with clear separation of concerns:

```none
acme (root)
├── acme-pom/                          # Build configuration
│   ├── acme-dependencies/             # Dependency version management (BOM)
│   └── acme-starter-parent/           # Parent POM with plugin configuration
│
├── acme-auth-client/                  # Reusable auth service client library
│
├── acme-auth-service-db/              # Authentication service (PostgreSQL-based)
├── acme-auth-service-ldap/           # Authentication service (LDAP-based)
│
├── acme-security/                     # Shared security infrastructure
│   ├── acme-security-core/            # Core security logic (paradigm-agnostic)
│   ├── acme-security-webmvc/          # MVC-specific security configuration
│   └── acme-security-webflux/         # WebFlux-specific security configuration
│
├── acme-persistence-jpa/              # JPA entities and repositories (blocking)
├── acme-persistence-r2dbc/            # R2DBC entities and repositories (reactive)
│
├── acme-api-mvc/                      # Spring MVC REST API
├── acme-api-webflux/                  # Spring WebFlux REST API
│
├── acme-test-integration-classic/     # Integration test framework (RestTemplate)
└── acme-test-integration-reactive/    # Integration test framework (WebClient)

acme-ui/                                # Next.js web UI (separate from Maven modules)
├── app/                                # Next.js App Router
│   ├── books/                          # Book management pages
│   └── api/                            # API routes (proxies to backend)
├── components/                         # React components
└── lib/                                # Utility functions and API clients
```

### Module Dependencies

```none
acme-api-mvc depends on:
  ├── acme-security-webmvc
  │   └── acme-security-core
  │       └── acme-auth-client
  └── acme-persistence-jpa

acme-api-webflux depends on:
  ├── acme-security-webflux
  │   └── acme-security-core
  │       └── acme-auth-client
  └── acme-persistence-r2dbc

acme-auth-service-db depends on:
  └── (standalone - no internal dependencies)

acme-auth-service-ldap depends on:
  └── (standalone - no internal dependencies)
```

### Module Responsibilities

#### Build Configuration Modules

**acme-dependencies**

- Centralized dependency version management
- Acts as a Bill of Materials (BOM)
- Ensures version consistency across modules
- Defines Spring Boot, Spring Cloud, and third-party library versions

**acme-starter-parent**

- Parent POM for all application modules
- Plugin configuration and management
- Build profiles and compiler settings
- Code quality tool configuration (Spotless)

#### Shared Libraries

**acme-auth-client**

- REST client wrapper for authentication service
- `AuthServiceClient` interface with default `RestTemplate` implementation
- `UserInfo` DTO for user data transfer
- Auto-configuration for easy integration
- Used by both MVC and WebFlux APIs

**acme-test-integration-classic**

- Integration test framework for MVC-style tests
- `IntegrationTestSuite` base class
- `RestRequestBuilder` fluent API for HTTP requests
- Utility classes for test data management
- Test helpers for assertions and JSON manipulation

**acme-test-integration-reactive**

- Integration test framework for reactive tests
- `ReactiveIntegrationTestSuite` base class
- `ReactiveRequestBuilder` and `ReactiveRequest` fluent API
- Returns `Mono<T>` directly for reactive testing
- Parallel to classic framework but with reactive types

#### Security Modules

**acme-security-core**

- Paradigm-agnostic security logic
- `AuthenticationService` interface for user lookup
- `CachedUserLookupService` with Spring Cache support
- `UserInformation` model and utilities
- `SecurityConstants` utility class
- SSL/TLS configuration support

**acme-security-webmvc**

- Spring MVC specific security configuration
- `WebMvcSecurityConfig` with servlet filter chain
- `RequestHeaderExtractor` for DN extraction
- Blocking `SecurityContext` integration
- `DevSecurityConfig` for development mode (disables SSL checks)

**acme-security-webflux**

- Spring WebFlux specific security configuration
- `WebFluxSecurityConfig` with reactive filter chain
- Reactive `ServerHttpRequest` header extraction
- Reactive `ReactiveSecurityContext` integration
- Non-blocking authentication flow

#### Persistence Modules

**acme-persistence-jpa**

- JPA entities with Hibernate
- Spring Data JPA repositories
- `Book` entity with auditing support
- JDBC-based blocking I/O
- Transaction management with `@Transactional`

**acme-persistence-r2dbc**

- R2DBC entities for reactive database access
- Spring Data R2DBC repositories
- `Book` entity (parallel to JPA version)
- Non-blocking database I/O
- Reactive transaction support

#### Application Modules

**acme-auth-service-ldap**

- Standalone Spring Boot microservice
- User management and authentication
- LDAP server (OpenLDAP) for user storage
- REST API for user lookup by DN
- Role assignment and management
- Spring LDAP for LDAP operations
- Runs on port 8082
- See [LDAP.md](LDAP.md) for detailed LDAP directory usage and querying guide

**acme-auth-service-db**

- Standalone Spring Boot microservice
- User management and authentication
- PostgreSQL database for user storage
- REST API for user lookup by DN
- Role assignment and management
- Spring Data JPA for database operations
- Runs on port 8082

**Both services are interchangeable** - they expose the same REST API contract (`/api/auth/users/{dn}`) and return the same response format, making them drop-in replacements for demonstration purposes.

**acme-api-mvc**

- Spring MVC REST API implementation
- Servlet-based request handling
- Blocking I/O throughout the stack
- JPA for database access
- `BookController`, `BookService`, `BookMapper`
- Global exception handler
- Runs on port 8080

**acme-api-webflux**

- Spring WebFlux REST API implementation
- Netty-based request handling
- Non-blocking I/O throughout the stack
- R2DBC for database access
- Same API contract as MVC version
- Returns `Mono<T>` and `Flux<T>`
- Runs on port 8081

**acme-ui**

- Next.js web application (React/TypeScript)
- Server-side rendering with App Router
- Client-side interactivity for forms and actions
- API routes that proxy requests to backend with `x-dn` header
- Automatic header injection from environment variables
- Book management interface (list, create, edit, delete)
- Runs on port 3001
- Uses pnpm for package management

**Note:** The UI is a separate Node.js application, not a Maven module. It communicates with the backend APIs via HTTP, automatically including the `x-dn` header from environment configuration for local development.

## Data Flow

### MVC Request Flow

```none
HTTP Request
    │
    ▼
┌─────────────────────────────────────┐
│  Servlet Container (Tomcat)         │
│  - One thread per request           │
│  - Thread blocks on I/O             │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Spring Security Filter Chain       │
│  - Extract DN from x-dn header      │
│  - Validate DN format               │
│  - Check cache for user             │
│  - Call auth service if not cached  │  ◄── Blocking HTTP call
│  - Create Authentication object     │
│  - Set SecurityContext              │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Controller Layer                   │
│  - @GetMapping, @PostMapping, etc.  │
│  - Validate request                 │
│  - Extract SecurityContext          │  ◄── Thread-local storage
│  - Call service layer               │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Service Layer                      │
│  - Business logic                   │
│  - MapStruct mapping                │
│  - Call repository                  │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  JPA Repository                     │
│  - Query generation                 │
│  - Hibernate session                │
│  - JDBC driver call                 │  ◄── Blocking I/O
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  PostgreSQL Database                │
│  - Execute query                    │
│  - Return results                   │
└─────────────┬───────────────────────┘
              │
              ▼
Response Entity ◄─── Thread returns
```

**Characteristics:**

- **Thread Model**: One thread per request (thread-per-request model)
- **Blocking Points**:
  - Auth service HTTP call
  - Database queries
  - Any external service calls
- **Concurrency**: Limited by thread pool size
- **Predictability**: Simple mental model, easier debugging
- **Resource Usage**: Higher memory per request (thread stack)

### WebFlux Request Flow

```none
HTTP Request
    │
    ▼
┌─────────────────────────────────────┐
│  Netty Event Loop                   │
│  - Small number of worker threads   │
│  - Event-driven, non-blocking       │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Reactive Security Filter Chain     │
│  - Extract DN from ServerHttpRequest│
│  - Mono<Authentication> pipeline    │
│  - Reactive cache lookup            │
│  - WebClient call if cache miss     │  ◄── Non-blocking
│  - ReactiveSecurityContext          │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Handler Function / Controller      │
│  - @GetMapping returns Mono<T>      │
│  - Reactive validation              │
│  - ReactiveSecurityContext access   │  ◄── Context propagation
│  - Return Mono/Flux                 │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  Service Layer                      │
│  - Reactive operators               │
│  - MapStruct mapping                │
│  - Return Mono/Flux                 │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  R2DBC Repository                   │
│  - Reactive query generation        │
│  - Non-blocking driver              │
│  - Return Mono/Flux                 │  ◄── Async I/O
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│  PostgreSQL Database                │
│  - Async protocol                   │
│  - Callback on completion           │
└─────────────┬───────────────────────┘
              │
              ▼
Mono<ResponseEntity> ◄─── Event loop continues
```

**Characteristics:**

- **Thread Model**: Event loop with small, fixed thread pool
- **Non-Blocking**: All I/O operations are asynchronous
- **Concurrency**: Much higher (thousands of concurrent requests)
- **Backpressure**: Built-in flow control
- **Complexity**: Steeper learning curve, harder debugging
- **Resource Usage**: Lower memory per request

## Security Architecture

### Authentication Flow

Both MVC and WebFlux use the same conceptual authentication flow:

```none
1. Client Request
   ├─ Header: x-dn: cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org
   └─ HTTPS with mTLS (in production)

2. Security Filter/WebFilter
   ├─ Extract DN from x-dn header
   ├─ Validate DN format
   └─ Normalize DN (trim whitespace)

3. User Lookup (with caching)
   ├─ Check Spring Cache
   │  ├─ Cache Hit: Return cached UserInfo
   │  └─ Cache Miss: Call Auth Service
   │     ├─ HTTP GET /api/auth/users/{dn}
   │     ├─ Auth Service queries user store (LDAP or DB) by DN
   │     ├─ User store returns user attributes and roles
   │     ├─ Deserialize UserInfo response
   │     └─ Store in cache
   └─ Return UserInfo

4. Authentication Object
   ├─ Create UserInformation from UserInfo
   ├─ Create Authentication object
   └─ Set in SecurityContext (MVC) or ReactiveSecurityContext (WebFlux)

5. Request Processing
   ├─ Controller extracts user info
   ├─ Service layer performs business logic
   └─ Audit fields populated with user DN

6. Response
   └─ Return result to client
```

### Security Components

**SecurityConstants**

- Utility class with security-related constants
- DN header name (`x-dn`)
- Public endpoints (Swagger, Actuator)
- Error messages

**UserInformation**

- Immutable model representing authenticated user
- Contains DN (Distinguished Name)
- Used throughout application for user identification

**UserInformationUtil**

- Static utility for creating `UserInformation` objects
- Validates and normalizes DN strings
- Converts `UserInfo` from auth service

**CachedUserLookupService**

- Implements caching for user lookups
- Reduces load on authentication service
- Uses Spring Cache abstraction
- Cache key: DN string
- TTL configured per environment

**DevSecurityConfig**

- Development-only configuration
- Disables SSL hostname verification
- Allows self-signed certificates
- **Must not be used in production**

**RequestResponseLoggingFilter** (MVC)

- Logs request and response headers for debugging and monitoring
- Executes once per request via `OncePerRequestFilter`
- Skips logging for Prometheus endpoint to reduce noise
- Logs at DEBUG level when enabled
- Formats headers with consistent structure

**RequestResponseLoggingWebFilter** (WebFlux)

- Reactive equivalent of `RequestResponseLoggingFilter`
- Logs request and response headers using `WebFilter`
- Uses exchange attributes to prevent duplicate logging
- Skips logging for Prometheus endpoint
- Logs at DEBUG level when enabled

### Role-Based Access Control

Currently the system has basic role support:

- Users have assigned roles (stored in auth service)
- Roles are included in `UserInfo`
- Can be extended for method-level security with `@PreAuthorize`

## Persistence Layer

### JPA (MVC)

**Technology Stack:**

- Spring Data JPA
- Hibernate ORM
- PostgreSQL JDBC driver
- Flyway migrations

**Entity Example:**

```java
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String isbn;

    // Audit fields
    private LocalDateTime createdAt;
    private String createdBy;
}
```

**Repository:**

```java
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
}
```

**Characteristics:**

- Blocking I/O
- JDBC connection pooling (HikariCP)
- Lazy loading support
- Second-level cache support
- Complex query support (Criteria API, JPQL)

### R2DBC (WebFlux)

**Technology Stack:**

- Spring Data R2DBC
- PostgreSQL R2DBC driver
- Flyway migrations (run separately)

**Entity Example:**

```java
@Table("books")
public class Book {
    @Id
    private Long id;

    private String isbn;

    // Audit fields
    private LocalDateTime createdAt;
    private String createdBy;
}
```

**Repository:**

```java
public interface BookRepository extends ReactiveCrudRepository<Book, Long> {
    Mono<Book> findByIsbn(String isbn);
    Mono<Boolean> existsByIsbn(String isbn);
}
```

**Characteristics:**

- Non-blocking I/O
- Reactive connection pool
- No lazy loading (not applicable)
- Simpler query support (no JPA features)
- Returns `Mono<T>` and `Flux<T>`

### Database Migrations

Both implementations use Flyway for database migrations, but applied differently:

- **MVC**: Flyway runs automatically on application startup
- **WebFlux**: Migrations run via separate command (`make migrate-r2dbc`)
  - R2DBC doesn't support synchronous migration on startup
  - Separation of concerns: schema vs. data access

## Testing Architecture

### Integration Testing

Both APIs have comprehensive integration test suites with fluent APIs:

**Classic (MVC):**

```java
@Test
void testGetBook() {
    ResponseEntity<BookResponse> response = getRequest("/api/v1/books/1")
        .retrieve(BookResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getTitle()).isEqualTo("1984");
}
```

**Reactive (WebFlux):**

```java
@Test
void testGetBook() {
    Mono<BookResponse> responseMono = getRequest("/api/v1/books/1")
        .retrieve(BookResponse.class);

    StepVerifier.create(responseMono)
        .assertNext(book -> {
            assertThat(book.getTitle()).isEqualTo("1984");
        })
        .verifyComplete();
}
```

### Test Infrastructure

**IntegrationTestSuite**

- Base class for MVC integration tests
- Manages test server lifecycle
- Provides default headers (DN)
- Helper methods for common operations

**ReactiveIntegrationTestSuite**

- Base class for WebFlux integration tests
- WebClient configuration
- Reactive StepVerifier assertions
- Fluent request builders

**Request Builders**

- `RestRequestBuilder`: Fluent API for `RestTemplate`
- `ReactiveRequestBuilder`: Fluent API for `WebClient`
- Convenience methods: `contentTypeJson()`, `bearerToken()`, etc.
- Terminal operations return appropriate types

## Deployment Architecture

### Local Development

```none
Developer Machine
├── Web UI (localhost:3001)
│   └── Next.js development server
├── MVC API (localhost:8080)
├── WebFlux API (localhost:8081)
├── Auth Service (localhost:8082)
├── PostgreSQL (Docker)
│   ├── Port 5432 (JPA database)
│   ├── Port 5433 (R2DBC database)
│   └── Port 5434 (Auth database)
├── OpenLDAP (Docker)
│   └── Port 389 (LDAP directory)
├── Prometheus (localhost:9090)
└── Grafana (localhost:3000)
```

### Production Deployment

```none
Load Balancer (mTLS)
    ├── API Gateway
    │   ├── MVC Instances (horizontal scaling)
    │   │   └── Connection Pool (limited by threads)
    │   └── WebFlux Instances (horizontal scaling)
    │       └── Reactive Pool (higher concurrency)
    │
    ├── Auth Service Cluster
    │   └── Multiple instances behind LB
    │
    ├── Database Cluster
    │   ├── Primary (write)
    │   └── Replicas (read)
    │
    └── Monitoring Stack
        ├── Prometheus (scraping)
        └── Grafana (visualization)
```

### Container Deployment

Each service has a multi-stage Dockerfile:

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
# ... build application

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
# ... run application
```

Services orchestrated with Docker Compose:

- Networking
- Volume management
- Environment configuration
- Health checks

## Technology Stack

### Core Frameworks

| Component     | MVC             | WebFlux         | Auth (LDAP)     | Auth (DB)        | UI           |
|---------------|-----------------|-----------------|-----------------|------------------|--------------|
| Framework     | Spring Boot 3.5 | Spring Boot 3.5 | Spring Boot 3.5 | Spring Boot 3.5  | Next.js 16.1 |
| Web Framework | Spring MVC      | Spring WebFlux  | Spring MVC      | Spring MVC       | React 19.2   |
| Server        | Tomcat          | Netty           | Tomcat          | Tomcat           | Next.js Dev  |
| Persistence   | JPA + Hibernate | R2DBC           | Spring LDAP     | Spring Data JPA  | N/A          |
| Database      | PostgreSQL      | PostgreSQL      | OpenLDAP        | PostgreSQL       | N/A          |
| HTTP Client   | RestTemplate    | WebClient       | N/A             | N/A              | Fetch API    |
| Language      | Java 17         | Java 17         | Java 17         | Java 17          | TypeScript 5 |

### Supporting Libraries

- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **Mapping**: MapStruct 1.6.x
- **Lombok**: Code generation (getters, builders, etc.)
- **OpenAPI**: SpringDoc for API documentation
- **Caching**: Spring Cache with Caffeine
- **Metrics**: Micrometer + Prometheus
- **Migrations**: Flyway
- **Testing**: JUnit 5, AssertJ, Reactor Test

### Build Tools

- **Build System**: Maven 3.9+ (Java modules), pnpm (UI)
- **Java Version**: 17 (LTS)
- **Node.js**: 20+ (for UI)
- **Code Formatting**: Spotless (Java), ESLint (TypeScript)
- **Containerization**: Docker + Docker Compose

## Design Patterns

### Used in the Application

**Dependency Injection (DI)**

- Constructor injection throughout
- Interface-based design
- Spring manages bean lifecycle

**Builder Pattern**

- Lombok `@Builder` on DTOs and entities
- Fluent request builders for tests
- Improves readability and immutability

**Repository Pattern**

- Spring Data repositories
- Abstraction over persistence
- Common interface for both JPA and R2DBC

**Mapper Pattern**

- MapStruct for DTO ↔ Entity conversion
- Compile-time code generation
- Type-safe transformations

**Utility Class Pattern**

- `SecurityConstants`, `UserInformationUtil`, etc.
- Private constructor via `@NoArgsConstructor(access = AccessLevel.PRIVATE)`
- Prevents instantiation

**Strategy Pattern**

- `AuthenticationService` interface
- Multiple implementations (caching vs. non-caching)
- Runtime selection via Spring configuration

**Template Method Pattern**

- `IntegrationTestSuite` and `ReactiveIntegrationTestSuite`
- Subclasses override specific behaviors
- Shared test infrastructure

**Filter Chain Pattern**

- Spring Security filter chain
- Request processing pipeline
- Authentication and authorization

## Key Architectural Decisions

### 1. Separate Persistence Modules

**Decision:** Create separate modules for JPA and R2DBC instead of trying to support both in one module.

**Rationale:**

- JPA and R2DBC have fundamentally different APIs
- Blocking vs. non-blocking can't be mixed
- Clearer separation of concerns
- Easier to maintain and test

**Trade-offs:**

- Code duplication (entities, repositories)
- Two separate databases (complexity in deployment)
- Benefits outweigh costs for this demonstration

### 2. Shared Security Core

**Decision:** Extract paradigm-agnostic security logic into `acme-security-core`.

**Rationale:**

- DN validation is the same regardless of paradigm
- UserInformation model is shared
- Auth service client is reused
- Reduces duplication

**Implementation:**

- Core module: Interfaces and models
- WebMVC module: Servlet filter implementation
- WebFlux module: Reactive filter implementation

### 3. External Authentication Service

**Decision:** Create standalone auth service instead of embedding in APIs.

**Rationale:**

- Simulates real-world microservice architecture
- Single source of truth for user data
- Can be scaled independently
- Demonstrates HTTP client usage

**Considerations:**

- Additional network latency
- Mitigated by caching
- More realistic for production scenarios

### 4. Caching Strategy

**Decision:** Implement user lookup caching at the security layer.

**Rationale:**

- Reduces load on auth service
- Improves response time
- Simple cache key (DN string)
- Spring Cache abstraction allows swapping implementations

**Configuration:**

- Cache provider: Caffeine (in-memory)
- TTL: Configurable per environment
- Eviction: Size-based + time-based

### 5. Fluent Test APIs

**Decision:** Create fluent request builders for integration tests.

**Rationale:**

- Improves test readability
- Reduces boilerplate
- Consistent API across classic and reactive tests
- Type-safe request construction

**Benefits:**

- Tests are easier to write
- Changes to request structure centralized
- Better developer experience

### 6. Multi-Module Maven Structure

**Decision:** Use Maven multi-module project with deep hierarchy.

**Rationale:**

- Clear module boundaries
- Dependency management
- Selective building
- Easier to reason about dependencies

**Structure:**

- Root aggregator POM
- Parent POMs for shared configuration
- Library modules
- Application modules

### 7. MapStruct for Mapping

**Decision:** Use MapStruct instead of manual mapping or reflection-based mappers.

**Rationale:**

- Compile-time code generation
- Type-safe
- Better performance than reflection
- IDE support for navigation

**Configuration:**

- Annotation processing in Maven
- Lombok integration
- Component model: Spring (`@Component`)

### 8. Development vs. Production Configuration

**Decision:** Separate dev and prod configurations with profile-specific beans.

**Rationale:**

- Development needs (self-signed certs)
- Production security requirements
- Clear separation via `@Profile("dev")`

**Implementation:**

- `DevSecurityConfig` only active in dev profile
- SSL verification disabled locally
- Stricter validation in production

## Comparison: MVC vs WebFlux

### When to Use MVC

✅ Good fit for:

- CRUD applications with simple I/O
- Existing team knowledge with Spring MVC
- Blocking third-party libraries
- Simpler debugging requirements
- Lower traffic applications

### When to Use WebFlux

✅ Good fit for:

- High concurrency requirements
- Streaming data / SSE
- Microservices with many I/O calls
- Modern reactive stack (R2DBC, WebClient)
- Backpressure requirements

### Performance Characteristics

**MVC:**

- Throughput: Limited by thread pool size
- Latency: Predictable, linear scaling
- Memory: Higher per request (thread stack ~1MB)
- Max Concurrent: ~200-500 requests (typical)

**WebFlux:**

- Throughput: Much higher (event-driven)
- Latency: Can be lower with proper tuning
- Memory: Lower per request (no thread per request)
- Max Concurrent: Thousands to tens of thousands

## Monitoring and Observability

### Metrics

All services expose Prometheus metrics at `/actuator/prometheus`:

- JVM metrics (heap, threads, GC)
- HTTP request metrics (count, duration, errors)
- Database connection pool metrics
- Cache metrics
- Custom business metrics

### Endpoints

- **Prometheus**: `http://localhost:9090`
- **Grafana**: `http://localhost:3000`
- **MVC Metrics**: `https://localhost:8080/actuator/prometheus`
- **WebFlux Metrics**: `https://localhost:8081/actuator/prometheus`

Monitoring uses mTLS for secure communication.

## Future Enhancements

Potential architectural improvements:

1. **Event-Driven Architecture**: Add messaging (Kafka, RabbitMQ)
2. **CQRS**: Separate read and write models
3. **Service Mesh**: Istio or Linkerd for cross-cutting concerns
4. **Distributed Tracing**: Zipkin or Jaeger
5. **API Gateway**: Spring Cloud Gateway
6. **Config Server**: Externalized configuration
7. **Circuit Breaker**: Resilience4j integration

## References

- [Spring MVC Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/)
- [Spring Data R2DBC](https://docs.spring.io/spring-data/r2dbc/reference/)
- [Project Reactor](https://projectreactor.io/docs)
- [Reactive Streams Specification](https://www.reactive-streams.org/)
