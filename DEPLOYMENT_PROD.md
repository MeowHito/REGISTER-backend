# Production Deployment Runbook

Target setup: **one server (VPS/EC2)** running
- **backend** + MySQL + Redis + RabbitMQ via Docker (`docker-compose.prod.yml`)
- **frontend** SPA via nginx + a tiny `return-2c2p` bridge, via Docker (`frontend/deploy/docker-compose.yml`)
- **HTTPS** in front (required — the app sets Secure cookies when `APP_ENV=PROD`)

Legend: 🤖 = file already provided in the repo · 🙋 = you must do it.

---

## 0. Prerequisites on the server 🙋
- A server with Docker + Docker Compose installed
- A domain (e.g. `app.example.com` for FE, `api.example.com` for BE) pointing to the server
- Ports 80/443 open

---

## 1. Backend 🤖 files ready
```bash
git clone https://github.com/MeowHito/REGISTER-backend.git
cd REGISTER-backend
cp .env.production.example .env.production
```
Edit `.env.production` 🙋 — fill every `<FILL>`. For THIS compose set the hosts to the
service names:
```
DB_URL=jdbc:mysql://mysql:3306/membership_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Bangkok
DB_USER=root
REDIS_HOST=redis
RMQ_HOST=rabbitmq
APP_ENV=PROD
APP_FE_URL=https://app.example.com
APP_BE_URL=https://api.example.com
AUTH_GOOGLE_REDIRECT_URL=https://app.example.com/auth/google/callback
JWT_SECRET_KEY=...           # use the generated random values
PARTICIPANT_TOKEN_SECRET=...
JPA_DDL_AUTO=none            # schema is loaded from db/schema-prod.sql on first init
```
Start:
```bash
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps      # all healthy
docker logs -f membership-app                      # wait for "Started MembershipApplication"
```
- The MySQL container auto-loads `db/schema-prod.sql` (53 tables) on first init.
- The app seeds menus/roles/admin users on first boot (password = `SEED_USER_PASSWORD`).
- Backend now listens on `:8080` (proxy it behind HTTPS as `api.example.com`).

> Resetting the DB later: `docker compose -f docker-compose.prod.yml down -v` wipes volumes
> (schema re-loads on next up). Do NOT do this once you have real data.

---

## 2. Frontend 🤖 files ready
```bash
git clone https://github.com/MeowHito/REGISTER-FRONTEND.git
cd REGISTER-FRONTEND
cp .env.production.example .env.production
```
Edit `.env.production` 🙋:
```
VITE_CONTEXT_URL=https://api.example.com      # backend URL, NO /membershipms (embedded Tomcat = root)
VITE_AUTH_GOOGLE_ID=...                        # production OAuth IDs
VITE_AUTH_FACEBOOK_ID=...
VITE_AUTH_LINE_LIFF=...
```
Build & run:
```bash
docker compose -f deploy/docker-compose.yml up -d --build
```
- nginx serves the built SPA on `:80` with SPA fallback + asset caching.
- The `return-2c2p` bridge handles the 2C2P payment callback (POST → 303 → `/registrationPaymentResult`).

> ⚠️ `VITE_*` are baked at build time. If you change them, rebuild (`up -d --build`).

---

## 3. HTTPS / reverse proxy 🙋 (required)
Put TLS in front of both containers. Easiest: a host nginx or Caddy doing:
- `https://app.example.com` → `http://127.0.0.1:80` (the FE container)
- `https://api.example.com` → `http://127.0.0.1:8080` (the BE container)

Get certs with certbot (`certbot --nginx`) or use Caddy (auto-HTTPS). Without HTTPS,
login cookies (Secure) won't work and OAuth will reject the redirect.

---

## 4. Provider dashboards 🙋 (switch to production)
- **Google**: add `https://app.example.com` to Authorized JavaScript origins and
  `https://app.example.com/auth/google/callback` to redirect URIs; **Publish** the app.
- **Facebook**: add the production domain + redirect; switch app to **Live** (needs Privacy Policy URL).
- **LINE**: set LIFF Endpoint + Callback to the production HTTPS URL.
- **Payments**: replace SCB/2C2P sandbox keys + URLs with production ones
  (`SCB_BASE_URL`, `INQUIRY_URL_2C2P`/`TOKEN_URL_2C2P` → `pgw.2c2p.com`).

---

## 5. Smoke test 🙋
- [ ] `https://app.example.com` loads; refresh on a deep route still works (SPA fallback)
- [ ] Login with email, then Google / Facebook / LINE
- [ ] An action that sends email actually delivers (needs `APP_ENV=PROD`)
- [ ] A file upload reaches S3 (bucket in `ap-southeast-1`)
- [ ] A test payment returns to `/registrationPaymentResult`

---

## Notes
- `Tahoma.jar` is still missing → a few PDF report cells may use a fallback font (non-blocking).
  See `CREDENTIALS_GUIDE.md` for how to build it.
- RabbitMQ management UI (15672) is not exposed; reach it via SSH tunnel if needed.
- Never commit `.env.production` (both repos gitignore it).
