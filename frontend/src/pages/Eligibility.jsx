import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Eligibility.css';

/* ─── API base (Vite proxy forwards /api → localhost:8080) ─── */
const API_BASE = '/api/v1';

/* ─── Schemes known to the platform ─────────────────────────── */
const KNOWN_SCHEMES = [
    {
        id: 1,
        backendId: 1,           // matches Scheme.id in DB
        name: 'PM-KISAN',
        fullName: 'Pradhan Mantri Kisan Samman Nidhi',
        category: 'Agriculture',
        target: 'Farmers',
        description:
            'Income support scheme providing ₹6,000 per year to eligible farmer families.',
        path: '/schemes/pm-kisan',
    },
    {
        id: 2,
        backendId: 2,
        name: 'National Scholarship Portal (NSP)',
        fullName: 'National Scholarship Portal',
        category: 'Education',
        target: 'Students',
        description:
            'Centralised platform offering scholarships for eligible students.',
        path: '/schemes/nsp',
    },
    {
        id: 3,
        backendId: 3,
        name: 'PMEGP',
        fullName: "Prime Minister's Employment Generation Programme",
        category: 'Business & Entrepreneurship',
        target: 'Entrepreneurs',
        description:
            'Credit-linked subsidy scheme supporting self-employment through micro-enterprises.',
        path: '/schemes/pmegp',
    },
];

/* ─── Criterion labels ───────────────────────────────────────── */
const CRITERION_LABELS = {
    ageCheck:      'Age Range',
    incomeCheck:   'Annual Income',
    landCheck:     'Land Holding',
    categoryCheck: 'Social Category',
    identityCheck: 'Identity Verified',
};

const CRITERION_MAX = {
    ageCheck:      20,
    incomeCheck:   30,
    landCheck:     20,
    categoryCheck: 20,
    identityCheck: 10,
};

