# MF-02 EER — schema audit and verification report

**Decision: VERIFIED against the repository schema at the pinned develop commit.** The 22 MF-02 tables also match the configured local MySQL database. The local database has V0–V24; the diagram deliberately represents repository V0–V26. V25 and V26 are source-verified and are not claimed to be applied locally. No database write, migration execution, or Java/frontend edit was performed. Publication is authorized separately after validation.

## Git and scope

- Repository: `Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal`.
- Base branch: `develop`.
- Base commit: `dec3d2c43d1ec8dac4c781634604f28e059fba53`.
- Documentation branch: `docs/mf02-eer-diagram`.
- Worktree: `E:\SE PROJECT_Vac\mf02-eer-diagram`.
- Verification date: 08 October 2026 (Asia/Colombo).
- Source schema: 27 runtime Flyway migrations V0–V26, creating 59 application tables. The 55th local table is `flyway_schema_history` and is not a module entity.
- Ownership is established from actual `campaign/**` entity/collection mappings, migration DDL, repositories, and cross-module adapters; not inferred from table names alone.
- 21 MF-02 `@Entity` tables plus one `@ElementCollection` table = **22 owned tables / 205 columns**. The master model contains **39 tables / 363 columns** including 17 external dependencies.

## Inventory by category

| Category | Tables | Exact table names |
|---|---:|---|
| Campaign | 6 | `vaccination_campaigns`, `campaign_location_targets`, `campaign_dose_programmes`, `campaign_lifecycle_events`, `campaign_center_assignments`, `campaign_questionnaire_assignments` |
| Center | 6 | `vaccination_centers`, `operating_hours`, `center_closures`, `operational_resources`, `operational_staff_rosters`, `operational_staff_identities` |
| Session | 8 | `vaccination_sessions`, `session_recurrence_rules`, `session_recurrence_rule_weekdays`, `session_capacity_plans`, `session_change_events`, `session_resource_assignments`, `session_staff_assignments`, `time_slots` |
| Shared/reference | 2 | `vaccine_types`, `target_groups` |
| External dependencies | 17 | `user_accounts`, `roles`, `questionnaire`, `questionnaire_version`, `consent_notice_version`, `screening_submission`, `eligibility_assessment`, `appointment`, `citizen_campaign_booking_guard`, `waitlist_entry`, `vaccination_records`, `manufacturers`, `vaccine_batches`, `center_inventories`, `stock_transfers`, `appointment_change_event`, `waitlist_change_event` |

`vaccine_types` and `target_groups` are implemented and owned by MF-02, while consumed across modules. Their amber color identifies shared/reference catalogs, not different ownership. `user_accounts` and `roles` are shared authentication tables outside MF-02; their gray headers explicitly say Shared authentication. Other gray tables identify MF-01, MF-03, MF-04, or MF-05 ownership.

The external set includes every table with a declared FK directly pointing into MF-02, every target of the selected MF-02 logical associations, and the small supporting FK closure needed to avoid dangling constraints. It is not a whole-system ERD. MF-04 `stock_transactions`, MF-05 drafts/certificates/corrections, authentication token tables, questionnaire item/question tables, and other transitive consumers are outside this module-focused boundary. Their migrations were still inspected. No omitted table has a direct declared FK to an MF-02 table.

## Source and live-database verification

`spring.flyway.locations=classpath:db/migration` in [backend/src/main/resources/application.properties](../../../backend/src/main/resources/application.properties) establishes the runtime migration source. The historical scripts under `database/migrations/` are not in the runtime Flyway location and were not applied a second time. They were inspected as references.

The existing Docker service `vaccination-portal-mysql` was available, running **MySQL 8.4.10**, database `vaccination_portal`. Schema queries used the configured application credentials inside the container; no credentials appear in these artifacts. Every inspection query ran inside `START TRANSACTION READ ONLY` / `ROLLBACK`. Queries accessed `information_schema.TABLES`, `COLUMNS`, `KEY_COLUMN_USAGE`, `STATISTICS`, `REFERENTIAL_CONSTRAINTS`, `CHECK_CONSTRAINTS`, and `flyway_schema_history`; no citizen, appointment, staff, or vaccination row data was read. No service was started and no migration was executed.

All 22 MF-02 tables match the reconstructed source for column names/order, SQL types, nullability, defaults, identity columns, primary/unique keys, foreign-key endpoints and delete/update actions, explicit indexes, and CHECK constraint names. Seven observed InnoDB FK supporting indexes are also included in DBML and the index inventory below. MySQL reports SQL `BOOLEAN` as `TINYINT(1)`; this is type normalization, not a discrepancy. The diagram retains the migration spelling `BOOLEAN`.

### Local schema version difference

Local Flyway history contains successful V0–V24 and does not contain V25 or V26. [backend/src/main/resources/db/migration/V25__mf03_waitlist_session_uniqueness.sql](../../../backend/src/main/resources/db/migration/V25__mf03_waitlist_session_uniqueness.sql) on the pinned commit replaces external `waitlist_entry.active_campaign_id` and `uk_waitlist_active_citizen_campaign` with generated `active_session_id` and `uk_waitlist_active_citizen_session(citizen_id, active_session_id)`. The files show the V26 repository state, including the V25 generated-column change. V26 adds eight FKs to appointment/waitlist/booking-guard tables and introduces external appointment_change_event and waitlist_change_event tables, each with an optional FK to session_change_events. Five formerly logical campaign/session/slot links are now enforced FKs. These two history tables and all applicable new FKs are included. The other three V26 tables have no direct MF-02 FK and are outside this module-focused diagram. V25/V26 changes are verified from SQL/JPA source, not claimed to be installed locally. Every MF-02 table is unaffected and live-validated.

### SQL/JPA distinctions and application-only invariants

- All 205 MF-02 columns were matched against entity fields and embedded attribute overrides. All 24 ordinary `@JoinColumn` entity associations match database FK targets and nullability; the 25th MF-02 FK belongs to the recurrence weekday collection. Annotated string lengths and scalar column nullability agree with migrations.
- `SessionRecurrenceRule.weekdays` is an `@ElementCollection`: its `@CollectionTable` join annotation does not explicitly specify `nullable=false`; SQL and the live schema require `recurrence_rule_id NOT NULL`. The diagram follows SQL. The collection has no declared SQL primary key; its two columns form a named composite UNIQUE constraint. It must not be drawn as an invented entity with an `id` or composite PK.
- `vaccination_campaigns` has SQL indexes `idx_campaign_status` and `idx_campaign_service_window`, and `vaccination_centers` has `idx_center_status`; these are not repeated as JPA `@Table(indexes=...)`. They remain actual schema indexes and are included.
- JPA scalar IDs can have real SQL FKs: e.g. external vaccination records have confirmed campaign/center FKs, and vaccine batches have a confirmed vaccine-type FK. The diagram follows SQL even where Java has no `@ManyToOne`. Conversely, Java service lookups do not create SQL FKs.
- SQL V13 adds four time-slot CHECK constraints. Other Bean Validation annotations/service rules, such as effective-date non-overlap, center occupancy/readiness rules, and assignment scheduling conflicts, are application invariants unless a migration declares a constraint. They are not invented as CHECK constraints in this model.
- `Instant`/`LocalTime` mappings do not specify a physical SQL precision. Migrations and MySQL metadata determine the actual `DATETIME(6)`, `TIMESTAMP` (no fractional precision), and `TIME` types shown here. Java lifecycle methods update timestamps independently of SQL defaults/ON UPDATE clauses.
- No schema/JPA entity inheritance or subtype/supertype specialization was found in MF-02. Enums are scalar VARCHAR columns, not subtype tables. `SessionScheduleSnapshot` is embedded twice into `session_change_events` and contributes old/new columns; it is not an extra table. `permitted_dose_numbers` is a VARCHAR field, not an undocumented dose junction table. Location `target_code` and `target_type` are values; no fictional location entity or FK was added.

## Relationship semantics and Crow's Foot notation

Solid blue edges F01–F53 represent **actual database foreign keys**. There are **25 FKs defined on MF-02 tables**, **13 inbound cross-module FKs** pointing directly into MF-02, and **15 supporting external FKs** in the selected reference closure = **53 shown**. All 38 FKs directly touching MF-02 are included. Dashed amber L01–L13 represent **application-only associations**, not confirmed referential constraints. Physical/logical relationships have separate named layers in draw.io, so they can be shown/hidden independently.

For each enforced FK, a non-null child FK column means exactly one parent (`||`); a nullable child FK means zero or one parent (`o|`). A parent may have zero children; a UNIQUE key on the FK limits it to zero or one child, otherwise zero or many (`o<`). Minimum child cardinality is zero even if application lifecycle rules later require a configuration: no SQL assertion requires every parent to have a child. ON DELETE CASCADE or SET NULL does not make child-to-parent participation mandatory/optional by itself; column nullability does. No identifying primary-key FK is inferred. The weekday collection is anchored by an FK and a composite candidate key, while retaining its absence of an explicitly declared PK. Solid line color/style identifies enforcement here, not the identifying/non-identifying convention used by some other diagram tools.

