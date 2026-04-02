#!/usr/bin/env bash
# Prints the canonical <revision> from acme-libs/acme-pom/pom.xml (single source of truth).
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
sed -n 's/^[[:space:]]*<revision>\([^<]*\)<\/revision>.*/\1/p' "${ROOT_DIR}/acme-libs/acme-pom/pom.xml" | head -1
