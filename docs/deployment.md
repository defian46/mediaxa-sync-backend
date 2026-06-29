# Deployment & Running Instructions — Mediaxa Business Suite

This document defines the instructions to run, build, and deploy the backend gateways and database containers.

---

## 1. Running Locally (No Docker)

1. Navigate to the backend directory:
   ```bash
   cd mediaxa-sync-backend
   ```
2. Copy the example environment template:
   ```bash
   cp .env.example .env
   ```
3. Set your postgres database URL connection parameters inside `.env`.
4. Install package dependencies:
   ```bash
   npm install
   ```
5. Push schema migration and generate client:
   ```bash
   npx prisma db push
   ```
6. Start the Express server:
   ```bash
   npm run dev
   ```

---

## 2. Docker Compose (Recommended for Local Dev Testing)

Ensure you have Docker installed. To run the API, PostgreSQL, and Redis cache together in an isolated network:
```bash
cd mediaxa-sync-backend
docker compose up --build
```
* **Ports Exposed**:
  - API Gateway: `http://localhost:3000`
  - PostgreSQL Database: `localhost:5432`
  - Redis: `localhost:6379`
* **Stop Container Services**:
  ```bash
  docker compose down
  ```

---

## 3. Production Deployment (Cloud Sync Deploy)
1. Run database schema migrations:
   ```bash
   DATABASE_URL="your-supabase-connection-url" npx prisma migrate deploy
   ```
2. Link your Git repository path to Render or Railway.
3. Configure environment parameters (`DATABASE_URL`, `JWT_SECRET`, `NODE_ENV=production`).
4. Trigger server build and run commands.
