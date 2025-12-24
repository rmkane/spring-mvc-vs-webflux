---
name: Multi-module Spring Boot Application
overview: Create a multi-module Spring Boot 3.5.6 Maven project with parent-pom, security (core/webmvc/webflux), persistence (jpa/r2dbc), and api (mvc/webflux) modules. Security will authenticate via x-username header using core user lookup service. MVC API uses JPA persistence, WebFlux API uses R2DBC persistence.
todos: []
---

# Multi-Module Spring Boot Application Structure

## Project Structure

```javascript
spring-mvc-vs-webflux/
├── pom.xml (root aggregator: org.acme)
├── Makefile (database and application management)
├── docker-compose.yml (PostgreSQL databases for JPA and R2DBC)
├── acme-pom/
│   ├── pom.xml (org.acme:acme-pom - parent aggregator)
│   ├── acme-dependencies/
│   │   └── pom.xml (org.acme:acme-dependencies, no parent, imports Spring Boot BOM)
│   └── acme-starter-parent/
│       └── pom.xml (org.acme:acme-starter-parent, parent: acme-dependencies)
├── acme-security/
│   ├── pom.xml (org.acme.security:acme-security, parent: acme-starter-parent)
│   ├── acme-security-core/
│   │   └── pom.xml (org.acme.security:acme-security-core) + UserLookupService
│   ├── acme-security-webmvc/
│   │   └── pom.xml (org.acme.security:acme-security-webmvc) + WebMvcSecurityConfig
│   └── acme-security-webflux/
│       └── pom.xml (org.acme.security:acme-security-webflux) + WebFluxSecurityConfig
├── acme-persistence/
│   ├── pom.xml (org.acme.persistence:acme-persistence, parent: acme-starter-parent)
│   ├── acme-persistence-jpa/
│   │   ├── pom.xml (org.acme.persistence:acme-persistence-jpa) + JPA repositories, entities
│   │   └── src/main/resources/db/migration/
│   │       ├── V1__Initial_schema.sql (Flyway migration)
│   │       └── V2__Seed_data.sql (Flyway migration with seed data)
│   └── acme-persistence-r2dbc/
│       ├── pom.xml (org.acme.persistence:acme-persistence-r2dbc) + R2DBC repositories, entities
│       └── src/main/resources/db/migration/
│           ├── V1__Initial_schema.sql (Flyway migration)
│           └── V2__Seed_data.sql (Flyway migration with seed data)
└── acme-api/
    ├── pom.xml (org.acme.api:acme-api, parent: acme-starter-parent)
    ├── acme-api-mvc/
    │   ├── pom.xml (org.acme.api:acme-api-mvc) + MVC controllers, services
    │   ├── Dockerfile (containerization for MVC API)
    │   └── src/main/resources/application.yml (JPA database configuration)
    │   └── **DEPENDS ON: acme-persistence-jpa** (JPA for blocking operations)
    └── acme-api-webflux/
        ├── pom.xml (org.acme.api:acme-api-webflux) + WebFlux controllers, reactive services
        ├── Dockerfile (containerization for WebFlux API)
        └── src/main/resources/application.yml (R2DBC database configuration)
        └── **DEPENDS ON: acme-persistence-r2dbc** (R2DBC for reactive operations)
```



## Critical Dependency Rules

### API MVC → JPA Persistence

- **acme-api-mvc** MUST depend on **acme-persistence-jpa**
- Uses blocking JPA repositories (`JpaRepository`)
- Traditional synchronous database operations
- Returns `ResponseEntity<T>` and standard Java objects

### API WebFlux → R2DBC Persistence

- **acme-api-webflux** MUST depend on **acme-persistence-r2dbc**
- Uses reactive R2DBC repositories (`ReactiveCrudRepository`)
- Non-blocking reactive database operations
- Returns `Mono<T>` and `Flux<T>`

## Implementation Details

### 1. Root Aggregator POM ([pom.xml](pom.xml))

- Group ID: `org.acme`
- Artifact ID: `acme` (or root project name)
- Packaging type: `pom`
- Declares `acme-pom`, `acme-security`, `acme-persistence`, and `acme-api` as modules
- Simple aggregator that groups all top-level modules together

### 2. Acme POM ([acme-pom/pom.xml](acme-pom/pom.xml))

- Group ID: `org.acme`
- Artifact ID: `acme-pom`
- Packaging type: `pom`
- Higher-order parent POM that aggregates `acme-dependencies` and `acme-starter-parent` as submodules
- Acts as a container/organizer for the dependency management modules

### 3. Acme Dependencies ([acme-pom/acme-dependencies/pom.xml](acme-pom/acme-dependencies/pom.xml))

