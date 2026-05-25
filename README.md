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

## JWT authenticatie (backend)

We gebruiken een JWT-filter voor beveiligde API-calls.

### Wat doet het filter

- Leest de `Authorization: Bearer <token>` header.
- Valideert het token (signatuur + expiratie).
- Controleert `tokenVersion` tegen de database om oude tokens na wachtwoordwijziging ongeldig te maken.
- Zet de gebruiker/rol in de Spring Security context voor autorisatie.

### Waarom nodig

- Zonder dit filter worden endpoints met `authenticated()` of `hasRole(...)` niet toegankelijk.
- Het is nodig om admin- en andere beveiligde routes correct af te schermen.

### Testen

- Er is een unit test die bevestigt dat een geldig token authenticatie zet en een verouderd token geweigerd wordt.

### Custom headers (X-User-*)

Naast de JWT gebruiken we context-headers vanuit de frontend: `X-User-Sub`, `X-User-Role`, `X-User-Name`.

- `X-User-Sub` is een unieke identifier om de gebruiker en bijhorende school te bepalen.
- `X-User-Role` wordt gebruikt als extra context in de controllers (bv. om het zicht of gedrag te sturen).
- `X-User-Name` is vooral voor display/audit-info (geen security).

Let op: in admin-mode kan `X-User-Sub` een vaste waarde zijn (bv. "admin"), dus het is geen betrouwbare unieke sleutel.

**Kritisch:** deze headers zijn client-controlled en dus niet betrouwbaar voor echte autorisatie.
Echte toegang wordt afgedwongen via de JWT + Spring Security (`authenticated()` / `hasRole(...)`).
De headers zijn dus convenience/context, niet de bron van waarheid voor security.

## Rate limiting (backend)

We gebruiken een eenvoudige in-memory rate limiter om gevoelige endpoints te beschermen tegen misbruik en onbedoelde load.

### Waar toegepast

- POST /api/boeken/isbn/**: 10 requests per minuut per client IP (import van ISBNs).
- POST /api/admin/login en POST /api/admin/setup: 5 requests per minuut per client IP (admin login/setup).

De limiet is per client IP (via `X-Forwarded-For` of anders `remoteAddr`). Als meerdere scholen achter dezelfde IP/proxy zitten, delen ze dezelfde limiet.

### Reden

- Bescherming tegen brute-force en credential stuffing op admin login.
- Beperken van zware of externe calls tijdens ISBN-import (quota/belasting).

### Trade-offs

- In-memory en per node: niet gedeeld tussen meerdere instances, dus niet geschikt voor horizontale schaal.
- Reset bij restart: limieten worden niet bewaard.
- Per IP: meerdere clients achter dezelfde IP delen de limiet; meerdere IPs kunnen de limiet omzeilen.

Voor een grotere productie-omgeving zouden we dit vervangen door een gedeelde limiter (bv. Redis/Bucket4j of Resilience4j).
