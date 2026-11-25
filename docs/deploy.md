# Deploying ShipSense (free tiers)

> Nothing here is auto-deployed. These are the steps to deploy yourself; only
> claim a live URL in the README once it is actually live.

ShipSense is four containers (Postgres, ML service, Java API, React frontend).
The cheapest path is to deploy each piece to a free tier.

## Option A — Render (simplest)

1. **Database**: create a free **PostgreSQL** instance on Render. Copy its
   internal connection string. Run `db/schema.sql` then `db/seed.sql` against it
   once (via `psql "$DATABASE_URL" -f db/schema.sql`).
2. **ML service**: new **Web Service** from the repo, root `ml-service`,
   environment *Docker* (uses `ml-service/Dockerfile`). No extra env needed.
3. **Java API**: new **Web Service**, root `backend-java`, Docker. Set env vars:
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
     (from step 1)
   - `ML_SERVICE_URL` = the ML service URL from step 2
   - `APP_CORS_ALLOWED_ORIGINS` = the frontend URL from step 4
4. **Frontend**: new **Static Site**, root `frontend`, build `npm ci && npm run build`,
   publish directory `dist`. Set build-time env `VITE_API_BASE_URL` = the Java API URL.

## Option B — Azure (student credit)

- **Azure Database for PostgreSQL Flexible Server** (burstable B1ms is cheapest).
- **Azure Container Apps** or **App Service for Containers** for the ML service and
  Java API (push images built from the Dockerfiles to GitHub Container Registry or ACR).
- **Azure Static Web Apps** for the React frontend.
- Wire the same environment variables as in Option A.

## Notes

- The free Postgres tiers are small; the seed data fits comfortably.
- Free web services may cold-start (sleep when idle) — the first request can be slow.
- Keep secrets in the platform's env-var settings, never in the repo.
