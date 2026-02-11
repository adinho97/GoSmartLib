# 🚀 Quick Start Guide

## One Command Deployment

```bash
docker-compose up --build
```

That's it! Your application will be running.

## Access Your Application

Once the deployment completes, access these URLs:

- **Frontend**: http://localhost
- **Backend API**: http://backend.localhost/api/items
- **API Documentation**: Check backend console for Spring Boot startup messages

## Available Commands

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Remove everything and start fresh
docker-compose down -v
docker-compose up --build

# View service status
docker-compose ps
```

## Platform-Specific Scripts

### Windows

```bash
deploy.bat
```

### Mac/Linux

```bash
bash deploy.sh
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Traefik (Reverse Proxy) - Port 80                      │
├─────────────────────────────────────────────────────────┤
│  Frontend (Angular + Nginx)     Backend (Spring Boot)    │
│  http://localhost               http://backend.localhost│
├─────────────────────────────────────────────────────────┤
│              MySQL Database (Port 3306)                  │
└─────────────────────────────────────────────────────────┘
```

## Troubleshooting

**Port 80 already in use?**

```bash
# Stop other services using port 80
# Or modify docker-compose.yaml ports section
```

**Services not starting?**

```bash
# Check logs
docker-compose logs

# Rebuild from scratch
docker-compose down -v
docker-compose up --build
```

**Frontend not loading?**

```bash
# Wait a bit longer for build
# Check frontend logs
docker-compose logs frontend
```

## Next Steps

- See [README.md](README.md) for detailed documentation
- See [DEPLOYMENT.md](DEPLOYMENT.md) for production considerations
- Modify credentials in `docker-compose.yaml` for security

---

**Ready?** Run `docker-compose up --build` now!
