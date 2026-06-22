# Klare Business — Frontend API Guide

Everything the frontend needs to build against the Klare Business backend: base URL, auth, the
response envelope, conventions, and every endpoint with its request and response.

- **Base URL (local):** `http://localhost:8080`
- **All API paths are prefixed:** `/api/v1`
- **Interactive docs:** `http://localhost:8080/docs` (Swagger) · `http://localhost:8080/v3/api-docs`

---

## 1. Conventions

### Response envelope
Every JSON response (success or error) has this shape:

```json
{ "success": true, "data": { /* payload */ }, "error": null, "timestamp": "2026-06-22T10:00:00Z" }
```

On error:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "violations": [{ "field": "administrator.password", "message": "must contain letters, numbers and a symbol" }],
    "traceId": "9f43...-..."
  },
  "timestamp": "2026-06-22T10:00:00Z"
}
```
`violations` is present only for field validation errors. `traceId` is for support/log correlation.

> **File downloads** (CSV/PDF) are the exception — they return the raw file with a
> `Content-Disposition: attachment` header, not the envelope.

### Authentication
- Log in (or register → verify → log in) to receive an **access token** and a **refresh token**.
- Send the access token on every protected request:
  `Authorization: Bearer <accessToken>`
- Access tokens expire in **15 minutes** (`expiresInSeconds: 900`). When a call returns `401`,
  call **`POST /auth/company/refresh`** with the refresh token to get a new pair, then retry.
- Store the refresh token securely; rotate it (each refresh returns a **new** refresh token and
  invalidates the old one — reusing an old one signs out all sessions).

### Error codes (HTTP status in parentheses)
| code | status | meaning |
|------|--------|---------|
| `VALIDATION_ERROR` | 400 | field validation failed (see `violations`) |
| `MALFORMED_REQUEST` | 400 | unreadable body |
| `INVALID_TOKEN` | 400 | invalid/expired verification, reset, or confirmation token |
| `UNAUTHORIZED` | 401 | missing/invalid/expired access or refresh token |
| `INVALID_CREDENTIALS` | 401 | wrong email/password |
| `EMAIL_NOT_VERIFIED` | 403 | must verify email before signing in |
| `ACCOUNT_INACTIVE` | 403 | account suspended/disabled |
| `FORBIDDEN` | 403 | authenticated but not allowed (role) |
| `ACCOUNT_LOCKED` | 423 | too many failed logins; locked temporarily |
| `NOT_FOUND` | 404 | resource not found |
| `CONFLICT` | 409 | duplicate / state conflict (e.g. email already used) |
| `IDEMPOTENCY_KEY_CONFLICT` | 409 | same Idempotency-Key reused with a different body |
| `IDEMPOTENCY_REQUEST_IN_PROGRESS` | 409 | a request with that key is still processing |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | bad upload type |
| `RATE_LIMITED` | 429 | too many requests (see `Retry-After` header) |
| `INTERNAL_ERROR` | 500 | unexpected server error |

### Idempotency
For mutating requests you want to make safe against retries (especially **register**, **fund wallet**,
**reset/change password**), send a header:
```
Idempotency-Key: <a fresh uuid you generate per user action>
```
Reuse the **same** key on retries of that exact action; generate a new one for a new action. A replay
returns the original response with `Idempotency-Replayed: true`.

### Rate limiting
On `429` the response includes `Retry-After` (seconds). Successful calls include
`X-Rate-Limit-Remaining`.

### Pagination
List endpoints return:
```json
{ "content": [ /* items */ ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3, "last": false }
```
Query params: `page` (0-based), `size`.

### Enums
- **Industry:** `TECHNOLOGY`, `RETAIL_AND_COMMERCE`, `FINANCIAL_SERVICES`, `MANUFACTURING`, `HOSPITALITY`, `OTHER`
- **PayrollBand:** `UNDER_10K`, `FROM_10K_TO_50K`, `FROM_50K_TO_200K`, `OVER_200K`
- **CompanyStatus:** `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`
- **BusinessUserRole:** `OWNER`, `ADMIN`, `HR`, `VIEWER`
- **EmployeeStatus:** `PENDING`, `ACTIVE`, `SUSPENDED`
- **WalletLinkStatus:** `PROVISIONING`, `LINKED`, `UNLINKED`
- **FundingStatus:** `AWAITING_OTP`, `AWAITING_APPROVAL`, `SUCCESS`, `FAILED`
- **PayrollRunStatus:** `PENDING_CONFIRMATION`, `PROCESSING`, `COMPLETED`, `CANCELLED`, `EXPIRED`, `FAILED`

Use `GET /auth/company/registration-options` for the human labels of Industry & PayrollBand.

---

## 2. Auth & onboarding  `/api/v1/auth/company`

These endpoints are **public** (no bearer token).

### POST `/register`
Creates the company (unverified) + owner admin, then emails a verification link. **Does not** return
tokens — the user must verify their email before logging in.

Request:
```json
{
  "company": {
    "name": "TechCorp Ltd",
    "registrationNumber": "CS-000-000",
    "industry": "TECHNOLOGY",
    "expectedMonthlyPayroll": "UNDER_10K"
  },
  "administrator": {
    "firstName": "Ama",
    "lastName": "Owusu",
    "email": "ama@techcorp.com",
    "phone": "+233054857203",
    "password": "Str0ng!Pass"
  },
  "consents": { "acceptedTerms": true, "authorizedFundMovement": true }
}
```
Rules: password ≥ 8 chars with letters + numbers + a symbol; both consents must be `true`; email &
registration number must be unique. Optionally send `Idempotency-Key`.

Response `201`:
```json
{ "email": "ama@techcorp.com", "message": "We've sent a verification link to your email. Click it to verify your company and sign in." }
```
→ Show a "Check your email" screen.

### GET `/verify-email?token=...`
Opened from the email link (browser navigation, not fetch). Verifies the company and **redirects
(302)** to `APP_LOGIN_URL?verified=success` (or `...?verified=invalid`). The frontend's login page
should read `?verified=` to show a success/failure banner.

### POST `/resend-verification`
Request `{ "email": "ama@techcorp.com" }` → `200` with a generic message (never reveals whether the
account exists).

### POST `/login`
Request:
```json
{ "email": "ama@techcorp.com", "password": "Str0ng!Pass", "rememberMe": true }
```
`rememberMe: true` → 30-day refresh token; `false` → ~12-hour session token.

Response `200` — **the standard auth payload** (also returned by `/refresh`):
```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",
  "expiresInSeconds": 900,
  "refreshToken": "mXZv6...",
  "refreshTokenExpiresAt": "2026-07-22T10:00:00Z",
  "user":   { "id": "uuid", "firstName": "Ama", "lastName": "Owusu", "email": "ama@techcorp.com", "role": "OWNER" },
  "company":{ "id": "uuid", "name": "TechCorp Ltd", "registrationNumber": "CS-000-000", "industry": "TECHNOLOGY", "expectedMonthlyPayroll": "UNDER_10K", "status": "ACTIVE" }
}
```
Errors: `401 INVALID_CREDENTIALS`, `403 EMAIL_NOT_VERIFIED`, `423 ACCOUNT_LOCKED` (after 5 failures),
`403 ACCOUNT_INACTIVE`.

### POST `/refresh`
Request `{ "refreshToken": "..." }` → `200` with a fresh auth payload (new access **and** new refresh
token). On `401`, send the user back to login.

### POST `/logout`
Request `{ "refreshToken": "..." }` → `200 { "message": "Signed out." }`. Always succeeds.

### POST `/forgot-password`
Request `{ "email": "ama@techcorp.com" }` → `200` generic message (enumeration-safe). Emails a reset link.

### POST `/reset-password`
Request `{ "token": "<from email>", "newPassword": "N3w!Str0ng" }` → `200 { "message": ... }`.
Single-use token; on success all sessions are revoked. Errors: `400 INVALID_TOKEN`, `400 VALIDATION_ERROR`.

### GET `/registration-options`
`200` with dropdown options for the registration form:
```json
{
  "industries":   [ { "value": "TECHNOLOGY", "label": "Technology" }, ... ],
  "payrollBands": [ { "value": "UNDER_10K", "label": "Under GHS 10,000" }, ... ]
}
```

---

## 3. Current account  `GET /api/v1/me`
Auth required. Returns the signed-in admin + company.
```json
{
  "user":    { "id": "uuid", "firstName": "Ama", "lastName": "Owusu", "email": "ama@techcorp.com", "role": "OWNER" },
  "company": { "id": "uuid", "name": "TechCorp Ltd", "registrationNumber": "CS-244-0091", "industry": "TECHNOLOGY", "expectedMonthlyPayroll": "UNDER_10K", "status": "ACTIVE" }
}
```

---

## 4. Dashboard  `GET /api/v1/dashboard/summary`
Auth required. One call powers the whole dashboard.
```json
{
  "greeting": { "firstName": "Ama", "companyName": "TechCorp Ltd" },
  "wallet":   { "balance": 80000.00, "pending": 0.00, "currency": "GHS" },
  "nextPayroll": {
    "date": "2026-06-28", "inDays": 6, "autoEnabled": true,
    "activeEmployees": 11, "totalToPay": 57800.00,
    "walletCoversInFull": true, "shortfall": 0
  },
  "lastPayroll": { "amount": 58700.00, "date": "2026-06-19", "successRate": 100, "employees": 11 },
  "stats": {
    "activeEmployees": 11, "pendingOnboarding": 1, "totalEmployees": 12,
    "addedThisMonth": 12, "totalMonthlyPayroll": 57800.00
  },
  "team": [ /* up to 5 EmployeeResponse (most recent) */ ]
}
```
`lastPayroll` is `null` if no payroll has been completed yet. Use `nextPayroll.walletCoversInFull` /
`shortfall` for the top-up banner.

---

## 5. Team / Employees  `/api/v1/employees`
Auth required. Mutations (create/update/import) require role `OWNER`/`ADMIN`/`HR`.

**EmployeeResponse** shape:
```json
{
  "id": "uuid", "firstName": "Kwame", "lastName": "Essien",
  "email": "kwame@techcorp.com", "phone": "+233241112233",
  "role": "Software Engineer", "monthlySalary": 5000.00,
  "status": "ACTIVE", "walletStatus": "LINKED", "createdAt": "2026-06-19T18:02:12Z"
}
```

### GET `/employees?q=&status=&page=0&size=20`
Paginated list (`PageResponse<EmployeeResponse>`). `q` searches name/email/phone.
`status` ∈ `ACTIVE|PENDING|SUSPENDED` (omit for all).

### GET `/employees/stats`
```json
{ "total": 12, "active": 10, "pending": 1, "suspended": 1 }
```

### POST `/employees`
Add one employee. `role` is **required**.
```json
{
  "firstName": "Kwame", "lastName": "Essien",
  "email": "kwame@techcorp.com", "phone": "+233241112233",
  "role": "Software Engineer", "monthlySalary": 5000.00,
  "sendInvitation": true
}
```
`sendInvitation` (default true) emails the employee an app invite. New employees start
`status: PENDING`, `walletStatus: PROVISIONING`. Response `200` → `EmployeeResponse`.
Errors: `409 CONFLICT` (email exists), `400 VALIDATION_ERROR`.

### GET `/employees/{id}` → `EmployeeResponse` (`404` if not in your company).

### PUT `/employees/{id}`
Full update (all fields required, incl. `role` and `status`):
```json
{
  "firstName": "Kwame", "lastName": "Essien", "email": "kwame@techcorp.com",
  "phone": "+233241112233", "role": "Senior Engineer",
  "monthlySalary": 5200.00, "status": "ACTIVE"
}
```

### POST `/employees/import?sendInvitations=true`  (multipart)
Bulk add via CSV or `.xlsx`. Form field: `file`. Columns: `name, email, phone, salary` (+ optional `role`).
Response `200`:
```json
{ "totalRows": 10, "imported": 8, "skipped": 2,
  "errors": [ { "row": 3, "email": "bad", "message": "invalid email" } ] }
