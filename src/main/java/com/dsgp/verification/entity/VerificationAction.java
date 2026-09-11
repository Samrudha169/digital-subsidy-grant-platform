package com.dsgp.verification.entity;

/**
 * The action taken during the verification workflow.
 */
public enum VerificationAction {

    /**
     * Verification process was started.
     */
    START,

    /**
     * An individual verification criterion was verified.
     */
    CRITERION_VERIFIED,

    /**
     * Officer approves the application at the current stage.
     */
    APPROVE,

    /**
     * Officer rejects the application.
     */
    REJECT,

    /**
     * Field Officer sends the application to the District Officer.
     */
    ESCALATE,

    /**
     * Field Officer requests correction from the beneficiary.
     */
    REQUEST_CORRECTION
}