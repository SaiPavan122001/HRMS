# HRMS - Setup Guide (Spring Boot + PostgreSQL, no Supabase)

## 1. Prerequisites
- Java 21, Maven 3.9+ (or run via Docker)
- Node 20 / npm
- Docker Desktop (for Postgres, Redis, Kafka, Prometheus, Grafana)

## 2. Environment
Copy `.env.example` to `.env` and fill in:
- DATABASE_URL / USERNAME / PASSWORD
- JWT_SECRET (random 256-bit)
- MAIL_* (SendGrid: host smtp.sendgrid.net, port 587, username `apikey`, password = SG.xxx, MAIL_FROM = a verified sender)
- GOOGLE_CLIENT_ID/SECRET and GITHUB_CLIENT_ID/SECRET (OAuth)
- FRONTEND_URL, CORS_ALLOWED_ORIGINS

## 3. Start infrastructure + app (Docker)
```
docker compose -f docker/docker-compose.yml up -d --build
```
- Frontend:   http://localhost:8080
- Backend API:http://localhost:8081/api/v1
- Swagger:    http://localhost:8081/swagger-ui.html
- Prometheus: http://localhost:9090
- Grafana:    http://localhost:3001 (admin / GRAFANA_PASSWORD, dashboard pre-provisioned)
- Kafka UI:   http://localhost:8085

## 4. Run locally (dev)
```
# terminal 1 - infra only
docker compose -f docker/docker-compose.yml up -d postgres redis kafka

# terminal 2 - backend (loads .env via shell/IDE)
cd backend && mvn spring-boot:run

# terminal 3 - frontend (vite proxies /api -> :8081)
cd frontend && npm run dev
```

## 5. First admin user
Sign up via the UI, verify your email, then promote yourself:
```sql
UPDATE user_roles SET role = 'SUPER_ADMIN'
WHERE user_id = (SELECT user_id FROM profiles WHERE email = 'you@company.com');
```
Also assign the RBAC role:
```sql
INSERT INTO user_role_assignments (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'you@company.com' AND r.name = 'Super Admin';
```

## 6. OAuth setup
- Google Cloud Console: OAuth client (Web). Redirect URI: `http://localhost:8081/login/oauth2/code/google`
- GitHub Developer settings: OAuth App. Callback: `http://localhost:8081/login/oauth2/code/github`

## 7. Email
All emails (verification, reset, leave/timesheet notifications, reminders) are real SMTP
via the configured provider and are sent asynchronously through Kafka consumers.
Set MAIL_ENABLED=false to log instead of send during development.

## 8. Data migration from Supabase (optional)
The original Supabase migrations remain in `supabase/migrations/` as the schema source of
truth. To import existing data, export each table from Supabase as CSV and copy into the
corresponding PostgreSQL table (users, profiles, attendance, leave_*, timesheet*, roles...).
Supabase password hashes (bcrypt) can be copied into `users.password` as-is - Spring
Security BCrypt verifies them transparently.

## 9. Verification checklist
1. Sign up -> receive real verification email -> verify -> sign in -> dashboard
2. Submit leave -> manager gets in-app + email notification -> approve -> employee notified
3. Timesheet submit/approve/reject flows + emails
4. Clock in/out/break; monthly report
5. WebSocket notifications appear live (notification bell)
6. Prometheus scrapes /actuator/prometheus; Grafana dashboard renders
7. Audit logs at /api/v1/admin/audit-logs
8. Refresh token rotation, logout, forgot/reset password
9. Google/GitHub OAuth sign-in
10. Rate limiting on /api/v1/auth/* (20 req/min)
