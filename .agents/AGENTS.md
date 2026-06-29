# Global Development Rules - POS UMKM Offline

This application is a commercial product designed for scale. Follow these rules in all future development phases:

1. **Production Mindset**: Do not just write code that "works". Always prioritize:
   - Maintainability
   - Scalability
   - Performance
   - Security
   - SOLID Principles
   - Clean Architecture

2. **Zero Technical Debt**: Avoid hacks, hardcoded credentials, duplicate code, and placeholder implementations.

3. **Cloud & SaaS Readiness**: Every feature, schema modification, or architecture shift must account for future sync capabilities:
   - Multi-Store support (via `storeId` constraints)
   - Multi-Device support (via `deviceId` constraints)
   - Cloud Sync / Offline-First sync flows
   - SaaS multi-tenant compatibility
   - Backward compatibility with older database schemas
   - Seamless data migrations (no loss of local user records)
