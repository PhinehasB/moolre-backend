# Klare — Business Backend

**Your money, already sorted.**

Klare is an automated salary management and bill-settlement platform built on the
[Moolre](https://moolre.com) payment ecosystem for the Ghanaian market. This repository is
**`klare-server`**, the backend that powers **Klare Business** — the web dashboard employers use to
manage their team, fund a wallet, and run payroll automatically every payday.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3.5 (Web, Security, Validation, Data JPA, Mail, Actuator) |
| Database | PostgreSQL (Neon), Hibernate `ddl-auto: update` (entities drive the schema — no manual migrations) |
| Auth | JWT (access + rotating refresh tokens), BCrypt-12 |
| Payments | Moolre (Collections, Disbursements, virtual bank accounts) |
| Docs | springdoc OpenAPI / Swagger UI |
| Other | Bucket4j (rate limiting), Apache POI (Excel import), OpenPDF (PDF reports), jjwt |

## Prerequisites

- Java 17 (`/usr/libexec/java_home -v 17` on macOS)
- A PostgreSQL database (Neon works out of the box)
- A Moolre account with **API access enabled** and **collections (merchant) activated**
- An SMTP provider (Gmail App Password works for testing) for verification / reset emails


> **Money model:** Klare uses **one master Moolre account** that holds all funds. Each company gets an
> internal `CompanyWallet` ledger plus a dedicated **virtual bank account** (Moolre `type=9`) for
> bank-transfer top-ups. Inflows/outflows are attributed per company via `externalref` and the virtual
> account number.

## Run

```bash
./mvnw spring-boot:run          # starts on http://localhost:8080
./mvnw test                     # boots the full context (smoke test against the DB)
```

- **Swagger UI:** http://localhost:8080/docs
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Health:** http://localhost:8080/actuator/health

## Features (Klare Business — Phase 1, complete)

- **Auth & onboarding** — company registration, **email verification**, login with
  brute-force lockout + "remember me", token refresh (rotation + reuse detection), logout,
  forgot/reset password.
- **Dashboard** — wallet balance, next automatic payroll, coverage, headcount stats,
  "paid last month", team preview.
- **Team** — paginated/searchable/filterable employee list, add (with role + invite), edit,
  per-status counts, **bulk CSV/Excel import** with row-level validation + a downloadable template.
- **Payroll** — run summary + coverage, **SMS-OTP confirmed** runs, **real Moolre bulk
  disbursement** (one transfer per active employee), reconciliation of pending transfers, CSV report.
- **Wallet** — balance + ledger, **two top-up methods**: MoMo (USSD prompt) and **bank transfer**
  (per-company Moolre virtual account), webhook crediting.
- **Transactions** — unified ledger (top-ups, payroll, service fees) with filter / search / CSV export.
- **Reports** — yearly stats + per-run reports (CSV & **PDF**) + annual tax summary (PDF).
- **Settings** — payroll automation (auto-pay, pay date, notifications), company profile, change password.

## Cross-cutting

- **Response envelope** — every JSON response is `{ success, data, error, timestamp }`.
- **JWT** — `Authorization: Bearer <accessToken>` on all protected routes; 15-minute access tokens.
- **Idempotency** — send an `Idempotency-Key` header on mutating requests to make retries safe.
- **Rate limiting** — per-IP buckets (stricter on `/auth/**`); `429` with `Retry-After`.
- **Role-based** — sensitive mutations are guarded to `OWNER`/`ADMIN`.

See **[docs/FRONTEND_API.md](docs/FRONTEND_API.md)** for the complete request/response reference
the frontend should build against.

## Project structure

```
com.project.klare_server
├── common/        envelope, errors, security config, rate-limit, idempotency, validation, persistence
├── moolre/        MoolreClient, properties, Ghana MoMo channel helpers
├── auth/          registration, login, email verification, password reset, JWT, /me
├── company/       Company + CompanyWallet
├── employee/      employees: CRUD, search, bulk import
├── dashboard/     dashboard summary aggregation
├── payroll/       payroll runs, OTP confirm, disbursement, reconciliation, reports
├── wallet/        wallet, MoMo + bank top-up, Moolre webhook
├── transactions/  unified ledger, filters, export
├── reports/       stats + CSV/PDF reports
└── settings/      payroll automation, company profile, password
```

Built on Moolre · Moolre Startup Cup 2026.
