Web-Based Vaccination Portal

AI Canonical Project Specification

This file is the canonical, self-contained context package for the Web-Based Vaccination Portal. It is written for use by AI systems and by human project members. It defines the project scope, ownership boundaries, workflows, business rules, data model, status models, technical baseline, security rules, use cases, test coverage and resolved design decisions.

AI Consumption Contract

Treat this document as the project source of truth for revision 2026-08-08-R8.

Use exactly five major functions unless the user explicitly changes the scope.

Do not infer that any planned feature is implemented. The default implementation state is NOT_VERIFIED.

Values marked USER_DATA_REQUIRED are genuinely unknown external facts. Never invent them.

All architecture, workflow and business-rule choices in this version are BASELINE_DECISION; they are no longer open recommendations.

When a summary conflicts with a detailed business rule, use the detailed business rule.

When two records use similar words such as Draft, Active or Cancelled, keep their status enums separate by entity. Do not create one global status enum.

Citizen registration, authentication, password reset and profile management are shared supporting functions. They are not MF-01.

MF-01 exclusively owns screening questions, answer options, questionnaires, questionnaire versions, eligibility rules, screening submissions, screening answers and eligibility assessments.

MF-02 links a campaign to one published questionnaire version. MF-02 must not create or edit detailed eligibility rules.

MF-02 owns service planning: campaign lifecycle, administrative location targeting, dose-programme configuration, centers, operating calendars, recurring sessions, physical resources, staffing, capacity and operational utilization. MF-03 owns citizen bookings, waitlists, rebooking and check-in; MF-04 owns batch quantities and stock allocation.

Session changes with affected bookings must never silently rewrite appointments. MF-02 records the schedule change and impact; MF-03 performs appointment migration, cancellation or waitlist handling with history and notification.

Eligibility is campaign-specific. A citizen account is created once, but screening and the final eligibility decision occur separately for each campaign.

Rule evaluation produces only a preliminary screening result. An authorized Healthcare Officer records the final eligibility decision. The portal does not diagnose medical conditions.

MF-05 demonstrates full CRUD only on unfinalized VaccinationDraft records. Finalized vaccination records and stock transactions are immutable operational history.

The first certificate issue is created automatically during successful vaccination finalization. Later actions are view, download, revoke and reissue—not a second initial “generate” operation.

Public certificate verification returns only minimum approved data and never exposes the citizen's full profile or medical notes.

The five modules must be integrated with consistent identifiers, foreign keys, API contracts, transactions and audit records.

Any generated code must use database constraints and service-level validation for critical rules; frontend validation alone is insufficient.

When asked about an external fact that is marked USER_DATA_REQUIRED, state that the fact was not provided instead of guessing.

When canonical files conflict, the Markdown source controls; treat DOCX diagrams and pagination as presentation only.

Source Authority and Synchronization

Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md is the normative semantic source for this release.

Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx is the synchronized human-readable derivative. Its diagrams, pagination and visual layout are presentational; they do not override the Markdown semantics.

Both files must carry document version 3.3-AI and content revision 2026-08-08-R8. A release with mismatched values is invalid.

Generated code, schemas, OpenAPI contracts, test plans and summaries must identify canonical project WBVP-SE2030 and the revision used.

If the two files ever conflict, use the Markdown file and regenerate the DOCX before implementation continues.

Canonical Snapshot

Area

Baseline decision

Project structure

Five combined major functions and five member ownership slots

Architecture

React SPA + Java 21 Spring Boot REST API + MySQL, implemented as a layered modular monolith

Authentication

Spring Security with role-based authorization; citizen self-registration; staff accounts created by an administrator

Eligibility

Separate decision for each citizen-campaign pair; final decision by Healthcare Officer

Campaign screening

MF-02 links a campaign to an immutable published QuestionnaireVersion owned by MF-01

MF-02 planning

Recurring sessions, center closures, staff/resource conflict checks, calculated capacity, location targeting, dose-programme constraints and operational utilization are baseline

Schedule changes

MF-02 records session changes and impact; MF-03 handles affected appointments, waitlists and citizen rebooking

Appointment access

Requires a currently valid ELIGIBLE assessment for the selected campaign

Waitlist

First-in-first-out by joined_at, then record ID; promotion rechecks eligibility and session validity

Inventory

Stock is held by center, vaccine type and batch; all quantity changes create append-only transactions

Administration

Unfinalized drafts have CRUD; finalization is one atomic transaction

Certificate

Version 1 is issued automatically during finalization; reissue creates a new certificate issue

Deletion

Hard delete only unused MF-01 configuration and unfinalized MF-05 drafts; operational history is retained

Notifications

In-app and email are baseline; SMS is outside the core baseline

Public verification

Valid/Revoked/Invalid result plus minimum identity and vaccination proof fields

Source authority

Markdown is normative; DOCX is a synchronized human-readable derivative

Conditional uniqueness

MySQL generated active-key columns plus ordinary unique indexes and citizen-campaign guard-row locking

Event delivery

Transactional outbox with idempotent consumers and retryable notification dispatch

Implementation status

Planning baseline only; no feature is assumed complete without evidence

User-Supplied Metadata Register

The following values were not supplied. AI systems must preserve them as unknown.

Field

Value

Status

Programme

USER_DATA_REQUIRED

Unknown external data

Institution

USER_DATA_REQUIRED

Unknown external data

Faculty or school

USER_DATA_REQUIRED

Unknown external data

Group number or name

USER_DATA_REQUIRED

Unknown external data

Lecturer name

USER_DATA_REQUIRED

Unknown external data

Member names and student IDs

USER_DATA_REQUIRED

Unknown external data

Repository URL

USER_DATA_REQUIRED

Unknown external data

Deployment URL

USER_DATA_REQUIRED

Unknown external data

Submission date

USER_DATA_REQUIRED

Unknown external data

Written approval for five-function assessment mapping

USER_DATA_REQUIRED

Evidence not supplied

Assessment alignment: The supplied assignment specification describes six major functions and six-member groups, while this project baseline uses five combined functions and five members. This document does not claim lecturer approval. The team must retain written approval or explain the mapping during assessment.

Lab 02 reporting alignment: Lab 02 separately requires six personas, a minimum of four user stories per persona, a prioritized Product Backlog, four sprints, and member-wise task allocation. The current team report uses six assessment personas and 30 user stories (five per persona) distributed once across four sprints. These Lab US-xx IDs are reporting/traceability labels; they do not create a sixth implementation function and do not replace canonical PB, FR, UC, TC, or business-rule identifiers.

Status Vocabulary Used in This Document

Label

Meaning

BASELINE_DECISION

A resolved project design choice that should be used for implementation and documentation

SOURCE_REQUIREMENT

A requirement originating from the supplied assignment or project source material

USER_DATA_REQUIRED

A real-world value or evidence item not supplied to the AI

FUTURE_OPTION

Deliberately outside the core delivery baseline

NOT_VERIFIED

Implementation or evidence has not been supplied

Project Contents

Introduction

System Overview

Stakeholders and User Roles

Functional Requirements Overview

MF-01 Pre-Vaccination Screening and Eligibility Rule Management

MF-02 Vaccination Campaign, Center and Session Management

MF-03 Appointment, Waitlist and Check-In Management

MF-04 Vaccine Inventory and Batch Management

MF-05 Vaccination Administration, Draft Records and Digital Certificate Management

Supporting Minor Functions

Non-Functional Requirements

Data Requirements and Conceptual Data Model

Business Rules and Validation Catalogue

Product Backlog and Acceptance Criteria

Use Case Catalogue

User Interface and Screen Inventory

Security, Privacy and Ethical Considerations

Testing Strategy

Project Management and Scrum Plan

Team Allocation and Ownership

Project Timeline

Risk Register

Constraints, Assumptions and Dependencies

Deployment and Environment Plan

Future Enhancements

Resolved Decision Register

Conclusion

References

Appendix A. Stakeholder Validation Guide

Appendix B. Requirements Traceability Matrix

Appendix C. Evidence Checklist

Appendix D. Glossary and Canonical Terms

Appendix E. Canonical State Transition Contracts

Appendix F. Release Integrity and AI Handoff Checklist

Executive Summary

The Web-Based Vaccination Portal is an integrated role-based web application that manages the vaccination journey from account access and campaign-specific eligibility screening to appointment booking, stock-controlled vaccination administration and verifiable digital certification.

The project uses five tightly integrated major functions. MF-01 manages configurable screening and final campaign-specific eligibility decisions. MF-02 manages campaign lifecycle, administrative service areas, dose-programme configuration, centers, operating calendars, recurring sessions, time slots, staff, physical resources, calculated capacity, schedule-change impact and operational utilization. MF-03 manages bookings, waitlists, affected-appointment handling and check-in. MF-04 manages vaccine batches, center stock and stock movements. MF-05 provides CRUD for unfinalized vaccination drafts and then finalizes a validated draft through one atomic transaction that creates the permanent vaccination record, deducts stock, completes the appointment, issues the initial certificate and removes the draft.

The technical baseline is a React single-page application built with Vite and Bootstrap 5, a Java 21 Spring Boot REST API, MySQL, Maven, Flyway database migrations, OpenAPI documentation, Git and GitHub. The accepted implementation form is a layered modular monolith because it preserves clear module boundaries while remaining practical for a five-member academic team.

1. Introduction

1.1 Background

Vaccination services combine identity management, campaign eligibility, scheduling, physical attendance, vaccine inventory, dose administration and certification. When these activities are handled through paper forms or disconnected systems, records can become inconsistent, capacity may be overbooked, stock may not match administered doses, and certificates may be difficult to verify.

The proposed portal connects the major activities through shared identifiers and controlled state transitions. Citizens and authorized staff use different role-based interfaces but operate on the same integrated data model.

1.2 Problem Statement

The project addresses these operational problems:

repeated citizen data entry and duplicate records;

unclear or non-campaign-specific eligibility decisions;

invalid booking against unavailable or unsuitable sessions;

race conditions that can overbook capacity;

disconnected inventory and vaccination records;

direct editing or deletion of permanent medical and stock history;

certificates that cannot be reliably verified or revoked;

insufficient role restrictions and audit evidence.

1.3 Project Overview

The system is a responsive web portal for citizens, administrators, healthcare officers, campaign officers, center managers, reception staff, inventory officers, vaccinators, certificate verifiers and management users. It supports screening and authorized decision-making but does not perform medical diagnosis.

1.4 Main Objective

Design and develop an integrated Java-based web application that manages vaccination services accurately, securely and efficiently from citizen account access and campaign-specific screening through controlled vaccination finalization and certificate verification.

1.5 Specific Objectives

Provide secure citizen and staff account access with role-based authorization.

Manage versioned screening questions, answer options, rules and questionnaires.

Record a separate final eligibility assessment for every citizen-campaign application.

Plan campaign lifecycle, administrative service areas, dose programmes, centers, operating calendars, recurring sessions, time slots, staff, physical resources and validated capacity.

Support appointment booking, rescheduling, cancellation, waitlisting and check-in.

Track vaccine batches, center inventory and every stock movement.

Demonstrate full CRUD on unfinalized vaccination drafts.

Finalize a vaccination safely using one atomic cross-module transaction.

Maintain immutable vaccination history and versioned certificate issues.

Provide audit logs, notifications, dashboards, reports and testable evidence.

1.6 Target Users

Citizen

System Administrator

Healthcare Officer

Campaign or Health Authority Officer

Vaccination Center Manager

Receptionist or Check-In Staff

Inventory Officer

Vaccinator

Certificate Verifier

Management or Reporting User

1.7 Project Scope

In scope

Scope area

Included capability

Shared access

Registration, email verification, login, logout, password reset, profile, role access

MF-01

Questions, options, rules, questionnaire versions, submissions, preliminary evaluation, final eligibility review

MF-02

Campaigns, vaccine types, target groups, questionnaire linkage, administrative location targets, dose programmes, centers, hours/closures, recurring sessions, slots, staff/resources, calculated capacity, schedule-change impact and operational analytics

MF-03

Search, booking, rescheduling, cancellation, waitlist, promotion, check-in, no-show

MF-04

Manufacturers, batches, center inventory, receipts, transfers, adjustments, deductions, alerts

MF-05

Draft CRUD, administration validation, atomic finalization, history, certificates, corrections, revocation and reissue

Shared services

In-app/email notifications, audit, dashboards, reports, CSV export, public certificate verification

Outside the core baseline

real payment processing;

automatic medical diagnosis;

national-scale identity-system integration;

production-scale SMS integration;

native mobile applications;

cross-country certificate trust frameworks;

predictive analytics, machine learning and demand forecasting;

production-grade disaster recovery beyond the academic deployment plan.

1.8 Key Assumptions

Users provide accurate identity and contact information.

Staff roles are assigned only by an administrator.

Healthcare Officers are responsible for final eligibility decisions.

Campaign and center staff maintain campaign schedules, center calendars, session resources and capacity; MF-03 staff maintain appointment and check-in records.

Inventory Officers record receipts, transfers and adjustments accurately.

Vaccinators confirm the physical administration before finalization.

Email may be sent through a sandbox service during evaluation.

The project will use synthetic or anonymized demonstration data.

2. System Overview

2.1 System Context

flowchart LR
    Citizen[Citizen] -->|profile, screening, booking, certificates| Portal[Web-Based Vaccination Portal]
    Admin[System Administrator] -->|accounts, roles, MF-01 configuration| Portal
    Health[Healthcare Officer] -->|final eligibility review| Portal
    Campaign[Campaign Officer] -->|campaign configuration| Portal
    Center[Center Staff] -->|sessions, capacity, check-in| Portal
    Inventory[Inventory Officer] -->|batches and stock movements| Portal
    Vaccinator[Vaccinator] -->|draft and finalization| Portal
    Verifier[Certificate Verifier] -->|reference or QR verification| Portal
    Portal -->|authorized summaries| Management[Management User]
    Portal -->|email messages| Email[Email Service]

Public users can verify a certificate without logging in, but the verification response is intentionally minimal.

2.2 End-to-End Vaccination Workflow

A citizen registers once, verifies the account and completes the profile.

The citizen selects a citizen-visible PUBLISHED or ACTIVE campaign inside its screening window and opens its linked published questionnaire version.

The citizen records consent, answers all required questions and submits the screening.

The rules engine records a preliminary result and triggered rules.

A Healthcare Officer reviews the submission and records ELIGIBLE or INELIGIBLE, with a reason and validity period.

A citizen with a currently valid ELIGIBLE assessment searches the selected campaign's centers, sessions and time slots.

The citizen books an available slot or joins the session waitlist.

Center staff validate the appointment and check the citizen in during the allowed window.

A Vaccinator creates or opens an unfinalized vaccination draft and selects the intended center inventory batch.

The system validates campaign, eligibility, appointment, check-in, vaccine, batch, stock, expiry, recall and dose sequence.

After physically administering the vaccine, the Vaccinator explicitly confirms administration and finalizes the draft.

One database transaction creates the permanent vaccination record, deducts stock, records the stock transaction, marks the appointment COMPLETED, creates certificate issue version 1 and deletes the draft.

The citizen views or downloads the certificate.

A public verifier checks the certificate using its reference or QR verification token.

flowchart LR
    A[Account and profile] --> B[Campaign screening]
    B --> C[Preliminary rule evaluation]
    C --> D[Healthcare Officer decision]
    D -->|Eligible| E[Appointment or waitlist]
    E --> F[Check-in]
    F --> G[Vaccination draft]
    G --> H[Physical administration confirmation]
    H --> I[Atomic finalization]
    I --> J[Permanent record]
    I --> K[Stock deduction]
    I --> L[Appointment completed]
    I --> M[Certificate issue v1]

2.3 Major Modules

ID

Major function

Primary output

MF-01

Pre-Vaccination Screening and Eligibility Rule Management

Reviewed, versioned eligibility assessment for a citizen-campaign pair

MF-02

Vaccination Campaign, Center and Session Management

Valid campaign/service-area configuration, operating calendars, recurring sessions, resources and defensible capacity with schedule-change impact control

MF-03

Appointment, Waitlist and Check-In Management

Confirmed appointment or waitlist position and traceable arrival state

MF-04

Vaccine Inventory and Batch Management

Accurate center-level quantity by batch with append-only movement history

MF-05

Vaccination Administration, Draft Records and Digital Certificate Management

Immutable vaccination record and automatically issued verifiable certificate

2.4 Baseline High-Level Architecture

The system is a layered modular monolith. Each major function has its own controller, application service, domain rules, repository interfaces and tests, while all modules share one deployable backend and one relational database.

