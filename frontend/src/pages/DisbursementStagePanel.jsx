import React, { useEffect, useState } from "react";
import "./DisbursementStagePanel.css";

const API_BASE = "/api/disbursements";

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

    const [stageNumber, setStageNumber] = useState("");
    const [amount, setAmount] = useState("");
    const [milestone, setMilestone] = useState("");
    const [dueDate, setDueDate] = useState("");

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
     * Calculate amounts from the stages.
     */
    const releasedAmount = stages.reduce(
        (sum, stage) =>
            stage.status === "RELEASED"
                ? sum + Number(stage.amount || 0)
                : sum,
        0
    );

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
        Number(sanctionedAmount) - plannedAmount,
        0
    );

    /*
     * Create a new stage.
     */
    const handleAddStage = async (event) => {
        event.preventDefault();

        if (!planId) {
            setError(
                "Disbursement plan is not available for this application."
            );
            return;
        }

        if (!stageNumber || Number(stageNumber) <= 0) {
            setError("Enter a valid stage number.");
            return;
        }

        if (!amount || Number(amount) <= 0) {
            setError("Enter a valid stage amount.");
            return;
        }

        if (Number(amount) > remainingAmount) {
            setError(
                `Stage amount cannot exceed the remaining amount of ${formatAmount(
                    remainingAmount
                )}.`
            );
            return;
        }

        if (!milestone.trim()) {
            setError("Enter the milestone.");
            return;
        }

        if (!dueDate) {
            setError("Select a due date.");
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        try {
            const response = await fetch(
                `${API_BASE}/${planId}/stages`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        stageNumber: Number(stageNumber),
                        amount: Number(amount),
                        milestone: milestone.trim(),
                        dueDate,
                    }),
                }
            );

            const data = await response
                .json()
                .catch(() => null);

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.error ||
                    "Unable to create disbursement stage."
                );
            }

            setStageNumber("");
            setAmount("");
            setMilestone("");
            setDueDate("");

            setSuccess(
                "Disbursement stage created successfully."
            );

            await loadPlanAndStages();
        } catch (err) {
            console.error(
                "Create stage error:",
                err
            );

            setError(
                err.message ||
                "Unable to create disbursement stage."
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
                </div>
            </section>
        );
    }

    return (
        <section className="disbursement-panel">
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

            {error && (
                <div className="disbursement-alert error">
                    {error}
                </div>
            )}

            {success && (
                <div className="disbursement-alert success">
                    {success}
                </div>
            )}

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
            </div>

            <div className="disbursement-section">
                <div className="disbursement-section-title">
                    <h4>Create Disbursement Stage</h4>

                    <span>
                        {remainingAmount > 0
                            ? `${formatAmount(
                                remainingAmount
                            )} available`
                            : "Full amount planned"}
                    </span>
                </div>

                <form
                    className="stage-form"
                    onSubmit={handleAddStage}
                >
                    <div className="stage-form-grid">
                        <div className="stage-field">
                            <label>Stage Number</label>

                            <input
                                type="number"
                                min="1"
                                value={stageNumber}
                                onChange={(e) =>
                                    setStageNumber(
                                        e.target.value
                                    )
                                }
                                placeholder="e.g. 1"
                                disabled={saving}
                            />
                        </div>

                        <div className="stage-field">
                            <label>Amount</label>

                            <input
                                type="number"
                                min="1"
                                step="0.01"
                                value={amount}
                                onChange={(e) =>
                                    setAmount(
                                        e.target.value
                                    )
                                }
                                placeholder="e.g. 50000"
                                disabled={saving}
                            />
                        </div>

                        <div className="stage-field">
                            <label>Due Date</label>

                            <input
                                type="date"
                                value={dueDate}
                                onChange={(e) =>
                                    setDueDate(
                                        e.target.value
                                    )
                                }
                                disabled={saving}
                            />
                        </div>

                        <div className="stage-field stage-field-wide">
                            <label>Milestone</label>

                            <input
                                type="text"
                                value={milestone}
                                onChange={(e) =>
                                    setMilestone(
                                        e.target.value
                                    )
                                }
                                placeholder="e.g. Purchase of approved equipment"
                                disabled={saving}
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        className="stage-primary-button"
                        disabled={
                            saving ||
                            remainingAmount <= 0
                        }
                    >
                        {saving
                            ? "Processing..."
                            : "Add Stage"}
                    </button>
                </form>
            </div>

            <div className="disbursement-section">
                <div className="disbursement-section-title">
                    <h4>Disbursement Stages</h4>

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
                            .map((stage) => (
                                <div
                                    className="stage-card"
                                    key={stage.id}
                                >
                                    <div className="stage-card-top">
                                        <div className="stage-number">
                                            Stage{" "}
                                            {stage.stageNumber}
                                        </div>

                                        <span
                                            className={`stage-status ${String(
                                                stage.status ||
                                                ""
                                            ).toLowerCase()}`}
                                        >
                                            {stage.status ||
                                                "PENDING"}
                                        </span>
                                    </div>

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

                                    <div className="stage-card-actions">
                                        {stage.status ===
                                            "PENDING" && (
                                                <button
                                                    type="button"
                                                    className="stage-verify-button"
                                                    onClick={() =>
                                                        handleVerify(
                                                            stage.id
                                                        )
                                                    }
                                                    disabled={
                                                        saving
                                                    }
                                                >
                                                    Verify Stage
                                                </button>
                                            )}

                                        {stage.status ===
                                            "VERIFIED" && (
                                                <button
                                                    type="button"
                                                    className="stage-release-button"
                                                    onClick={() =>
                                                        handleRelease(
                                                            stage.id
                                                        )
                                                    }
                                                    disabled={
                                                        saving
                                                    }
                                                >
                                                    Release Payment
                                                </button>
                                            )}

                                        {stage.status ===
                                            "RELEASED" && (
                                                <span className="stage-released-label">
                                                ✓ Payment Released
                                            </span>
                                            )}
                                    </div>
                                </div>
                            ))}
                    </div>
                )}
            </div>
        </section>
    );
}