- Group ID: `org.acme`
- Artifact ID: `acme-dependencies`
- Packaging type: `pom`
- **Has no parent** (standalone, similar to `spring-boot-dependencies`)
- Imports Spring Boot 3.5.6 BOM via `dependencyManagement`
- Manages versions for any other dependencies not covered by Spring Boot BOM
- Manages Lombok version in `dependencyManagement`
- Manages JPA/Spring Data JPA dependencies
- Manages R2DBC dependencies for reactive database access
- Manages Flyway dependencies for database migrations
- Acts as a BOM (Bill of Materials) for dependency version management

### 4. Acme Starter Parent ([acme-pom/acme-starter-parent/pom.xml](acme-pom/acme-starter-parent/pom.xml))

- Group ID: `org.acme`
- Artifact ID: `acme-starter-parent`
- Packaging type: `pom`
- Parent: `org.acme:acme-dependencies` (similar to `spring-boot-starter-parent`)
- Provides plugin management (Maven compiler, surefire, etc.)
- Configures Maven compiler plugin with annotation processing for Lombok
- Sets default properties and configurations
- This is the parent that `security`, `persistence`, and `api` modules will reference

### 5. Security Module Structure

#### Security Parent ([acme-security/pom.xml](acme-security/pom.xml))

- Group ID: `org.acme.security`
- Artifact ID: `acme-security`
- Packaging type: `pom`
- Parent: `org.acme:acme-starter-parent`
- Declares `acme-security-core`, `acme-security-webmvc`, and `acme-security-webflux` as modules
- Includes Lombok dependency (provided scope for annotation processing)

#### Security Core ([acme-security/acme-security-core/](acme-security/acme-security-core/))

- Group ID: `org.acme.security`
- Artifact ID: `acme-security-core`
- Contains `UserLookupService` that takes a username (from header) and creates a custom user principal
- Contains custom `UserPrincipal` class that implements `UserDetails` or extends Spring Security's `User`
- Simple implementation: creates a `UserPrincipal` object with the header value as the username (e.g., "Bob")
- `UserPrincipal` includes dummy roles/authorities (e.g., "ROLE_USER", "ROLE_ADMIN", "ROLE_OPERATOR") for role-based access control
- Roles are assigned to the user object and available via `getAuthorities()` method
- No database lookup - directly creates user object from header value with predefined roles
- The custom user object will be set as the principal in the SecurityContext authentication
- **Service layer access**:
- **MVC**: Use `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` (thread-local works)
- **WebFlux**: Use `@AuthenticationPrincipal` in controllers or `ReactiveSecurityContextHolder` in reactive chains (thread-local does NOT work)
- Service layer can retrieve user directly from SecurityContext (no serialization needed)
- Uses Lombok annotations (annotation processing configured via parent)

#### Security WebMVC ([acme-security/acme-security-webmvc/](acme-security/acme-security-webmvc/))

- Group ID: `org.acme.security`
- Artifact ID: `acme-security-webmvc`
- Depends on `acme-security-core`
- Uses Lombok annotations (annotation processing configured via parent)
- Contains `WebMvcSecurityConfig` that:
- Configures `RequestHeaderAuthenticationFilter` to read `x-username` header from HTTP request
- **Header Extraction**: Filter extracts `x-username` header value from `HttpServletRequest`
- **Missing Header Handling**: If `x-username` header is missing or empty, authentication fails and returns `401 Unauthorized` response
- **Error Response**: Missing header triggers `AuthenticationException` which is handled by Spring Security to return HTTP 401
- **User Lookup**: Extracted header value (e.g., "Bob") is passed to `UserLookupService.createUser(username)`
- `UserLookupService` creates custom `UserPrincipal` with header value as username and dummy roles
- Sets the custom user object as the principal in SecurityContext authentication
- **Authentication Manager**: Custom `AuthenticationManager` that uses `UserLookupService` to authenticate
- Enables method security (`@EnableMethodSecurity`) for role-based access control
- Sets up Spring Security filter chain for MVC
- **HTTP Only**: Applications run in HTTP (no SSL/TLS - handled by ingress layer above)
- **Ingress Context**: Headers (including `x-username`) are supplied through the ingress layer, which handles SSL/TLS termination and forwards headers to the application

#### Security WebFlux ([acme-security/acme-security-webflux/](acme-security/acme-security-webflux/))

