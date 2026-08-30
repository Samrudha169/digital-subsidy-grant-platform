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