# Deployment

```
User → Vercel (React) → HTTPS → Render (Spring Boot) → Supabase PostgreSQL
                                      └─ S3-compatible storage (required before real production use)
GitHub: source + CI (.github/workflows)
```

Deploy only from a green `main` (CI runs tests first). Keep production credentials out of local machines and the repo.

## 1. Supabase PostgreSQL
1. Create a project; note the database password.
2. *Project settings → Database → Connection string*. Use the **direct** or **session pooler** string (Flyway needs session
   features; avoid the transaction pooler on port 6543 for migrations).
3. Convert to JDBC form and put credentials in separate variables:
   - `DATABASE_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require`
   - `DATABASE_USERNAME=postgres` (or the pooler user, e.g. `postgres.<project-ref>`)
   - `DATABASE_PASSWORD=<password>`

Flyway creates the schema on first start. No manual SQL.

## 2. Render (backend)
Create a **Web Service** from the repo, root directory `backend`, runtime **Docker** (uses `backend/Dockerfile`).
Health check path: `/actuator/health`.

| Variable | Value |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | from Supabase |
| `JWT_SECRET` | random ≥ 32 chars (`openssl rand -base64 48`) |
| `FRONTEND_URL` | your Vercel URL(s), e.g. `https://gharse.vercel.app` (comma separate several); include `capacitor://localhost` for the iOS app |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | creates the first admin; remove or rotate afterwards |
| `STORAGE_PUBLIC_BASE_URL` | public base URL for uploads (see storage warning) |

The `prod` profile disables seed data and Swagger and switches logs to structured JSON. Migrations run on startup.
Free Render instances sleep; expect a cold start on the first request.

## 3. Vercel (frontend)
Import the repo, **root directory `frontend`**, framework Vite, build `npm run build`, output `dist`.
Set `VITE_API_URL=https://<your-render-service>.onrender.com`. `frontend/vercel.json` rewrites all routes to `index.html`
for client-side routing. Set `VITE_GOOGLE_MAPS_API_KEY` to a Google Maps Platform browser key with Maps JavaScript API,
Places API, and Geocoding API enabled. Restrict it to the deployed site and required APIs. After the first deploy, put
the Vercel URL into Render's `FRONTEND_URL` (CORS).

## 4. iOS app (Capacitor)
The iOS app reuses the React frontend. From `frontend`, build with the public Render backend URL and sync it into the iOS project:

```sh
cd frontend
VITE_API_URL=https://gharsecook-1.onrender.com \
VITE_GOOGLE_MAPS_API_KEY=<your-google-maps-key> npm run build:ios
npm run ios
```

Set the Render `FRONTEND_URL` variable to a comma-separated list containing both your Vercel site and `capacitor://localhost`, then redeploy the backend. Build and sign the iOS app with Xcode on a Mac; the repository does not contain Apple signing credentials.

Google Maps area search is used on cook discovery and the admin's Delivery areas page. Admins can save named place pins or
draw region boundaries and mark each as **Supported** or **Not supported**. Cook searches filter by the selected locality
and city and sort nearby cooks first. The map key is bundled into the frontend, so it is public:
restrict it in Google Cloud to the necessary Maps APIs and your website/app origins, and do not use a server credential.

## 5. Object storage — required before real use
Uploads currently go to local disk, which Render wipes on every deploy. Before onboarding real cooks, add an
`S3StorageService` (Supabase Storage, Cloudflare R2, S3 …) implementing `StorageService`, driven by `STORAGE_ENDPOINT`,
`STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY`, `STORAGE_BUCKET`. No other code needs to change. Until then, cooks can still
use the app, but photos will not persist across deploys.

## 6. CI/CD
`backend-ci.yml` (Java 21: test, package) and `frontend-ci.yml` (Node 22: install, lint, test, build) run on every PR
and push to `main`. Enable "auto-deploy only after CI passes" in Render and Vercel, or deploy from a protected branch.

## Checklist
- [ ] `JWT_SECRET` set and unique per environment
- [ ] `FRONTEND_URL` matches the deployed origin
- [ ] first admin created, `ADMIN_PASSWORD` rotated
- [ ] persistent object storage in place
- [ ] Supabase backups enabled
