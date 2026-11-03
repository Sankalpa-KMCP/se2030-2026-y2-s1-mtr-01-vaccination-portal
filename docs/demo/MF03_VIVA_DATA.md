# MF-03 local viva data

Run `database/seeds/mf03_viva_demo.sql` using your local MySQL connection after Flyway V26. Each run adds a separately named synthetic campaign and accounts. Existing records are not deleted or overwritten. Do not run against a production database.

The script prints the exact campaign, session, slot, appointment, waitlist, and account IDs. All demo accounts use password **VivaDemo!2026**. Email addresses have the form `mf03.<scenario>.<run>@example.test`.

| Account scenario | Demonstration |
|---|---|
| book | Search and book an open future slot; try again to demonstrate duplicate rejection |
| manage | View confirmed appointment; reschedule to replacement slot, then cancel |
| holder | Cancel the full-session booking to promote the first waiter |
| join | Join the full session through a prepared API request while it is still full |
| waiter | First waiting citizen; becomes confirmed after holder cancels |
| leave | Second waiting citizen; view queue position and leave |
| checkin | Staff checks in the confirmed arrival appointment |
| reverse | Staff reverses the preloaded checked-in appointment |
| noshow | Staff marks the overdue confirmed appointment as no-show |
| history | View cancelled, no-show, and completed records |
| ineligible | Booking rejection for screening ineligibility |
| unverified | Booking rejection for unverified email; .test mail cannot receive real email |
| staff | Center manager assigned to the synthetic center; staff check-in operations |
| reviewer | Synthetic healthcare officer referenced by seed eligibility assessments |

Use the campaign and center IDs printed by the script. Future bookings start 48 hours after loading; the full session starts 96 hours after loading. Search across today and the following seven days.

Check-in starts ten minutes after loading, so demonstrate it within about 40 minutes of loading. For later practice, run the seed again to obtain a fresh isolated dataset. Old data remains. Refresh/login again after switching accounts.

Full sessions are omitted from availability search. The waitlist page displays existing entries; it is not a general full-session join form. To demonstrate joining, use the authenticated API with the printed campaign/session IDs, or demonstrate the already seeded queue and promotion via the UI. Do not claim that searching a full session will display it.

The synthetic eligibility and initial appointment states were inserted for preparation. They do not represent real clinical decisions or runtime observer notifications. Perform a fresh booking/cancellation/promotion during the viva to show actual audit/notification observers. MF-05 completion additionally requires its vaccination prerequisites, which this MF-03 dataset does not seed.

## Dataset loaded on 9 October 2026

Campaign: **MF03-VIVA-20261009041456** (ID **2**). Center: **MF03 Viva Demo Center** (ID **3**).

All passwords: **VivaDemo!2026**. Replace `<scenario>` below with the account name:
`mf03.<scenario>.20261009041456@example.test`.

| Scenario/account | Citizen/user ID | Ready-to-use record |
|---|---:|---|
| book | 11 | Open slot **4**; search campaign **2**, center **3** |
| manage | 12 | Appointment **8**; reschedule from slot **4** to **5**, or cancel |
| holder | 13 | Appointment **9**, slot **6**; cancel to promote waiter |
| join | 14 | Full session **6**, campaign **2**; API join while full |
| waiter | 15 | Waitlist **4**, position **1** |
| leave | 16 | Waitlist **5**, position **2** |
| checkin | 17 | Appointment **10**, arrival slot **7** |
| reverse | 18 | Appointment **11**, already checked in on slot **7** |
| noshow | 19 | Appointment **12**, overdue slot **8** |
| history | 20 | Appointments **13** cancelled, **14** no-show, **15** completed |
| ineligible | 21 | Try booking slot **4** to demonstrate eligibility rejection |
| unverified | 22 | Try booking slot **4** to demonstrate verification requirement |
| staff | 9 | Center-manager login, operational supervisor at center **3** |
| reviewer | 10 | Healthcare officer used for synthetic assessment references |

The arrival slot starts at **09:54:56 Asia/Colombo on 9 October 2026**. The normal check-in window lasts approximately until **10:24:56**. If practising later, load a fresh dataset using the seed. Existing demo accounts and actions are retained; every fresh run prints new IDs.

### Suggested first practice

1. Log in with `mf03.book.20261009041456@example.test` and password `VivaDemo!2026`.
2. Open Book Appointment. Select MF03 Viva Demo Campaign and MF03 Viva Demo Center.
3. Search from 9 October to 16 October 2026, then book slot 4.
4. Open My Appointments and show the new CONFIRMED record.
5. Sign out before changing to another scenario account.

### Staff practice

Log in with `mf03.staff.20261009041456@example.test`. In Check-In Management, load slot **7**: appointment **10** is ready for check-in and **11** for reversal. Load slot **8** for appointment **12**, ready for no-show processing.

Verification performed: authenticated availability search returned slots 4 and 5; citizen appointment lists returned the expected seeded statuses; waitlist status returned positions 1 and 2; staff queue for slot 7 returned both arrival records. These were read checks, leaving the demonstrations available to perform yourself.
