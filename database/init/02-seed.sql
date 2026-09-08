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
 18, 60,
 150000.00,
 2.0000,
 'SC/ST',
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
-- Milestone 2 — Officer Seed Data
-- =============================================================================
-- Default password for all seeded officers: password123
-- BCrypt hash (strength 10) of "password123":
--   $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
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
(1, 'field.officer1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Ramesh Kumar',        'ramesh.kumar@dsgp.gov.in',    'FIELD_OFFICER',    'Pune',        1),
(2, 'field.officer2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Priya Sharma',        'priya.sharma@dsgp.gov.in',    'FIELD_OFFICER',    'Nashik',      1),

-- District Officers
(3, 'district.officer1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Anita Desai',         'anita.desai@dsgp.gov.in',     'DISTRICT_OFFICER', 'Pune',        1),
(4, 'district.officer2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Suresh Patil',        'suresh.patil@dsgp.gov.in',    'DISTRICT_OFFICER', 'Nashik',      1),

-- Finance Approver
(5, 'finance.approver1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Meena Joshi',         'meena.joshi@dsgp.gov.in',     'FINANCE_APPROVER', NULL,          1);
