
import React, { useEffect, useState } from "react";
import "./DisbursementStagePanel.css";

const API_BASE = "/api/v1/api/disbursements";
const BENEFICIARY_API_BASE = "/api/v1/beneficiaries";

const getComplianceInfo = (stage) => {
    const status = stage.complianceStatus || "PENDING";

    if (status === "COMPLETED") {
        return {
            type: "completed",
            label: "Compliance Completed",
            message: "All required compliance conditions have been verified."
        };
    }

    if (status === "NON_COMPLIANT") {
        return {
            type: "non-compliant",
            label: "Non-Compliant",
            message: "The required compliance conditions were not satisfied."
        };
    }

    if (!stage.dueDate) {
        return {
            type: "pending",
            label: "Compliance Pending",
            message: "Compliance verification is still pending."
        };
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const dueDate = new Date(stage.dueDate);
    dueDate.setHours(0, 0, 0, 0);

    const differenceInDays = Math.ceil(
        (dueDate - today) / (1000 * 60 * 60 * 24)
    );

    if (differenceInDays < 0) {
        return {
            type: "overdue",
            label: "Compliance Overdue",
            message: `${Math.abs(differenceInDays)} day(s) overdue.`
        };
    }

    if (differenceInDays === 0) {
        return {
            type: "due-today",
            label: "Compliance Due Today",
            message: "Compliance verification is due today."
        };
    }

    return {
        type: "pending",
        label: "Compliance Pending",
        message: `Due in ${differenceInDays} day(s).`
    };
};

function formatAmount(value) {
    if (value === null || value === undefined || value === "") {
        return "₹0.00";
    }

    return `₹${Number(value).toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })}`;
}

function formatDate(value) {
    if (!value) return "-";

    try {
        return new Date(value).toLocaleDateString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric"
        });
    } catch {
        return value;
    }
}


const getStageEvidenceInstructions = (stageNumber) => {
    switch (Number(stageNumber)) {
        case 1:
            return {
                title: "Initial Verification Documents",
                description:
                    "Verify the beneficiary's eligibility and initial setup before releasing Stage 1.",
                items: [
                    "Identity / beneficiary proof",
                    "Bank account proof",
                    "Approval / sanction / setup documents"
                ]
            };

        case 2:
            return {
                title: "Proof of Stage 1 Fund Utilization",
                description:
                    "Verify that the Stage 1 funds were used for the approved purchase or activity.",
                items: [
                    "Equipment or purchase invoice",
                    "Payment receipt or transaction proof",
                    "Photos of purchased equipment or completed activity"
                ]
            };

        case 3:
            return {
                title: "Final Completion & Previous Fund Utilization",
                description:
                    "Verify completion of the approved project and proper utilization of the funds already released in Stages 1 and 2.",
                items: [
                    "Final completion report",
                    "Utilization / expenditure statement for previously released funds",
                    "Final project / equipment photos",
                    "Remaining invoices or payment proofs, if applicable"
                ]
            };

        default:
            return {
                title: "Required Evidence",
                description:
                    "Verify the documents and evidence required for this disbursement stage.",
                items: [
                    "Required supporting documents",
                    "Payment / expenditure proof",
                    "Relevant project evidence"
                ]
            };
    }
};

const getStageDocumentRequirements = (stageNumber) => {
    switch (Number(stageNumber)) {
        case 2:
            return [
                {
                    type: "STAGE_2_INVOICE",
                    title: "Equipment / Purchase Invoice",
                    required: true
                },
                {
                    type: "STAGE_2_PAYMENT_PROOF",
                    title: "Payment / Transaction Proof",
                    required: true
                },
                {
                    type: "STAGE_2_ACTIVITY_PHOTO",
                    title: "Equipment / Activity Photos",
                    required: true
                }
            ];

        case 3:
            return [
                {
                    type: "FINAL_COMPLETION_REPORT",
                    title: "Final Completion Report",
                    required: true
                },
                {
                    type: "UTILIZATION_STATEMENT",
                    title: "Utilization / Expenditure Statement",
                    required: true
                },
                {
                    type: "FINAL_PROJECT_PHOTO",
                    title: "Final Project / Equipment Photos",
                    required: true
                },
                {
                    type: "FINAL_PAYMENT_PROOF",
                    title: "Remaining Invoice / Payment Proof",
                    required: false
                }
            ];

        default:
            return [];
    }
};

