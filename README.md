# GharSe — Find amazing Home Cooks around you

GharSe is a two-sided marketplace: customers discover talented **Home Cooks**, read their story, pre-order from limited
**batches**, and review them. The cook is the hero, not the restaurant.

```
React (Vite, TS, Tailwind)  --REST /api/v1-->  Spring Boot modular monolith  -->  PostgreSQL
```

This repository is the base MVP: customer, cook and admin experiences, JWT auth, atomic batch capacity, order lifecycle,
mock payment and delivery providers, reviews, notifications, seed data, tests and CI. See [docs/](docs) for details.

## Prerequisites

| Tool | Version | Used for |
|------|---------|----------|
| JDK | 21 | backend |
| Node.js | 20+ (22 recommended) | frontend |
| Docker | recent | local PostgreSQL and the backend tests (Testcontainers) |

## Run locally

```bash
# 1. Database (PostgreSQL 16 on localhost:5432, db/user/password: gharse)
docker compose up -d postgres

# 2. Backend  -> http://localhost:8080   (Swagger UI: /swagger-ui.html)
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# 3. Frontend -> http://localhost:5173
cd frontend
cp .env.example .env            # optional; defaults to http://localhost:8080
npm install
npm run dev
```

The `local` profile is the default. On first start Flyway creates the schema and the **demo data seeder** loads
5 cooks, 18 dishes, 10 batches, sample orders and reviews.

### Demo accounts (local only, password `Demo@1234`)

| Role | Email |
|------|-------|
| Customer | `priya@gharse.demo`, `rahul@gharse.demo` |
| Cook | `meena@gharse.demo`, `asha@gharse.demo`, `sneha@gharse.demo`, `kavita@gharse.demo`, `ramesh@gharse.demo` |
| Admin | `admin@gharse.demo` |

All seeded data uses `@gharse.demo` emails. It is created only when `gharse.seed.enabled=true` (the `local` profile) and
is never enabled in production.

Everything in containers instead: `docker compose --profile full up --build` (frontend on :5173, backend on :8080).

## Tests and checks

```bash
cd backend  && ./mvnw test            # needs Docker running (PostgreSQL via Testcontainers)
cd frontend && npm run lint && npm test && npm run build
```

> **Docker Engine 29+:** the project's surefire config pins `api.version=1.44` so Testcontainers can talk to it.

## Configuration (environment variables)

| Variable | Purpose | Default (local) |
|----------|---------|-----------------|
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/postgres?sslmode=require` | `jdbc:postgresql://localhost:5432/gharse` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | DB credentials | `gharse` / `gharse` |
| `JWT_SECRET` | HS256 secret, **≥ 32 chars, required outside local/test** | local-only dev value |
| `FRONTEND_URL` | Allowed CORS origin(s), comma separated | `http://localhost:5173` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Creates the first admin at startup if set (password ≥ 8 chars) | unset |
| `STORAGE_TYPE` | `local` for development or `supabase` for Supabase Storage | `local` |
| `SUPABASE_URL` / `SUPABASE_SERVICE_ROLE_KEY` | Backend-only Supabase project URL and service-role secret (required with Supabase storage) | unset |
| `SUPABASE_PUBLIC_BUCKET` / `SUPABASE_PRIVATE_BUCKET` | Public image bucket and private document bucket names | `gharse-media` / `gharse-documents` |
| `VITE_API_URL` (frontend) | Backend base URL | `http://localhost:8080` |
| `VITE_GOOGLE_MAPS_API_KEY` (frontend) | Google Maps JavaScript, Places, and Geocoding APIs key | unset |

The Google Maps key is bundled into the browser app. Restrict it to the required APIs and your app's allowed origins in Google Cloud.

Profiles: `local` (default), `test`, `prod`. Secrets are never committed; see `.gitignore`.

### Supabase Storage setup

1. In Supabase Storage, create a `gharse-media` bucket and make it public for profile, dish, and kitchen images.
2. Create a `gharse-documents` bucket and leave it private for cook certificates.
3. Set `STORAGE_TYPE=supabase`, `SUPABASE_URL`, and `SUPABASE_SERVICE_ROLE_KEY` in the backend environment.
   The service-role key is backend-only; do not add it to Vite/frontend variables or commit it.
4. For Render, the blueprint configures the bucket names and asks for the project URL and service key as secrets.
   After saving them, redeploy the backend. For local development, leave `STORAGE_TYPE=local`.

## Repository layout

```
backend/    Spring Boot 3.5, Java 21, Flyway migrations in src/main/resources/db/migration
frontend/   React + Vite + TypeScript + Tailwind
docs/       architecture, database, api, local-development, deployment, business-rules, future-roadmap
.github/    backend-ci.yml, frontend-ci.yml
```

## Deliberately not included yet

Kafka, Redis, Kubernetes, microservices, Elasticsearch, AI features, an own delivery fleet, loyalty/subscriptions,
dynamic pricing, referrals, ads, multi-city engine. Extension points (payment, delivery, storage, notification,
rate limiting) are interfaces so they can be added later. See [docs/future-roadmap.md](docs/future-roadmap.md).

## Known MVP limitations

- Payment and courier delivery are **mocks** (`MockPaymentProvider`, `MockDeliveryProvider`).
- Password reset tokens are shown only in local/test for manual testing; production email delivery is not implemented.
- Local disk storage is available for development; configure Supabase Storage for persistent deployed uploads.
  Public images use the configured public bucket, while cook documents use the private bucket and admin-only
  five-minute signed links.
- Refresh tokens are stateless JWTs (no server-side revocation list yet).
- Rate limiting is in-memory (single instance).
