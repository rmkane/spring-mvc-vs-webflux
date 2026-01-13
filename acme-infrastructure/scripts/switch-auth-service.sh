#!/bin/bash
# Script to switch between LDAP and DB auth services in Kubernetes

set -e

AUTH_TYPE="${1:-db}"

if [ "$AUTH_TYPE" != "ldap" ] && [ "$AUTH_TYPE" != "db" ]; then
    echo "Usage: $0 [ldap|db]"
    echo "  Defaults to 'db' if not specified"
    exit 1
fi

NAMESPACE="acme-apps"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOYMENTS_DIR="$(cd "$SCRIPT_DIR/../deployments" && pwd)"

echo "Switching to auth-service-${AUTH_TYPE}..."

# Update API services to point to the selected auth service
if [ "$AUTH_TYPE" == "ldap" ]; then
    AUTH_SERVICE="auth-service-ldap"
else
    AUTH_SERVICE="auth-service-db"
fi

echo "Updating API services to use ${AUTH_SERVICE}..."

# Update api-mvc deployment
kubectl set env deployment/api-mvc -n "$NAMESPACE" \
    AUTH_SERVICE_BASE_URL="https://${AUTH_SERVICE}:8082" || {
    echo "Failed to update api-mvc. Make sure it's deployed."
    exit 1
}

# Check if api-webflux exists and update it too
if kubectl get deployment api-webflux -n "$NAMESPACE" &>/dev/null; then
    kubectl set env deployment/api-webflux -n "$NAMESPACE" \
        AUTH_SERVICE_BASE_URL="https://${AUTH_SERVICE}:8082" || {
        echo "Failed to update api-webflux."
        exit 1
    }
fi

echo "✅ API services updated to use ${AUTH_SERVICE}"
echo ""
echo "To complete the switch:"
echo "  1. Scale down the old auth service:"
if [ "$AUTH_TYPE" == "ldap" ]; then
    echo "     kubectl scale deployment/auth-service-db -n $NAMESPACE --replicas=0"
else
    echo "     kubectl scale deployment/auth-service-ldap -n $NAMESPACE --replicas=0"
fi
echo ""
echo "  2. Scale up the new auth service:"
echo "     kubectl scale deployment/${AUTH_SERVICE} -n $NAMESPACE --replicas=1"
echo ""
echo "  3. Restart API services to pick up the new auth service:"
echo "     kubectl rollout restart deployment/api-mvc -n $NAMESPACE"
if kubectl get deployment api-webflux -n "$NAMESPACE" &>/dev/null; then
    echo "     kubectl rollout restart deployment/api-webflux -n $NAMESPACE"
fi