- Group ID: `org.acme.security`
- Artifact ID: `acme-security-webflux`
- Depends on `acme-security-core`
- Uses Lombok annotations (annotation processing configured via parent)
- Contains `WebFluxSecurityConfig` that:
- Configures reactive authentication manager (`ReactiveAuthenticationManager`)
- **Header Extraction**: Custom `ServerHttpAuthenticationConverter` extracts `x-username` header from `ServerWebExchange`
- **Missing Header Handling**: If `x-username` header is missing or empty, authentication fails and returns `401 Unauthorized` response (Mono.error with AuthenticationException)
- **Error Response**: Missing header triggers `AuthenticationException` wrapped in Mono, handled by Spring Security to return HTTP 401
- **User Lookup**: Extracted header value (e.g., "Bob") is passed to `UserLookupService.createUser(username)` (blocking call wrapped in Mono)
- Uses `UserLookupService` from core to create custom `UserPrincipal` with dummy roles
- Sets the custom user object as the principal in SecurityContext authentication
- **Reactive Authentication**: Authentication manager returns `Mono<Authentication>` for reactive chain
- Enables reactive method security (`@EnableReactiveMethodSecurity`) for role-based access control
- Sets up Spring Security WebFilter chain for WebFlux
- **HTTP Only**: Applications run in HTTP (no SSL/TLS - handled by ingress layer above)
- **Ingress Context**: Headers (including `x-username`) are supplied through the ingress layer, which handles SSL/TLS termination and forwards headers to the application

### 6. Persistence Module Structure

#### Persistence Parent ([acme-persistence/pom.xml](acme-persistence/pom.xml))

- Group ID: `org.acme.persistence`
- Artifact ID: `acme-persistence`
- Packaging type: `pom`
- Parent: `org.acme:acme-starter-parent`
- Declares `acme-persistence-jpa` and `acme-persistence-r2dbc` as modules
- Includes Lombok dependency (provided scope for annotation processing)

#### Persistence JPA ([acme-persistence/acme-persistence-jpa/](acme-persistence/acme-persistence-jpa/))

- Group ID: `org.acme.persistence`
- Artifact ID: `acme-persistence-jpa`
- **Used by: acme-api-mvc** (MVC API layer)
- Uses Lombok annotations (annotation processing configured via parent)
- Contains JPA entities (e.g., `Book` entity with `@Entity`, `@Table`, `@Id`, etc.)
- Contains Spring Data JPA repositories (e.g., `BookRepository extends JpaRepository<Book, Long>`)
- Traditional blocking database access using JPA/Hibernate
- Example: `Book` entity with fields like `id`, `title`, `author`, `isbn`, etc.
- Repository provides standard blocking CRUD methods via Spring Data JPA
- **Blocking operations**: All repository methods are synchronous and blocking
- **Flyway Migrations**: 
- `V1__Initial_schema.sql` - Creates `books` table and schema
- `V2__Seed_data.sql` - Inserts seed data for development/testing
- Migrations located in `src/main/resources/db/migration/`
- Flyway dependency included in POM
- Migrations run automatically on application startup

#### Persistence R2DBC ([acme-persistence/acme-persistence-r2dbc/](acme-persistence/acme-persistence-r2dbc/))

- Group ID: `org.acme.persistence`
- Artifact ID: `acme-persistence-r2dbc`
- **Used by: acme-api-webflux** (WebFlux API layer)
- Uses Lombok annotations (annotation processing configured via parent)
- Contains R2DBC entities (e.g., `Book` entity with `@Table`, `@Id`, etc. - R2DBC annotations)
- Contains Spring Data R2DBC repositories (e.g., `BookRepository extends ReactiveCrudRepository<Book, Long>`)
- Reactive, non-blocking database access using R2DBC
- Example: `Book` entity with same structure as JPA version but using R2DBC annotations
- Repository provides reactive CRUD methods returning `Mono<T>` and `Flux<T>`
- **Non-blocking operations**: All repository methods are reactive and non-blocking
- **Flyway Migrations**: 
- `V1__Initial_schema.sql` - Creates `books` table and schema
- `V2__Seed_data.sql` - Inserts seed data for development/testing
- Migrations located in `src/main/resources/db/migration/`
- Flyway dependency included in POM
- Migrations run automatically on application startup

### 7. API Module Structure

#### API Parent ([acme-api/pom.xml](acme-api/pom.xml))

- Group ID: `org.acme.api`
- Artifact ID: `acme-api`
- Packaging type: `pom`
- Parent: `org.acme:acme-starter-parent`
- Declares `acme-api-mvc` and `acme-api-webflux` as modules
- Includes Lombok dependency (provided scope for annotation processing)

#### API MVC ([acme-api/acme-api-mvc/](acme-api/acme-api-mvc/))

