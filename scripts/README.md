# API Test Scripts

This directory contains test scripts for the MVC and WebFlux APIs.

## Scripts

- `test-mvc.sh` - Test script for MVC API (port 8080)
- `test-webflux.sh` - Test script for WebFlux API (port 8081)

## Usage

Both scripts support the same operations:

### Get All Books

```bash
./scripts/test-mvc.sh get-all
./scripts/test-webflux.sh get-all
```

### Get One Book by ID

```bash
./scripts/test-mvc.sh get <id>
./scripts/test-webflux.sh get 1
```

### Create a Book

```bash
./scripts/test-mvc.sh create "Book Title" "Author Name" "ISBN-123"
./scripts/test-webflux.sh create "Book Title" "Author Name" "ISBN-123"
```

### Update a Book

```bash
./scripts/test-mvc.sh update <id> "Updated Title" "Updated Author" "ISBN-456"
./scripts/test-webflux.sh update 1 "Updated Title" "Updated Author" "ISBN-456"
```

### Delete a Book

```bash
./scripts/test-mvc.sh delete <id>
./scripts/test-webflux.sh delete 1
```

## Authentication

Both scripts use the `x-username` header for authentication. You can customize the username by setting the `X_USERNAME` environment variable:

```bash
X_USERNAME=Alice ./scripts/test-mvc.sh get-all
```

By default, the scripts use `Bob` as the username.

## Requirements

- `curl` - for making HTTP requests
- `jq` - for pretty-printing JSON (optional, script will work without it)

## Examples

```bash
# Get all books from MVC API
./scripts/test-mvc.sh get-all

# Get book with ID 1 from WebFlux API
./scripts/test-webflux.sh get 1

# Create a new book in MVC API
./scripts/test-mvc.sh create "The Great Gatsby" "F. Scott Fitzgerald" "978-0-7432-7356-5"

# Update book with ID 1 in WebFlux API
./scripts/test-webflux.sh update 1 "Updated Title" "Updated Author" "ISBN-123"

# Delete book with ID 1 from MVC API
./scripts/test-mvc.sh delete 1
```
