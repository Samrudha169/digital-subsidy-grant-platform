# Regional Hierarchy Structure
**Digital Subsidy & Grant Administration Platform (DSGP)**
*Milestone 1 — Architecture Design*

---

## 1. Overview

The DSGP platform is designed to operate across India's administrative hierarchy. Regional structure determines:
- How user roles are scoped (a Field Officer is assigned to a specific district)
- How beneficiary records are organised and searchable
- How analytics and reports are aggregated (village → block → district → state → national)
- How escalations are routed (Field Officer in district X escalates to District Officer of district X)

---

## 2. Regional Hierarchy Levels

```
National (Central Government)
    └── State (e.g., Maharashtra, Karnataka)
            └── District (e.g., Pune, Mysuru)
                    └── Block / Taluka (e.g., Haveli, Hunsur)
                            └── Village / Gram Panchayat
```

| Level | Description | Example |
|-------|-------------|---------|
| **National** | Central government oversight; policy and scheme definition | Ministry of Agriculture |
| **State** | State government nodal agency; state-level scheme management | Directorate of Agriculture, Maharashtra |
| **District** | District-level administration; primary verification unit | Pune District |
| **Block / Taluka** | Sub-district administrative unit | Haveli Taluka |
| **Village / Gram Panchayat** | Ground-level unit where beneficiaries reside | Uruli Kanchan |

---

## 3. Role Assignments by Regional Level

| Role | Regional Scope | Responsibilities |
|------|---------------|-----------------|
| **ADMINISTRATOR** | National / State | Scheme creation and configuration, user management, system oversight, reporting at all levels |
| **FINANCE_APPROVER** | State / District | Final approval of verified applications; triggers disbursement; reviews escalated cases at finance level |
| **DISTRICT_OFFICER** | District | Reviews escalations from Field Officers; approves or rejects escalated applications; manages Field Officer assignments within the district |
| **FIELD_OFFICER** | Block / Village | Ground-level identity verification; document review; physical condition confirmation; initial approval or escalation of applications |

### 3.1 Role-to-Region Binding

Each user account is assigned:
- A **role** (one of the four above)
- A **regional scope** (e.g., `district = "Pune"`, `state = "Maharashtra"`)

A Field Officer assigned to `district = "Pune"` can only view and act on beneficiaries and applications where `beneficiary.district = "Pune"`.

A District Officer assigned to `district = "Pune"` sees all Field Officer escalations from that district.

A Finance Approver assigned to `state = "Maharashtra"` sees all district-approved applications across that state.

An Administrator has national-level access with no regional restriction, or may be scoped to a single state.

---

## 4. Beneficiary Address Model

The `Beneficiary` entity captures structured address components that map directly to the regional hierarchy:

| Beneficiary Field | Regional Level |
|------------------|---------------|
| `state` | State |
| `district` | District |
| `taluka` | Block / Taluka |
| `village` | Village / Gram Panchayat |
| `pinCode` | Postal code (cross-reference) |

This structured model enables:
- Filtering beneficiaries by district/state for a Field or District Officer
- Aggregating application counts and disbursement values by region for analytics
- Routing Field Officer assignments based on `beneficiary.district`

---

## 5. Scheme Regional Budget Allocation

Schemes may have regional budget caps (implemented in later milestones):

```
Scheme
 └── SchemeBudget (National total)
        └── StateBudgetAllocation (per state)
                └── DistrictBudgetAllocation (per district, optional)
```

> **Milestone 1 scope:** Regional budget allocation is **designed** here. Implementation is in Milestone 4.

---

## 6. Analytics Aggregation by Region

The analytics module will support reporting at all hierarchy levels:

| Report | Aggregation Level |
|--------|-----------------|
| National scheme dashboard | National |
| State disbursement summary | State |
| District application status | District |
| Block-level beneficiary coverage | Block |
| Village-level beneficiary list | Village |

---

## 7. Planned Implementation

> **Phase:** Architecture design (Milestone 1). Regional scoping of user accounts implemented in Milestone 5 (Security & RBAC).

| Component | Description | Module |
|-----------|-------------|--------|
| `UserAccount` entity | Stores role + regional scope (district, state) | `com.dsgp.security` (Milestone 5) |
| Region-scoped queries | `BeneficiaryRepository.findByDistrict()`, `findByState()` | `com.dsgp.beneficiary.repository` (Milestone 2) |
| Regional analytics queries | Aggregation by state/district | `com.dsgp.analytics.service` (Milestone 4) |
| Budget allocation entity | `SchemeBudgetAllocation` | `com.dsgp.scheme.entity` (Milestone 4) |
