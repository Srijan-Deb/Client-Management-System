# CMS â€” Enterprise Client Management System
## End-to-End Build Roadmap (Java + MySQL core, zero-cost OSS stack)

> **Updated for v2 diagrams.** Changes from v1: ELK dropped from observability stack, Analytics/Reporting explicitly scoped to Phase 11 (dashed "stretch" boundary in the diagram), services labeled with their build phase, Consul deferred/optional, separate Keycloak MySQL added, and a clear split between the **dev build** (what you actually run day to day) and the **target/production architecture** (what v2 diagrams, with 3 Kafka brokers/k3s cluster/Redis Sentinel/MySQL replicas â€” see Â§2a).

---

## 1. Do the 4 diagrams align? (Quick audit)

Yes â€” they tell one consistent story, with a few gaps worth knowing about before you start coding:

| Diagram | What it defines | Consistent with the others? |
|---|---|---|
| **Flowchart** (business logic) | 7 flows: UserAccess, ClientMgmt, OpsFlow, ProductFlow, SupportFlow, PaymentFlow, BillingFlow | âœ… Matches the ERD tables (clients, products, subscriptions, contracts, invoices, payments, support_tickets) and the sequence diagram step-by-step |
| **Architecture** (zero-cost) | Client Layer â†’ Nginx â†’ Security (Kong/Keycloak) â†’ 6 Spring Boot microservices (Account, Notification, Client, Billing, **Analytics, Reporting**) â†’ Kafka/Redis/MySQL cluster â†’ Ops stack | âš ï¸ **Analytics Service, Reporting Service, OpenSearch, ClickHouse have no corresponding flow in the flowchart or sequence diagram.** They're clearly planned as a later phase (reporting/BI on top of the core system) â€” not a contradiction, just "not built yet." |
| **ERD** | 19 tables incl. users/roles (RBAC), accounts/clients (multi-tenant B2B model), contracts/subscriptions/invoices/payments (billing), support_tickets, audit_logs | âœ… Every table has a clear producer in the sequence diagram except `notification_templates` and `system_config`, which are supporting/config tables â€” expected to be seeded, not flow-driven |
| **Sequence diagram** | Concrete request path: Nginxâ†’Kongâ†’Keycloakâ†’(ClientSvc/BillingSvc/AccountSvc)â†’Kafkaâ†’NotifySvcâ†’MySQL/Redis/MinIO | âœ… Matches architecture's service names exactly (ClientSvc, AccountSvc, BillingSvc, NotifySvc) and matches flowchart steps almost 1:1 |

**One real gap to resolve now, not later:** the flowchart shows Support tickets being handled inside `CLIENTMGMT`'s territory (Client Service creates tickets, per the sequence diagram), but architecture has no dedicated "Support Service." **Decision: keep Support as a module inside Client Service for MVP**, split it into its own microservice only if support ticket volume/SLA logic grows complex later. This keeps you at 4 real services to build (Client, Account, Billing, Notification) instead of 6+, which is the right MVP scope for a solo/portfolio build.

---

## 2. Target architecture (trimmed for solo build, same shape as your diagram)

```
Client (Postman/React later) 
   â†’ Nginx (reverse proxy)
   â†’ Kong (API Gateway) â†’ Keycloak (OAuth2/JWT)
   â†’ [ Client Service | Account Service | Billing Service | Notification Service ]  (Spring Boot)
        â†• Redis (cache)        â†• Kafka (events)        â†• MySQL (primary, per-service schema)
   â†’ MinIO (S3-compatible, PDFs)
   â†’ Observability: Prometheus + Grafana, ELK, Jaeger
   â†’ CI/CD: GitHub Actions + SonarQube
```

Everything here has a **free/OSS tier and runs in Docker Compose on your laptop** â€” no cloud bill required until you want to deploy publicly (then a single free-tier VM or Render/Railway is enough).

### 2a. Dev build vs. target architecture (important distinction from v2)

Your v2 architecture diagram is a **production-grade target**: 3 Kafka brokers + Zookeeper, k3s Master + 3 Worker Nodes, Redis Sentinel + Master + Replica, MySQL Primary + 2 read replicas, a separate Keycloak MySQL, plus Consul for service discovery. That's the right thing to *diagram and explain in interviews* â€” it's not the right thing to *run continuously on a laptop* while you're still writing business logic.

