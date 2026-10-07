# MF05 — Administration, Drafts and Certificates design excerpt

Source basis: VaccinationFinalizationService and DigitalCertificateController; selected implementation/tests are linked in the [MF05 dossier](../../evidence/current/MF05.md).

```mermaid
flowchart TD
  N1["Vaccinator draft"]
  N2["Validation and administration confirmation"]
  N3["Atomic finalization"]
  N4["Finalized draft and record"]
  N5["Certificate lifecycle"]
  N6["Stock deduction"]
  N7["Appointment completion"]
  N8["Citizen history and masked verification"]
  N1 --> N2
  N2 --> N3
  N3 --> N4
  N4 --> N5
  N3 --> N6
  N3 --> N7
  N5 --> N8
```

Finalization requires a valid draft, checked-in appointment and compatible available batch. A unique record prevents duplicate completion. Certificate lifecycle includes revocation/reissue; a record can have multiple historical issues.

This is an implemented workflow excerpt, not every planned conceptual entity. Role checks and ownership are detailed in the dossier. Physical constraints are evidenced by its linked migrations. Human confirmation is still needed for member authorship and final captures, not for the five-function model.
