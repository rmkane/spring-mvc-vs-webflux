# Integration Test Framework

A Java framework for writing integration tests for REST APIs. Provides a fluent builder API for constructing HTTP requests with support for authentication, multipart uploads, query parameters, and path variables. Includes utilities for JSON parsing, response validation, resource loading, and response persistence.

## Installation

Add this dependency to your project's `pom.xml` with `test` scope:

```xml
<dependency>
    <groupId>org.acme.test</groupId>
    <artifactId>acme-integration-test</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

## Usage

### Basic Setup

Extend `IntegrationTestSuite` in your test classes:

```java
import org.acme.test.IntegrationTestSuite;
import org.acme.test.request.RestFetcher;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;

public class MyControllerTest extends IntegrationTestSuite {
    
    @Test
    public void testGetEndpoint() {
        ResponseEntity<String> response = restFetcher.fetch(
            get("/api/v1/users").build(),
            String.class
        );
        
        assertOk(response);
    }
}
```

### Making Requests

The framework provides convenience methods for common HTTP methods:

```java
// GET request
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/users").build(),
    String.class
);

// POST request with JSON body
ResponseEntity<String> response = restFetcher.fetch(
    post("/api/v1/users")
        .headers(getDefaultHeaders())
        .body(toJson(userObject))
        .build(),
    String.class
);

// PUT request
ResponseEntity<String> response = restFetcher.fetch(
    put("/api/v1/users/123")
        .headers(getDefaultHeaders())
        .body(toJson(updatedUser))
        .build(),
    String.class
);

// DELETE request
ResponseEntity<String> response = restFetcher.fetch(
    delete("/api/v1/users/123").build(),
    String.class
);
```

### Authentication

```java
// Bearer token authentication
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/protected")
        .bearerToken("your-token-here")
        .build(),
    String.class
);

// Basic authentication
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/protected")
        .basicAuth("username", "password")
        .build(),
    String.class
);
```

### Query Parameters and Path Variables

```java
// Query parameters
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/search")
        .queryParam("q", "test")
        .queryParam("limit", "10")
        .build(),
    String.class
);

// Path variables
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/users/{id}")
        .pathVar("id", 123)
        .build(),
    String.class
);
```

### Multipart File Uploads

```java
// Upload file from bytes
ResponseEntity<String> response = restFetcher.fetch(
    post("/api/v1/upload")
        .file("file", fileBytes, "document.pdf", MediaType.APPLICATION_PDF)
        .build(),
    String.class
);

// Upload file from File object
ResponseEntity<String> response = restFetcher.fetch(
    post("/api/v1/upload")
        .file("file", new File("path/to/file.pdf"))
        .build(),
    String.class
);

// Multipart with multiple parts
ResponseEntity<String> response = restFetcher.fetch(
    post("/api/v1/upload")
        .part("description", "File description")
        .file("file", fileBytes, "document.pdf")
        .build(),
    String.class
);
```

### Response Validation

```java
// Assert status code
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/users/123").build(),
    String.class
);
assertOk(response);  // Asserts 200 OK
assertStatus(response, HttpStatus.NOT_FOUND);  // Asserts specific status

// Assert JSON path value
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/users/123").build(),
    String.class
);
assertJsonPath(response, "user.name", "John Doe");
assertJsonPath(response, "user.address.city", "New York");
```

### JSON Utilities

```java
// Parse JSON response to Map
Map<String, Object> json = parseJsonResponse(response);
String userName = (String) json.get("name");

// Convert object to JSON string
String jsonBody = toJson(userObject);
```

### Resource Loading

```java
// Load resource as bytes
byte[] fileContent = loadResource("test-data/sample.pdf");

// Load resource as string
String jsonContent = loadResourceAsString("test-data/sample.json");
```

### Response Persistence

```java
// Write response to file (useful for debugging)
ResponseEntity<String> response = restFetcher.fetch(
    get("/api/v1/users").build(),
    String.class
);
writeJsonResponse(response.getBody(), "users-response.json");
writeResponse(response.getBody(), "raw-response.txt");
```

### Custom Base URL and Port

Override the base URL methods in your test class:

```java
public class MyControllerTest extends IntegrationTestSuite {
    
    @Override
    protected String getBaseUrl() {
        return "https://api.example.com";
    }
    
    @Override
    protected int getPort() {
        return 8443;
    }
    
    @Override
    protected String getProtocol() {
        return "https";
    }
}
```

Or configure via system properties or environment variables:

```bash
# System property
-Dtest.server.port=8080

# Environment variable
export TEST_SERVER_PORT=8080
```

## Features

- Fluent builder API for constructing HTTP requests
- Support for all HTTP methods (GET, POST, PUT, DELETE, etc.)
- Authentication helpers (Bearer token, Basic auth)
- Multipart file upload support
- Query parameters and path variables
- JSON parsing and validation utilities
- Response persistence for debugging
- Resource loading utilities
- Configurable base URL and port

## Requirements

- Java 17+
- Spring Boot 3.5.6+
- JUnit 5
