# API Design Specification
**Digital Subsidy & Grant Administration Platform (DSGP)**
*Milestone 1 — Architecture Design*

---

## 1. Overview

Base URL: `http://localhost:8080/api/v1`

All endpoints return JSON. All write operations require `Content-Type: application/json`.

### 1.1 Authentication

> **Milestone 1 status:** JWT authentication foundation is configured (`SecurityConfig.java`) but the JWT filter, `UserDetailsService`, and full RBAC are implemented in Milestone 5. During Milestone 1/2, all endpoints are temporarily open (`permitAll`) to enable development and testing.

**Planned auth header (Milestone 5+):**
```
Authorization: Bearer <JWT token>
```

### 1.2 Standard Error Response

All error responses use a consistent `ApiErrorResponse` structure:

```json
{
  "timestamp": "2025-04-15T10:32:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/beneficiaries",
  "fieldErrors": [
    { "field": "email", "message": "Email must be a valid address" }
  ]
}
```

---

## 2. Beneficiary API

**Base path:** `/beneficiaries`

---

### 2.1 Register Beneficiary

| | |
|---|---|
| **Method** | `POST` |
| **Path** | `/beneficiaries` |
| **Purpose** | Register a new beneficiary in the platform |
| **Auth** | FIELD_OFFICER, ADMINISTRATOR |

**Request body:**
```json
{
  "fullName":   "Ravi Kumar",
  "govId":      "AABCD1234E",
  "contact":    "9876543210",
  "email":      "ravi.kumar@example.com",
  "age":        38,
  "address":    "Village Uruli Kanchan, Pune",
  "schemeName": "PM-KISAN Samman Nidhi"
}
```

**Responses:**

| Status | Meaning |
|--------|---------|
| `201 Created` | Beneficiary registered successfully; returns `BeneficiaryResponse` |
| `400 Bad Request` | Validation failed (field errors in body) |
| `409 Conflict` | Duplicate Aadhaar/Gov ID or mobile number |

---

### 2.2 Get Beneficiary by ID

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/beneficiaries/{id}` |
| **Purpose** | Retrieve full beneficiary details by internal ID |
| **Auth** | FIELD_OFFICER, DISTRICT_OFFICER, FINANCE_APPROVER, ADMINISTRATOR |

**Path parameters:** `id` — integer, beneficiary primary key

**Response (200 OK):**
```json
{
  "id":         42,
  "fullName":   "Ravi Kumar",
  "govId":      "AABCD1234E",
  "contact":    "9876543210",
  "email":      "ravi.kumar@example.com",
  "age":        38,
  "address":    "Village Uruli Kanchan, Pune",
  "schemeName": "PM-KISAN Samman Nidhi"
}
```

| Status | Meaning |
|--------|---------|
| `200 OK` | Beneficiary found |
| `404 Not Found` | No beneficiary with the given ID |

---

### 2.3 Get Beneficiary by Government ID

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/beneficiaries/gov-id/{govId}` |
| **Purpose** | Look up a beneficiary by their government ID (Aadhaar / PAN) |
| **Auth** | FIELD_OFFICER, ADMINISTRATOR |

| Status | Meaning |
|--------|---------|
| `200 OK` | Returns `BeneficiaryResponse` |
| `404 Not Found` | No beneficiary with given gov ID |

---

