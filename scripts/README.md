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

Both scripts use the `x-dn` header for authentication. The Distinguished Name (DN) is looked up in the database, and the user's roles determine what operations they can perform.

### Available Users

The database is seeded with users from `USERS.md`. The default user is:

- **`cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org`** - Member of `ACME_READ_WRITE` group
  - ✅ Can perform: All operations (full access)

See `USERS.md` for the complete list of available users and their DNs.

### Customizing DN

You can customize the DN by setting the `X_DN` environment variable:

```bash
# Test with a different user DN
X_DN="cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org" ./scripts/test-mvc.sh get-all

# Test with full access user (default)
./scripts/test-mvc.sh create "New Book" "Author" "ISBN-123"
```

By default, the scripts use `cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org` as the DN.

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
# Test with different user DNs (adjust roles as needed in database)
X_DN="cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org" ./scripts/test-mvc.sh get-all

# Test full access (default user)
./scripts/test-mvc.sh create "New Book" "Author" "ISBN-123"
```
