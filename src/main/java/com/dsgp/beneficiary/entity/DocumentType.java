package com.dsgp.beneficiary.entity;

public enum DocumentType {

    IDENTITY_PROOF,

    ADDRESS_PROOF,

    INCOME_CERTIFICATE,

    LAND_RECORD,

    OCCUPATION_PROOF,

    CATEGORY_CERTIFICATE,

    BANK_ACCOUNT_PROOF,

    SCHEME_SPECIFIC_DOCUMENT,

    OTHER_SUPPORTING_DOCUMENT,

    // ============================================================
    // DISBURSEMENT STAGE 2 DOCUMENTS
    // ============================================================

    STAGE_2_INVOICE,

    STAGE_2_PAYMENT_PROOF,

    STAGE_2_ACTIVITY_PHOTO,

    // ============================================================
    // DISBURSEMENT STAGE 3 DOCUMENTS
    // ============================================================

    FINAL_COMPLETION_REPORT,

    UTILIZATION_STATEMENT,

    FINAL_PROJECT_PHOTO,

    FINAL_PAYMENT_PROOF
}