flowchart TB
    subgraph Client[Client Layer]
        React[React SPA - Vite and Bootstrap 5]
    end
    subgraph API[Application Layer - Java 21 Spring Boot]
        Security[Spring Security and RBAC]
        Controllers[REST Controllers /api/v1]
        Services[Module Application Services]
        Audit[Audit and Notification Services]
        Docs[OpenAPI Documentation]
    end
    subgraph Data[Data Layer]
        JPA[Spring Data JPA]
        DB[(MySQL)]
        Files[Certificate PDF and QR assets]
    end
    React --> Security --> Controllers --> Services --> JPA --> DB
    Services --> Audit
    Controllers --> Docs
    Services --> Files

Fixed technology baseline

Area

Decision

Frontend

React SPA, Vite build tooling, Bootstrap 5 component/layout styling

Backend

Java 21, Spring Boot, Spring Web, Spring Security, Spring Data JPA, Bean Validation

Database

MySQL with Flyway migrations

Build

Maven

API

REST/JSON, /api/v1, OpenAPI specification

Authentication

JWT access token plus Secure HttpOnly refresh-token cookie; RBAC on every protected endpoint

Testing

JUnit 5, Mockito, Spring Boot integration tests, Testcontainers/MySQL; Vitest and React Testing Library

Version control

Git and GitHub with feature branches and pull-request review

Demo deployment

Docker Compose for frontend, backend and MySQL; environment variables for secrets

Exact library patch versions belong in pom.xml and package.json; this context defines the architecture and product choices rather than freezing time-sensitive patch numbers.

2.5 Module Dependencies

flowchart LR
    Access[Shared Account and Access] --> MF1[MF-01 Screening and Eligibility]
    MF1 --> MF2[MF-02 Campaign and Session]
    MF1 --> MF3[MF-03 Appointment and Check-In]
    MF2 --> MF3
    MF2 --> MF4[MF-04 Inventory]
    MF3 --> MF5[MF-05 Administration and Certificate]
    MF4 --> MF5
    Access --> MF5
    MF5 --> Verify[Public Certificate Verification]

MF-01 and MF-02 collaborate through CampaignQuestionnaireAssignment: MF-01 publishes immutable questionnaire versions; MF-02 selects one published version for a campaign. Detailed rule ownership never moves to MF-02. MF-02 publishes validated availability and schedule-change events to MF-03, reads aggregate booking counts from MF-03, and does not own appointment mutation. It may read inventory-readiness aggregates from MF-04 but never owns batch quantities.

3. Stakeholders and User Roles

Role

Core responsibilities

Citizen

Register, manage own profile, complete campaign screening, view eligibility, book or manage appointments, view own vaccination history and certificates

System Administrator

Create staff accounts, assign roles, manage account state, manage MF-01 configuration, view audit records and system settings

Healthcare Officer

Review screening submissions, request corrections, record final eligibility, revoke an assessment with a reason

Campaign Officer

Manage campaign metadata, vaccine type, target group description, dates, status and published questionnaire linkage

Center Manager

Manage center calendars, closures, recurring sessions, slots, staff/resources, calculated capacity, schedule changes and utilization

Receptionist / Check-In Staff

Search appointments, verify citizen and session, check in, reverse an incorrect check-in with a reason, record no-show

Inventory Officer

Register manufacturers and batches, allocate stock, transfer stock, adjust quantity, quarantine or recall batches, review alerts

Vaccinator

View checked-in appointments, create/read/update/delete own unfinalized drafts, validate and finalize administered doses

Certificate Verifier

Use public verification; receives no administrative access

Management User

View authorized aggregate dashboards and exports without editing operational records

3.1 Role-Based Access Matrix

Legend: C create, R read, U update, D hard delete, A administrative transition/action.

Resource or action

Citizen

Admin

Healthcare

Campaign

Center

Inventory

Vaccinator

Public

Own account/profile

C/R/U

R/U/A

R limited

R limited

R limited

R limited

R limited

-

Staff accounts/roles

-

C/R/U/A

-

-

-

-

-

-

MF-01 questions/options/rules

-

C/R/U/D

R

-

-

-

-

-

Questionnaire versions

R published

C/R/U/D/A

R

R link only

-

-

-

-

Screening submission

C/R/U own

R

R/U/A

-

-

-

-

-

Eligibility assessment

R own

R

C/R/A

R summary

R booking check

-

R administration check

-

Campaign

R published/active

R/A

R

C/R/U/A

R

R

R

-

Center/session/slot

R available

R/A

R

R

C/R/U/A

R

R

-

Appointment/waitlist

C/R/U/A own

R/A

R

R

R/U/A

-

R

-

Batch/inventory

-

R

R limited

R summary

R summary

C/R/U/A

R selectable

-

Vaccination draft

-

R audit

R limited

-

R limited

R stock view

C/R/U/D/A

-

Vaccination record

R own

R

R

R aggregate

R authorized

R transaction link

R

-

Certificate issue

R own/download

R/A

R/A

-

R authorized

-

R/A

R verify only

Audit log

-

R

R own actions

R own actions

R own actions

R own actions

R own actions

-

All server endpoints enforce authorization. Hiding a button is not an authorization control.

4. Functional Requirements Overview

ID

Function

Requirement

FR-01

Account registration

Citizens can register with unique identity and email, verify email and create one profile

FR-02

Authentication

Users can log in, log out and reset passwords; staff accounts are administrator-created

FR-03

Authorization

Actions and data visibility are restricted by role and ownership

FR-04

Screening configuration

Admin manages questions, options, rules, questionnaires and unpublished versions with safe CRUD

FR-05

Screening submission

Citizen completes consent and the published questionnaire for a selected campaign

FR-06

Eligibility review

System calculates preliminary result; Healthcare Officer records final decision, reason and validity

FR-07

Campaign management

Campaign Officer manages lifecycle, dates, vaccine type, target group, administrative location targets and dose-programme configuration

FR-08

Questionnaire linkage

Campaign links to exactly one active published questionnaire version at a time

FR-09

Center/session planning

Center Manager manages center hours/closures, recurring sessions, slots, staff, physical resources and calculated capacity with conflict validation

FR-09A

Session change control

Authorized staff preview impact, suspend booking, reschedule or cancel sessions; MF-03 handles affected appointments and waitlists without silent rewriting

FR-09B

MF-02 operational analytics

Authorized staff view capacity, booking, utilization, cancellation and resource-load aggregates without exposing unnecessary citizen data

FR-10

Appointment search

Eligible citizens search currently bookable PUBLISHED or ACTIVE campaign availability

FR-11

Appointment lifecycle

Book, view, reschedule and cancel without duplicate active bookings or overbooking

FR-12

Waitlist

Join, leave and promote by deterministic FIFO order when capacity becomes available

FR-13

Check-in

Validate appointment and time window; record check-in, reversal or no-show

FR-14

Batch registration

Inventory Officer manages manufacturer and batch master data

FR-15

Center inventory

Allocate and track quantity by center, vaccine type and batch

FR-16

Stock transactions/alerts

Record receipts, transfers, adjustments and deductions; calculate low-stock and near-expiry alerts

FR-17

Vaccination draft CRUD

Vaccinator creates, reads, updates and hard-deletes unfinalized drafts without side effects

FR-18

Atomic finalization

Finalize only after administration confirmation; create permanent record, deduct stock, complete appointment, issue certificate and remove draft atomically

FR-19

Vaccination history

Authorized users view immutable dose history, corrections and next-dose information

FR-20

Certificate lifecycle

View/download certificate, revoke or reissue with audit; public verification by opaque reference or QR token

FR-21

Shared services

In-app/email notifications, audit logs, role dashboards and authorized CSV reports

5. MF-01 Pre-Vaccination Screening and Eligibility Rule Management

Attribute

Baseline

Purpose

Manage versioned screening configuration, collect campaign-specific citizen submissions and support authorized final eligibility decisions

Primary users

System Administrator, Citizen, Healthcare Officer

Main records

ConsentNoticeVersion, ScreeningQuestion, AnswerOption, EligibilityRule, Questionnaire, QuestionnaireVersion, QuestionnaireItem, CampaignQuestionnaireAssignment, ScreeningSubmission, ScreeningAnswer, EligibilityAssessment

Dependency boundary

Account/profile is shared; campaign metadata is MF-02; detailed questionnaire and rule ownership stays in MF-01

5.1 Core Capabilities

Create and manage reusable questions and answer options.

Create rule definitions that reference stable question codes and typed answer values.

Assemble immutable published questionnaire versions with ordered items and rule snapshots.

Assign a published version to a campaign through a controlled linkage.

Record the exact published consent/privacy notice version accepted by the citizen together with consent time and required answers for the selected campaign.

Calculate PRELIMINARY_PASS or PRELIMINARY_REVIEW_REQUIRED and store triggered rules.

Allow a Healthcare Officer to request corrections or record a final decision.

Display campaign-specific eligibility and review history.

Expire or revoke an assessment without deleting historical decisions.

5.2 CRUD and Record Lifecycle

Operation

Behaviour

Create

Questions, options, rules, questionnaires, draft versions, items and citizen submissions can be created by authorized actors

Read

Authorized users can view configuration, versions, submissions, triggered rules and review history according to role

Update

Only unused configuration and unpublished versions are editable; a citizen may edit a draft or corrections-required submission

Delete

Hard delete is allowed only when an MF-01 configuration record is unused and not referenced; published or historical data cannot be deleted

Published questionnaire versions are immutable. Changes require a new version number.

5.3 Key Rules and Validations

Rule ID

Rule

MF01-BR-01

Question code, rule code and questionnaire version number are unique within their parent scope

MF01-BR-02

Every selectable question has valid answer options; numeric/date questions use typed constraints

MF01-BR-03

A published version contains at least one active item and passes rule-reference validation

MF01-BR-04

A campaign can accept screening only through its currently linked published version

MF01-BR-05

One citizen can have only one active submission per campaign; database uniqueness enforces this

MF01-BR-06

Consent and all required answers are mandatory before submission

MF01-BR-07

Rule results are preliminary and cannot directly create an INELIGIBLE final assessment

MF01-BR-08

Only a Healthcare Officer can record the final assessment

MF01-BR-09

Final assessment stores citizen, campaign, submission, questionnaire version, reviewer, status, reason, reviewed_at and valid_until

MF01-BR-10

Eligibility starts at reviewed_at and remains valid until the earlier of valid_until and campaign end, unless revoked

MF01-BR-11

Publishing a newer questionnaire version does not automatically invalidate an existing assessment; explicit expiry or revocation is required

MF01-BR-12

Appointment booking and vaccination finalization both recheck that the assessment is currently valid

MF01-BR-13

A new submission is blocked while an ELIGIBLE assessment is current; it is allowed after EXPIRED, and after INELIGIBLE or REVOKED only when a Healthcare Officer records rescreening authorization and reason

MF01-BR-14

Every submitted screening records the immutable consent/privacy notice version accepted by the citizen and the consent timestamp

5.4 Separate Status Models

QuestionnaireVersionStatus

DRAFT, PUBLISHED, RETIRED

ScreeningSubmissionStatus

DRAFT, PENDING_REVIEW, CORRECTIONS_REQUIRED, REVIEWED

PreliminaryScreeningResult

PRELIMINARY_PASS, PRELIMINARY_REVIEW_REQUIRED

EligibilityAssessmentStatus

ELIGIBLE, INELIGIBLE, EXPIRED, REVOKED

These are separate enums. Do not combine them. EXPIRED is an effective state: current eligibility becomes false immediately when valid_until or the campaign end passes, even if a scheduled job has not yet persisted the label.

5.5 Function-Level User Story Examples (Canonical)

ID

User story

MF01-US-01

As an Admin, I can create, view, update and delete an unused screening question so MF-01 demonstrates safe CRUD

MF01-US-02

As an Admin, I can publish an immutable questionnaire version after validation

MF01-US-03

As a Citizen, I can complete the correct questionnaire for the selected campaign

MF01-US-04

As a Healthcare Officer, I can review the submission and record a reasoned final decision

MF01-US-05

As a Citizen, I can see a different eligibility status for each campaign

5.6 Acceptance Criteria

Deleting a referenced question, option, rule or published version returns a conflict error and changes no data.

A citizen cannot submit without consent or required answers.

A final assessment cannot be created by a non-Healthcare Officer.

Every final assessment references the exact questionnaire version used.

Booking fails when the assessment is missing, expired, revoked, ineligible or belongs to another campaign.

The UI calls the rule result preliminary and never presents it as a diagnosis.

Rescreening follows the explicit ELIGIBLE/INELIGIBLE/EXPIRED/REVOKED rules and never overwrites historical submissions or assessments.

A submitted screening retains the exact consent/privacy notice version accepted at submission time.

5.7 Evidence to Collect

Question/option CRUD screen and validation.

Rule builder or rule form.

Questionnaire version publish flow.

Citizen campaign screening form.

Healthcare Officer review screen.

Tests for immutable versions, deletion protection and campaign-specific eligibility.

6. MF-02 Vaccination Campaign, Center and Session Management

Attribute

Baseline

Purpose

Plan and control when, where, for whom and with what operational capacity vaccination services are offered

Primary users

Campaign Officer, Center Manager, System Administrator

Main records

VaccineType, TargetGroup, CampaignDoseProgramme, VaccinationCampaign, CampaignLocationTarget, CampaignQuestionnaireAssignment, VaccinationCenter, CampaignCenterAssignment, OperatingHours, CenterClosure, SessionRecurrenceRule, VaccinationSession, TimeSlot, OperationalResource, SessionResourceAssignment, StaffAssignment, SessionCapacityPlan, SessionChangeEvent

Ownership boundary

MF-02 owns service planning. MF-01 owns detailed eligibility logic; MF-03 owns appointments/waitlists/check-in; MF-04 owns vaccine batches and quantities; MF-05 owns administration records

6.1 Core Capabilities

Create and manage the shared VaccineType master and target-group descriptions. MF-04 reads VaccineType but does not own its lifecycle.

Configure a campaign dose programme: primary or booster purpose, permitted dose numbers, minimum interval, and whether previous-dose evidence is required. These are programme constraints, not detailed eligibility rules.

Create campaigns with lifecycle dates, citizen-visible booking windows and administrative location targets such as province, district, health region or selected centers.

Link a campaign to exactly one MF-01 published questionnaire version for each effective period.

Create and maintain vaccination centers, normal operating hours, maximum safe concurrent occupancy, optional per-session service ceiling, closure dates and temporary unavailability.

Assign centers to campaigns through effective-dated CampaignCenterAssignment records. Operational assignment is separate from citizen-facing administrative location targeting.

Define recurring-session rules such as selected weekdays, recurrence start/end and session time; generate individual sessions from the rule.

Edit or cancel one recurrence occurrence without altering the remaining series. A series-wide edit applies only to eligible future occurrences and must show impact before confirmation.

Create individual sessions and time slots inside campaign dates, booking windows and center availability.

Assign staff and physical resources such as rooms, vaccination stations and required equipment without time conflicts.

Calculate recommended session throughput from session duration, expected service time per citizen and active vaccination stations. Validate simultaneous occupancy separately against the center safe-concurrent-occupancy limit.

Allow an authorized manager to set approved capacity. Capacity above recommended throughput requires a reason and sufficient resources; it must not exceed an optional center per-session service ceiling. Concurrent occupancy must independently remain within the center safe-concurrent-occupancy limit.

Display confirmed bookings, waitlist count, remaining places, occupancy percentage and resource utilization using aggregate MF-03 data.

Suspend new bookings, reschedule or cancel a session with a mandatory reason and an impact preview.

Create a SessionChangeEvent for any material change. MF-03 consumes the event to preserve appointment history and perform rebooking, cancellation or waitlist handling.

Provide operational dashboards for campaign progress, session utilization, center utilization, cancellations and resource load.

Complete or archive campaigns and sessions without deleting operational history.

6.2 Responsibility Boundaries

Concern

Owning function

MF-02 responsibility

Eligibility questions/rules and final decision

MF-01

Select only a published questionnaire version; do not edit its rules

Citizen appointment, waitlist, rebooking and check-in

MF-03

Publish availability and schedule-change events; do not directly rewrite appointment history

Batch inventory and stock reservation/deduction

MF-04

May display aggregate readiness indicators, but does not own quantities or batch selection

Dose sequence validation during administration

MF-05

Defines programme constraints; MF-05 revalidates them at finalization

Notifications

Shared service

