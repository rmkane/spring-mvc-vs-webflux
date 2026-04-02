# Acme libs

This directory is the **shared libraries** tree: a single Maven multi-module project that publishes the platform **BOM**, **parent POM**, and reusable libraries (security, persistence, integration test support). Treat it like its own repository that you could clone and build with `mvn install` from here.

## What it is for

- **One parent chain** (`acme-pom` → `acme-dependencies` → `acme-starter-parent`) carries dependency management, plugin defaults, and conventions. Any change to that parent applies everywhere modules inherit from it—**security**, **persistence**, **test-integration** modules, and the `acme-libs` aggregator itself—without duplicating versions in each leaf `pom.xml`.
- **`acme-dependencies`** (the BOM) centralizes versions for third-party stacks (Spring Boot, Jackson overrides, MapStruct, Spotless, etc.) and for **first-party** `org.acme.*` libraries so their versions stay aligned with the platform line.
- **Consumers** (applications in other repos, or the simulated `acme-api-*` / `acme-auth-*` trees in this workspace) should depend on the BOM/parent and **bump only the parent version** when they adopt a new platform release. They do not need to chase individual library versions if those are managed in the BOM.

## How to bump the platform version

1. Edit **one** property in **`acme-pom/pom.xml`**:

   ```xml
   <properties>
     <revision>YOUR_NEW_VERSION</revision>
   </properties>
   ```

2. Install or deploy the `acme-pom` reactor so refreshed artifacts (and **flattened** POMs) land in your local repo or Nexus/Artifactory:

   ```bash
   mvn -f acme-pom/pom.xml clean install
   ```

   Or build the whole `acme-libs` tree from this directory:

   ```bash
   mvn clean install
   ```

   Use `-Drevision=YOUR_NEW_VERSION` if you need to override the POM default for a single build (CI releases, local experiments).

3. **Consumers** must declare a parent (or import the BOM) whose **version matches** the revision you published. In this workspace, simulated external apps use **`${revision}`** in `<parent><version>` (same as `acme-libs`); keep **`<revision>`** in `acme-pom/pom.xml` and consumer builds passing **`-Drevision`** in sync when you move them to the new line.

The parent and BOM versions use **`${revision}`** internally; **flatten-maven-plugin** on the `acme-pom` modules resolves that to a concrete version in the POMs installed to `~/.m2` (or your remote repository), so downstream builds do not see unresolved placeholders.

## Workspace integration

In the full git workspace, `scripts/build/read-revision.sh` reads `<revision>` from `acme-pom/pom.xml` so `make build` and Spotless pass a consistent `-Drevision` into every `mvn -f` step. You normally still edit only **`acme-pom/pom.xml`** to change the canonical line.

`scripts/quality/spotless-java.sh` also passes `-Dacme.spotless.eclipse.formatter=…` pointing at `acme-pom/formatter.xml`, so Spotless works for every `mvn -f` module even when the parent is resolved from `~/.m2`.

### If `make format` / Spotless breaks after parent changes

Snapshot **`acme-starter-parent`** in your local repository can lag behind the copy under `acme-pom/acme-starter-parent/` on disk. Reinstall the BOM/parent reactor so `~/.m2` matches the workspace:

```bash
mvn -f acme-pom/pom.xml install -DskipTests
```

Or run `make build` once. After that, `make format` should succeed again.

## Layout (high level)

| Path | Role |
| ---- | ---- |
| `acme-pom/` | Aggregates `acme-dependencies` (BOM) and `acme-starter-parent` (parent with plugins, Java level, Spotless, etc.). |
| `acme-security/` | Security core + WebMVC / WebFlux adapters. |
| `acme-persistence-jpa` / `acme-persistence-r2dbc` | Shared persistence modules. |
| `acme-test-integration-classic` / `acme-test-integration-reactive` | Shared integration test utilities for servlet vs reactive stacks. |

All of these inherit from **`acme-starter-parent`** (directly or via the `acme-libs` aggregator), so platform-wide parent and BOM updates stay in sync with the modules that matter most for application behavior and tests.
