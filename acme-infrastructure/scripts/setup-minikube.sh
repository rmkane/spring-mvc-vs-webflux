#!/bin/bash
# Quick setup script for Minikube X.509 testing

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=========================================="
echo "Minikube X.509 Setup"
echo "=========================================="
echo ""

# Check if minikube is running
if ! minikube status &>/dev/null; then
    echo "Starting Minikube..."
    minikube start
else
    echo "✓ Minikube is running"
fi

# Enable ingress
echo "Enabling NGINX Ingress..."
minikube addons enable ingress

# Wait for ingress to be ready
echo "Waiting for Ingress controller to be ready..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s || echo "Warning: Ingress may still be starting"

# Create namespaces
echo "Creating namespaces..."
kubectl apply -f "$PROJECT_ROOT/acme-infrastructure/infrastructure/namespace.yaml"

# Create CA chain secret (in acme-apps namespace where the Ingress is)
echo "Creating CA chain secret..."
if [ -f "$PROJECT_ROOT/acme-infrastructure/certs/ca/ca-chain.crt" ]; then
    # Note: NGINX Ingress requires the key to be named "ca.crt"
    kubectl create secret generic ca-chain-secret \
      --from-file=ca.crt="$PROJECT_ROOT/acme-infrastructure/certs/ca/ca-chain.crt" \
      -n acme-apps \
      --dry-run=client -o yaml | kubectl apply -f -
    echo "✓ CA chain secret created in acme-apps namespace"
else
    echo "⚠ Error: CA chain certificate not found at acme-infrastructure/certs/ca/ca-chain.crt"
    echo "   Run: ./acme-infrastructure/scripts/certs/setup-all-certs.sh"
    exit 1
fi

# Create TLS secret for ingress (in acme-apps namespace where the Ingress is)
# Note: TLS secrets must be in the same namespace as the Ingress resource
if [ -f "$PROJECT_ROOT/acme-infrastructure/certs/ingress.crt" ] && [ -f "$PROJECT_ROOT/acme-infrastructure/certs/ingress.key" ]; then
    echo "Creating Ingress TLS secret..."
    kubectl create secret tls ingress-tls-secret \
      --cert="$PROJECT_ROOT/acme-infrastructure/certs/ingress.crt" \
      --key="$PROJECT_ROOT/acme-infrastructure/certs/ingress.key" \
      -n acme-apps \
      --dry-run=client -o yaml | kubectl apply -f -
    echo "✓ Ingress TLS secret created in acme-apps namespace"
else
    echo "⚠ Warning: Ingress certificates not found."
    echo "   You'll need to create ingress-tls-secret manually:"
    echo "   kubectl create secret tls ingress-tls-secret \\"
    echo "     --cert=acme-infrastructure/certs/ingress.crt \\"
    echo "     --key=acme-infrastructure/certs/ingress.key \\"
    echo "     -n acme-apps"
fi

# Deploy test service
echo "Deploying test service..."
kubectl apply -f "$PROJECT_ROOT/acme-infrastructure/test/test-service.yaml"

# Wait for test service to be ready
echo "Waiting for test service to be ready..."
kubectl wait --namespace acme-apps \
  --for=condition=available deployment/test-headers \
  --timeout=60s || echo "Warning: Test service may still be starting"

# Enable snippets (required for header forwarding)
echo "Enabling snippet directives in Ingress controller..."
"$SCRIPT_DIR/enable-snippets.sh" || echo "⚠ Warning: Failed to enable snippets. You may need to enable them manually."

# Deploy ingress
echo "Deploying Ingress..."
kubectl apply -f "$PROJECT_ROOT/acme-infrastructure/infrastructure/ingress.yaml" || {
    echo "⚠ Error: Ingress deployment failed. Snippets may not be enabled."
    echo "   Try running: ./acme-infrastructure/scripts/enable-snippets.sh"
    echo "   Or use: kubectl apply -f acme-infrastructure/test/ingress-no-snippets.yaml (limited functionality)"
    exit 1
}

# Get ingress IP
echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
INGRESS_IP=$(minikube ip)
echo "Minikube IP: $INGRESS_IP"
echo ""
echo "⚠ IMPORTANT: Minikube requires a tunnel to expose the Ingress"
echo ""
echo "Next steps:"
echo "1. Start Minikube tunnel (in a separate terminal):"
echo "   minikube tunnel"
echo "   (Keep this running while testing)"
echo ""
echo "2. Wait for Ingress to get an external IP:"
echo "   kubectl get ingress -n acme-apps -w"
echo "   (Press Ctrl+C once you see an ADDRESS)"
echo ""
echo "3. Add to /etc/hosts:"
echo "   echo '$INGRESS_IP acme.local' | sudo tee -a /etc/hosts"
echo ""
echo "4. Test in browser:"
echo "   https://acme.local/test"
echo ""
echo "5. Check status:"
echo "   kubectl get ingress -n acme-apps"
echo "   kubectl get pods -n acme-apps"
echo ""
