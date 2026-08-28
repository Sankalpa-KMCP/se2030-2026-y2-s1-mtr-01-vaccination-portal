-- Flyway Database Migration V9: MF-02 Vaccination Session Booking Suspension
-- Adds authoritative booking suspension column to vaccination_sessions table
-- Canonical Section 6.6 / E.5 (SessionBookingStatus.SUSPENDED manual override)

ALTER TABLE vaccination_sessions
    ADD COLUMN booking_suspended BOOLEAN NOT NULL DEFAULT FALSE;
