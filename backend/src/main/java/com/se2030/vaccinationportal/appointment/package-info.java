/**
 * MF-03 — Appointment, Waitlist, and Check-In Management.
 *
 * <h2>Evaluator review map</h2>
 * <p>This package is the backend entry point for the complete MF-03 workflow:</p>
 * <ol>
 *   <li>{@code booking/web/AppointmentController} and {@code waitlist/web/WaitlistController} expose the REST API.</li>
 *   <li>Capability-local {@code dto/*} validates and defines the JSON request/response contract.</li>
 *   <li>Capability-local {@code service/*} contains the business rules and transaction boundaries.</li>
 *   <li>{@code persistence/repository/*} provides Spring Data JPA database access.</li>
 *   <li>{@code persistence/model/*} maps appointments and waitlist entries to MySQL tables.</li>
 *   <li>Provider-owned {@code screening/api/eligibility} and {@code campaign/api/capacity} connect MF-03 to eligibility and slot capacity.</li>
 *   <li>{@code web/AppointmentExceptionHandler} converts domain failures into consistent HTTP errors.</li>
 * </ol>
 *
 * <h2>CRUD demonstration</h2>
 * <ul>
 *   <li><b>Create:</b> booking an appointment and joining a waitlist create persisted records.</li>
 *   <li><b>Read:</b> citizens view their appointments and waitlist status; staff load the check-in queue.</li>
 *   <li><b>Update:</b> cancellation, rescheduling, promotion, check-in, reversal, and no-show processing
 *       update controlled domain states.</li>
 *   <li><b>Delete:</b> MF-03 deliberately uses status transitions instead of hard deletion so appointment
 *       and waitlist history remains auditable.</li>
 * </ul>
 *
 * <h2>Connectivity path</h2>
 * <p>
 * React pages call the functions in
 * {@code frontend/src/features/appointment-management/api}. Those functions use the shared
 * {@code apiClient}, which attaches the JWT and calls {@code /api/v1/appointments} or
 * {@code /api/v1/waitlist}. Controllers obtain the authenticated citizen from the JWT principal,
 * services execute the rules inside Spring transactions, and repositories persist the result through
 * JPA/Hibernate to MySQL. Flyway migrations {@code V1__mf03_appointment_foundation.sql} and
 * {@code V7__mf03_waitlist_foundation.sql} create the main MF-03 tables, indexes, generated columns,
 * and uniqueness constraints.
 * </p>
 *
 * <h2>Important integrity controls</h2>
 * <ul>
 *   <li>JWT ownership checks prevent one citizen from reading or changing another citizen's records.</li>
 *   <li>{@code @PreAuthorize} restricts citizen and staff endpoints by role.</li>
 *   <li>Eligibility is checked through the MF-01 port before booking or waitlist entry.</li>
 *   <li>Slot capacity is reserved and released through the MF-02 port.</li>
 *   <li>Transactions, guard-row pessimistic locks, JPA version fields, and database unique constraints
 *       protect against double booking and concurrent allocation of the last slot.</li>
 *   <li>Completed appointments are immutable, and late modifications require an authorized staff override.</li>
 * </ul>
 */
package com.se2030.vaccinationportal.appointment;
