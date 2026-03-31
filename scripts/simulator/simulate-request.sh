#!/usr/bin/env bash
#
# Manual GET with a normal curl user-agent — should appear in header debug logs
# (unlike automated probe traffic from simulate-traffic.sh).
#
# Usage: ./scripts/simulator/simulate-request.sh {mvc|webflux|<base-url>} [path]
#   path defaults to /api/v1/books
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

usage() {
  echo "Usage: $0 {mvc|webflux|<base-url>} [path]" >&2
  echo "  mvc|webflux   use default localhost ports 8080 / 8081" >&2
  echo "  <base-url>    e.g. http://127.0.0.1:9080 (no trailing slash)" >&2
  echo "  [path]        defaults to /api/v1/books" >&2
  exit 1
}

main() {
  [[ $# -lt 1 ]] && usage

  local target="$1"
  local endpoint="${2:-/api/v1/books}"
  local base_url

  case "$target" in
    mvc) base_url="${BASE_URL:-http://localhost:8080}" ;;
    webflux) base_url="${BASE_URL:-http://localhost:8081}" ;;
    http://* | https://*) base_url="$target" ;;
    *) usage ;;
  esac

  log "Manual request: GET ${base_url}${endpoint}"

  curl -sS -X GET "${base_url}${endpoint}" \
      "${ACME_MTLS_HEADERS[@]}" \
      -H "accept: application/json" \
      -H "user-agent: curl/8.7.1"
  printf '\n'
}

main "$@"