```
Partial success — valid rows are created, invalid rows reported per row. Max 1000 rows.

### GET `/employees/import/template`
Downloads `klare-employee-template.csv` (`name,email,phone,salary,role`).

---

## 6. Payroll  `/api/v1/payroll`
Auth required; initiate/confirm/reconcile require `OWNER`/`ADMIN`.

### GET `/payroll`
```json
{
  "run": {
    "activeEmployees": 11, "totalToPay": 57800.00, "walletBalance": 80000.00,
    "coveragePercent": 100, "walletCoversInFull": true, "shortfall": 0
  },
  "schedule": {
    "automaticPayroll": true, "payDate": 28,
    "notifyEmployeesBeforePayday": true, "notifyLeadDays": 2, "status": "ACTIVE"
  },
  "history": [
    { "id": "uuid", "period": "May 2026", "periodYear": 2026, "periodMonth": 5,
      "runDate": "2026-05-28", "employees": 9, "successRate": 100,
      "totalPaid": 45000.00, "status": "COMPLETED" }
  ]
}
```
Disable the "Run payroll" button when `run.walletCoversInFull` is `false`. "Edit schedule" →
`PUT /settings/payroll-automation`.

### POST `/payroll/runs/initiate`
Starts a run and **sends a 6-digit SMS code** to the admin's phone. Response → confirm-modal data:
```json
{ "runId": "uuid", "employeeCount": 11, "totalAmount": 57800.00,
  "walletBalance": 80000.00, "maskedPhone": "****7203", "codeExpiresAt": "2026-06-22T10:10:00Z" }
