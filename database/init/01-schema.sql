<<<<<<< HEAD
-- =============================================================================
-- Digital Subsidy & Grant Administration Platform — Initial Schema
-- =============================================================================
-- Table: beneficiary
-- Owner: GovernmentSchemeApplication (console CLI — teammate's original work)
-- DO NOT DROP OR ALTER this table.
-- =============================================================================
CREATE TABLE IF NOT EXISTS beneficiary (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(100)  NOT NULL,
    gov_id      VARCHAR(20)   NOT NULL,
    contact     VARCHAR(10)   NOT NULL,
    email       VARCHAR(150)  NOT NULL,
    age         INT           NOT NULL,
    address     VARCHAR(255)  NOT NULL,
    scheme_name VARCHAR(150)  NOT NULL
);

-- =============================================================================
-- Table: beneficiaries
-- Owner: Spring Boot JPA — Beneficiary entity (Module 1)
-- NOTE: Hibernate ddl-auto=update will also create/update this table on startup.
--       This DDL is provided for documentation and fresh-environment reproducibility.
-- =============================================================================
CREATE TABLE IF NOT EXISTS beneficiaries (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    date_of_birth       DATE         NOT NULL,
    gender              VARCHAR(10)  NOT NULL,
    aadhaar_number      VARCHAR(12)  NOT NULL,
    mobile_number       VARCHAR(10)  NOT NULL,
    email               VARCHAR(150),
    address             TEXT,
    village             VARCHAR(100),
    taluka              VARCHAR(100),
    district            VARCHAR(100) NOT NULL,
    state               VARCHAR(100) NOT NULL,
    pin_code            VARCHAR(6),
    annual_income       DECIMAL(15,2),
    land_holding        DECIMAL(10,4),
    category            VARCHAR(20)  NOT NULL,
    registration_status VARCHAR(20)  NOT NULL,
    registration_date   DATETIME     NOT NULL,
    created_by          VARCHAR(100),
    updated_at          DATETIME,
    identity_verified   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_beneficiary_aadhaar UNIQUE (aadhaar_number),
    CONSTRAINT uk_beneficiary_mobile  UNIQUE (mobile_number)
);

-- =============================================================================
-- Table: beneficiary_documents
-- Owner: Spring Boot JPA — BeneficiaryDocument entity (Module 1)
-- =============================================================================
CREATE TABLE IF NOT EXISTS beneficiary_documents (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    beneficiary_id     BIGINT       NOT NULL,
    document_type      VARCHAR(30)  NOT NULL,
    file_name          VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path          VARCHAR(500) NOT NULL,
    file_size          BIGINT,
    mime_type          VARCHAR(100),
    uploaded_at        DATETIME     NOT NULL,
    uploaded_by        VARCHAR(100),
    verified           TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_doc_beneficiary
        FOREIGN KEY (beneficiary_id) REFERENCES beneficiaries (id)
        ON DELETE CASCADE
);
=======
CREATE TABLE IF NOT EXISTS beneficiary (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           full_name VARCHAR(100) NOT NULL,
    gov_id VARCHAR(20) NOT NULL UNIQUE,
    contact VARCHAR(15) NOT NULL,
    email VARCHAR(150) NOT NULL,
    age INT NOT NULL,
    address VARCHAR(255) NOT NULL,
    scheme_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
>>>>>>> origin/main
