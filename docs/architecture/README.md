# Architecture Documentation

This directory contains architecture documentation for the Digital Subsidy & Grant Administration Platform (DSGP).

## Milestone 1 Documents (Complete)

| Document | Description |
|----------|-------------|
| [`eligibility-scoring.md`](eligibility-scoring.md) | Eligibility scoring criteria, weighted scoring algorithm, pass/fail threshold, and result structure |
| [`verification-workflow.md`](verification-workflow.md) | Multi-level verification workflow (Field → District → Finance), state machine, escalation, and SLA |
| [`disbursement-architecture.md`](disbursement-architecture.md) | Staged disbursement model, approval flow, treasury integration points, payment confirmation |
| [`regional-hierarchy.md`](regional-hierarchy.md) | National–State–District–Block–Village hierarchy, role assignments, regional scoping |
| [`api-design.md`](api-design.md) | REST API specifications for Beneficiary, Scheme, Eligibility, Verification, and Disbursement modules |
| [`integration-design.md`](integration-design.md) | Treasury System and External Beneficiary Database integration plans |

## Planned for Later Milestones

| Document | Planned Phase |
|----------|--------------|
| `database-design.md` — Full ERD and schema documentation | Milestone 2 |
| `security-design.md` — JWT, RBAC, and security architecture | Milestone 5 |
| `deployment-guide.md` — Environment setup and deployment | Milestone 5 |

---

*Milestone 1 objective: Establish project architecture and disbursement framework.*
