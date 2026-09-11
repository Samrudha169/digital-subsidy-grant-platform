import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import './Eligibility.css';

/* ─── API base (Vite proxy forwards /api → localhost:8080) ─── */
const API_BASE = '/api/v1';

/* ─── Schemes known to the platform ─────────────────────────── */
const KNOWN_SCHEMES = [
    {
        id: 1,
        backendId: 1,
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
    ageCheck: 'Age Range',
    incomeCheck: 'Annual Income',
    landCheck: 'Land Holding',
    occupationCheck: 'Occupation',
    categoryCheck: 'Social Category',
    identityCheck: 'Identity Verified',
};

/* ─── Scheme-specific criterion maximums ─────────────────────
   These values MUST match EligibilityScoringEngine.java exactly.

   PM-KISAN  (scheme id 1 / name "PM-KISAN"):
     age=15, income=25, land=25, occupation=20, category=5, identity=10
     Total = 100

   NSP  (scheme id 2 / name contains "National Scholarship"):
     age=20, income=30, occupation=30, category=10, identity=10
     (no landCheck criterion for NSP)  Total = 100

   PMEGP  (scheme id 3 / name "PMEGP"):
     age=20, income=30, occupation=20, category=20, identity=10
     (no landCheck criterion for PMEGP)  Total = 100
─────────────────────────────────────────────────────────── */
const CRITERION_MAX_BY_SCHEME = {
    'PM-KISAN': {
        ageCheck:        15,
        incomeCheck:     25,
        landCheck:       25,
        occupationCheck: 20,
        categoryCheck:    5,
        identityCheck:   10,
    },
    'NSP': {
        ageCheck:        20,
        incomeCheck:     30,
        occupationCheck: 30,
        categoryCheck:   10,
        identityCheck:   10,
    },
    'PMEGP': {
        ageCheck:        20,
        incomeCheck:     30,
        occupationCheck: 20,
        categoryCheck:   20,
        identityCheck:   10,
    },
};

/** Returns the criterion maximum for a given scheme key and criterion name.
 *  Falls back to a sensible default if the key is unknown. */
function getCriterionMax(schemeKey, criterion) {
    const map = CRITERION_MAX_BY_SCHEME[schemeKey];
    if (map && criterion in map) return map[criterion];
    // Fallback: return the max across all schemes for that criterion
    const allMaps = Object.values(CRITERION_MAX_BY_SCHEME);
    return Math.max(...allMaps.map(m => m[criterion] ?? 0));
}

/** Derives the schemeKey ('PM-KISAN' | 'NSP' | 'PMEGP') from a scheme name string. */
function resolveSchemeKey(schemeName) {
    if (!schemeName) return null;
    const n = schemeName.toUpperCase();
    if (n.includes('KISAN') || n.includes('PM-KISAN')) return 'PM-KISAN';
    if (n.includes('NSP') || n.includes('NATIONAL SCHOLARSHIP')) return 'NSP';
    if (n.includes('PMEGP') || n.includes('EMPLOYMENT GENERATION')) return 'PMEGP';
    return null;
}

/* ─── Required documents ──────────────────────────────────── */

const COMMON_DOCUMENTS = [
    { type: 'IDENTITY_PROOF', label: 'Aadhaar Card / PAN Card' },
    { type: 'ADDRESS_PROOF', label: 'Address Proof (Domicile / Electricity Bill)' },
    { type: 'INCOME_CERTIFICATE', label: 'Income Certificate' },
    { type: 'CATEGORY_CERTIFICATE', label: 'Caste / Category Certificate' },
    { type: 'BANK_ACCOUNT_PROOF', label: 'Bank Passbook / Cancelled Cheque' },
];

const SCHEME_DOCUMENTS = {
    'PM-KISAN': [
        { type: 'LAND_RECORD', label: 'Land Record / 7-12 Extract' },
        { type: 'OCCUPATION_PROOF', label: 'Farmer Proof / 7-12 Extract' },
        { type: 'SCHEME_SPECIFIC_DOCUMENT', label: 'PM-KISAN Land & Farmer Record' },
        { type: 'OTHER_SUPPORTING_DOCUMENT', label: 'Any Other Supporting Document' },
    ],

    'NSP': [
        { type: 'OCCUPATION_PROOF', label: 'College ID Card / Bonafide Certificate' },
        { type: 'SCHEME_SPECIFIC_DOCUMENT', label: 'College Admission Letter / Fee Receipt' },
        { type: 'OTHER_SUPPORTING_DOCUMENT', label: 'Previous Year Marksheet' },
    ],

    'PMEGP': [
        { type: 'OCCUPATION_PROOF', label: 'Business / Work Experience Proof' },
        { type: 'SCHEME_SPECIFIC_DOCUMENT', label: 'Business Project Report' },
        { type: 'OTHER_SUPPORTING_DOCUMENT', label: 'Training / Skill Certificate' },
    ],
};

function getRequiredDocs(schemeKey) {
    return [
        ...COMMON_DOCUMENTS,
        ...(SCHEME_DOCUMENTS[schemeKey] || []),
    ];
}

/* ═══════════════════════════════════════════════════════════════
   MAIN COMPONENT
═══════════════════════════════════════════════════════════════ */
function Eligibility() {

    /*
     * Reads:
     * /eligibility?scheme=PM-KISAN
     * /eligibility?scheme=NSP
     * /eligibility?scheme=PMEGP
     */
    const [searchParams] = useSearchParams();

    /* ── Active tab: 'quick' | 'live' ── */
    const [activeTab, setActiveTab] = useState('live');

    /* ── Quick-check (static) state ── */
    const [quickForm, setQuickForm] = useState({
        state: '',
        age: '',
        occupation: '',
        category: '',
        income: '',
    });

    const [quickResults, setQuickResults] = useState([]);
    const [quickSubmitted, setQuickSubmitted] = useState(false);

    /* ── Live scoring (backend) state ── */
    const [liveForm, setLiveForm] = useState({
        beneficiaryId: '',
        schemeId: '',
    });

    const [liveLoading, setLiveLoading] = useState(false);
    const [liveResult, setLiveResult] = useState(null);
    const [liveError, setLiveError] = useState('');
    const [liveValidation, setLiveValidation] = useState({});

    /* ── Application submission state ── */
    const [applicationLoading, setApplicationLoading] = useState(false);
    const [applicationResult, setApplicationResult] = useState(null);
    const [applicationError, setApplicationError] = useState('');

    /*
     * Document upload state — keyed by document type string.
     * Each entry: { file: File|null, status: 'idle'|'uploading'|'done'|'error', error: string }
     */
    const [docUploads, setDocUploads] = useState({});

    /* ════════════════════════════════════════════════════════════
       AUTO-LOAD BENEFICIARY + SCHEME
    ════════════════════════════════════════════════════════════ */
    useEffect(() => {

        /*
         * Get beneficiary ID saved during login.
         *
         * Example:
         * localStorage.setItem('beneficiaryId', beneficiary.id)
         */
        const storedBeneficiaryId =
            localStorage.getItem('beneficiaryId');

        if (!storedBeneficiaryId) {
            setLiveError(
                'Please log in as a beneficiary to check your eligibility.'
            );
            return;
        }

        /*
         * Get scheme from URL.
         *
         * Example:
         * /eligibility?scheme=PM-KISAN
         */
        const requestedScheme =
            searchParams.get('scheme');

        let selectedSchemeId = '';

        if (requestedScheme) {

            const scheme = KNOWN_SCHEMES.find(
                (s) =>
                    s.name.toLowerCase() ===
                    requestedScheme.toLowerCase()
            );

            if (scheme) {
                selectedSchemeId = String(scheme.backendId);
            }
        }

        setLiveForm((previous) => ({
            beneficiaryId:
                storedBeneficiaryId || previous.beneficiaryId,
            schemeId:
                selectedSchemeId || previous.schemeId,
        }));

        /*
         * When user comes from:
         *
         * Schemes → Apply Now
         *
         * they should directly see Live Eligibility.
         */
        if (requestedScheme) {
            setActiveTab('live');
        }

    }, [searchParams]);


    /* ════════════════════════════════════════════════════════════
       QUICK CHECK — static local filter
    ════════════════════════════════════════════════════════════ */
    const handleQuickChange = (e) =>
        setQuickForm({
            ...quickForm,
            [e.target.name]: e.target.value,
        });


    const handleQuickSubmit = (e) => {

        e.preventDefault();

        const eligible = [];

        if (quickForm.occupation === 'farmer')
            eligible.push(KNOWN_SCHEMES[0]);

        if (quickForm.occupation === 'student')
            eligible.push(KNOWN_SCHEMES[1]);

        if (
            quickForm.occupation === 'entrepreneur' ||
            quickForm.occupation === 'business-owner'
        ) {
            eligible.push(KNOWN_SCHEMES[2]);
        }

        setQuickResults(eligible);
        setQuickSubmitted(true);
    };


    /* ════════════════════════════════════════════════════════════
       LIVE SCORING — validation
    ════════════════════════════════════════════════════════════ */
    const validateLiveForm = () => {

        const errors = {};

        if (!liveForm.schemeId) {
            errors.schemeId =
                'Please select a scheme.';
        }

        return errors;
    };


    /* ════════════════════════════════════════════════════════════
       LIVE FORM CHANGE
    ════════════════════════════════════════════════════════════ */
    const handleLiveChange = (e) => {

        setLiveForm({
            ...liveForm,
            [e.target.name]: e.target.value,
        });

        if (liveValidation[e.target.name]) {
            setLiveValidation({
                ...liveValidation,
                [e.target.name]: '',
            });
        }

        if (liveError)
            setLiveError('');

        if (liveResult)
            setLiveResult(null);

        if (applicationResult)
            setApplicationResult(null);

        if (applicationError)
            setApplicationError('');
    };


    /* ════════════════════════════════════════════════════════════
       LIVE SCORING — backend API call
    ════════════════════════════════════════════════════════════ */
    const handleLiveSubmit = async (e) => {

        e.preventDefault();

        setLiveResult(null);
        setLiveError('');
        setApplicationResult(null);
        setApplicationError('');

        const errors = validateLiveForm();

        if (Object.keys(errors).length > 0) {

            setLiveValidation(errors);

            return;
        }

        setLiveLoading(true);

        try {

            const payload = {
                beneficiaryId:
                    parseInt(liveForm.beneficiaryId, 10),

                schemeId:
                    parseInt(liveForm.schemeId, 10),
            };

            const res = await fetch(
                `${API_BASE}/eligibility/check`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(payload),
                }
            );

            if (!res.ok) {

                const body =
                    await res.json().catch(() => ({}));

                const msg =
                    body.message ||
                    body.error ||
                    `Server error (${res.status})`;

                setLiveError(msg);

                return;
            }

            const data =
                await res.json();

            setLiveResult(data);

        } catch (err) {

            setLiveError(
                'Could not reach the backend. Make sure the Spring Boot server is running on port 8080.'
            );

        } finally {

            setLiveLoading(false);
        }
    };


    /* ════════════════════════════════════════════════════════════
       DOCUMENT UPLOAD — single file upload per document type
    ════════════════════════════════════════════════════════════ */
    const handleDocUpload = async (docType, file) => {

        const beneficiaryId = parseInt(liveForm.beneficiaryId, 10);
        if (!beneficiaryId || isNaN(beneficiaryId)) return;

        // Mark as uploading
        setDocUploads((prev) => ({
            ...prev,
            [docType]: { file, status: 'uploading', error: '' },
        }));

        const form = new FormData();
        form.append('file', file);
        form.append('documentType', docType);
        form.append('uploadedBy', String(beneficiaryId));

        try {
            const res = await fetch(
                `${API_BASE}/beneficiaries/${beneficiaryId}/documents`,
                { method: 'POST', body: form }
            );

            const body = await res.json().catch(() => ({}));

            if (!res.ok) {
                setDocUploads((prev) => ({
                    ...prev,
                    [docType]: {
                        file,
                        status: 'error',
                        error: body.message || `Upload failed (${res.status})`,
                    },
                }));
            } else {
                setDocUploads((prev) => ({
                    ...prev,
                    [docType]: { file, status: 'done', error: '' },
                }));
            }

        } catch {
            setDocUploads((prev) => ({
                ...prev,
                [docType]: {
                    file,
                    status: 'error',
                    error: 'Network error. Check that the server is running.',
                },
            }));
        }
    };

    /* ════════════════════════════════════════════════════════════
       APPLICATION SUBMISSION — backend API call
    ════════════════════════════════════════════════════════════ */
    const handleApplicationSubmit = async () => {

        setApplicationLoading(true);
        setApplicationError('');
        setApplicationResult(null);

        try {

            const payload = {
                beneficiaryId:
                    parseInt(liveForm.beneficiaryId, 10),

                schemeId:
                    parseInt(liveForm.schemeId, 10),
            };

            const res = await fetch(
                `${API_BASE}/applications`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(payload),
                }
            );

            const data =
                await res.json().catch(() => ({}));

            if (!res.ok) {

                const msg =
                    data.message ||
                    data.error ||
                    `Application submission failed (${res.status})`;

                setApplicationError(msg);

                return;
            }

            setApplicationResult(data);

        } catch (err) {

            setApplicationError(
                'Could not reach the backend. Make sure the Spring Boot server is running on port 8080.'
            );

        } finally {

            setApplicationLoading(false);
        }
    };


    /* ════════════════════════════════════════════════════════════
       RESET LIVE CHECK
    ════════════════════════════════════════════════════════════ */
    const resetLive = () => {

        /*
         * Keep the logged-in beneficiary ID and selected scheme
         * when resetting the result.
         */
        const storedBeneficiaryId =
            localStorage.getItem('beneficiaryId');

        const requestedScheme =
            searchParams.get('scheme');

        let selectedSchemeId = '';

        if (requestedScheme) {

            const scheme = KNOWN_SCHEMES.find(
                (s) =>
                    s.name.toLowerCase() ===
                    requestedScheme.toLowerCase()
            );

            if (scheme) {
                selectedSchemeId =
                    String(scheme.backendId);
            }
        }

        setLiveForm({
            beneficiaryId:
                storedBeneficiaryId || '',
            schemeId:
                selectedSchemeId || '',
        });

        setLiveResult(null);
        setLiveError('');
        setLiveValidation({});
        setApplicationResult(null);
        setApplicationError('');
        setDocUploads({});
    };


    /* ── Helpers ── */

    const scoreColor = (score) => {

        if (score >= 80)
            return 'var(--color-success)';

        if (score >= 60)
            return 'var(--color-warning)';

        return '#dc2626';
    };


    const scoreLabel = (score) => {

        if (score >= 80)
            return 'Strong';

        if (score >= 60)
            return 'Sufficient';

        return 'Insufficient';
    };


    /*
     * Find currently selected scheme.
     * Used to display a small contextual message when the
     * user entered through a scheme's Apply Now button.
     */
    const selectedScheme =
        KNOWN_SCHEMES.find(
            (s) =>
                String(s.backendId) ===
                String(liveForm.schemeId)
        );


    /* ═════════════════════════════════════════════════════════════
       RENDER
    ═════════════════════════════════════════════════════════════ */
    return (

        <div className="eligibility-page">

            {/* ── Hero ── */}

            <section className="eligibility-hero">

                <div className="eligibility-hero-content">

                    <h1>
                        Check Your Eligibility
                    </h1>

                    <p>
                        Discover government schemes you qualify for —
                        instantly, using our weighted scoring engine.
                    </p>

                </div>

            </section>


            {/* ── Tab bar ── */}

            <div className="elig-tab-bar">

                <div className="elig-tab-bar-inner">

                    <button
                        id="tab-quick"
                        className={`elig-tab-btn${
                            activeTab === 'quick'
                                ? ' active'
                                : ''
                        }`}
                        onClick={() => setActiveTab('quick')}
                        type="button"
                        aria-selected={activeTab === 'quick'}
                    >
                        Quick Scheme Finder
                    </button>


                    <button
                        id="tab-live"
                        className={`elig-tab-btn${
                            activeTab === 'live'
                                ? ' active'
                                : ''
                        }`}
                        onClick={() => setActiveTab('live')}
                        type="button"
                        aria-selected={activeTab === 'live'}
                    >
                        Live Eligibility Score

                        <span className="elig-tab-badge">
                            Live
                        </span>

                    </button>

                </div>

            </div>


            <main className="eligibility-container">


                {/* ══════════════════════════════════════════════
                    TAB 1 — QUICK CHECK
                ══════════════════════════════════════════════ */}

                {activeTab === 'quick' && (

                    <>

                        <div className="eligibility-card">

                            <div className="form-heading">

                                <h2>
                                    Your Profile
                                </h2>

                                <p>
                                    Provide your details to find relevant
                                    government schemes.
                                </p>

                            </div>


                            <form onSubmit={handleQuickSubmit}>

                                <div className="eligibility-form-group">

                                    <label htmlFor="state">
                                        State / Union Territory
                                    </label>

                                    <select
                                        id="state"
                                        name="state"
                                        value={quickForm.state}
                                        onChange={handleQuickChange}
                                        required
                                    >

                                        <option value="">
                                            Select your state
                                        </option>

                                        <option value="andhra-pradesh">
                                            Andhra Pradesh
                                        </option>

                                        <option value="karnataka">
                                            Karnataka
                                        </option>

                                        <option value="maharashtra">
                                            Maharashtra
                                        </option>

                                        <option value="tamil-nadu">
                                            Tamil Nadu
                                        </option>

                                        <option value="delhi">
                                            Delhi
                                        </option>

                                        <option value="other">
                                            Other
                                        </option>

                                    </select>

                                </div>


                                <div className="eligibility-form-group">

                                    <label htmlFor="age">
                                        Age Group
                                    </label>

                                    <select
                                        id="age"
                                        name="age"
                                        value={quickForm.age}
                                        onChange={handleQuickChange}
                                        required
                                    >

                                        <option value="">
                                            Select your age group
                                        </option>

                                        <option value="below-18">
                                            Below 18
                                        </option>

                                        <option value="18-25">
                                            18 – 25
                                        </option>

                                        <option value="26-40">
                                            26 – 40
                                        </option>

                                        <option value="41-60">
                                            41 – 60
                                        </option>

                                        <option value="above-60">
                                            Above 60
                                        </option>

                                    </select>

                                </div>


                                <div className="eligibility-form-group">

                                    <label htmlFor="occupation">
                                        Occupation / Status
                                    </label>

                                    <select
                                        id="occupation"
                                        name="occupation"
                                        value={quickForm.occupation}
                                        onChange={handleQuickChange}
                                        required
                                    >

                                        <option value="">
                                            Select your occupation
                                        </option>

                                        <option value="farmer">
                                            Farmer
                                        </option>

                                        <option value="student">
                                            Student
                                        </option>

                                        <option value="entrepreneur">
                                            Entrepreneur
                                        </option>

                                        <option value="business-owner">
                                            Small Business Owner
                                        </option>

                                        <option value="other">
                                            Other
                                        </option>

                                    </select>

                                </div>


                                <div className="eligibility-form-group">

                                    <label htmlFor="category">
                                        Social Category
                                    </label>

                                    <select
                                        id="category"
                                        name="category"
                                        value={quickForm.category}
                                        onChange={handleQuickChange}
                                        required
                                    >

                                        <option value="">
                                            Select your category
                                        </option>

                                        <option value="general">
                                            General
                                        </option>

                                        <option value="obc">
                                            OBC
                                        </option>

                                        <option value="sc">
                                            SC
                                        </option>

                                        <option value="st">
                                            ST
                                        </option>

                                        <option value="ews">
                                            EWS
                                        </option>

                                    </select>

                                </div>


                                <div className="eligibility-form-group">

                                    <label htmlFor="income">
                                        Annual Family Income
                                    </label>

                                    <select
                                        id="income"
                                        name="income"
                                        value={quickForm.income}
                                        onChange={handleQuickChange}
                                        required
                                    >

                                        <option value="">
                                            Select annual income
                                        </option>

                                        <option value="below-1">
                                            Below ₹1 Lakh
                                        </option>

                                        <option value="1-3">
                                            ₹1 – ₹3 Lakhs
                                        </option>

                                        <option value="3-5">
                                            ₹3 – ₹5 Lakhs
                                        </option>

                                        <option value="5-10">
                                            ₹5 – ₹10 Lakhs
                                        </option>

                                        <option value="above-10">
                                            Above ₹10 Lakhs
                                        </option>

                                    </select>

                                </div>


                                <button
                                    type="submit"
                                    className="eligibility-submit"
                                >
                                    Find Eligible Schemes
                                </button>

                            </form>

                        </div>


                        {quickSubmitted && (

                            <section className="eligibility-results">

                                <div className="results-heading">

                                    <h2>
                                        Eligible Schemes
                                    </h2>

                                    {quickResults.length > 0 ? (

                                        <p>
                                            Based on your profile, you may be
                                            eligible for the following schemes.
                                        </p>

                                    ) : (

                                        <p>
                                            No matching schemes found. Try
                                            adjusting your occupation or category.
                                        </p>

                                    )}

                                </div>


                                <div className="eligibility-results-grid">

                                    {quickResults.map((scheme) => (

                                        <div
                                            className="eligibility-scheme-card"
                                            key={scheme.id}
                                        >

                                            <div className="result-card-header">

                                                <span className="result-category">
                                                    {scheme.category}
                                                </span>

                                                <span className="result-target">
                                                    {scheme.target}
                                                </span>

                                            </div>


                                            <h3>
                                                {scheme.name}
                                            </h3>


                                            <p>
                                                {scheme.description}
                                            </p>


                                            <Link
                                                to={`${scheme.path}?apply=true`}
                                                className="result-button"
                                            >
                                                Apply Now
                                            </Link>

                                        </div>

                                    ))}

                                </div>

                            </section>

                        )}

                    </>

                )}


                {/* ══════════════════════════════════════════════
                    TAB 2 — LIVE SCORING
                ══════════════════════════════════════════════ */}

                {activeTab === 'live' && (

                    <>

                        <div className="eligibility-card live-card">

                            <div className="form-heading">

                                <h2>
                                    Live Eligibility Score
                                </h2>

                                <p>
                                    Enter your registered Beneficiary ID and
                                    select a scheme. Our weighted scoring engine
                                    will evaluate your profile against the
                                    scheme's criteria in real time.
                                </p>

                            </div>


                            {/* Selected scheme message */}

                            {selectedScheme && searchParams.get('scheme') && (

                                <div className="live-info-box">

                                    <span className="live-info-icon">
                                        ✓
                                    </span>

                                    <span>
                                        You are applying for{' '}
                                        <strong>
                                            {selectedScheme.name}
                                        </strong>
                                        . The scheme has been selected
                                        automatically.
                                    </span>

                                </div>

                            )}


                            {/* Info box */}

                            {!selectedScheme && (

                                <div className="live-info-box">

                                    <span className="live-info-icon">
                                        ℹ
                                    </span>

                                    <span>
                                        You must be registered as a beneficiary
                                        to use live scoring. Score ≥&nbsp;60 →
                                        <strong> ELIGIBLE</strong>.
                                        Score &lt;&nbsp;60 →
                                        <strong> INELIGIBLE</strong>.
                                    </span>

                                </div>

                            )}


                            <form
                                id="live-eligibility-form"
                                onSubmit={handleLiveSubmit}
                                noValidate
                            >

                                {/* Beneficiary ID */}

                                <div className="eligibility-form-group">

                                    <label htmlFor="beneficiaryId">

                                        Beneficiary ID

                                        <span
                                            className="field-required"
                                            aria-hidden="true"
                                        >
                                            {' '}*
                                        </span>

                                    </label>


                                    <input
                                        id="beneficiaryId"
                                        name="beneficiaryId"
                                        type="number"
                                        value={liveForm.beneficiaryId}
                                        readOnly
                                        className="elig-input"
                                        aria-describedby="beneficiaryId-hint"
                                    />


                                    {liveValidation.beneficiaryId && (

                                        <span
                                            id="err-beneficiaryId"
                                            className="field-error"
                                            role="alert"
                                        >
                                            {liveValidation.beneficiaryId}
                                        </span>

                                    )}


                                    <span
                                        id="beneficiaryId-hint"
                                        className="field-hint"
                                    >
                                        Your Beneficiary ID is automatically taken from your login.
                                    </span>

                                </div>


                                {/* Scheme selector */}

                                <div className="eligibility-form-group">

                                    <label htmlFor="schemeId">

                                        Scheme

                                        <span
                                            className="field-required"
                                            aria-hidden="true"
                                        >
                                            {' '}*
                                        </span>

                                    </label>


                                    <select
                                        id="schemeId"
                                        name="schemeId"
                                        value={liveForm.schemeId}
                                        onChange={handleLiveChange}
                                        className={
                                            liveValidation.schemeId
                                                ? 'input-error'
                                                : ''
                                        }
                                        aria-describedby={
                                            liveValidation.schemeId
                                                ? 'err-schemeId'
                                                : undefined
                                        }
                                    >

                                        <option value="">
                                            Select a scheme to evaluate against
                                        </option>


                                        {KNOWN_SCHEMES.map((s) => (

                                            <option
                                                key={s.backendId}
                                                value={s.backendId}
                                            >
                                                {s.name} — {s.category}
                                            </option>

                                        ))}

                                    </select>


                                    {liveValidation.schemeId && (

                                        <span
                                            id="err-schemeId"
                                            className="field-error"
                                            role="alert"
                                        >
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

                                            <span
                                                className="spinner"
                                                aria-hidden="true"
                                            />

                                            Evaluating…

                                        </span>

                                    ) : (
                                        'Check Eligibility'
                                    )}

                                </button>

                            </form>

                        </div>


                        {/* ── API Error ── */}

                        {liveError && (

                            <div
                                className="live-alert live-alert-error"
                                role="alert"
                            >

                                <span className="live-alert-icon">
                                    ✕
                                </span>

                                <div>

                                    <strong>
                                        Eligibility check failed
                                    </strong>

                                    <p>
                                        {liveError}
                                    </p>

                                </div>

                            </div>

                        )}


                        {/* ── Result panel ── */}

                        {liveResult && (

                            <section
                                className="live-result-panel"
                                aria-live="polite"
                            >

                                {/* Verdict banner */}

                                <div
                                    className={`verdict-banner ${
                                        liveResult.eligible
                                            ? 'verdict-eligible'
                                            : 'verdict-ineligible'
                                    }`}
                                >

                                    <div
                                        className="verdict-icon"
                                        aria-hidden="true"
                                    >
                                        {liveResult.eligible ? '✓' : '✕'}
                                    </div>


                                    <div className="verdict-text">

                                        <h2>
                                            {liveResult.eligible
                                                ? 'ELIGIBLE'
                                                : 'INELIGIBLE'}
                                        </h2>

                                        <p>

                                            {liveResult.eligible
                                                ? `Your profile qualifies for ${liveResult.schemeName}.`
                                                : `Your profile does not meet the minimum score for ${liveResult.schemeName}.`
                                            }

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

                                        <h3 className="score-dial-title">
                                            Eligibility Score
                                        </h3>


                                        <div
                                            className="score-dial"
                                            style={{
                                                '--score-color':
                                                    scoreColor(
                                                        liveResult.totalScore
                                                    )
                                            }}
                                        >

                                            <svg
                                                viewBox="0 0 120 120"
                                                className="score-svg"
                                                aria-hidden="true"
                                            >

                                                <circle
                                                    cx="60"
                                                    cy="60"
                                                    r="50"
                                                    fill="none"
                                                    stroke="var(--color-gray-200)"
                                                    strokeWidth="10"
                                                />


                                                <circle
                                                    cx="60"
                                                    cy="60"
                                                    r="50"
                                                    fill="none"
                                                    stroke={
                                                        scoreColor(
                                                            liveResult.totalScore
                                                        )
                                                    }
                                                    strokeWidth="10"
                                                    strokeLinecap="round"
                                                    strokeDasharray={`${
                                                        (liveResult.totalScore / 100) * 314
                                                    } 314`}
                                                    transform="rotate(-90 60 60)"
                                                />

                                            </svg>


                                            <div className="score-value">

                                                <span
                                                    className="score-number"
                                                    style={{
                                                        color:
                                                            scoreColor(
                                                                liveResult.totalScore
                                                            )
                                                    }}
                                                >
                                                    {liveResult.totalScore}
                                                </span>

                                                <span className="score-max">
                                                    /100
                                                </span>

                                            </div>

                                        </div>


                                        <div
                                            className="score-label"
                                            style={{
                                                color:
                                                    scoreColor(
                                                        liveResult.totalScore
                                                    )
                                            }}
                                        >
                                            {scoreLabel(
                                                liveResult.totalScore
                                            )}
                                        </div>


                                        <div className="score-threshold-note">

                                            Minimum score to qualify:{' '}
                                            <strong>
                                                60
                                            </strong>

                                        </div>


                                        <div className="score-meta">

                                            <div className="score-meta-row">

                                                <span>
                                                    Scheme
                                                </span>

                                                <strong>
                                                    {liveResult.schemeName}
                                                </strong>

                                            </div>


                                            <div className="score-meta-row">

                                                <span>
                                                    Beneficiary ID
                                                </span>

                                                <strong>
                                                    #{liveResult.beneficiaryId}
                                                </strong>

                                            </div>


                                            <div className="score-meta-row">

                                                <span>
                                                    Evaluated at
                                                </span>

                                                <strong>

                                                    {new Date(
                                                        liveResult.evaluatedAt
                                                    ).toLocaleString('en-IN')}

                                                </strong>

                                            </div>

                                        </div>

                                    </div>


                                    {/* Criteria breakdown */}

                                    <div className="criteria-card">

                                        <h3 className="criteria-title">
                                            Criteria Breakdown
                                        </h3>

                                        <p className="criteria-subtitle">
                                            Detailed evaluation against each
                                            eligibility criterion
                                        </p>


                                        <div className="criteria-list">

                                            {liveResult.criteria &&
                                                (() => {
                                                    // Resolve scheme key once for this result
                                                    const schemeKey = resolveSchemeKey(liveResult.schemeName);
                                                    return Object.entries(
                                                        liveResult.criteria
                                                    ).map(
                                                        ([key, crit]) => (

                                                            <div
                                                                key={key}
                                                                className={`criterion-row ${
                                                                    crit.passed
                                                                        ? 'crit-pass'
                                                                        : 'crit-fail'
                                                                }`}
                                                            >

                                                                <div className="crit-header">

                                                                <span
                                                                    className={`crit-icon ${
                                                                        crit.passed
                                                                            ? 'crit-icon-pass'
                                                                            : 'crit-icon-fail'
                                                                    }`}
                                                                    aria-hidden="true"
                                                                >
                                                                    {crit.passed
                                                                        ? '✓'
                                                                        : '✕'}
                                                                </span>


                                                                    <span className="crit-name">

                                                                    {
                                                                        CRITERION_LABELS[
                                                                            key
                                                                            ] ?? key
                                                                    }

                                                                </span>


                                                                    <span className="crit-points">

                                                                    {crit.points}/
                                                                        {
                                                                            getCriterionMax(schemeKey, key) ?? '?'
                                                                        }{' '}
                                                                        pts

                                                                </span>

                                                                </div>


                                                                {/* Progress bar */}

                                                                <div
                                                                    className="crit-bar-track"
                                                                    role="progressbar"
                                                                    aria-valuenow={
                                                                        crit.points
                                                                    }
                                                                    aria-valuemax={
                                                                        getCriterionMax(schemeKey, key)
                                                                    }
                                                                    aria-label={`${
                                                                        CRITERION_LABELS[
                                                                            key
                                                                            ] ?? key
                                                                    } score`}
                                                                >

                                                                    <div
                                                                        className={`crit-bar-fill ${
                                                                            crit.passed
                                                                                ? 'bar-pass'
                                                                                : 'bar-fail'
                                                                        }`}
                                                                        style={{
                                                                            width: `${
                                                                                (crit.points /
                                                                                    (getCriterionMax(schemeKey, key) ?? 100)) *
                                                                                100
                                                                            }%`,
                                                                        }}
                                                                    />

                                                                </div>


                                                                <p className="crit-detail">
                                                                    {crit.detail}
                                                                </p>

                                                            </div>
                                                        )
                                                    );
                                                })()}

                                        </div>

                                    </div>

                                </div>


                                {/* Next steps — eligible */}

                                {liveResult.eligible && (

                                    <div className="next-steps-card">

                                        <h3>
                                            🎉 Next Steps
                                        </h3>

                                        <p>

                                            Your eligibility score qualifies
                                            you for{' '}

                                            <strong>
                                                {liveResult.schemeName}
                                            </strong>
                                            . You can now continue with the
                                            application process.

                                        </p>


                                        {/* Required document upload section */}
                                        {(() => {
                                            const schemeKey = resolveSchemeKey(liveResult.schemeName);
                                            const requiredDocs = getRequiredDocs(schemeKey);
                                            if (requiredDocs.length === 0) return null;

                                            const allDone = requiredDocs.every(
                                                d => docUploads[d.type]?.status === 'done'
                                            );

                                            return (
                                                <div className="doc-upload-section">

                                                    <h4 className="doc-upload-title">
                                                        📎 Required Documents
                                                    </h4>

                                                    <p className="doc-upload-subtitle">
                                                        Upload all required documents for{' '}
                                                        <strong>{liveResult.schemeName}</strong>{' '}
                                                        before submitting your application.
                                                    </p>

                                                    <div className="doc-upload-list">
                                                        {requiredDocs.map(({ type, label }) => {
                                                            const entry = docUploads[type];
                                                            const status = entry?.status ?? 'idle';

                                                            return (
                                                                <div
                                                                    key={type}
                                                                    className={`doc-upload-row doc-upload-row--${status}`}
                                                                >
                                                                    <span className="doc-upload-label">
                                                                        {label}
                                                                        <span className="field-required" aria-hidden="true"> *</span>
                                                                    </span>

                                                                    <label
                                                                        className={`doc-file-btn${status === 'done' ? ' doc-file-btn--done' : ''}`}
                                                                        htmlFor={`doc-input-${type}`}
                                                                    >
                                                                        {status === 'uploading' && (
                                                                            <span className="spinner" aria-hidden="true" />
                                                                        )}
                                                                        {status === 'done'    ? '✓ Uploaded'     : null}
                                                                        {status === 'error'   ? '↺ Retry'        : null}
                                                                        {(status === 'idle' || status === 'uploading') && status !== 'uploading'
                                                                            ? 'Choose File'
                                                                            : null}
                                                                        {status === 'uploading' ? ' Uploading…' : null}

                                                                        <input
                                                                            id={`doc-input-${type}`}
                                                                            type="file"
                                                                            accept="image/*,application/pdf"
                                                                            style={{ display: 'none' }}
                                                                            onChange={e => {
                                                                                const file = e.target.files?.[0];
                                                                                if (file) handleDocUpload(type, file);
                                                                                e.target.value = '';
                                                                            }}
                                                                            disabled={status === 'uploading'}
                                                                        />
                                                                    </label>

                                                                    {entry?.file && status !== 'uploading' && (
                                                                        <span className="doc-filename-hint">
                                                                            {entry.file.name}
                                                                        </span>
                                                                    )}

                                                                    {status === 'error' && (
                                                                        <span className="doc-upload-error" role="alert">
                                                                            {entry.error}
                                                                        </span>
                                                                    )}

                                                                </div>
                                                            );
                                                        })}
                                                    </div>

                                                    {!allDone && (
                                                        <p className="doc-upload-note" role="status">
                                                            Upload all required documents to enable application submission.
                                                        </p>
                                                    )}

                                                    {allDone && (
                                                        <p className="doc-upload-note doc-upload-note--done" role="status">
                                                            ✓ All required documents uploaded. You may now submit your application.
                                                        </p>
                                                    )}

                                                </div>
                                            );
                                        })()}


                                        <div className="next-steps-actions">

                                            <Link
                                                to={`/schemes/${
                                                    liveResult.schemeName ===
                                                    'PM-KISAN'
                                                        ? 'pm-kisan'
                                                        : liveResult.schemeName ===
                                                        'NSP'
                                                            ? 'nsp'
                                                            : 'pmegp'
                                                }`}
                                                className="result-button"
                                            >
                                                View Scheme Details
                                            </Link>


                                            {/* Submit Application — gated on all docs uploaded */}
                                            {(() => {
                                                const schemeKey = resolveSchemeKey(liveResult.schemeName);
                                                const requiredDocs = getRequiredDocs(schemeKey)
                                                const allDone = requiredDocs.length === 0 ||
                                                    requiredDocs.every(d => docUploads[d.type]?.status === 'done');
                                                return (
                                                    <button
                                                        type="button"
                                                        className="result-button"
                                                        onClick={handleApplicationSubmit}
                                                        disabled={
                                                            applicationLoading ||
                                                            applicationResult !== null ||
                                                            !allDone
                                                        }
                                                        title={!allDone ? 'Upload all required documents first' : undefined}
                                                    >
                                                        {applicationLoading
                                                            ? 'Submitting…'
                                                            : applicationResult
                                                                ? 'Application Submitted ✓'
                                                                : 'Submit Application'
                                                        }
                                                    </button>
                                                );
                                            })()}


                                            <Link
                                                to="/track"
                                                className="result-button result-button-secondary"
                                            >
                                                Track Application
                                            </Link>

                                        </div>


                                        {/* Application submission error */}

                                        {applicationError && (

                                            <div
                                                className="live-alert live-alert-error"
                                                role="alert"
                                                style={{
                                                    marginTop: '1rem',
                                                }}
                                            >

                                                <span className="live-alert-icon">
                                                    ✕
                                                </span>

                                                <div>

                                                    <strong>
                                                        Application submission failed
                                                    </strong>

                                                    <p>
                                                        {applicationError}
                                                    </p>

                                                </div>

                                            </div>

                                        )}


                                        {/* Application submission success */}

                                        {applicationResult && (

                                            <div
                                                className="live-info-box"
                                                style={{
                                                    marginTop: '1rem',
                                                }}
                                            >

                                                <span className="live-info-icon">
                                                    ✓
                                                </span>

                                                <span>

                                                    Application submitted
                                                    successfully.
                                                    {' '}

                                                    <strong>
                                                        Application ID: #
                                                        {
                                                            applicationResult.applicationId
                                                        }
                                                    </strong>

                                                    {' '}— Status:{' '}

                                                    <strong>
                                                        {
                                                            applicationResult.applicationStatus
                                                        }
                                                    </strong>

                                                </span>

                                            </div>

                                        )}

                                    </div>

                                )}


                                {/* Next steps — ineligible */}

                                {!liveResult.eligible && (

                                    <div
                                        className="next-steps-card next-steps-ineligible"
                                    >

                                        <h3>
                                            What can you do?
                                        </h3>


                                        <p>

                                            Your current profile does not meet
                                            the minimum score of 60 for{' '}

                                            <strong>
                                                {liveResult.schemeName}
                                            </strong>
                                            . You may:

                                        </p>


                                        <ul>

                                            <li>
                                                Update your beneficiary profile
                                                with complete eligibility data.
                                            </li>

                                            <li>
                                                Complete identity verification
                                                with a Field Officer (+10 pts).
                                            </li>

                                            <li>
                                                Check eligibility for a different
                                                scheme.
                                            </li>

                                        </ul>


                                        <button
                                            className="eligibility-submit"
                                            style={{
                                                marginTop: '1rem',
                                                maxWidth: '16rem',
                                            }}
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
