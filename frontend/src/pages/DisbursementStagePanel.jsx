import React, { useEffect, useState } from "react";
import "./DisbursementStagePanel.css";

const API_BASE = "/api/v1/api/disbursements";
const DOCUMENT_API_BASE = "/api/v1";


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


/*
 * Convert backend document type into a readable label.
 */
function formatDocumentType(documentType) {
    if (!documentType) {
        return "Document";
    }

    const labels = {
        STAGE_2_INVOICE: "Invoice",
        STAGE_2_PAYMENT_PROOF: "Payment Proof",
        STAGE_2_ACTIVITY_PHOTO: "Activity Photo",

        FINAL_COMPLETION_REPORT: "Final Completion Report",
        UTILIZATION_STATEMENT: "Utilization / Expenditure Statement",
        FINAL_PROJECT_PHOTO: "Final Project Photo",
        FINAL_PAYMENT_PROOF: "Final Payment Proof"
    };

    if (labels[documentType]) {
        return labels[documentType];
    }

    return documentType
        .replace(/_/g, " ")
        .toLowerCase()
        .replace(/\b\w/g, (letter) => letter.toUpperCase());
}


/*
 * Return an appropriate icon based on document type / mime type.
 */
function getDocumentIcon(doc) {
    const documentType = String(
        doc?.documentType || ""
    ).toUpperCase();

    const mimeType = String(
        doc?.mimeType || ""
    ).toLowerCase();

    if (
        documentType.includes("PHOTO") ||
        mimeType.startsWith("image/")
    ) {
        return "🖼️";
    }

    if (
        documentType.includes("REPORT") ||
        documentType.includes("STATEMENT") ||
        mimeType === "application/pdf"
    ) {
        return "📄";
    }

    return "📎";
}


