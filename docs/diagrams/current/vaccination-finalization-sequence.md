# Atomic vaccination finalization

Source: [VaccinationFinalizationService](../../../backend/src/main/java/com/se2030/vaccinationportal/vaccination/administration/service/VaccinationFinalizationService.java). The dossier links rollback/deduction tests. Backend provider calls join the finalization transaction.

```mermaid
sequenceDiagram
  participant UI as Vaccinator UI
  participant S as Finalization service
  participant R as Record repository
  participant I as Inventory provider
  participant A as Appointment provider
  participant C as Certificate service
  UI->>S: Finalize confirmed draft
  Note over S,C: Single transaction; unexpected failure rolls back all writes
  S->>S: Validate draft, ownership, checked-in appointment and batch
  S->>R: Save unique vaccination record
  S->>I: Deduct dose under inventory locking and write audit
  S->>A: Complete appointment
  S->>C: Issue certificate record
  S->>S: Mark draft FINALIZED and flush
  S-->>UI: Finalization result
```

The implemented draft is retained as FINALIZED; it is not deleted by this flow. PDF rendering/download is a separate API concern. This sequence does not claim next-dose booking creation or an external delivery system.
