# 🔎 QuerySense

A natural-language-to-SQL analytics API. Users upload a dataset (CSV), then ask questions
about it in plain English — *"total revenue by region"*, *"top 5 customers by spend"* — and
get back real query results. A hosted LLM (Groq) translates English into SQL; everything that
makes that **safe and production-grade** is deterministic backend engineering. Built with
Spring Boot, Spring Security, Spring AI, Redis, and PostgreSQL, with a React frontend.

🌐 **Live demo:** **https://query-sense-frontend.vercel.app**
🔌 **Live API:** **https://querysense-api.onrender.com/ping**
⏳ *Note: the API runs on a free tier that sleeps when idle — the first request after a quiet
period can take ~50 seconds to wake up, then it's fast.*

📦 **Stack:** Java 17 · Spring Boot 4 · Spring AI (Groq) · JSQLParser · PostgreSQL (Neon) · Redis (Upstash) · React 18 · Docker · Render · Vercel

> **The core idea:** the AI does exactly *one* narrow job — turning a question into a SQL
> `SELECT`. It is never trusted. Every generated query is run through a deterministic safety
> pipeline (parse → single-statement → SELECT-only → tables-must-exist), executed by a
> **read-only** database user, and recorded in an audit log. The "AI" is a small, replaceable
> part; the engineering around it is the project.

---

## What It Does

- 📤 **Upload your own data** — drop a CSV; it's parsed, typed, and loaded into a real table
- 🗣️ **Ask in plain English** — questions are translated to SQL by a hosted LLM (Groq)
- 🛡️ **Multi-layer SQL safety** — every query is parsed and validated before it touches the DB
- 🔒 **Read-only execution** — queries run as a DB user that physically cannot write or alter data
- 👤 **JWT auth & registration** — secure login with BCrypt-hashed passwords and signed tokens
- 🧑‍✈️ **Role-based access (RBAC)** — admin-only endpoints for users and the audit trail
- ⚡ **Redis caching & rate limiting** — repeat questions return instantly; per-user request caps
- 📜 **Audit logging** — every query attempt (success or blocked) is recorded with who/what/when
- 📄 **Pagination** — large result sets are capped and paged
- 🖥️ **React frontend** — clean, professional SPA: upload, query, results table, admin views

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend Language | Java 17 |
| Backend Framework | Spring Boot 4.0.6 |
| Security | Spring Security 7, JWT (JJWT 0.12.6), BCrypt |
| AI / NL→SQL | Spring AI · Groq (`llama-3.3-70b-versatile`, hosted, OpenAI-compatible) |
| SQL Safety | JSQLParser 5.1 (parse + validate generated SQL) |
| Database | PostgreSQL (app DB + analytics DB) · Hibernate 7 · HikariCP |
| Cache / Rate limiting | Redis (caching + fixed-window per-user limits) |
| JSON | Jackson 3 (`tools.jackson`) |
| Frontend | React 18 · Vite 5 · Tailwind CSS 3 |
| Frontend Libraries | Framer Motion · lucide-react |
| Container | Docker (multi-stage build; PostgreSQL + Redis via Compose locally) |
| CSV Ingestion | Apache Commons CSV |
| Hosting (live) | Render (API) · Vercel (frontend) · Neon (PostgreSQL) · Upstash (Redis) |

---

## Architecture

A single Spring Boot service backed by **three datasources** (one for app data, two for the
analytics database — a privileged one for ingestion and a read-only one for querying), plus a
React frontend. In production the LLM is a **hosted Groq API** (no GPU needed), and Postgres
and Redis are managed cloud services.

```
                    ┌────────────────────────────┐
                    │   querysense-frontend (SPA) │   React · Vite · Tailwind  (Vercel)
                    └──────────────┬─────────────┘
                                   │  /auth/*  ·  /api/*   (Bearer JWT)
                                   ▼
                    ┌────────────────────────────┐
                    │      QuerySense API         │   Spring Boot  (Render, Docker)
                    │  Auth · NL→SQL · Safety ·   │
                    │  Cache · Audit · Ingest     │
                    └──┬─────────┬─────────┬──────┘
            JWT/RBAC   │  Groq   │  Redis  │  JDBC
                       │ (NL→SQL)│(cache + │
                       ▼         │ ratelim)▼
              ┌──────────────┐   │   ┌──────────────────────────────┐
              │ Postgres(app)│   │   │       Postgres (analytics)     │
              │ users ·      │   │   │  ┌────────────┐ ┌───────────┐ │
              │ audit_logs   │   ▼   │  │ read-only  │ │  admin    │ │
              └──────────────┘ Redis │  │ (queries)  │ │ (ingest)  │ │
                  (Neon)     (Upstash)│  └────────────┘ └───────────┘ │
                                      │            (Neon)              │
                                      └────────────────────────────────┘
                       Groq = hosted LLM API (api.groq.com), called over HTTPS
```