```
Errors: `409 CONFLICT` ("no active employees" / "wallet does not cover this payroll").

### POST `/payroll/runs/{runId}/confirm`
Request `{ "code": "123456" }`. Verifies the code and **disburses to each active employee via Moolre**.
Response → `PayrollRunResponse` (the run with `successRate`, `status`). If any transfers are still
pending, `status` is `PROCESSING` (call reconcile later). Errors: `400 INVALID_TOKEN` (wrong code; 5
attempts cancels the run), `409 CONFLICT` (expired / already processed).

### POST `/payroll/runs/{runId}/reconcile`
Polls Moolre for pending transfers and finalizes the run → `PayrollRunResponse`.

### GET `/payroll/runs/{runId}/report`
Downloads a CSV (`employee,amount,status`).

---

## 7. Wallet  `/api/v1/wallet`
Auth required; fund actions require `OWNER`/`ADMIN`.

### GET `/wallet`
```json
{
  "balance": 80000.00, "pending": 0.00, "currency": "GHS",
  "companyName": "TechCorp Ltd",
  "settlementAccountMasked": "****1001",
  "bankTopUp": { "accountName": "Ama Owusu MLR", "accountNumber": "0020480001001", "bankName": "First Atlantic Bank" },
  "ledger": [
    { "date": "2026-06-19T18:28:29Z", "description": "Payroll run · 11 employees",
      "reference": "MLR-177DD36", "status": "Success", "direction": "DEBIT", "amount": 58700.00 }
  ]
}
```
`bankTopUp` is the company's dedicated Moolre virtual account — show it as the "Top up by bank
transfer" details (a bank transfer to it credits the wallet automatically). `bankTopUp` may be `null`
if provisioning hasn't completed.

### Funding flow (MoMo top-up)
The "Fund wallet" modal is a 3-step async flow:

1. **POST `/wallet/fund`** — `{ "payer": "0505678589", "amount": 1.00 }`
   → `FundingResponse`. Moolre sends an OTP to the payer's phone.
   ```json
   { "externalRef": "kf_...", "status": "AWAITING_OTP", "amount": 1.00,
     "payer": "0505678589", "otpRequired": true, "message": "..." }
   ```
2. **POST `/wallet/fund/{externalRef}/otp`** — `{ "otpcode": "123456" }`
   → verifies, then triggers the USSD charge prompt. `status` becomes `AWAITING_APPROVAL`.
3. After the payer approves the prompt on their phone, **GET `/wallet/fund/{externalRef}/status`**
   (poll) → `status` becomes `SUCCESS` and the wallet is credited (or `FAILED`).

`FundingStatus`: `AWAITING_OTP` → `AWAITING_APPROVAL` → `SUCCESS` / `FAILED`.

---

## 8. Transactions  `/api/v1/transactions`
Auth required. Unified ledger (top-ups + payroll + service fees).

### GET `/transactions?filter=ALL&q=&page=0&size=20`
`filter` ∈ `ALL | INFLOWS | PAYOUTS | FAILED`. `q` searches description/reference.
Returns `PageResponse<LedgerEntry>`:
```json
{ "content": [
    { "date": "2026-05-28T09:00:00Z", "description": "Payroll run · 9 employees",
      "reference": "MLR-893FB33", "status": "Success", "direction": "DEBIT", "amount": 45000.00 },
    { "date": "2026-05-28T09:00:00Z", "description": "Service fee · payroll",
      "reference": "MLR-893FB33F", "status": "Success", "direction": "DEBIT", "amount": 225.00 }
  ],
  "page": 0, "size": 20, "totalElements": 8, "totalPages": 1, "last": true }
