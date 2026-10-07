# Shared authentication evidence

Supporting/cross-cutting capability, not an additional assessed function.

WBVP-SE2030; canonical business reference 3.3-AI / 2026-08-14-R9. Current implementation: `origin/develop` at `444364d4b742fa08455c4acf0538baa836e9529a` (2026-10-07 audit).

## Implemented contract

Access JWTs are signed and checked by Spring Security; RBAC and resource ownership remain server-side controls. The default access lifetime is 15 minutes. Frontend access token, stable server account ID and session generation are module-memory state; none is persisted in localStorage/sessionStorage. Login establishes a new generation/principal binding.

Email verification supports action-token links and the citizen OTP flow. Recovery responses avoid exposing account existence; successful password reset consumes its action token and revokes all refresh sessions. Login lockout has timed and account-state controls; refresh rechecks account eligibility rather than treating possession of a cookie as sufficient authorization.

Refresh tokens use 32 CSPRNG bytes (256 bits), opaque encoding and hash-only persistence. Lifetime is seven days. The HttpOnly cookie uses SameSite=Lax, Path=/api/v1/auth, Secure=true by default and seven-day Max-Age. Explicit local HTTP override may set Secure=false; clearing uses compatible attributes/path. Raw refresh tokens are not response JSON or browser-readable application storage.

Each use conditionally claims the old session and creates a same-family successor atomically. Reuse stamps the presented session and revokes the affected family. BadCredentialsException does not roll back these security writes; unexpected runtime/persistence exceptions still roll back. Unknown/blank or unrevoked expired tokens do not create bogus reuse state. Logout revokes the presented current family and clears the cookie; missing/invalid cookies are externally idempotent. Password reset revokes every family for that account.

## Browser session continuity

Established-session refresh captures generation and principal ID. After await it checks both before installing a token or recording genealogy. A different-user cookie replacement ends only the still-current stale session; an A-origin request cannot replay as B. Same-principal renewal remains permitted. Post-await generation fencing protects newer login/logout. Single-flight recovery coalesces requests; delayed same-session 401s use the already installed successor and replay at most once. Transient network/5xx refresh failures preserve token/principal; definitive auth failures end only their owning session.

Cold bootstrap is the explicit identity-establishment path. AuthContext uses independent bootstrap-attempt ownership so stale/StrictMode attempts cannot settle newer initialization. Refresh followed by /me must agree on principal. Cold 401 settles signed out; reload restores a valid cookie-backed session. Logout clears the memory binding and cookie.

## Source and decisive tests

- [authStorage](../../../frontend/src/shared/auth/authStorage.js), [apiClient](../../../frontend/src/shared/api/apiClient.js), [AuthContext](../../../frontend/src/auth/context/AuthContext.jsx)
- [apiClient tests](../../../frontend/src/shared/api/apiClient.test.js): cross-principal mutation replay prevention, same-principal recovery, transient failures and generation fences.
- [AuthContext tests](../../../frontend/src/auth/context/AuthContext.test.jsx): bootstrap ownership, settlement, identity agreement and login/logout races.
- [AuthServiceIntegrationTests.java](../../../backend/src/test/java/com/se2030/vaccinationportal/shared/auth/AuthServiceIntegrationTests.java), [RefreshTokenSessionServiceIntegrationTests.java](../../../backend/src/test/java/com/se2030/vaccinationportal/shared/auth/RefreshTokenSessionServiceIntegrationTests.java), [RefreshTokenSessionPersistenceIntegrationTests.java](../../../backend/src/test/java/com/se2030/vaccinationportal/shared/auth/RefreshTokenSessionPersistenceIntegrationTests.java), [LoginLockoutConcurrencyIntegrationTests.java](../../../backend/src/test/java/com/se2030/vaccinationportal/shared/auth/LoginLockoutConcurrencyIntegrationTests.java), [RefreshTokenConcurrencyMySqlIntegrationTests.java](../../../backend/src/test/java/com/se2030/vaccinationportal/testcontainers/RefreshTokenConcurrencyMySqlIntegrationTests.java)

## Executed baseline evidence

[Backend run 37586589296](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/actions/runs/37586589296), [frontend run 37586589293](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/actions/runs/37586589293). Both run heads are `444364d4b742fa08455c4acf0538baa836e9529a`.

Frontend: 1,141 tests / 93 files, lint green and production build green. Backend: 2,953 default/H2 tests green. MySQL profile: 56 tests green, including RefreshTokenConcurrencyMySqlIntegrationTests. Its isolated MySQL 8.4.3 container applies Flyway V0–V24 (25 migrations), asserts effective ddl-auto=validate and real MySQL connectivity. Two workers are released together; this establishes concurrent scheduling and resulting family invariants, not deterministic identical pre-claim snapshots.

[PR #256](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/pull/256) merged at `444364d4b742fa08455c4acf0538baa836e9529a`. Controller-reported PROMPT #029 Chrome observations agree with the source: different-user mismatch rejected with zero replay, same-user renewal succeeds, cross-tab logout and cold/reload lifecycle pass. A retained browser report/capture is MISSING in this repository; this document does not pretend those observations were re-executed here.

## Assembly limitations

Current screenshots and sanitized browser capture: MISSING — capture required during final evidence assembly. Production TLS/proxy/deployment topology: HUMAN_INPUT_REQUIRED. This package supplies repository/CI evidence, not proof of every deployment's effective cookie policy. See [API contract](../../api/openapi.yaml), [auth sequence](../../diagrams/current/auth-session-sequence.md) and [capture checklist](capture-checklist.md).
