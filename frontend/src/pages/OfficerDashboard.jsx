import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import './OfficerDashboard.css';

// ============================================================================
// CONSTANTS
// ============================================================================

const ROLES = {
    FIELD_OFFICER: 'FIELD_OFFICER',
    DISTRICT_OFFICER: 'DISTRICT_OFFICER',
    FINANCE_APPROVER: 'FINANCE_APPROVER',
};

const ROLE_STATUSES = {
    [ROLES.FIELD_OFFICER]: ['PENDING', 'UNDER_REVIEW'],
    [ROLES.DISTRICT_OFFICER]: ['FIELD_APPROVED'],
    [ROLES.FINANCE_APPROVER]: ['DISTRICT_APPROVED'],
};

const ROLE_LABELS = {
    [ROLES.FIELD_OFFICER]: 'Field Officer',
    [ROLES.DISTRICT_OFFICER]: 'District Officer',
    [ROLES.FINANCE_APPROVER]: 'Finance Approver',
};

const STATUS_CLASS = {
    PENDING: 'status-pending',
    UNDER_REVIEW: 'status-review',
    FIELD_APPROVED: 'status-field-approved',
    ESCALATED: 'status-escalated',
    DISTRICT_APPROVED: 'status-district-approved',
    APPROVED: 'status-approved',
    REJECTED: 'status-rejected',
    CORRECTION_REQUIRED: 'status-correction',
};

const REMARKS_REQUIRED_ACTIONS = new Set([
    'field-reject',
    'district-reject',
    'finance-reject',
    'request-correction',
]);

/*
 * Field verification proof configuration.
 *
 * IMPORTANT:
 * IDENTITY + ADDRESS intentionally use ONE Aadhaar criterion.
 */
const FIELD_PROOF_CONFIG = {
    IDENTITY_ADDRESS: {
        title: 'Identity & Address Verification',
        description:
            'Verify the beneficiary identity using Identity Proof and address using Address Proof.',
        documentTypes: ['IDENTITY_PROOF', 'ADDRESS_PROOF'],
        documentLabel: 'Identity Proof / Address Proof',
        type: 'DOCUMENT',
        icon: '🪪',
    },

    INCOME: {
        title: 'Income Verification',
        description:
            'Verify the beneficiary annual income using the submitted Income Certificate.',
        documentTypes: ['INCOME_CERTIFICATE'],
        documentLabel: 'Income Certificate',
        type: 'DOCUMENT',
        icon: '💰',
    },

    LAND: {
        title: 'Land Ownership / Holding Verification',
        description:
            'Verify land ownership and recorded land holding using the submitted Land Record.',
        documentTypes: ['LAND_RECORD'],
        documentLabel: 'Land Record',
        type: 'DOCUMENT',
        icon: '🌾',
    },

    OCCUPATION: {
        title: 'Occupation Verification',
        description:
            'Verify the registered occupation using the submitted Occupation Proof.',
        documentTypes: ['OCCUPATION_PROOF'],
        documentLabel: 'Occupation Proof',
        type: 'DOCUMENT',
        icon: '💼',
    },

    CATEGORY: {
        title: 'Category Verification',
        description:
            'Verify the beneficiary category using the submitted Category Certificate.',
        documentTypes: ['CATEGORY_CERTIFICATE'],
        documentLabel: 'Category Certificate',
        type: 'DOCUMENT',
        icon: '🏷️',
    },

    DOCUMENTS: {
        title: 'Document Completeness',
        description:
            'Review all supporting documents submitted with the application.',
        type: 'ALL_DOCUMENTS',
        icon: '📁',
    },

    JURISDICTION: {
        title: 'District Jurisdiction Verified',
        description:
            'Verify that the beneficiary belongs to the jurisdiction assigned to this District Officer.',
        type: 'JURISDICTION',
        icon: '📍',
    },

    FIELD_REVIEW: {
        title: 'Field Verification Review',
        description:
            'Review the verification work completed by the Field Officer.',
        type: 'FIELD_REVIEW',
        icon: '🔎',
    },

    SCHEME_REVIEW: {
        title: 'Scheme and Application Details Verified',
        description:
            'Verify the scheme, application information and eligibility result.',
        type: 'SCHEME_REVIEW',
        icon: '📋',
    },

    DISTRICT_APPROVAL: {
        title: 'District Approval Verified',
        description:
            'Review the District Officer approval and verification history.',
        type: 'DISTRICT_APPROVAL',
        icon: '🏛️',
    },

    GRANT_AMOUNT: {
        title: 'Grant Amount Verified',
        description:
            'Verify the applicable scheme and approved grant information.',
        type: 'GRANT_AMOUNT',
        icon: '💰',
    },

    PAYMENT_DETAILS: {
        title: 'Payment Details Verified',
        description:
            'Verify beneficiary bank account and payment information.',
        type: 'PAYMENT_DETAILS',
        icon: '🏦',
    },
};

// ============================================================================
// HELPERS
// ============================================================================

function formatDate(dateStr) {
    if (!dateStr) return '—';

    const d = new Date(dateStr);

    if (Number.isNaN(d.getTime())) {
        return dateStr;
    }

    return d.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
    });
}

function formatDateTime(dateStr) {
    if (!dateStr) return '—';

    const d = new Date(dateStr);

    if (Number.isNaN(d.getTime())) {
        return dateStr;
    }

    return d.toLocaleString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

function formatBytes(bytes) {
    if (!bytes) return '—';

    if (bytes < 1024) {
        return `${bytes} B`;
    }

    if (bytes < 1048576) {
        return `${(bytes / 1024).toFixed(1)} KB`;
    }

    return `${(bytes / 1048576).toFixed(1)} MB`;
}

function getCriterionConfig(criterion) {
    const code = String(
        criterion?.criterionCode || ''
    ).toUpperCase();

    if (FIELD_PROOF_CONFIG[code]) {
        return FIELD_PROOF_CONFIG[code];
    }

    if (code.includes('IDENTITY') || code.includes('ADDRESS')) {
        return FIELD_PROOF_CONFIG.IDENTITY_ADDRESS;
    }

    if (code.includes('INCOME')) {
        return FIELD_PROOF_CONFIG.INCOME;
    }

    if (code.includes('LAND')) {
        return FIELD_PROOF_CONFIG.LAND;
    }

    if (code.includes('OCCUPATION')) {
        return FIELD_PROOF_CONFIG.OCCUPATION;
    }

    if (code.includes('CATEGORY')) {
        return FIELD_PROOF_CONFIG.CATEGORY;
    }

    if (code.includes('DOCUMENT')) {
        return FIELD_PROOF_CONFIG.DOCUMENTS;
    }

    if (code.includes('JURISDICTION')) {
        return FIELD_PROOF_CONFIG.JURISDICTION;
    }

    if (code.includes('FIELD_REVIEW')) {
        return FIELD_PROOF_CONFIG.FIELD_REVIEW;
    }

    if (code.includes('SCHEME_REVIEW')) {
        return FIELD_PROOF_CONFIG.SCHEME_REVIEW;
    }

    if (code.includes('DISTRICT_APPROVAL')) {
        return FIELD_PROOF_CONFIG.DISTRICT_APPROVAL;
    }

    if (code.includes('GRANT_AMOUNT')) {
        return FIELD_PROOF_CONFIG.GRANT_AMOUNT;
    }

    if (code.includes('PAYMENT_DETAILS')) {
        return FIELD_PROOF_CONFIG.PAYMENT_DETAILS;
    }

    return {
        title:
            criterion?.criterionName ||
            'Verification Criterion',
        description:
            'Review the relevant beneficiary information and supporting evidence.',
        type: 'PROFILE',
        icon: '🔎',
    };
}

function getCriterionDisplayTitle(criterion) {
    const config = getCriterionConfig(criterion);

    return config.title;
}

function getStatusLabel(status) {
    if (!status) return 'Unknown';

    return status
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase());
}

// ============================================================================
// API
// ============================================================================

async function fetchApplicationsByStatus(status) {
    const res = await fetch(
        `/api/v1/applications?status=${status}`
    );

    if (!res.ok) {
        throw new Error(
            `Server error ${res.status} fetching ${status} applications.`
        );
    }

    return res.json();
}

async function fetchVerificationStatus(applicationId) {
    const res = await fetch(
        `/api/v1/verification/applications/${applicationId}`
    );

    if (!res.ok) {
        throw new Error(
            `Server error ${res.status} fetching verification status.`
        );
    }

    return res.json();
}

