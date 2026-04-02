#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REVISION="${REVISION:-$("${ROOT_DIR}/scripts/build/read-revision.sh")}"
FORMATTER="${ROOT_DIR}/acme-libs/acme-pom/formatter.xml"
GOAL="${1:-check}"

if [[ "$GOAL" != "check" && "$GOAL" != "apply" ]]; then
	echo "Usage: $0 [check|apply]" >&2
	exit 1
fi

POMS=(
	"acme-api-mvc/pom.xml"
	"acme-api-webflux/pom.xml"
	"acme-libs/acme-security/acme-security-core/pom.xml"
	"acme-libs/acme-security/acme-security-webmvc/pom.xml"
	"acme-libs/acme-security/acme-security-webflux/pom.xml"
	"acme-libs/acme-persistence-jpa/pom.xml"
	"acme-libs/acme-persistence-r2dbc/pom.xml"
	"acme-libs/acme-test-integration-classic/pom.xml"
	"acme-libs/acme-test-integration-reactive/pom.xml"
	"acme-auth-utils/pom.xml"
	"acme-auth-client/pom.xml"
	"acme-auth-service-ldap/pom.xml"
	"acme-auth-service-db/pom.xml"
)

for pom in "${POMS[@]}"; do
	echo
	echo "==> spotless:${GOAL} (${pom%/pom.xml})"
	mvn -Drevision="${REVISION}" -Dacme.spotless.eclipse.formatter="${FORMATTER}" -f "${ROOT_DIR}/${pom}" "spotless:${GOAL}"
done

echo
echo "Spotless ${GOAL} completed for all Java modules."
