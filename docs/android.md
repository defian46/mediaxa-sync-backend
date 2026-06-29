# Android Client Configuration — Mediaxa Business Suite

This document defines the local Room database structure, ViewModel bindings, and background workers scheduling of the offline client.

---

## 1. Local Database (Room)

* **Source of Truth**: The client treats the local Room database (`AppDatabase`) as the source of truth for all business screens.
* **Tables**: Mirror the PostgreSQL tables (using Kotlin `@Entity` annotation).
* **Sync Queue**: Local mutations are logged inside the `sync_queue` table with the following parameters:
  - `clientMutationId`: UUID generated on modification.
  - `entityType`: The entity class name (e.g., `CATEGORY`, `TRANSACTION`).
  - `operation`: `CREATE`, `UPDATE`, or `DELETE`.
  - `payload`: JSON string representation of the modified entity.
  - `updatedAt`: Device epoch timestamp.

---

## 2. Sync WorkManager (`SyncWorker`)

All network communication with the cloud backend is offloaded to Android's `WorkManager`:
* **Scheduling**: Triggered in `MainApplication.onCreate()` via `SyncScheduler.schedulePeriodicSync()`.
* **Interval**: Every **15 minutes** (minimum interval allowed by Android OS).
* **Constraints**:
  - Requires **Network Type: CONNECTED**.
  - Restricts execution if the device has low battery (optional).
* **Execution Flow**:
  1. Retrieve pending mutations from `SyncQueue` sorted by timestamp.
  2. Send batch payloads to `/api/v1/sync/push`.
  3. If response status is success, clear sent rows from `SyncQueue`.
  4. Fetch increment updates from `/api/v1/sync/pull` and save them into the local Room database.