```
querySense/
├── pom.xml
├── Dockerfile                  ← Multi-stage build (Maven → JRE) for cloud deploy
├── docker-compose.yml          ← Local infra: PostgreSQL :5440 + Redis :6379
├── db-init/                    ← Creates the analytics DB + read-only user on first boot
├── src/main/java/com/querySense/
│   ├── auth/        ← JWT, DB-backed users, registration, RBAC, admin endpoints
│   ├── nlsql/       ← English→SQL service + the /api/query controller
│   ├── safety/      ← JSQLParser validation (single-statement, SELECT-only, tables-exist)
│   ├── schema/      ← Live schema registry (cached, refreshed on upload)
│   ├── execution/   ← Read-only SQL execution + pagination
│   ├── cache/       ← Redis result caching + per-user rate limiting
│   ├── audit/       ← Audit logging of every query attempt
│   ├── ingest/      ← CSV upload → table creation (privileged datasource)
│   └── common/      ← Health check + global JSON error handler
├── src/main/resources/
│   └── application.yaml         ← All settings via ${ENV_VAR:local-default}
└── querysense-frontend/        ← React + Vite + Tailwind SPA
```

### Package Layout (`com.querySense`)

| Package | Purpose | Example |
|---------|---------|---------|
| `auth/` | Login, registration, JWT, roles, admin views | `JwtService`, `AuthController`, `DbUserDetailsService`, `AdminController` |
| `nlsql/` | Turns English into SQL and runs the query pipeline | `NlToSqlService`, `QueryController` |
| `safety/` | Deterministic validation of generated SQL | `SqlSafetyValidator`, `SchemaValidator`, `UnsafeSqlException` |
| `schema/` | Reads the live DB schema for the AI prompt + validation | `SchemaService` |
| `execution/` | Executes validated SQL read-only, with paging | `SqlExecutionService` |
| `cache/` | Redis caching + fixed-window rate limiting | `QueryCacheService`, `RateLimitService` |
| `audit/` | Append-only record of every query attempt | `AuditService`, `AuditLog`, `AuditRepository` |
| `ingest/` | Parse CSV, create table, bulk-insert rows | `CsvIngestService`, `UploadController`, `AnalyticsAdminDataSourceConfig` |
| `common/` | Health endpoint + global error handling | `HealthController`, `GlobalExceptionHandler` |

---

## How a Query Works

```
1. Client sends POST /api/query with a JWT and { question, page, size }.
2. Rate limit (Redis INCR + EXPIRE, per username): over the limit → 429.
3. Cache check (Redis, key = question + page + size, 10-min TTL):
   - Hit  → return the cached rows instantly (no AI, no DB).
   - Miss → continue.
4. NL→SQL: the live schema (table + column names) and the question are sent to Groq,
   which returns a single SELECT. The response is cleaned (markdown / stray semicolons).
5. Safety pipeline (deterministic, JSQLParser):
   - Must parse as valid SQL.
   - Must be exactly ONE statement (no stacked queries).
   - Root statement must be a SELECT (no INSERT / UPDATE / DELETE / DROP).
   - Every referenced table must exist in the schema (blocks hallucinated tables).
6. Execution: the validated SELECT is wrapped as a subquery with LIMIT/OFFSET and run
   through the READ-ONLY datasource.
7. The result is cached, and a SUCCESS (or BLOCKED) row is written to the audit log.
8. Response: { question, generatedSql, page, size, rowCount, rows, cached }.
```

Key safety guarantees:

- **The AI is never trusted.** Its output is treated as an untrusted string and must survive the full validation pipeline before running.
- **Single statement only.** Stacked queries like `... ; DROP TABLE ...` are rejected at parse time.
- **SELECT-only.** Anything that isn't a read is rejected before reaching the database.
- **No hallucinated tables.** Generated SQL referencing a table that doesn't exist is blocked.
- **Read-only at the database level.** Queries execute as a DB user with `SELECT`-only grants — so even a validation gap can't mutate data. Ingestion uses a *separate*, privileged connection that the query path never touches.
- **Every attempt is audited.** Successes and blocks are recorded with the username, question, generated SQL, and outcome.

> Defense in depth: in testing, when the model was asked to do something destructive it
> refused and returned prose — which then failed the SQL parser and was blocked anyway. Two
> independent layers would both have to fail for anything unsafe to run.

---

## Databases

Two databases, clean separation of concerns. In production these are hosted on **Neon**
(managed PostgreSQL); locally they run in Docker.

| Database | Table | Stores | Notes |
|----------|-------|--------|-------|
| `querysense` (app) | `users` | Accounts | `username` (unique), `password` (BCrypt hash), `role` |
| `querysense` (app) | `audit_logs` | Query trail | `username`, `question`, `generated_sql`, `status`, `row_count`, `detail`, `created_at` |
| `analytics` | *(user-uploaded)* | Uploaded datasets | Created on the fly from CSV; `SELECT` granted to the read-only user |