- Group ID: `org.acme.api`
- Artifact ID: `acme-api-mvc`
- **Depends on:**
- `acme-security-webmvc` (for MVC security configuration)
- **`acme-persistence-jpa`** (for JPA/blocking database access)
- Uses Lombok annotations (annotation processing configured via parent)
- Contains REST controllers with **idiomatic MVC patterns**:
- Returns `ResponseEntity<T>` for HTTP responses
- Uses traditional blocking service calls
- Returns standard Java objects (not reactive types)
- Uses `@RestController`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, etc.
- **Example endpoint**: `/api/books` with CRUD operations:
- `POST /api/books` - Create a new book
- `GET /api/books` - Retrieve all books
- `GET /api/books/{id}` - Read/retrieve a single book by ID
- `PUT /api/books/{id}` - Update an existing book
- `DELETE /api/books/{id}` - Delete a book
- Contains service classes that use **JPA repositories** (blocking):
- `BookService` with methods: `create()`, `findAll()`, `findById()`, `update()`, `delete()`
- All methods are blocking and return standard Java types (`Book`, `List<Book>`, `void`)
- Methods protected by role-based access control (e.g., `@PreAuthorize("hasRole('ADMIN')")`)
- **Accessing current user in MVC**:
- Controllers can inject principal via `@AuthenticationPrincipal UserPrincipal principal` parameter
- Service methods can retrieve current user: `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`
- `SecurityContextHolder` works in MVC because it uses thread-local storage (blocking operations)
- **Utility package** (`org.acme.api.util`):
- `SecurityContextUtil` class with static methods:
    - `getCurrentUserPrincipal()` - Retrieves `UserPrincipal` from `SecurityContextHolder`
    - Handles type checking and casting from `Authentication.getPrincipal()` to `UserPrincipal`
    - Provides type-safe access to the principal from SecurityContext
- Service methods can use utility to get principal directly from SecurityContext
- **Similar functionality to WebFlux variant** but using traditional MVC patterns with JPA
- Spring Boot application class with `@SpringBootApplication`

#### API WebFlux ([acme-api/acme-api-webflux/](acme-api/acme-api-webflux/))

- Group ID: `org.acme.api`
- Artifact ID: `acme-api-webflux`
- **Depends on:**
- `acme-security-webflux` (for WebFlux security configuration)
- **`acme-persistence-r2dbc`** (for R2DBC/reactive database access)
- Uses Lombok annotations (annotation processing configured via parent)
- Contains REST controllers with **idiomatic WebFlux/reactive patterns**:
- Returns `Mono<T>` for single values
- Returns `Flux<T>` for collections/streams
- Uses reactive, non-blocking service calls
- Uses `@RestController`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, etc. (same annotations, reactive return types)
- **Example endpoint**: `/api/books` with CRUD operations:
- `POST /api/books` - Create a new book (returns `Mono<Book>`)
- `GET /api/books` - Retrieve all books (returns `Flux<Book>`)
- `GET /api/books/{id}` - Read/retrieve a single book by ID (returns `Mono<Book>`)
- `PUT /api/books/{id}` - Update an existing book (returns `Mono<Book>`)
- `DELETE /api/books/{id}` - Delete a book (returns `Mono<Void>`)
- Contains reactive service classes that use **R2DBC repositories** (reactive):
- `BookService` with methods: `create()`, `findAll()`, `findById()`, `update()`, `delete()` (all returning Mono/Flux)
- All methods are reactive and return `Mono<T>` or `Flux<T>`
- Methods protected by role-based access control (e.g., `@PreAuthorize("hasRole('ADMIN')")`)
- **Accessing current user in WebFlux** (SecurityContextHolder does NOT work in WebFlux):
- **Option 1 (Recommended)**: Controllers inject principal via `@AuthenticationPrincipal UserPrincipal principal` parameter
    - Example: `@GetMapping("/books") public Mono<Book> getBooks(@AuthenticationPrincipal UserPrincipal principal)`
    - Pass principal to service methods as parameter
- **Option 2**: Use `ReactiveSecurityContextHolder.getContext()` in reactive chains
    - Example: `ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication).map(Authentication::getPrincipal).cast(UserPrincipal.class)`
    - Must be used within reactive chain (returns `Mono<UserPrincipal>`)
- **Note**: `SecurityContextHolder` does NOT work in WebFlux because it relies on thread-local storage, which doesn't work with non-blocking reactive operations
- **Utility package** (`org.acme.api.util`):
- `ReactiveSecurityContextUtil` class with static methods:
    - `getCurrentUserPrincipal()` - Returns `Mono<UserPrincipal>` from `ReactiveSecurityContextHolder`
    - Handles type checking and casting from `Authentication.getPrincipal()` to `UserPrincipal`
    - Provides type-safe reactive access to the principal from SecurityContext
    - All methods return reactive types (`Mono<T>`) for use in reactive chains
- Service methods can use utility to get principal directly from SecurityContext (reactive)
- **Similar functionality to MVC variant** but using reactive patterns (Mono/Flux) with R2DBC
- Spring Boot application class with `@SpringBootApplication`

## Dependency Hierarchy

