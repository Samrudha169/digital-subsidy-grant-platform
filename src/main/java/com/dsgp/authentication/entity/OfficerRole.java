package com.dsgp.authentication.entity;

/**
 * Roles available to users on the DSGP platform.
 *
 * FIELD_OFFICER     — Ground-level verification and document checking.
 * DISTRICT_OFFICER  — District-level review of escalated applications.
 * FINANCE_APPROVER  — Final financial approval before disbursement.
 * ADMIN             — Administrative management of the DSGP platform.
 */
public enum OfficerRole {

    FIELD_OFFICER,

    DISTRICT_OFFICER,

    FINANCE_APPROVER,

    ADMIN
}