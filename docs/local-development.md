# Local development

See the README for the three-command quick start. Details:

## Backend
- `./mvnw spring-boot:run` uses the `local` profile: Postgres at `localhost:5432/gharse`, demo data, Swagger UI, DEBUG logs.
- Override anything with env vars (`DATABASE_URL`, `JWT_SECRET`, …). `local` ships a dev-only JWT secret.
- Uploads are stored in `./uploads` and served at `/uploads/**`.
- Forgot-password requests return a one-time reset token in the local profile for manual testing. Production hides this
  token; configure an email or another secure delivery provider before enabling password resets for production users.
- Reset the database: `docker compose down -v && docker compose up -d postgres`.

## Tests
- `./mvnw test` starts a throw-away `postgres:16-alpine` container through Testcontainers, so **Docker must be running**.
- Docker Engine 29+ needs `api.version=1.44`; this is set in the surefire config.
- Test classes: auth/roles, cook approval + catalogue + search, batch capacity (incl. a concurrency test), cart + order
  lifecycle, reviews + favorites, file upload, order status unit tests.

## Frontend
- `npm run dev` (Vite, port 5173), `npm run lint`, `npm test` (Vitest + Testing Library), `npm run build`.
- `VITE_API_URL` in `frontend/.env` points at the backend. Set `VITE_GOOGLE_MAPS_API_KEY` to a Google Maps Platform key
  with Maps JavaScript API, Places API, and Geocoding API enabled. Restrict the key to the app's origins and those APIs.

### iOS app
The Capacitor app loads a bundled copy of the frontend; running Vite does not update it. After frontend changes, rebuild and sync the bundle, then relaunch the app in Xcode:

```sh
cd frontend
VITE_API_URL=https://gharsecook-1.onrender.com npm run build:ios
npm run ios
```

## Windows notes
- Use `mvnw.cmd` instead of `./mvnw`.
- If Docker Desktop is not running, start it first; `docker compose up -d postgres` and the backend tests need it.

## Troubleshooting
| Symptom | Fix |
|---------|-----|
| `JWT_SECRET must be set` | run with the `local` profile or export a ≥ 32 char `JWT_SECRET` |
| `Connection refused localhost:5432` | `docker compose up -d postgres` |
| Browser CORS error | `FRONTEND_URL` must exactly match the origin (scheme + host + port) |
| Testcontainers "Could not find a valid Docker environment" | start Docker; on Engine 29+ keep the `api.version` property |
