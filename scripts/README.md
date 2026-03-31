# Scripts

Supporting shell scripts for certificates, API testing, and traffic simulation. Run them from the **repository root** (paths below assume that).

## Directories

| Directory | README | Purpose |
| --------- | ------ | ------- |
| **`certs/`** | — | Generate and copy X.509 / mTLS material (CA, users, service keystores, Prometheus, etc.). |
| **`test/`** | [test/README.md](test/README.md) | **`test-mvc.sh`**, **`test-webflux.sh`**, **`test-all.sh`** — authenticated CRUD calls against the book APIs. |
| **`simulator/`** | [simulator/README.md](simulator/README.md) | **`simulate-request.sh`**, **`simulate-traffic.sh`** — manual requests and probe-style traffic (pairs with `acme.security.header-filter`). |

## Certificate scripts (`certs/`)

- `generate-ca.sh` — Root and Intermediate CA for X.509 client auth
- `generate-user-cert.sh` — User PKCS#12 bundles
- `generate-mtls-certs.sh` — Service mTLS keystores/truststores
- `generate-prometheus-certs.sh` — Prometheus scrape client certs
- `copy-mtls-certs.sh` — Copy generated material into module resources

Details live alongside those scripts; certificate usage is also documented in **`docs/X509.md`**, **`docs/SSL.md`**, and the root **`README.md`**.

## Quick links

- **API tests (curl CRUD):** [test/README.md](test/README.md)
- **Smoke + continuous traffic + Make targets (`make sim-request-mvc`, `make sim-request-webflux`, `make sim-traffic-start`):** [simulator/README.md](simulator/README.md)

## Shared conventions

- **DN headers:** Default names `x-amzn-mtls-clientcert-subject` / `x-amzn-mtls-clientcert-issuer`; override with `ACME_HEADER_SUBJECT_DN` / `ACME_HEADER_ISSUER_DN` where supported.
- **DN values:** Export `SSL_CLIENT_SUBJECT_DN` and `SSL_CLIENT_ISSUER_DN` for anything that calls protected APIs (see root **`README.md`** and **`USERS.md`**).
