import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Register.css';

/* ── API base (Vite proxy forwards /api → localhost:8080) ─── */
const API_BASE = '/api/v1';

/* ══════════════════════════════════════════════════════════════
   buildPayload — maps formData → BeneficiaryRegistrationRequest
   Mandatory fields are always included.
   Optional fields are only included when non-empty.
══════════════════════════════════════════════════════════════ */
function buildPayload(formData) {
    const payload = {
        fullName:   formData.fullName.trim(),
        govId:      formData.govId.trim(),
        contact:    formData.mobile.trim(),   // frontend "mobile" → backend "contact"
        email:      formData.email.trim(),
        age:        parseInt(formData.age, 10),
        address:    formData.address.trim(),
        schemeName: formData.schemeName.trim(),
    };

    const optionalString = (key, value) => {
        const trimmed = (value || '').trim();
        if (trimmed) payload[key] = trimmed;
    };

    optionalString('aadhaarNumber', formData.aadhaarNumber);
    optionalString('mobileNumber',  formData.mobileNumber);
    optionalString('village',       formData.village);
    optionalString('taluka',        formData.taluka);
    optionalString('district',      formData.district);
    optionalString('state',         formData.state);
    optionalString('pinCode',       formData.pinCode);

    if (formData.dateOfBirth) payload.dateOfBirth = formData.dateOfBirth;
    if (formData.gender)      payload.gender      = formData.gender;
    if (formData.category)    payload.category    = formData.category;

    const income = (formData.annualIncome || '').trim();
    if (income) payload.annualIncome = parseFloat(income);

    const land = (formData.landHolding || '').trim();
    if (land) payload.landHolding = parseFloat(land);

    return payload;
}

/* ══════════════════════════════════════════════════════════════
   Client-side validation — returns an array of error strings.
   Empty array = valid.
══════════════════════════════════════════════════════════════ */
function validate(formData) {
    const errors = [];

    if (!formData.fullName.trim())
        errors.push('Full name is required.');
    if (!formData.govId.trim())
        errors.push('Government ID is required.');
    if (!/^\d{10}$/.test(formData.mobile.trim()))
        errors.push('Contact number must be exactly 10 digits.');
    if (!formData.email.trim() || !/\S+@\S+\.\S+/.test(formData.email))
        errors.push('A valid email address is required.');
    const age = parseInt(formData.age, 10);
    if (!formData.age || isNaN(age) || age < 1 || age > 120)
        errors.push('Age must be a number between 1 and 120.');
    if (!formData.address.trim())
        errors.push('Address is required.');
    if (!formData.schemeName.trim())
        errors.push('Scheme name is required.');
    if (!formData.terms)
        errors.push('You must agree to the Terms of Service.');

    // Optional Aadhaar — if provided must be 12 digits
    const aadhaar = (formData.aadhaarNumber || '').trim();
    if (aadhaar && !/^\d{12}$/.test(aadhaar))
        errors.push('Aadhaar number must be exactly 12 digits.');

    // Optional mobile number — if provided must start 6-9 and be 10 digits
    const mob = (formData.mobileNumber || '').trim();
    if (mob && !/^[6-9]\d{9}$/.test(mob))
        errors.push('Mobile number must be a valid 10-digit Indian number starting with 6–9.');

    // Optional PIN code — if provided must be 6 digits
    const pin = (formData.pinCode || '').trim();
    if (pin && !/^\d{6}$/.test(pin))
        errors.push('PIN code must be exactly 6 digits.');

    return errors;
}

