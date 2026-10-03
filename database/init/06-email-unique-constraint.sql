-- =============================================================================
-- Digital Subsidy & Grant Administration Platform
-- Migration 06 — Add UNIQUE constraint on beneficiary.email
-- =============================================================================
--
-- PURPOSE
-- -------
-- Enforces at the database level that no two beneficiary rows share the
-- same email address.  This prevents the "Query did not return a unique
-- result: 2 results were returned" (NonUniqueResultException) that occurs
-- when findFirstByEmail / findByEmail is called on a column without
-- a UNIQUE index.
--
-- PRE-CONDITION
-- -------------
-- If the table already contains duplicate email rows (created before this
-- constraint was applied), you MUST deduplicate them FIRST.
-- Use the diagnostic query below to find duplicates:
--
--   SELECT email, COUNT(*) AS cnt
--   FROM   beneficiary
--   GROUP  BY email
--   HAVING cnt > 1;
--
-- For each duplicate group, decide which row to KEEP (typically the
-- most-recently registered row or the one with emailVerified = TRUE).
-- Then delete the unwanted rows by their primary key:
--
--   -- Example: keep the row with the higher id, delete the older duplicate
--   DELETE FROM beneficiary
--   WHERE  email = 'example@domain.com'
--     AND  id NOT IN (
--         SELECT max_id FROM (
--             SELECT MAX(id) AS max_id
--             FROM   beneficiary
--             WHERE  email = 'example@domain.com'
--         ) t
--     );
--
-- Repeat for every email that appears more than once, then run this script.
--
-- NOTE: If spring.jpa.hibernate.ddl-auto=update is set, Hibernate will add
-- this constraint automatically at startup once the @Column(unique=true)
-- annotation is placed on Beneficiary.email.  However, Hibernate's
-- ddl-auto=update will FAIL if duplicates already exist, so deduplication
-- must happen first regardless.
-- =============================================================================

-- Add the UNIQUE constraint (safe to run multiple times — IF NOT EXISTS guard)
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM   information_schema.TABLE_CONSTRAINTS
    WHERE  TABLE_SCHEMA   = DATABASE()
      AND  TABLE_NAME     = 'beneficiary'
      AND  CONSTRAINT_NAME = 'uk_beneficiary_email'
      AND  CONSTRAINT_TYPE = 'UNIQUE'
);

-- Only add the constraint if it does not already exist
SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE beneficiary ADD CONSTRAINT uk_beneficiary_email UNIQUE (email)',
    'SELECT ''UNIQUE constraint uk_beneficiary_email already exists — skipped'' AS note'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
