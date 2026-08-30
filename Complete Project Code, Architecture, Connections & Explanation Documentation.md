# Complete Project Code, Architecture, Connections & Explanation Documentation

Analyze my **entire project/codebase** and provide me with a complete, accurate, end-to-end explanation of how the project works.

I want the explanation in a way that I can **understand the code, understand how all components/tools are connected, and confidently explain the entire project to an interviewer, mentor, or technical team**.

## 1. Complete Project Overview

First, explain:

- What this project does
- Why this project was created
- The main business purpose
- The overall architecture
- Whether it is a monolithic or microservices architecture
- All major technologies, frameworks, databases, tools, and services used
- Why each technology/tool was selected
- The complete request/data flow from beginning to end

Provide a simple high-level architecture explanation before going into individual files.

---

## 2. Complete Project Folder Structure

Show the important project folder/file structure.

For every important file or folder, provide its **exact file path**.

For example:

```text
project-root/
├── api-gateway/
│   ├── src/main/java/...
│   └── pom.xml
├── client-service/
│   ├── src/main/java/...
│   └── pom.xml
├── ticket-service/
│   └── ...
├── billing-service/
│   └── ...
├── docker-compose.yml
└── ...
```

Do not just mention file names.

Always provide the complete path, for example:

```text
client-service/src/main/java/com/example/client/controller/ClientController.java
```

---

# 3. Explain Every Important File

For each important source/configuration file, explain:

### File Path
Give the exact path.

### Purpose
Explain what this file is responsible for.

### Code
Explain the important code sections.

### How It Works
Explain the execution flow step by step.

### Why It Is Required
Explain why this file exists and what would happen if it were removed or changed.

### Connections
Explain:

- Which other files/classes it communicates with
- Which service it communicates with
- Which database/table it uses
- Which API endpoint it supports
- Which configuration file controls it
- Which external tool/service it connects to

Do not explain code in isolation.

Always explain how the file fits into the complete system.

---

# 4. Explain How All Components Are Connected

This is extremely important.

Explain exactly **how each component/tool is connected to the others**.

For example, explain connections such as:

```text
Frontend
   ↓
API Gateway
   ↓
Authentication / Keycloak
   ↓
Client Service
   ↓
Database
```

And:

```text
API Gateway
   ↓
Ticket Service
   ↓
Notification Service
   ↓
MailHog
```

And:

```text
Billing Service
   ↓
Contract Generation
   ↓
PDF
   ↓
MinIO
```

And:

```text
Application
   ↓
OpenTelemetry / tracing
   ↓
Jaeger
```

And:

```text
Application
   ↓
Metrics
   ↓
Prometheus
   ↓
Grafana
```

For **every connection**, explain:

1. Who connects to whom?
2. Why are they connected?
3. How are they connected?
4. Which protocol is used?
5. Which URL/port is used?
6. Which configuration file contains the connection details?
7. Which environment variable is used, if any?
8. Which class/code actually creates or uses the connection?
9. What data is sent between them?
10. What happens if the connection fails?

---

# 5. Explain Connection Configuration in Detail

Find all connection-related configuration in the project.

Explain the purpose of things such as:

- `application.yml`
- `application.properties`
- `.env`
- `docker-compose.yml`
- Dockerfiles
- environment variables
- database URLs
- service URLs
- Keycloak configuration
- JWT configuration
- Redis configuration, if present
- Kafka/RabbitMQ configuration, if present
- MinIO configuration
- SMTP/MailHog configuration
- Prometheus configuration
- Grafana configuration
- Jaeger/OpenTelemetry configuration
- API Gateway routes
- service discovery configuration, if present

For every connection configuration, provide:

```text
File:
Exact path

Configuration:
<property>

Purpose:
<explanation>

Connects:
<source> → <destination>

Port:
<port>

Protocol:
<HTTP/HTTPS/etc.>

Used by:
<class/service>

Reason:
<why this connection exists>
```

---

# 6. Explain Docker Connectivity

Explain the complete Docker setup.

Start with:

```text
docker-compose.yml
```

Explain:

- Every container
- Container name
- Image
- Build configuration
- Port mapping
- Environment variables
- Volumes
- Networks
- Dependencies
- Health checks
- Restart policies

Most importantly, explain **how containers communicate with each other**.

For example:

```text
Host Machine
     |
     | localhost:8090
     ↓
API Gateway Container
     |
     | Docker network
     ↓
Client Service Container
     |
     ↓
Database Container
```

Explain the difference between:

```text
localhost
```

and Docker service/container names such as:

```text
client-service
database
keycloak
```

Explain why a service may use:

```text
http://keycloak:8080
```

inside Docker instead of:

```text
http://localhost:8080
```

---

# 7. Explain Keycloak Authentication

Explain the complete authentication flow.

Start from:

```text
User
 ↓
Postman / Frontend
 ↓
Keycloak
 ↓
JWT Access Token
 ↓
API Gateway
 ↓
Microservice
```

Explain:

