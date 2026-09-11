package com.dsgp.verification.entity;

/**
 * Status of an individual verification criterion.
 */
public enum VerificationCriterionStatus {

    /** Criterion has not yet been checked. */
    PENDING,

    /** Officer has verified and passed the criterion. */
    VERIFIED,

    /** Officer checked the criterion and found it invalid. */
    FAILED
}