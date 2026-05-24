# GoSmartLib

## Projectbeschrijving

GoSmartLib is een webtoepassing voor het beheren van een bibliotheek binnen GoScholengroep Antwerpen. De applicatie biedt een webinterface (frontend) en een REST API (backend) voor bibliotheekbeheer, met ondersteuning voor authenticatie en integraties zoals Smartschool.

## Architectuuroverzicht

- Frontend: Angular 20 SPA, gebouwd naar statische bestanden en geserveerd via Nginx.
- Backend: Spring Boot 3 (Java 17) REST API met JPA/Hibernate, beveiliging en JWT.
- Database: MySQL 8.
- Reverse proxy/ingress: Traefik met TLS (LetsEncrypt) voor domeingebaseerde routing.
- Docker Compose: orkestreert Traefik, backend, frontend en database.

### Datastromen (hoog niveau)

- Browser -> Traefik -> Frontend (Nginx) voor de SPA.
- Browser -> Traefik -> Backend voor /api requests.
- Backend -> MySQL voor dataopslag.
- Backend -> Smartschool OAuth API voor login/identiteit en aparte JWT based login voor admin.

## Projectstructuur

```
.
├─ backend/            # Spring Boot API
├─ frontend/           # Angular SPA
├─ traefik/            # Reverse proxy config
├─ docker-compose.yaml # Docker Compose stack
└─ README.md
```

## Setup (Docker Compose)

### Vereisten

- Docker + Docker Compose

### Configuratie

Maak een .env bestand in de root met minstens:

```
DOMAIN=example.com
MYSQL_ROOT_PASSWORD=change-me
MYSQL_DATABASE=gosmartlib
MYSQL_USER=gosmartlib
MYSQL_PASSWORD=change-me
SMARTSCHOOL_CLIENT_SECRET=change-me
INVITE_ADMIN_KEY=change-me
JWT_SECRET=change-me-32-characters-min
```

### Starten

```
docker compose up --build
```

### Docker image (backend)

- Multi-stage build: Maven enkel in de build stage.
- Runtime image gebruikt `eclipse-temurin:17-jre` (geen JDK of Maven in productie, nu kleiner).

### Belangrijke poorten

- Traefik: 80, 443 (dashboard op 127.0.0.1:8080)
- Frontend: 3000 -> container 80
- Backend: 8081 -> container 8080
- MySQL: 3307 -> container 3306

## Lokale ontwikkeling (zonder Docker)

### Backend (Spring Boot)

Vereisten: Java 17, Maven

1. Configureer lokale database in application-local.properties:
   - Host: localhost
   - DB: gosmartlib
2. Start de backend:

```
cd backend
mvn spring-boot:run
```

Standaard gebruikt de backend MySQL op localhost:3306.

### Frontend (Angular)

Vereisten: Node.js 20+ en npm

1. Installeer dependencies:

```
cd frontend
npm install
```

2. Start de dev server:

```
npm start
```

De proxy voor API-calls staat in proxy.conf.json en wijst naar http://localhost:8080.

## Tests

### Backend

```
cd backend
mvn test
```

### Frontend

```
cd frontend
npm test
```

## Extra configuratie

- CORS: zie backend/src/main/resources/application.properties
- Smartschool: client-id in application.properties, secret via SMARTSCHOOL_CLIENT_SECRET
- JWT: secret via JWT_SECRET