Requests notifications for campaign/session lifecycle events; shared service sends them

6.3 CRUD and Record Lifecycle

Record

Create

Read

Update

Delete/transition

VaccineType/TargetGroup

Yes

Yes

Yes while safe

Deactivate when referenced

CampaignDoseProgramme

Yes

Yes

Future-safe fields

Deactivate/supersede when referenced; preserve historical version

Campaign/LocationTarget

Yes

Yes

Future-safe fields

Cancel/complete/archive; no hard delete after dependencies

CampaignCenterAssignment

Yes

Yes

Future effective dates/status

End assignment; retain once referenced by a session

Center/OperatingHours

Yes

Yes

Yes

Deactivate when referenced

CenterClosure

Yes

Yes

Future closure edits

Cancel closure; retain history when it affected sessions

SessionRecurrenceRule

Yes

Yes

Future occurrences only

End/cancel rule; generated sessions remain separate records

Session/TimeSlot

Yes

Yes

Future-safe fields through change workflow

Cancel/close; no hard delete after booking or generated history

OperationalResource

Yes

Yes

Yes while safe

Maintenance/inactivate when referenced

Staff/ResourceAssignment

Yes

Yes

Future assignments

Remove if no historical service conflict

SessionCapacityPlan

Generated/approved

Yes

Recalculate or override with reason

Retain each approved revision used for bookings

SessionChangeEvent

System/authorized action

Yes

Processing metadata only

Append-only business event; consumption tracked per consumer

6.4 Capacity Calculation Baseline

The effective staffed station count is:

effective_station_count = min(assigned_vaccinator_count, available_vaccination_station_count, fully_equipped_station_count)

The recommended total session throughput is:

recommended_throughput = effective_station_count × floor(session_duration_minutes ÷ expected_service_minutes_per_citizen)

Each effective station represents one vaccination station with one non-conflicting assigned vaccinator and all required equipment for the relevant service window. Extra physical stations without staff, or extra staff without a complete station bundle, do not increase throughput.

The approved session capacity is:

approved_capacity ≤ optional_center_session_service_ceiling when that ceiling is configured.

For each slot, define its expected occupancy interval as:

[slot_start, max(slot_end, slot_start + expected_dwell_minutes))

and its expected occupancy units as:

slot_capacity × (1 + expected_accompanying_persons_per_booking)

At every slot start/end or derived dwell-end boundary, the sum of expected occupancy units for overlapping intervals must be less than or equal to safe_concurrent_occupancy. expected_accompanying_persons_per_booking is a stored planning value from 0.0 to 1.0, default 0.0. This safety calculation is independent of total session throughput.

Baseline defaults:

expected_service_minutes_per_citizen = 10 unless the campaign dose programme defines another positive value.

effective_station_count includes only complete staffed/equipped station bundles available for the full relevant service window.

The sum of time-slot capacities must not exceed the approved session capacity. Every slot combination must also pass the exact concurrent-occupancy calculation.

Approved capacity may be lower than the recommendation.

An authorized override may exceed recommended throughput only with a recorded reason and sufficient staff/resources. It must not exceed an optional per-session service ceiling, and it must still pass the concurrent-occupancy validation.

Capacity cannot be reduced below active confirmed appointments. The system must use the MF-03 authoritative booking count.

6.5 Key Rules and Validations

Rule ID

Rule

MF02-BR-01

Campaign end date is after start date; booking-open/close times are internally consistent

MF02-BR-02

Campaign has one vaccine type, one target-group description, and exactly one effective dose-programme version and published questionnaire assignment at any instant before publication; historical versions may be retained

MF02-BR-03

PUBLISHED and ACTIVE campaigns may accept screening/booking only inside configured booking windows; SUSPENDED, COMPLETED, CANCELLED and ARCHIVED campaigns do not accept new bookings

MF02-BR-04

Administrative location targeting controls campaign visibility/service area and is not a medical eligibility decision

MF02-BR-05

Session date/time falls inside campaign service dates, center operating hours and an active effective-dated CampaignCenterAssignment

MF02-BR-06

A session cannot overlap a center closure or an unavailable resource period

MF02-BR-07

A recurrence rule must have a valid interval, at least one occurrence pattern and an end boundary; generated occurrences are individually identifiable

MF02-BR-08

Editing one recurrence occurrence does not alter the rest of the series

MF02-BR-09

Staff, room, station and equipment assignments cannot overlap for the same resource

MF02-BR-10

Recommended throughput is calculated from duration, expected service time and active stations; safe concurrent occupancy is validated separately

MF02-BR-11

Slot capacity is a positive integer and total slot capacity does not exceed approved session capacity

MF02-BR-12

Capacity cannot be reduced below active confirmed appointments

MF02-BR-13

Remaining capacity = approved capacity - active confirmed appointments; waitlist entries do not consume confirmed capacity

MF02-BR-14

Booking availability is system-derived: FULL when remaining capacity is zero and OPEN again if capacity becomes available before booking closure, unless manually suspended

MF02-BR-15

Material changes to date, time, center or cancellation require impact preview, reason and SessionChangeEvent

MF02-BR-16

MF-02 never silently rewrites appointments. MF-03 processes affected appointments and waitlists transactionally and retains history

MF02-BR-17

A campaign questionnaire link changes only for future submissions; existing submissions retain their exact version reference

MF02-BR-18

A campaign dose-programme change applies only to future screening/booking unless an authorized migration explicitly records impact

MF02-BR-19

Operational analytics use aggregate counts and do not expose screening answers, medical notes or unnecessary citizen identity data

MF02-BR-20

Session completion and campaign completion do not delete sessions, assignments, capacity plans or change events

MF02-BR-21

Effective station count is limited by the smallest of assigned vaccinators, available vaccination stations and fully equipped station bundles for the service window

MF02-BR-22

Concurrent occupancy is calculated from overlapping slot/dwell intervals and expected accompanying-person units and must not exceed the center safety limit at any boundary

6.6 Separate Status Models

CampaignStatus

DRAFT, SCHEDULED, PUBLISHED, ACTIVE, SUSPENDED, COMPLETED, CANCELLED, ARCHIVED

DRAFT: editable and hidden from citizens.

SCHEDULED: internally approved with future publication/service dates.

PUBLISHED: citizen-visible; screening and booking depend on configured windows.

ACTIVE: service period is underway.

SUSPENDED: temporarily blocks new screening and booking while preserving records.

COMPLETED, CANCELLED, ARCHIVED: terminal or retained-history states.

SessionLifecycleStatus

DRAFT, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED

This enum represents the operational lifecycle only. CANCELLED is terminal; COMPLETED preserves all history.

SessionBookingStatus

NOT_OPEN, OPEN, FULL, CLOSED, SUSPENDED

OPEN, FULL and time-based CLOSED are derived from campaign/session windows, approved capacity and the authoritative confirmed-booking count. SUSPENDED is an explicit authorized override with a reason. Booking status never replaces or contradicts lifecycle status.

CenterStatus

ACTIVE, INACTIVE

Temporary closure is represented by CenterClosure, not by changing the center's long-term status.

OperationalResourceStatus

ACTIVE, MAINTENANCE, INACTIVE

Do not place campaign, session, center and resource statuses in the same enum.

6.7 Acceptance Criteria

An invalid campaign date range, missing dose programme or missing published questionnaire assignment blocks publication.

A campaign can target selected administrative areas or centers without creating an MF-01 medical rule.

A recurring rule generates the correct individual sessions and prevents duplicates when retried.

Editing one generated occurrence leaves all other occurrences unchanged.

A session outside campaign dates, center hours, or inside a closure cannot be saved.

Staff and physical-resource overlap is rejected with the conflicting assignment identified.

Recommended throughput is reproducible from stored duration, service time, assigned vaccinator, station and equipment inputs; approved capacity respects any per-session service ceiling.

Every overlapping slot/dwell interval passes the exact safe-concurrent-occupancy calculation including the stored accompanying-person factor.

Capacity override above the recommendation requires authorized role, reason and sufficient assigned resources.

Concurrent booking calculations use the same authoritative capacity and confirmed-count sources as the UI.

A session change with bookings displays affected counts before confirmation and creates one append-only change event.

MF-03, not MF-02, updates or cancels affected appointment and waitlist records.

Cancelled/archived records remain queryable for audit and reports.

MF-02 forms cannot create or edit MF-01 eligibility rules or MF-04 batch quantities.

Operational dashboards reconcile with MF-03 aggregate booking data.

6.8 Evidence to Collect

Campaign lifecycle, location-target and dose-programme screens.

Published-questionnaire selector.

Center operating-hours, safe-concurrent-occupancy, optional session-service-ceiling and closure forms.

Recurring-session rule and generated-occurrence calendar.

Single-occurrence edit and series-edit impact preview.

Session/slot capacity calculation and authorized override evidence.

Staff and physical-resource conflict validation.

Session suspension/reschedule/cancellation impact report and change-event audit.

Campaign, center, session and resource-utilization dashboard.

7. MF-03 Appointment, Waitlist and Check-In Management

Attribute

Baseline

Purpose

Allow eligible citizens to reserve valid capacity and support deterministic waitlist and arrival processing

Primary users

Citizen, Receptionist, Center Staff

Main records

Appointment, AppointmentChangeEvent, WaitlistEntry, WaitlistChangeEvent, CheckInEvent, ModuleEventConsumption

7.1 Core Capabilities

Search available sessions and slots for a selected currently bookable PUBLISHED or ACTIVE campaign.

Book one available slot.

View, reschedule and cancel an appointment.

Join or leave a session waitlist when no slot is available.

Promote the earliest still-valid waitlist entry when capacity opens.

Validate appointment identity, center, session and time at check-in.

Record no-show and authorized check-in reversal.

Consume each MF-02 SessionChangeEvent exactly once per MF-03 consumer using ModuleEventConsumption; preserve old/new schedule details, update affected appointments or waitlists through normal MF-03 transitions and request citizen notifications.

Do not automatically move a citizen to a different session without an explicit citizen or authorized-staff reschedule action.

7.2 CRUD and Record Lifecycle

Appointments are operational records and are not hard-deleted. Citizens create bookings and request state transitions; staff can perform authorized corrections with reasons. Waitlist entries are retained with terminal statuses. Their transitions are recorded in append-only WaitlistChangeEvent records.

7.3 Key Rules and Validations

Rule ID

Rule

MF03-BR-01

Booking requires a currently valid ELIGIBLE assessment for the same campaign

MF03-BR-02

One active appointment per citizen per campaign is enforced by a unique constraint or equivalent locking strategy

MF03-BR-03

Capacity allocation is atomic and prevents two users from taking the last place

MF03-BR-04

Citizen self-cancellation/rescheduling is allowed until 24 hours before slot start; later changes require staff override with a reason

MF03-BR-05

Rescheduling releases the old capacity and reserves the new capacity in one transaction

MF03-BR-06

Waitlist order is joined_at ascending, then waitlist_entry_id ascending; only one WAITING entry is allowed per citizen/session

MF03-BR-07

Promotion rechecks eligibility, campaign status and duplicate appointment rules; invalid entries are skipped and marked appropriately

MF03-BR-08

Successful promotion creates a confirmed appointment and marks the waitlist entry PROMOTED

MF03-BR-09

Standard check-in window is 30 minutes before to 30 minutes after slot start; staff override requires a reason

MF03-BR-10

Check-in is allowed only at the appointment center and for a CONFIRMED appointment

MF03-BR-11

A COMPLETED appointment cannot be cancelled, rescheduled or checked in again

MF03-BR-12

Each MF-02 session-change event is processed exactly once per consumer using a unique (consumer_code, source_event_id) record; processing status, attempts, completion time and error are retained

MF03-BR-13

A cancelled session cancels affected appointments and waitlist entries with linked appointment/waitlist change events and a source reason; rebooking uses the normal atomic reschedule/book flow and is never silent or automatic

MF03-BR-14

A citizen cannot hold a WAITING entry for a session when that citizen already has an active appointment for the same campaign; promotion is protected by the one-active-appointment constraint

MF03-BR-15

Check-in reversal returns the appointment from CHECKED_IN to CONFIRMED, requires a reason, and is rejected after vaccination finalization or appointment completion

MF03-BR-16

If an unfinalized vaccination draft exists, check-in reversal is rejected until the draft is deleted or explicitly invalidated through authorized MF-05 workflow

7.4 Separate Status Models

AppointmentStatus

CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW

WaitlistStatus

WAITING, PROMOTED, SKIPPED_INELIGIBLE, SKIPPED_DUPLICATE_APPOINTMENT, CANCELLED, SESSION_CANCELLED

CheckInEventType

CHECKED_IN, REVERSED

A valid reversal changes the appointment back to CONFIRMED. A reversal cannot occur after a permanent VaccinationRecord exists.

7.5 Acceptance Criteria

Ineligible or expired users cannot book.

Two concurrent users cannot both receive the final capacity place.

Session-change processing is idempotent and preserves old/new schedule history.

A cancelled session does not silently move citizens to another session.

Rescheduling is all-or-nothing.

Cancellation immediately triggers availability recalculation and waitlist promotion.

Wrong-center or out-of-window check-in is rejected unless an authorized override reason is recorded.

7.6 Evidence to Collect

Availability search and booking confirmation.

Last-slot concurrency test.

Reschedule/cancel flow.

Waitlist order and promotion test.

Check-in and override audit evidence.

8. MF-04 Vaccine Inventory and Batch Management

Attribute

Baseline

Purpose

Maintain reliable batch master data, center-level stock and append-only movement history

Primary users

Inventory Officer; read access for Center Manager and Vaccinator

Main records

Manufacturer, VaccineBatch, CenterInventory, StockTransaction

8.1 Core Capabilities

Register manufacturers and vaccine batches.

Record batch number, vaccine type, manufacture date, expiry date and batch state.

Receive and allocate stock to centers.

Transfer stock between centers through paired transactions.

Record authorized positive or negative adjustments with reasons.

Display current quantity by center, vaccine type and batch.

Calculate low-stock and near-expiry alerts.

Prevent unavailable, expired, recalled, quarantined or mismatched stock from administration.

8.2 Record Model and Lifecycle

VaccineBatch contains lot-level facts and no center quantity. CenterInventory contains the available quantity for one center-batch pair. StockTransfer is the correlation record for one source-to-destination movement. StockTransaction is append-only and is the audit history for every quantity change. VaccineType is shared master data owned by MF-02 and referenced read-only by MF-04.

Batch master data can be corrected only before transactions exist. Once referenced, corrections use controlled status changes or correction notes rather than deletion.

8.3 Key Rules and Validations

Rule ID

Rule

MF04-BR-01

Batch number is unique within manufacturer and vaccine type

MF04-BR-02

Manufacture date is before expiry date

MF04-BR-03

Quantities are stored in dose units; the baseline administration quantity is one dose

MF04-BR-04

No transaction may make center inventory negative

MF04-BR-05

Transfer creates an outbound transaction at the source and inbound transaction at the destination in one database transaction

MF04-BR-06

Adjustment requires reason, actor and timestamp

MF04-BR-07

Vaccination deduction is linked one-to-one with the permanent vaccination record

MF04-BR-08

Selectable inventory must be ACTIVE, not expired, not recalled/quarantined, located at the appointment center, match campaign vaccine type and have sufficient quantity

MF04-BR-09

Low-stock alert is calculated when total active quantity for a center-vaccine type is at or below 10 doses

MF04-BR-10

Near-expiry alert is calculated when an active batch expires within 30 days and quantity is greater than zero

MF04-BR-11

Every transfer has one StockTransfer correlation ID and exactly one paired TRANSFER_OUT and TRANSFER_IN transaction committed atomically

MF04-BR-12

A batch is operationally expired immediately after expiry_date; selection rejects it even if a scheduled status update has not yet persisted EXPIRED

8.4 Separate Status and Alert Models

BatchStatus

ACTIVE, QUARANTINED, RECALLED, EXPIRED, INACTIVE

StockTransactionType

RECEIPT, TRANSFER_OUT, TRANSFER_IN, ADJUSTMENT_INCREASE, ADJUSTMENT_DECREASE, VACCINATION_DEDUCTION

Calculated alerts

low_stock and near_expiry are calculated flags, not mutually exclusive batch statuses. EXPIRED is also enforced as an effective date-derived state, independent of background-job timing.

8.5 Acceptance Criteria

Duplicate batch and invalid date relationships are rejected.

Transfer or deduction exceeding available stock rolls back fully.

Expired/recalled/quarantined batches are never selectable for finalization.

