-- =============================================================================
-- MIGRATION 08 — Add missing columns to scheme_applications
--
-- The JPA entity SchemeApplication has three columns that were not in the
-- original CREATE TABLE (01-schema.sql): requested_amount, sanctioned_amount,
-- and rejected_at.  Run this script once against any existing database that
-- was initialised from 01-schema.sql before this migration was added.
--
-- NOTE: MySQL 8.0 does NOT support ADD COLUMN IF NOT EXISTS in ALTER TABLE.
-- This script uses a PROCEDURE + information_schema check to stay idempotent.
-- =============================================================================

DROP PROCEDURE IF EXISTS dsgp_migration_08;

DELIMITER $$
CREATE PROCEDURE dsgp_migration_08()
BEGIN
    -- requested_amount ---------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'scheme_applications'
          AND COLUMN_NAME  = 'requested_amount'
    ) THEN
        ALTER TABLE scheme_applications
            ADD COLUMN requested_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00
            COMMENT 'Amount requested by the beneficiary at submission time.';
    END IF;

    -- sanctioned_amount --------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'scheme_applications'
          AND COLUMN_NAME  = 'sanctioned_amount'
    ) THEN
        ALTER TABLE scheme_applications
            ADD COLUMN sanctioned_amount DECIMAL(12,2) NULL
            COMMENT 'Amount sanctioned by Finance Officer on final approval; NULL until then.';
    END IF;

    -- rejected_at --------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'scheme_applications'
          AND COLUMN_NAME  = 'rejected_at'
    ) THEN
        ALTER TABLE scheme_applications
            ADD COLUMN rejected_at DATETIME NULL
            COMMENT 'Timestamp when the application was rejected; used for the 30-day reapplication cooling period.';
    END IF;
END$$
DELIMITER ;

CALL dsgp_migration_08();
DROP PROCEDURE IF EXISTS dsgp_migration_08;
