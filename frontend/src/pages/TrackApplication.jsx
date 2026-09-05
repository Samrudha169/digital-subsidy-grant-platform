import { useState } from 'react';
import './TrackApplication.css';

// ── Status display helpers ────────────────────────────────────────────────────

const STATUS_LABELS = {
    PENDING:           'Submitted — Awaiting Review',
    UNDER_REVIEW:      'Under Field Officer Review',
    FIELD_APPROVED:    'Field Approved — Awaiting Finance',
    ESCALATED:         'Escalated to District Officer',
    DISTRICT_APPROVED: 'District Approved — Awaiting Finance',
    APPROVED:          'Approved ✓',
    REJECTED:          'Rejected',
};

const STATUS_CLASS = {
    PENDING:           'status-badge status-pending',
    UNDER_REVIEW:      'status-badge status-review',
    FIELD_APPROVED:    'status-badge status-field',
    ESCALATED:         'status-badge status-escalated',
    DISTRICT_APPROVED: 'status-badge status-district',
    APPROVED:          'status-badge status-approved',
    REJECTED:          'status-badge status-rejected',
};

function statusLabel(raw)  { return STATUS_LABELS[raw]  || raw; }
function statusClass(raw)  { return STATUS_CLASS[raw]   || 'status-badge'; }

function formatDate(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-IN', {
        day:    '2-digit',
        month:  'short',
        year:   'numeric',
        hour:   '2-digit',
        minute: '2-digit',
    });
}

// ── Component ─────────────────────────────────────────────────────────────────

