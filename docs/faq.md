# Frequently Asked Questions (FAQ) — Mediaxa POS

### Q1: Is Room database completely safe to run offline?
**A**: Yes! SQLite Room database acts as the local source of truth on the cashier terminal. The POS is 100% operational offline, including checkout transactions, item queries, and stock calculations.

### Q2: What happens if client pushes identical transactions twice?
**A**: The backend utilizes the double-tracking `clientMutationId` inside `processed_mutation_logs`. If the same transaction mutation is pushed again, the server detects the duplicate and ignores it, preventing duplicate records.

### Q3: Are transactions mutable after sync checkout?
**A**: No. Financial entities (transactions, transaction items, payments) are strictly **immutable**. They cannot be modified or deleted via sync mutations. Corrections are logged as reverse/void log records.

### Q4: Does the web dashboard support offline modes?
**A**: No. The Next.js web dashboard is designed for managers to check analytical totals and inventory configurations online. It connects directly to the PostgreSQL database.
