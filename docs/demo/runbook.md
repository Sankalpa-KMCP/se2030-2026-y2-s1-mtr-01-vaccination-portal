# Source-supported local demonstration runbook

Baseline: develop `444364d4b742fa08455c4acf0538baa836e9529a`. This is preparation guidance; it does not assert that a demo was executed. Refer to [backend](../../backend/README.md), [frontend](../../frontend/README.md) and [database](../../database/README.md) setup guides.

## Preparation

Use Java 21, a supported Node toolchain per frontend README, npm and Maven wrappers. Use an existing safe local MySQL or repository Compose MySQL setup. Compose supplies MySQL, not a verified deployed frontend/backend topology. Configure local database connectivity and JWT/email settings using documented environment variables and private local values; do not commit or capture credentials. Confirm staff-role accounts through supported registration/admin review, not inferred default accounts. Use synthetic citizen data.

Flyway runtime location is backend/src/main/resources/db/migration: V0–V24, 25 migrations. Keep Hibernate validation and existing data intact. Check health and schema history before demonstrating. Historical database/migrations scripts are not runtime inputs.

## Startup

From backend, Windows:

```powershell
$env:AUTH_REFRESH_COOKIE_SECURE = "false"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The Secure=false environment override is process-local and only appropriate to local HTTP; ensure a private local property does not override it unexpectedly. Secure=true is the canonical default for HTTPS. Configure database/JWT/email secrets privately as described in the setup guides; values are deliberately omitted here.

From frontend in a separate terminal:

```powershell
npm ci
npm run dev
```

The Vite /api proxy targets the local backend. Open the reported local URL. Do not enable public exposure of a local demo by assumption. Remove the process-local override after stopping the demonstration terminal if reusing it for other environments.

## Authentication considerations

Access JWT/principal binding are memory-only. A reload restores a cookie session via refresh then /me. Cookies are HttpOnly, SameSite=Lax and path /api/v1/auth; browser JS cannot read them. Different-user cross-tab cookie replacement signs out the stale tab on refresh; same-user renewal may continue. Do not display raw network bearer headers/cookie values. Confirm cold unauthenticated settlement, login, reload and logout before function captures.

## Suggested five-function demonstration

1. MF01: citizen screening then Healthcare Officer review/final eligibility.
2. MF02: campaign/center/session planning and readiness; arrange eligible availability before booking.
3. MF03: citizen booking/waitlist and permitted staff check-in.
4. MF04: suitable batch and center-stock setup/audit.
5. MF05: confirmed draft finalization, visible record/certificate, masked verification and supported lifecycle action.

These are an evidence order, not invented account/data fixtures. Select actual workflows supported by current controllers. Capture at least one meaningful validation/authorization denial and persisted outcome per function using the [checklist](../evidence/current/capture-checklist.md).

## Reset and operator decisions

HUMAN/OPERATOR DECISION REQUIRED: choose approved demo data preparation/cleanup. This runbook does not authorize destructive database resets, volume deletion or wiping existing user data. Deployment URL/topology, final video requirements, timing and ownership remain external inputs.

## Retained evidence

Record commit/date, role labels, expected/observed outcomes, API status and sanitized persisted state. Include CI links separately from browser observations. Keep screenshots privacy-safe, retain actual failures honestly and stop if workflow differs from documented source. Shut down only processes you started. No release/deployment or Git publication is part of this runbook.
