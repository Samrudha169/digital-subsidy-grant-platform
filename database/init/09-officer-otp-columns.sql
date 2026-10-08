-- =============================================================================
-- MIGRATION 09 — Add OTP columns to officers table
--
-- Required by the mandatory email OTP second factor introduced for
-- FIELD_OFFICER, DISTRICT_OFFICER, and FINANCE_APPROVER logins.
--
-- Columns are nullable so existing rows and ADMIN accounts are unaffected.
-- JPA ddl-auto=update will also apply these columns automatically, but this
-- script is provided for explicit, repeatable migration against production DBs.
--
-- NOTE: MySQL 8.0 does NOT support ADD COLUMN IF NOT EXISTS.
--       An idempotent PROCEDURE + information_schema check is used instead.
-- =============================================================================

DROP PROCEDURE IF EXISTS dsgp_migration_09;

DELIMITER $$
CREATE PROCEDURE dsgp_migration_09()
BEGIN
    -- otp_code ---------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'officers'
          AND COLUMN_NAME  = 'otp_code'
    ) THEN
        ALTER TABLE officers
            ADD COLUMN otp_code VARCHAR(6) NULL
            COMMENT '6-digit login OTP; NULL when no pending OTP.';
    END IF;

    -- otp_expires_at ---------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'officers'
          AND COLUMN_NAME  = 'otp_expires_at'
    ) THEN
        ALTER TABLE officers
            ADD COLUMN otp_expires_at DATETIME NULL
            COMMENT 'Expiry timestamp for the pending OTP; NULL when otp_code is NULL.';
    END IF;
END$$
DELIMITER ;

CALL dsgp_migration_09();
DROP PROCEDURE IF EXISTS dsgp_migration_09;