For dashed logical edges, the matching parent is always shown as **0..1 at the database-guarantee level**, because no FK prevents unmatched scalar values. Non-null IDs do not prove parent existence without a FK. L03 has 0..1 child because `operational_staff_identities.user_id` is unique; all other selected logical associations are 0..many from the parent. Application validation may normally expect an existing parent (e.g. a published questionnaire version or an ensured staff anchor); this is described in the evidence column and is not mislabeled as a database guarantee. Audit actor values are opaque VARCHAR IDs; system actors and string-encoded account IDs are not treated as mandatory BIGINT account FKs.

### Enforced FK register

| ID | Constraint | Child column(s) | Parent column(s) | Parents per child | Children per parent | DELETE / UPDATE | Source |
|---|---|---|---|---|---|---|---|
| F01 | `fk_campaign_vaccine_type` | `vaccination_campaigns.vaccine_type_id` | `vaccine_types.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql) |
| F02 | `fk_campaign_target_group` | `vaccination_campaigns.target_group_id` | `target_groups.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql) |
| F03 | `fk_clt_campaign` | `campaign_location_targets.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F04 | `fk_cdp_campaign` | `campaign_dose_programmes.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F05 | `fk_cle_campaign` | `campaign_lifecycle_events.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F06 | `fk_cca_campaign` | `campaign_center_assignments.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F07 | `fk_cca_center` | `campaign_center_assignments.center_id` | `vaccination_centers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F08 | `fk_cqa_campaign` | `campaign_questionnaire_assignments.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F09 | `fk_operating_hours_center` | `operating_hours.center_id` | `vaccination_centers.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F10 | `fk_center_closure_center` | `center_closures.center_id` | `vaccination_centers.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F11 | `fk_resource_center` | `operational_resources.center_id` | `vaccination_centers.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F12 | `fk_staff_roster_center` | `operational_staff_rosters.center_id` | `vaccination_centers.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql) |
| F13 | `fk_session_campaign` | `vaccination_sessions.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql) |
| F14 | `fk_session_center` | `vaccination_sessions.center_id` | `vaccination_centers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql) |
| F15 | `fk_session_recurrence_rule` | `vaccination_sessions.recurrence_rule_id` | `session_recurrence_rules.id` | 0..1 | 0..many | SET NULL / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F16 | `fk_srr_campaign` | `session_recurrence_rules.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F17 | `fk_srr_center` | `session_recurrence_rules.center_id` | `vaccination_centers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F18 | `fk_srrw_recurrence_rule` | `session_recurrence_rule_weekdays.recurrence_rule_id` | `session_recurrence_rules.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F19 | `fk_scp_session` | `session_capacity_plans.session_id` | `vaccination_sessions.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F20 | `fk_sce_session` | `session_change_events.session_id` | `vaccination_sessions.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F21 | `fk_sra_session` | `session_resource_assignments.session_id` | `vaccination_sessions.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F22 | `fk_sra_resource` | `session_resource_assignments.resource_id` | `operational_resources.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F23 | `fk_ssa_session` | `session_staff_assignments.session_id` | `vaccination_sessions.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F24 | `fk_ssa_roster` | `session_staff_assignments.staff_roster_id` | `operational_staff_rosters.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) |
| F25 | `fk_time_slot_session` | `time_slots.session_id` | `vaccination_sessions.id` | 1 | 0..many | CASCADE / NO ACTION | [backend/src/main/resources/db/migration/V3__mf02_time_slot.sql](../../../backend/src/main/resources/db/migration/V3__mf02_time_slot.sql) |
| F26 | `fk_user_accounts_requested_role` | `user_accounts.requested_role_id` | `roles.id` | 0..1 | 0..many | SET NULL / NO ACTION | [backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql](../../../backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql) |
| F27 | `fk_user_accounts_reviewer` | `user_accounts.reviewed_by_user_id` | `user_accounts.id` | 0..1 | 0..many | SET NULL / NO ACTION | [backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql](../../../backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql) |
| F28 | `fk_questionnaire_version_questionnaire` | `questionnaire_version.questionnaire_id` | `questionnaire.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql) |
| F29 | `fk_screening_sub_version` | `screening_submission.version_id` | `questionnaire_version.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql) |
| F30 | `fk_screening_sub_consent` | `screening_submission.consent_notice_version_id` | `consent_notice_version.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql) |
| F31 | `fk_eligibility_assessment_sub` | `eligibility_assessment.submission_id` | `screening_submission.id` | 1 | 0..1 | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql) |
| F32 | `fk_appointment_citizen` | `appointment.citizen_id` | `user_accounts.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F33 | `fk_appointment_campaign` | `appointment.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F34 | `fk_appointment_slot` | `appointment.slot_id` | `time_slots.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F35 | `fk_booking_guard_citizen` | `citizen_campaign_booking_guard.citizen_id` | `user_accounts.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F36 | `fk_booking_guard_campaign` | `citizen_campaign_booking_guard.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F37 | `fk_waitlist_citizen` | `waitlist_entry.citizen_id` | `user_accounts.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F38 | `fk_waitlist_campaign` | `waitlist_entry.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F39 | `fk_waitlist_session` | `waitlist_entry.session_id` | `vaccination_sessions.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F40 | `fk_vaccination_record_appointment` | `vaccination_records.appointment_id` | `appointment.id` | 1 | 0..1 | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql](../../../backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql) |
| F41 | `fk_vaccination_record_campaign` | `vaccination_records.campaign_id` | `vaccination_campaigns.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql](../../../backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql) |
| F42 | `fk_vaccination_record_center` | `vaccination_records.center_id` | `vaccination_centers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql](../../../backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql) |
| F43 | `fk_vaccine_batch_manufacturer` | `vaccine_batches.manufacturer_id` | `manufacturers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) |
| F44 | `fk_vaccine_batch_vaccine_type` | `vaccine_batches.vaccine_type_id` | `vaccine_types.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) |
| F45 | `fk_center_inventory_center` | `center_inventories.center_id` | `vaccination_centers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) |
| F46 | `fk_center_inventory_batch` | `center_inventories.batch_id` | `vaccine_batches.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) |
| F47 | `fk_stock_transfer_batch` | `stock_transfers.batch_id` | `vaccine_batches.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) |
| F48 | `fk_stock_transfer_source_center` | `stock_transfers.source_center_id` | `vaccination_centers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) |
| F49 | `fk_stock_transfer_destination_center` | `stock_transfers.destination_center_id` | `vaccination_centers.id` | 1 | 0..many | RESTRICT / NO ACTION | [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) |
| F50 | `fk_appointment_change_appointment` | `appointment_change_event.appointment_id` | `appointment.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F51 | `fk_appointment_change_session_event` | `appointment_change_event.source_session_change_event_id` | `session_change_events.id` | 0..1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F52 | `fk_waitlist_change_entry` | `waitlist_change_event.waitlist_entry_id` | `waitlist_entry.id` | 1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |
| F53 | `fk_waitlist_change_session_event` | `waitlist_change_event.source_session_change_event_id` | `session_change_events.id` | 0..1 | 0..many | NO ACTION / NO ACTION | [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) |

### Logical associations — NO database foreign key

| ID | Scalar/reference field | Intended target | DB matching cardinality | Evidence and limitation |
|---|---|---|---|---|
| L01 | `campaign_questionnaire_assignments.questionnaire_version_id` | `questionnaire_version.id` | 0..1 parent / 0..many children | Publication port checks that the selected version exists and is published. No SQL FK. [backend/src/main/java/com/se2030/vaccinationportal/campaign/questionnaire/service/CampaignQuestionnaireAssignmentService.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/questionnaire/service/CampaignQuestionnaireAssignmentService.java) |
| L02 | `operational_staff_rosters.user_id` | `user_accounts.id` | 0..1 parent / 0..many children | Scalar authenticated staff identity; roster registration does not enforce an account FK or account existence. [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/query/JpaStaffContextQuery.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/query/JpaStaffContextQuery.java) |
| L03 | `operational_staff_identities.user_id` | `user_accounts.id` | 0..1 parent / 0..1 children | Unique per-user locking anchor; scalar ID, no account existence constraint. At most one anchor per account. [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalStaffIdentity.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalStaffIdentity.java) |
| L04 | `session_staff_assignments.user_id` | `user_accounts.id` | 0..1 parent / 0..many children | Copied staff user identity, resolved against the authenticated principal. No SQL FK. [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/query/JpaStaffContextQuery.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/query/JpaStaffContextQuery.java) |
| L05 | `operational_staff_rosters.user_id` | `operational_staff_identities.user_id` | 0..1 parent / 0..many children | Registering or modifying a roster ensures and locks the unique user anchor. An application concurrency association, not a database FK. [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/service/OperationalStaffRosterService.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/service/OperationalStaffRosterService.java) |
| L06 | `session_staff_assignments.user_id` | `operational_staff_identities.user_id` | 0..1 parent / 0..many children | Assignment services ensure and lock the unique user anchor. Not a database FK. [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/service/StaffAssignmentService.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/service/StaffAssignmentService.java) |
| L07 | `session_change_events.old_center_id` | `vaccination_centers.id` | 0..1 parent / 0..many children | Embedded schedule snapshot copies a center ID. Historical snapshot, not a FK or a separate entity/table. [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionChangeEvent.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionChangeEvent.java) |
| L08 | `session_change_events.new_center_id` | `vaccination_centers.id` | 0..1 parent / 0..many children | Embedded schedule snapshot copies a center ID. Historical snapshot, not a FK or a separate entity/table. [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionChangeEvent.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionChangeEvent.java) |
| L09 | `campaign_lifecycle_events.actor_id` | `user_accounts.id` | 0..1 parent / 0..many children | Opaque VARCHAR(64) actor; authenticated user IDs are encoded as strings, but system/fallback actors also exist. Conditional account match only. [backend/src/main/java/com/se2030/vaccinationportal/campaign/core/web/CampaignController.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/web/CampaignController.java) |
| L10 | `session_change_events.actor_id` | `user_accounts.id` | 0..1 parent / 0..many children | Opaque VARCHAR(64) actor; controllers encode the trusted principal ID. Service-level actor values are opaque and not account FK constraints. [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/web/SessionController.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/web/SessionController.java) |
| L11 | `session_capacity_plans.approved_by` | `user_accounts.id` | 0..1 parent / 0..many children | Nullable VARCHAR(64) approval identity derived from AuthenticatedActor.userId(). Encoded string, not a database FK. [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/service/SessionCapacityPlanService.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/service/SessionCapacityPlanService.java) |
| L12 | `screening_submission.campaign_id` | `vaccination_campaigns.id` | 0..1 parent / 0..many children | MF-01 scalar campaign identity; submission service uses CampaignScreeningReader and the effective questionnaire assignment. [backend/src/main/java/com/se2030/vaccinationportal/screening/submission/service/ScreeningSubmissionService.java](../../../backend/src/main/java/com/se2030/vaccinationportal/screening/submission/service/ScreeningSubmissionService.java) |
| L13 | `eligibility_assessment.campaign_id` | `vaccination_campaigns.id` | 0..1 parent / 0..many children | MF-01 assessment retains scalar campaign ID associated with screening. No campaign FK. [backend/src/main/java/com/se2030/vaccinationportal/screening/eligibility/model/EligibilityAssessment.java](../../../backend/src/main/java/com/se2030/vaccinationportal/screening/eligibility/model/EligibilityAssessment.java) |

These 13 associations are explicitly retained as DBML table notes and source comments, not executable `Ref` declarations. Therefore importing/exporting DBML does not accidentally manufacture database FKs for application-only links. Draw.io shows all 13 dashed links. Other scalar references in external tables (e.g. vaccination record citizen/batch/vaccinator IDs) remain visible as columns without invented FK labels; detailed external-only logical models are outside this MF-02 scope.

## Associative tables and composite uniqueness

Four MF-02 relationship/assignment junction tables are present: `campaign_center_assignments` (campaign/center), `campaign_questionnaire_assignments` (campaign/published questionnaire version, only campaign side is FK-enforced), `session_resource_assignments` (session/resource), and `session_staff_assignments` (session/staff roster with copied user identity). Their surrogate `id` PKs and effective/scheduling columns are preserved. There is no fictional direct many-to-many FK that skips these assignment tables.

`session_recurrence_rule_weekdays` is an additional collection/associative storage table, with composite UNIQUE `(recurrence_rule_id, day_of_week)` and **no declared PK**. `campaign_location_targets` is an owned set of campaign target values with composite UNIQUE `(campaign_id, target_type, target_code)`, not a confirmed join to another location table. Capacity plans and dose programmes store revision/version history with composite UNIQUE keys, not one-to-one campaign/session objects.

External `center_inventories` associates a center and a vaccine batch using `(center_id, batch_id)` uniqueness. External `citizen_campaign_booking_guard` is a citizen/campaign serialization guard, not owned by MF-02. Historical campaign-center assignments and session staff/resource assignments have no UNIQUE pair constraint, so multiple temporal rows per pair are possible.

## Complete column and constraint dictionary

SQL spelling and nullability are taken from migrations, corroborated by information_schema where applied. `DEFAULT —` means no explicit default in the migration; nullable fields accept NULL. Indexes marked **implicit** were observed in local MySQL and automatically support declared FKs. Generated expressions are documentation of real stored-generated columns; they are not extra relationships.

### `vaccination_campaigns`

**Owner:** MF-02; **group:** Campaign.

**Create migration:** [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/VaccinationCampaign.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/VaccinationCampaign.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `code` | `VARCHAR(64)` | NO | UNIQUE uk_vaccination_campaign_code | DEFAULT — |
| `name` | `VARCHAR(128)` | NO | — | DEFAULT — |
| `description` | `VARCHAR(1000)` | YES | — | DEFAULT — |
| `vaccine_type_id` | `BIGINT` | NO | FK fk_campaign_vaccine_type | DEFAULT — |
| `target_group_id` | `BIGINT` | NO | FK fk_campaign_target_group | DEFAULT — |
| `service_start_date` | `DATE` | NO | — | DEFAULT — |
| `service_end_date` | `DATE` | NO | — | DEFAULT — |
| `booking_open_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `booking_close_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `status` | `VARCHAR(32)` | NO | — | DEFAULT 'DRAFT' |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_vaccination_campaign_code` UNIQUE `(code)`.
- `idx_campaign_status` INDEX `(status)`.
- `idx_campaign_service_window` INDEX `(service_start_date, service_end_date)`.
- `fk_campaign_target_group` INDEX `(target_group_id)` — implicit InnoDB FK supporting index, observed read-only.
- `fk_campaign_vaccine_type` INDEX `(vaccine_type_id)` — implicit InnoDB FK supporting index, observed read-only.

