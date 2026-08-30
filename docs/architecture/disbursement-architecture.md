# Disbursement Process Architecture
**Digital Subsidy & Grant Administration Platform (DSGP)**
*Milestone 1 — Architecture Design*

---

## 1. Overview

The Disbursement module manages the release of approved grant/subsidy funds to beneficiaries. Disbursement is milestone-based — funds are released in defined stages tied to verifiable conditions, rather than in a single lump sum. This staged approach reduces fraud risk, enables compliance tracking, and provides a clean audit trail for government accountability.

---

## 2. Staged / Milestone-Based Release

Each scheme defines one or more **disbursement milestones**. A milestone represents a specific stage at which a portion of the approved grant is released:

| Stage | Example (Agricultural Subsidy) | % Released |
|-------|-------------------------------|-----------|
| Stage 1 — Initial | Application APPROVED; identity confirmed | 30% |
| Stage 2 — Mid-term | Field Officer confirms crop sowing / project commencement | 40% |
| Stage 3 — Final | Beneficiary submits utilisation certificate; compliance verified | 30% |

> The number of stages and percentage split per stage are configurable per scheme by the Administrator.

### 2.1 Disbursement Record

Each disbursement stage release produces a `DisbursementRecord`:

```
DisbursementRecord {
    id
    schemeApplicationId   → FK to scheme_applications (APPROVED application)
    stage                 → INTEGER (1, 2, 3 ...)
    milestoneDescription  → "Crop sowing confirmed by Field Officer"
    amountDue             → DECIMAL — computed from scheme.grantAmount × stagePercentage
    amountReleased        → DECIMAL — actual amount sent to treasury
    disbursementStatus    → PENDING | TRIGGERED | CONFIRMED | FAILED
    triggeredAt           → DATETIME — when release was initiated
    confirmedAt           → DATETIME — when treasury confirmed payment
    treasuryReferenceNo   → VARCHAR — reference from Treasury System
    remarks               → VARCHAR — optional notes
}
```

---

## 3. Approval Flow

```
[Application: APPROVED]
        │
        ▼
Finance Approver triggers Stage 1 disbursement
        │
        ▼
[DisbursementRecord created: status = PENDING]
        │
        ▼
Platform calls Treasury System API
        │
   ┌────┴─────────┐
   │              │
SUCCESS        FAILURE
   │              │
status=TRIGGERED  status=FAILED → retry / alert
   │
   ▼
Treasury confirms payment (callback / polling)
   │
   ▼
[DisbursementRecord: status = CONFIRMED]
   │
   ▼
Next stage becomes available for triggering
(Field Officer confirms compliance condition)
```

---

## 4. Disbursement Triggers

| Stage | Who Triggers | Precondition |
|-------|-------------|-------------|
| Stage 1 | Finance Approver (manual) | Application `APPROVED` |
| Stage 2+ | Field Officer (manual) or System (scheduled) | Compliance record for previous stage confirmed |
| Final stage | Finance Approver | Utilisation certificate uploaded and verified |

For automated triggers, a scheduled job (Spring `@Scheduled` / cron) checks for pending milestone conditions daily and auto-triggers if all conditions are met and no manual hold is set.

---

## 5. Treasury Integration Points

Disbursement integrates with the external **Treasury System** for actual fund transfer. The integration is outbound REST/HTTPS:

```
DSGP Platform ──POST──► Treasury System API
                              │
                    Treasury validates & initiates payment
                              │
              ◄──── Callback (webhook) or polling response ────►
                    Treasury confirms / rejects payment
```

**Outbound request payload (conceptual):**
```json
{
  "referenceId":       "DSGP-APP-1001-STAGE-1",
  "beneficiaryName":   "Ravi Kumar",
  "aadhaarNumber":     "XXXX-XXXX-1234",
  "bankAccountNumber": "1234567890",
  "bankIfsc":          "SBIN0001234",
  "amount":            15000.00,
  "currency":          "INR",
  "schemeCode":        "PM-KISAN-2025",
  "remarkDescription": "Stage 1 disbursement — PM-KISAN Samman Nidhi"
}
```

See [`integration-design.md`](integration-design.md) for the full Treasury integration specification.

---

## 6. Payment Confirmation Concept

After funds are triggered:
1. **Treasury acknowledges** the request with a `treasuryReferenceNo`
2. Platform sets `disbursementStatus = TRIGGERED`
3. Treasury processes the bank transfer
4. Treasury sends a **confirmation callback** (webhook `POST /api/v1/disbursements/treasury-callback`) with success/failure status
5. Platform updates `disbursementStatus = CONFIRMED` (or `FAILED`)
6. `confirmedAt` is recorded
7. Audit log entry created
8. Compliance tracking module is notified to mark this stage as complete

**Failure handling:**
- On `FAILED` status: alert sent to Finance Approver
- Up to 3 automatic retries with exponential back-off (configurable)
- After max retries: manual intervention required; status set to `FAILED` permanently

---

## 7. Compliance Linkage

After a disbursement is `CONFIRMED`, the Compliance module creates a compliance obligation for the next stage:
- Beneficiary must submit a utilisation certificate within `N` days (configurable per scheme)
- Field Officer must confirm the milestone condition (e.g., crop sowed, project started)
- Non-compliance triggers a flag and pauses future disbursements

---

## 8. Planned Implementation

> **Phase:** Architecture design (Milestone 1). Implementation in Milestone 3.

| Component | Class | Package |
|-----------|-------|---------|
| Disbursement service | `DisbursementService` | `com.dsgp.disbursement.service` |
| Disbursement record entity | `DisbursementRecord` | `com.dsgp.disbursement.entity` |
| Repository | `DisbursementRepository` | `com.dsgp.disbursement.repository` |
| DTOs | `DisbursementTriggerRequest`, `DisbursementStatusResponse` | `com.dsgp.disbursement.dto` |
| REST endpoints | `DisbursementController` | `com.dsgp.disbursement.controller` |
| Treasury callback endpoint | included in `DisbursementController` | — |
| Scheduled job | `DisbursementScheduler` | `com.dsgp.disbursement.service` |
