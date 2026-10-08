# Authentication and refresh identity continuity

Source: [AuthController](../../../backend/src/main/java/com/se2030/vaccinationportal/shared/auth/controller/AuthController.java), [apiClient](../../../frontend/src/shared/api/apiClient.js) and [AuthContext](../../../frontend/src/auth/context/AuthContext.jsx). Access JWT validation is stateless; refresh-family state is persisted.

```mermaid
sequenceDiagram
  participant A as Tab A memory session
  participant B as Tab B
  participant Cookie as Shared HttpOnly cookie
  participant API as Auth API
  participant DB as Refresh session repository
  A->>API: Login credentials
  API->>DB: Persist hash and new family
  API-->>Cookie: Set refresh cookie
  API-->>A: Access JWT and stable user ID
  Note over A: Bind generation and principal; token memory only
  A->>API: Access JWT authenticated operation
  API-->>A: 401 before protected business operation
  A->>API: Refresh using shared cookie
  API->>DB: Conditional claim old session and create successor
  alt Valid single-use rotation
    API-->>Cookie: Replace refresh cookie
    API-->>A: Access JWT and user ID
    A->>A: Check initiating generation and principal
    A->>API: Replay at most once if both match
  else Rotated or revoked token reuse
    API->>DB: Stamp reuse and revoke family durably
    API-->>Cookie: Clear cookie
    API-->>A: 401
  end
  B->>API: Login in same browser context
  API-->>Cookie: Replace shared cookie
  A->>API: Later established-session refresh
  API-->>A: Access JWT and cookie owner ID
  alt Same principal and generation current
    A->>A: Install token and record same-session genealogy
    A->>API: Eligible request replay once
  else Different principal
    A->>A: Reject token; end only owning stale local session
    Note over A: No cross-principal replay; Tab B remains locally signed in
  end
  A->>API: Logout with current cookie
  API->>DB: Revoke presented family
  API-->>Cookie: Clear matching cookie
  API-->>A: 204
  A->>A: Clear token and principal; advance generation
```

Cold bootstrap explicitly establishes identity from refresh when no local principal is bound; /me must agree. Bootstrap-attempt ownership prevents stale initialization completion. Cookie default: HttpOnly, SameSite=Lax, Secure, /api/v1/auth, seven days; explicit local HTTP opt-out only. Cross-tab immediate logout synchronization is not promised.
