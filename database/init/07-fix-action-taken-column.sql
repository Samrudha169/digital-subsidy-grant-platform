-- =============================================================================
-- MIGRATION: Fix action_taken column width in verification_records
-- =============================================================================
--
-- Bug: action_taken was VARCHAR(10) but the Java enum VerificationAction
--      includes CRITERION_VERIFIED (18 chars) and REQUEST_CORRECTION (18 chars).
--      MySQL strict mode raised a data-too-long error when a criterion was
--      marked FAILED, rolling back the entire @Transactional update and leaving
--      the application stuck at UNDER_REVIEW instead of moving to REJECTED.
--
-- Fix: widen the column to VARCHAR(30).
--
-- Safe to run multiple times (MODIFY is idempotent if already VARCHAR(30)).
-- =============================================================================

ALTER TABLE verification_records
    MODIFY COLUMN action_taken VARCHAR(30) NOT NULL
    COMMENT 'APPROVE | REJECT | ESCALATE | CRITERION_VERIFIED | REQUEST_CORRECTION | START';
