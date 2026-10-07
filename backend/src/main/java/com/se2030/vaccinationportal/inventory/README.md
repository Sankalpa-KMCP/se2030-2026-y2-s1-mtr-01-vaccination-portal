# MF-04 batch boundary for MF-05

Inject `InventoryBatchPort` from `inventory.dependency`; do not access MF-04 repositories directly.

- `getBatchAvailability(centerId, batchId)` is read-only. It does not reserve or deduct stock. The returned snapshot includes the vaccine type ID, status, expiry date, quantity, and reason. MF-05 should compare `vaccineTypeId` with the campaign's vaccine type.
- `deductVaccinationDose(centerId, batchId, vaccinationRecordId)` is for finalization only. It revalidates under database locks, deducts exactly one dose, and inserts one unique `VACCINATION_DEDUCTION` audit transaction. It joins an existing transaction, so MF-05 should create the permanent vaccination record first and call this method inside the same finalization transaction. A failure rolls both operations back.
- V15 adds a foreign key from the audit transaction to `vaccination_records`; the record must exist by the time the deduction is flushed. The vaccination record ID is unique in `stock_transactions` to prevent duplicate deductions.

Inventory also owns manufacturer/batch management, stock validation, adjustments and auditing. Campaign center/vaccine references are obtained through `CampaignReferenceDataReader`; Screening and Vaccination must not access Inventory repositories. No vaccination draft operation changes stock.