No database CHECK constraints declared for this table.

### `campaign_location_targets`

**Owner:** MF-02; **group:** Campaign.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/CampaignLocationTarget.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/CampaignLocationTarget.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `campaign_id` | `BIGINT` | NO | FK fk_clt_campaign; UNIQUE uk_campaign_location_target | DEFAULT — |
| `target_type` | `VARCHAR(32)` | NO | UNIQUE uk_campaign_location_target | DEFAULT — |
| `target_code` | `VARCHAR(64)` | NO | UNIQUE uk_campaign_location_target | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_campaign_location_target` UNIQUE `(campaign_id, target_type, target_code)`.

No database CHECK constraints declared for this table.

### `campaign_dose_programmes`

**Owner:** MF-02; **group:** Campaign.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/CampaignDoseProgramme.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/CampaignDoseProgramme.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `campaign_id` | `BIGINT` | NO | FK fk_cdp_campaign; UNIQUE uk_campaign_dose_programme_version | DEFAULT — |
| `version_number` | `INT` | NO | UNIQUE uk_campaign_dose_programme_version | DEFAULT 1 |
| `dose_category` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `permitted_dose_numbers` | `VARCHAR(64)` | NO | — | DEFAULT — |
| `minimum_interval_days` | `INT` | NO | — | DEFAULT 0 |
| `previous_evidence_required` | `BOOLEAN` | NO | — | DEFAULT FALSE |
| `expected_service_minutes` | `INT` | NO | — | DEFAULT 10 |
| `active_from` | `DATE` | NO | — | DEFAULT — |
| `active_to` | `DATE` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_campaign_dose_programme_version` UNIQUE `(campaign_id, version_number)`.

No database CHECK constraints declared for this table.

### `campaign_lifecycle_events`

**Owner:** MF-02; **group:** Campaign.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/CampaignLifecycleEvent.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/model/CampaignLifecycleEvent.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `campaign_id` | `BIGINT` | NO | FK fk_cle_campaign | DEFAULT — |
| `change_type` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `previous_status` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `new_status` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `reason` | `VARCHAR(500)` | NO | — | DEFAULT — |
| `actor_id` | `VARCHAR(64)` | NO | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT — |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_campaign_lifecycle_campaign_created` INDEX `(campaign_id, created_at, id)`.

No database CHECK constraints declared for this table.

### `campaign_center_assignments`

**Owner:** MF-02; **group:** Campaign.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/CampaignCenterAssignment.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/CampaignCenterAssignment.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `campaign_id` | `BIGINT` | NO | FK fk_cca_campaign | DEFAULT — |
| `center_id` | `BIGINT` | NO | FK fk_cca_center | DEFAULT — |
| `active_from` | `DATE` | NO | — | DEFAULT — |
| `active_to` | `DATE` | YES | — | DEFAULT — |
| `assignment_status` | `VARCHAR(16)` | NO | — | DEFAULT 'ACTIVE' |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_campaign_center_assignment_campaign_history` INDEX `(campaign_id, active_from, id)`.
- `idx_campaign_center_assignment_center_history` INDEX `(center_id, active_from, id)`.
- `idx_campaign_center_assignment_overlap` INDEX `(campaign_id, center_id, assignment_status, active_from, active_to)`.

No database CHECK constraints declared for this table.

### `campaign_questionnaire_assignments`

