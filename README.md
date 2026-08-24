# Digital Subsidy & Grant Administration Platform

> **Current Phase: Architecture & Project Structure Setup**
> Business modules are not yet implemented.

---

## Overview

An enterprise platform for managing government subsidy and grant schemes. The system supports the complete lifecycle of a subsidy application — from beneficiary registration through multi-level verification, staged fund disbursement, compliance tracking, and regional analytics reporting.

## System Architecture

The platform is structured into five architectural modules:

| Module | Description | Technology |
|--------|-------------|-----------|
| **1. Beneficiary & Scheme Master Data** | Registration, document upload, identity validation, scheme rules | Spring Boot REST + JPA/Hibernate |
| **2. Eligibility Scoring & Verification Workflow** | Automated scoring, multi-level routing (Field → District → Finance), escalation | Spring Boot Service Layer + Rules Logic |
| **3. Staged Disbursement & Compliance Tracking** | Milestone-based fund release, compliance flags, reminders | Spring Boot Scheduler (Cron) + JPA |
| **4. Fund Utilization & Regional Analytics** | Scheme/region dashboards, PDF/Excel report export | Apache PDFBox / Apache POI |
| **5. Security, Integration & Deployment** | RBAC, audit logging, REST API, enterprise deployment | Spring Security + JWT Auth |

## User Roles

| Role | Responsibility |
|------|---------------|
| `FIELD_OFFICER` | Ground verification |
| `DISTRICT_OFFICER` | Review & escalation |
| `FINANCE_APPROVER` | Fund release approval |
| `ADMINISTRATOR` | System & scheme setup |

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3.2 |
| ORM | Hibernate / Spring Data JPA |
| Database | MySQL (`digital_subsidy_grant`) |
| Security | Spring Security + JWT (JJWT 0.12.5) |
| Build | Maven |
| Frontend (planned) | HTML, CSS, JavaScript |
| Reporting (planned) | Apache PDFBox, Apache POI |
| External APIs | REST over HTTPS/JSON |

## Project Structure

```
digital-subsidy-grant-platform/
│
├── src/
│   ├── main/
│   │   ├── java/com/dsgp/
│   │   │   ├── DigitalSubsidyGrantPlatformApplication.java
│   │   │   ├── config/          # Application-wide configuration
│   │   │   ├── security/        # Spring Security + JWT foundation
│   │   │   ├── beneficiary/     # Beneficiary registration module
│   │   │   ├── scheme/          # Scheme master data module
│   │   │   ├── eligibility/     # Eligibility scoring module
│   │   │   ├── verification/    # Verification workflow module
│   │   │   ├── disbursement/    # Staged disbursement module
│   │   │   ├── compliance/      # Compliance tracking module
│   │   │   ├── analytics/       # Fund utilization & analytics module
│   │   │   ├── audit/           # Audit log module
│   │   │   ├── document/        # Document management module
│   │   │   └── integration/
│   │   │       ├── treasury/    # Treasury System integration
│   │   │       └── beneficiary/ # Beneficiary Database integration
│   │   └── resources/
│   │       └── application.properties
│   └── test/java/com/dsgp/
│
├── frontend/                    # Web portal (HTML/CSS/JS) — planned
│   ├── css/
│   ├── js/
│   ├── pages/
│   └── assets/
│
├── docs/
│   └── architecture/            # Architecture documentation
│
├── pom.xml
├── README.md
└── .gitignore
```

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8.0+ (or compatible)

### Database Setup

Create the MySQL database before starting the application:

```sql
CREATE DATABASE digital_subsidy_grant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dsgp_user'@'localhost' IDENTIFIED BY '<your-password>';
GRANT ALL PRIVILEGES ON digital_subsidy_grant.* TO 'dsgp_user'@'localhost';
FLUSH PRIVILEGES;
```

### Configuration

Set the following environment variables (do **not** hard-code credentials):

```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=digital_subsidy_grant
DB_USERNAME=dsgp_user
DB_PASSWORD=<your-secure-password>
JWT_SECRET=<base64-encoded-256-bit-secret>
```

### Build & Run

```bash
# Compile and run tests (uses H2 in-memory DB — no MySQL required for tests)
mvn clean test

# Run the application (requires MySQL)
mvn spring-boot:run
```

The API will be available at: `http://localhost:8080/api/v1`

## Implementation Phases

- [x] **Phase 0** — Architecture & Project Structure Setup *(current)*
- [ ] **Phase 1** — Beneficiary & Scheme Master Data Module
- [ ] **Phase 2** — Eligibility Scoring & Verification Workflow
- [ ] **Phase 3** — Staged Disbursement & Compliance Tracking
- [ ] **Phase 4** — Fund Utilization & Regional Analytics
- [ ] **Phase 5** — Security, Integration & Enterprise Deployment

---

*This project follows a feature-based package architecture aligned with the system architecture diagram.*