/* ═══════════════════════════════════════════════════════════════
   MAIN COMPONENT
═══════════════════════════════════════════════════════════════ */
function Eligibility() {

    /* ── Active tab: 'quick' | 'live' ── */
    const [activeTab, setActiveTab] = useState('live');

    /* ── Quick-check (static) state ── */
    const [quickForm, setQuickForm] = useState({
        state: '', age: '', occupation: '', category: '', income: '',
    });
    const [quickResults, setQuickResults] = useState([]);
    const [quickSubmitted, setQuickSubmitted] = useState(false);

    /* ── Live scoring (backend) state ── */
    const [liveForm, setLiveForm] = useState({
        beneficiaryId: '',
        schemeId: '',
    });
    const [liveLoading, setLiveLoading] = useState(false);
    const [liveResult, setLiveResult]   = useState(null);
    const [liveError, setLiveError]     = useState('');
    const [liveValidation, setLiveValidation] = useState({});

    /* ════════════════════════════════════════════════════════════
       QUICK CHECK — static local filter (original Milestone 1 behaviour)
    ════════════════════════════════════════════════════════════ */
    const handleQuickChange = (e) =>
        setQuickForm({ ...quickForm, [e.target.name]: e.target.value });

    const handleQuickSubmit = (e) => {
        e.preventDefault();
        const eligible = [];
        if (quickForm.occupation === 'farmer')
            eligible.push(KNOWN_SCHEMES[0]);
        if (quickForm.occupation === 'student')
            eligible.push(KNOWN_SCHEMES[1]);
        if (quickForm.occupation === 'entrepreneur' || quickForm.occupation === 'business-owner')
            eligible.push(KNOWN_SCHEMES[2]);
        setQuickResults(eligible);
        setQuickSubmitted(true);
    };

    /* ════════════════════════════════════════════════════════════
       LIVE SCORING — backend API call
    ════════════════════════════════════════════════════════════ */
    const validateLiveForm = () => {
        const errors = {};
        if (!liveForm.beneficiaryId || isNaN(Number(liveForm.beneficiaryId)) || Number(liveForm.beneficiaryId) < 1)
            errors.beneficiaryId = 'Enter a valid Beneficiary ID (positive integer).';
        if (!liveForm.schemeId)
            errors.schemeId = 'Please select a scheme.';
        return errors;
    };

    const handleLiveChange = (e) => {
        setLiveForm({ ...liveForm, [e.target.name]: e.target.value });
        if (liveValidation[e.target.name])
            setLiveValidation({ ...liveValidation, [e.target.name]: '' });
        if (liveError) setLiveError('');
        if (liveResult) setLiveResult(null);
    };

    const handleLiveSubmit = async (e) => {
        e.preventDefault();
        setLiveResult(null);
        setLiveError('');

        const errors = validateLiveForm();
        if (Object.keys(errors).length > 0) {
            setLiveValidation(errors);
            return;
        }

        setLiveLoading(true);
        try {
            const payload = {
                beneficiaryId: parseInt(liveForm.beneficiaryId, 10),
                schemeId:      parseInt(liveForm.schemeId, 10),
            };

            const res = await fetch(`${API_BASE}/eligibility/check`, {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify(payload),
            });

            if (!res.ok) {
                const body = await res.json().catch(() => ({}));
                const msg = body.message || body.error || `Server error (${res.status})`;
                setLiveError(msg);
                return;
            }

            const data = await res.json();
            setLiveResult(data);
        } catch (err) {
            setLiveError(
                'Could not reach the backend. Make sure the Spring Boot server is running on port 8080.'
            );
        } finally {
            setLiveLoading(false);
        }
    };

    const resetLive = () => {
        setLiveForm({ beneficiaryId: '', schemeId: '' });
        setLiveResult(null);
        setLiveError('');
        setLiveValidation({});
    };

    /* ── helpers ── */
    const scoreColor = (score) => {
        if (score >= 80) return 'var(--color-success)';
        if (score >= 60) return 'var(--color-warning)';
        return '#dc2626';
    };

    const scoreLabel = (score) => {
        if (score >= 80) return 'Strong';
        if (score >= 60) return 'Sufficient';
        return 'Insufficient';
    };

    /* ════════════════════════════════════════════════════════════
       RENDER
    ════════════════════════════════════════════════════════════ */
    return (
        <div className="eligibility-page">

            {/* ── Hero ── */}
            <section className="eligibility-hero">
                <div className="eligibility-hero-content">
                    <h1>Check Your Eligibility</h1>
                    <p>
                        Discover government schemes you qualify for — instantly, using
                        our weighted scoring engine.
                    </p>
                </div>
            </section>

            {/* ── Tab bar ── */}
            <div className="elig-tab-bar">
                <div className="elig-tab-bar-inner">
                    <button
                        id="tab-quick"
                        className={`elig-tab-btn${activeTab === 'quick' ? ' active' : ''}`}
                        onClick={() => setActiveTab('quick')}
                        type="button"
                        aria-selected={activeTab === 'quick'}
                    >
                        Quick Scheme Finder
                    </button>
                    <button
                        id="tab-live"
                        className={`elig-tab-btn${activeTab === 'live' ? ' active' : ''}`}
                        onClick={() => setActiveTab('live')}
                        type="button"
                        aria-selected={activeTab === 'live'}
                    >
                        Live Eligibility Score
                        <span className="elig-tab-badge">Live</span>
                    </button>
                </div>
            </div>

            <main className="eligibility-container">

                {/* ══════════════════════════════════════════════
                    TAB 1 — QUICK CHECK (static, original behaviour)
                ══════════════════════════════════════════════ */}
                {activeTab === 'quick' && (
                    <>
                        <div className="eligibility-card">
                            <div className="form-heading">
                                <h2>Your Profile</h2>
                                <p>Provide your details to find relevant government schemes.</p>
                            </div>

                            <form onSubmit={handleQuickSubmit}>
                                <div className="eligibility-form-group">
                                    <label htmlFor="state">State / Union Territory</label>
                                    <select id="state" name="state" value={quickForm.state}
                                            onChange={handleQuickChange} required>
                                        <option value="">Select your state</option>
                                        <option value="andhra-pradesh">Andhra Pradesh</option>
                                        <option value="karnataka">Karnataka</option>
                                        <option value="maharashtra">Maharashtra</option>
                                        <option value="tamil-nadu">Tamil Nadu</option>
                                        <option value="delhi">Delhi</option>
                                        <option value="other">Other</option>
                                    </select>
                                </div>

                                <div className="eligibility-form-group">
                                    <label htmlFor="age">Age Group</label>
                                    <select id="age" name="age" value={quickForm.age}
                                            onChange={handleQuickChange} required>
                                        <option value="">Select your age group</option>
                                        <option value="below-18">Below 18</option>
                                        <option value="18-25">18 – 25</option>
                                        <option value="26-40">26 – 40</option>
                                        <option value="41-60">41 – 60</option>
                                        <option value="above-60">Above 60</option>
                                    </select>
                                </div>

                                <div className="eligibility-form-group">
                                    <label htmlFor="occupation">Occupation / Status</label>
                                    <select id="occupation" name="occupation"
                                            value={quickForm.occupation}
                                            onChange={handleQuickChange} required>
                                        <option value="">Select your occupation</option>
                                        <option value="farmer">Farmer</option>
                                        <option value="student">Student</option>
                                        <option value="entrepreneur">Entrepreneur</option>
                                        <option value="business-owner">Small Business Owner</option>
                                        <option value="other">Other</option>
                                    </select>
                                </div>

                                <div className="eligibility-form-group">
                                    <label htmlFor="category">Social Category</label>
                                    <select id="category" name="category"
                                            value={quickForm.category}
                                            onChange={handleQuickChange} required>
                                        <option value="">Select your category</option>
                                        <option value="general">General</option>
                                        <option value="obc">OBC</option>
                                        <option value="sc">SC</option>
                                        <option value="st">ST</option>
                                        <option value="ews">EWS</option>
                                    </select>
                                </div>

                                <div className="eligibility-form-group">
                                    <label htmlFor="income">Annual Family Income</label>
                                    <select id="income" name="income" value={quickForm.income}
                                            onChange={handleQuickChange} required>
                                        <option value="">Select annual income</option>
                                        <option value="below-1">Below ₹1 Lakh</option>
                                        <option value="1-3">₹1 – ₹3 Lakhs</option>
                                        <option value="3-5">₹3 – ₹5 Lakhs</option>
                                        <option value="5-10">₹5 – ₹10 Lakhs</option>
                                        <option value="above-10">Above ₹10 Lakhs</option>
                                    </select>
                                </div>

                                <button type="submit" className="eligibility-submit">
                                    Find Eligible Schemes
                                </button>
                            </form>
                        </div>

                        {quickSubmitted && (
                            <section className="eligibility-results">
                                <div className="results-heading">
                                    <h2>Eligible Schemes</h2>
                                    {quickResults.length > 0 ? (
                                        <p>Based on your profile, you may be eligible for the following schemes.</p>
                                    ) : (
                                        <p>No matching schemes found. Try adjusting your occupation or category.</p>
                                    )}
                                </div>

                                <div className="eligibility-results-grid">
                                    {quickResults.map((scheme) => (
                                        <div className="eligibility-scheme-card" key={scheme.id}>
                                            <div className="result-card-header">
                                                <span className="result-category">{scheme.category}</span>
                                                <span className="result-target">{scheme.target}</span>
                                            </div>
                                            <h3>{scheme.name}</h3>
                                            <p>{scheme.description}</p>
                                            <Link to={scheme.path} className="result-button">
                                                View Details
                                            </Link>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        )}
                    </>
                )}

                {/* ══════════════════════════════════════════════
                    TAB 2 — LIVE SCORING (backend-connected)
                ══════════════════════════════════════════════ */}
                {activeTab === 'live' && (
                    <>
                        <div className="eligibility-card live-card">
                            <div className="form-heading">
                                <h2>Live Eligibility Score</h2>
                                <p>
                                    Enter your registered Beneficiary ID and select a scheme.
                                    Our weighted scoring engine will evaluate your profile
                                    against the scheme's criteria in real time.
                                </p>
                            </div>

                            {/* Info box */}
                            <div className="live-info-box">
                                <span className="live-info-icon">ℹ</span>
                                <span>
                                    You must be registered as a beneficiary to use live scoring.
                                    Score ≥&nbsp;60 → <strong>ELIGIBLE</strong>.
                                    Score &lt;&nbsp;60 → <strong>INELIGIBLE</strong>.
                                </span>
                            </div>

                            <form id="live-eligibility-form" onSubmit={handleLiveSubmit} noValidate>

                                {/* Beneficiary ID */}
                                <div className="eligibility-form-group">
                                    <label htmlFor="beneficiaryId">
                                        Beneficiary ID
                                        <span className="field-required" aria-hidden="true"> *</span>
                                    </label>
                                    <input
                                        id="beneficiaryId"
                                        name="beneficiaryId"
                                        type="number"
                                        min="1"
                                        placeholder="e.g. 101"
                                        value={liveForm.beneficiaryId}
                                        onChange={handleLiveChange}
                                        className={`elig-input${liveValidation.beneficiaryId ? ' input-error' : ''}`}
                                        aria-describedby={liveValidation.beneficiaryId ? 'err-beneficiaryId' : undefined}
                                    />
                                    {liveValidation.beneficiaryId && (
                                        <span id="err-beneficiaryId" className="field-error" role="alert">
                                            {liveValidation.beneficiaryId}
                                        </span>
                                    )}
                                    <span className="field-hint">
                                        Your numeric ID assigned during beneficiary registration.
                                    </span>
                                </div>

                                {/* Scheme selector */}
                                <div className="eligibility-form-group">
                                    <label htmlFor="schemeId">
                                        Scheme
                                        <span className="field-required" aria-hidden="true"> *</span>
                                    </label>
                                    <select
                                        id="schemeId"
                                        name="schemeId"
                                        value={liveForm.schemeId}
                                        onChange={handleLiveChange}
                                        className={liveValidation.schemeId ? 'input-error' : ''}
                                        aria-describedby={liveValidation.schemeId ? 'err-schemeId' : undefined}
                                    >
                                        <option value="">Select a scheme to evaluate against</option>
                                        {KNOWN_SCHEMES.map((s) => (
                                            <option key={s.backendId} value={s.backendId}>
                                                {s.name} — {s.category}
                                            </option>
                                        ))}
                                    </select>
                                    {liveValidation.schemeId && (
                                        <span id="err-schemeId" className="field-error" role="alert">
                                            {liveValidation.schemeId}
                                        </span>
                                    )}
                                </div>

                                {/* Submit */}
                                <button
                                    id="btn-check-eligibility"
                                    type="submit"
                                    className="eligibility-submit"
                                    disabled={liveLoading}
                                >
                                    {liveLoading ? (
                                        <span className="btn-loading">
                                            <span className="spinner" aria-hidden="true" />
                                            Evaluating…
                                        </span>
                                    ) : 'Check Eligibility'}
                                </button>
                            </form>
                        </div>

                        {/* ── API Error ── */}
                        {liveError && (
                            <div className="live-alert live-alert-error" role="alert">
                                <span className="live-alert-icon">✕</span>
                                <div>
                                    <strong>Eligibility check failed</strong>
                                    <p>{liveError}</p>
                                </div>
                            </div>
                        )}

                        {/* ── Result panel ── */}
                        {liveResult && (
                            <section className="live-result-panel" aria-live="polite">

                                {/* Verdict banner */}
                                <div className={`verdict-banner ${liveResult.eligible ? 'verdict-eligible' : 'verdict-ineligible'}`}>
                                    <div className="verdict-icon" aria-hidden="true">
                                        {liveResult.eligible ? '✓' : '✕'}
                                    </div>
                                    <div className="verdict-text">
                                        <h2>
                                            {liveResult.eligible ? 'ELIGIBLE' : 'INELIGIBLE'}
                                        </h2>
                                        <p>
                                            {liveResult.eligible
                                                ? `Your profile qualifies for ${liveResult.schemeName}.`
                                                : `Your profile does not meet the minimum score for ${liveResult.schemeName}.`}
                                        </p>
                                    </div>
                                    <button
                                        className="verdict-reset"
                                        onClick={resetLive}
                                        type="button"
                                        aria-label="Run another check"
                                    >
                                        Check Again
                                    </button>
                                </div>

                                {/* Score + criteria grid */}
                                <div className="result-details-grid">

                                    {/* Score dial */}
                                    <div className="score-dial-card">
                                        <h3 className="score-dial-title">Eligibility Score</h3>
                                        <div className="score-dial"
                                             style={{ '--score-color': scoreColor(liveResult.totalScore) }}>
                                            <svg viewBox="0 0 120 120" className="score-svg" aria-hidden="true">
                                                {/* Track */}
                                                <circle cx="60" cy="60" r="50"
                                                        fill="none" stroke="var(--color-gray-200)"
                                                        strokeWidth="10" />
                                                {/* Progress arc */}
                                                <circle cx="60" cy="60" r="50"
                                                        fill="none"
                                                        stroke={scoreColor(liveResult.totalScore)}
                                                        strokeWidth="10"
                                                        strokeLinecap="round"
                                                        strokeDasharray={`${(liveResult.totalScore / 100) * 314} 314`}
                                                        transform="rotate(-90 60 60)" />
                                            </svg>
                                            <div className="score-value">
                                                <span className="score-number"
                                                      style={{ color: scoreColor(liveResult.totalScore) }}>
                                                    {liveResult.totalScore}
                                                </span>
                                                <span className="score-max">/100</span>
                                            </div>
                                        </div>
                                        <div className="score-label"
                                             style={{ color: scoreColor(liveResult.totalScore) }}>
                                            {scoreLabel(liveResult.totalScore)}
                                        </div>
                                        <div className="score-threshold-note">
                                            Minimum score to qualify: <strong>60</strong>
                                        </div>
                                        <div className="score-meta">
                                            <div className="score-meta-row">
                                                <span>Scheme</span>
                                                <strong>{liveResult.schemeName}</strong>
                                            </div>
                                            <div className="score-meta-row">
                                                <span>Beneficiary ID</span>
                                                <strong>#{liveResult.beneficiaryId}</strong>
                                            </div>
                                            <div className="score-meta-row">
                                                <span>Evaluated at</span>
                                                <strong>
                                                    {new Date(liveResult.evaluatedAt).toLocaleString('en-IN')}
                                                </strong>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Criteria breakdown */}
                                    <div className="criteria-card">
                                        <h3 className="criteria-title">Criteria Breakdown</h3>
                                        <p className="criteria-subtitle">
                                            Detailed evaluation against each eligibility criterion
                                        </p>

                                        <div className="criteria-list">
                                            {liveResult.criteria &&
                                                Object.entries(liveResult.criteria).map(([key, crit]) => (
                                                    <div
                                                        key={key}
                                                        className={`criterion-row ${crit.passed ? 'crit-pass' : 'crit-fail'}`}
                                                    >
                                                        <div className="crit-header">
                                                            <span className={`crit-icon ${crit.passed ? 'crit-icon-pass' : 'crit-icon-fail'}`}
                                                                  aria-hidden="true">
                                                                {crit.passed ? '✓' : '✕'}
                                                            </span>
                                                            <span className="crit-name">
                                                                {CRITERION_LABELS[key] ?? key}
                                                            </span>
                                                            <span className="crit-points">
                                                                {crit.points}/{CRITERION_MAX[key] ?? '?'} pts
                                                            </span>
                                                        </div>

                                                        {/* Progress bar */}
                                                        <div className="crit-bar-track"
                                                             role="progressbar"
                                                             aria-valuenow={crit.points}
                                                             aria-valuemax={CRITERION_MAX[key]}
                                                             aria-label={`${CRITERION_LABELS[key]} score`}>
                                                            <div
                                                                className={`crit-bar-fill ${crit.passed ? 'bar-pass' : 'bar-fail'}`}
                                                                style={{
                                                                    width: `${(crit.points / (CRITERION_MAX[key] ?? 100)) * 100}%`
                                                                }}
                                                            />
                                                        </div>

                                                        <p className="crit-detail">{crit.detail}</p>
                                                    </div>
                                                ))}
                                        </div>
                                    </div>
                                </div>

                                {/* Next steps */}
                                {liveResult.eligible && (
                                    <div className="next-steps-card">
                                        <h3>🎉 Next Steps</h3>
                                        <p>
                                            Your eligibility score qualifies you for{' '}
                                            <strong>{liveResult.schemeName}</strong>. A Field Officer
                                            will review your application and verify your documents.
                                        </p>
                                        <div className="next-steps-actions">
                                            <Link
                                                to={KNOWN_SCHEMES.find(
                                                    (s) => s.backendId === liveResult.schemeId
                                                )?.path ?? '/schemes'}
                                                className="result-button"
                                            >
                                                View Scheme Details
                                            </Link>
                                            <Link to="/track" className="result-button result-button-secondary">
                                                Track Application
                                            </Link>
                                        </div>
                                    </div>
                                )}

                                {!liveResult.eligible && (
                                    <div className="next-steps-card next-steps-ineligible">
                                        <h3>What can you do?</h3>
                                        <p>
                                            Your current profile does not meet the minimum score of 60 for{' '}
                                            <strong>{liveResult.schemeName}</strong>. You may:
                                        </p>
                                        <ul>
                                            <li>Update your beneficiary profile with complete eligibility data.</li>
                                            <li>Complete identity verification with a Field Officer (+10 pts).</li>
                                            <li>Check eligibility for a different scheme.</li>
                                        </ul>
                                        <button
                                            className="eligibility-submit"
                                            style={{ marginTop: '1rem', maxWidth: '16rem' }}
                                            onClick={resetLive}
                                            type="button"
                                        >
                                            Try Another Scheme
                                        </button>
                                    </div>
                                )}

                            </section>
                        )}
                    </>
                )}

            </main>
        </div>
    );
}

export default Eligibility;