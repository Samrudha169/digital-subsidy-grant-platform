import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import './OfficerDashboard.css';

// ─── Constants ────────────────────────────────────────────────────────────────

/**
 * Actual OfficerRole enum values from the backend.
 * Source: com.dsgp.authentication.entity.OfficerRole
 */
const ROLES = {
    FIELD_OFFICER:    'FIELD_OFFICER',
    DISTRICT_OFFICER: 'DISTRICT_OFFICER',
    FINANCE_APPROVER: 'FINANCE_APPROVER',
};

/** Statuses each role should query, per backend verification state machine. */
const ROLE_STATUSES = {
    [ROLES.FIELD_OFFICER]:    ['PENDING', 'UNDER_REVIEW'],
    [ROLES.DISTRICT_OFFICER]: ['ESCALATED'],
    [ROLES.FINANCE_APPROVER]: ['FIELD_APPROVED', 'DISTRICT_APPROVED'],
};

/** Human-readable label for each role. */
const ROLE_LABELS = {
    [ROLES.FIELD_OFFICER]:    'Field Officer',
    [ROLES.DISTRICT_OFFICER]: 'District Officer',
    [ROLES.FINANCE_APPROVER]: 'Finance Approver',
};

/**
 * Actions that REQUIRE a remarks value (enforced at backend service layer).
 * We mirror this in the UI to prevent pointless failed requests.
 */
const REMARKS_REQUIRED_ACTIONS = new Set([
    'field-reject',
    'district-reject',
    'finance-reject',
    'request-correction',
]);

/** Status colour class map */
const STATUS_CLASS = {
    PENDING:           'status-pending',
    UNDER_REVIEW:      'status-review',
    FIELD_APPROVED:    'status-field-approved',
    ESCALATED:         'status-escalated',
    DISTRICT_APPROVED: 'status-district-approved',
    APPROVED:          'status-approved',
    REJECTED:          'status-rejected',
    CORRECTION_REQUIRED: 'status-correction',
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatDate(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d)) return dateStr;
    return d.toLocaleDateString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
    });
}

function formatDateTime(dateStr) {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    if (isNaN(d)) return dateStr;
    return d.toLocaleString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
}

// ─── API calls ────────────────────────────────────────────────────────────────

async function fetchApplicationsByStatus(status) {
    const res = await fetch(`/api/v1/applications?status=${status}`);
    if (!res.ok) throw new Error(`Server error ${res.status} fetching ${status} applications.`);
    return res.json();
}

async function fetchVerificationStatus(applicationId) {
    const res = await fetch(`/api/v1/verification/applications/${applicationId}`);
    if (!res.ok) throw new Error(`Server error ${res.status} fetching verification status.`);
    return res.json();
}

