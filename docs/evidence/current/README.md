# Current technical evidence

WBVP-SE2030; canonical business reference 3.3-AI / 2026-08-14-R9. Current implementation: `origin/develop` at `444364d4b742fa08455c4acf0538baa836e9529a` (2026-10-07 audit).

## Evidence vocabulary

- VERIFIED: inspected source or executed CI at the stated commit. A passing suite does not prove every requirement.
- USER_CONSTRAINT: five assessed functions are permitted per project/controller confirmation in PROMPT #038; `VERIFIED_FINAL_BY_USER_CONSTRAINT`. No documentary lecturer approval is claimed or required as an unresolved field here.
- PARTIAL/MISSING: evidence has incomplete coverage or has not been retained.
- HUMAN_INPUT_REQUIRED: personal/group/operator facts cannot be inferred.
- UNKNOWN: authoritative instructions or runtime facts are unavailable.

## Exactly five assessed functions

| Function | Current technical evidence | Identity slot |
|---|---|---|
| MF01 — Screening and Eligibility | [MF01](MF01.md) | Member 1; unconfirmed identity |
| MF02 — Campaign, Center and Session | [MF02](MF02.md) | Member 2; unconfirmed identity |
| MF03 — Appointment, Waitlist and Check-In | [MF03](MF03.md) | Member 3; unconfirmed identity |
| MF04 — Inventory and Batch | [MF04](MF04.md) | Member 4; unconfirmed identity |
| MF05 — Administration, Drafts and Certificates | [MF05](MF05.md) | Member 5; unconfirmed identity |

Authentication, authorization, email verification/recovery, lockout, refresh sessions, profiles, analytics and assistant are cross-cutting/supporting capabilities, not another assessed function.

## Current executed CI

[Backend run 37586589296](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/actions/runs/37586589296), [frontend run 37586589293](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/actions/runs/37586589293). Both run heads are `444364d4b742fa08455c4acf0538baa836e9529a`.

| Result | Count/status | Evidence limitation |
|---|---|---|
| Backend default/H2 | 2,953 tests; zero failures/errors/skips | Whole-suite result, not per-requirement coverage |
| MySQL/Testcontainers profile | 56 tests; zero failures/errors/skips | Includes the refresh concurrency test, one passed |
| Frontend | 1,141 tests / 93 files | Vitest, not a full browser journey |
| Frontend lint/build | Successful | No replacement for retained assessment captures |

[PR #256](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/pull/256) merged refresh rotation into develop. The five-function package is baseline-bound and should be revalidated if develop advances.

## Package navigation

- [Requirements traceability](requirements-traceability.md)
- [Shared authentication evidence](shared-authentication.md)
- [Capture checklist](capture-checklist.md)
- [API inventory](../../api/current-endpoints.md) and [scoped OpenAPI](../../api/openapi.yaml)
- [Current diagram sources](../../diagrams/current/README.md)
- [Submission assembly](../../submission/README.md), [ownership](../../submission/team-and-ownership.md), [reflections](../../submission/individual-reflections.md)
- [Sprint evidence index](../sprints/README.md) and [demo runbook](../../demo/runbook.md)
- [Historical MF-02 Sprint 1 record](../sprint-1/MF02_Sprint1_Evidence.md): older checkpoint only.

## Unresolved assembly

HUMAN_INPUT_REQUIRED: confirmed identities/IDs/ownership, personal reflections, actual sprint dates/boards/meetings, group/lecturer metadata and deployment topology. UNKNOWN: original/final assessment instructions, deadline/format and whether a video is mandatory. MISSING: current screenshot/API/database captures and a retained integrated demonstration. Repository URL is verified: [repository](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal).
