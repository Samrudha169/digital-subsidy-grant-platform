-- =============================================================================
-- Digital Subsidy & Grant Administration Platform
-- Milestone 2 — Scheme Master Data Seed
-- =============================================================================
-- Inserts the three project schemes in a fixed order so that their
-- auto-increment IDs are stable:
--   id=1  PM-KISAN
--   id=2  NSP
--   id=3  PMEGP
--
-- Eligibility criteria source:
--   PM-KISAN  → eligibility-scoring.md §5 (canonical project example)
--   NSP       → No numeric thresholds defined in project docs; NULL = any value
--               auto-passes that criterion (per eligibility-scoring.md §3.1)
--   PMEGP     → No numeric thresholds defined in project docs; NULL = any value
--               auto-passes that criterion (per eligibility-scoring.md §3.1)
--
-- Uses INSERT IGNORE so re-running this file on an already-seeded database
-- is safe and idempotent.
-- =============================================================================

INSERT IGNORE INTO schemes
    (id, scheme_name, description,
     min_age, max_age, max_annual_income, max_land_holding,
     required_category, grant_amount, active)
VALUES
-- ── PM-KISAN Samman Nidhi ────────────────────────────────────────────────────
-- Criteria: eligibility-scoring.md §5
--   Age 18–60, income ≤ ₹1,50,000, land ≤ 2.0 ac, category SC/ST, grant ₹6,000
(1,
 'PM-KISAN Samman Nidhi',
 'Pradhan Mantri Kisan Samman Nidhi — Income support scheme providing ₹6,000 per year in three equal instalments of ₹2,000 to eligible farmer families.',
 18, 70,
 300000.00,
 5.0000,
 NULL,
 6000.00,
 1),

-- ── National Scholarship Portal ──────────────────────────────────────────────
-- No numeric thresholds defined in project documentation.
-- NULL thresholds → criterion auto-passes (full points awarded).
(2,
 'National Scholarship Portal',
 'NSP — A unified platform for all government scholarship schemes, enabling eligible students across India to apply, track, and receive scholarships through Direct Benefit Transfer.',
 NULL, NULL,
 NULL,
 NULL,
 NULL,
 NULL,
 1),

-- ── PMEGP ────────────────────────────────────────────────────────────────────
-- No numeric thresholds defined in project documentation.
-- Minimum age 18 (stated in PMEGP.jsx eligibility section — individual applicants
-- must have attained 18 years). Other thresholds NULL → auto-pass.
(3,
 'Prime Minister''s Employment Generation Programme',
 'PMEGP — A major credit-linked subsidy scheme facilitating self-employment through establishment of micro-enterprises in manufacturing, service, and trading sectors.',
 18, NULL,
 NULL,
 NULL,
 NULL,
 NULL,
 1);
-- =============================================================================
-- Milestone 2 — Correct PM-KISAN seed to match EligibilityScoringEngine.java
-- =============================================================================
-- The INSERT IGNORE above will not update an already-seeded row.
-- This UPDATE brings existing rows in line with the canonical engine values:
--   age 18-70, income <= 300000, land <= 5 acres, category NOT mandatory.
-- Run this in sequence after INSERT IGNORE so the state is always consistent.
-- =============================================================================
UPDATE schemes
SET    max_age           = 70,
       max_annual_income = 300000.00,
       max_land_holding  = 5.0000,
       required_category = NULL
WHERE  id = 1
  AND  scheme_name = 'PM-KISAN Samman Nidhi';

-- =============================================================================
-- Default password for all seeded officers: password123
-- BCrypt hash (strength 10) of "password123":
--   $2a$10$1mjJmEBaJgGkXv7yoV5S/e3sRy26n.2RP9AN25lMuApnY1WLapn1a
-- (Verified: BCryptPasswordEncoder.matches("password123", hash) == true)
-- The original seed hash was incorrect and did not match "password123".
--
-- IMPORTANT: Change passwords before any real deployment.
-- Officers are seeded with INSERT IGNORE so this is safe to re-run.
-- =============================================================================

CREATE TABLE IF NOT EXISTS officers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(150),
    role        VARCHAR(20)  NOT NULL,
    district    VARCHAR(100),
    active      TINYINT(1)   NOT NULL DEFAULT 1
);

INSERT IGNORE INTO officers (id, username, password, full_name, email, role, district, active)
VALUES
-- Field Officers
(1, 'field.officer1', '$2a$10$1mjJmEBaJgGkXv7yoV5S/e3sRy26n.2RP9AN25lMuApnY1WLapn1a',
    'Ramesh Kumar',        'ramesh.kumar@dsgp.gov.in',    'FIELD_OFFICER',    'Pune',        1),
(2, 'field.officer2', '$2a$10$1mjJmEBaJgGkXv7yoV5S/e3sRy26n.2RP9AN25lMuApnY1WLapn1a',
    'Priya Sharma',        'priya.sharma@dsgp.gov.in',    'FIELD_OFFICER',    'Nashik',      1),

-- District Officers
(3, 'district.officer1', '$2a$10$1mjJmEBaJgGkXv7yoV5S/e3sRy26n.2RP9AN25lMuApnY1WLapn1a',
    'Anita Desai',         'anita.desai@dsgp.gov.in',     'DISTRICT_OFFICER', 'Pune',        1),
(4, 'district.officer2', '$2a$10$1mjJmEBaJgGkXv7yoV5S/e3sRy26n.2RP9AN25lMuApnY1WLapn1a',
    'Suresh Patil',        'suresh.patil@dsgp.gov.in',    'DISTRICT_OFFICER', 'Nashik',      1),

-- Finance Approver
(5, 'finance.approver1', '$2a$10$1mjJmEBaJgGkXv7yoV5S/e3sRy26n.2RP9AN25lMuApnY1WLapn1a',
    'Meena Joshi',         'meena.joshi@dsgp.gov.in',     'FINANCE_APPROVER', NULL,          1);
-- =============================================================================
-- Location Master Data
-- =============================================================================
-- Development/test location hierarchy:
-- Maharashtra -> Pune -> Haveli -> Pune

INSERT IGNORE INTO states (id, name)
VALUES
(1, 'Maharashtra');

INSERT IGNORE INTO districts (id, name, state_id)
VALUES
(1, 'Pune', 1);

INSERT IGNORE INTO talukas (id, name, district_id)
VALUES
(1, 'Haveli', 1);

INSERT IGNORE INTO villages (id, name, taluka_id)
VALUES
(1, 'Pune', 1);