- Realm
- Client
- Client ID
- Client Secret
- Users
- Roles
- Permissions
- JWT
- Access token
- Refresh token
- Bearer authentication
- Token validation
- How the backend verifies the JWT
- Where Keycloak configuration exists in the code
- Which classes handle authentication/security

Also explain the difference between clients such as:

```text
cms-backend
cms-admin
```

if both exist in the project.

Explain exactly where each is used.

---

# 8. Explain API Gateway

Explain the API Gateway in detail.

Show:

- Exact file path
- Configuration file
- Route configuration
- Authentication/security configuration
- Filters
- Port
- Downstream services

Create a table like:

| Incoming API | Gateway Route | Destination Service |
|---|---|---|
| `/api/v1/clients/**` | Client route | Client Service |
| `/api/v1/tickets/**` | Ticket route | Ticket Service |
| `/api/v1/billing/**` | Billing route | Billing Service |

Use the actual routes from my project rather than assuming them.

Explain how the request travels through the gateway.

---

# 9. Explain Each Microservice End-to-End

For every microservice, explain the complete flow:

```text
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
```

If additional layers exist, explain them too.

For example:

```text
Controller
 ↓
DTO
 ↓
Mapper
 ↓
Service
 ↓
Repository
 ↓
Entity
 ↓
Database
```

For each layer explain:

- Exact file path
- Responsibility
- Important classes
- Important methods
- How it communicates with the next layer
- Why that layer exists

---

# 10. Explain Database Connectivity

Identify:

- Database technology
- Database container
- Database name
- Host
- Port
- Username configuration
- Password configuration
- JDBC/database URL
- Connection pool
- ORM
- JPA/Hibernate configuration
- Migration tool such as Flyway/Liquibase, if used

Explain:

```text
Application
 ↓
DataSource
 ↓
JPA/Hibernate
 ↓
Repository
 ↓
Database
```

Then explain the important entities/tables and their relationships.

---

# 11. Explain Entity Relationships

For every major entity, explain:

- Entity class path
- Database table
- Primary key
- Foreign keys
- Relationships
- `OneToMany`
- `ManyToOne`
- `OneToOne`
- `ManyToMany`

Explain why the relationships were designed that way.

Use simple examples.

---

# 12. Explain Complete Client Management Flow

Use the actual code and explain this complete flow:

```text
Create Client
 ↓
API Gateway
 ↓
Client Controller
 ↓
Client Service
 ↓
Repository
 ↓
Database
 ↓
Response
```

Then explain:

- Create client
- Get client
- List clients
- Search clients
- Update client
- Delete client, if available

For every API provide:

```text
HTTP Method:
URL:
Controller:
Method:
Service:
Repository:
Database:
Request DTO:
Response DTO:
Authentication:
Expected response:
```

---

# 13. Explain Complete Ticket Flow

Explain:

```text
Create Ticket
 ↓
Ticket Service
 ↓
Database
 ↓
Notification
 ↓
MailHog
```

Then explain:

- Ticket creation
- Assignment
- Comment
- Resolve
- Reopen
- Close
- Email notification

For every operation provide the exact file paths and methods responsible.

---

# 14. Explain Billing Flow

Explain the complete billing process.

For example:

```text
Client
 ↓
Account
 ↓
Contract
 ↓
Products
 ↓
Invoice
 ↓
Payment
```

Explain the actual flow implemented by the code.

For contracts, explain:

```text
API
 ↓
Billing Controller
 ↓
Billing Service
 ↓
Contract creation
 ↓
PDF generation
 ↓
MinIO
```

For payments explain:

```text
Payment API
 ↓
Payment Service
 ↓
Invoice
 ↓
Payment Provider / Stripe
 ↓
Payment Status
```

Use the actual implementation from the project.

---

# 15. Explain MinIO Connection

Explain:

- Why MinIO is used
- Where MinIO is configured
- Endpoint
- Port
- Access key
- Secret key
- Bucket
- Which service connects to MinIO
- Which class creates the connection
- Which method uploads/downloads files
- How generated PDFs are stored

Show the complete flow:

```text
Billing Service
 ↓
MinIO Client
 ↓
MinIO Server
 ↓
Bucket
 ↓
PDF Object
```

---

# 16. Explain Email/MailHog Connection

Explain:

```text
Application
 ↓
SMTP
 ↓
MailHog
 ↓
Web UI
```

Identify:

- SMTP configuration file
- SMTP host
- SMTP port
- Sender configuration
- Email service class
- Method responsible for sending emails
- MailHog container
- MailHog web interface

Explain why MailHog is used during development.

---

# 17. Explain Observability

Explain the complete connection between:

```text
Application
 ↓
Metrics → Prometheus → Grafana
```

and:

```text
Application
 ↓
Tracing → Jaeger
```

Explain:

### Prometheus
- What metrics are generated
- Where metrics are exposed
- Prometheus configuration
- Scrape configuration
- Endpoint
- Which service exposes metrics

### Grafana
- How Grafana connects to Prometheus
- Datasource configuration
- Dashboard configuration
- What the dashboards display

### Jaeger
- How traces are generated
- How services send traces
- Trace IDs
- Spans
- How to find a request in Jaeger

