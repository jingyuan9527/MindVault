#!/bin/bash
# Multi-architecture Docker build script for MindVault
set -e

VERSION=${1:-"latest"}
REGISTRY=${2:-"mindvault"}

echo "=== Building MindVault Multi-Arch Images ==="
echo "Version: $VERSION"
echo "Registry: $REGISTRY"
echo ""

# Enable buildx
docker buildx create --use --name mindvault-builder 2>/dev/null || docker buildx use mindvault-builder
docker buildx inspect --bootstrap

PLATFORMS="linux/amd64,linux/arm64"

echo ""
echo "=== Building Backend ==="
docker buildx build \
    --platform $PLATFORMS \
    -t $REGISTRY/backend:$VERSION \
    -t $REGISTRY/backend:latest \
    -f Dockerfile.backend \
    --push \
    ..

echo ""
echo "=== Building Frontend ==="
docker buildx build \
    --platform $PLATFORMS \
    -t $REGISTRY/frontend:$VERSION \
    -t $REGISTRY/frontend:latest \
    -f Dockerfile.frontend \
    --push \
    ..

echo ""
echo "=== Build Complete ==="
echo "Images:"
echo "  $REGISTRY/backend:$VERSION"
echo "  $REGISTRY/frontend:$VERSION"
echo ""
echo "To use with docker-compose, update image references in docker-compose.yml"