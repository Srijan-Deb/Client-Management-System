# CMS Admin Portal

Frontend for the internal admin/staff portal (Dashboard, Clients, Billing, Tickets, Settings).

## Tech Stack
- React 19 + TypeScript
- Vite
- Tailwind CSS v4
- React Router v6
- TanStack Query
- React Hook Form + Zod
- Axios
- Keycloak

## Setup Instructions
1. Install dependencies:
   ```bash
   npm install
   ```
2. Configure environment variables in `.env`:
   - `VITE_API_BASE_URL`: URL to the Spring Cloud Gateway
   - `VITE_KEYCLOAK_URL`: URL to the Keycloak instance
   - `VITE_KEYCLOAK_REALM`: Keycloak realm name (`cms`)
   - `VITE_KEYCLOAK_CLIENT_ID`: Keycloak client ID (`cms-admin`)
3. Run the development server:
   ```bash
   npm run dev
   ```
   The app runs on `http://localhost:3000`.