Every quantity change has a transaction with before quantity, change quantity and after quantity.

Vaccination finalization deducts exactly one configured dose unit once.

8.6 Evidence to Collect

Batch registration form.

Center stock view.

Receipt, transfer and adjustment flows.

Low-stock and near-expiry calculation tests.

Negative-stock and duplicate-deduction prevention tests.

9. MF-05 Vaccination Administration, Draft Records and Digital Certificate Management

Attribute

Baseline

Purpose

Prepare editable administration data safely, finalize the physical vaccination atomically and manage immutable records and versioned certificates

Primary users

Vaccinator, Citizen, Healthcare Officer, authorized Certificate Officer, Public Verifier

Main records

VaccinationDraft, VaccinationRecord, VaccinationCorrection, CertificateIssue, CertificateVerificationLog, ApiIdempotencyRecord

9.1 Core Capabilities

List checked-in citizens whose appointment and eligibility are valid.

Create an unfinalized draft linked to citizen, appointment, campaign, center and vaccinator.

Read a draft list and detail view.

Update selected inventory, dose number, administration time, next-dose date and authorized notes.

Permanently delete an unfinalized draft with a reason.

Validate eligibility, appointment, check-in, dose sequence, vaccine type and batch.

Require explicit confirmation that physical administration occurred.

Finalize through one idempotent database transaction.

Display immutable vaccination history.

Automatically issue certificate version 1.

Revoke and reissue certificates with audit and minimum-data public verification.

Record corrections without directly rewriting the original vaccination record.

9.2 Draft CRUD and Permanent Record Lifecycle

Operation

VaccinationDraft

VaccinationRecord

Create

Authorized Vaccinator after valid check-in

Created only by finalization service

Read

Owner and authorized staff

Citizen owner and authorized staff

Update

Allowed while unfinalized

Direct update prohibited

Delete

Hard delete allowed while unfinalized, with audit reason

Hard delete prohibited

Finalize

Converted through atomic service; draft removed

Becomes immutable operational history

Draft CRUD has no inventory, appointment-completion, history or certificate side effects.

9.3 Atomic Finalization Contract

Finalization must execute the following steps inside one database transaction:

Accept a client idempotency key and lock the appointment, draft and center-inventory row.

Verify the draft exists and belongs to the authenticated authorized Vaccinator.

Verify appointment status is CHECKED_IN and no permanent record already exists for the appointment.

Recheck the citizen's current ELIGIBLE assessment for the same campaign.

Revalidate batch status, expiry, recall/quarantine state, center, vaccine type and quantity.

Validate dose sequence and required fields.

Verify explicit administration_confirmed = true.

Create VaccinationRecord with a unique appointment_id.

Create one VACCINATION_DEDUCTION stock transaction and decrement center inventory.

Transition appointment to COMPLETED.

Create CertificateIssue version 1 with an opaque reference and QR verification token.

Delete the finalized draft.

Write audit entries and enqueue the certificate-ready notification.

Commit all changes. Any failure rolls back every step.

Unique constraints on VaccinationRecord.appointment_id, StockTransaction.vaccination_record_id and ApiIdempotencyRecord(actor_id, operation_code, idempotency_key) prevent duplicate finalization. Reuse with a different request fingerprint returns a deterministic conflict.

9.4 Key Rules and Validations

Rule ID

Rule

MF05-BR-01

A draft requires an authorized Vaccinator, valid appointment and successful check-in

MF05-BR-02

At most one active draft exists per appointment

MF05-BR-03

Draft delete is rejected after finalization

MF05-BR-04

Batch must belong to the appointment center and match the campaign vaccine type

MF05-BR-05

Finalization rechecks all dependencies even if the draft previously showed valid

MF05-BR-06

The original vaccination record is immutable; corrections are append-only and effective history is read through the original record plus all APPLIED correction overlays

MF05-BR-07

The initial certificate is automatically issued during finalization; users do not run a separate initial generate use case

MF05-BR-08

A revoked certificate remains stored and verifies as REVOKED

MF05-BR-09

Reissue creates a new CertificateIssue version; the previous valid issue becomes SUPERSEDED unless it was revoked

MF05-BR-10

QR content contains only a verification URL/token, not raw personal or medical data

MF05-BR-11

Public verification is rate-limited and logged without exposing sensitive profile data

MF05-BR-12

Approval alone does not alter effective data; applying a correction records field-level old/new values and creates the effective corrected projection

MF05-BR-13

Applying a correction that changes certificate-visible data supersedes the current valid certificate and requires issuance of a new version; the previous issue remains auditable

MF05-BR-14

Correctable fields and approver roles are explicitly allow-listed; identity of citizen, appointment linkage, stock transaction identity and original administration event are never silently rewritten

9.5 Separate Status Models

VaccinationDraftStatus

DRAFT, READY_FOR_FINALIZATION

CertificateStatus

VALID, REVOKED, SUPERSEDED

VaccinationCorrectionStatus

PENDING, APPROVED, REJECTED, APPLIED

APPROVED means authorized for application but not yet effective. APPLIED means the field-level correction overlay is included in the effective vaccination-history projection. VaccinationRecord remains immutable and does not reuse draft or certificate statuses.

9.6 Certificate Lifecycle

Version 1 is automatically created after successful finalization.

Citizen and authorized staff may view/download a valid issue.

Authorized certificate staff may revoke with a reason.

Reissue creates version n + 1 with a new reference/token.

Public verification returns:

verification result: VALID, REVOKED or INVALID;

certificate reference;

citizen display name in masked or approved form;

vaccine name, dose number and administration date;

issuing organization;

no national ID, address, phone, email, screening answers or clinical notes.

9.7 Acceptance Criteria

Draft create/read/update/delete changes no stock or permanent history.

Finalization with insufficient stock, invalid eligibility or wrong batch leaves all records unchanged.

Retrying a successful request cannot create another record, deduction or certificate issue.

A finalized record cannot be directly edited or deleted.

Reissue produces a new certificate version and preserves previous issues.

Public verification reveals only the approved minimum fields.

9.8 Evidence to Collect

Checked-in work queue.

Draft CRUD screens.

Batch validation and administration confirmation dialog.

Transaction rollback and idempotency tests.

Vaccination history and certificate download.

Revocation, reissue and public verification tests.

10. Supporting Minor Functions

Function

Baseline behaviour

Account and profile

Citizen self-registration, email verification, profile maintenance; staff accounts admin-created

Password reset

Single-use expiring token; old sessions revoked after reset

Notifications

In-app and email for eligibility decision, booking, cancellation, waitlist promotion, 24-hour reminder, certificate ready/revoked; requests are persisted through the transactional outbox

Audit

Actor, action, resource type/id, timestamp, result and reason for security or operational transitions

Dashboard

Role-specific counts and alerts; no unrestricted personal-data browsing

Search/filter

Server-side pagination, validated sort fields and role-scoped results

Reporting

Authorized aggregate reports and CSV export; certificate PDF generation is part of MF-05

Public verification

Opaque reference/QR token, rate limiting and minimum-data response

SMS, multilingual support and predictive analytics are future options, not core requirements; MF-02 descriptive operational utilization is core.

11. Non-Functional Requirements

ID

Category

Requirement

NFR-SEC-01

Security

TLS is required outside local development; passwords are salted/hashed through Spring Security-compatible encoders

NFR-SEC-02

Security

Backend authorization is enforced on every protected endpoint and object-level access is checked

NFR-SEC-03

Security

Tokens, secrets and database credentials are stored in environment configuration, never source control

NFR-PRV-01

Privacy

Collect and expose only data required for the current role and task

NFR-PRV-02

Privacy

Demonstrations use synthetic/anonymized data

NFR-PERF-01

Performance

Normal list/detail operations should complete within 3 seconds in the evaluation environment under expected academic load

NFR-CON-01

Concurrency

Booking, rescheduling, stock transfer and finalization use transactions/locking to prevent race conditions

NFR-REL-01

Reliability

Multi-record operations are atomic and idempotent where retries are possible

NFR-REL-02

Reliability

Asynchronous module actions and notifications use a transactional outbox, idempotent consumers, retry scheduling and dead-letter handling

NFR-USA-01

Usability

Responsive UI, clear status text, field-level validation and accessible labels

NFR-USA-02

Accessibility

Keyboard navigation, visible focus, semantic headings and non-color status indicators

NFR-MNT-01

Maintainability

Module packages, DTOs, services, repositories, migrations and tests follow consistent conventions

NFR-TST-01

Testability

Critical business rules have unit and integration tests; APIs are documented in OpenAPI

NFR-AUD-01

Auditability

Security-sensitive and irreversible actions produce audit records

NFR-BKP-01

Recovery

Demo database can be restored from migration scripts and controlled seed data

NFR-CMP-01

Compatibility

Supports current desktop/mobile versions of major Chromium, Firefox and Safari browsers used in evaluation

12. Data Requirements and Conceptual Data Model

12.1 Conceptual Relationships

