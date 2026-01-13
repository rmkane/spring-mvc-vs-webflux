#!/bin/bash
# Script to build and deploy all services to Kubernetes

set -e

echo "🚀 Building and deploying services to Kubernetes..."

# Check if minikube is running
if ! minikube status > /dev/null 2>&1; then
    echo "❌ Minikube is not running. Start it with: minikube start"
    exit 1
fi

# Set Docker to use minikube's Docker daemon
echo "📦 Setting up Docker environment for minikube..."
eval $(minikube docker-env)

# Build Docker images
echo ""
echo "🔨 Building Docker images..."

# Build custom LDAP image with Kubernetes fix
echo "Building custom LDAP image (osixia with K8s fix)..."
docker build -t acme-openldap:latest -f acme-infrastructure/docker/ldap/Dockerfile . || {
    echo "❌ Failed to build custom LDAP image"
    exit 1
}
echo "✅ Built acme-openldap:latest"

echo "Building auth-service-ldap..."
docker build -t acme-auth-service-ldap:latest -f acme-auth-service-ldap/Dockerfile . || {
    echo "❌ Failed to build auth-service-ldap"
    exit 1
}

echo "Building auth-service-db..."
docker build -t acme-auth-service-db:latest -f acme-auth-service-db/Dockerfile . || {
    echo "❌ Failed to build auth-service-db"
    exit 1
}

echo "Building api-mvc..."
docker build -t acme-api-mvc:latest -f acme-api-mvc/Dockerfile . || {
    echo "❌ Failed to build api-mvc"
    exit 1
}

# Deploy to Kubernetes
echo ""
echo "📤 Deploying to Kubernetes..."
kubectl apply -f acme-infrastructure/deployments/postgres-jpa.yaml
kubectl apply -f acme-infrastructure/deployments/postgres-auth.yaml
kubectl apply -f acme-infrastructure/deployments/ldap.yaml
kubectl apply -f acme-infrastructure/deployments/auth-service-ldap.yaml
kubectl apply -f acme-infrastructure/deployments/auth-service-db.yaml
kubectl apply -f acme-infrastructure/deployments/api-mvc.yaml

# Wait for deployments
echo ""
echo "⏳ Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/postgres-jpa -n acme-apps || true
kubectl wait --for=condition=available --timeout=300s deployment/postgres-auth -n acme-apps || true
kubectl wait --for=condition=available --timeout=300s deployment/ldap -n acme-apps || true
kubectl wait --for=condition=available --timeout=300s deployment/auth-service-ldap -n acme-apps || true
kubectl wait --for=condition=available --timeout=300s deployment/auth-service-db -n acme-apps || true
kubectl wait --for=condition=available --timeout=300s deployment/api-mvc -n acme-apps || true

# Show status
echo ""
echo "✅ Deployment complete! Pod status:"
kubectl get pods -n acme-apps

echo ""
echo "📋 Useful commands:"
echo "  kubectl get pods -n acme-apps              # Check pod status"
echo "  kubectl logs <pod-name> -n acme-apps      # View logs"
echo "  kubectl describe pod <pod-name> -n acme-apps  # Detailed info"