```
`direction` is `CREDIT` (inflow, show `+`) or `DEBIT` (payout, show `-`).

### GET `/transactions/export?filter=ALL&q=`
Downloads `transactions.csv` for the current filter/search.

---

## 9. Reports  `/api/v1/reports`
Auth required.

### GET `/reports`
```json
{
  "stats": { "year": 2026, "totalPaid": 176400.00, "payrollRuns": 4, "employeesPaid": 11, "reportsGenerated": 4 },
  "reports": [
    { "id": "uuid", "kind": "PAYROLL_RUN", "title": "Payroll run report",
      "period": "May 2026", "records": "9 employees", "formats": ["CSV", "PDF"] },
    { "id": "tax-summary", "kind": "TAX_SUMMARY", "title": "Annual tax summary",
      "period": "2026 YTD", "records": "all runs", "formats": ["PDF"] }
  ]
}
```

### GET `/reports/payroll-runs/{runId}?format=csv|pdf`
Downloads the payroll run report (CSV or PDF). Use the `id` from a `PAYROLL_RUN` report item.

### GET `/reports/tax-summary?format=pdf`
Downloads the annual tax summary (PDF only; `csv` → `400`).

---

## 10. Settings  `/api/v1/settings`
Auth required; payroll-automation & company-profile require `OWNER`/`ADMIN`.

### GET `/settings`
```json
{
  "payrollAutomation": { "automaticPayroll": true, "payDate": 28,
    "emailEstimateBeforeRun": true, "notifyEmployeesBeforePayday": true },
  "companyProfile": { "companyName": "TechCorp Ltd", "registrationNumber": "CS-244-0091", "adminEmail": "ama@techcorp.com" }
}
```

### PUT `/settings/payroll-automation`
```json
{ "automaticPayroll": true, "payDate": 28, "emailEstimateBeforeRun": true, "notifyEmployeesBeforePayday": true }
```
`payDate` must be **1–28**. Returns the updated `payrollAutomation`.

### PUT `/settings/company-profile`
```json
{ "companyName": "TechCorp Ltd", "registrationNumber": "CS-244-0091", "adminEmail": "ama@techcorp.com" }
```
Updates company name, registration number, and the admin's login email. Returns the updated
`companyProfile`. Errors: `409 CONFLICT` (registration number / email already in use).

### POST `/settings/change-password`
```json
{ "currentPassword": "Str0ng!Pass", "newPassword": "N3w!Str0ngPass" }
```
`200 { "message": ... }`. Signs out other sessions. Errors: `401 INVALID_CREDENTIALS` (wrong current),
`400 VALIDATION_ERROR` (weak new password).

---

## 11. Suggested frontend flow

1. **Register** → show "Check your email".
2. User clicks the email link → backend verifies → redirects to your `/login?verified=success`.
3. **Login** → store `accessToken` (memory) + `refreshToken` (secure storage).
4. Attach `Authorization: Bearer <accessToken>` to all `/api/v1/**` calls (except the public
   `/auth/company/**`).
5. On `401`, call `/auth/company/refresh`; on its failure, redirect to login.
6. Build the dashboard from `GET /dashboard/summary`; each nav item maps to one resource group above.

---

## 12. Notes for production
- The **Sandbox/Live** toggle in the UI is informational for now; the active Moolre environment is a
  server setting (`MOOLRE_ENV`).
- Collections (MoMo & bank top-ups) require the Moolre account's **merchant/collections** to be
  enabled. Disbursements (payroll) require **API access** enabled.
- The email **verification link** points at `APP_API_BASE_URL`, which must be publicly reachable for
  real emails to work.
