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