async function fetchVerificationCriteria(
    applicationId,
    stage
) {
    const res = await fetch(
        `/api/v1/verification/applications/${applicationId}/criteria?stage=${stage}`
    );

    const data = await res.json().catch(() => []);

    if (!res.ok) {
        throw new Error(
            data.message ||
            data.error ||
            'Failed to fetch verification criteria.'
        );
    }

    return data;
}

async function updateVerificationCriterion(
    applicationId,
    criterionId,
    performedBy,
    status,
    remarks
) {
    const body = {
        performedBy,
        status,
    };

    if (remarks && remarks.trim()) {
        body.remarks = remarks.trim();
    }

    const res = await fetch(
        `/api/v1/verification/applications/${applicationId}/criteria/${criterionId}`,
        {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(body),
        }
    );

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
        throw new Error(
            data.message ||
            data.error ||
            `Criterion update failed (HTTP ${res.status}).`
        );
    }

    return data;
}

async function postVerificationAction(
    applicationId,
    action,
    performedBy,
    remarks
) {
    const body = {
        performedBy,
    };

    if (remarks && remarks.trim()) {
        body.remarks = remarks.trim();
    }

    const res = await fetch(
        `/api/v1/verification/applications/${applicationId}/${action}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(body),
        }
    );

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
        throw new Error(
            data.message ||
            data.error ||
            `Action failed (HTTP ${res.status}).`
        );
    }

    return data;
}

async function fetchBeneficiary(beneficiaryId) {
    const res = await fetch(
        `/api/v1/beneficiaries/${beneficiaryId}`
    );

    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
        throw new Error(
            data.message ||
            data.error ||
            `Failed to fetch beneficiary information.`
        );
    }

    return data;
}

async function fetchBeneficiaryDocuments(
    beneficiaryId
) {
    const res = await fetch(
        `/api/v1/beneficiaries/${beneficiaryId}/documents`
    );

    if (!res.ok) {
        return [];
    }

    const data = await res.json().catch(() => []);

    return Array.isArray(data) ? data : [];
}

// ============================================================================
// SMALL COMPONENTS
// ============================================================================

function StatusBadge({ status }) {
    const cls =
        STATUS_CLASS[status] ||
        'status-unknown';

    return (
        <span className={`od-status-badge ${cls}`}>
            {getStatusLabel(status)}
        </span>
    );
}

function ScoreBadge({ score }) {
    const numericScore =
        Number(score) || 0;

    let cls = 'score-low';

    if (numericScore >= 70) {
        cls = 'score-high';
    } else if (numericScore >= 40) {
        cls = 'score-mid';
    }

    return (
        <span className={`od-score-badge ${cls}`}>
            {numericScore}
        </span>
    );
}

// ============================================================================
// REMARKS MODAL
// ============================================================================

function RemarksModal({
                          action,
                          app,
                          onConfirm,
                          onCancel,
                          loading,
                      }) {
    const [remarks, setRemarks] =
        useState('');

    const [error, setError] =
        useState('');

    const requiresRemarks =
        REMARKS_REQUIRED_ACTIONS.has(action);

    const actionLabel = action
        ? action
            .replace(/-/g, ' ')
            .replace(/\b\w/g, c => c.toUpperCase())
        : 'Action';

    const submit = () => {
        if (
            requiresRemarks &&
            !remarks.trim()
        ) {
            setError(
                'Remarks are required for this action.'
            );
            return;
        }

        setError('');

        onConfirm(
            remarks.trim()
        );
    };

    return (
        <div className="od-modal-overlay">
            <div className="od-modal-card">

                <div className="od-modal-header">
                    <div>
                        <span className="od-modal-eyebrow">
                            Verification Action
                        </span>

                        <h2>
                            {actionLabel}
                        </h2>

                        <p>
                            Application #
                            {app?.applicationId}
                            {' — '}
                            {app?.beneficiaryName}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="od-modal-close"
                        onClick={onCancel}
                        disabled={loading}
                    >
                        ×
                    </button>
                </div>

                <label
                    className="od-modal-label"
                    htmlFor="verification-remarks"
                >
                    Remarks
                    {requiresRemarks && (
                        <span> *</span>
                    )}
                </label>

                <textarea
                    id="verification-remarks"
                    className="od-modal-textarea"
                    rows={5}
                    placeholder={
                        requiresRemarks
                            ? 'Enter the reason for this action...'
                            : 'Add remarks (optional)...'
                    }
                    value={remarks}
                    onChange={e => {
                        setRemarks(
                            e.target.value
                        );

                        if (error) {
                            setError('');
                        }
                    }}
                    disabled={loading}
                    autoFocus
                />

                {error && (
                    <div className="od-modal-error">
                        {error}
                    </div>
                )}

                <div className="od-modal-actions">

                    <button
                        type="button"
                        className="od-btn od-btn-secondary"
                        onClick={onCancel}
                        disabled={loading}
                    >
                        Cancel
                    </button>

                    <button
                        type="button"
                        className="od-btn od-btn-primary"
                        onClick={submit}
                        disabled={loading}
                    >
                        {loading
                            ? 'Processing...'
                            : `Confirm ${actionLabel}`}
                    </button>

                </div>

            </div>
        </div>
    );
}

// ============================================================================
// CRITERION ACTION MODAL
// ============================================================================

function CriterionModal({
                            criterion,
                            app,
                            proofReviewed,
                            onConfirm,
                            onCancel,
                            loading,
                        }) {
    const [remarks, setRemarks] =
        useState(
            criterion?.remarks || ''
        );

    const [error, setError] =
        useState('');

    if (!criterion) {
        return null;
    }

    const config =
        getCriterionConfig(criterion);

    const submit = status => {

        if (
            status === 'FAILED' &&
            !remarks.trim()
        ) {
            setError(
                'Remarks are required when a criterion is marked as failed.'
            );
            return;
        }

        const requiresProofReview =
            ['DOCUMENT', 'ALL_DOCUMENTS'].includes(
                config.type
            );

        if (
            status === 'VERIFIED' &&
            requiresProofReview &&
            !proofReviewed
        ) {
            setError(
                'Review the required proof before verification.'
            );
            return;
        }

        setError('');

        onConfirm({
            criterion,
            status,
            remarks,
        });
    };

    return (
        <div className="od-modal-overlay">

            <div className="od-modal-card od-criterion-modal">

                <div className="od-modal-header">

                    <div>
                        <span className="od-modal-eyebrow">
                            {getStatusLabel(
                                criterion?.stage
                            )} Verification
                        </span>

                        <h2>
                            {getCriterionDisplayTitle(
                                criterion
                            )}
                        </h2>

                        <p>
                            Application #
                            {app?.applicationId}
                        </p>
                    </div>

                    <button
                        type="button"
                        className="od-modal-close"
                        onClick={onCancel}
                        disabled={loading}
                    >
                        ×
                    </button>

                </div>

                <div className="od-criterion-modal-summary">

                    <div className="od-criterion-modal-icon">
                        {config.icon}
                    </div>

                    <div>
                        <strong>
                            {config.title}
                        </strong>

                        <p>
                            {config.description}
                        </p>
                    </div>

                </div>

                <label
                    className="od-modal-label"
                    htmlFor="criterion-remarks"
                >
                    Verification Remarks
                </label>

                <textarea
                    id="criterion-remarks"
                    className="od-modal-textarea"
                    rows={4}
                    placeholder="Add verification remarks..."
                    value={remarks}
                    onChange={e => {
                        setRemarks(
                            e.target.value
                        );

                        if (error) {
                            setError('');
                        }
                    }}
                    disabled={loading}
                />

                {error && (
                    <div className="od-modal-error">
                        {error}
                    </div>
                )}

                <div className="od-modal-actions">

                    <button
                        type="button"
                        className="od-btn od-btn-secondary"
                        onClick={onCancel}
                        disabled={loading}
                    >
                        Cancel
                    </button>

                    <button
                        type="button"
                        className="od-btn od-btn-danger"
                        onClick={() =>
                            submit('FAILED')
                        }
                        disabled={loading}
                    >
                        ✕ Fail
                    </button>

                    <button
                        type="button"
                        className="od-btn od-btn-success"
                        onClick={() =>
                            submit('VERIFIED')
                        }
                        disabled={
                            loading ||
                            !proofReviewed
                        }
                        title={
                            !proofReviewed
                                ? 'Review the required proof first.'
                                : 'Verify criterion'
                        }
                    >
                        ✓ Review & Verify
                    </button>

                </div>

            </div>

        </div>
    );
}

// ============================================================================
// CRITERION CARD
// ============================================================================

function CriterionCard({
                           criterion,
                           documents,
                           beneficiary,
                           app,
                           fieldCriteria,
                           verificationHistory,
                           onReview,
                           onFail,
                           proofViewed,
                           disabled,
                       }) {
    const config =
        getCriterionConfig(criterion);

    const status =
        criterion.status || 'PENDING';

    const relevantDocuments =
        config.type === 'DOCUMENT'
            ? (documents || []).filter(doc =>
                config.documentTypes?.includes(
                    String(
                        doc.documentType || ''
                    ).toUpperCase()
                )
            )
            : [];

    const allDocuments =
        documents || [];

    const isStudent =
        String(
            beneficiary?.occupation || ''
        ).toLowerCase() === 'student';

    const landNotRequired =
        String(
            criterion?.criterionCode || ''
        ).toUpperCase() === 'LAND' &&
        isStudent;

    const hasProof =
        landNotRequired
            ? true
            : config.type === 'DOCUMENT'
                ? relevantDocuments.length > 0
                : config.type === 'ALL_DOCUMENTS'
                    ? allDocuments.length > 0
                    : true;

    const isNotApplicable =
        landNotRequired;

    const evidenceDoesNotRequireDocumentReview =
        !['DOCUMENT', 'ALL_DOCUMENTS'].includes(config.type);

    const profileValue =
        beneficiary &&
        config.profileField
            ? beneficiary[
                config.profileField
                ]
            : null;

    const isVerified =
        status === 'VERIFIED';

    const isFailed =
        status === 'FAILED';

    const isPending =
        status === 'PENDING';

    return (
        <article
            className={`od-criterion-card ${
                isVerified
                    ? 'criterion-verified'
                    : ''
            } ${
                isFailed
                    ? 'criterion-failed'
                    : ''
            }`}
        >

            <div className="od-criterion-top">

                <div className="od-criterion-heading">

                    <div className="od-criterion-icon">
                        {config.icon}
                    </div>

                    <div>

                        <h3>
                            {config.title}
                        </h3>

                        <p>
                            {config.description}
                        </p>

                    </div>

                </div>

                <div
                    className={`od-criterion-status ${
                        isVerified
                            ? 'verified'
                            : isFailed
                                ? 'failed'
                                : 'pending'
                    }`}
                >
                    {isVerified
                        ? '✓ Verified'
                        : isFailed
                            ? '✕ Failed'
                            : '○ Pending'}
                </div>

            </div>

            {isNotApplicable && (
                <div className="od-proof-area">

                    <div className="od-no-proof">
                        <span>ℹ️</span>

                        <div>
                            <strong>
                                Land Record Not Required
                            </strong>

                            <p>
                                Land ownership verification is not applicable for a student beneficiary.
                            </p>
                        </div>

                    </div>

                </div>
            )}

            {/* JURISDICTION EVIDENCE */}

            {config.type === 'JURISDICTION' && (
                <div className="od-proof-area">

                    <div className="od-proof-label">
                        Jurisdiction Evidence
                    </div>

                    <div className="od-info-grid">
                        <div className="od-info-card">
                            <InfoRow label="State" value={beneficiary?.state} />
                            <InfoRow label="District" value={beneficiary?.district} />
                            <InfoRow label="Taluka" value={beneficiary?.taluka} />
                            <InfoRow label="Village" value={beneficiary?.village} />
                            <InfoRow label="PIN Code" value={beneficiary?.pinCode} />
                            <InfoRow label="Address" value={beneficiary?.address} />
                        </div>
                    </div>

                </div>
            )}

            {/* FIELD OFFICER REVIEW EVIDENCE */}

            {config.type === 'FIELD_REVIEW' && (
                <div className="od-proof-area">

                    <div className="od-proof-label">
                        Field Officer Verification
                    </div>

                    <div className="od-document-list">
                        {(fieldCriteria || []).length === 0 ? (
                            <div className="od-no-proof">
                                <span>⚠️</span>
                                <div>
                                    <strong>Field verification data not found</strong>
                                    <p>The Field Officer verification records could not be loaded.</p>
                                </div>
                            </div>
                        ) : (
                            fieldCriteria.map(fieldCriterion => (
                                <div className="od-document-item" key={fieldCriterion.id}>
                                    <div className="od-document-icon">
                                        {fieldCriterion.status === 'VERIFIED'
                                            ? '✓'
                                            : fieldCriterion.status === 'FAILED'
                                                ? '✕'
                                                : '○'}
                                    </div>

                                    <div className="od-document-info">
                                        <strong>{fieldCriterion.criterionName}</strong>
                                        <span>
                                            Status: {getStatusLabel(fieldCriterion.status)}
                                            {fieldCriterion.verifiedBy
                                                ? ` · Verified by ${fieldCriterion.verifiedBy}`
                                                : ''}
                                        </span>
                                        {fieldCriterion.remarks && (
                                            <span>Remarks: {fieldCriterion.remarks}</span>
                                        )}
                                    </div>
                                </div>
                            ))
                        )}
                    </div>

                </div>
            )}

            {/* SCHEME / APPLICATION EVIDENCE */}

            {config.type === 'SCHEME_REVIEW' && (
                <div className="od-proof-area">

                    <div className="od-proof-label">
                        Application & Eligibility Evidence
                    </div>

                    <div className="od-info-grid">
                        <div className="od-info-card">
                            <InfoRow label="Application ID" value={app?.applicationId ? `#${app.applicationId}` : null} />
                            <InfoRow label="Beneficiary" value={beneficiary?.fullName} />
                            <InfoRow label="Scheme" value={app?.schemeName || beneficiary?.schemeName} />
                            <InfoRow label="Eligibility Score" value={app?.eligibilityScore != null ? `${app.eligibilityScore} / 100` : null} />
                            <InfoRow label="Eligibility Result" value={app?.eligibilityResult || 'Eligible'} />
                            <InfoRow label="Application Status" value={app?.applicationStatus} />
                        </div>
                    </div>

                </div>
            )}

            {/* FINANCE EVIDENCE */}

            {config.type === 'DISTRICT_APPROVAL' && (
                <div className="od-proof-area">
                    <div className="od-proof-label">District Approval Evidence</div>
                    <div className="od-document-list">
                        {(verificationHistory || []).filter(entry =>
                            String(entry.stage || '').toUpperCase() === 'DISTRICT'
                        ).length === 0 ? (
                            <div className="od-no-proof">
                                <span>⚠️</span>
                                <div>
                                    <strong>No district approval record found</strong>
                                    <p>Review the application status and verification history.</p>
                                </div>
                            </div>
                        ) : (
                            (verificationHistory || []).filter(entry =>
                                String(entry.stage || '').toUpperCase() === 'DISTRICT'
                            ).map((entry, index) => (
                                <div className="od-document-item" key={`${entry.performedAt || ''}-${index}`}>
                                    <div className="od-document-icon">✓</div>
                                    <div className="od-document-info">
                                        <strong>{String(entry.action || '').replace(/_/g, ' ')}</strong>
                                        <span>by {entry.performedBy || 'District Officer'}</span>
                                        {entry.remarks && <span>Remarks: {entry.remarks}</span>}
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}

            {config.type === 'GRANT_AMOUNT' && (
                <div className="od-proof-area">
                    <div className="od-proof-label">Grant Information</div>
                    <div className="od-info-grid">
                        <div className="od-info-card">
                            <InfoRow label="Scheme" value={app?.schemeName} />
                            <InfoRow label="Eligibility Score" value={app?.eligibilityScore != null ? `${app.eligibilityScore} / 100` : null} />
                            <InfoRow label="Application Status" value={app?.applicationStatus} />
                        </div>
                    </div>
                </div>
            )}

            {config.type === 'PAYMENT_DETAILS' && (
                <div className="od-proof-area">
                    <div className="od-proof-label">
                        Payment Information
                    </div>

                    <div className="od-info-grid">
                        <div className="od-info-card">

                            <InfoRow
                                label="Beneficiary"
                                value={beneficiary?.fullName}
                            />

                            <InfoRow
                                label="Bank Account Proof"
                                value={
                                    allDocuments.find(
                                        doc =>
                                            String(
                                                doc.documentType || ''
                                            ).toUpperCase() ===
                                            'BANK_ACCOUNT_PROOF'
                                    )?.originalFileName ||
                                    allDocuments.find(
                                        doc =>
                                            String(
                                                doc.documentType || ''
                                            ).toUpperCase() ===
                                            'BANK_ACCOUNT_PROOF'
                                    )?.fileName ||
                                    'Not uploaded'
                                }
                            />

                            {(() => {
                                const bankProof =
                                    allDocuments.find(
                                        doc =>
                                            String(
                                                doc.documentType || ''
                                            ).toUpperCase() ===
                                            'BANK_ACCOUNT_PROOF'
                                    );

                                if (!bankProof) {
                                    return null;
                                }

                                return (
                                    <div style={{ marginTop: '16px' }}>
                                        <a
                                            href={`/api/v1/beneficiaries/${beneficiary?.id}/documents/${bankProof.id}/download`}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="od-view-document"
                                        >
                                            👁 View Passbook
                                        </a>
                                    </div>
                                );
                            })()}

                            <InfoRow
                                label="Application Status"
                                value={app?.applicationStatus}
                            />

                        </div>
                    </div>
                </div>
            )}

            {/* PROFILE EVIDENCE */}

            {config.type === 'PROFILE' && (
                <div className="od-proof-area">
                    <div className="od-proof-label">Beneficiary Information</div>
                    <div className="od-info-grid">
                        <div className="od-info-card">
                            <InfoRow label="Occupation" value={beneficiary?.occupation} />
                            <InfoRow label="Category" value={beneficiary?.category} />
                            <InfoRow label="Annual Income" value={beneficiary?.annualIncome} />
                            <InfoRow label="Land Holding" value={beneficiary?.landHolding} />
                        </div>
                    </div>
                </div>
            )}

            {/* DOCUMENT PROOF */}

            {config.type === 'DOCUMENT' && (

                <div className="od-proof-area">

                    <div className="od-proof-label">
                        Required Proof
                    </div>

                    {relevantDocuments.length === 0 ? (

                        <div className="od-no-proof">
                            <span>⚠️</span>

                            <div>
                                <strong>
                                    {config.documentLabel}
                                    {' '}not submitted
                                </strong>

                                <p>
                                    The required document
                                    could not be found.
                                </p>
                            </div>
                        </div>

                    ) : (

                        <div className="od-document-list">

                            {relevantDocuments.map(
                                doc => (
                                    <div
                                        className="od-document-item"
                                        key={doc.id}
                                    >

                                        <div className="od-document-icon">
                                            📄
                                        </div>

                                        <div className="od-document-info">

                                            <strong>
                                                {doc.originalFileName ||
                                                    doc.fileName ||
                                                    config.documentLabel}
                                            </strong>

                                            <span>
                                                {config.documentLabel}
                                                {' · '}
                                                {formatBytes(
                                                    doc.fileSize
                                                )}
                                            </span>

                                        </div>

                                        <a
                                            href={`/api/v1/beneficiaries/${beneficiary?.id}/documents/${doc.id}/download`}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="od-view-document"
                                            onClick={() =>
                                                onReview(
                                                    criterion.id,
                                                    doc.id
                                                )
                                            }
                                        >
                                            View Document
                                        </a>

                                    </div>
                                )
                            )}

                        </div>

                    )}

                </div>

            )}



            {/* ALL DOCUMENTS */}

            {config.type === 'ALL_DOCUMENTS' && (

                <div className="od-proof-area">

                    <div className="od-proof-label">
                        Submitted Documents
                    </div>

                    {allDocuments.length === 0 ? (

                        <div className="od-no-proof">
                            <span>⚠️</span>

                            <div>
                                <strong>
                                    No documents submitted
                                </strong>

                                <p>
                                    There are no documents
                                    available for review.
                                </p>
                            </div>
                        </div>

                    ) : (

                        <div className="od-document-list">

                            {allDocuments.map(
                                doc => (
                                    <div
                                        className="od-document-item"
                                        key={doc.id}
                                    >

                                        <div className="od-document-icon">
                                            📄
                                        </div>

                                        <div className="od-document-info">

                                            <strong>
                                                {doc.originalFileName ||
                                                    doc.fileName ||
                                                    'Document'}
                                            </strong>

                                            <span>
                                                {String(
                                                    doc.documentType ||
                                                    'OTHER'
                                                ).replace(
                                                    /_/g,
                                                    ' '
                                                )}
                                                {' · '}
                                                {formatBytes(
                                                    doc.fileSize
                                                )}
                                            </span>

                                        </div>

                                        <a
                                            href={`/api/v1/beneficiaries/${beneficiary?.id}/documents/${doc.id}/download`}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="od-view-document"
                                            onClick={() =>
                                                onReview(
                                                    criterion.id,
                                                    doc.id
                                                )
                                            }
                                        >
                                            View Document
                                        </a>

                                    </div>
                                )
                            )}

                        </div>

                    )}

                </div>

            )}

            {/* CRITERION FOOTER */}

            <div className="od-criterion-footer">

                <div className="od-proof-state">

                    {isVerified && (
                        <span className="od-proof-success">
                            ✓ Verified
                            {criterion.verifiedBy
                                ? ` by ${criterion.verifiedBy}`
                                : ''}
                        </span>
                    )}

                    {isFailed && (
                        <span className="od-proof-failed">
                            ✕ Verification failed
                        </span>
                    )}

                    {isPending &&
                        config.type === 'DOCUMENT' &&
                        !hasProof && (
                            <span className="od-proof-warning">
                                Required document missing
                            </span>
                        )}

                    {isPending &&
                        config.type === 'DOCUMENT' &&
                        hasProof &&
                        !proofViewed && (
                            <span className="od-proof-warning">
                                Review the document before verification
                            </span>
                        )}

                    {isPending &&
                        config.type === 'PROFILE' &&
                        !proofViewed && (
                            <span className="od-proof-warning">
                                Review beneficiary details before verification
                            </span>
                        )}

                </div>

                <div className="od-criterion-actions">

                    <button
                        type="button"
                        className="od-criterion-fail"
                        onClick={() =>
                            onFail(criterion)
                        }
                        disabled={
                            disabled ||
                            isVerified
                        }
                    >
                        ✕ Fail
                    </button>

                    <button
                        type="button"
                        className="od-criterion-verify"
                        onClick={() =>
                            onReview(
                                criterion.id,
                                null,
                                true
                            )
                        }
                        disabled={
                            disabled ||
                            isVerified ||
                            (!evidenceDoesNotRequireDocumentReview && !proofViewed) ||
                            !hasProof
                        }
                    >
                        ✓ Review & Verify
                    </button>

                </div>

            </div>

        </article>
    );
}

// ============================================================================
// APPLICATION REVIEW
// ============================================================================

function ApplicationReview({
                               app,
                               verificationData,
                               beneficiary,
                               documents,
                               criteria,
                               supportingFieldCriteria,
                               criteriaLoading,
                               proofViewedMap,
                               onProofViewed,
                               onCriterionVerify,
                               onCriterionFail,
                               onBack,
                               onAction,
                               actionLoading,
                               officerRole,
                           }) {
    const safeCriteria =
        Array.isArray(criteria)
            ? criteria
            : [];

    const verifiedCount =
        safeCriteria.filter(
            c => c.status === 'VERIFIED'
        ).length;

    const failedCount =
        safeCriteria.filter(
            c => c.status === 'FAILED'
        ).length;

    const totalCriteria =
        safeCriteria.length;

    const progress =
        totalCriteria > 0
            ? Math.round(
                (verifiedCount /
                    totalCriteria) *
                100
            )
            : 0;

    const canFieldApprove =
        officerRole ===
        ROLES.FIELD_OFFICER &&
        app.applicationStatus ===
        'UNDER_REVIEW' &&
        totalCriteria > 0 &&
        verifiedCount === totalCriteria;

    const canDistrictApprove =
        officerRole === ROLES.DISTRICT_OFFICER &&
        app.applicationStatus === 'FIELD_APPROVED' &&
        totalCriteria > 0 &&
        verifiedCount === totalCriteria;

    const canFinanceApprove =
        officerRole === ROLES.FINANCE_APPROVER &&
        app.applicationStatus === 'DISTRICT_APPROVED' &&
        totalCriteria > 0 &&
        verifiedCount === totalCriteria;

    const displayBeneficiary =
        beneficiary || {};

    const address =
        displayBeneficiary.address ||
        [
            displayBeneficiary.village,
            displayBeneficiary.taluka,
            displayBeneficiary.district,
            displayBeneficiary.state,
        ]
            .filter(Boolean)
            .join(', ');

    return (
        <div className="od-review-page">

            {/* ================================================================
                REVIEW HEADER
            ================================================================ */}

            <header className="od-review-header">

                <button
                    type="button"
                    className="od-back-button"
                    onClick={onBack}
                >
                    ← Back to Applications
                </button>

                <div className="od-review-title-row">

                    <div>

                        <div className="od-review-kicker">
                            APPLICATION REVIEW
                        </div>

                        <h1>
                            {app.beneficiaryName ||
                                displayBeneficiary.fullName ||
                                'Beneficiary'}
                        </h1>

                        <p>
                            Application #
                            {app.applicationId}
                            {' · '}
                            {app.schemeName}
                        </p>

                    </div>

                    <div className="od-review-status-area">

                        <StatusBadge
                            status={
                                app.applicationStatus
                            }
                        />

                        <span className="od-role-chip">
                            {ROLE_LABELS[
                                officerRole
                                ] || officerRole}
                        </span>

                    </div>

                </div>

            </header>

            {/* ================================================================
                APPLICATION SUMMARY
            ================================================================ */}

            <section className="od-summary-grid">

                <div className="od-summary-card">

                    <span className="od-summary-icon">
                        📋
                    </span>

                    <div>
                        <span>
                            Application
                        </span>

                        <strong>
                            #{app.applicationId}
                        </strong>
                    </div>

                </div>

                <div className="od-summary-card">

                    <span className="od-summary-icon">
                        🏛️
                    </span>

                    <div>
                        <span>
                            Scheme
                        </span>

                        <strong>
                            {app.schemeName ||
                                '—'}
                        </strong>
                    </div>

                </div>

                <div className="od-summary-card">

                    <span className="od-summary-icon">
                        🎯
                    </span>

                    <div>
                        <span>
                            Eligibility Score
                        </span>

                        <strong>
                            <ScoreBadge
                                score={
                                    app.eligibilityScore
                                }
                            />
                            <small>
                                / 100
                            </small>
                        </strong>
                    </div>

                </div>

                <div className="od-summary-card">

                    <span className="od-summary-icon">
                        📅
                    </span>

                    <div>
                        <span>
                            Submitted
                        </span>

                        <strong>
                            {formatDate(
                                app.applicationDate
                            )}
                        </strong>
                    </div>

                </div>

            </section>

            {/* ================================================================
                BENEFICIARY INFORMATION
            ================================================================ */}

            <section className="od-section">

                <div className="od-section-heading">

                    <div>
                        <span className="od-section-kicker">
                            BENEFICIARY
                        </span>

                        <h2>
                            Beneficiary Information
                        </h2>
                    </div>

                    <span className="od-section-icon">
                        👤
                    </span>

                </div>

                <div className="od-info-grid">

                    {/* Personal */}

                    <div className="od-info-card">

                        <h3>
                            Personal Details
                        </h3>

                        <InfoRow
                            label="Full Name"
                            value={
                                displayBeneficiary.fullName ||
                                app.beneficiaryName
                            }
                        />

                        <InfoRow
                            label="First Name"
                            value={
                                displayBeneficiary.firstName
                            }
                        />

                        <InfoRow
                            label="Last Name"
                            value={
                                displayBeneficiary.lastName
                            }
                        />

                        <InfoRow
                            label="Age"
                            value={
                                displayBeneficiary.age
                                    ? `${displayBeneficiary.age} years`
                                    : null
                            }
                        />

                        <InfoRow
                            label="Date of Birth"
                            value={
                                displayBeneficiary.dateOfBirth
                                    ? formatDate(
                                        displayBeneficiary.dateOfBirth
                                    )
                                    : null
                            }
                        />

                        <InfoRow
                            label="Gender"
                            value={
                                displayBeneficiary.gender
                            }
                        />

                    </div>

                    {/* Identity / Contact */}

                    <div className="od-info-card">

                        <h3>
                            Identity & Contact
                        </h3>

                        <InfoRow
                            label="Aadhaar Number"
                            value={
                                displayBeneficiary.aadhaarNumber ||
                                displayBeneficiary.govId
                            }
                            sensitive
                        />

                        <InfoRow
                            label="Government ID"
                            value={
                                displayBeneficiary.govId
                            }
                            sensitive
                        />

                        <InfoRow
                            label="Mobile Number"
                            value={
                                displayBeneficiary.mobileNumber ||
                                displayBeneficiary.contact
                            }
                        />

                        <InfoRow
                            label="Email"
                            value={
                                displayBeneficiary.email
                            }
                        />

                    </div>

                    {/* Address */}

                    <div className="od-info-card">

                        <h3>
                            Address
                        </h3>

                        <InfoRow
                            label="Address"
                            value={address}
                        />

                        <InfoRow
                            label="Village"
                            value={
                                displayBeneficiary.village
                            }
                        />

                        <InfoRow
                            label="Taluka"
                            value={
                                displayBeneficiary.taluka
                            }
                        />

                        <InfoRow
                            label="District"
                            value={
                                displayBeneficiary.district
                            }
                        />

                        <InfoRow
                            label="State"
                            value={
                                displayBeneficiary.state
                            }
                        />

                        <InfoRow
                            label="PIN Code"
                            value={
                                displayBeneficiary.pinCode
                            }
                        />

                    </div>

                    {/* Financial */}

                    <div className="od-info-card">

                        <h3>
                            Financial & Scheme Details
                        </h3>

                        <InfoRow
                            label="Occupation"
                            value={
                                displayBeneficiary.occupation
                            }
                        />

                        <InfoRow
                            label="Annual Income"
                            value={
                                displayBeneficiary.annualIncome != null
                                    ? `₹${Number(
                                        displayBeneficiary.annualIncome
                                    ).toLocaleString('en-IN')}`
                                    : null
                            }
                        />

                        <InfoRow
                            label="Land Holding"
                            value={
                                displayBeneficiary.landHolding != null
                                    ? `${displayBeneficiary.landHolding} acres`
                                    : null
                            }
                        />

                        <InfoRow
                            label="Category"
                            value={
                                displayBeneficiary.category
                            }
                        />

                        <InfoRow
                            label="Scheme"
                            value={
                                app.schemeName
                            }
                        />

                    </div>

                </div>

            </section>

            {/* ================================================================
                FIELD VERIFICATION
            ================================================================ */}

            <section className="od-section od-verification-section">

                <div className="od-section-heading">

                    <div>
                        <span className="od-section-kicker">
                            {officerRole === ROLES.FIELD_OFFICER
                                ? 'FIELD OFFICER REVIEW'
                                : officerRole === ROLES.DISTRICT_OFFICER
                                    ? 'DISTRICT OFFICER REVIEW'
                                    : 'FINANCE REVIEW'}
                        </span>

                        <h2>
                            {officerRole === ROLES.FIELD_OFFICER
                                ? 'Field Verification Criteria'
                                : officerRole === ROLES.DISTRICT_OFFICER
                                    ? 'District Verification Criteria'
                                    : 'Finance Verification Criteria'}
                        </h2>

                        <p>
                            Review the evidence for each criterion
                            before recording your decision.
                        </p>
                    </div>

                    <div className="od-verification-counter">
                        <strong>
                            {verifiedCount}
                            <span>
                                /
                                {totalCriteria}
                            </span>
                        </strong>

                        <small>
                            Verified
                        </small>
                    </div>

                </div>

                <div className="od-progress-box">

                    <div className="od-progress-top">

                        <div>

                            <strong>
                                Verification Progress
                            </strong>

                            <span>
                                {progress}% complete
                            </span>

                        </div>

                        <span>
                            {verifiedCount}
                            {' '}of{' '}
                            {totalCriteria}
                            {' '}criteria verified
                        </span>

                    </div>

                    <div className="od-progress-track">

                        <div
                            className="od-progress-fill"
                            style={{
                                width: `${progress}%`,
                            }}
                        />

                    </div>

                    {failedCount > 0 && (
                        <div className="od-progress-warning">
                            ⚠️ {failedCount} criterion
                            {failedCount > 1
                                ? 'a'
                                : ''}
                            {' '}marked as failed.
                        </div>
                    )}

                </div>

                {criteriaLoading ? (

                    <div className="od-loading-card">
                        <div className="od-spinner" />
                        <p>
                            Loading verification criteria...
                        </p>
                    </div>

                ) : safeCriteria.length === 0 ? (

                    <div className="od-empty-card">

                        <span>
                            🔎
                        </span>

                        <h3>
                            No verification criteria found
                        </h3>

                        <p>
                            Verification criteria have not
                            been created for this application.
                        </p>

                    </div>

                ) : (

                    <div className="od-criteria-list">

                        {safeCriteria.map(
                            criterion => (
                                <CriterionCard
                                    key={
                                        criterion.id
                                    }
                                    criterion={
                                        criterion
                                    }
                                    documents={
                                        documents
                                    }
                                    beneficiary={
                                        displayBeneficiary
                                    }
                                    app={
                                        app
                                    }
                                    fieldCriteria={
                                        supportingFieldCriteria
                                    }
                                    verificationHistory={
                                        verificationData?.history || []
                                    }
                                    proofViewed={
                                        Boolean(
                                            proofViewedMap[
                                                criterion.id
                                                ]
                                        )
                                    }
                                    onReview={
                                        onProofViewed
                                    }
                                    onFail={
                                        onCriterionFail
                                    }
                                    disabled={
                                        actionLoading
                                    }
                                />
                            )
                        )}

                    </div>

                )}

                {/* ============================================================
                    APPROVAL AREA
                ============================================================ */}

                {officerRole ===
                    ROLES.FIELD_OFFICER && (
                        <div
                            className={`od-approval-box ${
                                canFieldApprove
                                    ? 'ready'
                                    : ''
                            }`}
                        >

                            <div>

                            <span className="od-approval-icon">
                                {canFieldApprove
                                    ? '✓'
                                    : '🔒'}
                            </span>

                                <div>

                                    <strong>
                                        {canFieldApprove
                                            ? 'Field Verification Complete'
                                            : 'Field Verification Incomplete'}
                                    </strong>

                                    <p>
                                        {canFieldApprove
                                            ? 'All field verification criteria have been verified. The application is ready for the next stage.'
                                            : `Verify all ${totalCriteria || 0} criteria before approving this application.`}
                                    </p>

                                </div>

                            </div>

                            <button
                                type="button"
                                className="od-final-approve"
                                disabled={
                                    !canFieldApprove ||
                                    actionLoading
                                }
                                onClick={() =>
                                    onAction(
                                        app,
                                        'field-approve',
                                        false
                                    )
                                }
                            >
                                {actionLoading
                                    ? 'Processing...'
                                    : '✓ Approve Application'}
                            </button>

                        </div>
                    )}

                {officerRole ===
                    ROLES.DISTRICT_OFFICER && (
                        <div className={`od-approval-box ${
                            canDistrictApprove ? 'ready' : ''
                        }`}>

                            <div>

                            <span className="od-approval-icon">
                                🏛️
                            </span>

                                <div>

                                    <strong>
                                        {canDistrictApprove
                                            ? 'District Verification Complete'
                                            : 'District Verification Incomplete'}
                                    </strong>

                                    <p>
                                        {canDistrictApprove
                                            ? 'All District verification criteria have been verified.'
                                            : `Verify all ${totalCriteria || 0} District criteria before approving this application.`}
                                    </p>

                                </div>

                            </div>

                            <button
                                type="button"
                                className="od-final-approve"
                                disabled={
                                    !canDistrictApprove ||
                                    actionLoading
                                }
                                onClick={() =>
                                    onAction(
                                        app,
                                        'district-approve',
                                        false
                                    )
                                }
                            >
                                ✓ District Approve
                            </button>

                        </div>
                    )}

                {officerRole ===
                    ROLES.FINANCE_APPROVER && (
                        <div className={`od-approval-box ${
                            canFinanceApprove ? 'ready' : ''
                        }`}>

                            <div>

                            <span className="od-approval-icon">
                                💰
                            </span>

                                <div>

                                    <strong>
                                        {canFinanceApprove
                                            ? 'Finance Verification Complete'
                                            : 'Finance Verification Incomplete'}
                                    </strong>

                                    <p>
                                        {canFinanceApprove
                                            ? 'All Finance verification criteria have been verified. Final approval is ready.'
                                            : `Verify all ${totalCriteria || 0} Finance criteria before final approval.`}
                                    </p>

                                </div>

                            </div>

                            <button
                                type="button"
                                className="od-final-approve"
                                disabled={
                                    !canFinanceApprove ||
                                    actionLoading
                                }
                                onClick={() =>
                                    onAction(
                                        app,
                                        'finance-approve',
                                        false
                                    )
                                }
                            >
                                ✓ Finance Approve
                            </button>

                        </div>
                    )}

            </section>

            {/* ================================================================
                VERIFICATION HISTORY
            ================================================================ */}

            <section className="od-section od-history-section">

                <div className="od-section-heading">

                    <div>
                        <span className="od-section-kicker">
                            AUDIT TRAIL
                        </span>

                        <h2>
                            Verification History
                        </h2>
                    </div>

                    <span className="od-section-icon">
                        🕐
                    </span>

                </div>

                {!verificationData?.history ||
                verificationData.history.length === 0 ? (

                    <div className="od-history-empty">
                        No verification actions have been recorded yet.
                    </div>

                ) : (

                    <div className="od-timeline">

                        {verificationData.history.map(
                            (entry, index) => (
                                <div
                                    className="od-timeline-item"
                                    key={index}
                                >

                                    <div className="od-timeline-dot">
                                        ✓
                                    </div>

                                    <div className="od-timeline-content">

                                        <div className="od-timeline-top">

                                            <strong>
                                                {String(
                                                    entry.action ||
                                                    ''
                                                ).replace(
                                                    /_/g,
                                                    ' '
                                                )}
                                            </strong>

                                            <span>
                                                {formatDateTime(
                                                    entry.performedAt
                                                )}
                                            </span>

                                        </div>

                                        <div className="od-timeline-meta">

                                            <span>
                                                {entry.stage}
                                            </span>

                                            <span>
                                                by{' '}
                                                {entry.performedBy}
                                            </span>

                                        </div>

                                        {entry.remarks && (
                                            <p>
                                                {entry.remarks}
                                            </p>
                                        )}

                                    </div>

                                </div>
                            )
                        )}

                    </div>

                )}

            </section>

        </div>
    );
}

// ============================================================================
// INFO ROW
// ============================================================================

function InfoRow({
                     label,
                     value,
                     sensitive = false,
                 }) {
    return (
        <div className="od-info-row">

            <span>
                {label}
            </span>

            <strong
                className={
                    sensitive
                        ? 'od-sensitive'
                        : ''
                }
            >
                {value || '—'}
            </strong>

        </div>
    );
}

// ============================================================================
// MAIN DASHBOARD
// ============================================================================

function OfficerDashboard() {

    const navigate = useNavigate();

    // ------------------------------------------------------------------------
    // Session
    // ------------------------------------------------------------------------

    const officerLoggedIn =
        localStorage.getItem(
            'officerLoggedIn'
        ) === 'true';

    const officerName =
        localStorage.getItem(
            'officerName'
        ) || 'Officer';

    const officerRole =
        localStorage.getItem(
            'officerRole'
        ) || '';

    const officerDistrict =
        localStorage.getItem(
            'officerDistrict'
        ) || '';

    const officerUsername =
        localStorage.getItem(
            'officerUsername'
        ) || '';

    // ------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------

    const [applications, setApplications] =
        useState([]);

    const [loading, setLoading] =
        useState(true);

    const [fetchError, setFetchError] =
        useState('');

    const [successMsg, setSuccessMsg] =
        useState('');

    const [actionError, setActionError] =
        useState('');

    const [selectedApp, setSelectedApp] =
        useState(null);

    const [verificationData, setVerificationData] =
        useState(null);

    const [beneficiary, setBeneficiary] =
        useState(null);

    const [documents, setDocuments] =
        useState([]);

    const [fieldCriteria, setFieldCriteria] =
        useState([]);

    const [supportingFieldCriteria, setSupportingFieldCriteria] =
        useState([]);

    const [criteriaLoading, setCriteriaLoading] =
        useState(false);

    const [proofViewedMap, setProofViewedMap] =
        useState({});

    const [pendingAction, setPendingAction] =
        useState(null);

    const [actionLoading, setActionLoading] =
        useState(false);

    const [pendingCriterion, setPendingCriterion] =
        useState(null);

    const [criterionLoading, setCriterionLoading] =
        useState(false);

    // ------------------------------------------------------------------------
    // Login protection
    // ------------------------------------------------------------------------

    useEffect(() => {

        if (!officerLoggedIn) {
            navigate(
                '/officer/login',
                { replace: true }
            );
        }

    }, [
        officerLoggedIn,
        navigate,
    ]);

    // ------------------------------------------------------------------------
    // Load applications
    // ------------------------------------------------------------------------

    const loadApplications =
        useCallback(async () => {

            if (
                !officerRole ||
                !ROLE_STATUSES[
                    officerRole
                    ]
            ) {
                setLoading(false);
                return;
            }

            setLoading(true);
            setFetchError('');

            try {

                const statuses =
                    ROLE_STATUSES[
                        officerRole
                        ];

                const results =
                    await Promise.all(
                        statuses.map(
                            status =>
                                fetchApplicationsByStatus(
                                    status
                                )
                        )
                    );

                const all =
                    results
                        .flat()
                        .sort(
                            (a, b) =>
                                new Date(
                                    b.applicationDate
                                ) -
                                new Date(
                                    a.applicationDate
                                )
                        );

                setApplications(all);

            } catch (err) {

                setFetchError(
                    err.message ||
                    'Failed to load applications.'
                );

            } finally {

                setLoading(false);

            }

        }, [
            officerRole,
        ]);

    useEffect(() => {

        if (officerLoggedIn) {
            loadApplications();
        }

    }, [
        officerLoggedIn,
        loadApplications,
    ]);

    // ------------------------------------------------------------------------
    // Open application
    // ------------------------------------------------------------------------

    const openApplication =
        async app => {

            setSelectedApp(app);

            setVerificationData(null);
            setBeneficiary(null);
            setDocuments([]);
            setFieldCriteria([]);
            setSupportingFieldCriteria([]);
            setProofViewedMap({});
            setActionError('');

            setCriteriaLoading(true);

            try {

                const requests = [
                    fetchVerificationStatus(
                        app.applicationId
                    ),
                    fetchBeneficiary(
                        app.beneficiaryId
                    ),
                    fetchBeneficiaryDocuments(
                        app.beneficiaryId
                    ),
                ];

                if (
                    officerRole === ROLES.DISTRICT_OFFICER ||
                    officerRole === ROLES.FINANCE_APPROVER
                ) {
                    requests.push(
                        fetchVerificationCriteria(
                            app.applicationId,
                            'FIELD'
                        )
                    );
                }

                if (
                    officerRole === ROLES.FINANCE_APPROVER
                ) {
                    requests.push(
                        fetchVerificationCriteria(
                            app.applicationId,
                            'DISTRICT'
                        )
                    );
                }

                const reviewStage =
                    officerRole === ROLES.FIELD_OFFICER
                        ? 'FIELD'
                        : officerRole === ROLES.DISTRICT_OFFICER
                            ? 'DISTRICT'
                            : 'FINANCE';

                requests.push(
                    fetchVerificationCriteria(
                        app.applicationId,
                        reviewStage
                    )
                );

                const results =
                    await Promise.all(
                        requests
                    );

                setVerificationData(
                    results[0]
                );

                setBeneficiary(
                    results[1]
                );

                setDocuments(
                    results[2]
                );

                if (officerRole === ROLES.FIELD_OFFICER) {
                    setFieldCriteria(
                        Array.isArray(results[3])
                            ? results[3]
                            : []
                    );

                    setSupportingFieldCriteria([]);
                } else if (
                    officerRole === ROLES.DISTRICT_OFFICER
                ) {
                    setSupportingFieldCriteria(
                        Array.isArray(results[3])
                            ? results[3]
                            : []
                    );

                    setFieldCriteria(
                        Array.isArray(results[4])
                            ? results[4]
                            : []
                    );
                } else {
                    setSupportingFieldCriteria(
                        [
                            ...(Array.isArray(results[3]) ? results[3] : []),
                            ...(Array.isArray(results[4]) ? results[4] : []),
                        ]
                    );

                    setFieldCriteria(
                        Array.isArray(results[5])
                            ? results[5]
                            : []
                    );
                }

            } catch (err) {

                setActionError(
                    err.message ||
                    'Failed to load application details.'
                );

            } finally {

                setCriteriaLoading(false);
            }
        };

    // ------------------------------------------------------------------------
    // Close application review
    // ------------------------------------------------------------------------

    const closeApplication =
        () => {

            setSelectedApp(null);
            setVerificationData(null);
            setBeneficiary(null);
            setDocuments([]);
            setFieldCriteria([]);
            setSupportingFieldCriteria([]);
            setProofViewedMap({});
            setActionError('');

        };

    // ------------------------------------------------------------------------
    // Proof reviewed
    // ------------------------------------------------------------------------

    const handleProofViewed =
        (
            criterionId,
            documentId,
            openVerify = false
        ) => {

            setProofViewedMap(
                previous => ({
                    ...previous,
                    [criterionId]: true,
                })
            );

            if (
                openVerify &&
                selectedApp
            ) {

                const criterion =
                    fieldCriteria.find(
                        c =>
                            c.id ===
                            criterionId
                    );

                if (criterion) {

                    setPendingCriterion({
                        criterion,
                        app: selectedApp,
                    });

                }
            }
        };

    // ------------------------------------------------------------------------
    // Criterion verify
    // ------------------------------------------------------------------------

    const handleCriterionVerify =
        async ({
                   criterion,
                   status,
                   remarks,
               }) => {

            if (!selectedApp) {
                return;
            }

            setCriterionLoading(true);
            setActionError('');

            try {

                await updateVerificationCriterion(
                    selectedApp.applicationId,
                    criterion.id,
                    officerUsername,
                    status,
                    remarks
                );

                const reviewStage =
                    officerRole === ROLES.FIELD_OFFICER
                        ? 'FIELD'
                        : officerRole === ROLES.DISTRICT_OFFICER
                            ? 'DISTRICT'
                            : officerRole === ROLES.FINANCE_APPROVER
                                ? 'FINANCE'
                                : null;

                if (!reviewStage) {
                    throw new Error('Unable to determine verification stage for the current officer.');
                }

                const updatedCriteria =
                    await fetchVerificationCriteria(
                        selectedApp.applicationId,
                        reviewStage
                    );

                setFieldCriteria(
                    Array.isArray(
                        updatedCriteria
                    )
                        ? updatedCriteria
                        : []
                );

                const updatedStatus =
                    await fetchVerificationStatus(
                        selectedApp.applicationId
                    );

                setVerificationData(
                    updatedStatus
                );

                setSuccessMsg(
                    status === 'VERIFIED'
                        ? 'Criterion verified successfully.'
                        : 'Criterion marked as failed.'
                );

                setPendingCriterion(null);

            } catch (err) {

                setActionError(
                    err.message ||
                    'Failed to update criterion.'
                );

            } finally {

                setCriterionLoading(false);
            }
        };

    // ------------------------------------------------------------------------
    // Criterion fail
    // ------------------------------------------------------------------------

    const handleCriterionFail =
        criterion => {

            setPendingCriterion({
                criterion,
                app: selectedApp,
            });

            setActionError('');
        };

    // ------------------------------------------------------------------------
    // Open verification action
    // ------------------------------------------------------------------------

    const openVerificationAction =
        (
            app,
            action
        ) => {

            setActionError('');
            setSuccessMsg('');

            setPendingAction({
                app,
                action,
                requiresRemarks:
                    REMARKS_REQUIRED_ACTIONS.has(
                        action
                    ),
            });
        };

    // ------------------------------------------------------------------------
    // Application action
    // ------------------------------------------------------------------------

    const handleActionClick =
        (
            app,
            action,
            requiresRemarks = false
        ) => {

            setActionError('');
            setSuccessMsg('');

            setPendingAction({
                app,
                action,
                requiresRemarks,
            });
        };

    // ------------------------------------------------------------------------
    // Action modal confirm
    // ------------------------------------------------------------------------

    const handleModalConfirm =
        async remarks => {

            if (!pendingAction) {
                return;
            }

            const {
                app,
                action,
            } = pendingAction;

            setActionLoading(true);
            setActionError('');

            try {

                await postVerificationAction(
                    app.applicationId,
                    action,
                    officerUsername,
                    remarks
                );

                setSuccessMsg(
                    `Action "${action.replace(
                        /-/g,
                        ' '
                    )}" completed successfully.`
                );

                setPendingAction(null);

                const updated =
                    await fetchVerificationStatus(
                        app.applicationId
                    );

                setVerificationData(
                    updated
                );

                await loadApplications();

                if (
                    selectedApp &&
                    selectedApp.applicationId ===
                    app.applicationId
                ) {

                    const refreshed =
                        applications.find(
                            item =>
                                item.applicationId ===
                                app.applicationId
                        );

                    if (refreshed) {
                        setSelectedApp(
                            refreshed
                        );
                    }
                }

            } catch (err) {

                setActionError(
                    err.message ||
                    'Action failed.'
                );

                setPendingAction(null);

            } finally {

                setActionLoading(false);
            }
        };

    // ------------------------------------------------------------------------
    // Logout
    // ------------------------------------------------------------------------

    const handleLogout =
        () => {

            [
                'officerLoggedIn',
                'officerId',
                'officerUsername',
                'officerName',
                'officerRole',
                'officerDistrict',
            ].forEach(
                key =>
                    localStorage.removeItem(
                        key
                    )
            );

            navigate(
                '/officer/login',
                { replace: true }
            );
        };

    // ------------------------------------------------------------------------
    // Full-screen review
    // ------------------------------------------------------------------------

    if (selectedApp) {

        return (
            <div className="od-page">

                <div className="od-topbar">

                    <div className="od-brand">

                        <div className="od-brand-mark">
                            🏛️
                        </div>

                        <div>
                            <strong>
                                DSGP
                            </strong>

                            <span>
                                Digital Subsidy & Grant Platform
                            </span>
                        </div>

                    </div>

                    <div className="od-topbar-right">

                        <div className="od-officer-info">

                            <strong>
                                {officerName}
                            </strong>

                            <span>
                                {ROLE_LABELS[
                                    officerRole
                                    ] || officerRole}
                                {officerDistrict
                                    ? ` · ${officerDistrict}`
                                    : ''}
                            </span>

                        </div>

                        <button
                            type="button"
                            className="od-logout"
                            onClick={
                                handleLogout
                            }
                        >
                            Logout
                        </button>

                    </div>

                </div>

                {successMsg && (
                    <div className="od-floating-success">
                        ✓ {successMsg}
                    </div>
                )}

                {actionError && (
                    <div className="od-floating-error">
                        ⚠️ {actionError}
                    </div>
                )}

                <ApplicationReview
                    app={selectedApp}
                    verificationData={
                        verificationData
                    }
                    beneficiary={
                        beneficiary
                    }
                    documents={
                        documents
                    }
                    criteria={
                        fieldCriteria
                    }
                    supportingFieldCriteria={
                        supportingFieldCriteria
                    }
                    criteriaLoading={
                        criteriaLoading
                    }
                    proofViewedMap={
                        proofViewedMap
                    }
                    onProofViewed={
                        handleProofViewed
                    }
                    onCriterionVerify={
                        criterion => {
                            setPendingCriterion({
                                criterion,
                                app: selectedApp,
                            });
                        }
                    }
                    onCriterionFail={
                        handleCriterionFail
                    }
                    onBack={
                        closeApplication
                    }
                    onAction={
                        handleActionClick
                    }
                    actionLoading={
                        actionLoading
                    }
                    officerRole={
                        officerRole
                    }
                />

                {pendingAction && (
                    <RemarksModal
                        action={
                            pendingAction.action
                        }
                        app={
                            pendingAction.app
                        }
                        onConfirm={
                            handleModalConfirm
                        }
                        onCancel={() =>
                            setPendingAction(null)
                        }
                        loading={
                            actionLoading
                        }
                    />
                )}

                {pendingCriterion && (
                    <CriterionModal
                        criterion={
                            pendingCriterion.criterion
                        }
                        app={
                            pendingCriterion.app
                        }
                        proofReviewed={
                            Boolean(
                                proofViewedMap[
                                    pendingCriterion
                                        .criterion
                                        .id
                                    ]
                            )
                        }
                        onConfirm={
                            handleCriterionVerify
                        }
                        onCancel={() =>
                            setPendingCriterion(null)
                        }
                        loading={
                            criterionLoading
                        }
                    />
                )}

            </div>
        );
    }

    // ------------------------------------------------------------------------
    // MAIN QUEUE
    // ------------------------------------------------------------------------

    return (
        <div className="od-page">

            {/* ================================================================
                TOP BAR
            ================================================================ */}

            <header className="od-topbar">

                <div className="od-brand">

                    <div className="od-brand-mark">
                        🏛️
                    </div>

                    <div>
                        <strong>
                            DSGP
                        </strong>

                        <span>
                            Digital Subsidy & Grant Platform
                        </span>
                    </div>

                </div>

                <div className="od-topbar-right">

                    <div className="od-officer-info">

                        <strong>
                            {officerName}
                        </strong>

                        <span>
                            {ROLE_LABELS[
                                officerRole
                                ] || officerRole}

                            {officerDistrict
                                ? ` · ${officerDistrict}`
                                : ''}
                        </span>

                    </div>

                    <button
                        type="button"
                        className="od-logout"
                        onClick={
                            handleLogout
                        }
                    >
                        Logout
                    </button>

                </div>

            </header>

            {/* ================================================================
                NOTIFICATIONS
            ================================================================ */}

            {successMsg && (
                <div className="od-notification od-notification-success">
                    <span>✓</span>
                    {successMsg}
                </div>
            )}

            {actionError && (
                <div className="od-notification od-notification-error">
                    <span>⚠️</span>
                    {actionError}
                </div>
            )}

            {/* ================================================================
                MAIN
            ================================================================ */}

            <main className="od-main">

                <section className="od-dashboard-heading">

                    <div>

                        <span className="od-dashboard-kicker">
                            OFFICER WORKSPACE
                        </span>

                        <h1>
                            Verification Queue
                        </h1>

                        <p>
                            Review beneficiary applications
                            assigned to your role.
                        </p>

                    </div>

                    <div className="od-queue-stat">

                        <strong>
                            {applications.length}
                        </strong>

                        <span>
                            Applications
                        </span>

                    </div>

                </section>

                {/* ============================================================
                    ERROR
                ============================================================ */}

                {fetchError && (

                    <div className="od-notification od-notification-error">
                        <span>⚠️</span>
                        {fetchError}
                    </div>

                )}

                {/* ============================================================
                    LOADING
                ============================================================ */}

                {loading ? (

                    <div className="od-loading-card od-main-loading">

                        <div className="od-spinner" />

                        <p>
                            Loading verification queue...
                        </p>

                    </div>

                ) : applications.length === 0 ? (

                    <div className="od-empty-card od-queue-empty">

                        <span>
                            ✓
                        </span>

                        <h2>
                            No Applications
                        </h2>

                        <p>
                            There are currently no applications
                            waiting for your review.
                        </p>

                    </div>

                ) : (

                    <section className="od-queue-card">

                        <div className="od-queue-card-header">

                            <div>
                                <h2>
                                    Applications
                                </h2>

                                <p>
                                    Click an application to open
                                    the full verification workspace.
                                </p>
                            </div>

                            <span className="od-role-chip">
                                {ROLE_LABELS[
                                    officerRole
                                    ] || officerRole}
                            </span>

                        </div>

                        <div className="od-table-wrapper">

                            <table className="od-table">

                                <thead>

                                <tr>

                                    <th>
                                        Application
                                    </th>

                                    <th>
                                        Beneficiary
                                    </th>

                                    <th>
                                        Scheme
                                    </th>

                                    <th>
                                        Eligibility
                                    </th>

                                    <th>
                                        Submitted
                                    </th>

                                    <th>
                                        Status
                                    </th>

                                    <th>
                                        Action
                                    </th>

                                </tr>

                                </thead>

                                <tbody>

                                {applications.map(
                                    app => (
                                        <tr
                                            key={
                                                app.applicationId
                                            }
                                            className="od-table-row"
                                            onClick={() =>
                                                openApplication(
                                                    app
                                                )
                                            }
                                        >

                                            <td>
                                                <strong className="od-app-id">
                                                    #
                                                    {
                                                        app.applicationId
                                                    }
                                                </strong>
                                            </td>

                                            <td>
                                                <div className="od-beneficiary-cell">

                                                    <div className="od-avatar">
                                                        {(
                                                            app.beneficiaryName ||
                                                            'B'
                                                        )
                                                            .charAt(
                                                                0
                                                            )
                                                            .toUpperCase()}
                                                    </div>

                                                    <div>
                                                        <strong>
                                                            {
                                                                app.beneficiaryName
                                                            }
                                                        </strong>

                                                        <span>
                                                            Click to review
                                                        </span>
                                                    </div>

                                                </div>
                                            </td>

                                            <td>
                                                <span className="od-scheme-chip">
                                                    {
                                                        app.schemeName
                                                    }
                                                </span>
                                            </td>

                                            <td>
                                                <ScoreBadge
                                                    score={
                                                        app.eligibilityScore
                                                    }
                                                />
                                            </td>

                                            <td>
                                                <span className="od-date">
                                                    {formatDate(
                                                        app.applicationDate
                                                    )}
                                                </span>
                                            </td>

                                            <td>
                                                <StatusBadge
                                                    status={
                                                        app.applicationStatus
                                                    }
                                                />
                                            </td>

                                            <td
                                                onClick={e =>
                                                    e.stopPropagation()
                                                }
                                            >

                                                {officerRole === ROLES.FIELD_OFFICER &&
                                                app.applicationStatus === 'PENDING' ? (

                                                    <button
                                                        type="button"
                                                        className="..."
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            openVerificationAction(app, 'start');
                                                        }}
                                                    >
                                                        ▶ Start Verification
                                                    </button>

                                                ) : (

                                                    <button
                                                        type="button"
                                                        className="od-open-button"
                                                        onClick={() =>
                                                            openApplication(app)
                                                        }
                                                    >
                                                        Review →
                                                    </button>

                                                )}
                                            </td>

                                        </tr>
                                    )
                                )}

                                </tbody>

                            </table>

                        </div>

                    </section>

                )}

            </main>

        </div>
    );
}

export default OfficerDashboard;

