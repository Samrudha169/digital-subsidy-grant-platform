# Integration Design Specification
**Digital Subsidy & Grant Administration Platform (DSGP)**
*Milestone 1 — Architecture Design*

---

## 1. Overview

The DSGP platform integrates with two external government systems:

| System | Direction | Purpose |
|--------|-----------|---------|
| **Treasury System** | Outbound (DSGP → Treasury) | Initiate fund transfers for approved disbursements; receive payment confirmations |
| **External Beneficiary Database** | Outbound (DSGP → Ext DB) | Cross-validate beneficiary identity and eligibility data against a central government registry |

Both integrations communicate over **REST/HTTPS with JSON payloads**. Authentication uses **API Key + HMAC signature** for government system security requirements.

---

## 2. Treasury System Integration

### 2.1 Purpose

When a Finance Approver triggers a disbursement stage, the DSGP platform calls the Treasury System to initiate the actual bank transfer to the beneficiary's account. The Treasury System manages all fund movement; DSGP tracks disbursement status against the Treasury's reference numbers.

### 2.2 Integration Direction

```
DSGP Platform ──HTTPS POST──► Treasury System
                                    │
                           Treasury processes payment
                                    │
              DSGP Platform ◄── Callback / Polling ── Treasury System
```

### 2.3 Configuration

```properties
# application.properties (Milestone 5 — not yet implemented)
integration.treasury.base-url=https://treasury.gov.in/api/v2
integration.treasury.api-key=${TREASURY_API_KEY}
integration.treasury.timeout-ms=15000
integration.treasury.callback-url=https://dsgp.gov.in/api/v1/disbursements/treasury-callback
```

### 2.4 Outbound — Payment Initiation

**Endpoint:** `POST {treasury.base-url}/payments/initiate`

**Authentication:**
```
X-API-Key: <treasury_api_key>
X-Signature: HMAC-SHA256(<requestBody>, <sharedSecret>)
X-Timestamp: 2025-04-15T10:32:00Z
```

**Request payload:**
```json
{
  "referenceId":         "DSGP-APP-101-STAGE-1",
  "beneficiaryName":     "Ravi Kumar",
  "aadhaarNumber":       "1234-5678-9012",
  "bankAccountNumber":   "1234567890",
  "bankIfsc":            "SBIN0001234",
  "amount":              6000.00,
  "currency":            "INR",
  "schemeCode":          "PMKISAN-2025",
  "stageNumber":         1,
  "remarkDescription":   "Stage 1 disbursement — PM-KISAN Samman Nidhi",
  "callbackUrl":         "https://dsgp.gov.in/api/v1/disbursements/treasury-callback"
}
```

**Response (202 Accepted):**
```json
{
  "treasuryReferenceNo": "TRY-20250415-00456",
  "status":              "QUEUED",
  "estimatedProcessingTime": "2025-04-15T14:00:00Z"
}
```

**Error responses:**

| HTTP Status | Meaning |
|-------------|---------|
| `400 Bad Request` | Invalid payload (missing fields, bad account number) |
| `401 Unauthorized` | API key invalid or signature mismatch |
| `409 Conflict` | Duplicate `referenceId` — payment already initiated |
| `503 Service Unavailable` | Treasury system temporarily unavailable |

### 2.5 Inbound — Payment Confirmation Callback

Treasury calls back to DSGP when payment is processed:

**Endpoint (DSGP receives):** `POST /api/v1/disbursements/treasury-callback`

**Request (from Treasury):**
```json
{
  "referenceId":         "DSGP-APP-101-STAGE-1",
  "treasuryReferenceNo": "TRY-20250415-00456",
  "status":              "SUCCESS",
  "processedAt":         "2025-04-15T13:45:00Z",
  "bankTransactionId":   "NEFT20250415009876",
  "remarks":             "Payment credited to account 1234567890"
}
```

Status values from Treasury: `SUCCESS`, `FAILED`, `PENDING`

**DSGP action on receipt:**
- `SUCCESS` → Set `DisbursementRecord.disbursementStatus = CONFIRMED`; record `confirmedAt`, `treasuryReferenceNo`, `bankTransactionId`; notify compliance module
- `FAILED` → Set `disbursementStatus = FAILED`; alert Finance Approver; schedule retry if within retry limit
- `PENDING` → No action; continue polling

### 2.6 Polling Fallback

If Treasury's callback is not received within 30 minutes, DSGP polls:

**Endpoint:** `GET {treasury.base-url}/payments/status/{treasuryReferenceNo}`

**Response:**
```json
{
  "treasuryReferenceNo": "TRY-20250415-00456",
  "status": "SUCCESS",
  "processedAt": "2025-04-15T13:45:00Z"
}
```

Polling interval: every 15 minutes, up to 3 retries, then alert.

### 2.7 Retry and Error Handling

