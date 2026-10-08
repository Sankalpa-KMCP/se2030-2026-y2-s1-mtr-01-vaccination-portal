# Implemented physical ER excerpt

Deliberately scoped to shared auth tables and actual foreign keys in [V4](../../../backend/src/main/resources/db/migration/V4__shared_authentication_foundation.sql) and [V22](../../../backend/src/main/resources/db/migration/V22__add_email_verification.sql). Function-specific implemented data/constraints are linked from MF dossiers; this is not a complete database ERD.

```mermaid
erDiagram
  user_accounts ||--o{ user_roles : has
  roles ||--o{ user_roles : assigned
  user_accounts ||--o{ refresh_token_sessions : owns
  user_accounts ||--o| email_otp_challenges : current_challenge
  user_accounts {
    bigint id PK
    varchar email UK
    varchar password_hash
    varchar account_status
  }
  roles {
    bigint id PK
    varchar code UK
  }
  user_roles {
    bigint user_id PK, FK
    bigint role_id PK, FK
  }
  refresh_token_sessions {
    bigint id PK
    bigint user_id FK
    varchar token_family_id
    varchar token_hash UK
    bigint rotated_from_id
    timestamp expires_at
    timestamp revoked_at
    timestamp reuse_detected_at
  }
  email_otp_challenges {
    bigint id PK
    bigint user_id FK, UK
    varchar code_hash
    timestamp expires_at
  }
```

rotated_from_id is a stored predecessor identifier; V4 does not declare a self-FK. [V24](../../../backend/src/main/resources/db/migration/V24__add_refresh_token_sessions_family_revocation_index.sql) adds the (token_family_id, revoked_at) family-revocation index. Raw refresh tokens and OTPs are absent from persisted fields. The full runtime schema is V0–V26, 27 migrations.