```javascript
root (org.acme:acme - aggregator)
├── acme-pom (org.acme:acme-pom - parent aggregator)
│   ├── acme-dependencies (org.acme:acme-dependencies - standalone, no parent, imports Spring Boot BOM)
│   └── acme-starter-parent (org.acme:acme-starter-parent - parent: acme-dependencies)
├── acme-security (org.acme.security:acme-security - parent: acme-starter-parent)
│   ├── acme-security-core (org.acme.security:acme-security-core)
│   ├── acme-security-webmvc (org.acme.security:acme-security-webmvc - depends on acme-security-core)
│   └── acme-security-webflux (org.acme.security:acme-security-webflux - depends on acme-security-core)
├── acme-persistence (org.acme.persistence:acme-persistence - parent: acme-starter-parent)
│   ├── acme-persistence-jpa (org.acme.persistence:acme-persistence-jpa - JPA repositories, entities)
│   └── acme-persistence-r2dbc (org.acme.persistence:acme-persistence-r2dbc - R2DBC reactive repositories, entities)
└── acme-api (org.acme.api:acme-api - parent: acme-starter-parent)
    ├── acme-api-mvc (org.acme.api:acme-api-mvc)
    │   └── DEPENDS ON: acme-security-webmvc + acme-persistence-jpa
    └── acme-api-webflux (org.acme.api:acme-api-webflux)
        └── DEPENDS ON: acme-security-webflux + acme-persistence-r2dbc
```



## Group ID Summary

- Root aggregator: `org.acme` (artifact: `acme`)
- Acme POM: `org.acme` (artifact: `acme-pom`)
- Dependencies BOM: `org.acme` (artifact: `acme-dependencies`)
- Starter Parent: `org.acme` (artifact: `acme-starter-parent`)
- Security modules: `org.acme.security` (artifacts: `acme-security`, `acme-security-core`, `acme-security-webmvc`, `acme-security-webflux`)
- Persistence modules: `org.acme.persistence` (artifacts: `acme-persistence`, `acme-persistence-jpa`, `acme-persistence-r2dbc`)
- API modules: `org.acme.api` (artifacts: `acme-api`, `acme-api-mvc`, `acme-api-webflux`)

## Persistence Layer Usage Rules

### ✅ Correct Usage

- **acme-api-mvc** → uses **acme-persistence-jpa** (blocking JPA)
- **acme-api-webflux** → uses **acme-persistence-r2dbc** (reactive R2DBC)

### ❌ Incorrect Usage (DO NOT DO THIS)

- **acme-api-mvc** → should NOT use **acme-persistence-r2dbc**
- **acme-api-webflux** → should NOT use **acme-persistence-jpa**

## Security Context Access Patterns

### ✅ MVC - Accessing Current User

**In Controllers:**

```java
@GetMapping("/books")
public ResponseEntity<List<Book>> getBooks(@AuthenticationPrincipal UserPrincipal principal) {
    // principal is automatically injected by Spring Security
    // Use principal directly
}
```

**In Service Methods (Direct Access):**

```java
public List<Book> findAll() {
    UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
        .getAuthentication()
        .getPrincipal();
    // Use principal
}
```

**Using Utility Class (Recommended):**

```java
import org.acme.api.util.SecurityContextUtil;

public List<Book> findAll() {
    // Get principal from SecurityContext (type-safe)
    UserPrincipal principal = SecurityContextUtil.getCurrentUserPrincipal();
    
    // Use principal directly
}
```



- `SecurityContextHolder` works in MVC because it uses thread-local storage
- Thread-local is safe in blocking/synchronous operations
- Can be accessed from anywhere in the request thread
- Utility class provides type-safe access to the principal

### ✅ WebFlux - Accessing Current User

**In Controllers (Recommended):**

```java
@GetMapping("/books")
public Mono<List<Book>> getBooks(@AuthenticationPrincipal UserPrincipal principal) {
    // principal is automatically injected by Spring Security
    // Pass to service methods as parameter
    return bookService.findAll(principal);
}
```

**In Reactive Chains (Alternative):**

```java
public Mono<List<Book>> findAll() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getPrincipal)
        .cast(UserPrincipal.class)
        .flatMap(principal -> {
            // Use principal in reactive chain
            return bookRepository.findAll();
        });
}
```

**Using Utility Class (Recommended):**

```java
import org.acme.api.util.ReactiveSecurityContextUtil;

public Mono<List<Book>> findAll() {
    return ReactiveSecurityContextUtil.getCurrentUserPrincipal()
        .flatMap(principal -> {
            // Use principal directly (no serialization needed)
            return bookRepository.findAll();
        });
}
```



