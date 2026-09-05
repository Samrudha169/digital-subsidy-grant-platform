-- =============================================================================
-- Digital Subsidy & Grant Administration Platform
-- Milestone 1 — Database Schema
-- =============================================================================


-- =============================================================================
-- TABLE: beneficiary
-- Full canonical beneficiary registration information (Milestone 1 model).
-- Original columns (full_name, gov_id, contact, email, age, address,
-- scheme_name) are preserved for backward compatibility.
-- Extended columns (aadhaar_number, mobile_number, first_name, last_name,
-- date_of_birth, gender, village, taluka, district, state, pin_code,
-- annual_income, land_holding, category, registration_status,
-- identity_verified) form the complete Milestone 1 data model.
-- =============================================================================
CREATE TABLE IF NOT EXISTS beneficiary (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    -- Original fields
    full_name           VARCHAR(100) NOT NULL,
    gov_id              VARCHAR(20)  NOT NULL,
    contact             VARCHAR(10)  NOT NULL,
    email               VARCHAR(150) NOT NULL,
    age                 INT          NOT NULL,
    address             VARCHAR(255) NOT NULL,
    scheme_name         VARCHAR(150) NOT NULL,
    -- Extended identity fields
    aadhaar_number      VARCHAR(12)  UNIQUE,
    mobile_number       VARCHAR(10)  UNIQUE,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    date_of_birth       DATE,
    gender              VARCHAR(10),
    -- Structured address components
    village             VARCHAR(150),
    taluka              VARCHAR(100),
    district            VARCHAR(100),
    state               VARCHAR(100),
    pin_code            VARCHAR(6),
    -- Financial eligibility fields
    annual_income       DECIMAL(15,2),
    land_holding        DECIMAL(10,4),
    -- Categorisation and lifecycle
    category            VARCHAR(10),
    registration_status VARCHAR(15)  NOT NULL DEFAULT 'PENDING',
    identity_verified   TINYINT(1)   NOT NULL DEFAULT 0
);


-- =============================================================================
-- TABLE: schemes
-- Government schemes and their eligibility criteria
-- =============================================================================
CREATE TABLE IF NOT EXISTS schemes (
                                       id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       scheme_name         VARCHAR(150) NOT NULL,
    description         TEXT,
    min_age             INT,
    max_age             INT,
    max_annual_income   DECIMAL(15,2),
    max_land_holding    DECIMAL(10,4),
    required_category   VARCHAR(20),
    grant_amount        DECIMAL(15,2),
    active              TINYINT(1) NOT NULL DEFAULT 1
    );


-- =============================================================================
-- TABLE: scheme_applications
-- Connects a beneficiary with a selected government scheme
-- =============================================================================
CREATE TABLE IF NOT EXISTS scheme_applications (
                                                   id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   beneficiary_id      INT NOT NULL,
                                                   scheme_id           BIGINT NOT NULL,
                                                   application_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    application_date    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_application_beneficiary
    FOREIGN KEY (beneficiary_id)
    REFERENCES beneficiary(id),

    CONSTRAINT fk_application_scheme
    FOREIGN KEY (scheme_id)
    REFERENCES schemes(id),

    CONSTRAINT uk_beneficiary_scheme
    UNIQUE (beneficiary_id, scheme_id)
    );


-- =============================================================================
-- TABLE: beneficiary_documents
-- Documents uploaded for beneficiary verification
-- =============================================================================
CREATE TABLE IF NOT EXISTS beneficiary_documents (
                                                     id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                     beneficiary_id     INT NOT NULL,
                                                     document_type      VARCHAR(30) NOT NULL,
    file_name          VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path          VARCHAR(500) NOT NULL,
    file_size          BIGINT,
    mime_type          VARCHAR(100),
    uploaded_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by        VARCHAR(100),
    verified           TINYINT(1) NOT NULL DEFAULT 0,

    CONSTRAINT fk_doc_beneficiary
    FOREIGN KEY (beneficiary_id)
    REFERENCES beneficiary(id)
    ON DELETE CASCADE
    );


-- =============================================================================
-- TABLE: eligibility_results                                [Milestone 2]
-- Stores the outcome of each eligibility evaluation run by the scoring engine.
-- One row per (beneficiary, scheme) pair — replaced on re-evaluation.
-- =============================================================================
CREATE TABLE IF NOT EXISTS eligibility_results (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    beneficiary_id      INT          NOT NULL,
    scheme_id           BIGINT       NOT NULL,
    scheme_name         VARCHAR(150) NOT NULL,
    total_score         INT          NOT NULL,
    eligibility_status  VARCHAR(15)  NOT NULL,   -- ELIGIBLE | INELIGIBLE
    criteria_json       TEXT,                    -- JSON breakdown per criterion
    evaluated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_eligibility_beneficiary
        FOREIGN KEY (beneficiary_id)
        REFERENCES beneficiary(id),

    CONSTRAINT fk_eligibility_scheme
        FOREIGN KEY (scheme_id)
        REFERENCES schemes(id),

    CONSTRAINT uq_eligibility_beneficiary_scheme
        UNIQUE (beneficiary_id, scheme_id)
);


-- =============================================================================
-- TABLE: verification_records                                [Milestone 2]
-- Stores one row per action taken by a verification officer on an application.
-- Provides a complete, immutable audit trail for the multi-level approval chain.
--
-- Stages:  FIELD | DISTRICT | FINANCE
-- Actions: APPROVE | REJECT | ESCALATE
--
-- Status transitions (stored in scheme_applications.application_status):
--   PENDING           → UNDER_REVIEW       (startVerification)
--   UNDER_REVIEW      → FIELD_APPROVED     (Field APPROVE)
--   UNDER_REVIEW      → ESCALATED          (Field ESCALATE)
--   UNDER_REVIEW      → REJECTED           (Field REJECT)
--   FIELD_APPROVED    → APPROVED           (Finance APPROVE)
--   FIELD_APPROVED    → REJECTED           (Finance REJECT)
--   ESCALATED         → DISTRICT_APPROVED  (District APPROVE)
--   ESCALATED         → REJECTED           (District REJECT)
--   DISTRICT_APPROVED → APPROVED           (Finance APPROVE)
--   DISTRICT_APPROVED → REJECTED           (Finance REJECT)
-- =============================================================================
CREATE TABLE IF NOT EXISTS verification_records (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_application_id   BIGINT       NOT NULL,
    stage                   VARCHAR(10)  NOT NULL,   -- FIELD | DISTRICT | FINANCE
    action_taken            VARCHAR(10)  NOT NULL,   -- APPROVE | REJECT | ESCALATE
    performed_by            VARCHAR(100) NOT NULL,
    performed_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks                 TEXT,

    CONSTRAINT fk_verification_application
        FOREIGN KEY (scheme_application_id)
        REFERENCES scheme_applications(id)
);