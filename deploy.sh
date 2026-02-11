#!/bin/bash
# Quick start script for Docker deployment

echo "🚀 Starting Docker Compose deployment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop."
    exit 1
fi

# Build images
echo "🔨 Building Docker images..."
docker-compose build --no-cache

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

# Start services
echo "📦 Starting services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be healthy..."
sleep 10

# Check status
echo ""
echo "📋 Service Status:"
docker-compose ps

echo ""
echo "✅ Deployment complete!"
echo ""
echo "🌐 Access points:"
echo "   Frontend:  http://localhost"
echo "   Backend:   http://backend.localhost/api/items"
echo "   Traefik:   http://localhost:8080 (if enabled)"
echo ""
echo "📊 View logs:"
echo "   All:       docker-compose logs -f"
echo "   Frontend:  docker-compose logs -f frontend"
echo "   Backend:   docker-compose logs -f backend"
echo "   Database:  docker-compose logs -f db"
echo ""
echo "🛑 Stop services:"
echo "   docker-compose down"
echo ""