- `SecurityContextHolder` does NOT work in WebFlux (thread-local doesn't work with non-blocking)
- Use `@AuthenticationPrincipal` in controller parameters (simplest approach)
- Or use `ReactiveSecurityContextHolder` within reactive chains (returns `Mono<UserPrincipal>`)
- Must stay within reactive context (Mono/Flux chains)
- Utility class provides reactive methods that return `Mono<T>` for use in reactive chains
- Principal is passed directly through authentication context (no serialization)

### ❌ Common Mistakes

- **DO NOT** use `SecurityContextHolder` in WebFlux - it will not work correctly
- **DO NOT** try to access SecurityContext outside reactive chains in WebFlux
- **DO** pass principal as method parameter in WebFlux service methods (from controller)

## Lombok Configuration

- **Lombok version management**: Defined in `acme-dependencies` BOM
- **Annotation processing**: Configured in `acme-starter-parent` Maven compiler plugin
- **Dependency scope**: Lombok added as `provided` dependency in `security`, `persistence`, and `api` parent modules
- **Modules using Lombok**: All security submodules (core, webmvc, webflux), all persistence submodules (jpa, r2dbc), and all API submodules (mvc, webflux)

## Infrastructure & Deployment

### Makefile ([Makefile](Makefile))

- Located at project root
- Commands to manage databases and applications:
- `make databases-up` - Start both PostgreSQL databases (JPA and R2DBC) using Docker Compose
- `make databases-down` - Stop both PostgreSQL databases
- `make databases-logs` - View database logs
- `make build` - Build all Maven modules
- `make clean` - Clean all Maven modules
- `make test` - Run all tests
- `make run-mvc` - Build and run MVC API application
- `make run-webflux` - Build and run WebFlux API application
- `make docker-build-mvc` - Build Docker image for MVC API
- `make docker-build-webflux` - Build Docker image for WebFlux API
- `make docker-run-mvc` - Run MVC API in Docker container
- `make docker-run-webflux` - Run WebFlux API in Docker container

### Docker Compose ([docker-compose.yml](docker-compose.yml))

- Located at project root
- Defines two PostgreSQL database services:
- **postgres-jpa**: Database for JPA/MVC API
    - Port: `5432` (default PostgreSQL port)
    - Database name: `acme_jpa`
    - Username: `acme_user`
    - Password: `acme_password`
    - Volume: Persistent data storage
- **postgres-r2dbc**: Database for R2DBC/WebFlux API
    - Port: `5433` (different port to avoid conflicts)
    - Database name: `acme_r2dbc`
    - Username: `acme_user`
    - Password: `acme_password`
    - Volume: Persistent data storage
- Both databases use PostgreSQL 15+ image
- Health checks configured for both services
- Networks configured for container communication

### Flyway Migrations

#### Persistence JPA Migrations ([acme-persistence/acme-persistence-jpa/src/main/resources/db/migration/](acme-persistence/acme-persistence-jpa/src/main/resources/db/migration/))

- **V1__Initial_schema.sql**: Creates initial database schema
- Creates `books` table with columns: `id` (BIGSERIAL PRIMARY KEY), `title` (VARCHAR), `author` (VARCHAR), `isbn` (VARCHAR), `publication_year` (INTEGER), `created_at` (TIMESTAMP), `updated_at` (TIMESTAMP)
- Adds indexes for common queries
- Sets up constraints and foreign keys if needed
- **V2__Seed_data.sql**: Inserts seed data
- Inserts sample book records for testing
- Example books with various authors, titles, ISBNs
- Provides initial data for development and testing
- Flyway dependency included in `acme-persistence-jpa` POM
- Flyway auto-configuration enabled in Spring Boot application
- Migrations run automatically on application startup

#### Persistence R2DBC Migrations ([acme-persistence/acme-persistence-r2dbc/src/main/resources/db/migration/](acme-persistence/acme-persistence-r2dbc/src/main/resources/db/migration/))

- **V1__Initial_schema.sql**: Creates initial database schema
- Creates `books` table with same structure as JPA version
- Columns: `id` (BIGSERIAL PRIMARY KEY), `title` (VARCHAR), `author` (VARCHAR), `isbn` (VARCHAR), `publication_year` (INTEGER), `created_at` (TIMESTAMP), `updated_at` (TIMESTAMP)
- Adds indexes for common queries
- Sets up constraints
- **V2__Seed_data.sql**: Inserts seed data
- Inserts sample book records (same as JPA version for consistency)
- Provides initial data for development and testing
- Flyway dependency included in `acme-persistence-r2dbc` POM
- Flyway auto-configuration enabled in Spring Boot application
- Migrations run automatically on application startup

### Dockerfiles

#### MVC API Dockerfile ([acme-api/acme-api-mvc/Dockerfile](acme-api/acme-api-mvc/Dockerfile))

- Multi-stage build:
- **Stage 1 (Build)**: Uses Maven image to build the application
    - Copies POM files and source code
    - Runs `mvn clean package -DskipTests` (or includes tests)
    - Creates JAR file
- **Stage 2 (Runtime)**: Uses OpenJDK 17+ or Eclipse Temurin image
    - Copies JAR from build stage
    - Exposes port 8080 (or configured port)
    - Sets entrypoint to run Spring Boot application
    - Configures JVM options for containerized environment
- Environment variables for database connection:
- `SPRING_DATASOURCE_URL`: jdbc:postgresql://postgres-jpa:5432/acme_jpa
- `SPRING_DATASOURCE_USERNAME`: acme_user
- `SPRING_DATASOURCE_PASSWORD`: acme_password
- Health check endpoint configured
- Non-root user for security

#### WebFlux API Dockerfile ([acme-api/acme-api-webflux/Dockerfile](acme-api/acme-api-webflux/Dockerfile))

- Multi-stage build:
- **Stage 1 (Build)**: Uses Maven image to build the application
    - Copies POM files and source code
    - Runs `mvn clean package -DskipTests` (or includes tests)
    - Creates JAR file
- **Stage 2 (Runtime)**: Uses OpenJDK 17+ or Eclipse Temurin image
    - Copies JAR from build stage
    - Exposes port 8081 (different port from MVC to avoid conflicts)
    - Sets entrypoint to run Spring Boot application
    - Configures JVM options for containerized environment
- Environment variables for database connection:
- `SPRING_R2DBC_URL`: r2dbc:postgresql://postgres-r2dbc:5432/acme_r2dbc
- `SPRING_R2DBC_USERNAME`: acme_user
- `SPRING_R2DBC_PASSWORD`: acme_password
- Health check endpoint configured
- Non-root user for security

### Application Configuration

#### MVC API Application Properties ([acme-api/acme-api-mvc/src/main/resources/application.yml](acme-api/acme-api-mvc/src/main/resources/application.yml))

- Server port: `8080`
- Database connection (JPA):
- URL: `jdbc:postgresql://localhost:5432/acme_jpa`
- Username: `acme_user`
- Password: `acme_password`
- HikariCP connection pool configuration
- Flyway configuration:
- Enabled: `true`
- Locations: `classpath:db/migration`
- Baseline on migrate: `true`
- JPA/Hibernate configuration:
- DDL auto: `validate` (Flyway handles schema)
- Show SQL: `false` (or `true` for development)

#### WebFlux API Application Properties ([acme-api/acme-api-webflux/src/main/resources/application.yml](acme-api/acme-api-webflux/src/main/resources/application.yml))

- Server port: `8081` (different from MVC)
- Database connection (R2DBC):
- URL: `r2dbc:postgresql://localhost:5433/acme_r2dbc`
- Username: `acme_user`
- Password: `acme_password`
- Connection pool configuration
- Flyway configuration:
- Enabled: `true`
- Locations: `classpath:db/migration`
- Baseline on migrate: `true`
- R2DBC configuration:
- Initialization mode: `always` (or `never` if Flyway handles it)

## Infrastructure Files Summary

1. **Makefile**: [Makefile](Makefile) - Root-level Makefile for managing databases and applications
2. **Docker Compose**: [docker-compose.yml](docker-compose.yml) - PostgreSQL databases for JPA and R2DBC
3. **JPA Flyway Migrations**: 

- [acme-persistence/acme-persistence-jpa/src/main/resources/db/migration/V1__Initial_schema.sql](acme-persistence/acme-persistence-jpa/src/main/resources/db/migration/V1__Initial_schema.sql)
- [acme-persistence/acme-persistence-jpa/src/main/resources/db/migration/V2__Seed_data.sql](acme-persistence/acme-persistence-jpa/src/main/resources/db/migration/V2__Seed_data.sql)

4. **R2DBC Flyway Migrations**: 

- [acme-persistence/acme-persistence-r2dbc/src/main/resources/db/migration/V1__Initial_schema.sql](acme-persistence/acme-persistence-r2dbc/src/main/resources/db/migration/V1__Initial_schema.sql)
- [acme-persistence/acme-persistence-r2dbc/src/main/resources/db/migration/V2__Seed_data.sql](acme-persistence/acme-persistence-r2dbc/src/main/resources/db/migration/V2__Seed_data.sql)

5. **MVC Dockerfile**: [acme-api/acme-api-mvc/Dockerfile](acme-api/acme-api-mvc/Dockerfile)
6. **WebFlux Dockerfile**: [acme-api/acme-api-webflux/Dockerfile](acme-api/acme-api-webflux/Dockerfile)
7. **MVC Application Config**: [acme-api/acme-api-mvc/src/main/resources/application.yml](acme-api/acme-api-mvc/src/main/resources/application.yml)
8. **WebFlux Application Config**: [acme-api/acme-api-webflux/src/main/resources/application.yml](acme-api/acme-api-webflux/src/main/resources/application.yml)
9. **MVC Security Utility**: [acme-api/acme-api-mvc/src/main/java/org/acme/api/util/SecurityContextUtil.java](acme-api/acme-api-mvc/src/main/java/org/acme/api/util/SecurityContextUtil.java) - Utility for type-safe access to principal from SecurityContext
10. **WebFlux Security Utility**: [acme-api/acme-api-webflux/src/main/java/org/acme/api/util/ReactiveSecurityContextUtil.java](acme-api/acme-api-webflux/src/main/java/org/acme/api/util/ReactiveSecurityContextUtil.java) - Reactive utility for type-safe access to principal from SecurityContext
11. **README**: [README.md](README.md) - Project documentation explaining MVC and WebFlux routes, similarities, and differences

## README Documentation

### README.md ([README.md](README.md))

Located at project root, provides comprehensive documentation:

#### Overview

- Project structure and architecture
- Purpose: Compare MVC vs WebFlux implementations
- Multi-module Maven project organization
- **Deployment Context**: Applications run behind an ingress layer that handles SSL/TLS termination and forwards headers

#### Getting Started

- Prerequisites (Java, Maven, Docker)
- Building the project: `make build`
- Starting databases: `make databases-up`
- Running applications: `make run-mvc` or `make run-webflux`

#### Architecture Overview

- Module structure explanation
- Dependency relationships
- Security layer architecture
- Persistence layer architecture

#### MVC Route (Traditional/Blocking)

- **Framework**: Spring MVC (Servlet-based)
- **Security**: `RequestHeaderAuthenticationFilter` extracts `x-username` header
- **Persistence**: JPA/Spring Data JPA (blocking database operations)
- **Return Types**: `ResponseEntity<T>`, standard Java objects
- **Security Context**: `SecurityContextHolder` (thread-local storage)
- **Execution Model**: Blocking, synchronous operations
- **Port**: 8080
- **Database**: PostgreSQL on port 5432 (`acme_jpa` database)

#### WebFlux Route (Reactive/Non-blocking)

- **Framework**: Spring WebFlux (Reactive)
- **Security**: Custom `ServerHttpAuthenticationConverter` extracts `x-username` header
- **Persistence**: R2DBC (reactive, non-blocking database operations)
- **Return Types**: `Mono<T>`, `Flux<T>` (reactive types)
- **Security Context**: `ReactiveSecurityContextHolder` (reactive context)
- **Execution Model**: Non-blocking, reactive operations
- **Port**: 8081
- **Database**: PostgreSQL on port 5433 (`acme_r2dbc` database)

#### How They Are The Same

- **Same Security Mechanism**: Both use `x-username` header for authentication
- **Same User Principal**: Both create `UserPrincipal` with same structure and roles
- **Same Role-Based Access Control**: Both use `@PreAuthorize` annotations
- **Same API Endpoints**: Both expose `/api/books` with same CRUD operations
- **Same Business Logic**: Same service layer functionality
- **Same Database Schema**: Both use identical `books` table structure
- **Same Error Handling**: Missing header returns `401 Unauthorized` in both

#### How They Are Different

- **Execution Model**:
- MVC: Blocking, thread-per-request model
- WebFlux: Non-blocking, event-loop model
- **Return Types**:
- MVC: `ResponseEntity<Book>`, `List<Book>`, standard Java types
- WebFlux: `Mono<Book>`, `Flux<Book>`, reactive types
- **Database Access**:
- MVC: JPA with blocking JDBC connections
- WebFlux: R2DBC with non-blocking reactive connections
- **Security Context Access**:
- MVC: `SecurityContextHolder.getContext()` (thread-local)
- WebFlux: `ReactiveSecurityContextHolder.getContext()` (reactive) or `@AuthenticationPrincipal`
- **Performance Characteristics**:
- MVC: Better for CPU-intensive, blocking I/O operations
- WebFlux: Better for high concurrency, I/O-bound operations
- **Ports**:
- MVC: 8080
- WebFlux: 8081

#### Security Implementation

- **Header-Based Authentication**: Both extract `x-username` header
- **Missing Header**: Returns `401 Unauthorized` (no authentication)
- **User Creation**: `UserLookupService` creates `UserPrincipal` with dummy roles
- **Role-Based Access**: Service methods protected with `@PreAuthorize("hasRole('ADMIN')")`

#### Testing the APIs

- **Example Request**:
  ```bash
    curl -H "x-username: Bob" http://localhost:8080/api/books  # MVC
    curl -H "x-username: Bob" http://localhost:8081/api/books  # WebFlux
  ```

- **Missing Header**:
  ```bash
    curl http://localhost:8080/api/books  # Returns 401 Unauthorized
  ```




#### Development Workflow

- Start databases: `make databases-up`
- Run MVC: `make run-mvc`
- Run WebFlux: `make run-webflux`
- Build Docker images: `make docker-build-mvc`, `make docker-build-webflux`
- Run tests: `make test`

#### Project Structure

- Module organization
- Dependency relationships