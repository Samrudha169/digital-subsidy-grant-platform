-- =============================================================================
-- Digital Subsidy & Grant Administration Platform
-- Milestone 1 — Database Schema
-- =============================================================================


-- =============================================================================
-- TABLE: beneficiary
-- Basic beneficiary registration information
-- =============================================================================
CREATE TABLE IF NOT EXISTS beneficiary (
                                           id          INT AUTO_INCREMENT PRIMARY KEY,
                                           full_name   VARCHAR(100) NOT NULL,
    gov_id      VARCHAR(20) NOT NULL,
    contact     VARCHAR(10) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    age         INT NOT NULL,
    address     VARCHAR(255) NOT NULL,
    scheme_name VARCHAR(150) NOT NULL
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