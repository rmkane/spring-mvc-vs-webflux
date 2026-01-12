#!/bin/bash
# Custom entrypoint for osixia/openldap that fixes Kubernetes FQDN issue
# This bypasses the osixia wrapper and starts slapd directly

set -e

# Copy custom schema and LDIF files
mkdir -p /container/service/slapd/assets/config/bootstrap/schema/custom
mkdir -p /container/service/slapd/assets/config/bootstrap/ldif/custom
cp /schema-import/*.schema /container/service/slapd/assets/config/bootstrap/schema/custom/ 2>/dev/null || true
cp /ldif-import/*.ldif /container/service/slapd/assets/config/bootstrap/ldif/custom/ 2>/dev/null || true

# Use osixia's tool/run but patch the environment to prevent FQDN issues
# The key is to set HOSTNAME before the scripts run
export HOSTNAME=ldap

# Create a custom environment file that overrides URL construction
mkdir -p /container/environment/99-custom
cat > /container/environment/99-custom/k8s-fix.yaml << 'ENVEOF'
# Kubernetes fix: Override URL construction
export LDAP_URLS="ldap://0.0.0.0:389 ldaps://0.0.0.0:636"
ENVEOF

# Now run the osixia tool/run which will process our custom env file
exec /container/tool/run
