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

Both scripts use the `x-username` header for authentication. The username is looked up in the database, and the user's roles determine what operations they can perform.

### Available Users

The database is seeded with three users for testing:

- **`noaccess`** - No roles
  - All operations return `403 Forbidden`
  
- **`readonly`** - Has `ROLE_READ_ONLY`
  - ✅ Can perform: `get-all`, `get <id>`
  - ❌ Cannot perform: `create`, `update`, `delete` (returns `403 Forbidden`)
  
- **`readwrite`** - Has `ROLE_READ_ONLY` + `ROLE_READ_WRITE`
  - ✅ Can perform: All operations (full access)

### Customizing Username

You can customize the username by setting the `X_USERNAME` environment variable:

```bash
# Test with read-only user
X_USERNAME=readonly ./scripts/test-mvc.sh get-all

# Test with no-access user (will fail)
X_USERNAME=noaccess ./scripts/test-mvc.sh get-all

# Test with read-write user (full access)
X_USERNAME=readwrite ./scripts/test-mvc.sh create "New Book" "Author" "ISBN-123"
```

By default, the scripts use `readwrite` as the username.

## Requirements

- `curl` - for making HTTP requests
- `jq` - for pretty-printing JSON (optional, script will work without it)

## Examples

### Basic Operations (using default `readwrite` user)

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

### Testing Role-Based Access Control

```bash
# Test read-only access (should succeed)
X_USERNAME=readonly ./scripts/test-mvc.sh get-all

# Test read-only access (should fail with 403)
X_USERNAME=readonly ./scripts/test-mvc.sh create "New Book" "Author" "ISBN-123"

# Test no-access user (should fail with 403)
X_USERNAME=noaccess ./scripts/test-mvc.sh get-all

# Test full access (should succeed)
X_USERNAME=readwrite ./scripts/test-mvc.sh create "New Book" "Author" "ISBN-123"
```
