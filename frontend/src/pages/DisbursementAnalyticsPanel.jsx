import React, { useEffect, useState } from "react";
import "./DisbursementAnalyticsPanel.css";

const API_BASE = "/api/v1/disbursements";

function formatAmount(value) {
    if (value === null || value === undefined || value === "") {
        return "₹0.00";
    }
    return `₹${Number(value).toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    })}`;
}

export default function DisbursementAnalyticsPanel() {

    const [data, setData]               = useState(null);
    const [loading, setLoading]         = useState(false);
    const [error, setError]             = useState("");
    const [downloading, setDownloading]     = useState(false);
    const [downloadingPdf, setDownloadingPdf] = useState(false);

    const fetchAnalytics = async () => {
        setLoading(true);
        setError("");

        try {
            const response = await fetch(`${API_BASE}/analytics`);

            if (!response.ok) {
                throw new Error(
                    `Failed to load analytics (HTTP ${response.status}).`
                );
            }

            setData(await response.json());
        } catch (err) {
            setError(err.message || "Unable to load disbursement analytics.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAnalytics();
    }, []);

    const handleExport = async () => {
        setDownloading(true);
        setError("");

        try {
            const response = await fetch(`${API_BASE}/analytics/export/excel`);

            if (!response.ok) {
                throw new Error(
                    `Export failed (HTTP ${response.status}).`
                );
            }

            const blob = await response.blob();
            const url  = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href     = url;
            link.download = "disbursement-analytics.xlsx";
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        } catch (err) {
            setError(err.message || "Unable to download Excel report.");
        } finally {
            setDownloading(false);
        }
    };

    const handleExportPdf = async () => {
        setDownloadingPdf(true);
        setError("");

        try {
            const response = await fetch(`${API_BASE}/analytics/export/pdf`);

            if (!response.ok) {
                throw new Error(
                    `PDF export failed (HTTP ${response.status}).`
                );
            }

            const blob = await response.blob();
            const url  = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href     = url;
            link.download = "disbursement-analytics.pdf";
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        } catch (err) {
            setError(err.message || "Unable to download PDF report.");
        } finally {
            setDownloadingPdf(false);
        }
    };

    return (
        <section className="analytics-panel">

            {/* HEADER */}
            <div className="analytics-header">
                <div>
                    <h3>Disbursement Analytics</h3>
                    <p>Live summary of sanctioned, planned, and released funds.</p>
                </div>

                <div className="analytics-header-actions">
                    <button
                        className="analytics-refresh-btn"
                        onClick={fetchAnalytics}
                        disabled={loading || downloading}
                    >
                        {loading ? "Loading..." : "Refresh"}
                    </button>

                    <button
                        className="analytics-export-btn"
                        onClick={handleExport}
                        disabled={loading || downloading || downloadingPdf}
                        title="Download analytics as Excel (.xlsx)"
                    >
                        {downloading ? "Downloading..." : "⬇ Export Excel"}
                    </button>

                    <button
                        className="analytics-export-btn pdf"
                        onClick={handleExportPdf}
                        disabled={loading || downloading || downloadingPdf}
                        title="Download analytics as PDF"
                    >
                        {downloadingPdf ? "Downloading..." : "⬇ Export PDF"}
                    </button>
                </div>
            </div>

            {/* ERROR */}
            {error && (
                <div className="analytics-alert error">{error}</div>
            )}

            {/* LOADING SKELETON */}
            {loading && !data && (
                <div className="analytics-loading">
                    Loading analytics…
                </div>
            )}

            {/* CONTENT */}
            {data && (
                <>
                    {/* SUMMARY CARDS */}
                    <div className="analytics-summary-grid">

                        <div className="analytics-card">
                            <span className="analytics-card-label">
                                Total Sanctioned
                            </span>
                            <strong className="analytics-card-value">
                                {formatAmount(data.totalSanctioned)}
                            </strong>
                        </div>

                        <div className="analytics-card">
                            <span className="analytics-card-label">
                                Total Planned
                            </span>
                            <strong className="analytics-card-value">
                                {formatAmount(data.totalPlanned)}
                            </strong>
                        </div>

                        <div className="analytics-card released">
                            <span className="analytics-card-label">
                                Total Released
                            </span>
                            <strong className="analytics-card-value">
                                {formatAmount(data.totalReleased)}
                            </strong>
                        </div>

                        <div className="analytics-card remaining">
                            <span className="analytics-card-label">
                                Total Remaining
                            </span>
                            <strong className="analytics-card-value">
                                {formatAmount(data.totalRemaining)}
                            </strong>
                        </div>

                    </div>

                    {/* BREAKDOWN TABLES */}
                    <div className="analytics-breakdown-grid">

                        {/* BY SCHEME */}
                        <div className="analytics-breakdown-section">
                            <h4 className="analytics-breakdown-title">
                                Released by Scheme
                            </h4>

                            {Object.keys(data.releasedByScheme || {}).length === 0 ? (
                                <p className="analytics-empty">
                                    No releases recorded yet.
                                </p>
                            ) : (
                                <table className="analytics-table">
                                    <thead>
                                        <tr>
                                            <th>Scheme</th>
                                            <th>Released</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {Object.entries(data.releasedByScheme)
                                            .sort((a, b) => b[1] - a[1])
                                            .map(([scheme, amount]) => (
                                                <tr key={scheme}>
                                                    <td>{scheme}</td>
                                                    <td>{formatAmount(amount)}</td>
                                                </tr>
                                            ))}
                                    </tbody>
                                </table>
                            )}
                        </div>

                        {/* BY STATE */}
                        <div className="analytics-breakdown-section">
                            <h4 className="analytics-breakdown-title">
                                Released by State
                            </h4>

                            {Object.keys(data.releasedByState || {}).length === 0 ? (
                                <p className="analytics-empty">
                                    No releases recorded yet.
                                </p>
                            ) : (
                                <table className="analytics-table">
                                    <thead>
                                        <tr>
                                            <th>State</th>
                                            <th>Released</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {Object.entries(data.releasedByState)
                                            .sort((a, b) => b[1] - a[1])
                                            .map(([state, amount]) => (
                                                <tr key={state}>
                                                    <td>{state}</td>
                                                    <td>{formatAmount(amount)}</td>
                                                </tr>
                                            ))}
                                    </tbody>
                                </table>
                            )}
                        </div>

                    </div>
                </>
            )}

        </section>
    );
}