function TrackApplication() {

    const [applicationId, setApplicationId] = useState('');
    const [loading,       setLoading]       = useState(false);
    const [error,         setError]         = useState(null);   // string | null
    const [appData,       setAppData]       = useState(null);   // VerificationStatusResponse | null

    // ── Form submission ───────────────────────────────────────────────────────

    const handleSubmit = async (e) => {
        e.preventDefault();

        const trimmed = applicationId.trim();

        if (!trimmed) {
            setError('Please enter your Application ID.');
            setAppData(null);
            return;
        }

        if (!/^\d+$/.test(trimmed)) {
            setError('Application ID must be a number (e.g. 42).');
            setAppData(null);
            return;
        }

        setLoading(true);
        setError(null);
        setAppData(null);

        try {
            /*
             * Calls:  GET /api/v1/verification/applications/{id}
             * Returns: VerificationStatusResponse
             *   applicationId, beneficiaryId, beneficiaryName,
             *   schemeId, schemeName, applicationStatus,
             *   applicationDate, history[]
             *
             * This endpoint is served by VerificationController and is the
             * canonical source of truth for an application's current state
             * plus its full verification audit trail.
             */
            const res = await fetch(`/api/v1/verification/applications/${trimmed}`);

            if (res.status === 404) {
                setError(`No application found with ID ${trimmed}. Please check your ID and try again.`);
                return;
            }

            if (!res.ok) {
                const body = await res.json().catch(() => null);
                setError(body?.message || `Server error (${res.status}). Please try again later.`);
                return;
            }

            const data = await res.json();
            setAppData(data);

        } catch (err) {
            setError('Unable to reach the server. Please check your connection and try again.');
        } finally {
            setLoading(false);
        }
    };

    // ── Render ────────────────────────────────────────────────────────────────

    return (
        <div className="track-page">

            {/* Page Header */}
            <section className="track-hero">
                <div className="track-container">
                    <h1>Track Your Application</h1>
                    <p>
                        Enter your application ID below to check the
                        current status of your government scheme application.
                    </p>
                </div>
            </section>


            {/* Tracking Section */}
            <section className="track-section">
                <div className="track-container">
                    <div className="track-card">

                        <div className="track-card-header">
                            <h2>Application Status</h2>
                            <p>
                                Enter the numeric Application ID provided to you
                                after submitting your application.
                            </p>
                        </div>


                        {/* Tracking Form */}
                        <form className="track-form" onSubmit={handleSubmit}>

                            <div className="track-form-group">
                                <label htmlFor="applicationId">
                                    Application ID
                                </label>
                                <input
                                    id="applicationId"
                                    type="text"
                                    inputMode="numeric"
                                    value={applicationId}
                                    onChange={(e) => setApplicationId(e.target.value)}
                                    placeholder="e.g. 42"
                                    aria-label="Application ID"
                                    disabled={loading}
                                />
                            </div>

                            <button
                                type="submit"
                                className="track-button"
                                disabled={loading}
                            >
                                {loading ? 'Searching…' : 'Track Status'}
                            </button>

                        </form>


                        {/* Loading state */}
                        {loading && (
                            <div className="track-result" style={{ textAlign: 'center', padding: 'var(--spacing-xl)' }}>
                                <div className="track-spinner" aria-label="Loading" />
                                <p style={{ marginTop: 'var(--spacing-md)', color: 'var(--color-text-secondary)' }}>
                                    Fetching application details…
                                </p>
                            </div>
                        )}


                        {/* Error state */}
                        {!loading && error && (
                            <div className="track-result error" role="alert">
                                <h3>Unable to Track Application</h3>
                                <p>{error}</p>
                            </div>
                        )}


                        {/* Application detail panel */}
                        {!loading && appData && (
                            <div className="track-detail-panel">

                                {/* Status banner */}
                                <div className="track-detail-status-row">
                                    <span className="track-detail-label">Current Status</span>
                                    <span className={statusClass(appData.applicationStatus)}>
                                        {statusLabel(appData.applicationStatus)}
                                    </span>
                                </div>

                                {/* Core fields */}
                                <div className="track-detail-grid">

                                    <div className="track-detail-field">
                                        <span className="track-detail-label">Application ID</span>
                                        <span className="track-detail-value">
                                            {appData.applicationId}
                                        </span>
                                    </div>

                                    <div className="track-detail-field">
                                        <span className="track-detail-label">Beneficiary</span>
                                        <span className="track-detail-value">
                                            {appData.beneficiaryName}
                                            <small> (ID: {appData.beneficiaryId})</small>
                                        </span>
                                    </div>

                                    <div className="track-detail-field">
                                        <span className="track-detail-label">Scheme</span>
                                        <span className="track-detail-value">
                                            {appData.schemeName}
                                        </span>
                                    </div>

                                    <div className="track-detail-field">
                                        <span className="track-detail-label">Submitted On</span>
                                        <span className="track-detail-value">
                                            {formatDate(appData.applicationDate)}
                                        </span>
                                    </div>

                                </div>

                                {/* Verification History */}
                                {appData.history && appData.history.length > 0 ? (
                                    <div className="track-history">
                                        <h3 className="track-history-title">
                                            Verification History
                                        </h3>
                                        <div className="track-history-table-wrap">
                                            <table className="track-history-table">
                                                <thead>
                                                    <tr>
                                                        <th>Stage</th>
                                                        <th>Action</th>
                                                        <th>Officer</th>
                                                        <th>Date &amp; Time</th>
                                                        <th>Remarks</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {appData.history.map((entry, idx) => (
                                                        <tr key={idx}>
                                                            <td>
                                                                <span className={`stage-badge stage-${entry.stage?.toLowerCase()}`}>
                                                                    {entry.stage}
                                                                </span>
                                                            </td>
                                                            <td>
                                                                <span className={`action-badge action-${entry.action?.toLowerCase()}`}>
                                                                    {entry.action}
                                                                </span>
                                                            </td>
                                                            <td>{entry.performedBy || '—'}</td>
                                                            <td>{formatDate(entry.performedAt)}</td>
                                                            <td className="track-history-remarks">
                                                                {entry.remarks || '—'}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="track-history">
                                        <p className="track-history-empty">
                                            No verification actions have been recorded yet.
                                            Your application is awaiting field officer assignment.
                                        </p>
                                    </div>
                                )}

                            </div>
                        )}


                        {/* Help Information */}
                        <div className="track-help">
                            <h3>Where can I find my Application ID?</h3>
                            <p>
                                Your Application ID is the numeric ID provided after
                                successfully submitting a scheme application. It appears
                                on your application confirmation screen immediately after
                                submission.
                            </p>
                        </div>

                    </div>
                </div>
            </section>


            {/* Status Information */}
            <section className="track-info-section">
                <div className="track-container">

                    <div className="track-section-header">
                        <h2>Application Process</h2>
                        <p>
                            Your application generally moves through
                            the following stages.
                        </p>
                    </div>

                    <div className="track-status-grid">

                        <div className="track-status-card">
                            <div className="track-status-number">01</div>
                            <h3>Submitted</h3>
                            <p>
                                Your application has been successfully
                                submitted and is awaiting field officer review.
                            </p>
                        </div>

                        <div className="track-status-card">
                            <div className="track-status-number">02</div>
                            <h3>Under Review</h3>
                            <p>
                                A Field Officer is reviewing your submitted
                                information and documents on the ground.
                            </p>
                        </div>

                        <div className="track-status-card">
                            <div className="track-status-number">03</div>
                            <h3>Approved</h3>
                            <p>
                                Your application has been approved
                                after successful multi-level verification.
                            </p>
                        </div>

                        <div className="track-status-card">
                            <div className="track-status-number">04</div>
                            <h3>Completed</h3>
                            <p>
                                The application process has been completed
                                and assistance can be processed.
                            </p>
                        </div>

                    </div>

                </div>
            </section>

        </div>
    );
}

export default TrackApplication;