| Scenario | Action |
|----------|--------|
| `503` from Treasury | Retry after 5 min, 15 min, 60 min (exponential back-off) |
| `FAILED` status from callback | Alert Finance Approver; mark `FAILED`; require manual resubmission |
| Callback not received in 30 min | Switch to polling |
| Max retries exhausted | Set `FAILED`; create alert; require Finance Approver manual intervention |

### 2.8 Reconciliation

At end-of-day, DSGP runs a reconciliation job:
1. Fetches all disbursements with status `TRIGGERED` older than 24 hours
2. Polls Treasury for each
3. Updates status and logs discrepancies
4. Generates a daily reconciliation report for Finance Approvers

---

## 3. External Beneficiary Database Integration

### 3.1 Purpose

Before or during eligibility scoring, DSGP cross-validates the beneficiary's Aadhaar number, income, and land holding against India's central **Beneficiary Registry** (analogous to PM-KISAN Beneficiary DB, Pradhan Mantri Jan Dhan Yojana DB, or state Aadhaar-seeded land records). This prevents duplicate registration and ensures data accuracy.

### 3.2 Integration Direction

```
DSGP Platform ──HTTPS POST──► External Beneficiary DB
                                        │
                              Returns validated beneficiary profile
                                        │
              DSGP Platform ◄──────────────────────────────
```

### 3.3 Configuration

```properties
# application.properties (Milestone 5 — not yet implemented)
integration.beneficiary-db.base-url=https://beneficiary-registry.gov.in/api/v1
integration.beneficiary-db.api-key=${BENEFICIARY_DB_API_KEY}
integration.beneficiary-db.timeout-ms=10000
```

### 3.4 Outbound — Identity Validation Request

**Endpoint:** `POST {beneficiary-db.base-url}/validate`

**Authentication:**
```
X-API-Key: <beneficiary_db_api_key>
X-Timestamp: 2025-04-15T10:32:00Z
```

**Request payload:**
```json
{
  "aadhaarNumber": "1234-5678-9012",
  "fullName":      "Ravi Kumar",
  "dateOfBirth":   "1986-03-15",
  "state":         "Maharashtra"
}
```

**Response (200 OK):**
```json
{
  "aadhaarNumber":    "1234-5678-9012",
  "validationStatus": "VALID",
  "nameMatch":        true,
  "dobMatch":         true,
  "annualIncome":     85000.00,
  "landHolding":      1.5,
  "category":         "OBC",
  "existingSchemes":  ["PM-AWAS-YOJANA"],
  "blacklisted":      false,
  "validatedAt":      "2025-04-15T10:32:05Z"
}
```

`validationStatus` values: `VALID`, `NOT_FOUND`, `MISMATCH`, `BLACKLISTED`

### 3.5 Data Usage

| Field from External DB | Usage in DSGP |
|-----------------------|---------------|
| `nameMatch`, `dobMatch` | Confirm identity before setting `identityVerified = true` |
| `annualIncome` | Cross-validate against beneficiary's self-declared income |
| `landHolding` | Cross-validate against self-declared land holding |
| `category` | Confirm social category for eligibility scoring |
| `existingSchemes` | Detect if beneficiary is already receiving a conflicting scheme |
| `blacklisted` | Block registration if `true` |

### 3.6 Error Handling

| Scenario | Action |
|----------|--------|
| `NOT_FOUND` | Flag beneficiary for manual verification; do not auto-reject |
| `MISMATCH` | Record discrepancy; Field Officer must resolve manually |
| `BLACKLISTED` | Block registration; raise alert to Administrator |
| `503` / timeout | Fall back to manual verification; log for retry |
| API quota exceeded | Queue validation request; retry in next batch |

### 3.7 Privacy and Data Handling

- Aadhaar numbers are **never stored in plain text** in application logs
- API calls to the external DB are logged with masked Aadhaar (show only last 4 digits: `XXXX-XXXX-9012`)
- External DB responses are cached for 24 hours to avoid repeated calls during the same application window
- All external calls are audited in the `audit_log` table

---

## 4. Planned Implementation

> **Phase:** Architecture design (Milestone 1). Implementation in Milestone 5 (Integration & Security).

| Component | Class | Package |
|-----------|-------|---------|
| Treasury REST client | `TreasuryClient` | `com.dsgp.integration.treasury.client` |
| Treasury DTOs | `PaymentInitiationRequest`, `PaymentConfirmationResponse` | `com.dsgp.integration.treasury.dto` |
| Treasury config | `TreasuryIntegrationConfig` | `com.dsgp.integration.treasury.config` |
| Beneficiary DB client | `BeneficiaryDbClient` | `com.dsgp.integration.beneficiary.client` |
| Beneficiary DB DTOs | `BeneficiaryValidationRequest`, `BeneficiaryValidationResponse` | `com.dsgp.integration.beneficiary.dto` |
| Beneficiary DB config | `BeneficiaryDbIntegrationConfig` | `com.dsgp.integration.beneficiary.config` |
