#!/usr/bin/env bash
#
# Shared defaults for traffic simulators (source from other scripts; do not run directly).
#
# Header names: keep in sync with acme.security.headers in application.yml.
# Default DNs: same as README.md / integration tests (override with SSL_CLIENT_*).
#
# shellcheck shell=bash

MTLS_SUBJECT_HEADER="${ACME_HEADER_SUBJECT_DN:-x-amzn-mtls-clientcert-subject}"
MTLS_ISSUER_HEADER="${ACME_HEADER_ISSUER_DN:-x-amzn-mtls-clientcert-issuer}"

_DEFAULT_SUBJECT='CN=jdoe,OU=Engineering,OU=Users,DC=corp,DC=acme,DC=org'
_DEFAULT_ISSUER='CN=Acme Intermediate CA,O=Acme Corp,C=US'

SSL_CLIENT_SUBJECT_DN="${SSL_CLIENT_SUBJECT_DN:-$_DEFAULT_SUBJECT}"
SSL_CLIENT_ISSUER_DN="${SSL_CLIENT_ISSUER_DN:-$_DEFAULT_ISSUER}"

# Extra curl -H args for /api/** calls (health probes do not use these)
ACME_MTLS_HEADERS=(
  -H "${MTLS_SUBJECT_HEADER}: ${SSL_CLIENT_SUBJECT_DN}"
  -H "${MTLS_ISSUER_HEADER}: ${SSL_CLIENT_ISSUER_DN}"
)

timestamp() {
  date '+%Y-%m-%d %H:%M:%S'
}

log() {
  printf '[%s] %s\n' "$(timestamp)" "$*"
}
