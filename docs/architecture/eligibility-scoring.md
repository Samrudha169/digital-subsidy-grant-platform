# Eligibility Scoring Architecture
**Digital Subsidy & Grant Administration Platform (DSGP)**
*Milestone 1 — Architecture Design*

---

## 1. Overview

The Eligibility Scoring module evaluates whether a registered beneficiary qualifies for a given government scheme. Scoring is performed automatically by the platform's rules engine when a beneficiary submits an application or when a Field Officer triggers an eligibility check.

The result is a structured `EligibilityResult` that indicates whether the beneficiary **passes** or **fails** eligibility for that scheme, along with a numeric score and per-criterion breakdown for transparency and auditability.

---

## 2. Eligibility Criteria

Each scheme (`Scheme` entity) defines its own eligibility thresholds. The scoring engine evaluates the following criteria against the beneficiary's registered profile:

| Criterion | Scheme Field | Beneficiary Field | Logic |
|-----------|-------------|-------------------|-------|
| **Age Range** | `minAge`, `maxAge` | `age` / `dateOfBirth` | Beneficiary age must fall within `[minAge, maxAge]` |
| **Annual Income** | `maxAnnualIncome` | `annualIncome` | Beneficiary income must be ≤ scheme threshold |
| **Land Holding** | `maxLandHolding` | `landHolding` | Beneficiary land holding must be ≤ scheme threshold |
| **Social Category** | `requiredCategory` | `category` | If scheme specifies a required category, beneficiary must match (or be a sub-category where applicable) |
| **Identity Verified** | *(system rule)* | `identityVerified` | Beneficiary must have `identityVerified = true` before eligibility is confirmed |
| **Registration Status** | *(system rule)* | `registrationStatus` | Beneficiary must be in `ACTIVE` status |

---

## 3. Scoring Approach

The scoring engine uses a **weighted point system**. Each criterion contributes a defined number of points to a total eligibility score out of 100.

### 3.1 Score Weights

| Criterion | Max Points | Condition for Full Points |
|-----------|-----------|--------------------------|
| Age Range | 20 | Age falls within scheme's `[minAge, maxAge]` range |
| Annual Income | 30 | Income ≤ `maxAnnualIncome` |
| Land Holding | 20 | Land holding ≤ `maxLandHolding` |
| Category Match | 20 | Beneficiary category matches `requiredCategory` (or scheme has no category restriction) |
| Identity Verified | 10 | `identityVerified = true` |
| **Total** | **100** | |

> **Note:** If a scheme does not define a particular threshold (e.g., `maxAge` is `null`), that criterion is considered automatically satisfied and its full points are awarded.

### 3.2 Scoring Logic (Pseudocode)

```
score = 0

// Age check
if scheme.minAge == null AND scheme.maxAge == null:
    score += 20
else if beneficiary.age >= scheme.minAge AND beneficiary.age <= scheme.maxAge:
    score += 20
else:
    score += 0   // Fail this criterion

// Income check
if scheme.maxAnnualIncome == null:
    score += 30
else if beneficiary.annualIncome <= scheme.maxAnnualIncome:
    score += 30
else:
    score += 0

// Land holding check
if scheme.maxLandHolding == null:
    score += 20
else if beneficiary.landHolding <= scheme.maxLandHolding:
    score += 20
else:
    score += 0

// Category check
if scheme.requiredCategory == null:
    score += 20
else if beneficiary.category == scheme.requiredCategory:
    score += 20
else:
    score += 0

// Identity verification
if beneficiary.identityVerified == true:
    score += 10
else:
    score += 0
```

---

## 4. Pass / Fail Threshold

| Score | Outcome |
|-------|---------|
| **≥ 60** | **ELIGIBLE** — Application proceeds to verification workflow |
| **< 60** | **INELIGIBLE** — Application rejected at scoring stage; reason logged |

> The 60-point threshold is the system default. It may be overridden per scheme by an Administrator in a future release.

---

## 5. Eligibility Result Structure

The scoring engine produces an `EligibilityResult` response for each evaluation:

```json
{
  "beneficiaryId": 101,
  "schemeId": 5,
  "schemeName": "PM-KISAN Samman Nidhi",
  "totalScore": 80,
  "eligible": true,
  "criteria": {
    "ageCheck":      { "points": 20, "passed": true,  "detail": "Age 42 within range [18, 60]" },
    "incomeCheck":   { "points": 30, "passed": true,  "detail": "Income ₹85,000 ≤ threshold ₹1,50,000" },
    "landCheck":     { "points": 20, "passed": true,  "detail": "Land 1.5 ac ≤ threshold 2.0 ac" },
    "categoryCheck": { "points": 10, "passed": false, "detail": "Category GENERAL; required SC/ST" },
    "identityCheck": { "points": 0,  "passed": false, "detail": "Identity not yet verified" }
  },
  "evaluatedAt": "2025-04-15T10:32:00"
}
```

---

## 6. Integration Points

```
Beneficiary Profile ──► Eligibility Scoring Engine ──► EligibilityResult
        │                          │
   Scheme Rules            (if ELIGIBLE)
                                   │
                            Verification Workflow
```

- **Input:** `beneficiaryId` + `schemeId`
- **Output:** `EligibilityResult` (stored in `eligibility_results` table — Milestone 2)
- **Trigger:** Application submission by beneficiary or manual check by Field Officer
- **Downstream:** A PASS result triggers the verification workflow (see `verification-workflow.md`)

---

## 7. Planned Implementation

> **Phase:** Architecture design (Milestone 1). Implementation in Milestone 2.

| Component | Class | Package |
|-----------|-------|---------|
| Scoring service interface | `EligibilityService` | `com.dsgp.eligibility.service` |
| Scoring engine implementation | `EligibilityScoringEngine` | `com.dsgp.eligibility.service` |
| Result entity | `EligibilityResult` | `com.dsgp.eligibility.entity` |
| Result DTO | `EligibilityResultResponse` | `com.dsgp.eligibility.dto` |
| REST endpoint | `EligibilityController` | `com.dsgp.eligibility.controller` |
| Repository | `EligibilityResultRepository` | `com.dsgp.eligibility.repository` |
