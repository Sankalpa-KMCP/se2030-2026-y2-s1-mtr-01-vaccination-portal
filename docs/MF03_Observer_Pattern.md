# MF03 Observer pattern

MF03 publishes immutable appointment events after saving a booking, cancellation or successful waitlist promotion. Spring defers delivery until the surrounding transaction commits. A rollback delivers nothing. FIFO selection, capacity allocation and synchronous promotion are unchanged.

| Lecture participant | Implementation |
| --- | --- |
| Subject | `AppointmentEventSubject` holds the subscriber list and notifies each observer |
| Observer | `AppointmentObserver.update(AppointmentEvent)` |
| Concrete observers | `ConsoleAppointmentNotificationObserver`, `AppointmentAuditObserver` |
| State-change producers | Booking, cancellation and waitlist promotion services |

Spring injects all Observer implementations when constructing the Subject. Add another Spring component implementing the interface to register another subscriber without editing the business services. The subscriber list is fixed for that application context; runtime subscribe/unsubscribe is not needed here.

## Demonstration

1. Run the backend and book an eligible citizen into a slot. Show `[MF03 NOTIFICATION]` and `[MF03 AUDIT]` entries for BOOKED.
2. Join another eligible citizen to the session waitlist using the existing flow.
3. Cancel the booked appointment within the permitted window (or use a valid staff override).
4. Show cancellation and WAITLIST_PROMOTED audit entries and their separate notification messages. Verify the earliest eligible FIFO citizen has the confirmed appointment.
5. Explain that the services depend on an event publisher rather than the concrete notification or audit handlers. The subject delivers the same event to both observers after commit.

## Benefits and limitations

The business workflow is separated from notification and audit handling. Subscriber failure is logged and does not stop other subscribers or turn a committed booking into an API failure. Events contain IDs and timestamps rather than mutable JPA entities.

Notification delivery is to the server console for the demonstration; no email, SMS or in-app delivery is claimed. Audit entries use application logging, not a separate database history table. Delivery is synchronous after commit and best effort: there is no durable outbox or retry, so process termination or observer failure can lose a notification. A production delivery integration should use durable storage and retries.

## Validation

`AppointmentEventSubjectTests` checks after-commit fan-out, rollback suppression, events outside transactions, failure isolation and all three event types. Existing booking, cancellation, waitlist promotion and concurrency tests should also be run to check the unchanged business behaviour.
