# CMS — Client Management System: Complete Project Documentation

> **Beginner-friendly, start-to-finish handbook.** No prior knowledge of this project assumed.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Complete Project Architecture](#2-complete-project-architecture)
3. [Project Folder Structure](#3-project-folder-structure)
4. [Installation and Initial Setup](#4-installation-and-initial-setup)
5. [How to Start the Project Manually](#5-how-to-start-the-project-manually)
6. [How to Stop the Project](#6-how-to-stop-the-project)
7. [Complete Manual Operation Guide](#7-complete-manual-operation-guide)
8. [Authentication and Authorization](#8-authentication-and-authorization)
9. [API Documentation](#9-api-documentation)
10. [Database Documentation](#10-database-documentation)
11. [Docker Documentation](#11-docker-documentation)
12. [Git and GitHub Workflow](#12-git-and-github-workflow)
13. [CI/CD Pipeline](#13-cicd-pipeline)
14. [Postman Testing Guide](#14-postman-testing-guide)
15. [Troubleshooting Guide](#15-troubleshooting-guide)
16. [Logs and Monitoring](#16-logs-and-monitoring)
17. [Backup and Recovery](#17-backup-and-recovery)
18. [Development Workflow](#18-development-workflow)
19. [Production / Deployment Guide](#19-production--deployment-guide)
20. [Daily Operation Checklist](#20-daily-operation-checklist)
21. [Commands Cheat Sheet](#21-commands-cheat-sheet)
22. [Complete End-to-End Example](#22-complete-end-to-end-example)

---

## 1. Project Overview

### What is this project?

The **Client Management System (CMS)** is a backend platform for managing B2B (business-to-business) clients. It is built as a **microservices application** — meaning instead of one large program, the system is split into several smaller, independent services that each handle a specific area of the business.

### Problem it solves

A company that manages many business clients needs a system to:
- Onboard new clients and store their information
- Manage contracts, subscriptions, and billing
- Process and track payments
- Handle support tickets from clients
- Send automated email notifications when key events occur
- Monitor the health and performance of all services

This project provides all of these capabilities in one integrated platform.

### Key Features

| Feature | Description |
|---|---|
| Client Onboarding | Register a new client, store contacts and addresses |
| Account Management | Link clients to multi-tenant accounts |
| Billing and Contracts | Create contracts, generate invoices as PDFs, store in cloud storage |
| Payment Processing | Process payments via Stripe (test mode), handle retries |
| Support Tickets | Create, assign, resolve, and close support tickets |
| Notifications | Send email alerts on key events via Kafka |
| Security | JWT-based authentication, Role-Based Access Control (RBAC) |
| Observability | Distributed tracing, metrics dashboards, alerting |

### Who uses the system?

| Role | What they can do |
|---|---|
| `admin` | Full access to everything — manage users, clients, billing, support |
| `account_manager` | Create and manage clients, contracts, invoices, payments |
| `support_agent` | View client data, manage and respond to support tickets |
| `client` | Create and view their own support tickets |

### Overall Architecture (simple version)

```
[Postman / Browser / React Frontend]
              |
              | HTTPS on port 8090
              v
    [API Gateway — Spring Cloud Gateway]
         |         |         |
         v         v         v
  [Client   ] [Account  ] [Billing  ]  [Notification]
  [Service  ] [Service  ] [Service  ]  [Service     ]
  [:8081    ] [:8082    ] [:8083    ]  [:8084       ]
         |         |         |               |
         +----+----+----+----+               |
              |                             |
         [MySQL DB]  [Redis Cache]    [Kafka Events]
              |                             |
         [MinIO S3]              [Notification listens]
```

### Technologies Used

| Technology | Purpose | Why chosen |
|---|---|---|
| Java 21 | Primary language | Industry standard for enterprise SDE roles |
| Spring Boot 3.3 | Application framework | Auto-configuration, production-grade features |
| Spring Cloud Gateway | API Gateway | Reactive routing, rate limiting, JWT validation |
| Keycloak 24 | Authentication | OAuth2/OIDC, JWT, RBAC out of the box |
| MySQL 8 | Relational database | Per-service schema isolation |
| Redis | Cache | Cache-aside pattern, session/profile caching |
| Apache Kafka 3.7 | Async messaging | Decouples services; event-driven architecture |
| MinIO | Object storage | Stores generated PDF contracts/invoices (S3-compatible) |
| Flyway | DB schema migrations | Version-controlled, repeatable schema changes |
| MapStruct | Object mapping | Zero-runtime-cost DTO to Entity conversion |
| Prometheus + Grafana | Metrics and dashboards | Health monitoring across all services |
| Jaeger | Distributed tracing | Traces a request as it travels across services |
| Docker and Docker Compose | Containerization | Run all infrastructure with one command |
| Maven (multi-module) | Build tool | One repo, separate module per service |
| GitHub Actions | CI/CD | Automated build, test, and image push |

---

## 2. Complete Project Architecture

### 2.1 Request Flow (from Postman to Database and back)

```
Step 1:  You send POST /api/v1/clients from Postman to port 8090.
Step 2:  API Gateway receives the request and checks the JWT token.
Step 3:  API Gateway validates the token using Keycloak's public keys.
Step 4:  If valid, Gateway routes the request to Client Service on port 8081.
Step 5:  Client Service checks Redis to prevent duplicate email creation.
Step 6:  Client Service calls Account Service (port 8082) to create an account.
Step 7:  Client Service persists the new client to MySQL (cms_client schema).
Step 8:  Client Service publishes a CLIENT_ONBOARDED event to Kafka.
Step 9:  Notification Service consumes the event and sends a welcome email.
Step 10: Client Service returns 201 Created with client details.
```

### 2.2 Component Details

#### API Gateway (port 8090)
- **What it is:** The single entry point for all external requests.
- **Responsibility:** Route requests to the correct service, validate JWT, apply rate limiting (20 req/s per user).
- **Communicates with:** All backend services via HTTP.

#### Keycloak (port 8080)
- **What it is:** An open-source Identity and Access Management (IAM) server.
- **Responsibility:** Issues JWT access tokens after validating username and password. Manages realms, users, and roles.

#### Client Service (port 8081)
- **What it is:** The core service for managing business clients.
- **Responsibility:** CRUD for clients, contacts, addresses, and support tickets. Caches client profiles in Redis.
- **Communicates with:** Account Service (REST), Kafka (produces events), Redis (cache), MySQL (cms_client schema).

#### Account Service (port 8082)
- **What it is:** Manages business accounts in a multi-tenant B2B model.
- **Responsibility:** Creates accounts and links them to clients.

#### Billing Service (port 8083)
- **What it is:** Handles all financial operations.
- **Responsibility:** Creates contracts, generates PDF invoices and uploads to MinIO, processes payments via Stripe.
- **Communicates with:** MySQL (cms_billing schema), MinIO (PDF storage), Kafka (produces PAYMENT_SUCCESS/PAYMENT_FAILED).

#### Notification Service (port 8084)
- **What it is:** An event-driven email dispatcher.
- **Responsibility:** Listens to Kafka topics and sends emails for every key event.

#### MySQL (port 3306)
- Each service has its own isolated schema: `cms_client`, `cms_account`, `cms_billing`, `cms_notification`

#### Redis (port 6379)
- Stores client profiles (TTL 30 min), duplicate email checks, API Gateway rate limit counters.

#### Kafka + Zookeeper (ports 9092, 2181)
- **Topics:** `CLIENT_ONBOARDED`, `INVOICE_GENERATED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `TICKET_CREATED`

#### MinIO (port 9000 / 9001 console)
- Stores generated contract and invoice PDF files.

#### Observability Stack

| Tool | Port | Purpose |
|---|---|---|
| Jaeger | 16686 | Distributed trace viewer |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Metrics dashboards (admin/admin) |
| Alertmanager | 9093 | Alert routing |
| Mailhog | 8025 | Fake SMTP — catches all dev emails |

---

## 3. Project Folder Structure

```
Client Management System/
|
+-- .env                           <- Local secrets (git-ignored). NEVER commit this.
+-- .github/workflows/ci.yml       <- GitHub Actions CI/CD pipeline
+-- docker-compose.yml             <- Starts all infrastructure containers
+-- docker-compose.prod.yml        <- Production overrides
+-- pom.xml                        <- Root Maven POM — defines all modules
+-- build_and_import.ps1           <- Builds Docker images and imports to k3d
+-- start_all.ps1                  <- Starts all 5 Spring Boot services
+-- k3d-deploy.ps1                 <- Deploys to local Kubernetes cluster
|
+-- common/                        <- Shared library (DTOs, exceptions, Kafka events)
|
+-- api-gateway/                   <- Spring Cloud Gateway service
|   +-- src/main/java/.../gateway/
|       +-- config/                <- Security, rate limiting, route config
|
+-- client-service/                <- Client, Contact, Address, Support Ticket service
|   +-- src/main/java/.../client/
|   |   +-- controller/            <- REST endpoints
|   |   +-- service/               <- Business logic
|   |   +-- repository/            <- JPA repositories
|   |   +-- domain/entity/         <- JPA entity classes
|   |   +-- dto/                   <- Request/Response DTOs
|   |   +-- mapper/                <- MapStruct mappers (interface only)
|   |   +-- config/                <- SecurityConfig, RedisConfig
|   |   +-- filter/                <- UserSyncFilter
|   +-- src/main/resources/
|       +-- db/migration/          <- Flyway SQL scripts V1 through V8
|   +-- target/generated-sources/  <- AUTO-GENERATED. Do not edit manually.
|
+-- account-service/               <- Account management service
+-- billing-service/               <- Contracts, invoices, payments, MinIO
+-- notification-service/          <- Kafka consumer, email dispatching
|
+-- cms-admin/                     <- React + Vite frontend (TypeScript)
|   +-- src/                       <- React components, pages, API clients
|   +-- e2e/                       <- Playwright E2E browser tests
|   +-- playwright.config.ts       <- E2E test configuration
|   +-- package.json               <- Frontend dependencies
|
+-- docker/                        <- Infrastructure config files
|   +-- keycloak-provision.sh      <- Sets up Keycloak realm, users, and roles
|   +-- observability/
|       +-- prometheus.yml         <- Prometheus scrape configuration
|
+-- k8s/                           <- Kubernetes manifests
+-- helm/                          <- Helm chart for Kubernetes deployment
+-- postman/                       <- Postman collection files
    +-- ECMS_ClientMgmt_Phase2.postman_collection.json
    +-- ECMS_Phase6_Tickets.postman_collection.json
```

### Key rules about folders

| Folder | Can you edit it? | What happens if deleted? |
|---|---|---|
| `target/` | No — auto-generated by Maven | Recreated on next `mvn compile` |
| `target/generated-sources/` | No — generated by MapStruct | Recreated on next `mvn compile` |
| `src/main/resources/db/migration/` | Carefully — follow Flyway rules | Flyway may conflict on next start |
| `.env` | Yes — your configuration | Services will fail to start |
| `docker-compose.yml` | Yes — test changes carefully | Infrastructure may not start |

---

## 4. Installation and Initial Setup

### 4.1 Required Software

| Software | Version Required | Download |
|---|---|---|
| Java JDK | 21 or higher | https://adoptium.net/ |
| Apache Maven | 3.9+ | https://maven.apache.org/download.cgi |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop/ |
| Git | Latest | https://git-scm.com/downloads |
| Postman | Latest | https://www.postman.com/downloads/ |
| Node.js (for frontend) | 20+ | https://nodejs.org/ |

### 4.2 Verify Installations

```powershell
java -version          # Expected: openjdk version "21..."
mvn -version           # Expected: Apache Maven 3.9...
docker --version       # Expected: Docker version 27...
git --version          # Expected: git version 2...
node --version         # Expected: v20...
npm --version          # Expected: 10...
```

### 4.3 Clone the Repository

```powershell
cd C:\Users\<YourName>\Downloads
git clone https://github.com/srijan-deb/Client-Management-System.git
cd "Client Management System"
```

### 4.4 Configure Environment Variables

The `.env` file already exists with safe development defaults:

```
MYSQL_ROOT_PASSWORD=cms_root_pass
MYSQL_PASSWORD=cms_pass
MYSQL_USERNAME=cms_user
KEYCLOAK_ADMIN_PASSWORD=admin123
MINIO_ROOT_USER=cms_minio
MINIO_ROOT_PASSWORD=cms_minio_secret
MINIO_ACCESS_KEY=cms_minio
MINIO_SECRET_KEY=cms_minio_secret
STRIPE_API_KEY=sk_test_mock_1234567890
```

> **IMPORTANT:** Never commit the `.env` file to Git. It is already in `.gitignore`.

### 4.5 Build all Java Services

```powershell
# From the project root
mvn clean package -DskipTests
```

Expected output: `[INFO] BUILD SUCCESS` after ~2-3 minutes.

### 4.6 Install Frontend Dependencies

```powershell
cd cms-admin
npm install
cd ..
```

### 4.7 Start Infrastructure Containers

```powershell
docker compose up -d
```

Verify with `docker compose ps` — all containers should show `healthy` or `running`.

### 4.8 Provision Keycloak (First time only)

```powershell
# Requires Git Bash on Windows
bash docker/keycloak-provision.sh
```

Expected output:
```
Provisioning Complete
  Test users:
    testuser@cms.com / Test@1234   -> roles: admin, support_agent
    acctmgr@cms.com  / Test@1234   -> roles: account_manager
```

Verify: Open `http://localhost:8080/admin` — log in with `admin` / `admin123` — you should see a `cms` realm.

---

## 5. How to Start the Project Manually

Follow this exact order every time.

**Step 1: Start Docker Desktop**
Double-click the Docker Desktop icon. Wait until the icon stops animating.

**Step 2: Start all infrastructure containers**
```powershell
docker compose up -d
```
Wait 30-60 seconds for MySQL and Kafka to become healthy.

**Step 3: Verify infrastructure is healthy**
```powershell
docker compose ps
```
All important services must show `healthy`.

**Step 4: Start all backend microservices**
```powershell
.\start_all.ps1
```
Five new PowerShell windows open — one per service.

**Step 5: Wait for all services to fully boot**
In each window, watch for:
```
Started <ServiceName>Application in X seconds
```

**Step 6 (Optional): Start the React Frontend**
```powershell
cd cms-admin
npm run dev
```
Frontend available at `http://localhost:4200`.

**Step 7: Verify the system**

| Check | URL | Expected result |
|---|---|---|
| API Gateway health | `http://localhost:8090/actuator/health` | `{"status":"UP"}` |
| Client Service | `http://localhost:8081/actuator/health` | `{"status":"UP"}` |
| Keycloak | `http://localhost:8080/realms/cms` | JSON realm info |
| Grafana | `http://localhost:3000` | Login page |
| Jaeger | `http://localhost:16686` | Traces dashboard |
| Mailhog | `http://localhost:8025` | Email inbox UI |

---

## 6. How to Stop the Project

**Step 1: Stop the React frontend**
In the `npm run dev` terminal, press **Ctrl + C**.

**Step 2: Stop the Spring Boot services**
In each of the 5 service PowerShell windows, press **Ctrl + C**.

> Do NOT force-close the windows without Ctrl + C — this may leave orphan Java processes on the ports.

**Step 3: Verify no Java processes remain**
```powershell
netstat -ano | findstr ":808"
```
If a process is still running, kill it:
```powershell
taskkill /PID <PID_NUMBER> /F
```

**Step 4: Stop Docker infrastructure**
```powershell
docker compose down
```
Data in volumes is preserved.

**Confirm everything is stopped:**
```powershell
docker compose ps       # Should show no running containers
netstat -ano | findstr ":8081"   # Should return nothing
```

---

## 7. Complete Manual Operation Guide

### 7.1 Get an Authentication Token

Before calling any API, you need a JWT token.

In Postman, create a new `POST` request:
- URL: `http://localhost:8080/realms/cms/protocol/openid-connect/token`
- Body: `x-www-form-urlencoded`

| Key | Value |
|---|---|
| `grant_type` | `password` |
| `client_id` | `cms-backend` |
| `client_secret` | `cms-backend-secret-dev` |
| `username` | `testuser` |
| `password` | `Test@1234` |

Click **Send**. Copy the `access_token` from the response.

### 7.2 Create a Client

**POST** `http://localhost:8090/api/v1/clients`
Authorization: Bearer Token (paste your token)
Body (JSON):
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "+1-555-0100",
  "companyName": "Acme Corp",
  "tier": "PREMIUM",
  "contacts": [
    {
      "contactName": "Jane Doe",
      "contactEmail": "jane@acme.com",
      "contactPhone": "+1-555-0101",
      "role": "PRIMARY"
    }
  ],
  "addresses": [
    {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "postalCode": "10001",
      "country": "USA",
      "primary": true
    }
  ]
}
```
Expected result: `201 Created` with client data including a `clientId`.

### 7.3 View a Client

`GET http://localhost:8090/api/v1/clients/{clientId}` — Replace `{clientId}` with the ID from the create response.

### 7.4 Search / List Clients

- `GET http://localhost:8090/api/v1/clients` — returns all clients (paginated, 20 per page)
- `GET http://localhost:8090/api/v1/clients?search=john` — searches by name and email

### 7.5 Update a Client

`PUT http://localhost:8090/api/v1/clients/{clientId}` with:
```json
{
  "phone": "+1-555-9999",
  "tier": "ENTERPRISE"
}
```

### 7.6 Create a Support Ticket

`POST http://localhost:8090/api/v1/tickets`
```json
{
  "clientId": 1,
  "subject": "Cannot access invoice PDF",
  "description": "The download link for invoice INV-2026-001 returns a 403 error.",
  "priority": "HIGH"
}
```
Expected: `201 Created`. Check Mailhog at `http://localhost:8025` for a notification email.

### 7.7 Manage a Support Ticket

| Action | Endpoint | Required Role |
|---|---|---|
| Assign to agent | `PUT /api/v1/tickets/{id}/assign?agentId=2` | admin, support_agent |
| Resolve | `PUT /api/v1/tickets/{id}/resolve` | admin, support_agent |
| Reopen | `PUT /api/v1/tickets/{id}/reopen` | client, admin, support_agent |
| Close | `PUT /api/v1/tickets/{id}/close` | admin, support_agent |
| Add comment | `POST /api/v1/tickets/{id}/comments` with `{"comment":"Working on it"}` | All roles |

### 7.8 Create a Contract (Billing)

`POST http://localhost:8090/api/v1/billing/contracts`
```json
{
  "clientId": 1,
  "accountId": 1,
  "startDate": "2026-09-01",
  "endDate": "2027-08-31",
  "productIds": [1, 2]
}
```
Expected: `201 Created`. A PDF is generated and stored in MinIO.

### 7.9 Process a Payment

`POST http://localhost:8090/api/v1/billing/payments`
```json
{
  "invoiceId": 1,
  "amount": 1500.00,
  "currency": "USD",
  "paymentMethod": "STRIPE"
}
```
Expected: `200 OK` with `"status": "PAID"`.

### 7.10 Observability Tools

| Tool | URL | Login |
|---|---|---|
| Jaeger Traces | http://localhost:16686 | No login needed |
| Grafana Dashboards | http://localhost:3000 | admin / admin |
| Prometheus Metrics | http://localhost:9090 | No login needed |
| Mailhog (emails) | http://localhost:8025 | No login needed |

---

## 8. Authentication and Authorization

### Key Concepts

| Term | Explanation |
|---|---|
| **Realm** | A Keycloak namespace holding all users, roles, and clients. This project uses the `cms` realm. |
| **Client (Keycloak)** | A registered application that can request tokens. This project uses `cms-backend`. |
| **User** | A person with login credentials in Keycloak. |
| **Role** | A label assigned to a user: `admin`, `account_manager`, `support_agent`, `client`. |
| **JWT** | JSON Web Token — a signed digital pass proving who you are and what roles you have. |
| **Access Token** | A short-lived JWT (1 hour) included in every API request. |
| **Refresh Token** | A longer-lived token used to get a new access token without logging in again. |
| **JWKS** | JSON Web Key Set — Keycloak's public keys used by services to verify JWTs without calling Keycloak. |

### Login Flow

```
1. You send username + password to Keycloak
2. Keycloak validates credentials
3. Keycloak creates a JWT with your user ID, email, and roles
4. Keycloak returns the JWT with a 1-hour expiry
5. You include the JWT in the Authorization header of every request
6. API Gateway validates the JWT signature using Keycloak's cached public keys
7. If valid, Gateway passes the request to the backend service
8. Backend checks @PreAuthorize role annotations against JWT roles
9. If authorized, business logic runs and returns the response
```

### How to manually create a Keycloak user

1. Go to `http://localhost:8080/admin`
2. Log in: `admin` / `admin123`
3. Select the `cms` realm (top-left dropdown)
4. Go to **Users** → **Create new user**
5. Fill in username, email, first name, last name → **Create**
6. Go to the **Credentials** tab → Set password → disable "Temporary"
7. Go to the **Role mapping** tab → **Assign role** → pick a role

### How roles are enforced

```java
@PostMapping
@PreAuthorize("hasAnyRole('admin', 'account_manager')")
public ResponseEntity<ClientResponse> createClient(...) { ... }
```

If a user with role `support_agent` tries to POST to `/api/v1/clients`, the response is `403 Forbidden`.

---

## 9. API Documentation

### Base URL
All requests go through the API Gateway: `http://localhost:8090`

### Common Headers
```
Authorization: Bearer <your_access_token>
Content-Type: application/json
```

### Common Error Responses

| HTTP Status | Meaning |
|---|---|
| `400 Bad Request` | Validation failed — check `fieldErrors` in response body |
| `401 Unauthorized` | Missing or expired token — get a new token |
| `403 Forbidden` | Your role doesn't have permission |
| `404 Not Found` | Resource does not exist |
| `429 Too Many Requests` | Rate limit exceeded — wait and retry |
| `500 Internal Server Error` | Server error — check service logs |

---

### Client Service APIs

#### POST /api/v1/clients — Create a client
- **Roles:** `admin`, `account_manager`
- **Success:** `201 Created` + Location header + client object

#### GET /api/v1/clients/{id} — Get a client by ID
- **Roles:** `admin`, `account_manager`, `support_agent`
- **Success:** `200 OK` + full client object

#### GET /api/v1/clients — Search / list clients
- **Roles:** `admin`, `account_manager`, `support_agent`
- **Query params:** `search` (optional), `page`, `size`, `sort`
- **Success:** `200 OK` + paginated list

#### PUT /api/v1/clients/{id} — Update a client
- **Roles:** `admin`, `account_manager`
- **Success:** `200 OK` + updated client object

#### GET /api/v1/clients/{id}/activity — Get activity logs
- **Roles:** `admin`, `account_manager`, `support_agent`

---

### Support Ticket APIs

| Method | URL | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/tickets` | All roles | Create a ticket |
| `GET` | `/api/v1/tickets/{id}` | All roles | Get ticket by ID |
| `GET` | `/api/v1/tickets?clientId=1` | All roles | List tickets by client |
| `PUT` | `/api/v1/tickets/{id}/assign?agentId=2` | admin, support_agent | Assign ticket |
| `PUT` | `/api/v1/tickets/{id}/resolve` | admin, support_agent | Resolve ticket |
| `PUT` | `/api/v1/tickets/{id}/reopen` | client, admin, support_agent | Reopen ticket |
| `PUT` | `/api/v1/tickets/{id}/close` | admin, support_agent | Close ticket |
| `POST` | `/api/v1/tickets/{id}/comments` | All roles | Add a comment |

---

### Billing Service APIs

| Method | URL | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/billing/contracts` | admin, account_manager | Create a contract (generates PDF) |
| `POST` | `/api/v1/billing/payments` | admin, account_manager | Process a payment |

---

## 10. Database Documentation

### Schema Overview

| Schema | Owner Service | Main Purpose |
|---|---|---|
| `cms_client` | Client Service | Clients, contacts, addresses, support tickets, activity logs |
| `cms_account` | Account Service | Accounts linked to clients |
| `cms_billing` | Billing Service | Products, contracts, invoices, payments, subscriptions |
| `cms_notification` | Notification Service | Notification templates and delivery history |

### Key Tables in cms_client

#### clients table

| Column | Type | Description |
|---|---|---|
| `client_id` | BIGINT (PK) | Auto-increment unique identifier |
| `account_id` | BIGINT | FK to Account Service (nullable until provisioned) |
| `first_name`, `last_name` | VARCHAR(100) | Client name |
| `email` | VARCHAR(255) UNIQUE | Must be unique |
| `tier` | ENUM | `STANDARD`, `PREMIUM`, `ENTERPRISE` |
| `status` | ENUM | `ACTIVE`, `INACTIVE`, `SUSPENDED` |
| `created_by` | BIGINT | FK to users.user_id |
| `created_at`, `updated_at` | DATETIME | Auto-managed timestamps |

#### contacts table
Stores contacts for a client. FK: `client_id → clients.client_id`.

#### addresses table
Stores addresses for a client. FK: `client_id → clients.client_id`.

#### support_tickets table
Stores client support tickets. FK: `client_id → clients.client_id`.

#### activity_logs table
Audit trail — every create/update/delete action writes a record with who did it and when.

### Key Tables in cms_billing

#### contracts table

| Column | Description |
|---|---|
| `id` | Primary key |
| `client_id`, `account_id` | Which client/account |
| `total_value` | Computed total of all products |
| `pdf_url` | URL of generated PDF in MinIO |
| `start_date`, `end_date` | Contract validity period |

#### invoices table
Generated from contracts. Contains `invoice_number`, `subtotal`, `tax_amount`, `total_amount`. FK to `contracts`.

#### payments table
Records each payment attempt. Stores `status` (`PAID` / `FAILED`) and payment method.

### Schema Migrations (Flyway)

Files in `src/main/resources/db/migration/`:

```
V1__init.sql                              -> Initial schema / users table
V2__create_users_projection.sql           -> Local Keycloak user projection
V3__create_clients.sql                    -> clients table
V4__create_contacts.sql                   -> contacts table
V5__create_addresses.sql                  -> addresses table
V6__create_activity_logs.sql              -> activity_logs table
V7__create_support_tickets.sql            -> support_tickets table
V8__add_resolved_and_closed_at...sql      -> Incremental column additions
```

> **Rule:** Never rename or edit an already-applied Flyway file. Only add new ones (V9, V10, etc.).

---

## 11. Docker Documentation

### Key Files

| File | Purpose |
|---|---|
| `docker-compose.yml` | Defines all dev infrastructure containers |
| `docker-compose.prod.yml` | Production overrides |
| `<service>/Dockerfile` | How to build each Spring Boot service |

### Container Reference

| Container | Port(s) | Description |
|---|---|---|
| `cms-mysql` | 3306 | MySQL 8 database server |
| `cms-redis` | 6379 | Redis cache |
| `cms-zookeeper` | 2181 | Kafka coordinator |
| `cms-kafka` | 9092, 29092 | Event streaming |
| `cms-keycloak` | 8080 | Authentication server |
| `cms-minio` | 9000, 9001 | Object storage |
| `cms-mailhog` | 1025, 8025 | Fake email server |
| `cms-prometheus` | 9090 | Metrics collection |
| `cms-grafana` | 3000 | Metrics dashboards |
| `cms-jaeger` | 16686, 4317 | Distributed tracing |
| `cms-alertmanager` | 9093 | Alert routing |

### Essential Docker Commands

```powershell
# Start all containers
docker compose up -d

# Start only specific containers
docker compose up -d cms-mysql cms-kafka

# Stop all containers (preserves data)
docker compose down

# Stop and DELETE all data volumes — DANGER
docker compose down -v

# View logs for a container
docker compose logs cms-mysql
docker compose logs -f cms-kafka       # -f = follow/stream

# View status of all containers
docker compose ps

# Restart a single container
docker compose restart cms-keycloak

# Enter a running container shell
docker exec -it cms-mysql bash
docker exec -it cms-mysql mysql -u cms_user -pcms_pass cms_client

# Build a single service image
docker build -t ghcr.io/srijan-deb/cms-client-service:latest ./client-service

# Build all images and import to k3d
.\build_and_import.ps1

# View all local images
docker images

# Check resource usage
docker stats
```

### Troubleshooting unhealthy containers

```powershell
# See why a container is unhealthy
docker inspect cms-mysql

# View full container logs
docker compose logs --tail=100 cms-kafka

# Force recreate a container
docker compose up -d --force-recreate cms-mysql
```

---

## 12. Git and GitHub Workflow

### Initial setup

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

### Daily workflow

```powershell
# 1. Get the latest changes
git pull origin main

# 2. Create a feature branch
git checkout -b feature/add-search-filter

# 3. Make your changes...

# 4. See what changed
git status
git diff

# 5. Stage your changes
git add .
git add path/to/specific/file.java

# 6. Commit
git commit -m "feat(client): add search filter by company name"

# 7. Push to GitHub
git push origin feature/add-search-filter

# 8. Open a Pull Request on GitHub

# 9. After merge, clean up
git checkout main
git pull origin main
git branch -d feature/add-search-filter
```

### Commit message convention

```
<type>(<scope>): <description>

Types: feat, fix, docs, refactor, test, chore
Examples:
  feat(billing): add PDF generation for invoices
  fix(client): prevent duplicate email registration
  docs: update README with k8s deployment steps
```

### Resolving merge conflicts

```powershell
git pull origin main         # Reports conflicts
# Edit the conflicted file — look for <<<<<<, =======, >>>>>>>
# Keep the correct version
git add <conflicted-file>
git commit -m "fix: resolve merge conflict in ClientService"
```

---

## 13. CI/CD Pipeline

Defined in `.github/workflows/ci.yml`.

### What triggers the pipeline
- Any **push** to the `main` branch
- Any **pull request** targeting `main`

### Pipeline Stages

```
Push to main / PR to main
        |
        v
[1. test] — Run all tests using Testcontainers
    - Spins up real MySQL + Kafka in Docker
    - Runs JUnit 5 unit and integration tests
        |
        v
[2. sonar] — SonarQube code quality analysis
    - Checks code coverage, bugs, code smells
    - Quality gate failure = pipeline failure
        |
        v
[3. build-push] — (push to main only, not PRs)
    - mvn spring-boot:build-image (Paketo Buildpacks)
    - Pushes all 5 images to GHCR:
        ghcr.io/srijan-deb/cms-api-gateway:latest
        ghcr.io/srijan-deb/cms-client-service:latest
        (and 3 more)
```

### Required GitHub Secrets

Go to GitHub repo → **Settings** → **Secrets and variables** → **Actions**:

| Secret Name | Value |
|---|---|
| `SONAR_TOKEN` | From sonarcloud.io → My Account → Security |
| `SONAR_HOST_URL` | `https://sonarcloud.io` |
| `GITHUB_TOKEN` | Auto-injected by GitHub Actions — no setup needed |

### How to verify the pipeline

1. Go to `https://github.com/srijan-deb/Client-Management-System/actions`
2. Click the latest workflow run
3. Green check = passed, Red X = failed
4. Click a failed stage to see the full log

---

## 14. Postman Testing Guide

### Step 1: Import collections

1. Open Postman → **Import**
2. Import both files from the `postman/` folder

### Step 2: Create a Postman Environment

1. Click **Environments** → **Create Environment** → name it `CMS Local Dev`
2. Add these variables:

| Variable | Initial Value |
|---|---|
| `base_url` | `http://localhost:8090` |
| `keycloak_url` | `http://localhost:8080` |
| `jwt_token` | *(leave blank)* |
| `client_id` | *(leave blank)* |

3. Select `CMS Local Dev` from the environment dropdown.

### Step 3: Create the "Get Token" request

1. New request: `POST {{keycloak_url}}/realms/cms/protocol/openid-connect/token`
2. Body → `x-www-form-urlencoded`:

| Key | Value |
|---|---|
| `grant_type` | `password` |
| `client_id` | `cms-backend` |
| `client_secret` | `cms-backend-secret-dev` |
| `username` | `testuser` |
| `password` | `Test@1234` |

3. **Scripts** → **Post-response**:
```javascript
var json = pm.response.json();
pm.environment.set("jwt_token", json.access_token);
console.log("Token saved! Expires in:", json.expires_in, "seconds");
```

4. Click **Send** — token is saved automatically.

### Step 4: Configure the collection to use the token

1. Click the collection root
2. **Authorization** tab → Type: **Bearer Token** → Token: `{{jwt_token}}`

### Step 5: Test in this order

1. **Get Token** (saves jwt_token)
2. **POST /api/v1/clients** (save clientId to environment)
3. **GET /api/v1/clients/{{client_id}}**
4. **GET /api/v1/clients?search=John**
5. **PUT /api/v1/clients/{{client_id}}**
6. **POST /api/v1/tickets**

### Expected status codes

| Operation | Expected Status |
|---|---|
| Create (POST) | `201 Created` |
| Read (GET) | `200 OK` |
| Update (PUT) | `200 OK` |
| Validation error | `400 Bad Request` |
| Wrong role | `403 Forbidden` |
| Resource not found | `404 Not Found` |

---

## 15. Troubleshooting Guide

### Docker is not running
**Problem:** `docker compose up` fails immediately.
**Solution:** Start Docker Desktop. Wait for the icon to stop animating.
**Verify:** `docker version`

---

### Port already in use
**Problem:** `Port 8081 is already in use`
**Check:** `netstat -ano | findstr ":8081"`
**Solution:** `taskkill /PID <PID> /F`

---

### Container is not starting / unhealthy
**Problem:** `docker compose ps` shows unhealthy
**Check:** `docker compose logs cms-mysql`
**Solutions:**
- MySQL: Verify `MYSQL_ROOT_PASSWORD` in `.env`
- Kafka: Needs Zookeeper healthy first — wait 30 seconds
- Keycloak: Needs MySQL healthy — wait longer

---

### 401 Unauthorized
**Cause:** Token missing or expired (1 hour lifetime)
**Solution:** Run the "Get Token" request again.

---

### 403 Forbidden
**Cause:** User lacks the required role.
**Solution:**
1. Decode token at https://jwt.io — check `realm_access.roles`
2. Ensure correct role is assigned in Keycloak admin console
3. Switch to `testuser` which has `admin` role

---

### Backend service not starting
**Check:** Scroll up in the service window for the error.
**Common causes:**
- `Connection refused to MySQL` — MySQL not healthy yet. Wait and restart service.
- `Connection refused to Redis` — Same for Redis.
- Port already in use — see port conflict section.

---

### Maven build failure
**Common causes:**
- Wrong Java: `java -version` must show 21
- Test failure: use `mvn clean package -DskipTests`

---

### Frontend not loading
**Check:** Is `npm run dev` still running?
**Fix:** `cd cms-admin && npm install && npm run dev`

---

### Kafka events not processed (no notification emails)
**Check:**
```powershell
docker compose logs cms-kafka
docker compose logs notification-service
```
Look for `Consumer group ... subscribed to topic CLIENT_ONBOARDED`

---

## 16. Logs and Monitoring

### View infrastructure logs

```powershell
docker compose logs -f cms-mysql
docker compose logs -f cms-kafka
docker compose logs --tail=200 cms-keycloak
```

### Spring Boot service logs

Logs are printed directly in the PowerShell service windows. Format:
```
2026-08-25 08:15:32 [thread] INFO  ClassName - message
```

### Using Jaeger for traces

1. Open `http://localhost:16686`
2. Select a service from the dropdown
3. Click **Find Traces**
4. Click a trace to see the full request journey across services

### Using Grafana

1. Open `http://localhost:3000` — log in `admin` / `admin`
2. Go to **Dashboards** — see HTTP rates, error rates, JVM memory

### Prometheus queries

Open `http://localhost:9090`. Example query:
```
http_server_requests_seconds_count{status="200"}
```

---

## 17. Backup and Recovery

### Backup the MySQL database

```powershell
docker exec cms-mysql mysqldump -u cms_user -pcms_pass --databases cms_client cms_account cms_billing cms_notification > backup.sql
```

### Restore the database

```powershell
Get-Content backup.sql | docker exec -i cms-mysql mysql -u cms_user -pcms_pass
```

### Files to back up

- `.env` — local secrets
- `docker-compose.yml` — infrastructure config
- `src/main/resources/db/migration/*.sql` — schema history (already in Git)

### Recover from service failure

```powershell
docker compose restart cms-mysql
```
Then restart the affected Spring Boot service from `start_all.ps1`.

### Recover deleted `target/` folder

```powershell
mvn clean package -DskipTests   # Maven regenerates everything
```

---

## 18. Development Workflow

```
1. git pull origin main            -> Get latest code
2. git checkout -b feature/xyz    -> Create feature branch
3. Edit source code                -> Make changes
4. mvn clean compile               -> Fast compile check
5. mvn test                        -> Run unit tests
6. .\start_all.ps1                 -> Start services locally
7. Postman testing                 -> Test API changes manually
8. docker compose logs -f <svc>   -> Check for errors
9. git add . && git commit         -> Commit work
10. git push origin feature/xyz    -> Push to GitHub
11. Create PR on GitHub            -> CI/CD runs automatically
12. Merge PR                       -> Images pushed to GHCR
```

---

## 19. Production / Deployment Guide

### Local dev vs. Production

| Setting | Local Dev | Production |
|---|---|---|
| Database | Single MySQL | MySQL Primary + Read Replicas |
| Kafka | 1 broker | 3 brokers, partitioned topics |
| Redis | Single instance | Sentinel + Replica |
| Orchestration | Docker Compose | Kubernetes (k3d / k3s) |
| Secrets | `.env` file | Kubernetes Secrets / Vault |

### Deploy to Kubernetes (k3d)

```powershell
# 1. Build and import all images
.\build_and_import.ps1

# 2. Deploy with Helm
kubectl create namespace cms
helm install cms ./helm/cms `
  --namespace cms `
  --set secrets.mysqlPassword=cms_pass `
  --set secrets.minioSecretKey=cms_minio_secret `
  --set secrets.stripeApiKey=sk_test_mock_1234567890

# 3. Verify pods
kubectl get pods -n cms

# 4. Upgrade after code change
.\build_and_import.ps1
helm upgrade cms ./helm/cms --namespace cms
```

### Production Docker Compose

```powershell
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Production security checklist

- [ ] Replace all default passwords in `.env` with strong random values
- [ ] Set `KEYCLOAK_ADMIN_PASSWORD` to something secure
- [ ] Use real Stripe live key for `STRIPE_API_KEY`
- [ ] Configure HTTPS/TLS on the API Gateway
- [ ] Set Redis `requirepass`
- [ ] Restrict MySQL to service containers only

---

## 20. Daily Operation Checklist

**Starting the project:**
- [ ] Start Docker Desktop — wait for it to be ready
- [ ] Run `docker compose up -d`
- [ ] Run `docker compose ps` — verify all containers healthy
- [ ] Verify Keycloak at `http://localhost:8080`
- [ ] Run `.\start_all.ps1`
- [ ] Wait for all 5 services to log "Started ...Application"
- [ ] Verify Gateway: `curl http://localhost:8090/actuator/health`
- [ ] (Optional) Start frontend: `cd cms-admin && npm run dev`

**Testing:**
- [ ] Get a fresh token from Keycloak in Postman
- [ ] Run test requests

**Monitoring:**
- [ ] Check Mailhog: `http://localhost:8025`
- [ ] Check Grafana: `http://localhost:3000`
- [ ] Check Jaeger: `http://localhost:16686`

**Stopping the project:**
- [ ] Press Ctrl+C in all 5 service PowerShell windows
- [ ] Press Ctrl+C in the npm dev window
- [ ] Run `docker compose down`
- [ ] (Optional) Quit Docker Desktop

---

## 21. Commands Cheat Sheet

### Docker

```powershell
docker compose up -d                        # Start all containers
docker compose down                         # Stop containers (keep data)
docker compose down -v                      # Stop + erase all data
docker compose ps                           # Container status
docker compose logs -f <name>              # Stream container logs
docker compose restart <name>              # Restart one container
docker exec -it cms-mysql bash             # Shell in MySQL container
docker images                               # List local images
docker stats                                # Live resource usage
```

### Git

```powershell
git clone <url>                             # Clone repository
git pull origin main                        # Get latest changes
git checkout -b feature/xyz                 # New feature branch
git status                                  # What changed
git diff                                    # Exact changes
git add .                                   # Stage all changes
git commit -m "feat: my change"             # Commit
git push origin feature/xyz                 # Push to GitHub
git branch -d feature/xyz                   # Delete local branch
```

### Maven

```powershell
mvn clean package -DskipTests              # Build all (fast)
mvn clean package                          # Build + run tests
mvn clean compile                          # Compile only
mvn test                                   # Run tests
mvn spring-boot:run                        # Run from service directory
mvn verify -Pcoverage                      # Coverage report
```

### npm / Frontend

```powershell
npm install                                 # Install dependencies
npm run dev                                 # Start dev server
npm run build                              # Production build
npm test                                   # Unit tests
npx playwright test                        # E2E tests
```

### Database

```powershell
# Connect to MySQL
docker exec -it cms-mysql mysql -u cms_user -pcms_pass

# MySQL commands (inside the prompt)
# SHOW DATABASES;
# USE cms_client;
# SHOW TABLES;
# SELECT * FROM clients LIMIT 5;

# Backup
docker exec cms-mysql mysqldump -u cms_user -pcms_pass cms_client > backup.sql

# Restore
Get-Content backup.sql | docker exec -i cms-mysql mysql -u cms_user -pcms_pass
```

### Keycloak

```powershell
# Provision Keycloak (first time)
bash docker/keycloak-provision.sh

# Get a token via curl
curl -X POST http://localhost:8080/realms/cms/protocol/openid-connect/token `
  -d "grant_type=password&client_id=cms-backend&client_secret=cms-backend-secret-dev&username=testuser&password=Test@1234"
```

### Build and Deploy

```powershell
.\build_and_import.ps1                      # Build images + import to k3d
.\start_all.ps1                             # Start all Spring Boot services
.\k3d-deploy.ps1                            # Deploy to k3d Kubernetes
helm upgrade cms ./helm/cms --namespace cms # Upgrade Kubernetes deployment
kubectl get pods -n cms                     # View pod status
kubectl logs -n cms <pod-name>              # View pod logs
```

### Troubleshooting

```powershell
netstat -ano | findstr ":8081"              # Find what's using a port
taskkill /PID <PID> /F                      # Kill a process by PID
Get-Process java                            # All running Java processes
docker inspect <container>                  # Full container details
kubectl describe pod -n cms <pod-name>     # Kubernetes pod details
```

---

## 22. Complete End-to-End Example

This section walks through a full real-world workflow from starting your computer to stopping the project, explaining what happens internally at every stage.

---

### Step 1: Start the computer and Docker Desktop

Double-click the Docker Desktop icon. Wait for the whale icon to stop animating.

**Internally:** Docker Engine starts. All previously created data volumes (MySQL, MinIO, Redis) are still intact from your last session.

---

### Step 2: Start all infrastructure

```powershell
cd "C:\Users\dell\Downloads\Client Management System"
docker compose up -d
```

**Internally:**
- Docker reads `docker-compose.yml`
- MySQL, Redis, Zookeeper, Kafka, Keycloak, MinIO, Mailhog, Prometheus, Grafana, Jaeger all start
- Health checks run — Kafka waits for Zookeeper, Keycloak waits for MySQL
- All containers reach healthy status in ~45 seconds

Verify: `docker compose ps` — all green.

---

### Step 3: Start the Spring Boot microservices

```powershell
.\start_all.ps1
```

**Internally:**
- 5 PowerShell windows open
- Each runs `mvn spring-boot:run` with the `local` profile
- Spring Boot loads config from `application.yml` and environment variables from `.env`
- Each service connects to MySQL and runs Flyway migrations (tables already exist from before — Flyway skips)
- Each service connects to Redis, registers a cache manager
- Client, Account, Billing Services register as Kafka producers
- Notification Service subscribes to Kafka topics as a consumer
- All services register Prometheus metrics endpoints

Wait for: `Started <ServiceName>Application in X seconds` in all 5 windows.

---

### Step 4: Get an authentication token

In Postman, send:
```
POST http://localhost:8080/realms/cms/protocol/openid-connect/token
Body: grant_type=password, client_id=cms-backend, client_secret=cms-backend-secret-dev
      username=testuser, password=Test@1234
```

**Internally:**
- Keycloak checks the `testuser` credentials against its internal store
- Keycloak finds the user has roles: `admin`, `support_agent`
- Keycloak signs a JWT using RS256 with its private key
- JWT payload: `sub=<user-id>`, `preferred_username=testuser`, `realm_access.roles=["admin","support_agent"]`
- Token returned with 3600 second (1 hour) expiry

Copy the `access_token`. Set it as Bearer Token in Postman.

---

### Step 5: Create a client

```
POST http://localhost:8090/api/v1/clients
Authorization: Bearer <token>
Body: { "firstName": "Alice", "lastName": "Chen", "email": "alice@techcorp.com",
        "companyName": "TechCorp", "tier": "ENTERPRISE", ... }
```

**Internally:**
1. API Gateway (port 8090) receives request
2. Gateway fetches Keycloak's JWKS public keys (cached in memory), verifies JWT signature
3. JWT valid — Gateway routes to Client Service (port 8081)
4. `@PreAuthorize("hasAnyRole('admin','account_manager')")` passes
5. Client Service checks Redis: `cms:email:alice@techcorp.com` — cache miss (safe to proceed)
6. Client Service calls Account Service: `POST http://account-service:8082/api/v1/accounts`
7. Account Service inserts a new account in `cms_account` DB, returns `accountId=1`
8. Client Service inserts the client in `cms_client.clients` with `account_id=1`
9. Client Service writes to `activity_logs`: "CLIENT_CREATED by testuser"
10. Client Service caches the client in Redis: `cms:client:1` with 30-minute TTL
11. Client Service publishes `CLIENT_ONBOARDED` event to Kafka
12. Notification Service consumes the event, sends welcome email via Mailhog
13. Client Service returns `201 Created` with the full client JSON

**Check Mailhog** at `http://localhost:8025` — welcome email is there!

---

### Step 6: Create a contract and invoice

```
POST http://localhost:8090/api/v1/billing/contracts
Body: { "clientId": 1, "accountId": 1, "startDate": "2026-09-01",
        "endDate": "2027-08-31", "productIds": [1] }
```

**Internally:**
1. Billing Service creates a `contracts` record in `cms_billing`
2. Billing Service generates an invoice with a unique invoice number
3. Billing Service creates a PDF using OpenPDF
4. PDF is uploaded to MinIO bucket `cms-contracts`
5. MinIO object URL stored as `pdf_url` in contracts table
6. `INVOICE_GENERATED` Kafka event published
7. Notification Service sends "Invoice Ready" email

**Check MinIO** console at `http://localhost:9001` — you can see the PDF!

---

### Step 7: Process a payment

```
POST http://localhost:8090/api/v1/billing/payments
Body: { "invoiceId": 1, "amount": 5000.00, "currency": "USD", "paymentMethod": "STRIPE" }
```

**Internally:**
1. Billing Service calls Stripe API in test mode (no real money moves)
2. Stripe returns success
3. Invoice `status` updated to `PAID`
4. A `payments` record created
5. `PAYMENT_SUCCESS` Kafka event published
6. Notification Service sends "Payment Confirmed" email

Response: `200 OK` with `"status": "PAID"`

---

### Step 8: View the distributed trace

1. Open `http://localhost:16686` (Jaeger)
2. Select service: `client-service`
3. Click **Find Traces** — see the `POST /api/v1/clients` trace
4. Click it — see spans for each hop: `api-gateway → client-service → account-service`

---

### Step 9: Check the database

```powershell
docker exec -it cms-mysql mysql -u cms_user -pcms_pass
```

In MySQL:
```sql
USE cms_client;
SELECT client_id, first_name, email, tier, status FROM clients;
SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT 5;
USE cms_billing;
SELECT * FROM contracts;
SELECT * FROM payments;
```

---

### Step 10: Stop the project safely

```powershell
# 1. Ctrl+C in all 5 service windows
# 2. Ctrl+C in npm dev window (if started)

# 3. Stop infrastructure
docker compose down
```

**Internally:** All Spring Boot processes exit gracefully (Spring triggers shutdown hooks, releasing DB connections). Docker containers stop. Data in MySQL, MinIO, Redis volumes is preserved for next time.

---

*Documentation generated from actual project source code, configuration, and infrastructure — August 2026.*
