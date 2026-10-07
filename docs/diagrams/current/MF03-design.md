# MF03 — Appointment, Waitlist and Check-In design excerpt

Source basis: AppointmentBookingService and WaitlistPromotionService; selected implementation/tests are linked in the [MF03 dossier](../../evidence/current/MF03.md).

```mermaid
flowchart TD
  N1["Citizen availability search"]
  N2["Booking guard and eligibility"]
  N3["Slot capacity reservation"]
  N4["Appointment"]
  N5["Full slot"]
  N6["FIFO waitlist"]
  N7["Eligibility recheck"]
  N8["Promotion"]
  N9["Permitted staff check in"]
  N10["Checked in"]
  N1 --> N2
  N2 --> N3
  N3 --> N4
  N5 --> N6
  N6 --> N7
  N7 --> N8
  N4 --> N9
  N9 --> N10
```

Booking retains citizen/campaign guard and eligibility checks before reserving capacity. Waitlist ordering is FIFO. Object ownership is enforced separately from role checks.

This is an implemented workflow excerpt, not every planned conceptual entity. Role checks and ownership are detailed in the dossier. Physical constraints are evidenced by its linked migrations. Human confirmation is still needed for member authorship and final captures, not for the five-function model.
