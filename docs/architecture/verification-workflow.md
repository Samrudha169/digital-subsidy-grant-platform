# Verification Workflow Architecture
**Digital Subsidy & Grant Administration Platform (DSGP)**
*Milestone 1 — Architecture Design*

---

## 1. Overview

The Verification Workflow defines the multi-level review process that a scheme application must pass through before funds are released. After a beneficiary's application passes the eligibility scoring step, it enters this workflow and progresses through a structured chain of review stages, each performed by a different user role.

---

## 2. Roles Involved

| Role | Responsibilities |
|------|-----------------|
| **FIELD_OFFICER** | Ground-level verification — confirms beneficiary identity, documents, and physical conditions |
| **DISTRICT_OFFICER** | Second-level review — reviews Field Officer findings, approves or escalates to Finance |
| **FINANCE_APPROVER** | Final approval — authorises fund release or rejects at the financial level |
| **ADMINISTRATOR** | System oversight — can reassign, override, or reopen a workflow if required |

---

## 3. Verification Stages

```
[ELIGIBLE Beneficiary Application]
           │
           ▼
  ┌─────────────────────┐
  │  STAGE 1            │
  │  Field Officer      │ ← Assigned from regional pool based on beneficiary's district
  │  Verification       │
  └────────┬────────────┘
           │
     ┌─────┴──────────┐
     │                │
  APPROVE          REJECT / ESCALATE
     │                │
     │          ┌─────┴──────────────┐
     │          │  STAGE 2 (if       │
     │          │  ESCALATED)        │
     │          │  District Officer  │
     │          │  Review            │
     │          └─────┬──────────────┘
     │                │
     │          ┌─────┴──────┐
     │          │            │
     │       APPROVE      REJECT
     │          │
     ▼          ▼
  ┌─────────────────────┐
  │  STAGE 3            │
  │  Finance Approver   │ ← Final authorisation
  │  Approval           │
  └────────┬────────────┘
           │
     ┌─────┴──────┐
     │            │
  APPROVE      REJECT
     │
     ▼
[APPROVED — Eligible for Disbursement]
```

---

## 4. Application Status State Machine

| Status | Description | Who sets it |
|--------|-------------|-------------|
| `PENDING` | Submitted, awaiting Field Officer assignment | System (on submission) |
| `UNDER_REVIEW` | Assigned to a Field Officer | System (on assignment) |
| `FIELD_APPROVED` | Approved at Field Officer stage | FIELD_OFFICER |
| `ESCALATED` | Sent to District Officer for second review | FIELD_OFFICER |
| `DISTRICT_APPROVED` | Approved at District Officer stage | DISTRICT_OFFICER |
| `REJECTED` | Rejected at any stage (final state) | FIELD_OFFICER / DISTRICT_OFFICER / FINANCE_APPROVER |
| `APPROVED` | Approved by Finance Approver; eligible for disbursement | FINANCE_APPROVER |

### 4.1 Valid Transitions

```
PENDING        → UNDER_REVIEW
UNDER_REVIEW   → FIELD_APPROVED
UNDER_REVIEW   → ESCALATED
UNDER_REVIEW   → REJECTED
FIELD_APPROVED → APPROVED          (Finance Approver reviews)
FIELD_APPROVED → REJECTED
ESCALATED      → DISTRICT_APPROVED
ESCALATED      → REJECTED
DISTRICT_APPROVED → APPROVED
DISTRICT_APPROVED → REJECTED
```

> `REJECTED` and `APPROVED` are terminal states. An ADMINISTRATOR may reopen a rejected application by resetting it to `PENDING`.

---

## 5. Escalation Handling

A Field Officer escalates an application when:
- The beneficiary's documentation is incomplete but the field conditions justify consideration
- Conflicting eligibility factors require a higher authority decision
- The application value exceeds the Field Officer's authorisation limit (configurable per scheme)

Upon escalation:
1. Application status moves from `UNDER_REVIEW` → `ESCALATED`
2. The District Officer for the corresponding region is notified
3. An escalation reason is recorded in the audit log
4. The District Officer either approves (`DISTRICT_APPROVED`) or rejects (`REJECTED`)

---

## 6. Rejection Handling

Rejection at any stage is final unless an ADMINISTRATOR intervenes. When an application is rejected:
1. Status moves to `REJECTED`
2. The rejection reason is stored (mandatory field)
3. The beneficiary is notified (via future notification module)
4. The rejection event is recorded in the audit log
5. The beneficiary may re-apply for the same scheme after a configured cooling-off period (future milestone)

---

## 7. Verification Record

Each stage action produces a `VerificationRecord` stored in the `verification_records` table (Milestone 2 implementation):

```
VerificationRecord {
    id
    schemeApplicationId   → FK to scheme_applications
    stage                 → FIELD / DISTRICT / FINANCE
    actionTaken           → APPROVE / REJECT / ESCALATE
    performedBy           → username of officer
    performedAt           → timestamp
    remarks               → free-text reason / notes
    documentIds[]         → documents reviewed at this stage
}
```

---

## 8. SLA and Timeouts

| Stage | Target SLA | Action on Breach |
|-------|-----------|-----------------|
| Field Officer review | 5 working days | Auto-escalate to District Officer |
| District Officer review | 3 working days | Alert to Administrator |
| Finance Approver review | 2 working days | Alert to Administrator |

---

## 9. Planned Implementation

> **Phase:** Architecture design (Milestone 1). Implementation in Milestone 2.

| Component | Class | Package |
|-----------|-------|---------|
| Workflow service | `VerificationService` | `com.dsgp.verification.service` |
| Verification record entity | `VerificationRecord` | `com.dsgp.verification.entity` |
| Verification record repository | `VerificationRepository` | `com.dsgp.verification.repository` |
| DTOs | `VerificationActionRequest`, `VerificationStatusResponse` | `com.dsgp.verification.dto` |
| REST endpoints | `VerificationController` | `com.dsgp.verification.controller` |
