#!/usr/bin/env bash

SCRIPTS_DIR="$(dirname "$0")"

"$SCRIPTS_DIR/test-mvc.sh" get-all
"$SCRIPTS_DIR/test-webflux.sh" get-all

"$SCRIPTS_DIR/test-mvc.sh" get 1
"$SCRIPTS_DIR/test-webflux.sh" get 1
