# Backend Sync Engine Guide — Mediaxa Business Suite

This document defines the backend server structure, controller patterns, and security middlewares.

---

## 1. Directory Blueprint

* `src/app.js`: Express server initialization and middleware integration.
* `src/server.js`: Web entry point.
* `src/middlewares/`:
  - `auth.middleware.js`: JWT signature verification and device revocation validation.
  - `tenant.middleware.js`: Multi-store boundary isolation verification.
  - `request.logger.js`: Winston log file generation.
* `src/services/`:
  - `auth.service.js`: Session management and Bcrypt credentials check.
  - `sync.service.js`: Conflict resolution engine and pagination query builders.

---

## 2. Express Pipeline Middleware

All synchronization routes `/api/v1/sync/*` are guarded by the following execution sequence:

```
Incoming Request
  │
  ▼
Request Logger (Generates requestId and starts timer)
  │
  ▼
Rate Limiter (Max 200 requests / 15 minutes)
  │
  ▼
JWT Auth Middleware (Extracts storeUuid, deviceId and validates active registration)
  │
  ▼
Tenant Verification (Checks storeUuid inside payload matches JWT claim)
  │
  ▼
Zod Validator Schema (Strict properties and type checks)
  │
  ▼
Controller Endpoint Execution
```
---

## 3. Database Isolation Level
To ensure concurrency safety and prevent transaction duplication, Prisma queries in the sync service run within standard transactional batches (`prisma.$transaction`).
