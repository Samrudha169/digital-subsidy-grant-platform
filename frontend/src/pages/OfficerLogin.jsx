import { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { verifyOfficerOtp, resendOfficerOtp } from '../services/api';
import './OfficerLogin.css';

/**
 * OfficerLogin — Separate authentication portal for government officers.
 *
 * Two-step flow for FIELD_OFFICER, DISTRICT_OFFICER, and FINANCE_APPROVER:
 *   Step 1 — Enter username + password   → POST /auth/officer-login
 *   Step 2 — Enter 6-digit email OTP     → POST /auth/officer-verify-otp
 *
 * ADMIN accounts skip OTP and are routed to the admin dashboard immediately.
 *
 * Officer roles:
 * - FIELD_OFFICER
 * - DISTRICT_OFFICER
 * - FINANCE_APPROVER
 * - ADMIN
 */
function OfficerLogin({ onOfficerLogin }) {

    const navigate = useNavigate();

    // ── Step 1 state ──────────────────────────────────────────────────────
    const [form, setForm] = useState({
        username: '',
        password: ''
    });

    const [errors, setErrors] = useState({});
    const [serverError, setServerError] = useState('');
    const [loading, setLoading] = useState(false);

    // ── Step 2 (OTP) state ─────────────────────────────────────────────────
    /**
     * When otpStep is true, password auth succeeded and the officer must
     * submit their email OTP before any session is stored.
     */
    const [otpStep, setOtpStep] = useState(false);

    /**
     * Pending officer data from step 1 — held in component state only.
     * NOT written to localStorage until OTP verification succeeds.
     */
    const [pendingOfficer, setPendingOfficer] = useState(null);

    const [otpValue, setOtpValue] = useState('');
    const [otpError, setOtpError] = useState('');
    const [otpLoading, setOtpLoading] = useState(false);
    const [otpInfoMsg, setOtpInfoMsg] = useState('');

    /**
     * Client-side resend cooldown counter (seconds remaining).
     * Starts at 60 when the OTP step is entered and counts down to 0.
     * Resets to 60 after every successful resend.
     * Mirrors the 60-second server-side cooldown.
     */
    const [resendCountdown, setResendCountdown] = useState(60);
    const countdownRef = useRef(null);

    // Start/restart the countdown whenever the OTP step becomes active.
    useEffect(() => {
        if (!otpStep) return;

        // Clear any existing interval before starting a new one
        if (countdownRef.current) clearInterval(countdownRef.current);

        setResendCountdown(60);

        countdownRef.current = setInterval(() => {
            setResendCountdown(prev => {
                if (prev <= 1) {
                    clearInterval(countdownRef.current);
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(countdownRef.current);
    }, [otpStep]);

    /** Resets the resend countdown to 60 and restarts the interval. */
    const resetResendCountdown = () => {
        if (countdownRef.current) clearInterval(countdownRef.current);
        setResendCountdown(60);
        countdownRef.current = setInterval(() => {
            setResendCountdown(prev => {
                if (prev <= 1) {
                    clearInterval(countdownRef.current);
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
    };


    // ── Step 1: Input change handler ──────────────────────────────────────

    const handleChange = (e) => {

        const { name, value } = e.target;

        setForm(prev => ({
            ...prev,
            [name]: value
        }));

        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }

        setServerError('');
    };


    // ── Step 1: Validation ────────────────────────────────────────────────

    const validate = () => {

        const newErrors = {};

        if (!form.username.trim()) {
            newErrors.username = 'Username is required.';
        }

        if (!form.password) {
            newErrors.password = 'Password is required.';
        }

        return newErrors;
    };


    // ── Step 1: Form submission ───────────────────────────────────────────

    const handleSubmit = async (e) => {

        e.preventDefault();

        const validationErrors = validate();

        if (Object.keys(validationErrors).length > 0) {
            setErrors(validationErrors);
            return;
        }

        setLoading(true);
        setServerError('');

        try {

            const res = await fetch(
                '/api/v1/auth/officer-login',
                {
                    method: 'POST',

                    headers: {
                        'Content-Type': 'application/json'
                    },

                    body: JSON.stringify({
                        username: form.username.trim(),
                        password: form.password
                    })
                }
            );


            // Safely read the response
            const responseText = await res.text();
            let data = {};

            if (responseText) {
                try {
                    data = JSON.parse(responseText);
                } catch (parseError) {
                    console.error('Invalid JSON response:', responseText);
                    throw new Error(`Server returned an invalid response (HTTP ${res.status})`);
                }
            }


            // Backend returned an error
            if (!res.ok || !data.success) {
                setServerError(
                    data.message ||
                    'Invalid username or password.'
                );
                return;
            }


            // ── ADMIN: no OTP required, store session immediately ─────────────
            if (data.role === 'ADMIN' && !data.otpRequired) {
                storeOfficerSession(data);
                if (onOfficerLogin) onOfficerLogin(data.role);
                navigate('/admin/dashboard');
                return;
            }


            // ── Non-ADMIN: OTP required ───────────────────────────────────────
            // Hold officer data in state — do NOT write to localStorage yet.
            setPendingOfficer(data);
            setOtpStep(true);


        } catch (error) {

            console.error('Officer login error:', error);

            setServerError(
                error.message ||
                'Unable to connect to the server. Please try again.'
            );

        } finally {
            setLoading(false);
        }
    };


    // ── Step 2: OTP submission ────────────────────────────────────────────

    const handleOtpSubmit = async (e) => {

        e.preventDefault();

        if (!/^\d{6}$/.test(otpValue)) {
            setOtpError('Please enter a valid 6-digit code.');
            return;
        }

        setOtpLoading(true);
        setOtpError('');
        setOtpInfoMsg('');

        try {

            const result = await verifyOfficerOtp(pendingOfficer.officerId, otpValue);

            if (!result.success) {
                setOtpError(result.message || 'Invalid or expired code. Please try again.');
                return;
            }

            // ── OTP verified: now write session and navigate ───────────────────
            storeOfficerSession(pendingOfficer);

            if (onOfficerLogin) onOfficerLogin(pendingOfficer.role);

            navigate('/officer/dashboard');

        } catch (error) {

            console.error('OTP verification error:', error);
            setOtpError('Unable to verify the code. Please try again.');

        } finally {
            setOtpLoading(false);
        }
    };


    // ── Step 2: Resend OTP ────────────────────────────────────────────────

    const handleResendOtp = async () => {

        setOtpLoading(true);
        setOtpError('');
        setOtpInfoMsg('');

        try {

            const result = await resendOfficerOtp(pendingOfficer.officerId);

            if (!result.success) {
                setOtpError(result.message || 'Could not resend the code. Please try again.');
            } else {
                setOtpInfoMsg('A new verification code has been sent to your email.');
                setOtpValue('');
                resetResendCountdown();
            }

        } catch (error) {

            console.error('OTP resend error:', error);
            setOtpError('Unable to resend. Please try again.');

        } finally {
            setOtpLoading(false);
        }
    };


    // ── Helper: write officer session to localStorage ─────────────────────

    const storeOfficerSession = (data) => {
        localStorage.setItem('officerLoggedIn', 'true');
        localStorage.setItem('officerId',       String(data.officerId));
        localStorage.setItem('officerUsername', data.username || '');
        localStorage.setItem('officerName',     data.fullName || '');
        localStorage.setItem('officerRole',     data.role     || '');
        localStorage.setItem('officerDistrict', data.district || '');
    };


    // ── Render — OTP step ─────────────────────────────────────────────────

    if (otpStep && pendingOfficer) {
        return (
            <div className="officer-login-page">

                <div className="officer-login-card">

                    {/* Header */}
                    <div className="officer-login-header">

                        <div className="officer-badge">
                            <span className="officer-badge-icon">🔐</span>
                            Email Verification
                        </div>

                        <h1>Enter Verification Code</h1>

                        <p>
                            A 6-digit code has been sent to your registered
                            email address. Enter it below to complete login.
                        </p>

                        <p style={{ fontSize: '0.85rem', opacity: 0.7, marginTop: '0.25rem' }}>
                            Code expires in 5 minutes.
                        </p>

                    </div>


                    {/* Error */}
                    {otpError && (
                        <div
                            className="officer-alert officer-alert-error"
                            role="alert"
                        >
                            <span className="officer-alert-icon">✕</span>
                            <span>{otpError}</span>
                        </div>
                    )}

                    {/* Info / success message */}
                    {otpInfoMsg && (
                        <div
                            className="officer-alert officer-alert-success"
                            role="status"
                        >
                            <span className="officer-alert-icon">✓</span>
                            <span>{otpInfoMsg}</span>
                        </div>
                    )}


                    {/* OTP form */}
                    <form
                        className="officer-form"
                        onSubmit={handleOtpSubmit}
                        noValidate
                    >

                        <div className="officer-form-group">

                            <label htmlFor="officer-otp-code">
                                Verification Code
                            </label>

                            <input
                                id="officer-otp-code"
                                name="otp"
                                type="text"
                                inputMode="numeric"
                                autoComplete="one-time-code"
                                placeholder="000000"
                                maxLength={6}
                                value={otpValue}
                                onChange={(e) => {
                                    setOtpValue(e.target.value.replace(/\D/g, ''));
                                    setOtpError('');
                                }}
                                className={otpError ? 'input-error' : ''}
                                disabled={otpLoading}
                                style={{ letterSpacing: '0.4em', fontSize: '1.5rem', textAlign: 'center' }}
                            />

                        </div>


                        <button
                            id="btn-officer-otp-verify"
                            type="submit"
                            className="officer-submit-btn"
                            disabled={otpLoading || otpValue.length !== 6}
                        >
                            {otpLoading ? 'Verifying…' : 'Verify & Sign In'}
                        </button>

                    </form>


                    {/* Resend + back links */}
                    <div className="officer-login-footer" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>

                        <button
                            id="btn-officer-otp-resend"
                            type="button"
                            className="link-btn"
                            onClick={handleResendOtp}
                            disabled={otpLoading || resendCountdown > 0}
                            style={{
                                background: 'none',
                                border: 'none',
                                cursor: resendCountdown > 0 ? 'default' : 'pointer',
                                color: resendCountdown > 0 ? '#475569' : 'var(--primary, #4f8ef7)',
                                textDecoration: resendCountdown > 0 ? 'none' : 'underline',
                                padding: 0,
                                transition: 'color 0.3s'
                            }}
                        >
                            {resendCountdown > 0
                                ? `Resend Code (${resendCountdown}s)`
                                : 'Resend Code'}
                        </button>

                        <button
                            id="btn-officer-otp-back"
                            type="button"
                            className="link-btn"
                            onClick={() => {
                                setOtpStep(false);
                                setPendingOfficer(null);
                                setOtpValue('');
                                setOtpError('');
                                setOtpInfoMsg('');
                            }}
                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--secondary, #aaa)', textDecoration: 'underline', padding: 0 }}
                        >
                            ← Back to Login
                        </button>

                    </div>

                </div>

            </div>
        );
    }


    // ── Render — Step 1: username + password ──────────────────────────────

    return (

        <div className="officer-login-page">

            <div className="officer-login-card">


                {/* Header */}

                <div className="officer-login-header">

                    <div className="officer-badge">

                        <span className="officer-badge-icon">
                            🏛️
                        </span>

                        Government Officer Portal

                    </div>


                    <h1>
                        Officer Sign In
                    </h1>


                    <p>
                        Authenticate with your assigned officer credentials.
                    </p>

                </div>


                {/* Role chips */}

                <div className="role-chips">

                    <span className="role-chip role-chip-field">
                        Field Officer
                    </span>

                    <span className="role-chip role-chip-district">
                        District Officer
                    </span>

                    <span className="role-chip role-chip-finance">
                        Finance Approver
                    </span>

                    <span className="role-chip">
                        Administrator
                    </span>

                </div>


                {/* Server error */}

                {serverError && (

                    <div
                        className="officer-alert officer-alert-error"
                        role="alert"
                    >

                        <span className="officer-alert-icon">
                            ✕
                        </span>

                        <span>
                            {serverError}
                        </span>

                    </div>
                )}


                {/* Login form */}

                <form
                    className="officer-form"
                    onSubmit={handleSubmit}
                    noValidate
                >


                    {/* Username */}

                    <div className="officer-form-group">

                        <label htmlFor="officer-username">
                            Username
                        </label>

                        <input
                            id="officer-username"
                            name="username"
                            type="text"
                            autoComplete="username"
                            placeholder="e.g. field.officer1"
                            value={form.username}
                            onChange={handleChange}
                            className={
                                errors.username
                                    ? 'input-error'
                                    : ''
                            }
                            disabled={loading}
                        />

                        {errors.username && (

                            <span
                                className="field-error-msg"
                                role="alert"
                            >
                                {errors.username}
                            </span>

                        )}

                    </div>


                    {/* Password */}

                    <div className="officer-form-group">

                        <label htmlFor="officer-password">
                            Password
                        </label>

                        <input
                            id="officer-password"
                            name="password"
                            type="password"
                            autoComplete="current-password"
                            placeholder="Enter your password"
                            value={form.password}
                            onChange={handleChange}
                            className={
                                errors.password
                                    ? 'input-error'
                                    : ''
                            }
                            disabled={loading}
                        />

                        {errors.password && (

                            <span
                                className="field-error-msg"
                                role="alert"
                            >
                                {errors.password}
                            </span>

                        )}

                    </div>


                    {/* Login button */}

                    <button
                        id="btn-officer-login"
                        type="submit"
                        className="officer-submit-btn"
                        disabled={loading}
                    >

                        {loading
                            ? 'Signing in…'
                            : 'Sign In'
                        }

                    </button>

                </form>


                {/* Footer */}

                <div className="officer-login-footer">

                    Not an officer?{' '}

                    <Link to="/login">
                        Beneficiary Login
                    </Link>

                </div>


            </div>

        </div>
    );
}

export default OfficerLogin;