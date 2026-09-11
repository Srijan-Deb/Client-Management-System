# Security Policy

## Supported Versions

The following versions of the Client Management System (CMS) are currently maintained with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Security Overview

The Client Management System enforces enterprise-grade security standards across all layers:
- **Stateless OAuth2 / OpenID Connect**: Authenticated via Keycloak with RS256-signed JWT tokens.
- **Role-Based Access Control (RBAC)**: Fine-grained permissions enforced at API Gateway (`api-gateway`) and resource servers with Spring Security `@PreAuthorize`.
- **Tenant & Client Isolation**: B2B accounts and clients can only access resources belonging to their organization.
- **Secrets Management**: Sensitive credentials and private keys are externalized through environment variables and never checked into source control.

## Reporting a Vulnerability

If you discover a potential security vulnerability within this project:
1. **Do not disclose publicly**: Avoid opening public GitHub issues for security vulnerabilities.
2. **Submit a report**: Contact the maintainers directly via security advisory or email at `srijan159753@gmail.com` with full reproduction steps and impact assessment.
3. **Response Timeline**: Maintainers will acknowledge reports within 48 hours and work on a coordinated fix and release timeline.

