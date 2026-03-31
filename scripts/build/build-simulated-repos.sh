#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REVISION="${REVISION:-$("${ROOT_DIR}/scripts/build/read-revision.sh")}"
MAVEN_GOALS="${MAVEN_GOALS:-clean install}"

# shellcheck disable=SC2206
GOALS_ARRAY=(${MAVEN_GOALS})
MAVEN_ARGS=("-U" "-Drevision=${REVISION}" "${GOALS_ARRAY[@]}")

build_repo() {
  local label="$1"
  local pom_path="$2"
  echo
  echo "==> Building ${label}"
  mvn -f "${ROOT_DIR}/${pom_path}" "${MAVEN_ARGS[@]}"
}

# Install parent/BOM coordinates first so non-framework repos can resolve parents.
build_repo "acme-framework/acme-pom" "acme-framework/acme-pom/pom.xml"

# Simulated external auth repos.
build_repo "acme-auth-utils" "acme-auth-utils/pom.xml"
build_repo "acme-auth-client" "acme-auth-client/pom.xml"
build_repo "acme-auth-service-ldap" "acme-auth-service-ldap/pom.xml"
build_repo "acme-auth-service-db" "acme-auth-service-db/pom.xml"

# Simulated framework repo.
build_repo "acme-framework" "acme-framework/pom.xml"

# Simulated API repos.
build_repo "acme-api-mvc" "acme-api-mvc/pom.xml"
build_repo "acme-api-webflux" "acme-api-webflux/pom.xml"

echo
echo "All simulated repos built successfully."
