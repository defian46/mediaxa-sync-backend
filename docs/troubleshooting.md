# Troubleshooting Manual — Mediaxa Business Suite

This guide lists common error codes, diagnostic checks, and recovery procedures.

---

## 1. Cloud Sync Failures (Red Warning alerts)

* **Symptom**: Android Sync Monitor dashboard displays "Sync Failed" warning counts.
* **Diagnostics**:
  1. Check device network connectivity. Verify the terminal is connected to Wi-Fi/data.
  2. Test API gateway health route: open `https://your-api-domain.render.com/api/health` in a browser.
  3. Verify device status: check if the device ID has been revoked in the backend administration panel.
* **Resolution**:
  - Re-authenticate the cashier to refresh expired JWT session tokens.
  - Check the backend `security.log` to see if requests are rejected due to invalid store UUID mismatch.

---

## 2. Prisma Database Migration Conflicts
* **Symptom**: `npx prisma db push` or `prisma migrate dev` returns target schema conflicts.
* **Resolution**:
  - If developing locally, reset SQLite: remove `mediaxa-sync-backend/prisma/dev.db` and run `npx prisma migrate dev --name init` to rebuild local schemas.
  - If deploying to PostgreSQL production, do not run dev migrations. Run:
    ```bash
    npx prisma migrate deploy
    ```
    This applies committed migrations safely without data loss.

---

## 3. SQLite Database Corruption Recovery (Android)
* **Symptom**: Android local database throws reading SQLite exception loops.
* **Resolution**:
  - Clean client storage: Clear App Storage & Cache in Android Settings.
  - Log in again. The Room database will be initialized from clean templates, and background workers will pull the latest cloud configurations automatically.
