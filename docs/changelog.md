# Changelog — Mediaxa Business Suite

All changes and milestone sprints details are recorded in this document.

---

## [1.0.0-RC1] — 2026-06-27

### Added (SaaS & Sync Ready)
* **API Documentation**: OpenAPI 3.1 + Swagger UI configurations mounted at `/api/docs`.
* **Container Integration**: Multi-stage `Dockerfile` and `docker-compose.yml` to orchestrate Express, PostgreSQL, and Redis cache.
* **Winston Logs Isolation**: Segmented runtime log files (`application.log`, `request.log`, `sync.log`, `security.log`, `error.log`).
* **Sensitive Masking**: Automatic masking interceptor for `password`, `pin`, `accessToken`, and `refreshToken`.
* **Monitoring**: Exposed `/api/health`, `/api/version`, and Prometheus text-format `/api/metrics` routes.
* **Next.js Web Foundation**: Created dashboard boilerplate, Axios JWT auth renewal, protected routes, and MUI Material 3 theme.
* **High-Volume DB Seeder**: Optimized bulk data seeder (`src/utils/seeder.js`) populating stress test volumes in just 7.07s.
* **Android Crash Handler**: Custom global uncaught exception reporter saving dumps locally and pushing to cloud logs endpoint.

### Fixed
* Fixed TypeScript compilation warnings regarding MUI v6 Grid size components and typography stylings.
* Fixed table deletion order inside the database seeder to prevent SQL foreign key violations.
* Fixed safe req.body checks in tenant middleware to prevent TypeErrors on GET requests.
