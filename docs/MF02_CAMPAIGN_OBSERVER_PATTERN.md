# MF-02 campaign lifecycle Observer pattern

Campaign management uses the lecture-style **Observer pattern** to record lifecycle audit history.
The service announces a change through a Subject, rather than saving the audit record itself.

## Lecture roles and implementation

| Lecture role | Implementation | Responsibility |
|---|---|---|
| Subject | [CampaignLifecycleSubject](../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/observer/CampaignLifecycleSubject.java) | Defines `addObserver`, `removeObserver`, and `notifyObservers`. |
| Observer | [CampaignLifecycleObserver](../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/observer/CampaignLifecycleObserver.java) | Defines `update(event)`. |
| Concrete Subject | [CampaignLifecycleNotifier](../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/observer/CampaignLifecycleNotifier.java) | Registers observers and notifies them synchronously in registration order. |
| Concrete Observer | [CampaignAuditObserver](../backend/src/main/java/com/se2030/vaccinationportal/campaign/core/observer/CampaignAuditObserver.java) | Saves the supplied event to existing campaign audit history. |

Spring injects the observer beans into the notifier at startup. There is one production observer.
Tests register a second observer to demonstrate one-to-many notification and removal.

## Demonstration

1. Sign in with permission to manage campaigns and open **Campaigns**.
2. Open an Active campaign's details and note its **Audit History** count.
3. Choose **Suspend Campaign**, enter a valid reason, and confirm.
4. Confirm that status is **Suspended** and Audit History contains exactly one new entry.
5. Show the following call chain in the code:

```text
VaccinationCampaignService.suspendCampaign(...)
  -> validates input, locks the campaign, and changes its status
  -> creates the existing CampaignLifecycleEvent
  -> lifecycleSubject.notifyObservers(event)
  -> CampaignLifecycleNotifier calls each observer.update(event)
  -> CampaignAuditObserver saves the event
  -> the service transaction commits
  -> Audit History displays the new record
```

Presentation explanation: "My campaign module uses Observer to separate lifecycle changes from
audit recording. The campaign service notifies a Subject, which updates registered Observers.
The audit Observer records the change. Other transactional observers could be added without
changing the lifecycle service."

## Behaviour and transaction safety

- Suspension, resumption, completion, cancellation, and archival use notifications.
- Schedule, Publish, and Activate retain their existing behaviour without new audit entries.
- Notifications reuse `CampaignLifecycleEvent` and the `campaign_lifecycle_events` table.
- Callbacks run inside the existing service transaction, not asynchronously or after commit.
- An observer failure propagates: the campaign change and all audit writes roll back together.
- Observers must not mutate the event or perform external side effects: database rollback cannot
  undo an email or remote request.
- Duplicate registrations are ignored. Null registrations are rejected. The observer list uses
  a stable snapshot, so adding/removing an observer during notification affects the next call.
- Runtime registration is an internal Java API, not a new HTTP endpoint. Production wiring keeps
  the audit observer registered; unregistering it would stop audit recording.

## Verification

- `CampaignLifecycleNotifierTests`: two observers, order, duplicates, removal, stable snapshots,
  null validation, and failure propagation.
- `CampaignAuditObserverTests`: exactly one save and persistence failure propagation.
- `CampaignObserverAtomicityIntegrationTests`: one committed audit entry on success; both status
  and an already inserted audit entry roll back when a later observer fails. Assertions run
  outside the failed service transaction.
- Existing campaign lifecycle, persistence, atomicity, and concurrency tests verify compatibility.

No frontend, API, or database migration is required.

From `backend/`, run the Observer tests with:

```powershell
mvn "-Dtest=CampaignLifecycleNotifierTests,CampaignAuditObserverTests,CampaignObserverAtomicityIntegrationTests" test
```

The separate MySQL concurrency profile requires Docker. On Docker engines incompatible with the
bundled client's default API version, use the process-local compatibility setting:

```powershell
mvn -Pmysql-concurrency "-Dapi.version=1.44" "-Dtest=CampaignLifecycleMySqlConcurrencyIntegrationTests" test
```