**Owner:** MF-02; **group:** Campaign.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/questionnaire/model/CampaignQuestionnaireAssignment.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/questionnaire/model/CampaignQuestionnaireAssignment.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `campaign_id` | `BIGINT` | NO | FK fk_cqa_campaign | DEFAULT — |
| `questionnaire_version_id` | `BIGINT` | NO | — | DEFAULT — |
| `active_from` | `DATE` | NO | — | DEFAULT — |
| `active_to` | `DATE` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_cqa_campaign_active` INDEX `(campaign_id, active_from, active_to)`.
- `idx_cqa_qversion` INDEX `(questionnaire_version_id)`.

No database CHECK constraints declared for this table.

### `vaccination_centers`

**Owner:** MF-02; **group:** Center.

**Create migration:** [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/VaccinationCenter.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/VaccinationCenter.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `code` | `VARCHAR(32)` | NO | UNIQUE uk_vaccination_center_code | DEFAULT — |
| `name` | `VARCHAR(128)` | NO | — | DEFAULT — |
| `address` | `VARCHAR(255)` | NO | — | DEFAULT — |
| `contact` | `VARCHAR(128)` | NO | — | DEFAULT — |
| `timezone` | `VARCHAR(64)` | NO | — | DEFAULT 'UTC' |
| `center_status` | `VARCHAR(16)` | NO | — | DEFAULT 'ACTIVE' |
| `safe_concurrent_occupancy` | `INT` | NO | — | DEFAULT — |
| `session_service_ceiling` | `INT` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_vaccination_center_code` UNIQUE `(code)`.
- `idx_center_status` INDEX `(center_status)`.

No database CHECK constraints declared for this table.

### `operating_hours`

**Owner:** MF-02; **group:** Center.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperatingHours.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperatingHours.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `center_id` | `BIGINT` | NO | FK fk_operating_hours_center | DEFAULT — |
| `day_of_week` | `VARCHAR(16)` | NO | — | DEFAULT — |
| `opening_time` | `TIME` | NO | — | DEFAULT — |
| `closing_time` | `TIME` | NO | — | DEFAULT — |
| `effective_from` | `DATE` | NO | — | DEFAULT — |
| `effective_to` | `DATE` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_operating_hours_center_day` INDEX `(center_id, day_of_week)`.

No database CHECK constraints declared for this table.

### `center_closures`

**Owner:** MF-02; **group:** Center.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/CenterClosure.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/CenterClosure.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `center_id` | `BIGINT` | NO | FK fk_center_closure_center | DEFAULT — |
| `start_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `end_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `reason` | `VARCHAR(255)` | NO | — | DEFAULT — |
| `status` | `VARCHAR(16)` | NO | — | DEFAULT 'ACTIVE' |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_center_closure_center_start` INDEX `(center_id, start_at, id)`.
- `idx_center_closure_active_overlap` INDEX `(center_id, status, start_at, end_at)`.

No database CHECK constraints declared for this table.

### `operational_resources`

**Owner:** MF-02; **group:** Center.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalResource.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalResource.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `center_id` | `BIGINT` | NO | FK fk_resource_center; UNIQUE uk_center_resource_code | DEFAULT — |
| `code` | `VARCHAR(64)` | NO | UNIQUE uk_center_resource_code | DEFAULT — |
| `resource_type` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `name` | `VARCHAR(128)` | NO | — | DEFAULT — |
| `status` | `VARCHAR(16)` | NO | — | DEFAULT 'ACTIVE' |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_center_resource_code` UNIQUE `(center_id, code)`.
- `idx_operational_resource_center_type` INDEX `(center_id, resource_type, status)`.

No database CHECK constraints declared for this table.

### `operational_staff_rosters`

**Owner:** MF-02; **group:** Center.

**Create migration:** [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalStaffRoster.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalStaffRoster.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `center_id` | `BIGINT` | NO | FK fk_staff_roster_center; UNIQUE uk_center_staff_user | DEFAULT — |
| `user_id` | `BIGINT` | NO | UNIQUE uk_center_staff_user | DEFAULT — |
| `role` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `status` | `VARCHAR(16)` | NO | — | DEFAULT 'ACTIVE' |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_center_staff_user` UNIQUE `(center_id, user_id)`.
- `idx_operational_staff_user` INDEX `(user_id)`.
- `idx_operational_staff_center_role` INDEX `(center_id, role, status)`.

No database CHECK constraints declared for this table.

### `operational_staff_identities`

**Owner:** MF-02; **group:** Center.

