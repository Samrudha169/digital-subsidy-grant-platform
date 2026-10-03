import React, { useEffect, useState } from "react";
import "./DisbursementStagePanel.css";

const API_BASE = "/api/v1/api/disbursements";

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
        maximumFractionDigits: 2,
    })}`;
}

function formatDate(value) {
    if (!value) return "-";

    try {
        return new Date(value).toLocaleDateString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric",
        });
    } catch {
        return value;
    }
}

export default function DisbursementStagePanel({ application }) {
    const applicationId =
        application?.applicationId ?? application?.id;

    const [plan, setPlan] = useState(null);
    const [stages, setStages] = useState([]);

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const planId = plan?.id || null;


    /*
     * Load the disbursement plan using the application ID,
     * then load all stages belonging to that plan.
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

            setStages(
                Array.isArray(stagesData)
                    ? stagesData
                    : []
            );
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

    useEffect(() => {
        if (applicationId) {
            loadPlanAndStages();
        } else {
            setPlan(null);
            setStages([]);
            setError("Application ID is not available.");
        }
    }, [applicationId]);

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
     * Remaining amount according to the stages currently planned.
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
     * Create a disbursement plan for this approved application.
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
                    method: "POST",
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
     * Verify a stage.
     */
    const handleVerify = async (stageId) => {
        setSaving(true);
        setError("");
        setSuccess("");

        try {
            const response = await fetch(
                `${API_BASE}/stages/${stageId}/verify`,
                {
                    method: "PUT",
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
                    method: "PUT",
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
 * Update compliance status of a released stage.
 */
    const handleComplianceUpdate = async (stageId, status) => {
        const remarks = window.prompt(
            "Enter compliance remarks:"
        );

        if (remarks === null || !remarks.trim()) {
            setError("Compliance remarks are required.");
            return;
        }

        const verifiedBy = window.prompt(
            "Enter the name of the officer verifying compliance:"
        );

        if (verifiedBy === null || !verifiedBy.trim()) {
            setError("Verified by is required.");
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        try {
            const params = new URLSearchParams({
                status,
                remarks: remarks.trim(),
                verifiedBy: verifiedBy.trim(),
            });

            const response = await fetch(
                `${API_BASE}/stages/${stageId}/compliance?${params.toString()}`,
                {
                    method: "PUT",
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
                status === "COMPLETED"
                    ? "Stage compliance marked as completed."
                    : status === "NON_COMPLIANT"
                        ? "Stage marked as non-compliant."
                        : "Compliance status updated."
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
                        Create, verify and release payment stages
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
                                    {info.type === "non-compliant"
                                        ? "!"
                                        : "!"}
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


            {/* AUTOMATIC STAGE ALLOCATION */}
            <div className="disbursement-section">

                <div className="disbursement-section-title">
                    <h4>Automatic Stage Allocation</h4>

                    <span>
                        {stages.length > 0
                            ? "System Generated"
                            : "Awaiting stages"}
                    </span>
                </div>

                <div className="stage-complete-message">

                    <div className="stage-complete-icon">
                        ✓
                    </div>

                    <div className="stage-complete-content">

                        <strong>
                            Stages are generated automatically
                        </strong>

                        <p>
                            The system automatically divides the sanctioned
                            amount into three stages with due dates at
                            30, 60 and 90 days. Stage amounts are distributed
                            equally, with any rounding difference applied to
                            the final stage.
                        </p>

                        <p>
                            Total allocated:{" "}
                            <strong>
                                {formatAmount(plannedAmount)}
                            </strong>
                            {" "}of{" "}
                            <strong>
                                {formatAmount(sanctionedAmount)}
                            </strong>
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
                        disabled={
                            loading || saving
                        }
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
                                    Number(
                                        a.stageNumber || 0
                                    ) -
                                    Number(
                                        b.stageNumber || 0
                                    )
                            )
                            .map((stage) => {
                                const complianceInfo = getComplianceInfo(stage);

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

                                            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>

                                                {stage.overdue && (
                                                    <span
                                                        className="stage-status"
                                                        style={{
                                                            background: "#7f1d1d",
                                                            color: "#fca5a5",
                                                            border: "1px solid #991b1b",
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
                                                                fontWeight: "600",
                                                            }}
                                                            title="Non-compliant: payment overdue"
                                                        >
                                                        (Non-compliant)
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

                                        <div>
                                            <span className="compliance-label">Compliance</span>

                                            <strong className={`compliance-status ${complianceInfo.type}`}>
                                                {complianceInfo.label}
                                            </strong>

                                            <small className={`compliance-message ${complianceInfo.type}`}>
                                                {complianceInfo.message}
                                            </small>
                                        </div>

                                        {/* STAGE ACTIONS */}
                                        <div className="stage-card-actions">

                                            {stage.status === "PENDING" && (
                                                <>
                                                    <select
                                                        value={
                                                            stage.complianceStatus ||
                                                            "PENDING"
                                                        }
                                                        onChange={(e) =>
                                                            handleComplianceUpdate(
                                                                stage.id,
                                                                e.target.value
                                                            )
                                                        }
                                                        disabled={saving}
                                                        className="compliance-select"
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

                                                    <button
                                                        type="button"
                                                        className="stage-verify-button"
                                                        onClick={() =>
                                                            handleVerify(stage.id)
                                                        }
                                                        disabled={
                                                            saving ||
                                                            stage.complianceStatus !==
                                                            "COMPLETED"
                                                        }
                                                    >
                                                        Verify Stage
                                                    </button>
                                                </>
                                            )}

                                            {stage.status === "VERIFIED" && (
                                                <button
                                                    type="button"
                                                    className="stage-release-button"
                                                    onClick={() =>
                                                        handleRelease(stage.id)
                                                    }
                                                    disabled={saving}
                                                >
                                                    Release Payment
                                                </button>
                                            )}

                                            {stage.status === "RELEASED" && (
                                                <>
                                                <span className="stage-released-label">
                                                    ✓ Payment Released
                                                </span>

                                                    <select
                                                        value={
                                                            stage.complianceStatus ||
                                                            "PENDING"
                                                        }
                                                        onChange={(e) =>
                                                            handleComplianceUpdate(
                                                                stage.id,
                                                                e.target.value
                                                            )
                                                        }
                                                        disabled={saving}
                                                        className="compliance-select"
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
                                                </>
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
