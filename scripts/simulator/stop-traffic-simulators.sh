#!/usr/bin/env bash
# Stop background traffic simulators started with simulate-traffic.sh ... --daemon

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="${SCRIPT_DIR}/run"

if [[ ! -d "$RUN_DIR" ]]; then
  echo "No run directory; nothing to stop."
  exit 0
fi

stopped=0
for pidfile in "$RUN_DIR"/*-traffic.pid; do
  [[ -e "$pidfile" ]] || continue
  pid="$(cat "$pidfile")"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" && echo "Stopped PID $pid ($(basename "$pidfile"))"
    stopped=$((stopped + 1))
  else
    echo "Stale PID file $(basename "$pidfile"); removing"
  fi
  rm -f "$pidfile"
done

if [[ "$stopped" -eq 0 ]]; then
  echo "No running simulators found."
fi
