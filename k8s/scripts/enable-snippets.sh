#!/bin/bash
# Enable snippet directives in NGINX Ingress Controller for Minikube

set -e

echo "Enabling snippet directives in NGINX Ingress Controller..."

# Check if ConfigMap exists
if ! kubectl get configmap ingress-nginx-controller -n ingress-nginx &>/dev/null; then
    echo "⚠ Error: ingress-nginx-controller ConfigMap not found"
    echo "   Make sure NGINX Ingress is installed: minikube addons enable ingress"
    exit 1
fi

# Check if allow-snippet-annotations and allow-risky-annotations are already set
CURRENT_SNIPPET=$(kubectl get configmap ingress-nginx-controller -n ingress-nginx -o jsonpath='{.data.allow-snippet-annotations}' 2>/dev/null || echo "")
CURRENT_RISKY=$(kubectl get configmap ingress-nginx-controller -n ingress-nginx -o jsonpath='{.data.allow-risky-annotations}' 2>/dev/null || echo "")

if [ "$CURRENT_SNIPPET" = "true" ] && [ "$CURRENT_RISKY" = "true" ]; then
    echo "✓ Snippet and risky annotations already enabled"
    exit 0
fi

# Add allow-snippet-annotations and allow-risky-annotations to the ConfigMap
echo "Patching Ingress controller ConfigMap..."

# Build patch JSON without requiring jq
PATCH_JSON='{"data":{'
NEEDS_COMMA=false

if [ "$CURRENT_SNIPPET" != "true" ]; then
    PATCH_JSON="${PATCH_JSON}\"allow-snippet-annotations\":\"true\""
    NEEDS_COMMA=true
fi

if [ "$CURRENT_RISKY" != "true" ]; then
    if [ "$NEEDS_COMMA" = true ]; then
        PATCH_JSON="${PATCH_JSON},"
    fi
    PATCH_JSON="${PATCH_JSON}\"allow-risky-annotations\":\"true\""
fi

PATCH_JSON="${PATCH_JSON}}}"

kubectl patch configmap ingress-nginx-controller -n ingress-nginx \
  --type merge \
  -p "$PATCH_JSON"

echo "✓ Snippet and risky annotations enabled"
echo "Restarting Ingress controller pods..."
kubectl rollout restart deployment ingress-nginx-controller -n ingress-nginx
echo "Waiting for rollout to complete..."
kubectl rollout status deployment ingress-nginx-controller -n ingress-nginx --timeout=120s

# Delete and recreate the admission webhook to pick up new configuration
echo "Refreshing admission webhook to pick up new configuration..."
if kubectl get validatingwebhookconfiguration ingress-nginx-admission &>/dev/null; then
    kubectl delete validatingwebhookconfiguration ingress-nginx-admission
    echo "Waiting for webhook to be recreated..."
    sleep 5
    # The webhook will be automatically recreated by the Ingress controller
fi

echo "✓ Ingress controller restarted with snippets enabled"