### 2.4 List All Beneficiaries

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/beneficiaries` |
| **Purpose** | Retrieve all registered beneficiaries |
| **Auth** | DISTRICT_OFFICER, FINANCE_APPROVER, ADMINISTRATOR |

**Response (200 OK):** Array of `BeneficiaryResponse` objects.

---

### 2.5 Update Beneficiary

| | |
|---|---|
| **Method** | `PUT` |
| **Path** | `/beneficiaries/{id}` |
| **Purpose** | Update an existing beneficiary's details |
| **Auth** | FIELD_OFFICER, ADMINISTRATOR |

Request body: same structure as registration. All fields required.

| Status | Meaning |
|--------|---------|
| `200 OK` | Updated successfully |
| `400 Bad Request` | Validation failed |
| `404 Not Found` | Beneficiary not found |
| `409 Conflict` | Duplicate gov ID or mobile |

---

### 2.6 Delete Beneficiary

| | |
|---|---|
| **Method** | `DELETE` |
| **Path** | `/beneficiaries/{id}` |
| **Purpose** | Remove a beneficiary from the system |
| **Auth** | ADMINISTRATOR only |

| Status | Meaning |
|--------|---------|
| `204 No Content` | Deleted successfully |
| `404 Not Found` | Beneficiary not found |

---

## 3. Scheme API *(Planned — Milestone 2)*

**Base path:** `/schemes`

> The `Scheme` JPA entity and `SchemeRepository` are implemented. The REST controller is planned for Milestone 2.

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| `POST` | `/schemes` | Create a new scheme | ADMINISTRATOR |
| `GET` | `/schemes` | List all active schemes | All authenticated |
| `GET` | `/schemes/{id}` | Get scheme by ID | All authenticated |
| `PUT` | `/schemes/{id}` | Update scheme details | ADMINISTRATOR |
| `DELETE` | `/schemes/{id}` | Deactivate a scheme | ADMINISTRATOR |

**Scheme response structure:**
```json
{
  "id":               5,
  "schemeName":       "PM-KISAN Samman Nidhi",
  "description":      "Income support to farmer families",
  "minAge":           18,
  "maxAge":           60,
  "maxAnnualIncome":  150000.00,
  "maxLandHolding":   2.0,
  "requiredCategory": null,
  "grantAmount":      6000.00,
  "active":           true
}
```

---

## 4. Eligibility API *(Planned — Milestone 2)*

**Base path:** `/eligibility`

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| `POST` | `/eligibility/check` | Run eligibility check for a beneficiary + scheme | FIELD_OFFICER, ADMINISTRATOR |
| `GET` | `/eligibility/{beneficiaryId}` | Get all eligibility results for a beneficiary | FIELD_OFFICER, DISTRICT_OFFICER |
| `GET` | `/eligibility/{beneficiaryId}/scheme/{schemeId}` | Get eligibility result for specific application | All roles |

**Check request:**
```json
{
  "beneficiaryId": 42,
  "schemeId":      5
}
```

**Check response (200 OK):**
```json
{
  "beneficiaryId": 42,
  "schemeId":      5,
  "schemeName":    "PM-KISAN Samman Nidhi",
  "totalScore":    80,
  "eligible":      true,
  "criteria": {
    "ageCheck":      { "points": 20, "passed": true },
    "incomeCheck":   { "points": 30, "passed": true },
    "landCheck":     { "points": 20, "passed": true },
    "categoryCheck": { "points": 10, "passed": false },
    "identityCheck": { "points": 0,  "passed": false }
  },
  "evaluatedAt": "2025-04-15T10:32:00"
}
```

| Status | Meaning |
|--------|---------|
| `200 OK` | Eligibility evaluated; result in body |
| `404 Not Found` | Beneficiary or scheme not found |
| `409 Conflict` | Eligibility already checked for this combination |

---

## 5. Verification API *(Planned — Milestone 2)*

**Base path:** `/verification`

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| `GET` | `/verification/applications` | List applications pending this officer's action | FIELD_OFFICER, DISTRICT_OFFICER, FINANCE_APPROVER |
| `GET` | `/verification/applications/{applicationId}` | Get full application + verification history | All roles |
| `POST` | `/verification/applications/{applicationId}/approve` | Approve at current stage | FIELD_OFFICER, DISTRICT_OFFICER, FINANCE_APPROVER |
| `POST` | `/verification/applications/{applicationId}/reject` | Reject application | FIELD_OFFICER, DISTRICT_OFFICER, FINANCE_APPROVER |
| `POST` | `/verification/applications/{applicationId}/escalate` | Escalate to next level | FIELD_OFFICER |

**Action request body (approve / reject / escalate):**
```json
{
  "remarks": "Documents verified in person. Land holding confirmed at 1.2 acres."
}
```

**Application status response:**
```json
{
  "applicationId":     101,
  "beneficiaryId":     42,
  "schemeId":          5,
  "applicationStatus": "FIELD_APPROVED",
  "applicationDate":   "2025-04-10T09:15:00",
  "history": [
    {
      "stage":       "FIELD",
      "action":      "APPROVE",
      "performedBy": "officer_rajan",
      "performedAt": "2025-04-14T14:20:00",
      "remarks":     "All documents verified."
    }
  ]
}
```

| Status | Meaning |
|--------|---------|
| `200 OK` | Action performed |
| `400 Bad Request` | Invalid transition |
| `403 Forbidden` | Officer not authorised for this stage/region |
| `404 Not Found` | Application not found |

---

## 6. Disbursement API *(Planned — Milestone 3)*

**Base path:** `/disbursements`

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| `GET` | `/disbursements/applications/{applicationId}` | Get disbursement schedule for an application | FINANCE_APPROVER, ADMINISTRATOR |
| `POST` | `/disbursements/applications/{applicationId}/trigger/{stage}` | Trigger a disbursement stage | FINANCE_APPROVER |
| `GET` | `/disbursements/{disbursementId}/status` | Get disbursement status | All roles |
| `POST` | `/disbursements/treasury-callback` | Receive payment confirmation from Treasury | System (internal) |