Provide the exact configuration and file paths.

---

# 18. Explain One Complete Request From Start to Finish

Choose one important API, preferably:

```text
POST /api/v1/clients
```

and explain the request **line by line through the entire application**.

For example:

```text
Postman
 ↓
http://localhost:8090
 ↓
API Gateway
 ↓
Authentication
 ↓
Client Service
 ↓
ClientController
 ↓
ClientService
 ↓
ClientRepository
 ↓
Hibernate/JPA
 ↓
Database
 ↓
Response
 ↓
Postman
```

For every arrow, explain:

- What happens
- Which file handles it
- Exact file path
- Method/class name
- Data being passed
- Why it goes to the next component

---

# 19. Explain Error Handling

Find and explain:

- Global exception handlers
- Custom exceptions
- Validation
- HTTP status codes
- Error response structure
- Logging

Explain what happens when:

- Client doesn't exist
- Invalid JWT
- Missing required field
- Duplicate email
- Database is unavailable
- Another service is unavailable
- Payment fails
- MinIO fails
- Email fails

Use actual implementation from the project.

---

# 20. Explain Configuration Management

Identify all configuration files and explain which configuration is used in:

- Development
- Docker
- Production, if available

Explain:

```text
Environment Variable
 ↓
Configuration
 ↓
Application
```

Clearly identify sensitive values such as:

- Passwords
- Client secrets
- API keys
- Database credentials

Do not expose real secrets unnecessarily. Mask them as:

```text
********
```

---

# 21. Manual Operation Guide

After explaining the code, provide a complete manual testing guide.

Start from:

```text
1. Start Docker
2. Verify containers
3. Verify health
4. Login to Keycloak
5. Get JWT
6. Create client
7. View client
8. Search client
9. Update client
10. Create ticket
11. Check MailHog
12. Assign ticket
13. Add comment
14. Resolve/reopen/close ticket
15. Create contract
16. Check MinIO
17. Process payment
18. Check Jaeger
19. Check Prometheus
20. Check Grafana
```

For every step provide:

- URL
- HTTP method
- Headers
- Authorization
- Body
- Expected response
- What to verify

---

# 22. Explain the Project Like an Interview Answer

At the end, create an **easy-to-speak explanation** of the project.

Give me:

### 30-second explanation

Something I can say when the interviewer asks:

> "Tell me about your project."

### 2-minute explanation

Explain:

- Problem
- Solution
- Architecture
- Technologies
- Major modules
- Authentication
- Database
- Billing
- Observability

### 5-minute technical explanation

Explain the complete architecture and request flow in a way that sounds natural when spoken.

---

# 23. Interview Questions Based on My Actual Code

Generate technical interview questions based ONLY on what actually exists in my project.

For each question provide:

### Question
### Short Answer
### Detailed Answer
### Relevant File Path

Include questions about:

- Architecture
- Java/Spring Boot
- Microservices
- API Gateway
- Keycloak
- JWT
- Docker
- Database
- JPA/Hibernate
- REST APIs
- DTOs
- Exception handling
- Billing
- Stripe
- MinIO
- MailHog
- Prometheus
- Grafana
- Jaeger
- Configuration
- Inter-service communication

---

# 24. Important Rules

Follow these rules while analyzing the project:

1. **Do not guess.**
2. Do not invent files, classes, endpoints, databases, or connections.
3. Use the actual codebase as the source of truth.
4. Always provide the **exact file path** when discussing code.
5. Mention the exact class and method name whenever possible.
6. Explain relationships between files, not just individual files.
7. Explain why each technology/component is used.
8. Explain how components communicate.
9. Clearly distinguish between:
   - Host machine
   - Docker container
   - Internal Docker network
   - External URL
10. If something is configured but not actually used, clearly mention it.
11. If something is missing or incomplete, clearly mention it.
12. If the implementation differs from the documentation, point out the difference.
13. Do not assume that an endpoint or feature exists just because it is mentioned in documentation.
14. Use actual values from configuration where safe; mask secrets.
15. Explain complicated concepts in simple language.
16. Assume that I am learning the project and need to explain it verbally.

---

# 25. Final Output Structure

Organize the final documentation exactly in this order:

1. Project Overview
2. Technology Stack
3. Complete Folder Structure
4. Architecture
5. Component Connections
6. Docker Architecture
7. Keycloak Authentication
8. API Gateway
9. Client Service
10. Ticket Service
11. Billing Service
12. Database
13. MinIO
14. Email/MailHog
15. Prometheus
16. Grafana
17. Jaeger
18. Complete API Request Flow
19. Error Handling
20. Configuration Management
21. Complete Manual Operation Guide
22. End-to-End Business Flow
23. Interview Explanation
24. Interview Questions & Answers
25. Common Problems and Troubleshooting
26. Final Architecture Summary

Make the explanation **step-by-step, practical, and easy to speak**, so that after reading it I can explain:

> **What each file does → why it exists → how it connects to other files → how services communicate → how data moves → how the tools are connected → how the complete application works from start to finish.**