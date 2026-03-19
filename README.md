# Apisentra

An IoT platform for monitoring insect activity. ESP32 devices perform on-device species detection using a lightweight YOLO model and report results to a central backend. Users manage their devices and view detection data through a web dashboard.

---

## Overview

The system follows an edge-compute model:

1. A user creates a device in the dashboard and receives a provisioning token
2. The token is transferred to an ESP32 via serial or BLE
3. The ESP32 registers with the backend and receives a long-lived API key
4. The device captures images, runs inference locally, and posts detection results
5. The backend validates, stores, and serves the results

Supported target species: *Apis mellifera* (honey bee), *Vespa crabro* (European hornet), *Vespa velutina nigrithorax* (Asian hornet).

---

## Architecture

```
web/
├── webapi/   # Spring Boot backend (REST API, device auth, database)
└── ux/       # React frontend (device dashboard, provisioning UI)
```

| Layer | Technology |
|-------|-----------|
| Backend | Java 25, Spring Boot 4, Spring Security, JPA/Hibernate |
| Database | H2 (default, in-memory) · PostgreSQL (production profile) |
| Frontend | React 19, Vite, Bootstrap 5, React Router |
| Device | ESP32, YOLO (on-device inference) |

---

## Prerequisites

- **Java 25** — for the backend (`./mvnw` wrapper is included, no separate Maven install needed)
- **Node.js + npm** — for the frontend

---

## Quick Start (Demo Mode)

The fastest way to run the full stack locally with no external dependencies.

**Terminal 1 — Backend:**
```bash
cd webapi
DEMO_MODE=true DDL_AUTO=create OTP_STUB=true ./mvnw spring-boot:run
```

**Terminal 2 — Frontend:**
```bash
cd ux
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

**Demo credentials:** `alice@test.com` (3 pre-seeded devices available)

To log in with OTP: enter the email, click *Send Code*, and the code will be shown on screen (no email required in demo mode).

> `DDL_AUTO=create` creates the database schema on startup. `DEMO_MODE=true` seeds the demo user and devices. `OTP_STUB=true` returns OTP codes in-band so no SMTP server is needed.

---

## Configuration

All settings can be provided as environment variables.

| Variable | Default | Description |
|----------|---------|-------------|
| `DDL_AUTO` | `none` | Hibernate DDL strategy (`create`, `create-drop`, `none`) |
| `DEMO_MODE` | `false` | Seed demo data on startup (`alice@test.com` + 3 devices) |
| `OTP_STUB` | `false` | Return OTP token in the API response instead of sending email |
| `REG_TOKEN_EXPIRY` | `600` | Provisioning token lifetime in seconds |
| `JDBC_URL` | `jdbc:h2:mem:espylah` | Database JDBC URL |
| `JDBC_USERNAME` | `sa` | Database username |
| `JDBC_PASSOWRD` | `password` | Database password |
| `MAIL_HOST` | `localhost` | SMTP host (used when `OTP_STUB=false`) |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | _(empty)_ | SMTP username |
| `MAIL_PASSWORD` | _(empty)_ | SMTP password |
| `MAIL_FROM` | `noreply@espylah.local` | From address on OTP emails |

---

## Running in Development

### Backend

```bash
cd webapi

# Run (H2 in-memory, schema must be created)
DDL_AUTO=create ./mvnw spring-boot:run

# Run with demo data and no-email OTP
DEMO_MODE=true DDL_AUTO=create OTP_STUB=true ./mvnw spring-boot:run

# Package as JAR
./mvnw clean package

# Build with PostgreSQL profile
./mvnw -P pgres clean install
```

The backend starts on **`http://localhost:8080`**.

### Frontend

```bash
cd ux

npm install        # First time only
npm run dev        # Dev server with hot reload
npm run build      # Production build
npm run preview    # Preview the production build
npm run lint       # Run ESLint
```

The dev server starts on **`http://localhost:5173`** and proxies all `/api/*` requests to `http://localhost:8080`.

---

## Running Tests

Tests use an H2 in-memory database and do not require a running server.

```bash
cd webapi

./mvnw test                          # Run all tests
./mvnw test -Dtest=ClassName         # Run a single test class
```

---

## Database

By default the backend uses an **H2 in-memory database** that resets on every restart. To persist data, point `JDBC_URL` at a file-based H2 or a PostgreSQL instance.

**PostgreSQL:**
```bash
./mvnw -P pgres spring-boot:run \
  JDBC_URL=jdbc:postgresql://localhost:5432/apisentra \
  JDBC_USERNAME=user \
  JDBC_PASSOWRD=secret
```

Schema must exist before running with `DDL_AUTO=none`. Use `DDL_AUTO=create` on first run to generate it.
