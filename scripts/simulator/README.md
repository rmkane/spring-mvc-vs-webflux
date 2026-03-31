# Simulators (`scripts/simulator/`)

Tools for quick smoke checks and **continuous, probe-style traffic** against running APIs. Continuous traffic uses **Kubernetes / ELB–style `User-Agent` values** so they line up with `acme.security.header-filter.ignore-headers` in the API `application.yml` (DEBUG request/response header logging is skipped when a pattern matches).

## Layout

| Path | Role |
| ---- | ---- |
| `lib/common.sh` | Shared defaults (header names, jdoe DNs if `SSL_CLIENT_*` unset) — sourced by other scripts |
| `run/` | Legacy PID/log files from older daemon mode |

## One-shot smoke

**`simulate-request.sh`** — manual single `GET` with normal `curl` user-agent
(intended to show up in header DEBUG logs). Preferred entrypoint is via `make`.

- Usage: `./scripts/simulator/simulate-request.sh {mvc|webflux|<base-url>} [path]`
- Defaults: target `mvc`/`webflux` maps to `http://localhost:8080`/`8081`; path defaults to `/api/v1/books`
- DN/header defaults come from `lib/common.sh` (`SSL_CLIENT_*` and `ACME_HEADER_*` supported)

```bash
export SSL_CLIENT_SUBJECT_DN="cn=jdoe,..."
export SSL_CLIENT_ISSUER_DN="CN=Acme Intermediate CA,..."
make sim-request-mvc
make sim-request-webflux
```

```bash
# Optional direct script usage
./scripts/simulator/simulate-request.sh mvc /api/v1/books
./scripts/simulator/simulate-request.sh webflux /api/v1/books
```

## Continuous traffic (`simulate-traffic.sh`)

Each cycle performs:

1. `GET /actuator/health/liveness` — `User-Agent: HealthChecker/1.0`
2. `GET /actuator/health/readiness` — `User-Agent: HealthChecker/1.0`
3. `GET /actuator/health` — `User-Agent: ELB-HealthChecker/2.0`

### Modes

- **Foreground single target:** `./scripts/simulator/simulate-traffic.sh mvc` or `webflux`, or pass a full base URL as the first argument.
- **Foreground both targets:** `./scripts/simulator/simulate-traffic.sh all` (MVC then WebFlux each cycle).
- **Interval:** `INTERVAL_SECONDS` (default `5`).
- **Display:** each cycle refreshes the terminal and prints per-endpoint status.

```bash
./scripts/simulator/simulate-traffic.sh mvc
./scripts/simulator/simulate-traffic.sh webflux
./scripts/simulator/simulate-traffic.sh all
```

### Make (repo root)

```bash
make sim-request-mvc
make sim-request-webflux
make sim-traffic-start   # both APIs
make sim-traffic-mvc
make sim-traffic-webflux
```

## Prerequisites

- **`management.endpoint.health.probes.enabled: true`** in each API `application.yml` so `/actuator/health/liveness` and `/actuator/health/readiness` exist.
- **`acme.security.header-filter.ignore-headers`** configured with matching `user-agent` patterns (see committed `application.yml` files).

## See also

- [../README.md](../README.md) — overview of all script directories
- [../test/README.md](../test/README.md) — CRUD test scripts
