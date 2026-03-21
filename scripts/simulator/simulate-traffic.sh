#!/usr/bin/env bash
#
# Periodic probe-style traffic (health endpoints only with probe User-Agents).
# Designed to pair with acme.security.header-filter.ignore-headers for user-agent patterns
# (HealthChecker/*, ELB-HealthChecker/*, kube-probe/*) so DEBUG header logging stays quiet.
#
# Usage:
#   ./simulate-traffic.sh mvc                      # foreground, http://localhost:8080
#   ./simulate-traffic.sh webflux                  # foreground, http://localhost:8081
#   ./simulate-traffic.sh all                      # foreground, both MVC + WebFlux
#   ./simulate-traffic.sh http://127.0.0.1:9080    # foreground, custom base URL
#
# Environment:
#   INTERVAL_SECONDS   seconds between cycles (default: 5)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

INTERVAL_SECONDS="${INTERVAL_SECONDS:-5}"

readonly -a SCENARIOS=(
  "GET /actuator/health/liveness (HealthChecker/1.0)|/actuator/health/liveness|HealthChecker/1.0"
  "GET /actuator/health/readiness (HealthChecker/1.0)|/actuator/health/readiness|HealthChecker/1.0"
  "GET /actuator/health (ELB-HealthChecker/2.0)|/actuator/health|ELB-HealthChecker/2.0"
)

shutdown_gracefully() {
  printf '\n'
  log "Received SIGINT; exiting traffic simulator."
  exit 0
}

http_get_code() {
  local url="$1"
  local user_agent="$2"

  local -a curl_args=(
    -sS -o /dev/null -w '%{http_code}' -X GET "$url"
    -H "user-agent: ${user_agent}"
    -H "accept: application/json"
  )
  curl "${curl_args[@]}"
}

clear_screen() {
  printf '\033[2J\033[H'
}

print_cycle_banner() {
  clear_screen
  log "Traffic simulator — interval ${INTERVAL_SECONDS}s, base ${BASE_URL}"
  printf '\n'
}

print_result() {
  local description="$1"
  local http_code="$2"
  local label="OK"
  if [[ "$http_code" != "200" ]]; then
    label="ERROR"
  fi
  log "${description} -> HTTP ${http_code} (${label})"
}

run_scenario() {
  local description="$1"
  local path="$2"
  local user_agent="$3"
  local url="${BASE_URL}${path}"
  local code
  code="$(http_get_code "$url" "$user_agent")"
  print_result "$description" "$code"
}

run_all_scenarios() {
  local entry
  for entry in "${SCENARIOS[@]}"; do
    IFS='|' read -r description path user_agent <<<"$entry"
    run_scenario "$description" "$path" "$user_agent"
  done
}

run_all_targets() {
  local original_base="${BASE_URL:-}"

  BASE_URL="${BASE_URL_MVC:-http://localhost:8080}"
  print_cycle_banner
  log "Target: MVC (${BASE_URL})"
  run_all_scenarios
  printf '\n'

  BASE_URL="${BASE_URL_WEBFLUX:-http://localhost:8081}"
  log "Target: WebFlux (${BASE_URL})"
  run_all_scenarios
  printf '\n'

  BASE_URL="${original_base}"
}

run_loop() {
  trap shutdown_gracefully INT
  while true; do
    if [[ "${TARGET_MODE}" == "all" ]]; then
      run_all_targets
    else
      print_cycle_banner
      run_all_scenarios
      printf '\n'
    fi
    log "Sleeping ${INTERVAL_SECONDS}s before next cycle..."
    sleep "$INTERVAL_SECONDS"
  done
}

usage() {
  echo "Usage: $0 {mvc|webflux|all|<base-url>}" >&2
  echo "  mvc|webflux   use default localhost ports 8080 / 8081" >&2
  echo "  all           run both mvc + webflux each cycle" >&2
  echo "  <base-url>    e.g. http://127.0.0.1:9080 (no trailing slash)" >&2
  exit 1
}

main() {
  [[ $# -lt 1 ]] && usage

  local target="$1"

  case "$target" in
    mvc)
      TARGET_MODE="single"
      BASE_URL="${BASE_URL:-http://localhost:8080}"
      ;;
    webflux)
      TARGET_MODE="single"
      BASE_URL="${BASE_URL:-http://localhost:8081}"
      ;;
    all)
      TARGET_MODE="all"
      ;;
    http://* | https://*)
      TARGET_MODE="single"
      BASE_URL="$target"
      ;;
    *) usage ;;
  esac

  run_loop
}

main "$@"