async function postVerificationAction(applicationId, action, performedBy, remarks) {
    const body = { performedBy };
    if (remarks && remarks.trim()) body.remarks = remarks.trim();

    const res = await fetch(`/api/v1/verification/applications/${applicationId}/${action}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });

    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
        throw new Error(data.message || data.error || `Action failed (HTTP ${res.status}).`);
    }
    return data;
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function StatusBadge({ status }) {
    const cls = STATUS_CLASS[status] || 'status-unknown';
    const label = status ? status.replace(/_/g, ' ') : 'Unknown';
    return <span className={`status-badge ${cls}`}>{label}</span>;
}

function ScoreBadge({ score }) {
    const cls = score >= 70 ? 'score-high' : score >= 40 ? 'score-mid' : 'score-low';
    return <span className={`score-badge ${cls}`}>{score}</span>;
}

/**
 * Inline action buttons for a single application row.
 * Decides which buttons to show based on the application's current status and the officer role.
 */
function ActionButtons({ app, role, officerUsername, onAction }) {
    const status = app.applicationStatus;

    if (role === ROLES.FIELD_OFFICER) {
        if (status === 'PENDING') {
            return (
                <button
                    className="action-btn btn-start"
                    onClick={() => onAction(app, 'start', false)}
                    title="Move to Under Review"
                >
                    ▶ Start Verification
                </button>
            );
        }
        if (status === 'UNDER_REVIEW') {
            return (
                <div className="action-group">
                    <button
                        className="action-btn btn-approve"
                        onClick={() => onAction(app, 'field-approve', false)}
                    >
                        ✓ Approve
                    </button>
                    <button
                        className="action-btn btn-reject"
                        onClick={() => onAction(app, 'field-reject', true)}
                    >
                        ✕ Reject
                    </button>
                    <button
                        className="action-btn btn-escalate"
                        onClick={() => onAction(app, 'escalate', false)}
                    >
                        ↑ Escalate
                    </button>
                    <button
                        className="action-btn btn-correction"
                        onClick={() => onAction(app, 'request-correction', true)}
                    >
                        ✎ Request Correction
                    </button>
                </div>
            );
        }
    }

    if (role === ROLES.DISTRICT_OFFICER && status === 'ESCALATED') {
        return (
            <div className="action-group">
                <button
                    className="action-btn btn-approve"
                    onClick={() => onAction(app, 'district-approve', false)}
                >
                    ✓ District Approve
                </button>
                <button
                    className="action-btn btn-reject"
                    onClick={() => onAction(app, 'district-reject', true)}
                >
                    ✕ District Reject
                </button>
            </div>
        );
    }

    if (role === ROLES.FINANCE_APPROVER &&
        (status === 'FIELD_APPROVED' || status === 'DISTRICT_APPROVED')) {
        return (
            <div className="action-group">
                <button
                    className="action-btn btn-approve"
                    onClick={() => onAction(app, 'finance-approve', false)}
                >
                    ✓ Finance Approve
                </button>
                <button
                    className="action-btn btn-reject"
                    onClick={() => onAction(app, 'finance-reject', true)}
                >
                    ✕ Finance Reject
                </button>
            </div>
        );
    }

    return <span className="no-action">—</span>;
}

/**
 * Remarks modal — shown when an action requires a reason before submitting.
 */
function RemarksModal({ action, app, onConfirm, onCancel, loading }) {
    const [remarks, setRemarks] = useState('');
    const [error, setError] = useState('');

    const actionLabel = action
        ? action.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
        : '';

    const required = REMARKS_REQUIRED_ACTIONS.has(action);

    const handleSubmit = () => {
        if (required && !remarks.trim()) {
            setError('Remarks are required for this action.');
            return;
        }
        setError('');
        onConfirm(remarks);
    };

    return (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-label="Action remarks">
            <div className="modal-card">
                <div className="modal-header">
                    <span className="modal-icon">📋</span>
                    <h2>{actionLabel}</h2>
                </div>
                <p className="modal-app-info">
                    Application <strong>#{app.applicationId}</strong> —{' '}
                    <strong>{app.beneficiaryName}</strong> ({app.schemeName})
                </p>

                <label className="modal-label" htmlFor="modal-remarks">
                    Remarks{required ? ' (required)' : ' (optional)'}
                </label>
                <textarea
                    id="modal-remarks"
                    className={`modal-textarea ${error ? 'textarea-error' : ''}`}
                    rows={4}
                    placeholder={
                        required
                            ? 'Provide a clear reason for this decision…'
                            : 'Add optional notes for this action…'
                    }
                    value={remarks}
                    onChange={e => {
                        setRemarks(e.target.value);
                        if (error) setError('');
                    }}
                    disabled={loading}
                    autoFocus
                />
                {error && <p className="modal-error" role="alert">{error}</p>}

                <div className="modal-actions">
                    <button
                        className="modal-btn modal-btn-cancel"
                        onClick={onCancel}
                        disabled={loading}
                    >
                        Cancel
                    </button>
                    <button
                        className="modal-btn modal-btn-confirm"
                        onClick={handleSubmit}
                        disabled={loading}
                    >
                        {loading ? 'Submitting…' : 'Confirm'}
                    </button>
                </div>
            </div>
        </div>
    );
}

/**
 * Verification history panel — shown when officer clicks a row.
 */
function HistoryPanel({ verificationData, onClose }) {
    if (!verificationData) return null;
    const { applicationId, beneficiaryName, schemeName, applicationStatus, history } = verificationData;

    return (
        <div className="history-overlay" onClick={onClose}>
            <div className="history-panel" onClick={e => e.stopPropagation()}>
                <div className="history-header">
                    <div>
                        <h2>Verification History</h2>
                        <p>
                            App #{applicationId} — {beneficiaryName} ({schemeName})
                        </p>
                    </div>
                    <button className="history-close-btn" onClick={onClose} aria-label="Close">✕</button>
                </div>

                <div className="history-current-status">
                    Current Status: <StatusBadge status={applicationStatus} />
                </div>

                {(!history || history.length === 0) ? (
                    <p className="history-empty">No verification actions recorded yet.</p>
                ) : (
                    <ol className="history-timeline">
                        {history.map((entry, i) => (
                            <li key={i} className={`history-item action-${(entry.action || '').toLowerCase()}`}>
                                <div className="history-item-header">
                                    <span className="history-stage">{entry.stage}</span>
                                    <span className="history-action">{entry.action}</span>
                                    <span className="history-by">by {entry.performedBy}</span>
                                </div>
                                <div className="history-time">{formatDateTime(entry.performedAt)}</div>
                                {entry.remarks && (
                                    <div className="history-remarks">"{entry.remarks}"</div>
                                )}
                            </li>
                        ))}
                    </ol>
                )}
            </div>
        </div>
    );
}

// ─── Main Component ───────────────────────────────────────────────────────────

/**
 * OfficerDashboard — role-aware work queue for government officers.
 *
 * Auth: reads officer session from localStorage (set by OfficerLogin).
 * If not authenticated, redirects to /officer/login immediately.
 *
 * Role-specific queues:
 *  FIELD_OFFICER    → PENDING + UNDER_REVIEW applications
 *  DISTRICT_OFFICER → ESCALATED applications
 *  FINANCE_APPROVER → FIELD_APPROVED + DISTRICT_APPROVED applications
 */
function OfficerDashboard() {
    const navigate = useNavigate();

    // ── Session ──────────────────────────────────────────────────────────────

    const officerLoggedIn  = localStorage.getItem('officerLoggedIn') === 'true';
    const officerName      = localStorage.getItem('officerName')      || 'Officer';
    const officerRole      = localStorage.getItem('officerRole')      || '';
    const officerDistrict  = localStorage.getItem('officerDistrict')  || '';
    const officerUsername  = localStorage.getItem('officerUsername')  || '';

    // Redirect if not authenticated
    useEffect(() => {
        if (!officerLoggedIn) {
            navigate('/officer/login', { replace: true });
        }
    }, [officerLoggedIn, navigate]);

    // ── State ────────────────────────────────────────────────────────────────

    const [applications, setApplications]   = useState([]);
    const [loading, setLoading]             = useState(true);
    const [fetchError, setFetchError]       = useState('');
    const [successMsg, setSuccessMsg]       = useState('');
    const [actionError, setActionError]     = useState('');

    // Modal state
    const [pendingAction, setPendingAction] = useState(null); // { app, action, requiresRemarks }
    const [actionLoading, setActionLoading] = useState(false);

    // Detail/history panel
    const [historyData, setHistoryData]     = useState(null);
    const [historyLoading, setHistoryLoading] = useState(false);

    // ── Data loading ─────────────────────────────────────────────────────────

    const loadApplications = useCallback(async () => {
        if (!officerRole || !ROLE_STATUSES[officerRole]) return;

        setLoading(true);
        setFetchError('');

        try {
            const statusList = ROLE_STATUSES[officerRole];
            // Fetch all relevant statuses in parallel
            const results = await Promise.all(
                statusList.map(s => fetchApplicationsByStatus(s))
            );
            // Flatten and sort by date descending
            const all = results.flat().sort((a, b) =>
                new Date(b.applicationDate) - new Date(a.applicationDate)
            );
            setApplications(all);
        } catch (err) {
            setFetchError(err.message || 'Failed to load applications.');
        } finally {
            setLoading(false);
        }
    }, [officerRole]);

    useEffect(() => {
        if (officerLoggedIn) {
            loadApplications();
        }
    }, [officerLoggedIn, loadApplications]);

    // Auto-clear success/error messages
    useEffect(() => {
        if (successMsg) {
            const t = setTimeout(() => setSuccessMsg(''), 5000);
            return () => clearTimeout(t);
        }
    }, [successMsg]);

    useEffect(() => {
        if (actionError) {
            const t = setTimeout(() => setActionError(''), 7000);
            return () => clearTimeout(t);
        }
    }, [actionError]);

    // ── Logout ───────────────────────────────────────────────────────────────

    const handleLogout = () => {
        ['officerLoggedIn', 'officerId', 'officerUsername',
         'officerName', 'officerRole', 'officerDistrict'].forEach(
            key => localStorage.removeItem(key)
        );
        navigate('/officer/login', { replace: true });
    };

    // ── Row click → history panel ─────────────────────────────────────────────

    const handleRowClick = async (app) => {
        setHistoryLoading(true);
        setHistoryData(null);
        try {
            const data = await fetchVerificationStatus(app.applicationId);
            setHistoryData(data);
        } catch {
            // silently fail — history panel just won't open
        } finally {
            setHistoryLoading(false);
        }
    };

    // ── Action handlers ───────────────────────────────────────────────────────

    /**
     * Called when an action button is clicked.
     * If remarks are required (or optional but desired), open modal.
     * For "start" action, we skip modal and proceed directly with empty remarks.
     */
    const handleActionClick = (app, action, requiresRemarks) => {
        setActionError('');
        setSuccessMsg('');
        setPendingAction({ app, action, requiresRemarks });
    };

    const handleModalConfirm = async (remarks) => {
        if (!pendingAction) return;
        const { app, action } = pendingAction;

        setActionLoading(true);
        try {
            await postVerificationAction(
                app.applicationId,
                action,
                officerUsername,
                remarks
            );
            setSuccessMsg(
                `Action "${action.replace(/-/g, ' ')}" completed for Application #${app.applicationId}.`
            );
            setPendingAction(null);
            await loadApplications();   // Refresh the list
        } catch (err) {
            setActionError(err.message || 'Action failed. Please try again.');
            setPendingAction(null);
        } finally {
            setActionLoading(false);
        }
    };

    const handleModalCancel = () => {
        setPendingAction(null);
        setActionError('');
    };

    // ── Guard: if not logged in, render nothing (redirect in useEffect) ────────

    if (!officerLoggedIn) return null;

    // ── Derived ───────────────────────────────────────────────────────────────

    const roleLabel    = ROLE_LABELS[officerRole] || officerRole;
    const roleClass    = (officerRole || '').toLowerCase().replace(/_/g, '-');
    const statusLabels = (ROLE_STATUSES[officerRole] || []).join(', ');

    // ── Render ────────────────────────────────────────────────────────────────

    return (
        <div className="od-page">

            {/* ── Top bar ─────────────────────────────────────────────────── */}
            <header className="od-topbar">
                <div className="od-topbar-inner">
                    <div className="od-brand">
                        <span className="od-brand-icon">🏛️</span>
                        <div>
                            <span className="od-brand-title">DSGP Officer Portal</span>
                            <span className="od-brand-sub">Digital Subsidy &amp; Grant Platform</span>
                        </div>
                    </div>

                    <div className="od-officer-info">
                        <span className={`od-role-badge role-${roleClass}`}>{roleLabel}</span>
                        <div className="od-officer-details">
                            <span className="od-officer-name">{officerName}</span>
                            {officerDistrict && (
                                <span className="od-officer-district">📍 {officerDistrict}</span>
                            )}
                        </div>
                        <button
                            id="btn-officer-logout"
                            className="od-logout-btn"
                            onClick={handleLogout}
                        >
                            Sign Out
                        </button>
                    </div>
                </div>
            </header>

            {/* ── Content ─────────────────────────────────────────────────── */}
            <main className="od-main">

                {/* Queue heading */}
                <div className="od-queue-header">
                    <div>
                        <h1 className="od-queue-title">Work Queue</h1>
                        <p className="od-queue-sub">
                            Showing applications with status:{' '}
                            <strong>{statusLabels || '—'}</strong>
                        </p>
                    </div>
                    <button
                        className="od-refresh-btn"
                        onClick={loadApplications}
                        disabled={loading}
                        title="Refresh application list"
                    >
                        {loading ? '↻ Loading…' : '↻ Refresh'}
                    </button>
                </div>

                {/* Success / Error banners */}
                {successMsg && (
                    <div className="od-banner od-banner-success" role="status">
                        <span>✓</span> {successMsg}
                    </div>
                )}
                {actionError && (
                    <div className="od-banner od-banner-error" role="alert">
                        <span>✕</span> {actionError}
                    </div>
                )}
                {fetchError && (
                    <div className="od-banner od-banner-error" role="alert">
                        <span>✕</span> {fetchError}
                        <button className="od-retry-link" onClick={loadApplications}>Retry</button>
                    </div>
                )}

                {/* Loading state */}
                {loading && (
                    <div className="od-loading-state">
                        <div className="od-spinner" />
                        <p>Loading applications…</p>
                    </div>
                )}

                {/* Empty state */}
                {!loading && !fetchError && applications.length === 0 && (
                    <div className="od-empty-state">
                        <div className="od-empty-icon">📭</div>
                        <h2>No Applications in Queue</h2>
                        <p>
                            There are currently no applications with status{' '}
                            <strong>{statusLabels}</strong> assigned to your work queue.
                        </p>
                    </div>
                )}

                {/* Applications table */}
                {!loading && applications.length > 0 && (
                    <div className="od-table-wrapper">
                        <table className="od-table" aria-label="Applications work queue">
                            <thead>
                                <tr>
                                    <th>App ID</th>
                                    <th>Beneficiary Name</th>
                                    <th>Scheme</th>
                                    <th>Eligibility Score</th>
                                    <th>Date</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {applications.map(app => (
                                    <tr
                                        key={app.applicationId}
                                        className="od-table-row"
                                        title="Click to view verification history"
                                    >
                                        <td
                                            className="od-app-id"
                                            onClick={() => handleRowClick(app)}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            #{app.applicationId}
                                        </td>
                                        <td
                                            className="od-beneficiary-name"
                                            onClick={() => handleRowClick(app)}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            {app.beneficiaryName}
                                        </td>
                                        <td
                                            className="od-scheme-name"
                                            onClick={() => handleRowClick(app)}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            {app.schemeName}
                                        </td>
                                        <td onClick={() => handleRowClick(app)} style={{ cursor: 'pointer' }}>
                                            <ScoreBadge score={app.eligibilityScore} />
                                        </td>
                                        <td
                                            className="od-date"
                                            onClick={() => handleRowClick(app)}
                                            style={{ cursor: 'pointer' }}
                                        >
                                            {formatDate(app.applicationDate)}
                                        </td>
                                        <td onClick={() => handleRowClick(app)} style={{ cursor: 'pointer' }}>
                                            <StatusBadge status={app.applicationStatus} />
                                        </td>
                                        <td className="od-actions-cell" onClick={e => e.stopPropagation()}>
                                            <ActionButtons
                                                app={app}
                                                role={officerRole}
                                                officerUsername={officerUsername}
                                                onAction={handleActionClick}
                                            />
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>

                        <p className="od-table-hint">
                            💡 Click any row (except Actions column) to view verification history.
                            {historyLoading && ' Loading history…'}
                        </p>
                    </div>
                )}

            </main>

            {/* ── Remarks modal ──────────────────────────────────────────── */}
            {pendingAction && (
                <RemarksModal
                    action={pendingAction.action}
                    app={pendingAction.app}
                    onConfirm={handleModalConfirm}
                    onCancel={handleModalCancel}
                    loading={actionLoading}
                />
            )}

            {/* ── Verification history panel ─────────────────────────────── */}
            {historyData && (
                <HistoryPanel
                    verificationData={historyData}
                    onClose={() => setHistoryData(null)}
                />
            )}

        </div>
    );
}

export default OfficerDashboard;