/* ══════════════════════════════════════════════════════════════
   MAIN COMPONENT
══════════════════════════════════════════════════════════════ */
function Register() {

    const [formData, setFormData] = useState({
        // Mandatory fields
        fullName:     '',
        govId:        '',
        mobile:       '',   // maps to backend "contact"
        email:        '',
        age:          '',
        address:      '',
        schemeName:   '',
        terms:        false,

        // Optional eligibility fields
        aadhaarNumber: '',
        mobileNumber:  '',
        dateOfBirth:   '',
        gender:        '',
        village:       '',
        taluka:        '',
        district:      '',
        state:         '',
        pinCode:       '',
        annualIncome:  '',
        landHolding:   '',
        category:      '',
    });

    const [loading,       setLoading]       = useState(false);
    const [clientErrors,  setClientErrors]  = useState([]);   // validation errors
    const [serverError,   setServerError]   = useState('');   // server error message
    const [successData,   setSuccessData]   = useState(null); // { id, fullName }

    /* ── Field change handler ── */
    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value,
        }));
        // Clear errors on edit
        if (clientErrors.length) setClientErrors([]);
        if (serverError)         setServerError('');
    };

    /* ── Submit handler ── */
    const handleSubmit = async (e) => {
        e.preventDefault();
        setSuccessData(null);
        setServerError('');

        const errors = validate(formData);
        if (errors.length > 0) {
            setClientErrors(errors);
            return;
        }
        setClientErrors([]);

        setLoading(true);
        try {
            const payload = buildPayload(formData);

            const res = await fetch(`${API_BASE}/beneficiaries`, {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify(payload),
            });

            const body = await res.json().catch(() => ({}));

            if (!res.ok) {
                // 409 Conflict = duplicate, 400 = validation, 500 = server error
                const msg = body.message || body.error
                    || `Registration failed (HTTP ${res.status}).`;
                setServerError(msg);
                return;
            }

            // Success — backend returns BeneficiaryResponse with id
            setSuccessData({ id: body.id, fullName: body.fullName || formData.fullName.trim() });

        } catch {
            setServerError(
                'Could not reach the server. Make sure the Spring Boot backend is running on port 8080.'
            );
        } finally {
            setLoading(false);
        }
    };

    /* ════════════════════════════════════════════════════════════
       SUCCESS VIEW — shown after successful registration
    ════════════════════════════════════════════════════════════ */
    if (successData) {
        return (
            <div className="register-page">

                <header className="register-header">
                    <div className="register-header-container">
                        <Link to="/" className="register-brand">
                            <h1>DSGP</h1>
                            <p>Digital Subsidy &amp; Grant Platform</p>
                        </Link>
                        <Link to="/" className="register-home-link">Back to Home</Link>
                    </div>
                </header>

                <main className="register-main">
                    <div className="register-card">
                        <div className="register-success">

                            <div className="register-success-icon">✅</div>

                            <h3>Registration Successful!</h3>

                            <p>
                                Welcome, <strong>{successData.fullName}</strong>.
                                Your beneficiary profile has been created.
                            </p>

                            <p>Your Beneficiary ID is:</p>

                            <div className="register-success-id">
                                #{successData.id}
                            </div>

                            <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)' }}>
                                Keep this ID safe — you will need it to check eligibility
                                and track your applications.
                            </p>

                            <div className="register-success-actions">
                                <Link
                                    to="/eligibility"
                                    className="register-success-primary"
                                >
                                    Check Scheme Eligibility →
                                </Link>
                                <Link
                                    to="/"
                                    className="register-success-secondary"
                                >
                                    Return to Home
                                </Link>
                            </div>

                        </div>
                    </div>
                </main>

                <footer className="register-footer">
                    <p>&copy; 2024 Digital Subsidy &amp; Grant Platform (DSGP)</p>
                </footer>

            </div>
        );
    }

    /* ════════════════════════════════════════════════════════════
       REGISTRATION FORM VIEW
    ════════════════════════════════════════════════════════════ */
    return (
        <div className="register-page">

            {/* Header */}
            <header className="register-header">
                <div className="register-header-container">
                    <Link to="/" className="register-brand">
                        <h1>DSGP</h1>
                        <p>Digital Subsidy &amp; Grant Platform</p>
                    </Link>
                    <Link to="/" className="register-home-link">Back to Home</Link>
                </div>
            </header>


            {/* Main */}
            <main className="register-main">
                <div className="register-card">

                    <div className="register-card-header">
                        <h2>Create Account</h2>
                        <p>Register as a beneficiary to access government schemes.</p>
                    </div>


                    {/* ── Client-side validation errors ── */}
                    {clientErrors.length > 0 && (
                        <div className="register-alert register-alert-error" role="alert">
                            <span className="register-alert-icon">⚠️</span>
                            <div className="register-alert-body">
                                <strong>Please fix the following:</strong>
                                <ul>
                                    {clientErrors.map((err, i) => (
                                        <li key={i}>{err}</li>
                                    ))}
                                </ul>
                            </div>
                        </div>
                    )}

                    {/* ── Server error ── */}
                    {serverError && (
                        <div className="register-alert register-alert-error" role="alert">
                            <span className="register-alert-icon">❌</span>
                            <div className="register-alert-body">
                                <strong>Registration failed</strong>
                                <span>{serverError}</span>
                            </div>
                        </div>
                    )}


                    <form className="register-form" onSubmit={handleSubmit} noValidate>

                        {/* ── SECTION: Basic Information ── */}
                        <p className="register-section-label">Basic Information</p>

                        <div className="register-form-group">
                            <label htmlFor="fullName">Full Name <span aria-hidden="true">*</span></label>
                            <input
                                id="fullName"
                                name="fullName"
                                type="text"
                                value={formData.fullName}
                                onChange={handleChange}
                                placeholder="e.g. Ravi Kumar"
                                required
                            />
                        </div>

                        <div className="register-form-row">
                            <div className="register-form-group">
                                <label htmlFor="govId">Government ID <span aria-hidden="true">*</span></label>
                                <input
                                    id="govId"
                                    name="govId"
                                    type="text"
                                    value={formData.govId}
                                    onChange={handleChange}
                                    placeholder="Aadhaar / Voter ID"
                                    required
                                />
                            </div>
                            <div className="register-form-group">
                                <label htmlFor="age">Age <span aria-hidden="true">*</span></label>
                                <input
                                    id="age"
                                    name="age"
                                    type="number"
                                    min="1"
                                    max="120"
                                    value={formData.age}
                                    onChange={handleChange}
                                    placeholder="e.g. 35"
                                    required
                                />
                            </div>
                        </div>

                        <div className="register-form-row">
                            <div className="register-form-group">
                                <label htmlFor="email">Email Address <span aria-hidden="true">*</span></label>
                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    placeholder="you@example.com"
                                    required
                                />
                            </div>
                            <div className="register-form-group">
                                <label htmlFor="mobile">Contact Number <span aria-hidden="true">*</span></label>
                                <input
                                    id="mobile"
                                    name="mobile"
                                    type="tel"
                                    value={formData.mobile}
                                    onChange={handleChange}
                                    placeholder="10-digit number"
                                    required
                                />
                            </div>
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="address">Address <span aria-hidden="true">*</span></label>
                            <input
                                id="address"
                                name="address"
                                type="text"
                                value={formData.address}
                                onChange={handleChange}
                                placeholder="Full residential address"
                                required
                            />
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="schemeName">Scheme of Interest <span aria-hidden="true">*</span></label>
                            <select
                                id="schemeName"
                                name="schemeName"
                                value={formData.schemeName}
                                onChange={handleChange}
                                required
                            >
                                <option value="">Select a scheme</option>
                                <option value="PM-KISAN Samman Nidhi">PM-KISAN Samman Nidhi</option>
                                <option value="National Scholarship Portal">National Scholarship Portal (NSP)</option>
                                <option value="Prime Minister's Employment Generation Programme">PMEGP</option>
                            </select>
                        </div>


                        {/* ── SECTION: Identity & Demographics (Optional) ── */}
                        <p className="register-section-label">Identity &amp; Demographics (Optional)</p>

                        <div className="register-form-row">
                            <div className="register-form-group">
                                <label htmlFor="aadhaarNumber">Aadhaar Number</label>
                                <input
                                    id="aadhaarNumber"
                                    name="aadhaarNumber"
                                    type="text"
                                    value={formData.aadhaarNumber}
                                    onChange={handleChange}
                                    placeholder="12-digit Aadhaar"
                                    maxLength="12"
                                />
                            </div>
                            <div className="register-form-group">
                                <label htmlFor="mobileNumber">Mobile Number</label>
                                <input
                                    id="mobileNumber"
                                    name="mobileNumber"
                                    type="tel"
                                    value={formData.mobileNumber}
                                    onChange={handleChange}
                                    placeholder="10-digit (starts 6-9)"
                                    maxLength="10"
                                />
                            </div>
                        </div>

                        <div className="register-form-row">
                            <div className="register-form-group">
                                <label htmlFor="dateOfBirth">Date of Birth</label>
                                <input
                                    id="dateOfBirth"
                                    name="dateOfBirth"
                                    type="date"
                                    value={formData.dateOfBirth}
                                    onChange={handleChange}
                                />
                            </div>
                            <div className="register-form-group">
                                <label htmlFor="gender">Gender</label>
                                <select
                                    id="gender"
                                    name="gender"
                                    value={formData.gender}
                                    onChange={handleChange}
                                >
                                    <option value="">Select gender</option>
                                    <option value="MALE">Male</option>
                                    <option value="FEMALE">Female</option>
                                    <option value="OTHER">Other</option>
                                </select>
                            </div>
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="category">Social Category</label>
                            <select
                                id="category"
                                name="category"
                                value={formData.category}
                                onChange={handleChange}
                            >
                                <option value="">Select category</option>
                                <option value="GENERAL">General</option>
                                <option value="OBC">OBC</option>
                                <option value="SC">SC</option>
                                <option value="ST">ST</option>
                            </select>
                        </div>


                        {/* ── SECTION: Address Details (Optional) ── */}
                        <p className="register-section-label">Address Details (Optional)</p>

                        <div className="register-form-row">
                            <div className="register-form-group">
                                <label htmlFor="village">Village</label>
                                <input
                                    id="village"
                                    name="village"
                                    type="text"
                                    value={formData.village}
                                    onChange={handleChange}
                                    placeholder="Village name"
                                />
                            </div>
                            <div className="register-form-group">
                                <label htmlFor="taluka">Taluka</label>
                                <input
                                    id="taluka"
                                    name="taluka"
                                    type="text"
                                    value={formData.taluka}
                                    onChange={handleChange}
                                    placeholder="Taluka / Block"
                                />
                            </div>
                        </div>

                        <div className="register-form-row">
                            <div className="register-form-group">
                                <label htmlFor="district">District</label>
                                <input
                                    id="district"
                                    name="district"
                                    type="text"
                                    value={formData.district}
                                    onChange={handleChange}
                                    placeholder="District"
                                />
                            </div>
                            <div className="register-form-group">
                                <label htmlFor="state">State</label>
                                <input
                                    id="state"
                                    name="state"
                                    type="text"
                                    value={formData.state}
                                    onChange={handleChange}
                                    placeholder="State"
                                />
                            </div>
                        </div>

                        <div className="register-form-group">
                            <label htmlFor="pinCode">PIN Code</label>
                            <input
                                id="pinCode"
                                name="pinCode"
                                type="text"
                                value={formData.pinCode}
                                onChange={handleChange}
                                placeholder="6-digit PIN code"
                                maxLength="6"
                            />
                        </div>


                        {/* ── SECTION: Financial Information (Optional) ── */}
                        <p className="register-section-label">Financial Information (Optional)</p>

                        <div className="register-form-row">
                            <div className="register-form-group">
                                <label htmlFor="annualIncome">Annual Income (₹)</label>
                                <input
                                    id="annualIncome"
                                    name="annualIncome"
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={formData.annualIncome}
                                    onChange={handleChange}
                                    placeholder="e.g. 120000"
                                />
                            </div>
                            <div className="register-form-group">
                                <label htmlFor="landHolding">Land Holding (acres)</label>
                                <input
                                    id="landHolding"
                                    name="landHolding"
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={formData.landHolding}
                                    onChange={handleChange}
                                    placeholder="e.g. 1.5"
                                />
                            </div>
                        </div>


                        {/* ── Terms ── */}
                        <label className="register-terms">
                            <input
                                type="checkbox"
                                name="terms"
                                checked={formData.terms}
                                onChange={handleChange}
                            />
                            <span>
                                I agree to the{' '}
                                <Link to="/terms">Terms of Service</Link>
                                {' '}and{' '}
                                <Link to="/privacy">Privacy Policy</Link>.
                            </span>
                        </label>


                        {/* ── Submit ── */}
                        <button
                            id="register-submit-btn"
                            type="submit"
                            className="register-button"
                            disabled={loading}
                        >
                            {loading && <span className="register-button-spinner" aria-hidden="true" />}
                            {loading ? 'Registering…' : 'Create Account'}
                        </button>

                    </form>


                    {/* ── Already have an account? ── */}
                    <div className="register-login">
                        <p>Already have an account?</p>
                        <Link to="/login">Sign In</Link>
                    </div>

                </div>
            </main>


            {/* Footer */}
            <footer className="register-footer">
                <p>&copy; 2024 Digital Subsidy &amp; Grant Platform (DSGP)</p>
            </footer>

        </div>
    );
}

export default Register;