erDiagram
    USER_ACCOUNT ||--o| CITIZEN_PROFILE : owns
    USER_ACCOUNT }o--o{ ROLE : assigned
    USER_ACCOUNT ||--o{ EMAIL_VERIFICATION_TOKEN : verifies_with
    USER_ACCOUNT ||--o{ PASSWORD_RESET_TOKEN : resets_with
    USER_ACCOUNT ||--o{ REFRESH_TOKEN_SESSION : authenticates_with
    VACCINATION_CAMPAIGN }o--|| VACCINE_TYPE : uses
    VACCINATION_CAMPAIGN ||--o{ CAMPAIGN_DOSE_PROGRAMME : versions
    VACCINATION_CAMPAIGN ||--o{ CAMPAIGN_LOCATION_TARGET : targets
    VACCINATION_CAMPAIGN ||--o{ CAMPAIGN_QUESTIONNAIRE_ASSIGNMENT : assigns
    QUESTIONNAIRE ||--o{ QUESTIONNAIRE_VERSION : versions
    QUESTIONNAIRE_VERSION ||--o{ QUESTIONNAIRE_ITEM : contains
    SCREENING_QUESTION ||--o{ QUESTIONNAIRE_ITEM : referenced_by
    SCREENING_QUESTION ||--o{ ANSWER_OPTION : has
    QUESTIONNAIRE_VERSION ||--o{ ELIGIBILITY_RULE : snapshots
    CITIZEN_PROFILE ||--o{ SCREENING_SUBMISSION : creates
    VACCINATION_CAMPAIGN ||--o{ SCREENING_SUBMISSION : receives
    CONSENT_NOTICE_VERSION ||--o{ SCREENING_SUBMISSION : accepted_by
    SCREENING_SUBMISSION ||--o{ SCREENING_ANSWER : contains
    SCREENING_SUBMISSION ||--o| ELIGIBILITY_ASSESSMENT : reviewed_as
    VACCINATION_CAMPAIGN ||--o{ CAMPAIGN_CENTER_ASSIGNMENT : assigns
    VACCINATION_CENTER ||--o{ CAMPAIGN_CENTER_ASSIGNMENT : serves
    VACCINATION_CENTER ||--o{ OPERATING_HOURS : defines
    VACCINATION_CENTER ||--o{ CENTER_CLOSURE : blocks
    VACCINATION_CAMPAIGN ||--o{ SESSION_RECURRENCE_RULE : defines
    SESSION_RECURRENCE_RULE ||--o{ VACCINATION_SESSION : generates
    VACCINATION_CAMPAIGN ||--o{ VACCINATION_SESSION : schedules
    VACCINATION_CENTER ||--o{ VACCINATION_SESSION : hosts
    VACCINATION_SESSION ||--o{ TIME_SLOT : contains
    VACCINATION_SESSION ||--o{ STAFF_ASSIGNMENT : staffed_by
    OPERATIONAL_RESOURCE ||--o{ SESSION_RESOURCE_ASSIGNMENT : allocated_as
    VACCINATION_SESSION ||--o{ SESSION_RESOURCE_ASSIGNMENT : uses
    VACCINATION_SESSION ||--o{ SESSION_CAPACITY_PLAN : planned_by
    VACCINATION_SESSION ||--o{ SESSION_CHANGE_EVENT : changed_by
    SESSION_CHANGE_EVENT ||--o{ MODULE_EVENT_CONSUMPTION : consumed_by
    CITIZEN_PROFILE ||--o{ APPOINTMENT : books
    TIME_SLOT ||--o{ APPOINTMENT : reserves
    APPOINTMENT ||--o{ APPOINTMENT_CHANGE_EVENT : changes
    VACCINATION_SESSION ||--o{ WAITLIST_ENTRY : queues
    WAITLIST_ENTRY ||--o{ WAITLIST_CHANGE_EVENT : changes
    APPOINTMENT ||--o{ CHECK_IN_EVENT : records
    VACCINE_BATCH ||--o{ CENTER_INVENTORY : stocked_as
    VACCINATION_CENTER ||--o{ CENTER_INVENTORY : holds
    CENTER_INVENTORY ||--o{ STOCK_TRANSACTION : changes
    STOCK_TRANSFER ||--|{ STOCK_TRANSACTION : posts
    APPOINTMENT ||--o| VACCINATION_DRAFT : prepares
    APPOINTMENT ||--o| VACCINATION_RECORD : completes
    VACCINATION_RECORD ||--o{ VACCINATION_CORRECTION : corrected_by
    VACCINATION_RECORD ||--o{ CERTIFICATE_ISSUE : evidenced_by
    CERTIFICATE_ISSUE ||--o{ CERTIFICATE_VERIFICATION_LOG : checked_in
    USER_ACCOUNT ||--o{ API_IDEMPOTENCY_RECORD : submits
    TRANSACTIONAL_OUTBOX_EVENT ||--o{ MODULE_EVENT_CONSUMPTION : consumed_as

12.2 Core Entity Catalogue

Entity

Essential fields and constraints

UserAccount

id, email unique, password_hash, account_status, email_verified_at, locked_until nullable, created_at

EmailVerificationToken

id, user_id, token_hash unique, expires_at, used_at nullable, created_at; raw token never stored

PasswordResetToken

id, user_id, token_hash unique, expires_at, used_at nullable, created_at; single-use and revokes active sessions after successful reset

RefreshTokenSession

id, user_id, token_family_id, token_hash unique, rotated_from_id nullable, issued_at, expires_at, revoked_at nullable, reuse_detected_at nullable, client metadata minimized

Role/UserRole

stable role code; many-to-many assignment

CitizenProfile

user_id unique, identity_number unique, name, date_of_birth, contact data

VaccineType

id, code unique, name, dose_unit, active

TargetGroup

id, code unique, description, active; not a detailed eligibility rule

VaccinationCampaign

id, code unique, vaccine_type_id, target_group_id, service dates, booking window, campaign_status

CampaignDoseProgramme

id, campaign_id, version_number, dose_category, permitted_dose_numbers, minimum_interval_days, previous_evidence_required, expected_service_minutes, active_from/to; no overlapping effective periods per campaign

CampaignLocationTarget

id, campaign_id, target_type (PROVINCE, DISTRICT, HEALTH_REGION, CENTER), target_code; unique campaign/type/code; controls visibility/service area, not operational assignment

CampaignQuestionnaireAssignment

id, campaign_id, questionnaire_version_id, active_from, active_to; version must be published; no overlapping effective periods per campaign

CampaignCenterAssignment

id, campaign_id, center_id, active_from, active_to, assignment_status; unique effective campaign/center period and required by sessions

ConsentNoticeVersion

id, notice_code, version_number, content_hash, published_at, retired_at nullable; immutable after publication; unique notice/version

ScreeningQuestion

id, code unique, text, type, required/default metadata, active

AnswerOption

id, question_id, code, label, typed value, display_order

EligibilityRule

id, rule_code, questionnaire_version_id, condition JSON/normalized conditions, preliminary result, priority

Questionnaire

id, code unique, name

QuestionnaireVersion

id, questionnaire_id, version_number, status, published_at; unique parent/version

QuestionnaireItem

version_id, question_id, display_order, required override; unique version/order

ScreeningSubmission

id, citizen_id, campaign_id, questionnaire_version_id, consent_notice_version_id, status, consent_at, submitted_at; one active per citizen/campaign

ScreeningAnswer

submission_id, question_id, typed answer; unique submission/question

EligibilityAssessment

submission_id unique, status, preliminary_result, reviewer_id, reason, reviewed_at, valid_until, revoked_at, rescreening_authorized_at/by/reason nullable

VaccinationCenter

id, code unique, name, address, contact, center_status, safe_concurrent_occupancy, session_service_ceiling nullable

OperatingHours

id, center_id, day_of_week/effective dates, opening, closing

CenterClosure

id, center_id, start/end, reason, status; no overlapping active duplicate closure

SessionRecurrenceRule

id, campaign_id, center_id, weekdays/pattern, start_date, end_date, start/end time, timezone, recurrence_status, generation_key unique

VaccinationSession

id, campaign_id, center_id, recurrence_rule_id nullable, occurrence_key unique nullable, start/end, booking_close_at, lifecycle_status, booking_status, booking_suspension_reason nullable

TimeSlot

id, session_id, start/end, capacity, lock_version

OperationalResource

id, center_id, code unique within center, resource_type (ROOM, VACCINATION_STATION, EQUIPMENT), name, resource_status

StaffAssignment

id, user_id, session_id, role, start/end; no overlap for same user

SessionResourceAssignment

id, resource_id, session_id, start/end; no overlap for same resource

SessionCapacityPlan

id, session_id, revision, duration_minutes, expected_service_minutes, assigned_vaccinator_count, available_station_count, fully_equipped_station_count, effective_station_count, recommended_throughput, approved_capacity, safe_concurrent_occupancy, expected_dwell_minutes, expected_accompanying_persons_per_booking, session_service_ceiling nullable, override_reason nullable, approved_by, created_at

SessionChangeEvent

id, session_id, change_type, old/new schedule snapshot, reason, affected_appointment_count, affected_waitlist_count, actor_id, created_at; append-only

ModuleEventConsumption

id, consumer_code, source_event_type, source_event_id, processing_status, attempt_count, processed_at, last_error; unique consumer/source event

TransactionalOutboxEvent

id, deduplication_key unique, aggregate_type/id, event_type, schema_version, minimized_payload, status, attempt_count, next_attempt_at, created_at, delivered_at nullable, last_error nullable

CitizenCampaignBookingGuard

citizen_id, campaign_id, lock_version; unique citizen/campaign row used only to serialize appointment/waitlist cross-table checks

Appointment

id, citizen_id, campaign_id, slot_id, status, booked_at, cancelled_at, lock_version; one active citizen/campaign

AppointmentChangeEvent

id, appointment_id, source_session_change_event_id nullable, change_type, old/new slot or schedule snapshot, reason, actor_id nullable, created_at; append-only

WaitlistEntry

id, citizen_id, campaign_id, session_id, status, joined_at; unique citizen/session while WAITING; cannot coexist with an active same-campaign appointment

WaitlistChangeEvent

id, waitlist_entry_id, source_session_change_event_id nullable, old_status, new_status, reason, actor_id nullable, created_at; append-only

CheckInEvent

id, appointment_id, event_type, actor_id, event_at, reason

Manufacturer

id, code/name unique

VaccineBatch

id, manufacturer_id, vaccine_type_id, batch_number, manufacture_date, expiry_date, batch_status

CenterInventory

id, center_id, batch_id, quantity, lock_version; unique center/batch

StockTransfer

id, correlation_id unique, batch_id, source_center_id, destination_center_id, quantity, status, requested_by, completed_at, reason

StockTransaction

id, inventory_id, stock_transfer_id nullable, type, delta, before_qty, after_qty, actor_id, reason, vaccination_record_id nullable, created_at

VaccinationDraft

id, appointment_id unique, inventory_id, vaccinator_id, dose_number, administered_at, next_dose_date, notes, draft_status, updated_at

VaccinationRecord

id, appointment_id unique, citizen_id, campaign_id, center_id, vaccine_type_id, batch_id, vaccinator_id, dose_number, administered_at, created_at

VaccinationCorrection

id, record_id, field_code, old_value_snapshot, proposed_value, reason, status, requested_by, approved_by, applied_by, approved_at, applied_at; append-only and allow-listed fields only

CertificateIssue

id, vaccination_record_id, version_number, reference unique, verification_token_hash unique, status, issued_at, revoked_at, reason; unique record/version

CertificateVerificationLog

id, certificate_issue_id nullable, token fingerprint, result, checked_at, network metadata minimized

ApiIdempotencyRecord

id, actor_id, operation_code, idempotency_key, request_fingerprint, processing_status, result_entity_type/id nullable, response_code nullable, created_at, completed_at, expires_at; unique actor/operation/key

Notification

id, recipient_user_id, type, channel, status, payload summary, created_at, sent_at, read_at

AuditLog

id, actor_id nullable, action, resource_type, resource_id, result, reason, created_at; append-only

12.3 Database Integrity Requirements

Use foreign keys for every cross-module reference.

Use unique constraints for identity number, email, campaign code, questionnaire version, campaign-location target, recurrence generation key/occurrence key, active citizen-campaign submission, active citizen-campaign appointment, appointment draft, appointment vaccination record, certificate reference and vaccination deduction.

Use optimistic locking (lock_version) or database row locks for time slots and inventory; recurring-session generation must also be idempotent through unique occurrence keys.

Use decimal/integer dose units consistently; baseline quantity is integer doses.

Store timestamps in UTC and render in the user's local timezone.

Use soft lifecycle transitions for operational records; hard deletion is limited to explicitly permitted entities.

Never store raw email-verification, password-reset, refresh or public-verification tokens; store secure hashes.

12.4 MySQL Conditional Uniqueness and Concurrency Baseline

MySQL does not provide general partial unique indexes. Conditional uniqueness must therefore use generated stored key columns plus ordinary unique indexes. Generated keys return the parent identifier only while the row is in an active state and return NULL otherwise; MySQL permits multiple NULL values in a unique index.

Invariant

Generated key and unique index baseline

One active screening submission per citizen/campaign

active_campaign_id = CASE WHEN status IN ('DRAFT','PENDING_REVIEW','CORRECTIONS_REQUIRED') THEN campaign_id ELSE NULL END; unique (citizen_id, active_campaign_id)

One active appointment per citizen/campaign

active_campaign_id = CASE WHEN status IN ('CONFIRMED','CHECKED_IN') THEN campaign_id ELSE NULL END; unique (citizen_id, active_campaign_id)

One waiting entry per citizen/session

waiting_session_id = CASE WHEN status = 'WAITING' THEN session_id ELSE NULL END; unique (citizen_id, waiting_session_id)

One vaccination record per appointment

Ordinary unique index on VaccinationRecord.appointment_id

One vaccination deduction per permanent record

Ordinary unique index on nullable StockTransaction.vaccination_record_id; only VACCINATION_DEDUCTION populates it

One certificate version per record

Ordinary unique index on (vaccination_record_id, version_number)

Cross-table rules cannot be guaranteed by a generated index alone. CitizenCampaignBookingGuard contains one row per citizen/campaign and is locked with SELECT ... FOR UPDATE whenever an appointment is created, cancelled, rescheduled, promoted from a waitlist or when a waiting entry is created. The same transaction rechecks active appointments and waiting entries before writing. This serializes competing operations without restricting a citizen to only one waiting session unless a separate rule explicitly does so.

Effective-period overlap for questionnaire assignments, dose-programme versions and campaign-center assignments is enforced by locking the campaign or assignment guard row, querying for existing_start < new_end AND new_start < existing_end, and rejecting any overlap before insert/update. Frontend checks are supplementary only. Generated columns are migration-level persistence details and are not writable API fields.

12.5 Transactional Outbox and Event Delivery

Any database transaction that requires a later module action or notification must insert a TransactionalOutboxEvent in the same transaction as the authoritative state change. The transaction must not call an email provider or other network service directly.

Each outbox event has a unique deduplication_key, source aggregate type/id, event type, schema version, minimized payload, status, attempt count, next-attempt time and timestamps.

A dispatcher reads committed PENDING events, delivers them after commit, and marks them DELIVERED. Failures remain retryable with bounded exponential backoff; exhausted events become DEAD_LETTER and require an authorized operational review.

Consumers use ModuleEventConsumption with a unique consumer/source-event key so retries produce exactly-once business effects.

Notification delivery creates or updates Notification records idempotently. A notification is not treated as sent until its channel status is SENT.

Vaccination finalization creates the permanent record, stock deduction, appointment completion, certificate issue and required outbox events atomically. A notification failure must never roll back an already committed vaccination, and a successful vaccination must never lose its notification request.

Outbox payloads contain identifiers and minimum display data only; they must not contain passwords, raw tokens, complete screening answers or unrestricted clinical notes.

13. Business Rules and Validation Catalogue

ID

Cross-module rule

BR-001

Citizen account exists once; screening and eligibility are separate per campaign

BR-002

MF-01 owns eligibility rules; MF-02 only links a published questionnaire version

BR-003

Rule evaluation is preliminary; final eligibility is a human-authorized decision

BR-004

Current eligibility is required both at booking and finalization

BR-005

One active appointment per citizen/campaign

BR-006

Capacity cannot become negative and is allocated atomically

BR-007

Waitlist promotion uses deterministic FIFO and revalidation

BR-008

Check-in occurs only for a valid confirmed appointment at the correct center/time, except audited override

BR-009

All inventory quantity changes create append-only stock transactions

BR-010

Unavailable/expired/recalled/quarantined inventory cannot be administered

BR-011

Draft CRUD creates no operational side effects

BR-012

Vaccination finalization is atomic and idempotent

BR-013

Finalized vaccination records are immutable

BR-014

Initial certificate issue is automatic during finalization

BR-015

Certificate revocation/reissue preserves all previous issue history

BR-016

Public verification returns only minimum approved information

BR-017

Hard deletion is allowed only for unused MF-01 configuration and unfinalized MF-05 drafts

BR-018

Sensitive actions require authorization and audit

BR-019

Cancellation/rescheduling cutoff is 24 hours before the slot unless staff override is recorded

BR-020

Standard check-in window is 30 minutes before to 30 minutes after the slot start

BR-021

Low-stock threshold is 10 doses; near-expiry threshold is 30 days

BR-022

In-app and email notifications are required; SMS is optional future scope

BR-023

MF-02 administrative location targeting controls service visibility/access and must not be represented as a medical diagnosis rule

BR-024

Recurring-session generation is idempotent; one occurrence edit does not mutate the series

BR-025

Sessions cannot overlap center closures, staff assignments or physical-resource assignments

BR-026

Approved session capacity is bounded by center safety limits and validated operational resources

BR-027

MF-02 schedule changes create impact events; MF-03 preserves and processes affected appointment/waitlist history

BR-028

A campaign may retain multiple questionnaire and dose-programme versions, but effective periods cannot overlap

BR-029

Every session requires an active campaign-center assignment for its full duration

BR-030

Session lifecycle and booking availability are independent state dimensions

BR-031

Active waitlist uniqueness and same-campaign appointment incompatibility are database-enforced

BR-032

Check-in reversal is prohibited after finalization and returns a valid appointment to CONFIRMED

BR-033

Applied vaccination corrections are read as field-level overlays and trigger certificate reissue when certificate-visible data changes

BR-034

Retryable write APIs persist actor-scoped idempotency records and reject key reuse with a changed payload

BR-035

Operational analytics use aggregate data and must reconcile with authoritative module records

BR-036

Rescreening is blocked during current eligibility, allowed after expiry, and requires Healthcare Officer authorization after ineligibility or revocation

BR-037

Eligibility and batch expiry are enforced from authoritative dates even before an optional background status update runs

BR-038

Async notifications and cross-module actions are requested through a transactional outbox written with the source transaction

BR-039

Verification, password-reset, refresh and public-verification tokens are stored only as secure hashes; refresh rotation detects reuse and revokes the affected family

BR-040

Every inventory transfer uses one correlation record and one atomic outbound/inbound transaction pair

BR-041

Effective staffed station count is limited by staff, physical stations and required equipment; unstaffed resources do not increase throughput

BR-042

Concurrent occupancy uses overlapping slot/dwell intervals and accompanying-person units, separate from total throughput

BR-043

Each screening submission stores the exact immutable consent/privacy notice version accepted by the citizen

BR-044

MF-02 owns the shared VaccineType master lifecycle; MF-04 references it without duplicating master ownership

BR-045

MySQL conditional uniqueness uses generated active-key columns, ordinary unique indexes and guard-row locking for cross-table booking checks

14. Product Backlog and Acceptance Criteria

Priority uses MoSCoW: MUST, SHOULD, COULD.

Story

Priority

Acceptance summary

PB-01 Citizen registration/email verification

MUST

Unique identity/email, verified account before protected citizen actions

PB-02 Login/logout/password reset

MUST

Secure authentication, expiring reset token, invalid session rejection

PB-02A Token-session security

MUST

Hashed verification/reset/refresh tokens, rotation, reuse detection and session revocation

PB-03 Staff account and RBAC

MUST

Admin-created staff, role-protected endpoints and object access

PB-04 MF-01 configuration CRUD

MUST

Safe create/read/update/delete of unused config with reference protection

PB-05 Questionnaire publish/versioning

MUST

Immutable published version and exact historical reference

PB-06 Citizen campaign screening

MUST

Consent and required answers; one active submission per campaign

PB-07 Healthcare eligibility review

MUST

Final status, reason, reviewer and validity recorded

PB-08 Campaign lifecycle, targeting and dose programme

MUST

Controlled lifecycle, booking windows, administrative service areas and versioned programme constraints

PB-09 Center operating calendar and closures

MUST

Valid center records, hard limits, hours, closures and schedule boundaries

PB-10 Recurring sessions and occurrence control

MUST

Idempotent generation, one-occurrence edit and safe future-series changes

PB-10A Staff and physical-resource assignment

MUST

No user, room, station or equipment overlap

PB-10B Calculated and approved capacity

MUST

Reproducible recommendation, hard-limit enforcement and audited override

PB-10C Session reschedule/cancellation impact

MUST

Preview, reason, change event and MF-03 affected-record workflow

PB-10D MF-02 operational analytics

SHOULD

Reconciled capacity, utilization, cancellation and resource-load aggregates

PB-11 Search and book appointment

MUST

Only current eligible citizen; no duplicate/overbooking

PB-12 Reschedule/cancel

MUST

Atomic reschedule, 24-hour citizen cutoff, staff override audit

PB-13 Waitlist and promotion

SHOULD

FIFO promotion with eligibility recheck and notification

PB-14 Check-in/no-show

MUST

Correct center/window and status transition

PB-15 Batch registration

MUST

Unique valid lot data and controlled status

PB-16 Stock receipt/transfer/adjustment

MUST

Append-only transactions and no negative quantity

PB-17 Stock alerts

SHOULD

10-dose low-stock and 30-day near-expiry flags

PB-18 Vaccination draft CRUD

MUST

Full CRUD before finalization with no side effects

PB-19 Atomic vaccination finalization

MUST

Record + deduction + appointment + certificate + draft removal all-or-nothing

PB-20 Vaccination history

MUST

Authorized immutable history with correction trail

PB-21 Certificate view/download

MUST

Version 1 available after finalization

PB-22 Certificate revoke/reissue

SHOULD

Reasoned audit, new version, old issue preserved

PB-23 Public certificate verification

MUST

Valid/revoked/invalid and minimum data only

PB-24 Notifications

SHOULD

In-app/email for defined lifecycle events

PB-24A Transactional outbox

MUST

Atomic event request, idempotent delivery, retry and dead-letter evidence

PB-25 Cross-module dashboards/reports

SHOULD

Role-scoped aggregates and CSV export beyond MF-02 operational dashboard

PB-26 Accessibility and responsive UI

MUST

Keyboard, labels, responsive layouts and clear errors

PB-27 Audit log

MUST

Critical actions captured and read-restricted

PB-28 Predictive analytics/SMS/multilingual

COULD

Implement only after all MUST stories pass

14.1 Lab 02 Assessment Product Backlog Alignment

The canonical PB-xx backlog above remains the implementation-oriented Product Backlog. The current Lab 02 report also maintains an assessment-facing persona backlog. Lab story IDs are traceability labels only: they do not replace or narrow canonical functional requirements, use cases, tests, business rules, or module ownership.

Current Lab 02 reporting baseline:

six personas: Citizen, Healthcare Officer, Campaign Officer, Center Manager, Inventory Officer and Vaccinator;

30 user stories (US-01 through US-30), five per persona;

priorities expressed as High / Medium / Low in the Lab report;

exactly four assessment sprint allocations;

each US-xx item appears exactly once in the Sprint Plan and has an owner.

Lab story

Persona

Exact current Lab 02 user story

Priority

Primary technical area

Sprint

Owner(s)

US-01

Citizen

As a Citizen, I want to register and verify my account using my email, so that I can securely access the vaccination portal.

High

Shared Access

Sprint 1

Member 1 / Member 3

US-02

Citizen

As a Citizen, I want to complete the campaign-specific screening questionnaire, so that my eligibility for vaccination can be assessed.

High

MF-01

Sprint 2

Member 1

US-03

Citizen

As a Citizen, I want to search for and book an available appointment slot at a nearby vaccination center, so that I can get vaccinated at a convenient time.

High

MF-03

Sprint 3

Member 3

US-04

Citizen

As a Citizen, I want to view and download my digital vaccination certificate, so that I can prove my vaccination status when required.

Medium

MF-05

Sprint 4

Member 5

US-05

Healthcare Officer

As a Healthcare Officer, I want to review a citizen's screening submission, so that I can make an informed eligibility decision.

High

MF-01

Sprint 2

Member 1

US-06

Healthcare Officer

As a Healthcare Officer, I want to record a final eligibility status with a reason and validity period, so that a citizen's booking eligibility is accurately controlled.

High

MF-01

Sprint 2

Member 1

US-07

Healthcare Officer

As a Healthcare Officer, I want to request corrections on an incomplete or unclear screening submission, so that I have sufficient information before deciding.

Medium

MF-01

Sprint 2

Member 1

US-08

Healthcare Officer

As a Healthcare Officer, I want to revoke a previously issued eligibility assessment with a reason, so that inaccurate assessments do not remain valid.

Low

MF-01

Sprint 4

Member 1

US-09

Campaign Officer

As a Campaign Officer, I want to create and manage a vaccination campaign's lifecycle and target group, so that citizens can be screened and booked for the right campaign.

High

MF-02

Sprint 1

Member 2

US-10

Campaign Officer

As a Campaign Officer, I want to link a campaign to a single published questionnaire version, so that screening stays consistent with campaign requirements.

High

MF-02

Sprint 1

Member 2

US-11

Campaign Officer

As a Campaign Officer, I want to define a campaign's dose programme, so that the correct dose numbers and intervals are followed.

High

MF-02

Sprint 1

Member 2

US-12

Campaign Officer

As a Campaign Officer, I want to assign vaccination centers to a campaign, so that services are offered only at approved locations.

High

MF-02

Sprint 1

Member 2

US-13

Center Manager

As a Center Manager, I want to configure a center's operating hours, closures and recurring sessions, so that available booking capacity is accurate.

High

MF-02

Sprint 1

Member 2

US-14

Center Manager

As a Center Manager, I want to assign staff and resources to sessions, so that sessions can run without scheduling conflicts.

High

MF-02

Sprint 2

Member 2

US-15

Center Manager

As a Center Manager, I want to approve session capacity based on available staff and resources, so that the center is not overbooked.

High

MF-02

Sprint 3

Member 2

US-16

Center Manager

As a Center Manager, I want to preview the impact of rescheduling or cancelling a session, so that affected appointments are handled without silently breaking citizen bookings.

Medium

MF-02

Sprint 4

Member 2

US-17

Inventory Officer

As an Inventory Officer, I want to register vaccine batches with manufacturer, lot number and expiry date, so that stock can be tracked accurately.

High

MF-04

Sprint 2

Member 4

US-18

Inventory Officer

As an Inventory Officer, I want to allocate and transfer stock between centers, so that vaccine supply matches demand across locations.

High

MF-04

Sprint 3

Member 4

US-19

Inventory Officer

As an Inventory Officer, I want to record stock receipts, transfers and adjustments as append-only transactions, so that inventory history remains auditable.

Medium

MF-04

Sprint 3

Member 4

US-20

Inventory Officer

As an Inventory Officer, I want to receive low-stock and near-expiry alerts, so that I can act before shortages or wastage occur.

Low

MF-04

Sprint 4

Member 4

US-21

Vaccinator

As a Vaccinator, I want to view a list of checked-in and eligible appointments, so that I know who is ready for vaccination.

Medium

MF-03 / MF-05

Sprint 3

Member 3 / Member 5

US-22

Vaccinator

As a Vaccinator, I want to create and edit an unfinalized vaccination draft, so that I can record dose details before finalizing.

High

MF-05

Sprint 3

Member 5

US-23

Vaccinator

As a Vaccinator, I want to confirm physical administration before finalizing a draft, so that only actually-administered doses are recorded.

High

MF-05

Sprint 4

Member 5

US-24

Vaccinator

As a Vaccinator, I want the system to atomically finalize the vaccination, so that the record, stock deduction, appointment completion and certificate are all created consistently.

High

MF-05 (cross-module finalization)

Sprint 4

Member 5

US-25

Citizen

As a Citizen, I want to reschedule or cancel my appointment when my plans change, so that the released slot can be used by another citizen.

Medium

MF-03

Sprint 3

Member 3

US-26

Healthcare Officer

As a Healthcare Officer, I want to authorize rescreening when an ineligible or revoked assessment needs to be reviewed again, so that a citizen can submit a new screening when appropriate.

Medium

MF-01

Sprint 4

Member 1

US-27

Campaign Officer

As a Campaign Officer, I want to define the campaign's service areas and booking period, so that citizens can see and book the campaign only in the intended locations and time window.

High

MF-02

Sprint 1

Member 2

US-28

Center Manager

As a Center Manager, I want to view session capacity and utilization information, so that I can identify underused or overloaded sessions and plan center resources better.

Medium

MF-02

Sprint 4

Member 2

US-29

Inventory Officer

As an Inventory Officer, I want to quarantine or recall a vaccine batch when it should no longer be used, so that unsafe or unavailable stock cannot be selected for vaccination.

Medium

MF-04

Sprint 2

Member 4

US-30

Vaccinator

As a Vaccinator, I want to delete an incorrect unfinalized vaccination draft before administration is confirmed, so that mistakes can be corrected without changing permanent vaccination records.

Medium

MF-05

Sprint 3

Member 5

Traceability rules:

US-01 is shared account/access work; it is not MF-01 business ownership.

US-21 crosses MF-03 and MF-05: MF-03 owns the checked-in appointment state; MF-05 consumes that state for the Vaccinator work queue.

US-24 is led by MF-05 but finalization crosses MF-03 appointment completion and MF-04 stock deduction inside the canonical atomic transaction.

Lab stories intentionally summarize some technical requirements. Canonical behaviors such as questionnaire versioning, waitlist promotion, check-in reversal, session recurrence idempotency, stock-transfer correlation, certificate revoke/reissue, transactional outbox, security and concurrency remain required even when they do not have a dedicated Lab US-xx row.

If the Lab report changes, this table and all function handoffs must be revalidated against the new US-xx allocation before progress is reported.

15. Use Case Catalogue

ID

Use case

Primary actor

Outcome

UC-01

Register Citizen Account

Citizen

Verified active account and profile

UC-02

Authenticate or Reset Password

User

Secure session or recovered access

UC-03

Manage Staff Roles

Admin

Authorized staff account and role set

UC-04

Manage Screening Configuration

Admin

Valid reusable question/option/rule records

UC-05

Publish Questionnaire Version

Admin

Immutable published version

UC-06

Submit Campaign Screening

Citizen

Pending-review submission

UC-07

Review Eligibility

Healthcare Officer

Final campaign-specific assessment

UC-08

Manage Campaign, Targeting and Dose Programme

Campaign Officer

Valid campaign lifecycle and service configuration

UC-09

Link Published Questionnaire

Campaign Officer

Campaign uses an MF-01 published version

UC-10

Manage Center Calendar

Center Manager

Valid hours, hard limit and closures

UC-10A

Generate Recurring Sessions

Center Manager

Idempotent individual session occurrences

UC-10B

Assign Staff/Resources and Approve Capacity

Center Manager

Conflict-free assignments and defensible capacity

UC-10C

Reschedule, Suspend or Cancel Session

Center Manager

Impact preview, change event and downstream MF-03 processing

UC-10D

View MF-02 Operational Analytics

Campaign Officer/Center Manager

Reconciled utilization and resource-load aggregates

UC-11

Book Appointment

Citizen

Confirmed appointment or validation error

UC-12

Reschedule or Cancel

Citizen/Staff

Updated capacity and history

UC-13

Join/Promote Waitlist

Citizen/System

Waiting or promoted entry

UC-14

Check In Citizen

Center Staff

Appointment becomes checked-in

UC-15

Manage Batch and Stock

Inventory Officer

Valid batch/inventory and transaction history

UC-16

Create/Read/Update/Delete Draft

Vaccinator

Unfinalized draft managed without side effects

UC-17

Finalize Vaccination

Vaccinator

Atomic permanent record, deduction, completion and certificate issue

UC-18

View Vaccination History

Citizen/Authorized Staff

Immutable history and correction trail

UC-19

View or Download Certificate

Citizen/Authorized Staff

Current certificate issue accessed

UC-20

Revoke or Reissue Certificate

Authorized Staff

Audited certificate lifecycle transition

UC-21

Verify Certificate

Public Verifier

Minimal valid/revoked/invalid response

UC-22

View Dashboard/Export Report

Authorized User

Role-scoped aggregate information

15.1 Detailed Use Case: UC-17 Finalize Vaccination

Field

Definition

Preconditions

Authenticated Vaccinator; valid checked-in appointment; valid current eligibility; unfinalized draft; selectable inventory

Trigger

Vaccinator confirms physical administration and selects Finalize

Main flow

Revalidate dependencies, execute atomic finalization contract, return permanent record and certificate summary

Alternate flows

Invalid eligibility, appointment, dose sequence, batch, expiry, recall or quantity returns a specific validation error and changes no data

Postconditions

One immutable record, one deduction, appointment completed, certificate issue v1 valid, draft removed, audit/notification queued

Idempotency

Retry returns the existing successful result or deterministic conflict; no duplicates

16. User Interface and Screen Inventory

Role/area

Required screens

Public

Login, register, forgot/reset password, certificate verification

Citizen

Dashboard, profile, campaign list, screening form, eligibility history, availability search, appointment detail, waitlist, vaccination history, certificate list/detail

Admin/MF-01

User/role management, question list/form, options, rule form, questionnaire builder, version publish/detail, audit search

Healthcare

Pending review queue, submission detail, triggered rules, decision/correction form, assessment history

Campaign/MF-02

Campaign list/form/detail, lifecycle/booking windows, questionnaire selector, location targeting, dose-programme version, center assignment, campaign transitions and progress dashboard

Center/MF-02

Center form, operating hours, closure calendar, hard capacity, recurrence builder, generated-session calendar, slot editor, staff/resource assignment, capacity calculation/override, schedule-change impact and utilization dashboard

Center/MF-03

Appointment search, affected-appointment/rebooking queue, waitlist handling and check-in queue

Inventory/MF-04

Manufacturer/batch form, center inventory, receipt, transfer, adjustment, transaction history, alert list

Vaccinator/MF-05

Checked-in queue, draft list/form/detail, validation summary, finalization confirmation, completed record view

Certificate staff

Certificate issue history, revoke, reissue

Management

Aggregate dashboards and CSV export

16.1 UI Rules

Show entity-specific status text and do not rely on color alone.

Keep destructive and irreversible actions behind explicit confirmation.

Session reschedule/cancellation confirmation must display affected appointment and waitlist counts before submission.

Recurrence editing must clearly distinguish “this occurrence” from “this and eligible future occurrences.”

Capacity override UI must show calculated inputs, recommendation, hard limit and required reason.

Draft delete requires a reason and clearly states that it is allowed only before administration finalization.

Finalization confirmation must say that the vaccine was physically administered and that the action creates permanent records and deducts stock.

Preserve valid entered values after validation errors.

Use server-provided validation messages mapped to fields where possible.

Public verification must not expose navigation or data for authenticated modules.

Responsive tables switch to cards or horizontal scrolling on narrow screens.

17. Security, Privacy and Ethical Considerations

17.1 Authentication and Authorization

Citizens self-register; staff accounts are administrator-created.

Email verification is required before citizen screening or booking.

Email-verification and password-reset tokens are random, single-use, stored only as hashes and expire; password-reset tokens expire after 30 minutes.

Access tokens are short-lived. Refresh tokens are stored in Secure HttpOnly SameSite cookies, stored server-side only as hashes, rotated on every use, grouped into token families and protected by reuse detection that revokes the affected family.

Successful password reset and account disablement revoke all active refresh-token sessions for the account.

Backend methods verify role and ownership; direct object reference attacks must be tested.

Account states are PENDING_VERIFICATION, ACTIVE, LOCKED, DISABLED.

17.2 Data Protection

Store only information needed by the project.

Do not log passwords, tokens, full screening answers or certificate QR contents.

Mask identity numbers in lists and verification results.

Use synthetic/anonymized demo data.

Restrict audit-log access.

Retention periods are USER_DATA_REQUIRED pending approved institutional/legal policy. Until defined, do not hard-delete protected operational history; use access restriction, archival status and auditable disposal procedures.

17.3 Ethical Boundary

The portal is a workflow and record-management system. Preliminary rules can highlight answers for review, but only authorized healthcare staff issue final eligibility decisions. Wording must not imply diagnosis, guaranteed safety or replacement of professional judgment.

17.4 Public Verification Security

Use high-entropy opaque tokens.

Store token hashes where practical.

Apply rate limiting and generic invalid responses.

Log result and time while minimizing network metadata.

QR codes contain no raw identity or medical data.

18. Testing Strategy

18.1 Test Levels

Level

Focus

Unit

Validators, rule evaluation, status transitions, recurrence expansion, capacity calculations, conflict detection, alert thresholds

Repository

Constraints, query scopes, status filters, optimistic locking

Service integration

Recurrence generation, session change impact, booking/reschedule, transfer, finalization and certificate reissue transactions

API

Authentication, authorization, validation, error format and idempotency

Frontend

Form behaviour, role routing, accessible labels, status display

End-to-end

Citizen-to-certificate vertical slice

Security

IDOR, role bypass, token misuse, rate limits, sensitive-data exposure

Concurrency

Last-slot booking, stock deduction and finalization retries

18.2 Starter Test Cases

ID

Scenario

Expected result

TC-01

Duplicate citizen identity/email registration

Rejected; no duplicate account

TC-02

Unverified citizen opens screening/booking

Access blocked with verification guidance

TC-03

Citizen calls admin endpoint

Forbidden

TC-04

Delete unused screening question

Row removed and audited

TC-05

Delete referenced screening question

Conflict; no data changed

TC-06

Publish version with invalid rule reference

Rejected

TC-07

Submit without consent/required answer

Rejected with field errors

TC-08

Final eligibility by wrong role

Forbidden

TC-09

Book with assessment for another campaign

Rejected

TC-10

Publish campaign without questionnaire/dose programme

Rejected

TC-10A

Campaign administrative location target applied

Correct service visibility without creating medical eligibility decision

TC-11

Session outside center hours or inside closure

Rejected

TC-11A

Generate recurring sessions twice with same key

No duplicate occurrences

TC-11B

Edit one recurrence occurrence

Other occurrences unchanged

TC-11C

Double-book staff/room/station/equipment

Rejected with conflict

TC-11D

Calculate recommended capacity

Matches stored duration/service/station inputs and hard limit

TC-11E

Override above recommendation without reason/role

Rejected

TC-11F

Reduce capacity below confirmed bookings

Rejected

TC-11G

Reschedule session with bookings

Impact preview and one change event; MF-03 preserves/processes appointments

TC-11H

MF-02 utilization report

Reconciles with MF-03 aggregate confirmed counts

TC-12

Two users book final place concurrently

Exactly one confirmed

TC-13

Reschedule new slot fails

Original appointment remains confirmed

TC-14

Cancel appointment

Capacity updates and first valid waitlist entry promoted

TC-15

Check in at wrong center

Rejected unless authorized override

TC-16

Register duplicate batch

Rejected

TC-17

Transfer above available quantity

Full rollback

TC-18

Near-expiry and low-stock same batch

Both calculated flags can be true

TC-19

Create/update/delete draft

No stock, appointment completion, history or certificate change

TC-20

Finalize with expired batch

Full rollback; draft remains

TC-21

Successful finalization

One record, one deduction, appointment completed, certificate v1, draft removed

TC-22

Retry successful finalization

No duplicate record/deduction/certificate

TC-23

Direct update/delete finalized record

Rejected

TC-24

Revoke certificate

Public verification returns revoked; issue retained

TC-25

Reissue certificate

New version/reference; previous issue retained/superseded

TC-26

Public verification response

Only approved minimum fields returned

TC-27

Password reset token reused/expired

Rejected

TC-28

Citizen requests another citizen's certificate

Forbidden

TC-29

Concurrent duplicate active submission

Exactly one active citizen-campaign submission; other transaction conflicts cleanly

TC-30

Concurrent appointment creation and waitlist promotion for same campaign

Guard-row serialization leaves at most one active appointment and preserves valid waitlist history

TC-31

Source transaction commits but email provider is unavailable

State change and outbox event commit; retry later sends once without duplicating business effects

TC-32

Refresh token reused after rotation

Token family revoked; request rejected and security event audited

TC-33

Citizen rescreens after valid/ineligible/expired/revoked assessment

Valid blocked; expired allowed; ineligible/revoked require recorded officer authorization

TC-34

Eligibility/batch date has expired but status job has not run

Booking/finalization or stock selection rejects immediately from authoritative date

TC-35

Transfer succeeds

One transfer correlation and exactly one outbound/inbound pair commit with matching quantity

TC-36

Capacity inputs have more stations than vaccinators/equipment

Effective station count uses the smallest complete resource count

TC-37

Overlapping dwell intervals exceed safe occupancy

Capacity plan rejected even when total throughput is within limit

TC-38

Screening submitted after notice update

Submission retains the exact accepted consent notice version and content hash

18.3 Definition of Done

A feature is done only when:

requirement and acceptance criteria are mapped;

API, UI, persistence and migrations are integrated;

role and object-level authorization are implemented;

positive, negative and concurrency tests pass where applicable;

no known critical defect remains;

code is reviewed and merged;

OpenAPI and project documentation are updated;

screenshots/commit/test evidence is retained;

the assigned member can explain the implementation.

19. Project Management and Scrum Plan

19.1 Scrum Roles

Role

Baseline assignment

Product Owner representative

Member 1 / MF-01 owner until the team records another name

Scrum Master and integration coordinator

Member 2 / MF-02 owner until the team records another name

Developers

All five members

Test owner for a story

A member other than the primary implementer where practical

Stakeholder reviewers

Lecturer and interviewed domain representatives; names are USER_DATA_REQUIRED

19.2 Working Practices

Maintain one prioritized product backlog with requirement, use-case and test IDs.

Keep the four-sprint high-level plan required for Lab 02, but refine the detailed Sprint Backlog, acceptance criteria and implementation tasks at the beginning of each sprint rather than pretending every task is fixed upfront.

Use feature branches and pull requests.

Require at least one peer review before merge.

Integrate continuously; do not leave five isolated modules until the end.

Keep API contracts, enum values and migrations in version control.

Demonstrate a vertical slice early.

Record decisions and blockers in lightweight meeting notes.

19.3 Repository Structure

repository-root/
├── frontend/                 React, Vite, Bootstrap
├── backend/                  Java 21 Spring Boot Maven project
│   └── src/main/java/.../
│       ├── shared/
│       ├── screening/        MF-01
│       ├── campaign/         MF-02
│       ├── appointment/      MF-03
│       ├── inventory/        MF-04
│       └── administration/   MF-05
├── database/                 Flyway migrations, controlled seed data and database evidence
├── docs/                     canonical files, handoffs, architecture, assessment and evidence
├── docker-compose.yml
├── README.md
└── .github/                  issue and pull-request templates

20. Team Allocation and Ownership

Member slot

Primary ownership

Secondary responsibility

Member 1

MF-01 Screening and Eligibility

Product backlog clarification and MF-01 integration tests

Member 2

MF-02 Campaign, Center and Session

Scrum coordination and cross-module schedule contracts

Member 3

MF-03 Appointment, Waitlist and Check-In

Frontend integration and concurrency tests

Member 4

MF-04 Inventory and Batch

Database migrations, seed data and transaction tests

Member 5

MF-05 Administration, Drafts and Certificates

End-to-end finalization, certificate and integration coordination

Names and student IDs remain USER_DATA_REQUIRED. All members contribute to shared authentication, documentation, testing and final integration.

21. Project Timeline

The canonical Scrum baseline uses four assessment-aligned sprints to match Lab 02. The current Lab report uses three-week working windows (Weeks 1-3, 4-6, 7-9 and 10-12). Those windows are team planning periods, not a duration mandated by the Lab sheet. Detailed Sprint Backlog tasks are refined at the start of each sprint.

Sprint

Team focus

Integrated demonstration

Sprint 1

Shared foundation, MF-01 screening configuration/versioning, and MF-02 campaign/center foundations

Published questionnaire can be linked to a valid campaign; basic center planning foundation is available

Sprint 2

Citizen screening/eligibility, MF-02 session/resource setup, MF-04 batch/inventory foundation, and MF-03 booking-contract preparation

Reviewed eligible citizen, validated session availability and valid stock foundation are available

Sprint 3

MF-03 booking/reschedule/waitlist/check-in, MF-04 stock movements, and MF-05 vaccination-draft workflow

Checked-in appointment, selectable stock and editable vaccination draft are available

Sprint 4

MF-02 schedule-change handling, MF-05 atomic finalization/certificates, alerts/dashboards, security, regression and evidence

Release candidate demonstrates the end-to-end citizen-to-certificate flow

Current Lab 02 story allocation:

Sprint 1: US-01, US-09, US-10, US-11, US-12, US-13, US-27

Sprint 2: US-02, US-05, US-06, US-07, US-14, US-17, US-29

Sprint 3: US-03, US-15, US-18, US-19, US-21, US-22, US-25, US-30

Sprint 4: US-04, US-08, US-16, US-20, US-23, US-24, US-26, US-28

The story allocation above is assessment-facing. Canonical technical work that has no dedicated Lab story is still planned inside the relevant sprint where its dependency becomes necessary.

22. Risk Register

ID

Risk

Probability

Impact

Mitigation

R-01

Five-function scope differs from assignment wording

Medium

High

Retain written approval/evidence; explain mapping clearly

R-02

Modules developed in isolation

Medium

High

Shared contracts, vertical slice, continuous integration

R-03

Overbooking race condition

Medium

High

Transaction/locking plus concurrency tests

R-04

Stock and dose history diverge

Medium

High

Atomic finalization and unique deduction link

R-05

Unsafe deletion corrupts history

Medium

High

Explicit deletion matrix and FK conflict tests

R-06

Rule engine appears to diagnose

Medium

High

Preliminary terminology and mandatory human final decision

R-07

Certificate exposes sensitive data

Medium

High

Opaque token, minimum response, security tests

R-08

Optional features delay core flow

High

Medium

MoSCoW priorities; defer COULD items

R-09

Team contribution evidence incomplete

Medium

High

Commit/test/screenshot evidence checklist per sprint

R-10

Demo environment failure

Medium

High

Docker Compose, seed data, backup demo machine/export

R-11

Recurring session generation creates duplicates

Medium

High

Unique occurrence keys, idempotent generation and retry tests

R-12

Staff/resources are double-booked or capacity is unrealistic

Medium

High

Conflict constraints, calculated capacity, hard limits and override audit

R-13

Session changes corrupt appointment history

Medium

High

Impact preview, append-only change event and MF-03 transactional handling

23. Constraints, Assumptions and Dependencies

Type

Baseline

Academic

Java-based web application, evidence of individual contribution, Scrum process and documented testing

Team

Five ownership slots; names/IDs not supplied

Technical

React, Java 21, Spring Boot, MySQL, Maven, Git/GitHub

Scope

Five major functions plus shared support; no payment, automatic diagnosis or MF-02 ownership of appointments/batch quantities

Data

Synthetic/anonymized demonstration data

External

Email sandbox may be used; SMS not required

Assessment

Written approval for five-function mapping not supplied

Implementation

No completion claim without repository, screenshots or test evidence

24. Deployment and Environment Plan

24.1 Environments

Environment

Purpose

Baseline setup

Local

Individual development and unit tests

Frontend dev server, backend, local/container MySQL

Shared integration

Pull-request integration and API testing

Docker Compose with controlled shared seed data

Evaluation/demo

Stable release candidate

Docker Compose on prepared machine/server with environment secrets and backup seed/reset procedure

24.2 Configuration

Use .env.example without real secrets.

Use Spring profiles: local, test, demo.

Use Flyway for schema creation and migration.

Use deterministic synthetic seed data.

Configure CORS for the actual frontend origin only.

Set certificate verification base URL by environment.

Store generated certificate assets outside the source tree or regenerate them from certificate metadata.

Log application errors without sensitive request bodies.

24.3 API Conventions

Base path: /api/v1.

JSON uses stable IDs and ISO-8601 timestamps.

Validation errors use a consistent problem format with code, message and field errors.

Pagination uses page, size, sort with server-side allowed-field validation.

State transitions use explicit action endpoints where clearer than generic updates, for example /appointments/{id}/cancel and /vaccination-drafts/{id}/finalize.

Finalization requires an Idempotency-Key header. The server stores an ApiIdempotencyRecord keyed by authenticated actor, operation and key, with request fingerprint, processing status, result reference, response code, timestamps and expiry.

25. Future Enhancements

The following are FUTURE_OPTION and must not displace core delivery:

SMS gateway integration;

multilingual UI;

national identity or health-system integration;

native mobile application;

predictive demand analytics, capacity forecasting and stock forecasting;

anonymous rule-change simulation;

cross-border certificate trust integration;

production monitoring, high availability and disaster recovery;

accessibility certification beyond baseline testing.

26. Resolved Decision Register

Decision ID

Decision

Rationale

DEC-001

Use five combined major functions

Matches the team's chosen five-member structure; approval evidence remains external

DEC-002

Use a layered modular monolith

Clear ownership with lower deployment complexity than microservices

DEC-003

Use React/Vite/Bootstrap, Java 21/Spring Boot, MySQL, Maven, Flyway and GitHub

Coherent academic web stack and source-aligned direction

DEC-004

Use JWT access plus HttpOnly refresh-token cookie

Supports SPA authentication while protecting the long-lived token

DEC-005

MF-01 owns detailed eligibility configuration

Prevents duplicated rule ownership and inconsistent data

DEC-006

MF-02 links only published questionnaire versions

Preserves module boundary and historical versioning

DEC-007

Eligibility is separate per citizen-campaign pair

Campaigns can have different criteria and validity

DEC-008

Final eligibility requires Healthcare Officer review

Prevents automated diagnosis/decision claims

DEC-009

Existing assessments survive new questionnaire publication until expiry/revocation

Preserves historical decisions and avoids silent invalidation

DEC-010

Citizen cancellation/reschedule cutoff is 24 hours

Clear default operational window with staff override

DEC-011

Check-in window is ±30 minutes

Clear default with audited override

DEC-012

Waitlist is deterministic FIFO and auto-promotes after revalidation

Fair, testable behaviour without an extra acceptance state

DEC-013

Low-stock threshold is 10 doses; near-expiry is 30 days

Simple, demonstrable baseline values

DEC-014

Batch state and alert flags are separate

A batch can be active, low stock and near expiry simultaneously

DEC-015

Draft CRUD has no side effects

Makes MF-05 CRUD safe and reversible before administration finalization

DEC-016

Vaccination finalization is atomic and idempotent

Prevents partial records and duplicate deductions

DEC-017

Initial certificate is issued automatically during finalization

Removes inconsistent separate generation behaviour

DEC-018

Reissue creates a new certificate version

Preserves auditability and previous issues

DEC-019

Hard deletion is strictly limited

Protects eligibility, appointment, stock, vaccination and certificate history

DEC-020

In-app and email are baseline; SMS is future scope

Keeps communication useful and implementation feasible

DEC-021

Public verification uses opaque tokens and minimum data

Protects privacy while supporting trust

DEC-022

Four assessment-aligned sprints with one-sprint-at-a-time detailed refinement

Matches Lab 02 reporting while keeping detailed Sprint Backlog tasks adaptive

DEC-023

MF-02 includes administrative location targeting and versioned dose-programme configuration

Strengthens campaign planning without transferring medical-rule ownership from MF-01

DEC-024

Recurring rules generate individually stored sessions with idempotent occurrence keys

Supports repeat scheduling, single-occurrence edits and auditability

DEC-025

Center closures and physical resources are first-class planning records

Prevents invalid scheduling and makes MF-02 more than basic CRUD

DEC-026

Recommended total throughput is calculated from duration, service time and stations; concurrent occupancy is validated independently

Avoids incorrectly treating simultaneous occupancy as total session capacity

DEC-027

Capacity above recommended throughput requires authorized reason, resource sufficiency and compliance with any session service ceiling plus concurrent-occupancy limit

Balances operational flexibility with safety and auditability

DEC-028

MF-02 emits schedule-change impact events; MF-03 owns affected appointment/waitlist mutation

Preserves module boundaries and booking history

DEC-029

MF-02 operational analytics are aggregate and reconciled to authoritative module counts

Supports planning without duplicating transactional ownership or exposing sensitive data

DEC-030

Campaign questionnaire assignments and dose programmes are effective-dated one-to-many histories with one active version at a time

Preserves historical references while preventing ambiguous current configuration

DEC-031

Campaign-center operational assignment is modeled separately from location targeting

Distinguishes service-area visibility from authorization to host sessions

DEC-032

Session lifecycle and booking availability use separate status models

Prevents contradictory or stale combined states

DEC-033

MF-03 event consumption and API retry handling use persistent idempotency records

Makes exactly-once effects testable under retries and failures

DEC-034

Vaccination corrections use immutable field-level overlays and certificate reissue when visible data changes

Preserves legal/audit history while exposing an unambiguous effective record

DEC-035

Retention periods remain USER_DATA_REQUIRED until approved policy values are supplied

Prevents AI systems from inventing legal or institutional retention periods

DEC-036

Markdown is the normative semantic source and DOCX is its synchronized human-readable derivative

Eliminates dual-source ambiguity while retaining a polished document

DEC-037

MySQL conditional uniqueness uses generated active-key columns and guard-row locking

Produces valid MySQL constraints and deterministic concurrency behavior

DEC-038

Async effects use a transactional outbox and idempotent consumers

Prevents lost events and prevents network failures from corrupting committed transactions

DEC-039

Authentication uses persistent hashed verification/reset/refresh records with rotation and reuse detection

Makes shared authentication implementable and testable instead of implicit

DEC-040

Rescreening is automatic after expiry but officer-authorized after ineligibility or revocation

Prevents uncontrolled repeated submissions while permitting legitimate reassessment

DEC-041

Eligibility and batch expiry are effective from dates independent of background status persistence

Removes timing gaps that could allow invalid booking or administration

DEC-042

MF-02 owns VaccineType; MF-04 reads the shared master

Prevents duplicate ownership of vaccine master data

DEC-043

Stock transfers use an explicit correlation record with one atomic outbound/inbound pair

Makes transfer reconciliation and rollback unambiguous

DEC-044

Effective station count is the smallest complete staff/station/equipment bundle count

Prevents overstating capacity from unusable resources

DEC-045

Concurrent occupancy is calculated from slot/dwell overlap and accompanying-person units

Makes the safety constraint reproducible and testable

DEC-046

Screening submissions retain an immutable consent/privacy notice version

Provides auditable evidence of exactly what the citizen accepted

27. Conclusion

The project baseline resolves the internal architectural and workflow questions documented in this package. It defines a coherent five-module vaccination workflow, explicit ownership boundaries, separate lifecycle and availability statuses, safe deletion rules, recurring-session and resource-conflict controls, separate throughput and concurrent-occupancy constraints, exactly-once event effects through a transactional outbox, MySQL-valid conditional uniqueness, hashed authentication-session records, center-level inventory with correlated transfers, explicit rescreening and expiry behavior, reproducible staffed capacity/occupancy, immutable consent evidence, atomic administration finalization, immutable correction overlays and a versioned certificate lifecycle.

Remaining unknowns are explicitly external or policy-dependent: team identities, URLs, dates, approval proof, actual implementation artifacts, and approved retention periods. These are marked USER_DATA_REQUIRED; AI systems must not invent them or claim implementation completion.

28. References

Use IEEE-style references in submitted reports. The following source categories are approved for the project bibliography when actually consulted:

SE2030 Software Engineering Group Assignment Specification, 2026.

Web-Based Vaccination Portal Five Major Functions and Supporting Minor Functions, project scope document, 2026.

Web-Based Vaccination Portal Project Proposal Report Tree, 2026.

Official React and Vite documentation.

Official Java 21 and Spring Boot reference documentation.

Official Spring Security, Spring Data JPA and Bean Validation documentation.

Official MySQL, Maven and Flyway documentation.

Official Bootstrap documentation.

OWASP guidance for web application security, authentication and access control.

Do not fabricate access dates, authors or URLs. Insert the exact references used by the team when preparing the final report.

Appendix A - Stakeholder Validation Guide

The baseline decisions are usable without an interview, but stakeholder interviews can validate domain wording and local operating policy.

Stakeholder

Validation topics

Healthcare or medical-services manager

Screening wording, final-decision authority, validity/revocation reasons

Vaccination-center officer

Operating hours/closures, recurrence patterns, resources, service time, capacity override, cancellation impact, check-in window and no-show process

Vaccine stock coordinator

Receipt/transfer workflow, adjustment reasons, threshold suitability

Data-protection/security officer

Role visibility, retention, public verification fields and audit access

Citizen-services representative

Form clarity, booking flow, notifications and certificate access

Record actual answers separately. When an approved stakeholder answer changes this baseline, update the document revision and resolved decision register.

Appendix B - Requirements Traceability Matrix

Requirements

Module

Use cases

Tests

FR-01 to FR-03

Shared access

UC-01 to UC-03

TC-01 to TC-03, TC-27 to TC-28

FR-04 to FR-06

MF-01

UC-04 to UC-07

TC-04 to TC-09

FR-07 to FR-09B

MF-02

UC-08 to UC-10D

TC-10 to TC-11H

FR-10 to FR-13

MF-03

UC-11 to UC-14

TC-12 to TC-15

FR-14 to FR-16

MF-04

UC-15

TC-16 to TC-18

FR-17 to FR-20

MF-05

UC-16 to UC-21

TC-19 to TC-26

FR-21

Shared services

UC-22 and notifications/audit within other use cases

Role, notification and audit tests across modules

Implementation evidence remains USER_DATA_REQUIRED until source code, screenshots, API output and test results are supplied.

Appendix C - Evidence Checklist

For each major function, retain:

owner name/student ID;

requirement and user-story IDs;

design diagram or data model excerpt;

UI screenshots;

API request/response evidence;

database or migration evidence;

positive and negative tests;

commit and pull-request links;

integration demonstration;

short individual reflection.

Shared evidence must include:

account/RBAC tests;

end-to-end citizen-to-certificate test;

recurring-session idempotency, staff/resource conflict and capacity-override tests;

session-change impact and affected-appointment preservation test;

last-slot booking concurrency test;

stock transfer/finalization rollback tests;

public verification privacy test;

sprint board and decision log;

setup guide and demo reset procedure.

Appendix D - Glossary and Canonical Terms

Term

Canonical meaning

Screening

Citizen answers and consent collected for one campaign

Preliminary result

Rule-engine output used to support review; not a medical diagnosis or final decision

Eligibility assessment

Healthcare Officer's final status for one citizen-campaign submission

Questionnaire version

Immutable published snapshot of questions, ordering and rule references

Campaign questionnaire assignment

MF-02 link from a campaign to an MF-01 published version

Administrative location target

MF-02 service-area/visibility constraint; not a clinical or medical eligibility decision

Campaign dose programme

Versioned operational rules for dose purpose/number, minimum interval, evidence requirement and expected service time

Center closure

Temporary date/time interval in which the center cannot host sessions

Session recurrence rule

Pattern that idempotently generates individually stored session occurrences

Operational resource

Room, vaccination station or equipment item allocated to a session

Session capacity plan

Stored calculation inputs, recommendation, approved capacity and override audit for one revision

Session change event

Append-only MF-02 record of a material schedule change and affected aggregate counts

Session

Scheduled vaccination event at a center

Time slot

Bookable capacity window inside a session

Appointment change event

Append-only MF-03 history for booking, reschedule, session-impact or cancellation changes

Waitlist entry

Ordered request for capacity; not a confirmed appointment

Center inventory

Quantity of one batch at one center

Vaccination draft

Editable/deletable pre-finalization administration record with no operational side effects

Vaccination record

Immutable permanent record created only by successful finalization

Certificate issue

One version of a certificate linked to a vaccination record

Reissue

Creation of a new certificate issue version while retaining prior issues

Revocation

Terminal invalidation of one certificate issue with a reason

Finalization

Atomic transaction that creates permanent history, deducts stock, completes appointment and issues certificate

Appendix E - Canonical State Transition Contracts

A transition not listed below is rejected. Every accepted transition records the actor, timestamp and reason when the table requires one. Derived states are recalculated from authoritative data and must not be changed through a generic status-update endpoint.

E.1 UserAccount

Current state

Action

New state

Authorized actor

Preconditions and side effects

PENDING_VERIFICATION

Verify email with valid single-use token

ACTIVE

Citizen/system

Token hash matches, token unused and unexpired; set email_verified_at; consume token

PENDING_VERIFICATION

Disable account

DISABLED

Administrator

Reason and audit required

ACTIVE

Security/manual lock

LOCKED

System or Administrator

Record lock reason and optional locked_until; revoke active refresh-token family when compromise is suspected

LOCKED

Unlock

ACTIVE

System after expiry or Administrator

Lock condition cleared; audit manual unlock

ACTIVE or LOCKED

Disable account

DISABLED

Administrator

Reason required; revoke all refresh-token sessions

DISABLED

Re-enable account

ACTIVE

Administrator

Identity and role checks pass; reason and audit required

E.2 QuestionnaireVersion

Current state

Action

New state

Authorized actor

Preconditions and side effects

DRAFT

Publish

PUBLISHED

Administrator

At least one active item; all rule/question references valid; set published_at; version becomes immutable

PUBLISHED

Retire

RETIRED

Administrator

No new effective assignment may use it; existing submissions/assessments retain the reference

DRAFT

Hard delete

Record removed

Administrator

Version is unused, unpublished and has no dependent record

E.3 ScreeningSubmission

Current state

Action

New state

Authorized actor

Preconditions and side effects

DRAFT

Submit

PENDING_REVIEW

Citizen

Campaign accepts screening; consent notice version recorded; required answers valid; store preliminary result and submitted_at

PENDING_REVIEW

Request correction

CORRECTIONS_REQUIRED

Healthcare Officer

Reason and requested fields recorded; citizen notified through outbox

CORRECTIONS_REQUIRED

Resubmit

PENDING_REVIEW

Citizen

Required corrections completed; preserve submission/version history

PENDING_REVIEW

Record final assessment

REVIEWED

Healthcare Officer

Creates exactly one final EligibilityAssessment; reviewer and reason recorded

DRAFT or CORRECTIONS_REQUIRED

Delete unreviewed draft

Record removed

Citizen or authorized staff

No final assessment or immutable review event exists

E.4 EligibilityAssessment

Current state

Action

New state

Authorized actor

Preconditions and side effects

No assessment

Approve

ELIGIBLE

Healthcare Officer

Submission is pending review; set reviewed_at, valid_until, reviewer and reason

No assessment

Decline

INELIGIBLE

Healthcare Officer

Submission is pending review; reason mandatory

ELIGIBLE

Revoke

REVOKED

Healthcare Officer

Reason and revoked_at required; future booking/finalization blocked

ELIGIBLE

Validity passes

EXPIRED effective state

System-derived; optional scheduled persistence

Current eligibility is false immediately after valid_until or campaign end even if the stored row has not yet been updated

INELIGIBLE or REVOKED

Authorize rescreening

State unchanged

Healthcare Officer

Set rescreening_authorized_at, actor and reason; permits a new submission while the campaign screening window is open

EXPIRED

Start new screening

State unchanged

Citizen

New submission allowed while the campaign screening window is open; historical assessment retained

E.5 VaccinationCampaign

Current state

Action

New state

Authorized actor

Preconditions and side effects

DRAFT

Approve future campaign

SCHEDULED

Campaign Officer/Administrator

Dates, vaccine type, target, dose programme, questionnaire assignment and centers valid

DRAFT or SCHEDULED

Publish

PUBLISHED

Campaign Officer/Administrator

Citizen-visible configuration complete; publication time reached or manual authorization recorded

PUBLISHED

Start service

ACTIVE

System or Campaign Officer

Service start reached and campaign not suspended/cancelled

PUBLISHED or ACTIVE

Suspend

SUSPENDED

Campaign Officer/Administrator

Reason required; blocks new screening/booking; records impact event

SUSPENDED

Resume before service start

PUBLISHED

Campaign Officer/Administrator

Blocking condition cleared; reason/audit recorded

SUSPENDED

Resume during service period

ACTIVE

Campaign Officer/Administrator

Blocking condition cleared; reason/audit recorded

PUBLISHED, ACTIVE or SUSPENDED

Complete

COMPLETED

Campaign Officer/Administrator

Service work closed; no new bookings; history retained

DRAFT, SCHEDULED, PUBLISHED, ACTIVE or SUSPENDED

Cancel

CANCELLED

Campaign Officer/Administrator

Reason and impact preview required once dependencies exist

COMPLETED or CANCELLED

Archive

ARCHIVED

Administrator

Retention policy permits archival; no hard deletion

E.6 VaccinationSession Lifecycle

Current state

Action

New state

Authorized actor

Preconditions and side effects

DRAFT

Schedule

SCHEDULED

Center Manager/Campaign Officer

Campaign-center assignment, operating hours, resources, capacity and conflicts validated

SCHEDULED

Start

IN_PROGRESS

Center Manager/authorized staff

Current time/override valid; session not cancelled

IN_PROGRESS

Complete

COMPLETED

Center Manager

Attendance and operational close checks completed; history retained

DRAFT or SCHEDULED

Cancel

CANCELLED

Center Manager/Campaign Officer

Reason and impact preview required; creates SessionChangeEvent; MF-03 handles affected bookings/waitlist

E.7 SessionBookingStatus

Current/derived state

Trigger/action

New/derived state

Authority

Rule

NOT_OPEN

Booking window opens and session is schedulable

OPEN or FULL

System-derived

Depends on remaining confirmed capacity

OPEN

Remaining capacity becomes zero

FULL

System-derived

Waitlist does not consume confirmed capacity

FULL

Capacity becomes available before booking close

OPEN

System-derived

Recomputed after cancellation/reschedule/capacity change

NOT_OPEN, OPEN or FULL

Authorized suspension

SUSPENDED

Center Manager/Campaign Officer

Reason required; explicit override stored separately from derived state

SUSPENDED

Resume

Recomputed NOT_OPEN, OPEN, FULL or CLOSED

Center Manager/Campaign Officer + system

Derived from current windows, lifecycle and capacity

NOT_OPEN, OPEN or FULL

Booking deadline passes or lifecycle blocks booking

CLOSED

System-derived

Cannot be manually reopened without changing authoritative window/lifecycle data

E.8 Appointment

Current state

Action

New state

Authorized actor

Preconditions and side effects

No appointment

Book or promote

CONFIRMED

Citizen/staff or system promotion

Same-campaign eligibility valid; capacity allocated atomically; guard row locked; history and notification created

CONFIRMED

Reschedule

CONFIRMED

Citizen/staff

Cutoff/override valid; target capacity allocated before source released; append change event

CONFIRMED

Check in

CHECKED_IN

Authorized center staff

Correct center/window or audited override; append check-in event

CHECKED_IN

Reverse check-in

CONFIRMED

Authorized center staff

Reason required; no permanent vaccination record exists

CHECKED_IN

Finalize vaccination

COMPLETED

Vaccinator through MF-05

Atomic finalization succeeds

CONFIRMED

Cancel

CANCELLED

Citizen/staff

Cutoff/override valid; release capacity; append history; evaluate waitlist promotion

CONFIRMED

Mark no-show

NO_SHOW

Authorized staff/system

Check-in window ended; reason/audit; release operational capacity as applicable

E.9 WaitlistEntry

Current state

Action

New state

Authorized actor

Preconditions and side effects

No entry

Join

WAITING

Citizen/staff

Session valid; no duplicate waiting entry; booking guard transaction checks active same-campaign appointment

WAITING

Promote

PROMOTED

System/authorized staff

FIFO candidate, eligibility/session/capacity revalidated; appointment created atomically

WAITING

Eligibility invalid at promotion

SKIPPED_INELIGIBLE

System

Reason snapshot retained; next candidate evaluated

WAITING

Active same-campaign appointment exists

SKIPPED_DUPLICATE_APPOINTMENT

System

Guard transaction confirms conflict; next candidate evaluated

WAITING

Cancel entry

CANCELLED

Citizen/staff

Reason and history recorded

WAITING

Session cancelled

SESSION_CANCELLED

System consuming session event

Source event reference retained; notification requested

E.10 VaccineBatch

Current state

Action

New state

Authorized actor

Preconditions and side effects

ACTIVE

Quarantine

QUARANTINED

Inventory Officer

Reason required; immediately unavailable for administration

QUARANTINED

Clear quarantine

ACTIVE

Inventory Officer

Authorized evidence/reason recorded; expiry and recall checks still pass

ACTIVE or QUARANTINED

Recall

RECALLED

Inventory Officer/Administrator

Reason required; terminal for selection

ACTIVE or QUARANTINED

Expiry date passes

EXPIRED effective state

System-derived; optional scheduled persistence

Unavailable immediately based on date even before status persistence

ACTIVE or QUARANTINED

Retire zero-stock batch

INACTIVE

Inventory Officer

Quantity is zero and no pending transfer; reason recorded

E.11 VaccinationDraft

Current state

Action

New state

Authorized actor

Preconditions and side effects

No draft

Create

DRAFT

Vaccinator

Appointment exists; one draft per appointment; no operational side effect

DRAFT

Validate readiness

READY_FOR_FINALIZATION

Vaccinator

Required fields and selectable inventory pass validation

READY_FOR_FINALIZATION

Edit readiness-critical field

DRAFT

Vaccinator

Revalidation required; no operational side effect

DRAFT or READY_FOR_FINALIZATION

Delete

Record removed

Vaccinator

No permanent record exists; no stock/appointment/certificate side effect

READY_FOR_FINALIZATION

Finalize

Record removed after commit

Vaccinator

Atomic finalization creates immutable records and outbox events; rollback preserves draft

E.12 VaccinationCorrection

Current state

Action

New state

Authorized actor

Preconditions and side effects

No correction

Request

PENDING

Authorized staff

Allow-listed field; old value snapshot, proposed value and reason recorded

PENDING

Approve

APPROVED

Independent authorized approver

Separation-of-duty rule passes; approval timestamp recorded

PENDING

Reject

REJECTED

Authorized approver

Reason required; terminal

APPROVED

Withdraw before application

REJECTED

Authorized approver

Reason required; no overlay applied

APPROVED

Apply overlay

APPLIED

Authorized correction officer/system

Overlay becomes effective; certificate-visible change creates reissue workflow/outbox event

E.13 CertificateIssue

Current state

Action

New state

Authorized actor

Preconditions and side effects

No issue

Initial issue during finalization

VALID

System in MF-05 transaction

Version 1, unique reference and token hash created atomically

VALID

Revoke

REVOKED

Authorized certificate staff

Reason and revoked_at required; public verification changes immediately

VALID

Reissue

SUPERSEDED

Authorized certificate staff/system

New higher-version issue is created as VALID; previous issue retained

REVOKED

Reissue

REVOKED remains

Authorized certificate staff

New higher-version issue may be VALID; revoked issue never changes to superseded or valid

Appendix F - Release Integrity and AI Handoff Checklist

A package is releasable only when all checks below pass.

Check

Required result

Version synchronization

Markdown metadata, AI contract, DOCX visible footer and DOCX custom properties all use 3.3-AI / 2026-08-08-R8

Canonical authority

Markdown is marked normative and DOCX is marked as its synchronized derivative

Identifier integrity

Requirement, business-rule, test-case and decision identifiers are unique within their namespaces

Status integrity

Every status used by a rule or transition appears in its entity-specific enum; no global status enum is introduced

Unknown-data discipline

Only genuinely external values remain USER_DATA_REQUIRED; no invented names, URLs, approvals or retention periods

Cross-module ownership

MF-01 eligibility, MF-02 planning, MF-03 booking, MF-04 inventory and MF-05 administration boundaries remain intact

Persistence integrity

Foreign keys, ordinary uniqueness, generated active-key uniqueness, guard-row locking, optimistic/pessimistic locking and idempotency are represented in migrations and integration tests

Event integrity

State change and outbox insertion are atomic; consumers and notification writes are idempotent

Documentation parity

Material rules added to one canonical file are present in the other; diagram differences are presentational only

Delivery QA

DOCX renders without clipping/overlap, tables remain readable, metadata is current, accessibility audit has no unresolved high-severity defect, and the final ZIP opens successfully