**Create migration:** [backend/src/main/resources/db/migration/V2__mf02_operational_staff_identity.sql](../../../backend/src/main/resources/db/migration/V2__mf02_operational_staff_identity.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalStaffIdentity.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/center/model/OperationalStaffIdentity.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `user_id` | `BIGINT` | NO | UNIQUE uk_operational_staff_identity_user | DEFAULT — |
| `created_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_operational_staff_identity_user` UNIQUE `(user_id)`.

No database CHECK constraints declared for this table.

### `vaccination_sessions`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql).

**Alter migrations:** [backend/src/main/resources/db/migration/V9__mf02_session_booking_suspension.sql](../../../backend/src/main/resources/db/migration/V9__mf02_session_booking_suspension.sql), [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/VaccinationSession.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/VaccinationSession.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `campaign_id` | `BIGINT` | NO | FK fk_session_campaign | DEFAULT — |
| `center_id` | `BIGINT` | NO | FK fk_session_center | DEFAULT — |
| `recurrence_rule_id` | `BIGINT` | YES | FK fk_session_recurrence_rule | DEFAULT — |
| `occurrence_key` | `VARCHAR(64)` | YES | UNIQUE uk_session_occurrence_key | DEFAULT — |
| `origin_occurrence_date` | `DATE` | YES | — | DEFAULT — |
| `start_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `end_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `booking_close_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `schedule_timezone` | `VARCHAR(64)` | NO | — | DEFAULT 'UTC' |
| `lifecycle_status` | `VARCHAR(16)` | NO | — | DEFAULT 'DRAFT' |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |
| `booking_suspended` | `BOOLEAN` | NO | — | DEFAULT FALSE |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_session_occurrence_key` UNIQUE `(occurrence_key)`.
- `idx_session_campaign_schedule` INDEX `(campaign_id, start_at, end_at, lifecycle_status)`.
- `idx_session_center_schedule` INDEX `(center_id, start_at, end_at, lifecycle_status)`.
- `idx_session_recurrence_origin` INDEX `(recurrence_rule_id, origin_occurrence_date)`.

No database CHECK constraints declared for this table.

### `session_recurrence_rules`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/recurrence/model/SessionRecurrenceRule.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/recurrence/model/SessionRecurrenceRule.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `campaign_id` | `BIGINT` | NO | FK fk_srr_campaign | DEFAULT — |
| `center_id` | `BIGINT` | NO | FK fk_srr_center | DEFAULT — |
| `start_date` | `DATE` | NO | — | DEFAULT — |
| `end_date` | `DATE` | NO | — | DEFAULT — |
| `start_time` | `TIME` | NO | — | DEFAULT — |
| `end_time` | `TIME` | NO | — | DEFAULT — |
| `timezone` | `VARCHAR(64)` | NO | — | DEFAULT 'UTC' |
| `recurrence_status` | `VARCHAR(16)` | NO | — | DEFAULT 'ACTIVE' |
| `generation_key` | `VARCHAR(36)` | NO | UNIQUE uk_recurrence_rule_generation_key | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_recurrence_rule_generation_key` UNIQUE `(generation_key)`.
- `idx_recurrence_rule_campaign_range` INDEX `(campaign_id, start_date, end_date, recurrence_status)`.
- `idx_recurrence_rule_center_range` INDEX `(center_id, start_date, end_date, recurrence_status)`.

No database CHECK constraints declared for this table.

### `session_recurrence_rule_weekdays`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/recurrence/model/SessionRecurrenceRule.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/recurrence/model/SessionRecurrenceRule.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `recurrence_rule_id` | `BIGINT` | NO | FK fk_srrw_recurrence_rule; UNIQUE uk_recurrence_rule_weekday | DEFAULT — |
| `day_of_week` | `VARCHAR(16)` | NO | UNIQUE uk_recurrence_rule_weekday | DEFAULT — |

**No declared PRIMARY KEY.**

**Keys and relevant indexes:**

- `uk_recurrence_rule_weekday` UNIQUE `(recurrence_rule_id, day_of_week)`.

No database CHECK constraints declared for this table.

### `session_capacity_plans`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionCapacityPlan.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionCapacityPlan.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `session_id` | `BIGINT` | NO | FK fk_scp_session; UNIQUE uk_session_capacity_plan_revision | DEFAULT — |
| `revision` | `INT` | NO | UNIQUE uk_session_capacity_plan_revision | DEFAULT 1 |
| `duration_minutes` | `INT` | NO | — | DEFAULT — |
| `expected_service_minutes` | `INT` | NO | — | DEFAULT 10 |
| `assigned_vaccinator_count` | `INT` | NO | — | DEFAULT 0 |
| `available_station_count` | `INT` | NO | — | DEFAULT 0 |
| `fully_equipped_station_count` | `INT` | NO | — | DEFAULT 0 |
| `effective_station_count` | `INT` | NO | — | DEFAULT 0 |
| `recommended_throughput` | `INT` | NO | — | DEFAULT 0 |
| `approved_capacity` | `INT` | NO | — | DEFAULT 0 |
| `safe_concurrent_occupancy` | `INT` | NO | — | DEFAULT — |
| `expected_dwell_minutes` | `INT` | NO | — | DEFAULT 15 |
| `expected_accompanying_persons_per_booking` | `DOUBLE` | NO | — | DEFAULT 0.0 |
| `session_service_ceiling` | `INT` | YES | — | DEFAULT — |
| `override_reason` | `VARCHAR(255)` | YES | — | DEFAULT — |
| `approved_by` | `VARCHAR(64)` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_session_capacity_plan_revision` UNIQUE `(session_id, revision)`.
- `idx_capacity_plan_session_rev` INDEX `(session_id, revision)`.

No database CHECK constraints declared for this table.

### `session_change_events`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionChangeEvent.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionChangeEvent.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `session_id` | `BIGINT` | NO | FK fk_sce_session | DEFAULT — |
| `change_type` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `old_center_id` | `BIGINT` | NO | — | DEFAULT — |
| `old_schedule_timezone` | `VARCHAR(64)` | NO | — | DEFAULT — |
| `old_start_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `old_end_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `old_booking_close_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `old_lifecycle_status` | `VARCHAR(16)` | NO | — | DEFAULT — |
| `new_center_id` | `BIGINT` | NO | — | DEFAULT — |
| `new_schedule_timezone` | `VARCHAR(64)` | NO | — | DEFAULT — |
| `new_start_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `new_end_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `new_booking_close_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `new_lifecycle_status` | `VARCHAR(16)` | NO | — | DEFAULT — |
| `reason` | `VARCHAR(500)` | NO | — | DEFAULT — |
| `affected_appointment_count` | `INT` | NO | — | DEFAULT 0 |
| `affected_waitlist_count` | `INT` | NO | — | DEFAULT 0 |
| `actor_id` | `VARCHAR(64)` | NO | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT — |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_session_change_session_created` INDEX `(session_id, created_at, id)`.

No database CHECK constraints declared for this table.

### `session_resource_assignments`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionResourceAssignment.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/SessionResourceAssignment.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `session_id` | `BIGINT` | NO | FK fk_sra_session | DEFAULT — |
| `resource_id` | `BIGINT` | NO | FK fk_sra_resource | DEFAULT — |
| `start_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `end_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_resource_assignment_resource_window` INDEX `(resource_id, start_at, end_at)`.
- `idx_resource_assignment_session` INDEX `(session_id)`.

No database CHECK constraints declared for this table.

### `session_staff_assignments`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/StaffAssignment.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/StaffAssignment.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `session_id` | `BIGINT` | NO | FK fk_ssa_session | DEFAULT — |
| `staff_roster_id` | `BIGINT` | NO | FK fk_ssa_roster | DEFAULT — |
| `user_id` | `BIGINT` | NO | — | DEFAULT — |
| `role` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `start_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `end_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_staff_assignment_roster_window` INDEX `(staff_roster_id, start_at, end_at)`.
- `idx_staff_assignment_user_window` INDEX `(user_id, start_at, end_at)`.
- `idx_staff_assignment_session` INDEX `(session_id)`.

No database CHECK constraints declared for this table.

### `time_slots`

**Owner:** MF-02; **group:** Session.

**Create migration:** [backend/src/main/resources/db/migration/V3__mf02_time_slot.sql](../../../backend/src/main/resources/db/migration/V3__mf02_time_slot.sql).

**Alter migrations:** [backend/src/main/resources/db/migration/V13__mf02_time_slot_integrity_constraints.sql](../../../backend/src/main/resources/db/migration/V13__mf02_time_slot_integrity_constraints.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/TimeSlot.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/session/planning/model/TimeSlot.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `session_id` | `BIGINT` | NO | FK fk_time_slot_session | DEFAULT — |
| `start_time` | `TIMESTAMP` | NO | — | DEFAULT — |
| `end_time` | `TIMESTAMP` | NO | — | DEFAULT — |
| `capacity` | `INT` | NO | — | DEFAULT — |
| `reserved_count` | `INT` | NO | — | DEFAULT 0 |
| `lock_version` | `BIGINT` | NO | — | DEFAULT 0 |
| `created_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |
| `updated_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_time_slot_session` INDEX `(session_id)`.
- `idx_time_slot_start_time` INDEX `(start_time)`.

**Database CHECK constraints:**

- `chk_time_slots_capacity_positive`: `capacity > 0`.
- `chk_time_slots_reserved_nonnegative`: `reserved_count >= 0`.
- `chk_time_slots_reserved_within_capacity`: `reserved_count <= capacity`.
- `chk_time_slots_time_order`: `start_time < end_time`.

### `vaccine_types`

**Owner:** MF-02; **group:** Shared/reference.

**Create migration:** [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/reference/model/VaccineType.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/reference/model/VaccineType.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `code` | `VARCHAR(64)` | NO | UNIQUE uk_vaccine_type_code | DEFAULT — |
| `name` | `VARCHAR(128)` | NO | — | DEFAULT — |
| `dose_unit` | `VARCHAR(32)` | NO | — | DEFAULT 'dose' |
| `active` | `BOOLEAN` | NO | — | DEFAULT TRUE |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_vaccine_type_code` UNIQUE `(code)`.

No database CHECK constraints declared for this table.

### `target_groups`

**Owner:** MF-02; **group:** Shared/reference.

**Create migration:** [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql).

**JPA:** [backend/src/main/java/com/se2030/vaccinationportal/campaign/reference/model/TargetGroup.java](../../../backend/src/main/java/com/se2030/vaccinationportal/campaign/reference/model/TargetGroup.java).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `code` | `VARCHAR(64)` | NO | UNIQUE uk_target_group_code | DEFAULT — |
| `description` | `VARCHAR(255)` | NO | — | DEFAULT — |
| `active` | `BOOLEAN` | NO | — | DEFAULT TRUE |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_target_group_code` UNIQUE `(code)`.

No database CHECK constraints declared for this table.

### `user_accounts`

**Owner:** Shared authentication; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V4__shared_authentication_foundation.sql](../../../backend/src/main/resources/db/migration/V4__shared_authentication_foundation.sql).

**Alter migrations:** [backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql](../../../backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql), [backend/src/main/resources/db/migration/V23__user_account_login_lockout.sql](../../../backend/src/main/resources/db/migration/V23__user_account_login_lockout.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `full_name` | `VARCHAR(255)` | YES | — | DEFAULT — |
| `email` | `VARCHAR(255)` | NO | UNIQUE uk_user_accounts_email | DEFAULT — |
| `phone_number` | `VARCHAR(32)` | YES | — | DEFAULT — |
| `password_hash` | `VARCHAR(255)` | NO | — | DEFAULT — |
| `account_status` | `VARCHAR(32)` | NO | — | DEFAULT 'ACTIVE' |
| `requested_role_id` | `BIGINT` | YES | FK fk_user_accounts_requested_role | DEFAULT — |
| `reviewed_by_user_id` | `BIGINT` | YES | FK fk_user_accounts_reviewer | DEFAULT — |
| `reviewed_at` | `TIMESTAMP` | YES | — | DEFAULT — |
| `rejection_reason` | `VARCHAR(500)` | YES | — | DEFAULT — |
| `email_verified_at` | `TIMESTAMP` | YES | — | DEFAULT — |
| `locked_until` | `TIMESTAMP` | YES | — | DEFAULT — |
| `created_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |
| `updated_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP; ON UPDATE CURRENT_TIMESTAMP(6) |
| `failed_login_attempts` | `INT` | NO | — | DEFAULT 0 |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_user_accounts_email` UNIQUE `(email)`.
- `idx_user_accounts_status_created` INDEX `(account_status, created_at)`.
- `idx_user_accounts_requested_role` INDEX `(requested_role_id)`.
- `fk_user_accounts_reviewer` INDEX `(reviewed_by_user_id)` — implicit InnoDB FK supporting index, observed read-only.

No database CHECK constraints declared for this table.

### `roles`

**Owner:** Shared authentication; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V4__shared_authentication_foundation.sql](../../../backend/src/main/resources/db/migration/V4__shared_authentication_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `code` | `VARCHAR(64)` | NO | UNIQUE uk_roles_code | DEFAULT — |
| `name` | `VARCHAR(128)` | NO | — | DEFAULT — |
| `created_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_roles_code` UNIQUE `(code)`.

No database CHECK constraints declared for this table.

### `questionnaire`

**Owner:** MF-01; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `code` | `VARCHAR(64)` | NO | UNIQUE code | DEFAULT — |
| `name` | `VARCHAR(255)` | NO | — | DEFAULT — |
| `description` | `TEXT` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `code` UNIQUE `(code)`.

No database CHECK constraints declared for this table.

### `questionnaire_version`

**Owner:** MF-01; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `questionnaire_id` | `BIGINT` | NO | FK fk_questionnaire_version_questionnaire; UNIQUE uq_questionnaire_version | DEFAULT — |
| `version_number` | `INT` | NO | UNIQUE uq_questionnaire_version | DEFAULT — |
| `status` | `VARCHAR(32)` | NO | — | DEFAULT 'DRAFT' |
| `published_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `retired_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uq_questionnaire_version` UNIQUE `(questionnaire_id, version_number)`.

No database CHECK constraints declared for this table.

### `consent_notice_version`

**Owner:** MF-01; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `notice_code` | `VARCHAR(64)` | NO | UNIQUE uq_consent_notice_version | DEFAULT — |
| `version_number` | `INT` | NO | UNIQUE uq_consent_notice_version | DEFAULT — |
| `title` | `VARCHAR(255)` | NO | — | DEFAULT — |
| `content` | `TEXT` | NO | — | DEFAULT — |
| `content_hash` | `VARCHAR(128)` | NO | — | DEFAULT — |
| `published_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `retired_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uq_consent_notice_version` UNIQUE `(notice_code, version_number)`.

No database CHECK constraints declared for this table.

### `screening_submission`

**Owner:** MF-01; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql).

**Alter migrations:** [backend/src/main/resources/db/migration/V6__mf01_screening_corrections.sql](../../../backend/src/main/resources/db/migration/V6__mf01_screening_corrections.sql), [backend/src/main/resources/db/migration/V21__screening_submission_active_uniqueness.sql](../../../backend/src/main/resources/db/migration/V21__screening_submission_active_uniqueness.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `citizen_id` | `BIGINT` | NO | UNIQUE uk_screening_submission_active_citizen_campaign | DEFAULT — |
| `campaign_id` | `BIGINT` | NO | — | DEFAULT — |
| `version_id` | `BIGINT` | NO | FK fk_screening_sub_version | DEFAULT — |
| `consent_notice_version_id` | `BIGINT` | NO | FK fk_screening_sub_consent | DEFAULT — |
| `status` | `VARCHAR(32)` | NO | — | DEFAULT 'DRAFT' |
| `preliminary_result` | `VARCHAR(64)` | YES | — | DEFAULT — |
| `consent_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `submitted_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |
| `correction_instructions` | `TEXT` | YES | — | DEFAULT — |
| `correction_requested_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `active_campaign_id` | `BIGINT` | YES | UNIQUE uk_screening_submission_active_citizen_campaign | DEFAULT —; GENERATED ALWAYS AS (CASE WHEN status IN ('DRAFT', 'PENDING_REVIEW', 'CORRECTIONS_REQUIRED') THEN campaign_id ELSE NULL END) STORED |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_sub_citizen_campaign` INDEX `(citizen_id, campaign_id)`.
- `idx_sub_status` INDEX `(status)`.
- `uk_screening_submission_active_citizen_campaign` UNIQUE `(citizen_id, active_campaign_id)`.
- `fk_screening_sub_consent` INDEX `(consent_notice_version_id)` — implicit InnoDB FK supporting index, observed read-only.
- `fk_screening_sub_version` INDEX `(version_id)` — implicit InnoDB FK supporting index, observed read-only.

No database CHECK constraints declared for this table.

### `eligibility_assessment`

**Owner:** MF-01; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `submission_id` | `BIGINT` | NO | FK fk_eligibility_assessment_sub; UNIQUE submission_id | DEFAULT — |
| `citizen_id` | `BIGINT` | NO | — | DEFAULT — |
| `campaign_id` | `BIGINT` | NO | — | DEFAULT — |
| `reviewer_id` | `BIGINT` | NO | — | DEFAULT — |
| `status` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `preliminary_result` | `VARCHAR(64)` | NO | — | DEFAULT — |
| `reason` | `TEXT` | NO | — | DEFAULT — |
| `clinical_notes` | `TEXT` | YES | — | DEFAULT — |
| `reviewed_at` | `DATETIME(6)` | NO | — | DEFAULT — |
| `valid_until` | `DATETIME(6)` | YES | — | DEFAULT — |
| `revoked_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `revocation_reason` | `TEXT` | YES | — | DEFAULT — |
| `rescreening_authorized_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `rescreening_authorized_by` | `BIGINT` | YES | — | DEFAULT — |
| `rescreening_reason` | `TEXT` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `submission_id` UNIQUE `(submission_id)`.
- `idx_assess_citizen_campaign` INDEX `(citizen_id, campaign_id, status)`.

No database CHECK constraints declared for this table.

### `appointment`

**Owner:** MF-03; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V1__mf03_appointment_foundation.sql](../../../backend/src/main/resources/db/migration/V1__mf03_appointment_foundation.sql).

**Alter migrations:** [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `citizen_id` | `BIGINT` | NO | FK fk_appointment_citizen; UNIQUE uk_appointment_active_citizen_campaign | DEFAULT — |
| `campaign_id` | `BIGINT` | NO | FK fk_appointment_campaign | DEFAULT — |
| `slot_id` | `BIGINT` | NO | FK fk_appointment_slot | DEFAULT — |
| `status` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `booked_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |
| `cancelled_at` | `TIMESTAMP` | YES | — | DEFAULT — |
| `lock_version` | `BIGINT` | NO | — | DEFAULT 0 |
| `active_campaign_id` | `BIGINT` | YES | UNIQUE uk_appointment_active_citizen_campaign | DEFAULT —; GENERATED ALWAYS AS (CASE WHEN status IN ('CONFIRMED', 'CHECKED_IN') THEN campaign_id ELSE NULL END) STORED |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_appointment_active_citizen_campaign` UNIQUE `(citizen_id, active_campaign_id)`.
- `idx_appointment_citizen` INDEX `(citizen_id)`.
- `idx_appointment_campaign` INDEX `(campaign_id)`.
- `idx_appointment_slot` INDEX `(slot_id)`.
- `idx_appointment_status` INDEX `(status)`.

No database CHECK constraints declared for this table.

### `citizen_campaign_booking_guard`

**Owner:** MF-03; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V1__mf03_appointment_foundation.sql](../../../backend/src/main/resources/db/migration/V1__mf03_appointment_foundation.sql).

**Alter migrations:** [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `citizen_id` | `BIGINT` | NO | FK fk_booking_guard_citizen; UNIQUE uk_booking_guard_citizen_campaign | DEFAULT — |
| `campaign_id` | `BIGINT` | NO | FK fk_booking_guard_campaign; UNIQUE uk_booking_guard_citizen_campaign | DEFAULT — |
| `lock_version` | `BIGINT` | NO | — | DEFAULT 0 |
| `created_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_booking_guard_citizen_campaign` UNIQUE `(citizen_id, campaign_id)`.
- `idx_booking_guard_citizen` INDEX `(citizen_id)`.
- `idx_booking_guard_campaign` INDEX `(campaign_id)`.

No database CHECK constraints declared for this table.

### `waitlist_entry`

**Owner:** MF-03; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V7__mf03_waitlist_foundation.sql](../../../backend/src/main/resources/db/migration/V7__mf03_waitlist_foundation.sql).

**Alter migrations:** [backend/src/main/resources/db/migration/V25__mf03_waitlist_session_uniqueness.sql](../../../backend/src/main/resources/db/migration/V25__mf03_waitlist_session_uniqueness.sql), [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `citizen_id` | `BIGINT` | NO | FK fk_waitlist_citizen; UNIQUE uk_waitlist_active_citizen_session | DEFAULT — |
| `campaign_id` | `BIGINT` | NO | FK fk_waitlist_campaign | DEFAULT — |
| `session_id` | `BIGINT` | NO | FK fk_waitlist_session | DEFAULT — |
| `status` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `joined_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |
| `lock_version` | `BIGINT` | NO | — | DEFAULT 0 |
| `active_session_id` | `BIGINT` | YES | UNIQUE uk_waitlist_active_citizen_session | DEFAULT —; GENERATED ALWAYS AS (CASE WHEN status = 'WAITING' THEN session_id ELSE NULL END) STORED |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `idx_waitlist_citizen` INDEX `(citizen_id)`.
- `idx_waitlist_campaign` INDEX `(campaign_id)`.
- `idx_waitlist_session_status_joined` INDEX `(session_id, status, joined_at)`.
- `uk_waitlist_active_citizen_session` UNIQUE `(citizen_id, active_session_id)`.

No database CHECK constraints declared for this table.

### `vaccination_records`

**Owner:** MF-05; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql](../../../backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `appointment_id` | `BIGINT` | NO | FK fk_vaccination_record_appointment; UNIQUE uk_vaccination_record_appointment | DEFAULT — |
| `citizen_id` | `BIGINT` | NO | — | DEFAULT — |
| `campaign_id` | `BIGINT` | NO | FK fk_vaccination_record_campaign | DEFAULT — |
| `center_id` | `BIGINT` | NO | FK fk_vaccination_record_center | DEFAULT — |
| `vaccinator_id` | `BIGINT` | NO | — | DEFAULT — |
| `vaccine_batch_id` | `BIGINT` | NO | — | DEFAULT — |
| `dose_number` | `INT` | NO | — | DEFAULT — |
| `administration_time` | `DATETIME(6)` | NO | — | DEFAULT — |
| `next_dose_date` | `DATE` | YES | — | DEFAULT — |
| `authorized_notes` | `VARCHAR(2000)` | YES | — | DEFAULT — |
| `finalized_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_vaccination_record_appointment` UNIQUE `(appointment_id)`.
- `idx_vaccination_record_citizen_administered` INDEX `(citizen_id, administration_time)`.
- `idx_vaccination_record_batch` INDEX `(vaccine_batch_id)`.
- `fk_vaccination_record_campaign` INDEX `(campaign_id)` — implicit InnoDB FK supporting index, observed read-only.
- `fk_vaccination_record_center` INDEX `(center_id)` — implicit InnoDB FK supporting index, observed read-only.

No database CHECK constraints declared for this table.

### `manufacturers`

**Owner:** MF-04; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `code` | `VARCHAR(64)` | NO | UNIQUE uk_manufacturer_code | DEFAULT — |
| `name` | `VARCHAR(128)` | NO | UNIQUE uk_manufacturer_name | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_manufacturer_code` UNIQUE `(code)`.
- `uk_manufacturer_name` UNIQUE `(name)`.

**Database CHECK constraints:**

- `chk_manufacturer_code_not_blank`: `TRIM(code) <> ''`.
- `chk_manufacturer_name_not_blank`: `TRIM(name) <> ''`.

### `vaccine_batches`

**Owner:** MF-04; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `manufacturer_id` | `BIGINT` | NO | FK fk_vaccine_batch_manufacturer; UNIQUE uk_vaccine_batch_identity | DEFAULT — |
| `vaccine_type_id` | `BIGINT` | NO | FK fk_vaccine_batch_vaccine_type; UNIQUE uk_vaccine_batch_identity | DEFAULT — |
| `batch_number` | `VARCHAR(128)` | NO | UNIQUE uk_vaccine_batch_identity | DEFAULT — |
| `manufacture_date` | `DATE` | NO | — | DEFAULT — |
| `expiry_date` | `DATE` | NO | — | DEFAULT — |
| `batch_status` | `VARCHAR(32)` | NO | — | DEFAULT 'ACTIVE' |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_vaccine_batch_identity` UNIQUE `(manufacturer_id, vaccine_type_id, batch_number)`.
- `idx_vaccine_batch_vaccine_type` INDEX `(vaccine_type_id)`.
- `idx_vaccine_batch_status` INDEX `(batch_status)`.
- `idx_vaccine_batch_expiry_date` INDEX `(expiry_date)`.

**Database CHECK constraints:**

- `chk_vaccine_batch_number_not_blank`: `TRIM(batch_number) <> ''`.
- `chk_vaccine_batch_dates`: `manufacture_date < expiry_date`.
- `chk_vaccine_batch_status`: ` batch_status IN ('ACTIVE', 'QUARANTINED', 'RECALLED', 'EXPIRED', 'INACTIVE') `.

### `center_inventories`

**Owner:** MF-04; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `center_id` | `BIGINT` | NO | FK fk_center_inventory_center; UNIQUE uk_center_inventory_center_batch | DEFAULT — |
| `batch_id` | `BIGINT` | NO | FK fk_center_inventory_batch; UNIQUE uk_center_inventory_center_batch | DEFAULT — |
| `quantity` | `INT` | NO | — | DEFAULT 0 |
| `lock_version` | `BIGINT` | NO | — | DEFAULT 0 |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |
| `updated_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6); ON UPDATE CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_center_inventory_center_batch` UNIQUE `(center_id, batch_id)`.
- `idx_center_inventory_batch` INDEX `(batch_id)`.
- `idx_center_inventory_center_quantity` INDEX `(center_id, quantity)`.

**Database CHECK constraints:**

- `chk_center_inventory_quantity`: `quantity >= 0`.

### `stock_transfers`

**Owner:** MF-04; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `correlation_id` | `VARCHAR(64)` | NO | UNIQUE uk_stock_transfer_correlation | DEFAULT — |
| `batch_id` | `BIGINT` | NO | FK fk_stock_transfer_batch | DEFAULT — |
| `source_center_id` | `BIGINT` | NO | FK fk_stock_transfer_source_center | DEFAULT — |
| `destination_center_id` | `BIGINT` | NO | FK fk_stock_transfer_destination_center | DEFAULT — |
| `quantity` | `INT` | NO | — | DEFAULT — |
| `status` | `VARCHAR(32)` | NO | — | DEFAULT — |
| `requested_by` | `BIGINT` | NO | — | DEFAULT — |
| `completed_at` | `DATETIME(6)` | YES | — | DEFAULT — |
| `reason` | `VARCHAR(500)` | YES | — | DEFAULT — |
| `created_at` | `DATETIME(6)` | NO | — | DEFAULT CURRENT_TIMESTAMP(6) |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_stock_transfer_correlation` UNIQUE `(correlation_id)`.
- `idx_stock_transfer_batch` INDEX `(batch_id)`.
- `idx_stock_transfer_source_center` INDEX `(source_center_id)`.
- `idx_stock_transfer_destination_center` INDEX `(destination_center_id)`.
- `idx_stock_transfer_requested_by` INDEX `(requested_by)`.
- `idx_stock_transfer_status_created` INDEX `(status, created_at)`.

**Database CHECK constraints:**

- `chk_stock_transfer_correlation_not_blank`: `TRIM(correlation_id) <> ''`.
- `chk_stock_transfer_quantity`: `quantity > 0`.
- `chk_stock_transfer_distinct_centers`: `source_center_id <> destination_center_id`.

### `appointment_change_event`

**Owner:** MF-03; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `appointment_id` | `BIGINT` | NO | FK fk_appointment_change_appointment; UNIQUE uk_appointment_change_session_event | DEFAULT — |
| `source_session_change_event_id` | `BIGINT` | YES | FK fk_appointment_change_session_event; UNIQUE uk_appointment_change_session_event | DEFAULT — |
| `change_type` | `VARCHAR(40)` | NO | UNIQUE uk_appointment_change_session_event | DEFAULT — |
| `old_slot_id` | `BIGINT` | YES | — | DEFAULT — |
| `new_slot_id` | `BIGINT` | YES | — | DEFAULT — |
| `reason` | `VARCHAR(500)` | YES | — | DEFAULT — |
| `actor_id` | `VARCHAR(64)` | YES | — | DEFAULT — |
| `created_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_appointment_change_session_event` UNIQUE `(appointment_id, source_session_change_event_id, change_type)`.
- `idx_appointment_change_history` INDEX `(appointment_id, created_at, id)`.

No database CHECK constraints declared for this table.

### `waitlist_change_event`

**Owner:** MF-03; **group:** External dependencies.

**Create migration:** [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql).

| Column | SQL type | Nullable | Key membership | Default / extra |
|---|---|---|---|---|
| `id` | `BIGINT` | NO | PK | DEFAULT —; AUTO_INCREMENT |
| `waitlist_entry_id` | `BIGINT` | NO | FK fk_waitlist_change_entry; UNIQUE uk_waitlist_change_session_event | DEFAULT — |
| `source_session_change_event_id` | `BIGINT` | YES | FK fk_waitlist_change_session_event; UNIQUE uk_waitlist_change_session_event | DEFAULT — |
| `old_status` | `VARCHAR(40)` | YES | — | DEFAULT — |
| `new_status` | `VARCHAR(40)` | NO | — | DEFAULT — |
| `reason` | `VARCHAR(500)` | YES | — | DEFAULT — |
| `actor_id` | `VARCHAR(64)` | YES | — | DEFAULT — |
| `created_at` | `TIMESTAMP` | NO | — | DEFAULT CURRENT_TIMESTAMP |

**Keys and relevant indexes:**

- `PRIMARY` PRIMARY KEY `(id)`.
- `uk_waitlist_change_session_event` UNIQUE `(waitlist_entry_id, source_session_change_event_id)`.
- `idx_waitlist_change_history` INDEX `(waitlist_entry_id, created_at, id)`.

No database CHECK constraints declared for this table.

## Artifact validation and usability

- **Completeness:** source ownership inventory, master draw.io objects, and parsed DBML table sets are exactly equal for all 22 owned tables. The master has exactly 39 table objects and no duplicate table. Focus pages repeat tables deliberately as views, not additional master entities.
- **Columns/keys:** all 363 shown columns were compared independently against reconstructed SQL for names/order, SQL type, nullability and PK/FK flags. Parsed DBML column/PK/unique/index definitions match the source/live model. There are 205 owned columns. Every key endpoint exists.
- **Relationships:** all 53 DBML Ref endpoints and referential actions match SQL. Master XML contains exactly 53 physical edges plus 13 dashed logical edges; each edge connects the correct actual parent/child column row. All Crow's Foot markers were checked against nullability/uniqueness.
- **Draw.io:** uncompressed `mxfile` with five valid `mxGraphModel` pages; cell IDs and parent/source/target references are valid within each page. All five pages were decoded and rendered using the draw.io-compatible mxGraph engine and the official draw.io ER marker library in a Chromium browser. Tables/column rows and relationships are editable native cells, not flattened pictures.
- **SVG:** exported from the decoded master draw.io graph using `mxSvgCanvas2D` / `mxImageExport`. It is a pure vector SVG with no `foreignObject`, linked raster image, or external font dependency. Every table name, column name, and relationship ID is present in the SVG text. Canvas: **5805 × 5626**.
- **PNG:** the SVG was rasterized successfully with Sharp to **5805 × 5626** pixels. Complete overview and table detail were visually inspected. All five pages were checked for overlapping relationship labels; label offsets were adjusted where needed.
- **DBML:** parsed and semantically validated with `@dbml/core`; 39 tables and 53 actual FK Ref declarations. SQL CHECK/ON UPDATE/generated-column expressions and application-only associations are preserved in notes because DBML is not a lossless MySQL DDL executor. This DBML is documentation source, not a migration script.
- **Readability:** table name/owner headers are 17px, column names 13px, SQL types 12px and key/nullability labels 11px at 100% zoom. Every table includes every column, PK/FK flags and unique-key membership. The master is intentionally a large poster; fitting it onto one A4 page makes text unreadable. Use 100%/125% zoom, a large-format report appendix, or the focused pages for viva slides. Orthogonal routing avoids table bodies; some shared routing paths/line crossings remain in the complete master. Separate physical/logical layers and focused pages reduce that density without omitting master tables.

| Page | Tables | Relationships | Purpose |
|---|---:|---:|---|
| 01 Master - all 22 MF-02 tables | 39 | 66 | Complete master |
| 02 Campaign | 11 | 10 | Focused editable view; tables intentionally repeat across pages |
| 03 Center | 13 | 19 | Focused editable view; tables intentionally repeat across pages |
| 04 Session | 13 | 19 | Focused editable view; tables intentionally repeat across pages |
| 05 Cross-module references | 29 | 52 | Focused editable view; tables intentionally repeat across pages |

## Preservation and remaining issues

Only the five MF02-EER documentation artifacts are included in this change. The documentation worktree was fast-forwarded to the pinned remote develop commit before regeneration. No application source, migration, MF02-design.md, or unrelated edits in other worktrees were modified. No backend startup or database migration was used for documentation validation. GitHub CI is checked separately during publication.

The local MF-03 waitlist database still lacks V25 and V26; they can be applied during the normal application migration workflow. This task did not change it. Application-only scalar/opaque actor links cannot be called referential constraints; their lack of SQL enforcement is explicit in every artifact. There are no omitted MF-02 tables, unknown physical FK endpoints, invented entity specializations, or unresolved MF-02 schema contradictions.

## Inspected migration manifest

SHA-256 identifies the exact inspected source. All runtime migrations were parsed, including other-module migrations which alter or reference the selected tables.

| Migration | SHA-256 |
|---|---|
| [backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql](../../../backend/src/main/resources/db/migration/V0__core_campaign_center_session_foundation.sql) | `554279f37e95b02d909648aae4260fb1c624146cfce6969fd1d225bae3d4aea2` |
| [backend/src/main/resources/db/migration/V1__mf03_appointment_foundation.sql](../../../backend/src/main/resources/db/migration/V1__mf03_appointment_foundation.sql) | `26da20e708baee4eb5396476448b83bb893c721926f61c4c70d2dab85f48c454` |
| [backend/src/main/resources/db/migration/V2__mf02_operational_staff_identity.sql](../../../backend/src/main/resources/db/migration/V2__mf02_operational_staff_identity.sql) | `a2de7c93ce2e587ee705d20dc8114c52ada33505afc3c63399ec95ebe69ad5fd` |
| [backend/src/main/resources/db/migration/V3__mf02_time_slot.sql](../../../backend/src/main/resources/db/migration/V3__mf02_time_slot.sql) | `dae9c93fa011d366dfc0adef6db79fa0a4d71083085a0d3a792eff4776a0f86f` |
| [backend/src/main/resources/db/migration/V4__shared_authentication_foundation.sql](../../../backend/src/main/resources/db/migration/V4__shared_authentication_foundation.sql) | `34983f4af6654e8eef03ce7fc9f4e8a9f5aacb5fb538dbcac4725425bc4aa1a5` |
| [backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql](../../../backend/src/main/resources/db/migration/V5__mf01_screening_foundation.sql) | `a5288ea671a500ddcee6f7962617f61cb667b63fde94b1c2c152d6590d966a83` |
| [backend/src/main/resources/db/migration/V6__mf01_screening_corrections.sql](../../../backend/src/main/resources/db/migration/V6__mf01_screening_corrections.sql) | `51db32026d8862bcfc3a3ad03be28f48b212fc3158ab2a3b6b28b0045d086bd8` |
| [backend/src/main/resources/db/migration/V7__mf03_waitlist_foundation.sql](../../../backend/src/main/resources/db/migration/V7__mf03_waitlist_foundation.sql) | `42a43407e1561fe48673ae00b201481f52d8e559a1fff7ef8dc4d90e58c0f98b` |
| [backend/src/main/resources/db/migration/V8__mf01_screening_triggered_rule_evidence.sql](../../../backend/src/main/resources/db/migration/V8__mf01_screening_triggered_rule_evidence.sql) | `4a36c268e6a6fc883bc56470d838a30ce8c9eac9181792a2b7a42b0ff907510b` |
| [backend/src/main/resources/db/migration/V9__mf02_session_booking_suspension.sql](../../../backend/src/main/resources/db/migration/V9__mf02_session_booking_suspension.sql) | `4cf10079458c0ee0ba284411661ef137b6f99c0cec1d8d21a43a3a26e27e4408` |
| [backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql](../../../backend/src/main/resources/db/migration/V10__mf02_campaign_session_supporting_tables.sql) | `6729b052f493efbc884dd2a4431884b7c434a2ae45a33108c8cc7386037cf6a3` |
| [backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql](../../../backend/src/main/resources/db/migration/V11__mf05_vaccination_foundation.sql) | `81c601154a71fe026c0bae78da7469e2bfa76b487aab2204c1c7dc173db354cf` |
| [backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql](../../../backend/src/main/resources/db/migration/V12__shared_auth_registration_foundation.sql) | `e73498d01d04870b25ba57a98bd8069301eb1d293d73869392cd698186c40fe4` |
| [backend/src/main/resources/db/migration/V13__mf02_time_slot_integrity_constraints.sql](../../../backend/src/main/resources/db/migration/V13__mf02_time_slot_integrity_constraints.sql) | `d0159fdbb3c383ddf25249206155674ae4b16a710a211f4aeada42d074eea8a8` |
| [backend/src/main/resources/db/migration/V14__citizen_profile_foundation.sql](../../../backend/src/main/resources/db/migration/V14__citizen_profile_foundation.sql) | `e879e0468ec1b167ac01fbb471f2beda058baaa462da60eed2e783f6e66d7de5` |
| [backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql](../../../backend/src/main/resources/db/migration/V15__mf04_inventory_foundation.sql) | `2327608bfac3e74c6cb08f0d4b73a07bc742ee7ac88c4442386bcc81f8cffdb5` |
| [backend/src/main/resources/db/migration/V16__add_super_admin_role.sql](../../../backend/src/main/resources/db/migration/V16__add_super_admin_role.sql) | `4970a0da18e97cc972b5da0e6bc19d77e73e8e91f9c7ddcb8fb087cab18f7467` |
| [backend/src/main/resources/db/migration/V17__auth_verification_and_password_recovery.sql](../../../backend/src/main/resources/db/migration/V17__auth_verification_and_password_recovery.sql) | `d1b6ffb49e386ef1df7d9aba446e69abebee1d6f1d7d29232aaf041cce81942d` |
| [backend/src/main/resources/db/migration/V18__mf05_correction_certificate_lifecycle.sql](../../../backend/src/main/resources/db/migration/V18__mf05_correction_certificate_lifecycle.sql) | `def6005cdbea2e7ceeeb35b8c7e24aa7e07bff6297b1200f15b463e5045afa3c` |
| [backend/src/main/resources/db/migration/V19__add_vaccinator_auth_role.sql](../../../backend/src/main/resources/db/migration/V19__add_vaccinator_auth_role.sql) | `3459ae38f6910aadff3eeb18c67d674d0203421a1e77597040b8219eb9572d67` |
| [backend/src/main/resources/db/migration/V20__vaccination_draft_deletion_audit.sql](../../../backend/src/main/resources/db/migration/V20__vaccination_draft_deletion_audit.sql) | `64b999c23e88bed01eb7df2afdef5195640e9c8359166c23e2282c0d7e481277` |
| [backend/src/main/resources/db/migration/V21__screening_submission_active_uniqueness.sql](../../../backend/src/main/resources/db/migration/V21__screening_submission_active_uniqueness.sql) | `acd9bd288c7b64778d1968fd73948697fe43eb75984dcb881792d5c80b081882` |
| [backend/src/main/resources/db/migration/V22__add_email_verification.sql](../../../backend/src/main/resources/db/migration/V22__add_email_verification.sql) | `cccb891362b623fa7c4e76d99f81be149ce5e60ddca318390544c2075c888768` |
| [backend/src/main/resources/db/migration/V23__user_account_login_lockout.sql](../../../backend/src/main/resources/db/migration/V23__user_account_login_lockout.sql) | `005ef5c9d1b5394700851f4cba250668dec597c265ad917b4cf0881f91de74e3` |
| [backend/src/main/resources/db/migration/V24__add_refresh_token_sessions_family_revocation_index.sql](../../../backend/src/main/resources/db/migration/V24__add_refresh_token_sessions_family_revocation_index.sql) | `813334dfeae110571ecd0c1e2a896d76e81b2928454df9a60fc96ded68a7e10a` |
| [backend/src/main/resources/db/migration/V25__mf03_waitlist_session_uniqueness.sql](../../../backend/src/main/resources/db/migration/V25__mf03_waitlist_session_uniqueness.sql) | `3129e45f7e1eb574a44a2cdb7d7846faa4a4cd1537da08894e287ebce9de1ef4` |
| [backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql](../../../backend/src/main/resources/db/migration/V26__mf03_history_and_event_consumption.sql) | `5ff2528afd1e3170b8e5fd30258648f78cf9cde595fb80f149db79015a011e67` |
