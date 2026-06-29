# API Endpoints Index — Mediaxa Business Suite

This document lists all active API paths versioned under `/api/v1` and their authentication scopes.

---

## 1. Authentication Moduel

### POST `/auth/login`
* **Description**: Validates user credentials, registers device ID, and issues access & refresh tokens.
* **Payload**:
  ```json
  {
    "username": "cashier_john",
    "password": "securepassword",
    "deviceId": "DEVICE-UUID-12345",
    "deviceName": "Cashier Phone Alpha"
  }
  ```
* **Returns**: Access tokens, refresh tokens, and store profile.

### POST `/auth/refresh`
* **Description**: Issue a new JWT access token.
* **Payload**:
  ```json
  { "refreshToken": "ref-xyz-123..." }
  ```

---

## 2. Synchronization Module

### POST `/sync/push`
* **Security**: Bearer JWT Token + Store Mismatch Middleware.
* **Description**: Pushes localized client SQLite changes to the cloud database.
* **Payload**:
  ```json
  {
    "schemaVersion": 1,
    "appVersion": "1.0.0",
    "storeUuid": "store-uuid-999",
    "deviceId": "DEVICE-UUID-12345",
    "userUuid": "user-uuid-888",
    "mutations": [...]
  }
  ```

### GET `/sync/pull`
* **Security**: Bearer JWT Token.
* **Description**: Returns all changes since lastSyncTime using cursor pagination offsets.
* **Query Parameters**:
  - `storeUuid` (String, Required)
  - `lastSyncTime` (Number/Timestamp)
  - `cursor` (String, Optional)
  - `limit` (Number, Default: 50)
