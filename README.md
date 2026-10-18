# HRMS

A full-stack Human Resource Management platform covering employee attendance, leave, timesheets and role-based access control, with real-time notifications and a production-style observability stack.

**Stack:** Java 21 · Spring Boot · Spring Security (JWT + OAuth2) · JPA/Hibernate · PostgreSQL · Redis · Kafka · WebSocket · React · TypeScript · Vite · Tailwind CSS · shadcn/ui · Docker · Prometheus · Grafana

## Features

- **Attendance:** clock in/out, breaks and monthly reports
- **Leave management:** employees submit leave requests; managers get in-app and email notifications and approve or reject
- **Timesheets:** submit, approve and reject flows with email notifications
- **RBAC:** role-based access control with Super Admin and assignable roles
- **Authentication:** email sign-up with verification, sign-in, refresh-token rotation, logout, forgot/reset password, plus Google and GitHub OAuth sign-in
- **Real-time notifications:** WebSocket-driven notification bell
- **Asynchronous email:** verification, reset, leave, timesheet and reminder emails are sent through Kafka consumers over real SMTP (set `MAIL_ENABLED=false` to log instead during development)
- **Audit logs:** admin endpoint at `/api/v1/admin/audit-logs`
- **Rate limiting:** 20 requests/minute on `/api/v1/auth/*`
- **Observability:** Prometheus scrapes `/actuator/prometheus`; a Grafana dashboard is pre-provisioned
- **API docs:** Swagger UI

## Architecture

```
React + TypeScript (Vite)  ──REST / WebSocket──▶  Spring Boot API
                                                    │
                       ┌────────────┬───────────────┼───────────────┐
                       ▼            ▼               ▼               ▼
                  PostgreSQL      Redis           Kafka        Prometheus ▶ Grafana
                  (data, RBAC)  (cache / limits)  (email &
                                                  notification
                                                  consumers)
```

## Repository layout

| Path | Contents |
|---|---|
| `backend/` | Spring Boot service (Maven, Dockerfile) |
| `frontend/` | React + TypeScript web app |
| `docker/` | Docker Compose stack: Postgres, Redis, Kafka, Prometheus, Grafana, Kafka UI |
| `supabase/migrations/` | Original schema migrations, kept as the schema source of truth |
| `README-SETUP.md` | Full setup, OAuth, email and verification guide |

## Quick start

Prerequisites: Java 21, Maven 3.9+, Node 20+, Docker Desktop.

```bash
cp .env.example .env        # fill in database, JWT_SECRET, mail and OAuth values
docker compose -f docker/docker-compose.yml up -d --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:8080 |
| Backend API | http://localhost:8081/api/v1 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 |
| Kafka UI | http://localhost:8085 |

### Local development

```bash
# infrastructure only
docker compose -f docker/docker-compose.yml up -d postgres redis kafka

# backend
cd backend && mvn spring-boot:run

# frontend (Vite proxies /api to :8081)
cd frontend && npm install && npm run dev
```

The first admin user, OAuth setup, email configuration and a full verification checklist are in [README-SETUP.md](README-SETUP.md).

## Author

Adusumalli Sai Pavan: [GitHub](https://github.com/SaiPavan122001) · [LinkedIn](https://www.linkedin.com/in/adusumalli-sai-pavan-111836208/) · [Portfolio](https://saipavan122001.github.io/Pavan-Portfolio/)
