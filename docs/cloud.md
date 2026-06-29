# Cloud Infrastructure — Mediaxa Business Suite

This document defines the deployment options, cloud services, and integration parameters.

---

## 1. Database Cloud Setup (Supabase / Neon)

### Option A: Supabase (Recommended)
1. Register for a free account at [Supabase](https://supabase.com/).
2. Create a new project named `mediaxa-pos`.
3. Locate the database connection string:
   * **Transaction Connection Mode (Port 6543)**: Recommended for serverless backends and Docker networks.
   * Format: `postgresql://postgres.[username]:[password]@aws-0-ap-southeast-3.pooler.supabase.com:6543/postgres?pgbouncer=true`

### Option B: Neon Database
1. Register at [Neon](https://neon.tech/).
2. Initialize a free cluster instance.
3. Retrieve connection URL:
   * Format: `postgresql://[username]:[password]@ep-soft-star-12345.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`

---

## 2. Cloud Server Instance (Render / Railway)

### Deploying to Render
1. Create a new **Web Service** on Render.
2. Link the repository path.
3. Configure settings:
   - **Environment**: `Node`
   - **Build Command**: `cd mediaxa-sync-backend && npm install && npx prisma generate`
   - **Start Command**: `cd mediaxa-sync-backend && npm start`
4. Set variables:
   - `DATABASE_URL`: Set to the Supabase connection string.
   - `JWT_SECRET`: Secure random string.
   - `NODE_ENV`: `production`
