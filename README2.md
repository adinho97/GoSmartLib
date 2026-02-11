# Docker Deployment Setup

This is a full-stack application with Angular frontend, Spring Boot backend, MySQL database, and Traefik reverse proxy.

## Architecture

- **Frontend**: Angular 16 application served by Nginx
- **Backend**: Spring Boot REST API
- **Database**: MySQL 8
- **Reverse Proxy**: Traefik v2.10 for routing and load balancing

## Prerequisites

- Docker Desktop installed and running
- Docker Compose v2+

## Quick Start

1. **Build and start all services:**

   ```bash
   docker-compose up --build
   ```

2. **Access the application:**
   - **Frontend**: http://localhost (or http://localhost:80)
   - **Backend API**: http://backend.localhost/api/items
   - **Traefik Dashboard**: http://localhost:8080 (if enabled)

3. **Stop the services:**
   ```bash
   docker-compose down
   ```

## Services

### Frontend

- **Port**: 80 (via Traefik)
- **Hostname**: localhost
- **Built with**: Angular 16, Nginx
- **Features**: Item listing and creation form

### Backend

- **Port**: 8080
- **Hostname**: backend.localhost
- **Built with**: Spring Boot 3.2, Java 17
- **Endpoints**:
  - `GET /api/items` - List all items
  - `POST /api/items` - Create a new item

### Database

- **Type**: MySQL 8
- **Host**: db
- **Port**: 3306
- **Database**: mydb
- **User**: root
- **Password**: rootpassword
- **Data Persistence**: Named volume `db_data`

### Reverse Proxy

- **Type**: Traefik v2.10
- **Port**: 80 (HTTP)
- **Configuration**: `traefik/traefik.yml`
- **Auto-discovery**: Docker labels on containers

## Project Structure

```
├── docker-compose.yaml        # Main compose configuration
├── traefik/
│   ├── traefik.yml           # Traefik configuration
│   └── acme.json             # ACME certificates (auto-generated)
├── backend/
│   ├── Dockerfile            # Spring Boot multi-stage build
│   ├── pom.xml               # Maven dependencies
│   └── src/                  # Java source code
├── frontend/
│   ├── Dockerfile            # Angular multi-stage build
│   ├── package.json          # Node dependencies
│   ├── angular.json          # Angular CLI configuration
│   ├── tsconfig.json         # TypeScript configuration
│   └── src/                  # Angular application
└── .gitignore                # Git ignore rules
```

## Environment Variables

Backend uses the following configuration (see `backend/src/main/resources/application.properties`):

- `MYSQL_DATABASE=mydb`
- `MYSQL_ROOT_PASSWORD=rootpassword`

## Development Notes

- **CORS**: Enabled in backend controller for all origins
- **Database Initialization**: Hibernate auto-creates tables on first run (ddl-auto=update)
- **Frontend Build**: Angular production build optimized for deployment
- **Traefik**: Uses Docker service discovery for automatic routing

## Troubleshooting

1. **Port 80 in use:**

   ```bash
   docker-compose down
   # or change port in docker-compose.yaml
   ```

2. **Database connection errors:**
   - Ensure MySQL service is running: `docker-compose ps`
   - Check logs: `docker-compose logs db`

3. **Frontend not building:**
   - Clear node_modules: `docker volume prune`
   - Rebuild: `docker-compose up --build --no-cache`

4. **Backend API not responding:**
   - Check backend logs: `docker-compose logs backend`
   - Verify database is initialized: `docker-compose logs db`

## Scaling & Production

For production deployment:

1. Update `application.properties` with proper database credentials
2. Configure Traefik for HTTPS/SSL (ACME)
3. Use environment-specific docker-compose files
4. Set proper resource limits in docker-compose.yaml
5. Use health checks for each service
6. Store database backups separately
