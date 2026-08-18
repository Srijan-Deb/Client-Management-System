# CMS - Client Management System

[![CI](https://github.com/Srijan-Deb/Client-Management-System/actions/workflows/ci.yml/badge.svg)](https://github.com/Srijan-Deb/Client-Management-System/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)

> A production-grade, event-driven microservices system built with **Java 21 + Spring Boot 3**,
> **Apache Kafka**, **MySQL 8**, **Redis**, **Keycloak**, and **MinIO** - running entirely on
> free/OSS tooling.

---

## Architecture Overview

```
+--------------------------------------------------+
|                  CLIENT TIER                     |
|    Postman / React (later) / Partner API         |
+--------------------------------------------------+
                      | HTTPS
+--------------------------------------------------+
|                   EDGE TIER                      |
|    API Gateway (Spring Cloud Gateway :8090)      |
|              Keycloak (OAuth2 / JWT)             |
+--------------------------------------------------+
        |              |              |
        v              v              v
+----------+    +-----------+    +----------+
|  Client  |    |  Account  |    | Billing  |
| Service  |    |  Service  |    | Service  |
|  :8081   |    |   :8082   |    |  :8083   |
+----------+    +-----------+    +----------+
        |              |              |
        +------+--------+------+------+
               |               |
               v               v
        [Apache Kafka]    [MySQL 8 / Redis]
               |
               v
    +---------------------+
    | Notification Service|
    |       :8084         |
    +---------------------+
               |
               v
         [Email / SMS]
```

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| `api-gateway` | 8090 | JWT validation, rate limiting (20 req/s per user), routing |
| `client-service` | 8081 | Client onboarding, contacts, addresses, support tickets |
| `account-service` | 8082 | Account linking, multi-tenant B2B accounts |
| `billing-service` | 8083 | Contracts, invoices, payments, MinIO document storage |
| `notification-service` | 8084 | Kafka consumer, email/SMS dispatch |

---

## Tech Stack

- **Language:** Java 21 (virtual threads ready)
- **Framework:** Spring Boot 3.3.2, Spring Cloud 2023.0.3
- **API Gateway:** Spring Cloud Gateway (WebFlux, reactive)
- **Auth:** Keycloak 24.0.5 (OAuth2/OIDC, JWT RS256)
- **Messaging:** Apache Kafka 3.7 (with distributed trace propagation)
- **Database:** MySQL 8.0 per service (Flyway migrations)
- **Cache:** Redis (Spring Cache + Gateway rate limiter)
- **Storage:** MinIO (S3-compatible, for billing documents)
- **Observability:** Micrometer + OpenTelemetry -> Jaeger, Prometheus + Grafana
- **Testing:** JUnit 5, Testcontainers (real MySQL + Kafka in CI), WireMock
- **CI/CD:** GitHub Actions, SonarQube, GHCR (Phase 9)

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker Desktop (for Testcontainers in integration tests)
- All infrastructure services started via `docker-compose.yml`

### Quick Start (Local Dev)

```bash
# 1. Start infrastructure (MySQL, Kafka, Redis, Keycloak, MinIO, Jaeger, Prometheus)
docker compose up -d

# 2. Copy .env and fill in secrets
cp .env.example .env    # (or use the provided .env with dev defaults)

# 3. Build everything (skip tests for speed)
mvn clean package -DskipTests

# 4. Start each service in its own terminal
cd api-gateway          && mvn spring-boot:run
cd client-service       && mvn spring-boot:run
cd account-service      && mvn spring-boot:run
cd billing-service      && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
```

### Run Tests (Testcontainers - requires Docker)

```bash
# Unit + Integration tests (Testcontainers spins up MySQL + Kafka automatically)
mvn test

# With coverage report (JaCoCo)
mvn verify -Pcoverage
```

### Production Deployment (Docker Compose)

```bash
# Pull images from GHCR and start with production settings
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Kubernetes / k3s Deployment (Helm)

```bash
# Dry-run to validate the chart renders correctly
helm template cms ./helm/cms --set secrets.mysqlPassword=mypass

# Install into the 'cms' namespace
kubectl create namespace cms
helm install cms ./helm/cms \
  --namespace cms \
  --set secrets.mysqlPassword=mypass \
  --set secrets.minioSecretKey=mysecret \
  --set secrets.stripeApiKey=sk_live_...

# Upgrade after new image push
helm upgrade cms ./helm/cms --namespace cms --set global.imageTag=1.2.0
```

---

## CI/CD Pipeline (Phase 9)

```
Push to main
    |
    +-> [test] mvn test (Testcontainers: real MySQL + Kafka)
    |       |
    |       +-> JUnit results published to GitHub Actions UI
    |
    +-> [sonar] SonarQube quality gate
    |
    +-> [build-push] (main only)
            |
            +-> mvn spring-boot:build-image (Paketo Buildpacks)
            +-> Push to ghcr.io/Srijan-Deb/cms-<service>:latest
```

**Required GitHub Actions secrets:**

| Secret | How to get it |
|---|---|
| `SONAR_TOKEN` | Generate at [sonarcloud.io](https://sonarcloud.io) -> My Account -> Security |
| `SONAR_HOST_URL` | `https://sonarcloud.io` (SonarCloud free) or your self-hosted URL |
| `GITHUB_TOKEN` | Automatically injected by GitHub Actions - no setup needed |

---

## Security (Phase 8)

- **Rate limiting:** 20 req/s per authenticated user (JWT sub), 5 req/s per IP on `/auth/**`
  (burst allowances 2x). Returns `429 Too Many Requests` when exhausted.
- **Bean validation:** All request DTOs have `@NotBlank`, `@Size`, `@Positive`, `@Email`, `@Pattern` constraints.
  Invalid requests return `400 {"errorCode":"VALIDATION_ERROR","fieldErrors":{...}}`.
- **Centralized error handling:** `@RestControllerAdvice` on all services - consistent error envelope.
- **Secrets externalized:** DB passwords, MinIO keys, Stripe API key all read from environment
  variables. `.env` file is git-ignored. `docker-compose.yml` uses `env_file: .env`.
- **XSS protection:** Free-text fields (`companyName`) validated with `@Pattern(^[^<>&"']*$)`.
- **SQLi:** All queries use JPA/Spring Data (prepared statements) - zero `nativeQuery` string concatenation.
- **Audit logs:** Every mutating action written to `activity_logs` table in each service's database.

---

## Observability (Phase 7)

| Tool | URL | What it shows |
|---|---|---|
| Jaeger | http://localhost:16686 | Distributed traces: Gateway -> Service -> Kafka -> Notification |
| Prometheus | http://localhost:9090 | Metrics scrape from all 5 services |
| Grafana | http://localhost:3000 | Dashboards (admin/admin) |
| Alertmanager | http://localhost:9093 | Alert routing |

---

## Project Phases

| Phase | Description | Status |
|---|---|---|
| 1 | Foundation (Spring Boot, Keycloak, Docker Compose) | Done |
| 2 | Client Service (CRUD, Contacts, Addresses) | Done |
| 3 | Account Service (linking, multi-tenant) | Done |
| 4 | Billing Service (contracts, invoices, Stripe, MinIO) | Done |
| 5 | Notification Service (Kafka consumer, email/SMS) | Done |
| 6 | Support Tickets + Circuit Breaker + Resilience4j | Done |
| 7 | Observability (Jaeger, Prometheus, Grafana) | Done |
| 8 | Security Hardening (rate limiting, validation, secrets) | Done |
| 9 | CI/CD (GitHub Actions, SonarQube, GHCR, Helm) | Done |