The `analytics` database is reached two ways: a **read-only** user (`analytics_readonly`,
`SELECT` only) for running queries, and a **privileged** connection used solely by the
ingestion path to create tables and insert rows.

---

## API Endpoints

| Method | Endpoint | Auth | What It Does |
|--------|----------|------|--------------|
| `POST` | `/auth/register` | public | Create a user (BCrypt-hashed) → `201` / `409` if taken |
| `POST` | `/auth/login` | public | Log in → returns a JWT |
| `POST` | `/api/upload` | JWT (multipart) | Upload a CSV → creates a queryable table |
| `POST` | `/api/query` | JWT | Ask a question → generated SQL + result rows |
| `GET` | `/api/admin/users` | JWT + `ADMIN` | List all users (no password hashes) |
| `GET` | `/api/admin/audit` | JWT + `ADMIN` | Recent query audit log |
| `GET` | `/ping` | public | Health check |

### Example

```bash
# Against the live API (or swap in http://localhost:8084 for local)
BASE=https://querysense-api.onrender.com

# 1. Log in
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | jq -r .token)

# 2. Upload a dataset
curl -s -X POST $BASE/api/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@sales.csv" -F "table=sales"

# 3. Ask a question
curl -s -X POST $BASE/api/query \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"total revenue by region"}'
```

---

## Getting Started Locally

### Prerequisites
- Java 17
- Maven 3.9+
- Docker
- Node.js 18+ (for the frontend)
- A free **Groq API key** — sign up at [console.groq.com](https://console.groq.com) and create a key

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/MayankHarnotiya/querySense.git
cd querySense

# 2. Provide your Groq API key (the app reads GROQ_API_KEY)
export GROQ_API_KEY=gsk_your_key_here          # Windows PowerShell: $env:GROQ_API_KEY="gsk_..."

# 3. Start infrastructure (PostgreSQL :5440, Redis :6379)
docker compose up -d

# 4. Run the API  → http://localhost:8084
mvn spring-boot:run

# 5. Run the frontend
cd querysense-frontend
npm install
npm run dev             # → http://localhost:5173
```

Then open the frontend, register an account, upload a CSV, and ask a question. The Vite dev
server proxies `/api` and `/auth` to the backend, so there's no CORS to configure locally.

Key local settings live in `application.yaml`, all expressed as `${ENV_VAR:local-default}`:
PostgreSQL on `5440`, Redis on `6379`, the app on `8084`, the Groq base URL under
`spring.ai.openai.base-url` (`https://api.groq.com/openai/v1` — the `/v1` is required), and
the model under `spring.ai.openai.chat.options.model` (`llama-3.3-70b-versatile`).

> **First admin:** registration always creates a `USER`. To grant yourself admin, run
> `UPDATE users SET role='ADMIN' WHERE username='<you>';` against the `querysense` database,
> then log out and back in so the new JWT carries the role.

---

## Deployment

> Status: **live.** ✅ The whole stack runs on free tiers, co-located in one region. The only
> cloud-specific change from local was the LLM: because the project is built on **Spring AI's
> provider abstraction**, the local Ollama model was swapped for the hosted **Groq API** with
> a dependency + config change rather than a rewrite. The backend is containerized with a
> multi-stage Dockerfile, and all secrets/URLs are injected as environment variables.

| Component | Platform | Notes |
|-----------|----------|-------|
| **Frontend** | **Vercel** | Static Vite build of `querysense-frontend`; backend URL via `VITE_API_BASE` |
| **API** | **Render** | Spring Boot in Docker; LLM via hosted Groq API; free tier sleeps when idle (~50s cold start) |
| **App + Analytics DB** | **Neon** (managed PostgreSQL) | Users, audit, and uploaded datasets; read-only + privileged roles |
| **Cache** | **Upstash** (managed Redis) | Caching + rate-limit counters, over TLS |

Cross-origin requests from the Vercel frontend to the Render API are handled by a CORS
configuration whose allowed origins are supplied via the `CORS_ALLOWED_ORIGINS` environment
variable (no code change to add a new origin).

A planned hardening step for multi-user deployment is isolating each user's uploads into
their own PostgreSQL schema, so datasets never collide or leak across accounts.

---

## Author

**Mayank Harnotiya**
Backend Software Engineer · Java · Spring Boot · AWS

- 📧 [mayankharnotiya25@gmail.com](mailto:mayankharnotiya25@gmail.com)
- 💼 [LinkedIn](https://www.linkedin.com/in/mayankharnotiya/)
- 💻 [GitHub](https://github.com/MayankHarnotiya)

Available for full-time opportunities · Immediate joiner

---

*Built as a personal project to explore how to put an LLM behind a real, safe backend — where
the model is treated as untrusted input and every layer (validation, read-only execution,
auth, auditing) is engineered to contain it.*
