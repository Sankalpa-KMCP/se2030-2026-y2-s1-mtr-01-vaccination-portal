# MF04 — Inventory and Batch design excerpt

Source basis: InventoryBatchPort and inventory service integration tests; selected implementation/tests are linked in the [MF04 dossier](../../evidence/current/MF04.md).

```mermaid
flowchart TD
  N1["Batch definition"]
  N2["Center inventory"]
  N3["Locked availability recheck"]
  N4["Vaccination deduction"]
  N5["Stock transaction audit"]
  N6["Inventory Officer"]
  N7["Supported adjustment"]
  N1 --> N2
  N2 --> N3
  N3 --> N4
  N4 --> N5
  N6 --> N7
  N7 --> N5
```

Draft editing does not change stock. Finalization deduction joins the caller transaction, revalidates availability under locks and records a unique vaccination deduction. Do not assume every planned inventory capability is present.

This is an implemented workflow excerpt, not every planned conceptual entity. Role checks and ownership are detailed in the dossier. Physical constraints are evidenced by its linked migrations. Human confirmation is still needed for member authorship and final captures, not for the five-function model.