export default function DisbursementStagePanel({ application }) {

    const applicationId =
        application?.applicationId ?? application?.id;

    const beneficiaryId =
        application?.beneficiaryId ?? application?.beneficiary?.id;

    const [plan, setPlan] = useState(null);
    const [stages, setStages] = useState([]);

    const [stageDocuments, setStageDocuments] = useState({});
    const [documentsLoading, setDocumentsLoading] = useState(false);

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    /*
     * Temporary compliance form data for each stage.
     *
     * Example:
     * {
     *   1: {
     *      status: "COMPLETED",
     *      remarks: "Documents verified.",
     *      verifiedBy: "Finance Officer"
     *   }
     * }
     */
    const [complianceForms, setComplianceForms] = useState({});

    const planId = plan?.id || null;

    /*
     * Load the disbursement plan and all stages.
     */
    const loadPlanAndStages = async () => {

        if (!applicationId) {
            setPlan(null);
            setStages([]);
            setError("Application ID is not available.");
            return;
        }

        setLoading(true);
        setError("");

        try {

            /*
             * Step 1:
             * Find the disbursement plan linked to this application.
             */
            const planResponse = await fetch(
                `${API_BASE}/application/${applicationId}/plan`
            );

            if (planResponse.status === 404) {
                setPlan(null);
                setStages([]);
                setError(
                    "No disbursement plan exists for this approved application."
                );
                return;
            }

            if (!planResponse.ok) {
                throw new Error(
                    "Unable to load the disbursement plan."
                );
            }

            const planData = await planResponse.json();

            setPlan(planData);

            /*
             * Step 2:
             * Once we have the plan ID, load its stages.
             */
            if (!planData?.id) {
                setStages([]);

                throw new Error(
                    "Disbursement plan ID was not returned by the server."
                );
            }

            const stagesResponse = await fetch(
                `${API_BASE}/${planData.id}/stages`
            );

            if (!stagesResponse.ok) {
                throw new Error(
                    "Unable to load disbursement stages."
                );
            }

            const stagesData = await stagesResponse.json();

            const loadedStages = Array.isArray(stagesData)
                ? stagesData
                : [];

            setStages(loadedStages);

            /*
             * Load existing compliance information into the form.
             */
            const existingForms = {};

            loadedStages.forEach((stage) => {
                existingForms[stage.id] = {
                    status: stage.complianceStatus || "PENDING",
                    remarks: stage.complianceRemarks || "",
                    verifiedBy: stage.complianceVerifiedBy || ""
                };
            });

            setComplianceForms(existingForms);

        } catch (err) {

            console.error(
                "Disbursement loading error:",
                err
            );

            setError(
                err.message ||
                "Unable to load disbursement data."
            );

        } finally {
            setLoading(false);
        }
    };

    const loadStageDocuments = async (loadedStages = stages) => {

        if (!beneficiaryId || !applicationId) {
            setStageDocuments({});
            return;
        }

        const stageNumbers = loadedStages
            .map((stage) => Number(stage.stageNumber))
            .filter((stageNumber) => stageNumber === 2 || stageNumber === 3);

        if (stageNumbers.length === 0) {
            setStageDocuments({});
            return;
        }

        setDocumentsLoading(true);

        try {

            const results = await Promise.all(
                stageNumbers.map(async (stageNumber) => {
                    const response = await fetch(
                        `${BENEFICIARY_API_BASE}/${beneficiaryId}/documents/application/${applicationId}/stage/${stageNumber}`
                    );

                    if (!response.ok) {
                        return [stageNumber, []];
                    }

                    const data = await response.json();
                    return [
                        stageNumber,
                        Array.isArray(data) ? data : []
                    ];
                })
            );

            setStageDocuments(
                Object.fromEntries(results)
            );

        } catch (err) {
            console.error(
                "Stage document loading error:",
                err
            );
            setStageDocuments({});
        } finally {
            setDocumentsLoading(false);
        }
    };

    const getStageDocument = (stageNumber, documentType) => {
        const documents =
            stageDocuments[Number(stageNumber)] || [];

        return documents.find(
            (document) =>
                document.documentType === documentType
        );
    };

    const handleDocumentDownload = (documentId) => {
        if (!beneficiaryId || !documentId) {
            return;
        }

        window.open(
            `${BENEFICIARY_API_BASE}/${beneficiaryId}/documents/${documentId}/download`,
            "_blank"
        );
    };

    const hasRequiredStageDocuments = (stageNumber) => {
        const requirements =
            getStageDocumentRequirements(stageNumber)
                .filter((item) => item.required);

        return requirements.every((requirement) =>
            Boolean(
                getStageDocument(
                    stageNumber,
                    requirement.type
                )
            )
        );
    };

    useEffect(() => {

        if (applicationId) {
            loadPlanAndStages();
        } else {
            setPlan(null);
            setStages([]);
            setError("Application ID is not available.");
        }

    }, [applicationId]);

    useEffect(() => {
        if (applicationId && beneficiaryId && stages.length > 0) {
            loadStageDocuments(stages);
        }
    }, [applicationId, beneficiaryId, stages]);

    /*
     * Calculate released amount from released stages.
     */
    const releasedAmount = stages.reduce(
        (sum, stage) =>
            stage.status === "RELEASED"
                ? sum + Number(stage.amount || 0)
                : sum,
        0
    );

    /*
     * Calculate total amount allocated to stages.
     */
    const plannedAmount = stages.reduce(
        (sum, stage) =>
            sum + Number(stage.amount || 0),
        0
    );

    /*
     * Prefer the amount stored in the disbursement plan.
     * Fall back to the application if necessary.
     */
    const sanctionedAmount =
        plan?.totalAmount ??
        application?.sanctionedAmount ??
        application?.scheme?.grantAmount ??
        0;

    /*
     * Remaining amount to be released.
     */
    const remainingAmount = Math.max(
        Number(sanctionedAmount) - releasedAmount,
        0
    );

    const complianceAlerts = stages
        .map((stage) => ({
            stage,
            info: getComplianceInfo(stage)
        }))
        .filter(({ info }) =>
            info.type === "overdue" ||
            info.type === "due-today" ||
            info.type === "non-compliant"
        );

    /*
     * Create a disbursement plan.
     */
    const handleCreatePlan = async () => {

        if (!applicationId) {
            setError("Application ID is not available.");
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        try {

            const response = await fetch(
                `${API_BASE}/application/${applicationId}/plan?type=STAGED`,
                {
                    method: "POST"
                }
            );

            const data = await response
                .json()
                .catch(() => null);

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.error ||
                    "Unable to create disbursement plan."
                );
            }

            setPlan(data);

            setSuccess(
                "Disbursement plan created successfully."
            );

            await loadPlanAndStages();

        } catch (err) {

            console.error(
                "Create disbursement plan error:",
                err
            );

            setError(
                err.message ||
                "Unable to create disbursement plan."
            );

        } finally {
            setSaving(false);
        }
    };

    /*
     * Update a compliance form field.
     */
    const updateComplianceForm = (
        stageId,
        field,
        value
    ) => {

        setComplianceForms((previous) => ({
            ...previous,

            [stageId]: {
                ...(previous[stageId] || {
                    status: "PENDING",
                    remarks: "",
                    verifiedBy: ""
                }),

                [field]: value
            }
        }));
    };

    /*
     * Save compliance review.
     */
    const handleComplianceUpdate = async (stageId) => {

        const form = complianceForms[stageId];

        if (!form) {
            setError("Compliance information is not available.");
            return;
        }

        if (!form.status) {
            setError("Please select a compliance status.");
            return;
        }

        if (!form.remarks?.trim()) {
            setError("Please enter compliance remarks.");
            return;
        }

        if (!form.verifiedBy?.trim()) {
            setError("Please enter the name of the verifying officer.");
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        try {

            const params = new URLSearchParams({
                status: form.status,
                remarks: form.remarks.trim(),
                verifiedBy: form.verifiedBy.trim()
            });

            const response = await fetch(
                `${API_BASE}/stages/${stageId}/compliance?${params.toString()}`,
                {
                    method: "PUT"
                }
            );

            const data = await response
                .json()
                .catch(() => null);

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.error ||
                    "Unable to update compliance status."
                );
            }

            setSuccess(
                form.status === "COMPLETED"
                    ? "Compliance review completed successfully."
                    : form.status === "NON_COMPLIANT"
                        ? "Stage marked as non-compliant."
                        : "Compliance status saved successfully."
            );

            await loadPlanAndStages();

        } catch (err) {

            console.error(
                "Compliance update error:",
                err
            );

            setError(
                err.message ||
                "Unable to update compliance status."
            );

        } finally {
            setSaving(false);
        }
    };

    /*
     * Verify a stage.
     *
     * Backend will also enforce that compliance
     * must be COMPLETED.
     */
    const handleVerify = async (stageId) => {

        const stage = stages.find(
            (item) => item.id === stageId
        );

        if (stage?.complianceStatus !== "COMPLETED") {
            setError(
                "Compliance must be completed before verifying this stage."
            );
            return;
        }

        const stageNumber = Number(stage?.stageNumber);

        if (
            (stageNumber === 2 || stageNumber === 3) &&
            !hasRequiredStageDocuments(stageNumber)
        ) {
            setError(
                `Please review and ensure all required Stage ${stageNumber} documents have been uploaded before verifying this stage.`
            );
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        try {

            const response = await fetch(
                `${API_BASE}/stages/${stageId}/verify`,
                {
                    method: "PUT"
                }
            );

            const data = await response
                .json()
                .catch(() => null);

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.error ||
                    "Unable to verify stage."
                );
            }

            setSuccess(
                "Stage verified successfully."
            );

            await loadPlanAndStages();

        } catch (err) {

            console.error(
                "Verify stage error:",
                err
            );

            setError(
                err.message ||
                "Unable to verify stage."
            );

        } finally {
            setSaving(false);
        }
    };

    /*
     * Release a verified stage.
     */
    const handleRelease = async (stageId) => {

        const confirmed = window.confirm(
            "Are you sure you want to release this stage?"
        );

        if (!confirmed) return;

        setSaving(true);
        setError("");
        setSuccess("");

        try {

            const response = await fetch(
                `${API_BASE}/stages/${stageId}/release`,
                {
                    method: "PUT"
                }
            );

            const data = await response
                .json()
                .catch(() => null);

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.error ||
                    "Unable to release stage."
                );
            }

            setSuccess(
                "Stage released successfully."
            );

            await loadPlanAndStages();

        } catch (err) {

            console.error(
                "Release stage error:",
                err
            );

            setError(
                err.message ||
                "Unable to release stage."
            );

        } finally {
            setSaving(false);
        }
    };

    /*
     * No application ID.
     */
    if (!applicationId) {
        return (
            <section className="disbursement-panel">

                <div className="disbursement-header">
                    <div>
                        <h3>Disbursement</h3>

                        <p>
                            Manage staged disbursement for this
                            approved application.
                        </p>
                    </div>

                    <span className="disbursement-badge warning">
                        APPLICATION ID NOT AVAILABLE
                    </span>
                </div>

                <div className="disbursement-warning">
                    <strong>
                        Application ID is not available.
                    </strong>

                    <p>
                        The approved application does not contain
                        a valid application ID, so its disbursement
                        plan cannot be loaded.
                    </p>
                </div>

            </section>
        );
    }

    /*
     * Application exists but no disbursement plan exists.
     */
    if (!planId && !loading) {
        return (
            <section className="disbursement-panel">

                <div className="disbursement-header">
                    <div>
                        <h3>Disbursement</h3>

                        <p>
                            Manage staged disbursement for this
                            approved application.
                        </p>
                    </div>

                    <span className="disbursement-badge warning">
                        PLAN NOT AVAILABLE
                    </span>
                </div>

                {error && (
                    <div className="disbursement-alert error">
                        {error}
                    </div>
                )}

                <div className="disbursement-warning">

                    <strong>
                        Disbursement plan not found.
                    </strong>

                    <p>
                        No disbursement plan is currently linked
                        to this approved application.
                    </p>

                    <button
                        type="button"
                        className="stage-primary-button"
                        onClick={handleCreatePlan}
                        disabled={saving}
                    >
                        {saving
                            ? "Creating Plan..."
                            : "Create Disbursement Plan"}
                    </button>

                </div>

            </section>
        );
    }

    return (
        <section className="disbursement-panel">

            {/* HEADER */}
            <div className="disbursement-header">

                <div>
                    <h3>Staged Disbursement</h3>

                    <p>
                        Automatically generated disbursement schedule
                        for this approved application.
                    </p>
                </div>

                <span className="disbursement-badge">
                    PLAN #{planId}
                </span>

            </div>

            {/* ERROR */}
            {error && (
                <div className="disbursement-alert error">
                    {error}
                </div>
            )}

            {/* SUCCESS */}
            {success && (
                <div className="disbursement-alert success">
                    {success}
                </div>
            )}

            {/* SUMMARY */}
            <div className="disbursement-summary">

                <div className="disbursement-summary-card">
                    <span>Sanctioned Amount</span>

                    <strong>
                        {formatAmount(sanctionedAmount)}
                    </strong>
                </div>

                <div className="disbursement-summary-card">
                    <span>Planned Amount</span>

                    <strong>
                        {formatAmount(plannedAmount)}
                    </strong>
                </div>

                <div className="disbursement-summary-card">
                    <span>Released Amount</span>

                    <strong>
                        {formatAmount(releasedAmount)}
                    </strong>
                </div>

                <div className="disbursement-summary-card">
                    <span>Remaining</span>

                    <strong>
                        {formatAmount(remainingAmount)}
                    </strong>
                </div>

                <div className="disbursement-summary-card">
                    <span>Plan Status</span>

                    <strong>
                        {plan?.status ?? "-"}
                    </strong>
                </div>

                <div className="disbursement-summary-card">
                    <span>Disbursement Type</span>

                    <strong>
                        {plan?.disbursementType ?? "-"}
                    </strong>
                </div>

            </div>

            {/* COMPLIANCE ALERTS */}
            {complianceAlerts.length > 0 && (
                <div className="compliance-alerts-section">

                    <div className="compliance-alerts-header">

                        <h4>Compliance Alerts</h4>

                        <span>
                            {complianceAlerts.length} alert
                            {complianceAlerts.length > 1 ? "s" : ""}
                        </span>

                    </div>

                    <div className="compliance-alert-list">

                        {complianceAlerts.map(({ stage, info }) => (

                            <div
                                key={stage.id}
                                className={`compliance-alert-item ${info.type}`}
                            >

                                <div className="compliance-alert-icon">
                                    !
                                </div>

                                <div className="compliance-alert-content">

                                    <strong>
                                        Stage {stage.stageNumber} — {info.label}
                                    </strong>

                                    <span>
                                        {info.message}
                                    </span>

                                    <small>
                                        Milestone: {stage.milestone || "-"}
                                    </small>

                                </div>

                            </div>

                        ))}

                    </div>

                </div>
            )}

            {/* AUTOMATIC DISBURSEMENT SCHEDULE */}
            <div className="disbursement-section">

                <div className="disbursement-section-title">

                    <h4>Automatic Disbursement Schedule</h4>

                    <span>
                        System Generated
                    </span>

                </div>

                <div className="stage-complete-message">

                    <div className="stage-complete-icon">
                        ✓
                    </div>

                    <div className="stage-complete-content">

                        <strong>
                            Disbursement stages generated automatically
                        </strong>

                        <p>
                            The system has automatically allocated the full
                            sanctioned amount across the configured
                            disbursement stages.
                        </p>

                        <p>
                            Finance Officer must complete compliance review,
                            verify the stage, and then release the payment.
                        </p>

                    </div>

                </div>

            </div>

            {/* DISBURSEMENT STAGES */}
            <div className="disbursement-section">

                <div className="disbursement-section-title">

                    <h4>
                        Disbursement Stages
                    </h4>

                    <button
                        type="button"
                        className="stage-refresh-button"
                        onClick={loadPlanAndStages}
                        disabled={loading || saving}
                    >
                        {loading
                            ? "Loading..."
                            : "Refresh"}
                    </button>

                </div>

                {loading ? (

                    <div className="stage-empty-state">
                        Loading disbursement stages...
                    </div>

                ) : stages.length === 0 ? (

                    <div className="stage-empty-state">
                        No disbursement stages have been
                        created yet.
                    </div>

                ) : (

                    <div className="stage-list">

                        {stages
                            .slice()
                            .sort(
                                (a, b) =>
                                    Number(a.stageNumber || 0) -
                                    Number(b.stageNumber || 0)
                            )
                            .map((stage) => {

                                const complianceInfo =
                                    getComplianceInfo(stage);

                                const complianceForm =
                                    complianceForms[stage.id] || {
                                        status:
                                            stage.complianceStatus ||
                                            "PENDING",
                                        remarks:
                                            stage.complianceRemarks ||
                                            "",
                                        verifiedBy:
                                            stage.complianceVerifiedBy ||
                                            ""
                                    };

                                const complianceCompleted =
                                    stage.complianceStatus === "COMPLETED";

                                return (
                                    <div
                                        className="stage-card"
                                        key={stage.id}
                                    >

                                        {/* STAGE HEADER */}
                                        <div className="stage-card-top">

                                            <div className="stage-number">
                                                Stage{" "}
                                                {stage.stageNumber}
                                            </div>

                                            <div
                                                style={{
                                                    display: "flex",
                                                    alignItems: "center",
                                                    gap: "8px"
                                                }}
                                            >

                                                {stage.overdue && (
                                                    <span
                                                        className="stage-status"
                                                        style={{
                                                            background: "#7f1d1d",
                                                            color: "#fca5a5",
                                                            border: "1px solid #991b1b"
                                                        }}
                                                        title="This stage has passed its due date and has not been released."
                                                    >
                                                        ⚠ OVERDUE
                                                    </span>
                                                )}

                                                <span
                                                    className={`stage-status ${String(
                                                        stage.status || ""
                                                    ).toLowerCase()}`}
                                                >
                                                    {stage.status ||
                                                        "PENDING"}
                                                </span>

                                            </div>

                                        </div>

                                        {/* STAGE DETAILS */}
                                        <div className="stage-card-details">

                                            <div>
                                                <span>
                                                    Amount
                                                </span>

                                                <strong>
                                                    {formatAmount(
                                                        stage.amount
                                                    )}
                                                </strong>
                                            </div>

                                            <div>
                                                <span>
                                                    Milestone
                                                </span>

                                                <strong>
                                                    {stage.milestone ||
                                                        "-"}
                                                </strong>
                                            </div>

                                            <div>
                                                <span>
                                                    Due Date
                                                </span>

                                                <strong>
                                                    {formatDate(
                                                        stage.dueDate
                                                    )}

                                                    {stage.overdue && (
                                                        <span
                                                            style={{
                                                                marginLeft: "6px",
                                                                fontSize: "0.75rem",
                                                                color: "#fca5a5",
                                                                fontWeight: "600"
                                                            }}
                                                            title="This stage is overdue."
                                                        >
                                                            (Overdue)
                                                        </span>
                                                    )}

                                                </strong>
                                            </div>

                                            <div>
                                                <span>
                                                    Released At
                                                </span>

                                                <strong>
                                                    {formatDate(
                                                        stage.releasedAt
                                                    )}
                                                </strong>
                                            </div>

                                        </div>

                                        {/* STAGE-SPECIFIC EVIDENCE */}
                                        {(() => {
                                            const evidence = getStageEvidenceInstructions(stage.stageNumber);

                                            return (
                                                <div
                                                    style={{
                                                        marginTop: "18px",
                                                        padding: "14px",
                                                        borderRadius: "10px",
                                                        border: "1px solid #93c5fd",
                                                        background: "#eff6ff",
                                                        color: "#1e293b"
                                                    }}
                                                >
                                                    <strong style={{ color: "#1e3a8a" }}>
                                                        📋 {evidence.title}
                                                    </strong>

                                                    <p
                                                        style={{
                                                            margin: "6px 0 10px",
                                                            opacity: 0.9
                                                        }}
                                                    >
                                                        {evidence.description}
                                                    </p>

                                                    <ul
                                                        style={{
                                                            margin: 0,
                                                            paddingLeft: "20px"
                                                        }}
                                                    >
                                                        {evidence.items.map((item, index) => (
                                                            <li key={index} style={{ marginBottom: "5px" }}>
                                                                {item}
                                                            </li>
                                                        ))}
                                                    </ul>
                                                </div>
                                            );
                                        })()}

                                        {/* BENEFICIARY-UPLOADED STAGE DOCUMENTS */}
                                        {Number(stage.stageNumber) >= 2 && (
                                            <div
                                                style={{
                                                    marginTop: "18px",
                                                    padding: "16px",
                                                    borderRadius: "10px",
                                                    border: "1px solid #cbd5e1",
                                                    background: "#ffffff",
                                                    color: "#1e293b"
                                                }}
                                            >

                                                <div
                                                    style={{
                                                        display: "flex",
                                                        justifyContent: "space-between",
                                                        alignItems: "center",
                                                        gap: "10px",
                                                        flexWrap: "wrap",
                                                        marginBottom: "12px"
                                                    }}
                                                >
                                                    <div>
                                                        <strong
                                                            style={{
                                                                color: "#0f172a",
                                                                fontSize: "15px"
                                                            }}
                                                        >
                                                            📎 Beneficiary Uploaded Documents
                                                        </strong>

                                                        <p
                                                            style={{
                                                                margin: "5px 0 0",
                                                                color: "#64748b",
                                                                fontSize: "13px"
                                                            }}
                                                        >
                                                            Review the actual evidence submitted for Stage {stage.stageNumber} before completing the compliance review.
                                                        </p>
                                                    </div>

                                                    {documentsLoading && (
                                                        <span
                                                            style={{
                                                                fontSize: "12px",
                                                                color: "#64748b"
                                                            }}
                                                        >
                                                            Loading documents...
                                                        </span>
                                                    )}
                                                </div>

                                                {!beneficiaryId ? (
                                                    <div
                                                        style={{
                                                            padding: "10px",
                                                            borderRadius: "7px",
                                                            background: "#fef2f2",
                                                            color: "#991b1b",
                                                            fontSize: "13px"
                                                        }}
                                                    >
                                                        Beneficiary ID is not available for document retrieval.
                                                    </div>
                                                ) : (
                                                    <div
                                                        style={{
                                                            display: "grid",
                                                            gap: "9px"
                                                        }}
                                                    >
                                                        {getStageDocumentRequirements(stage.stageNumber).map((requirement) => {
                                                            const document = getStageDocument(
                                                                stage.stageNumber,
                                                                requirement.type
                                                            );

                                                            return (
                                                                <div
                                                                    key={requirement.type}
                                                                    style={{
                                                                        display: "flex",
                                                                        justifyContent: "space-between",
                                                                        alignItems: "center",
                                                                        gap: "12px",
                                                                        padding: "11px 12px",
                                                                        borderRadius: "8px",
                                                                        border: document
                                                                            ? "1px solid #bbf7d0"
                                                                            : "1px solid #e2e8f0",
                                                                        background: document
                                                                            ? "#f0fdf4"
                                                                            : "#f8fafc",
                                                                        flexWrap: "wrap"
                                                                    }}
                                                                >
                                                                    <div>
                                                                        <strong
                                                                            style={{
                                                                                display: "block",
                                                                                fontSize: "13px"
                                                                            }}
                                                                        >
                                                                            {document ? "✓" : "○"} {requirement.title}
                                                                            {!requirement.required && (
                                                                                <span
                                                                                    style={{
                                                                                        marginLeft: "6px",
                                                                                        fontSize: "11px",
                                                                                        color: "#64748b",
                                                                                        fontWeight: "normal"
                                                                                    }}
                                                                                >
                                                                                    (Optional)
                                                                                </span>
                                                                            )}
                                                                        </strong>

                                                                        {document ? (
                                                                            <span
                                                                                style={{
                                                                                    display: "block",
                                                                                    marginTop: "3px",
                                                                                    color: "#475569",
                                                                                    fontSize: "12px"
                                                                                }}
                                                                            >
                                                                                {document.originalFileName || document.fileName}
                                                                            </span>
                                                                        ) : (
                                                                            <span
                                                                                style={{
                                                                                    display: "block",
                                                                                    marginTop: "3px",
                                                                                    color: requirement.required
                                                                                        ? "#b45309"
                                                                                        : "#64748b",
                                                                                    fontSize: "12px"
                                                                                }}
                                                                            >
                                                                                {requirement.required
                                                                                    ? "Not uploaded"
                                                                                    : "Not uploaded — optional"}
                                                                            </span>
                                                                        )}
                                                                    </div>

                                                                    {document && (
                                                                        <button
                                                                            type="button"
                                                                            onClick={() =>
                                                                                handleDocumentDownload(
                                                                                    document.id
                                                                                )
                                                                            }
                                                                            style={{
                                                                                padding: "7px 12px",
                                                                                border: "1px solid #2563eb",
                                                                                borderRadius: "6px",
                                                                                background: "#ffffff",
                                                                                color: "#2563eb",
                                                                                fontWeight: "600",
                                                                                cursor: "pointer"
                                                                            }}
                                                                        >
                                                                            View / Download
                                                                        </button>
                                                                    )}
                                                                </div>
                                                            );
                                                        })}
                                                    </div>
                                                )}

                                                <div
                                                    style={{
                                                        marginTop: "12px",
                                                        fontSize: "12px",
                                                        color: "#64748b"
                                                    }}
                                                >
                                                    Stage {stage.stageNumber} documents are linked specifically to Application #{applicationId}.
                                                </div>

                                            </div>
                                        )}


                                        {/* COMPLIANCE STATUS */}
                                        <div
                                            style={{
                                                marginTop: "18px",
                                                padding: "14px",
                                                borderRadius: "10px",
                                                border: "1px solid rgba(148, 163, 184, 0.25)",
                                                background: "#f1f5f9",
                                                color: "#1e293b"
                                            }}
                                        >

                                            <div
                                                style={{
                                                    display: "flex",
                                                    justifyContent: "space-between",
                                                    alignItems: "center",
                                                    gap: "12px",
                                                    marginBottom: "10px"
                                                }}
                                            >

                                                <div>
                                                    <span className="compliance-label">
                                                        Compliance Review
                                                    </span>

                                                    <strong
                                                        className={`compliance-status ${complianceInfo.type}`}
                                                        style={{
                                                            display: "block",
                                                            marginTop: "4px",
                                                            color:
                                                                complianceInfo.type === "completed"
                                                                    ? "#15803d"
                                                                    : complianceInfo.type === "non-compliant"
                                                                        ? "#b91c1c"
                                                                        : "#b45309"
                                                        }}
                                                    >
                                                        {complianceInfo.label}
                                                    </strong>
                                                </div>

                                                {stage.complianceVerifiedAt && (
                                                    <small>
                                                        Reviewed on{" "}
                                                        {formatDate(
                                                            stage.complianceVerifiedAt
                                                        )}
                                                    </small>
                                                )}

                                            </div>

                                            <small
                                                className={`compliance-message ${complianceInfo.type}`}
                                                style={{
                                                    display: "block",
                                                    marginBottom: "14px"
                                                }}
                                            >
                                                {complianceInfo.message}
                                            </small>

                                            {/* COMPLIANCE FORM */}
                                            {stage.status !== "RELEASED" && (

                                                <div
                                                    style={{
                                                        display: "grid",
                                                        gap: "10px"
                                                    }}
                                                >

                                                    <div>

                                                        <label
                                                            style={{
                                                                display: "block",
                                                                marginBottom: "5px",
                                                                fontWeight: "600"
                                                            }}
                                                        >
                                                            Compliance Status
                                                        </label>

                                                        <select
                                                            value={
                                                                complianceForm.status
                                                            }
                                                            onChange={(e) =>
                                                                updateComplianceForm(
                                                                    stage.id,
                                                                    "status",
                                                                    e.target.value
                                                                )
                                                            }
                                                            disabled={saving}
                                                            className="compliance-select"
                                                            style={{
                                                                width: "100%"
                                                            }}
                                                        >

                                                            <option value="PENDING">
                                                                Compliance Pending
                                                            </option>

                                                            <option value="COMPLETED">
                                                                Completed
                                                            </option>

                                                            <option value="NON_COMPLIANT">
                                                                Non-Compliant
                                                            </option>

                                                        </select>

                                                    </div>

                                                    <div>

                                                        <label
                                                            style={{
                                                                display: "block",
                                                                marginBottom: "5px",
                                                                fontWeight: "600"
                                                            }}
                                                        >
                                                            Compliance Remarks
                                                        </label>

                                                        <textarea
                                                            value={
                                                                complianceForm.remarks
                                                            }
                                                            onChange={(e) =>
                                                                updateComplianceForm(
                                                                    stage.id,
                                                                    "remarks",
                                                                    e.target.value
                                                                )
                                                            }
                                                            disabled={saving}
                                                            placeholder="Enter documents reviewed, conditions checked, observations, or reason for non-compliance..."
                                                            rows={3}
                                                            style={{
                                                                width: "100%",
                                                                boxSizing: "border-box",
                                                                resize: "vertical",
                                                                padding: "10px",
                                                                borderRadius: "8px",
                                                                border: "1px solid rgba(148, 163, 184, 0.35)",
                                                                background: "#ffffff",
                                                                color: "#1e293b"
                                                            }}
                                                        />

                                                    </div>

                                                    <div>

                                                        <label
                                                            style={{
                                                                display: "block",
                                                                marginBottom: "5px",
                                                                fontWeight: "600"
                                                            }}
                                                        >
                                                            Verified By
                                                        </label>

                                                        <input
                                                            type="text"
                                                            value={
                                                                complianceForm.verifiedBy
                                                            }
                                                            onChange={(e) =>
                                                                updateComplianceForm(
                                                                    stage.id,
                                                                    "verifiedBy",
                                                                    e.target.value
                                                                )
                                                            }
                                                            disabled={saving}
                                                            placeholder="Enter Finance Officer name"
                                                            style={{
                                                                width: "100%",
                                                                boxSizing: "border-box",
                                                                padding: "10px",
                                                                borderRadius: "8px",
                                                                border: "1px solid rgba(148, 163, 184, 0.35)",
                                                                background: "#ffffff",
                                                                color: "#1e293b"
                                                            }}
                                                        />

                                                    </div>

                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            handleComplianceUpdate(
                                                                stage.id
                                                            )
                                                        }
                                                        disabled={saving}
                                                        style={{
                                                            marginTop: "4px",
                                                            padding: "10px 14px",
                                                            borderRadius: "8px",
                                                            border: "none",
                                                            cursor: saving
                                                                ? "not-allowed"
                                                                : "pointer",
                                                            fontWeight: "600"
                                                        }}
                                                    >
                                                        {saving
                                                            ? "Saving..."
                                                            : "Save Compliance Review"}
                                                    </button>

                                                </div>

                                            )}

                                            {/* SAVED COMPLIANCE DETAILS */}
                                            {stage.complianceRemarks && (
                                                <div
                                                    style={{
                                                        marginTop: "14px",
                                                        paddingTop: "12px",
                                                        borderTop: "1px solid rgba(148, 163, 184, 0.2)"
                                                    }}
                                                >

                                                    <strong>
                                                        Review Remarks
                                                    </strong>

                                                    <p
                                                        style={{
                                                            margin: "5px 0"
                                                        }}
                                                    >
                                                        {stage.complianceRemarks}
                                                    </p>

                                                    {stage.complianceVerifiedBy && (
                                                        <small>
                                                            Verified by:{" "}
                                                            {
                                                                stage.complianceVerifiedBy
                                                            }
                                                        </small>
                                                    )}

                                                </div>
                                            )}

                                        </div>

                                        {/* STAGE ACTIONS */}
                                        <div className="stage-card-actions">

                                            {/* VERIFY */}
                                            {stage.status === "PENDING" && (
                                                <button
                                                    type="button"
                                                    className="stage-verify-button"
                                                    onClick={() =>
                                                        handleVerify(
                                                            stage.id
                                                        )
                                                    }
                                                    disabled={
                                                        saving ||
                                                        !complianceCompleted
                                                    }
                                                    title={
                                                        !complianceCompleted
                                                            ? "Complete compliance review before verifying this stage."
                                                            : "Verify this stage"
                                                    }
                                                >
                                                    {complianceCompleted
                                                        ? "Verify Stage"
                                                        : "Complete Compliance First"}
                                                </button>
                                            )}

                                            {/* RELEASE */}
                                            {stage.status === "VERIFIED" && (
                                                <button
                                                    type="button"
                                                    className="stage-release-button"
                                                    onClick={() =>
                                                        handleRelease(
                                                            stage.id
                                                        )
                                                    }
                                                    disabled={saving}
                                                >
                                                    Release Payment
                                                </button>
                                            )}

                                            {/* RELEASED */}
                                            {stage.status === "RELEASED" && (
                                                <span className="stage-released-label">
                                                    ✓ Payment Released
                                                </span>
                                            )}

                                        </div>

                                    </div>
                                );
                            })}

                    </div>

                )}

            </div>

        </section>
    );
}
