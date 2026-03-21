# API test scripts (`scripts/test/`)

Shell helpers for calling the MVC and WebFlux book APIs with subject/issuer DN headers.

## Scripts

| Script | API | Default base URL |
| ------ | --- | ---------------- |
| `test-mvc.sh` | MVC | `http://localhost:8080/api/v1/books` |
| `test-webflux.sh` | WebFlux | `http://localhost:8081/api/v1/books` |
| `test-all.sh` | Both | Runs checks against both APIs |

## Requirements

- `curl` — HTTP requests
- `jq` — optional, for prettier JSON output

## Environment

Set **`SSL_CLIENT_SUBJECT_DN`** and **`SSL_CLIENT_ISSUER_DN`** before running (same values as integration tests / curl examples in the root `README.md`).

Optional header names (if the backend uses non-default names):

- `ACME_HEADER_SUBJECT_DN` (default `x-amzn-mtls-clientcert-subject`)
- `ACME_HEADER_ISSUER_DN` (default `x-amzn-mtls-clientcert-issuer`)

The Subject DN is resolved in the auth layer; roles control which operations succeed.

### Default DNs (if you export nothing)

The test scripts **require** the two `SSL_CLIENT_*` variables. Typical values:

- Subject: `cn=jdoe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org`
- Issuer: `CN=Acme Intermediate CA,O=Acme Corp,C=US`

### Other users

Users are seeded per `USERS.md`. Example read-write user:

- `cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org` — `ACME_READ_WRITE`

## Operations

Both `test-mvc.sh` and `test-webflux.sh` share the same commands.

### Get all books

```bash
./scripts/test/test-mvc.sh get-all
./scripts/test/test-webflux.sh get-all
```

### Get one book by ID

```bash
./scripts/test/test-mvc.sh get <id>
./scripts/test/test-webflux.sh get 1
```

### Create a book

```bash
./scripts/test/test-mvc.sh create "Book Title" "Author Name" "ISBN-123"
./scripts/test/test-webflux.sh create "Book Title" "Author Name" "ISBN-123"
```

### Update a book

```bash
./scripts/test/test-mvc.sh update <id> "Updated Title" "Updated Author" "ISBN-456"
./scripts/test/test-webflux.sh update 1 "Updated Title" "Updated Author" "ISBN-456"
```

### Delete a book

```bash
./scripts/test/test-mvc.sh delete <id>
./scripts/test/test-webflux.sh delete 1
```

## Examples

### Default user (full access)

```bash
export SSL_CLIENT_SUBJECT_DN="CN=jdoe,OU=Engineering,OU=Users,DC=corp,DC=acme,DC=org"
export SSL_CLIENT_ISSUER_DN="CN=Acme Intermediate CA,O=Acme Corp,C=US"

./scripts/test/test-mvc.sh get-all
./scripts/test/test-webflux.sh get 1
./scripts/test/test-mvc.sh create "The Great Gatsby" "F. Scott Fitzgerald" "978-0-7432-7356-5"
./scripts/test/test-webflux.sh update 1 "Updated Title" "Updated Author" "ISBN-123"
./scripts/test/test-mvc.sh delete 1
```

### Another user (RBAC)

```bash
SSL_CLIENT_SUBJECT_DN="cn=asmith,ou=hr,ou=users,dc=corp,dc=acme,dc=org" \
SSL_CLIENT_ISSUER_DN="CN=Acme Intermediate CA,O=Acme Corp,C=US" \
./scripts/test/test-mvc.sh get-all
```

## See also

- [../README.md](../README.md) — overview of all script directories
- [../simulator/README.md](../simulator/README.md) — smoke and continuous traffic simulators
