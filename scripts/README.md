# Scripts Directory

This directory contains organized scripts for the Acme application.

## Directory Structure

- **`certs/`** - Certificate generation and management scripts
- **`test/`** - API test scripts for MVC and WebFlux

## Certificate Scripts (`certs/`)

- `generate-ca.sh` - Generate Root CA and Intermediate CA for X509 authentication
- `generate-user-cert.sh` - Generate X509 client certificates for LDAP users
- `generate-mtls-certs.sh` - Generate mTLS certificates for service-to-service communication
- `generate-prometheus-certs.sh` - Generate Prometheus client certificates
- `copy-mtls-certs.sh` - Copy mTLS certificates to service resource directories

## Test Scripts (`test/`)

- `test-mvc.sh` - Test script for MVC API (port 8080)
- `test-webflux.sh` - Test script for WebFlux API (port 8081)
- `test-all.sh` - Run tests against both APIs

## Usage

Both scripts support the same operations:

### Get All Books

```bash
./scripts/test/test-mvc.sh get-all
./scripts/test/test-webflux.sh get-all
```

### Get One Book by ID

```bash
./scripts/test/test-mvc.sh get <id>
./scripts/test/test-webflux.sh get 1
```

### Create a Book

```bash
./scripts/test/test-mvc.sh create "Book Title" "Author Name" "ISBN-123"
./scripts/test/test-webflux.sh create "Book Title" "Author Name" "ISBN-123"
```

### Update a Book

```bash
./scripts/test/test-mvc.sh update <id> "Updated Title" "Updated Author" "ISBN-456"
./scripts/test/test-webflux.sh update 1 "Updated Title" "Updated Author" "ISBN-456"
```

### Delete a Book

```bash
./scripts/test/test-mvc.sh delete <id>
./scripts/test/test-webflux.sh delete 1
```

## Authentication

Both scripts use configurable subject and issuer DN headers for authentication (default header names: `x-amzn-mtls-clientcert-subject`, `x-amzn-mtls-clientcert-issuer`). The Subject Distinguished Name (DN) is looked up in the database, and the user's roles determine what operations they can perform.

### Available Users

The database is seeded with users from `USERS.md`. The default user is:

- **`cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org`** - Member of `ACME_READ_WRITE` group
  - ✅ Can perform: All operations (full access)

See `USERS.md` for the complete list of available users and their DNs.

### Customizing DN

You can customize the DNs by setting the `SSL_CLIENT_SUBJECT_DN` and `SSL_CLIENT_ISSUER_DN` environment variables:

```bash
# Test with a different user Subject DN
SSL_CLIENT_SUBJECT_DN="cn=asmith,ou=hr,ou=users,dc=corp,dc=acme,dc=org" \
SSL_CLIENT_ISSUER_DN="CN=Acme Intermediate CA,O=Acme Corp,C=US" \
./scripts/test/test-mvc.sh get-all

# Test with full access user (default)
./scripts/test/test-mvc.sh create "New Book" "Author" "ISBN-123"
```

By default, the scripts use:
- Subject DN: `cn=jdoe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org`
- Issuer DN: `CN=Acme Intermediate CA,O=Acme Corp,C=US`

## Requirements

- `curl` - for making HTTP requests
- `jq` - for pretty-printing JSON (optional, script will work without it)

## Examples

### Basic Operations (using default `readwrite` user)

```bash
# Get all books from MVC API
./scripts/test/test-mvc.sh get-all

# Get book with ID 1 from WebFlux API
./scripts/test/test-webflux.sh get 1

# Create a new book in MVC API
./scripts/test/test-mvc.sh create "The Great Gatsby" "F. Scott Fitzgerald" "978-0-7432-7356-5"

# Update book with ID 1 in WebFlux API
./scripts/test/test-webflux.sh update 1 "Updated Title" "Updated Author" "ISBN-123"

# Delete book with ID 1 from MVC API
./scripts/test/test-mvc.sh delete 1
```

### Testing Role-Based Access Control

```bash
# Test with different user Subject DNs (adjust roles as needed in database)
SSL_CLIENT_SUBJECT_DN="cn=asmith,ou=hr,ou=users,dc=corp,dc=acme,dc=org" \
SSL_CLIENT_ISSUER_DN="CN=Acme Intermediate CA,O=Acme Corp,C=US" \
./scripts/test/test-mvc.sh get-all

# Test full access (default user)
./scripts/test/test-mvc.sh create "New Book" "Author" "ISBN-123"
```