| | Phase 0â€“9 (what you actually build/run) | Phase 10â€“11 (what you demo from the v2 diagram) |
|---|---|---|
| Kafka | 1 broker, no Zookeeper cluster | 3 brokers, partitioned topics |
| MySQL | 1 instance, all schemas | Primary + 2 read replicas |
| Redis | 1 instance | Sentinel + Master + Replica |
| Orchestration | Docker Compose | k3d (k3s-in-Docker) with 3 worker containers |
| Service discovery | Not needed at 4 services | Consul (optional â€” k8s DNS may already cover this; see Â§7 note) |

Spin the clustered version up only long enough to load-test, screenshot, and prove it scales (Phase 10) â€” don't run it as your default dev environment.

---

## 3. Tech stack mapping (per your diagram, Java-core)

| Layer | Tool | Why |
|---|---|---|
| Language/Framework | Java 21 + Spring Boot 3 | Core requirement, industry-standard for SDE roles |
| Build | Maven (multi-module) or Gradle | Multi-module = one repo, one service per module |
| Database | MySQL 8 | Core requirement; use Flyway/Liquibase for schema migrations |
| Cache | Redis | Session/profile caching, duplicate-email checks |
| Messaging | Apache Kafka + Zookeeper | Event-driven service decoupling (CLIENT_ONBOARDED, INVOICE_GENERATED, PAYMENT_SUCCESS, TICKET_CREATED) |
| Auth | Keycloak | OAuth2/JWT, roles (account_manager etc.) â€” matches your `roles`/`users` ERD tables |
| Gateway | Spring Cloud Gateway (simpler than Kong for a solo project) or Kong OSS if you want it exactly as diagrammed | Central routing, rate-limiting |
| Reverse proxy | Nginx | TLS termination, static routing |
| Object storage | MinIO | Contract/invoice PDFs (S3 API-compatible) |
| PDF generation | OpenPDF or iText (community) | Contract/invoice PDF generation |
| Containers | Docker + Docker Compose (skip k3s until Phase 7) | Local dev simplicity first |
| Observability | Prometheus + Grafana, Micrometer/OpenTelemetry + Jaeger, AlertManager | Matches v2's OBSTIER exactly â€” ELK dropped in favor of `audit_logs` as the log-of-record + Grafana/Jaeger, lighter to run |
| CI/CD | GitHub Actions (2000 free min/month) + SonarQube (Docker) | Matches your CICD box |
| Testing | JUnit 5, Mockito, Testcontainers (spin up real MySQL/Kafka in CI) | Critical for demonstrating engineering maturity |
| Service discovery | Skip for MVP; Consul optional in Phase 11 | At 4 services, k8s-native DNS (once you're on k3s) covers this â€” Consul adds real value once you scale past ~8â€“10 services |
| Auth DB | Separate MySQL schema/instance for Keycloak (per v2's Keycloak MySQL) | Keeps Keycloak's realm data isolated from app data â€” standard practice, avoids coupling auth to your app's migrations |

---

## 4. Build order (mirrors your sequence diagram's flow, so each phase is demoable)

### Phase 0 â€” Setup (2â€“3 days)
- Multi-module Maven repo: `cms-parent`, `client-service`, `account-service`, `billing-service`, `notification-service`, `common` (shared DTOs/events)
- `docker-compose.yml`: MySQL, Redis, Kafka+Zookeeper, Keycloak, MinIO, Mailhog (fake SMTP for dev)
- Design your Kafka topics now: `CLIENT_ONBOARDED`, `INVOICE_GENERATED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `TICKET_CREATED`

### Phase 1 â€” Auth (UserAccess flow) (3â€“5 days)
- Provision a **separate MySQL schema/instance for Keycloak** (per v2's dedicated Keycloak MySQL) â€” don't reuse your app database
- Keycloak realm `cms`, roles from your `roles` table (account_manager, admin, support_agent)
- Spring Security OAuth2 Resource Server in each service to validate JWT
- Nginx â†’ Gateway â†’ Keycloak token flow exactly as your sequence diagram (steps 1â€“10)
- **Deliverable:** login â†’ JWT issued â†’ protected endpoint returns 401/200 correctly

### Phase 2 â€” Client Service (ClientMgmt flow) (1â€“1.5 weeks)
- `clients`, `contacts`, `addresses`, `activity_logs` tables (Flyway migrations from your ERD)
- Redis: duplicate-email check cache, client profile cache (TTL 30 min, as in your sequence diagram)
- Publish `CLIENT_ONBOARDED` to Kafka on client creation
- **Deliverable:** POST /clients â†’ 201, cached, event published

### Phase 3 â€” Account Service (linked in ClientMgmt) (3â€“5 days)
- `accounts` table, linked to client via `POST /accounts/link/{client_id}` (per your sequence diagram)
- Consume nothing yet â€” just called synchronously by Client Service

### Phase 4 â€” Notification Service (event consumer) (3â€“5 days)
- Kafka consumer for `CLIENT_ONBOARDED`, `INVOICE_GENERATED`, `PAYMENT_SUCCESS`, `TICKET_CREATED`
- `notification_templates`, `notifications` tables
- Mailhog for dev (swap to a real free SMTP like Brevo/SendGrid free tier later)

### Phase 5 â€” Billing Service (ProductFlow + BillingFlow + PaymentFlow) (2 weeks â€” biggest chunk)
- `products`, `product_categories`, `subscriptions`, `contracts`, `invoices`, `payments` tables
- Contract â†’ invoice generation â†’ PDF via OpenPDF â†’ upload to MinIO (matches your sequence diagram exactly)
- Payment: mock/stub a gateway first (Razorpay/Stripe **test mode** is free) â€” implement retry logic on failure per your PAYMENTFLOW diagram (`Retry?` branch)
- Publish `PAYMENT_SUCCESS` / `PAYMENT_FAILED`

### Phase 6 â€” Support Tickets (SupportFlow) (3â€“5 days)
- Module inside Client Service (per decision in Â§1): `support_tickets`, `ticket_comments`
- Publish `TICKET_CREATED` â†’ Notification Service sends confirmation

### Phase 7 â€” Observability (OpsFlow) (1 week)
- Micrometer â†’ Prometheus â†’ Grafana dashboards
- OpenTelemetry â†’ Jaeger tracing across services (this is what makes the project stand out in interviews â€” most portfolio projects skip this)
- AlertManager for threshold alerts (as in your OPSFLOW box)
- **No ELK** â€” v2 dropped it; `audit_logs` (already written on every mutating action) plus Grafana/Jaeger is your log-of-record. Lighter to run, one less stateful service to babysit

### Phase 8 â€” Security hardening (3â€“5 days)
- Rate limiting at gateway, input validation, `audit_logs` write-through on every mutating action
- Vault (OSS) for secrets instead of `.env` files â€” good talking point for interviews

### Phase 9 â€” CI/CD (3â€“5 days)
- GitHub Actions: build â†’ test (Testcontainers) â†’ SonarQube scan â†’ Docker image â†’ push to GitHub Container Registry (free, replaces Nexus for a solo project)

### Phase 10 â€” Scalability proof (1 week, optional but high-value)
- Move Docker Compose â†’ **k3d** (k3s-in-Docker, still free, matches v2's k3s Master + 3 Worker Nodes without needing separate machines)
- MySQL: add the 2 read replicas from v2, route read-heavy queries (client search/list) to them
- Kafka: go from 1 broker to the 3-broker setup in v2, partition `PAYMENT_SUCCESS`/`INVOICE_GENERATED` topics, run 2 consumer instances of Notification Service to prove horizontal scaling
- Redis: add Sentinel + Replica per v2 (only needs to run for the demo/load test, not permanently)
- HAProxy or just multiple gateway replicas behind Nginx
- This phase is where you *actually build* the clustered version of the architecture â€” treat it as a time-boxed exercise (spin up, load test, screenshot, tear down), not your new daily dev environment

### Phase 11 â€” Analytics/Reporting + optional infra (stretch, matches the "gap" from Â§1)
- This is where v2's dashed-line `PHASE 11 STRETCH` box gets built: Analytics/Reporting services, OpenSearch (x2 nodes), ClickHouse
- Simple version: nightly batch job (or Kafka CDC stream, as v2 labels it) aggregates MySQL â†’ ClickHouse for a "revenue by product" dashboard in Grafana
- If you want the full v2 picture: add Consul for service discovery and HashiCorp Vault for secrets here too â€” both are more defensible once you're running the multi-node Phase 10 setup than they are at 4 services on Docker Compose

---

## 5. Suggested timeline
~9â€“11 weeks solo, part-time (Phases 0â€“9 = solid, demo-able, resume-ready project; Phases 10â€“11 = differentiators if you have more runway).

## 6. What makes this stand out on a resume/interview
1. **Event-driven microservices with real Kafka topics**, not just REST-to-REST calls
2. **Distributed tracing (Jaeger)** â€” very few fresher projects have this
3. **Read replicas + caching strategy** you can explain (cache-aside pattern, TTL choices, cache invalidation on update)
4. **Idempotent payment retry logic** â€” a genuinely hard distributed-systems problem, be ready to explain how you avoid double-charging on retry
5. Entirely **zero-cost**, so you can keep it running and link a live demo

---

*Generated from your 4 uploaded diagrams (business flowchart, zero-cost architecture, ERD, sequence diagram) â€” Aug 2026.*