export default function DisbursementStagePanel({ application }) {

    const applicationId =
        application?.applicationId ??
        application?.id;


    /*
     * Beneficiary ID can be exposed by the application
     * in different shapes.
     */
    const beneficiaryId =
        application?.beneficiaryId ??
        application?.beneficiary?.id ??
        application?.beneficiary?.beneficiaryId ??
        application?.beneficiaryIdNumber ??
        null;


    const [plan, setPlan] = useState(null);
    const [stages, setStages] = useState([]);

    /*
     * Documents are stored by stage ID.
     *
     * Example:
     *
     * {
     *     12: [document1, document2],
     *     13: [document3, document4]
     * }
     */
    const [stageDocuments, setStageDocuments] = useState({});

    const [loading, setLoading] = useState(false);
    const [documentsLoading, setDocumentsLoading] = useState({});
    const [saving, setSaving] = useState(false);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    /*
     * Stores compliance information entered on the page
     * before it is saved to the backend.
     */
    const [complianceDrafts, setComplianceDrafts] = useState({});


    /*
     * ============================================================
     * LOAD DOCUMENTS FOR ONE STAGE
     * ============================================================
     */
    const loadStageDocuments = async (stage) => {

        if (
            !beneficiaryId ||
            !applicationId ||
            !stage?.stageNumber
        ) {
            return;
        }

        const stageId = stage.id;

        setDocumentsLoading((previous) => ({
            ...previous,
            [stageId]: true
        }));

        try {

            const response = await fetch(
                `${DOCUMENT_API_BASE}/beneficiaries/${beneficiaryId}/documents/application/${applicationId}/stage/${stage.stageNumber}`
            );

            if (!response.ok) {
                throw new Error(
                    "Unable to load beneficiary documents."
                );
            }

            const data = await response.json();

            setStageDocuments((previous) => ({
                ...previous,
                [stageId]: Array.isArray(data)
                    ? data
                    : []
            }));

        } catch (err) {

            console.error(
                `Unable to load documents for Stage ${stage.stageNumber}:`,
                err
            );

            setStageDocuments((previous) => ({
                ...previous,
                [stageId]: []
            }));

        } finally {

            setDocumentsLoading((previous) => ({
                ...previous,
                [stageId]: false
            }));
        }
    };


    /*
     * ============================================================
     * LOAD ALL STAGE DOCUMENTS
     * ============================================================
     */
    const loadAllStageDocuments = async (stageList) => {

        if (
            !beneficiaryId ||
            !applicationId ||
            !Array.isArray(stageList)
        ) {
            return;
        }

        const documentStages = stageList.filter(
            (stage) =>
                Number(stage.stageNumber) === 2 ||
                Number(stage.stageNumber) === 3
        );

        await Promise.all(
            documentStages.map((stage) =>
                loadStageDocuments(stage)
            )
        );
    };


    /*
     * ============================================================
     * LOAD PLAN + STAGES
     * ============================================================
     */
    const loadPlanAndStages = async () => {

        if (!applicationId) {
            setPlan(null);
            setStages([]);
            setStageDocuments({});
            setError("Application ID is not available.");
            return;
        }

        setLoading(true);
        setError("");

        try {

            /*
             * Step 1:
             * Load disbursement plan.
             */
            const planResponse = await fetch(
                `${API_BASE}/application/${applicationId}/plan`
            );

            if (planResponse.status === 404) {

                setPlan(null);
                setStages([]);
                setStageDocuments({});

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
             * Load stages.
             */
            if (!planData?.id) {

                setStages([]);
                setStageDocuments({});

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
             * Step 3:
             * Load beneficiary documents for Stage 2 and Stage 3.
             */
            await loadAllStageDocuments(loadedStages);

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
            setStageDocuments({});

            setError(
                "Application ID is not available."
            );
        }

    }, [
        applicationId,
        beneficiaryId
    ]);


    /*
     * ============================================================
     * SUMMARY CALCULATIONS
     * ============================================================
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


    const sanctionedAmount =
        plan?.totalAmount ??
        application?.sanctionedAmount ??
        application?.scheme?.grantAmount ??
        0;


    const remainingAmount = Math.max(
        Number(sanctionedAmount) - releasedAmount,
        0
    );


    /*
     * ============================================================
     * COMPLIANCE ALERTS
     * ============================================================
     */

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
     * ============================================================
     * CREATE DISBURSEMENT PLAN
     * ============================================================
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
     * ============================================================
     * VERIFY STAGE
     * ============================================================
     */
    const handleVerify = async (stageId) => {

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
     * ============================================================
     * RELEASE STAGE
     * ============================================================
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
     * ============================================================
     * UPDATE COMPLIANCE STATUS
     *
     * No browser popup.
     * Remarks and Finance Officer name are entered directly
     * on the page.
     * ============================================================
     */
    const handleComplianceUpdate = async (stageId) => {

        const draft = complianceDrafts[stageId] || {};

        const status =
            draft.status ||
            "PENDING";

        const remarks =
            draft.remarks ||
            "";

        const verifiedBy =
            draft.verifiedBy ||
            "";


        if (
            status !== "PENDING" &&
            !remarks.trim()
        ) {
            setError(
                "Compliance remarks are required."
            );
            return;
        }


        if (
            status !== "PENDING" &&
            !verifiedBy.trim()
        ) {
            setError(
                "Finance Officer name is required."
            );
            return;
        }


        setSaving(true);
        setError("");
        setSuccess("");

        try {

            const params = new URLSearchParams({
                status,
                remarks: remarks.trim(),
                verifiedBy: verifiedBy.trim()
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
                status === "COMPLETED"
                    ? "Stage compliance marked as completed."
                    : status === "NON_COMPLIANT"
                        ? "Stage marked as non-compliant."
                        : "Compliance status updated."
            );


            await loadPlanAndStages();


            /*
             * Keep the entered values available on screen.
             */
            setComplianceDrafts((previous) => ({
                ...previous,
                [stageId]: {
                    status,
                    remarks,
                    verifiedBy
                }
            }));


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
     * ============================================================
     * VIEW DOCUMENT
     * ============================================================
     */
    const handleViewDocument = (documentId) => {

        if (
            !beneficiaryId ||
            !documentId
        ) {
            setError(
                "Beneficiary or document information is missing."
            );
            return;
        }


        const url =
            `${DOCUMENT_API_BASE}/beneficiaries/${beneficiaryId}/documents/${documentId}/download`;


        window.open(
            url,
            "_blank",
            "noopener,noreferrer"
        );
    };


    /*
     * ============================================================
     * DOWNLOAD DOCUMENT
     * ============================================================
     */
    const handleDownloadDocument = async (doc) => {

        if (
            !beneficiaryId ||
            !doc?.id
        ) {
            setError(
                "Beneficiary or document information is missing."
            );
            return;
        }


        try {

            const response = await fetch(
                `${DOCUMENT_API_BASE}/beneficiaries/${beneficiaryId}/documents/${doc.id}/download`
            );


            if (!response.ok) {
                throw new Error(
                    "Unable to download document."
                );
            }


            const blob =
                await response.blob();


            const blobUrl =
                window.URL.createObjectURL(blob);


            const link =
                window.document.createElement("a");


            link.href = blobUrl;


            link.download =
                doc.originalFileName ||
                doc.fileName ||
                "beneficiary-document";


            window.document.body.appendChild(link);


            link.click();


            link.remove();


            window.URL.revokeObjectURL(blobUrl);


        } catch (err) {

            console.error(
                "Document download error:",
                err
            );


            setError(
                err.message ||
                "Unable to download document."
            );
        }
    };


    /*
     * ============================================================
     * REFRESH DOCUMENTS
     * ============================================================
     */
    const handleRefreshStageDocuments = async (stage) => {
        await loadStageDocuments(stage);
    };


    /*
     * ============================================================
     * RENDER BENEFICIARY DOCUMENTS
     * ============================================================
     */
    const renderBeneficiaryDocuments = (stage) => {

        const stageNumber =
            Number(stage?.stageNumber);


        /*
         * Stage 1 documents are handled through
         * Finance Officer verification.
         */
        if (
            stageNumber !== 2 &&
            stageNumber !== 3
        ) {
            return null;
        }


        const documents =
            stageDocuments[stage.id] || [];


        const isLoading =
            documentsLoading[stage.id] === true;


        return (
            <div
                style={{
                    marginTop: "18px",
                    padding: "18px",
                    borderRadius: "10px",
                    background: "#f8fafc",
                    border: "1px solid #dbeafe"
                }}
            >

                <div
                    style={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        gap: "12px",
                        marginBottom: "14px"
                    }}
                >

                    <div>

                        <strong
                            style={{
                                display: "block",
                                fontSize: "1rem",
                                color: "#1e293b"
                            }}
                        >
                            Beneficiary Submitted Documents
                        </strong>

                        <small
                            style={{
                                color: "#64748b"
                            }}
                        >
                            Stage {stageNumber} documents submitted
                            by the beneficiary for verification.
                        </small>

                    </div>


                    <button
                        type="button"
                        onClick={() =>
                            handleRefreshStageDocuments(stage)
                        }
                        disabled={isLoading}
                        style={{
                            padding: "7px 12px",
                            borderRadius: "6px",
                            border: "1px solid #cbd5e1",
                            background: "#ffffff",
                            cursor: isLoading
                                ? "not-allowed"
                                : "pointer",
                            fontSize: "0.85rem"
                        }}
                    >
                        {isLoading
                            ? "Loading..."
                            : "Refresh"}
                    </button>

                </div>


                {isLoading ? (

                    <div
                        style={{
                            padding: "14px",
                            textAlign: "center",
                            color: "#64748b"
                        }}
                    >
                        Loading beneficiary documents...
                    </div>

                ) : !beneficiaryId ? (

                    <div
                        style={{
                            padding: "14px",
                            borderRadius: "7px",
                            background: "#fff7ed",
                            border: "1px solid #fed7aa",
                            color: "#9a3412"
                        }}
                    >
                        Beneficiary information is not available
                        for this application.
                    </div>

                ) : documents.length === 0 ? (

                    <div
                        style={{
                            padding: "14px",
                            borderRadius: "7px",
                            background: "#ffffff",
                            border: "1px solid #e2e8f0",
                            color: "#64748b"
                        }}
                    >
                        No beneficiary documents have been
                        uploaded for this stage yet.
                    </div>

                ) : (

                    <div
                        style={{
                            display: "flex",
                            flexDirection: "column",
                            gap: "10px"
                        }}
                    >

                        {documents.map((doc) => (

                            <div
                                key={doc.id}
                                style={{
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "space-between",
                                    gap: "15px",
                                    padding: "12px 14px",
                                    borderRadius: "8px",
                                    background: "#ffffff",
                                    border: "1px solid #e2e8f0"
                                }}
                            >

                                <div
                                    style={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: "10px",
                                        minWidth: 0
                                    }}
                                >

                                    <span
                                        style={{
                                            fontSize: "1.25rem"
                                        }}
                                    >
                                        {getDocumentIcon(doc)}
                                    </span>


                                    <div
                                        style={{
                                            minWidth: 0
                                        }}
                                    >

                                        <strong
                                            style={{
                                                display: "block",
                                                color: "#1e293b",
                                                wordBreak: "break-word"
                                            }}
                                        >
                                            {formatDocumentType(
                                                doc.documentType
                                            )}
                                        </strong>


                                        <small
                                            style={{
                                                display: "block",
                                                color: "#64748b",
                                                wordBreak: "break-word"
                                            }}
                                        >
                                            {doc.originalFileName ||
                                                doc.fileName ||
                                                "Uploaded document"}
                                        </small>


                                        {doc.uploadedAt && (
                                            <small
                                                style={{
                                                    display: "block",
                                                    color: "#94a3b8",
                                                    marginTop: "2px"
                                                }}
                                            >
                                                Uploaded:{" "}
                                                {formatDate(
                                                    doc.uploadedAt
                                                )}
                                            </small>
                                        )}

                                    </div>

                                </div>


                                <div
                                    style={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: "8px",
                                        flexShrink: 0
                                    }}
                                >

                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleViewDocument(
                                                doc.id
                                            )
                                        }
                                        style={{
                                            padding: "7px 12px",
                                            borderRadius: "6px",
                                            border: "1px solid #2563eb",
                                            background: "#ffffff",
                                            color: "#2563eb",
                                            cursor: "pointer",
                                            fontSize: "0.85rem"
                                        }}
                                    >
                                        View
                                    </button>


                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleDownloadDocument(
                                                doc
                                            )
                                        }
                                        style={{
                                            padding: "7px 12px",
                                            borderRadius: "6px",
                                            border: "1px solid #16a34a",
                                            background: "#16a34a",
                                            color: "#ffffff",
                                            cursor: "pointer",
                                            fontSize: "0.85rem"
                                        }}
                                    >
                                        Download
                                    </button>

                                </div>

                            </div>

                        ))}

                    </div>

                )}

            </div>
        );
    };


    /*
     * ============================================================
     * RENDER SAVED COMPLIANCE INFORMATION
     * ============================================================
     */
    const renderSavedComplianceInfo = (stage) => {

        const remarks =
            stage.complianceRemarks ||
            complianceDrafts[stage.id]?.remarks ||
            "";

        const verifiedBy =
            stage.complianceVerifiedBy ||
            complianceDrafts[stage.id]?.verifiedBy ||
            "";

        const verifiedAt =
            stage.complianceVerifiedAt;


        if (
            !remarks &&
            !verifiedBy
        ) {
            return null;
        }


        return (
            <div
                style={{
                    marginTop: "14px",
                    padding: "14px 16px",
                    borderRadius: "8px",
                    background: "#f8fafc",
                    border: "1px solid #e2e8f0"
                }}
            >

                <div
                    style={{
                        fontWeight: "600",
                        color: "#1e293b",
                        marginBottom: "8px"
                    }}
                >
                    Compliance Verification
                </div>


                {remarks && (
                    <div
                        style={{
                            marginBottom: "7px"
                        }}
                    >
                        <span
                            style={{
                                fontWeight: "600",
                                color: "#475569"
                            }}
                        >
                            Remarks:
                        </span>

                        <span
                            style={{
                                marginLeft: "6px",
                                color: "#334155"
                            }}
                        >
                            {remarks}
                        </span>
                    </div>
                )}


                {verifiedBy && (
                    <div
                        style={{
                            marginBottom: "7px"
                        }}
                    >
                        <span
                            style={{
                                fontWeight: "600",
                                color: "#475569"
                            }}
                        >
                            Verified By:
                        </span>

                        <span
                            style={{
                                marginLeft: "6px",
                                color: "#334155"
                            }}
                        >
                            {verifiedBy}
                        </span>
                    </div>
                )}


                {verifiedAt && (
                    <div>

                        <span
                            style={{
                                fontWeight: "600",
                                color: "#475569"
                            }}
                        >
                            Verified At:
                        </span>

                        <span
                            style={{
                                marginLeft: "6px",
                                color: "#64748b"
                            }}
                        >
                            {formatDate(verifiedAt)}
                        </span>

                    </div>
                )}

            </div>
        );
    };


    /*
     * ============================================================
     * APPLICATION ID NOT AVAILABLE
     * ============================================================
     */
    if (!applicationId) {

        return (
            <section className="disbursement-panel">

                <div className="disbursement-header">

                    <div>

                        <h3>
                            Disbursement
                        </h3>

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
     * ============================================================
     * NO DISBURSEMENT PLAN
     * ============================================================
     */
    if (!plan && !loading) {

        return (
            <section className="disbursement-panel">

                <div className="disbursement-header">

                    <div>

                        <h3>
                            Disbursement
                        </h3>

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


    /*
     * ============================================================
     * MAIN DISBURSEMENT PANEL
     * ============================================================
     */
    return (

        <section className="disbursement-panel">


            {/* ======================================================
                HEADER
            ====================================================== */}

            <div className="disbursement-header">

                <div>

                    <h3>
                        Staged Disbursement
                    </h3>

                    <p>
                        Create, verify and release payment stages
                        for this approved application.
                    </p>

                </div>


                <span className="disbursement-badge">
                    PLAN #{plan?.id}
                </span>

            </div>


            {/* ======================================================
                ERROR
            ====================================================== */}

            {error && (
                <div className="disbursement-alert error">
                    {error}
                </div>
            )}


            {/* ======================================================
                SUCCESS
            ====================================================== */}

            {success && (
                <div className="disbursement-alert success">
                    {success}
                </div>
            )}


            {/* ======================================================
                SUMMARY
            ====================================================== */}

            <div className="disbursement-summary">

                <div className="disbursement-summary-card">

                    <span>
                        Sanctioned Amount
                    </span>

                    <strong>
                        {formatAmount(
                            sanctionedAmount
                        )}
                    </strong>

                </div>


                <div className="disbursement-summary-card">

                    <span>
                        Planned Amount
                    </span>

                    <strong>
                        {formatAmount(
                            plannedAmount
                        )}
                    </strong>

                </div>


                <div className="disbursement-summary-card">

                    <span>
                        Released Amount
                    </span>

                    <strong>
                        {formatAmount(
                            releasedAmount
                        )}
                    </strong>

                </div>


                <div className="disbursement-summary-card">

                    <span>
                        Remaining
                    </span>

                    <strong>
                        {formatAmount(
                            remainingAmount
                        )}
                    </strong>

                </div>


                <div className="disbursement-summary-card">

                    <span>
                        Plan Status
                    </span>

                    <strong>
                        {plan?.status ?? "-"}
                    </strong>

                </div>


                <div className="disbursement-summary-card">

                    <span>
                        Disbursement Type
                    </span>

                    <strong>
                        {plan?.disbursementType ?? "-"}
                    </strong>

                </div>

            </div>


            {/* ======================================================
                COMPLIANCE ALERTS
            ====================================================== */}

            {complianceAlerts.length > 0 && (

                <div className="compliance-alerts-section">

                    <div className="compliance-alerts-header">

                        <h4>
                            Compliance Alerts
                        </h4>

                        <span>
                            {complianceAlerts.length} alert
                            {complianceAlerts.length > 1
                                ? "s"
                                : ""}
                        </span>

                    </div>


                    <div className="compliance-alert-list">

                        {complianceAlerts.map(
                            ({ stage, info }) => (

                                <div
                                    key={stage.id}
                                    className={`compliance-alert-item ${info.type}`}
                                >

                                    <div className="compliance-alert-icon">
                                        !
                                    </div>


                                    <div className="compliance-alert-content">

                                        <strong>
                                            Stage{" "}
                                            {stage.stageNumber}
                                            {" — "}
                                            {info.label}
                                        </strong>

                                        <span>
                                            {info.message}
                                        </span>

                                        <small>
                                            Milestone:{" "}
                                            {stage.milestone ||
                                                "-"}
                                        </small>

                                    </div>

                                </div>

                            )
                        )}

                    </div>

                </div>

            )}


            {/* ======================================================
                AUTOMATIC STAGE ALLOCATION
            ====================================================== */}

            <div className="disbursement-section">

                <div className="disbursement-section-title">

                    <h4>
                        Automatic Stage Allocation
                    </h4>

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
                            The system automatically divides the
                            sanctioned amount into three stages with
                            due dates at 30, 60 and 90 days.
                            Stage 1 receives 40%, Stage 2 receives
                            35%, and Stage 3 receives the remaining
                            amount.
                        </p>


                        <p>

                            Total allocated:{" "}

                            <strong>
                                {formatAmount(
                                    plannedAmount
                                )}
                            </strong>

                            {" "}of{" "}

                            <strong>
                                {formatAmount(
                                    sanctionedAmount
                                )}
                            </strong>

                        </p>

                    </div>

                </div>

            </div>


            {/* ======================================================
                DISBURSEMENT STAGES
            ====================================================== */}

            <div className="disbursement-section">


                <div className="disbursement-section-title">

                    <h4>
                        Disbursement Stages
                    </h4>


                    <button
                        type="button"
                        className="stage-refresh-button"
                        onClick={
                            loadPlanAndStages
                        }
                        disabled={
                            loading ||
                            saving
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

                                const complianceInfo =
                                    getComplianceInfo(
                                        stage
                                    );


                                const currentDraft =
                                    complianceDrafts[stage.id] || {};


                                const currentStatus =
                                    currentDraft.status ??
                                    stage.complianceStatus ??
                                    "PENDING";


                                return (

                                    <div
                                        className="stage-card"
                                        key={stage.id}
                                    >


                                        {/* ==================================
                                            STAGE HEADER
                                        ================================== */}

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
                                                    >
                                                        ⚠ OVERDUE
                                                    </span>

                                                )}


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

                                        </div>


                                        {/* ==================================
                                            STAGE DETAILS
                                        ================================== */}

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


                                        {/* ==================================
                                            COMPLIANCE STATUS
                                        ================================== */}

                                        <div>

                                            <span className="compliance-label">
                                                Compliance
                                            </span>


                                            <strong
                                                className={`compliance-status ${complianceInfo.type}`}
                                            >
                                                {complianceInfo.label}
                                            </strong>


                                            <small
                                                className={`compliance-message ${complianceInfo.type}`}
                                            >
                                                {complianceInfo.message}
                                            </small>

                                        </div>


                                        {/* ==================================
                                            BENEFICIARY DOCUMENTS
                                        ================================== */}

                                        {renderBeneficiaryDocuments(
                                            stage
                                        )}


                                        {/* ==================================
                                            SAVED COMPLIANCE DETAILS
                                        ================================== */}

                                        {renderSavedComplianceInfo(
                                            stage
                                        )}


                                        {/* ==================================
                                            STAGE ACTIONS
                                        ================================== */}

                                        <div className="stage-card-actions">


                                            {stage.status === "PENDING" && (

                                                <div
                                                    style={{
                                                        width: "100%",
                                                        display: "flex",
                                                        flexDirection: "column",
                                                        gap: "14px"
                                                    }}
                                                >


                                                    {/* =========================
                                                        COMPLIANCE STATUS
                                                    ========================= */}

                                                    <div>

                                                        <label
                                                            style={{
                                                                display: "block",
                                                                fontWeight: "600",
                                                                marginBottom: "6px"
                                                            }}
                                                        >
                                                            Compliance Status
                                                        </label>


                                                        <select
                                                            value={
                                                                currentStatus
                                                            }
                                                            onChange={(e) =>
                                                                setComplianceDrafts(
                                                                    (previous) => ({
                                                                        ...previous,
                                                                        [stage.id]: {
                                                                            ...(previous[stage.id] || {}),
                                                                            status: e.target.value
                                                                        }
                                                                    })
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

                                                    </div>


                                                    {/* =========================
                                                        REMARKS + VERIFIED BY
                                                    ========================= */}

                                                    {(
                                                        currentStatus === "COMPLETED" ||
                                                        currentStatus === "NON_COMPLIANT"
                                                    ) && (

                                                        <div
                                                            style={{
                                                                width: "100%",
                                                                display: "flex",
                                                                flexDirection: "column",
                                                                gap: "12px"
                                                            }}
                                                        >


                                                            {/* COMPLIANCE REMARKS */}

                                                            <div>

                                                                <label
                                                                    style={{
                                                                        display: "block",
                                                                        fontWeight: "600",
                                                                        marginBottom: "6px"
                                                                    }}
                                                                >
                                                                    Compliance Remarks
                                                                </label>


                                                                <textarea
                                                                    value={
                                                                        currentDraft.remarks ||
                                                                        stage.complianceRemarks ||
                                                                        ""
                                                                    }
                                                                    onChange={(e) =>
                                                                        setComplianceDrafts(
                                                                            (previous) => ({
                                                                                ...previous,
                                                                                [stage.id]: {
                                                                                    ...(previous[stage.id] || {}),
                                                                                    remarks: e.target.value
                                                                                }
                                                                            })
                                                                        )
                                                                    }
                                                                    placeholder="Enter compliance remarks"
                                                                    rows={3}
                                                                    disabled={saving}
                                                                    style={{
                                                                        width: "100%",
                                                                        padding: "10px",
                                                                        border: "1px solid #cbd5e1",
                                                                        borderRadius: "6px",
                                                                        resize: "vertical",
                                                                        fontFamily: "inherit",
                                                                        boxSizing: "border-box"
                                                                    }}
                                                                />

                                                            </div>


                                                            {/* FINANCE OFFICER */}

                                                            <div>

                                                                <label
                                                                    style={{
                                                                        display: "block",
                                                                        fontWeight: "600",
                                                                        marginBottom: "6px"
                                                                    }}
                                                                >
                                                                    Verified By (Finance Officer)
                                                                </label>


                                                                <input
                                                                    type="text"
                                                                    value={
                                                                        currentDraft.verifiedBy ||
                                                                        stage.complianceVerifiedBy ||
                                                                        ""
                                                                    }
                                                                    onChange={(e) =>
                                                                        setComplianceDrafts(
                                                                            (previous) => ({
                                                                                ...previous,
                                                                                [stage.id]: {
                                                                                    ...(previous[stage.id] || {}),
                                                                                    verifiedBy: e.target.value
                                                                                }
                                                                            })
                                                                        )
                                                                    }
                                                                    placeholder="Enter Finance Officer name"
                                                                    disabled={saving}
                                                                    style={{
                                                                        width: "100%",
                                                                        padding: "10px",
                                                                        border: "1px solid #cbd5e1",
                                                                        borderRadius: "6px",
                                                                        boxSizing: "border-box"
                                                                    }}
                                                                />

                                                            </div>


                                                            {/* SAVE COMPLIANCE */}

                                                            <div
                                                                style={{
                                                                    display: "flex",
                                                                    gap: "10px",
                                                                    alignItems: "center"
                                                                }}
                                                            >

                                                                <button
                                                                    type="button"
                                                                    className="stage-verify-button"
                                                                    onClick={() =>
                                                                        handleComplianceUpdate(
                                                                            stage.id
                                                                        )
                                                                    }
                                                                    disabled={saving}
                                                                >
                                                                    {saving
                                                                        ? "Saving..."
                                                                        : "Save Compliance"}
                                                                </button>

                                                            </div>

                                                        </div>

                                                    )}


                                                    {/* =========================
                                                        VERIFY STAGE
                                                    ========================= */}

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
                                                            stage.complianceStatus !==
                                                            "COMPLETED"
                                                        }
                                                    >
                                                        Verify Stage
                                                    </button>

                                                </div>

                                            )}


                                            {/* ==================================
                                                VERIFIED
                                            ================================== */}

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


                                            {/* ==================================
                                                RELEASED
                                            ================================== */}

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