import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Login.css';

const API = 'http://localhost:8080/api/v1';

function Login({ onLogin }) {
    const navigate = useNavigate();

    // ── Step state ──────────────────────────────────────────────────────────
    // 'password' → show email+password form
    // 'otp'      → show OTP input (email already verified by backend)
    const [step, setStep] = useState('password');

    // ── Password step state ─────────────────────────────────────────────────
    const [formData, setFormData] = useState({
        email: '',
        password: '',
        remember: false
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // ── OTP step state ──────────────────────────────────────────────────────
    const [otpEmail, setOtpEmail] = useState('');   // email from backend response
    const [otp, setOtp] = useState('');
    const [otpLoading, setOtpLoading] = useState(false);
    const [otpError, setOtpError] = useState('');
    const [resendCooldown, setResendCooldown] = useState(0);
    const cooldownRef = useRef(null);

    // Tick the resend countdown every second
    useEffect(() => {
        if (resendCooldown > 0) {
            cooldownRef.current = setTimeout(
                () => setResendCooldown(c => c - 1),
                1000
            );
        }
        return () => clearTimeout(cooldownRef.current);
    }, [resendCooldown]);

    // ── Password step handlers ───────────────────────────────────────────────
    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            const response = await fetch(`${API}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: formData.email,
                    password: formData.password
                })
            });

            const responseText = await response.text();
            let data = {};
            if (responseText) {
                try { data = JSON.parse(responseText); }
                catch { throw new Error(`Server returned an invalid response (HTTP ${response.status})`); }
            }

            if (!response.ok) {
                setError(data.message || `Login failed (HTTP ${response.status})`);
                return;
            }

            if (!data.success) {
                setError(data.message || 'Invalid email or password');
                return;
            }

            // ── Two-step: OTP required ────────────────────────────────────────
            if (data.otpRequired) {
                setOtpEmail(data.email);
                setStep('otp');
                setResendCooldown(60);
                return;
            }

            // ── Legacy / immediate success ────────────────────────────────────
            grantSession(data);

        } catch (err) {
            setError(`Login request failed: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    // ── OTP step handlers ────────────────────────────────────────────────────
    const handleOtpSubmit = async (e) => {
        e.preventDefault();
        setOtpLoading(true);
        setOtpError('');

        try {
            const response = await fetch(`${API}/auth/verify-login-otp`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: otpEmail, otp })
            });

            const responseText = await response.text();
            let data = {};
            if (responseText) {
                try { data = JSON.parse(responseText); }
                catch { throw new Error('Invalid server response'); }
            }

            if (!response.ok || !data.success) {
                setOtpError(data.message || 'Invalid login code. Please try again.');
                return;
            }

            // OTP verified — fetch beneficiary details then grant session
            // The verify endpoint returns OtpVerifyResponse (success, message).
            // We need beneficiaryId/name; fetch them by email via a second lookup,
            // or re-use the original beneficiary fields if the backend returns them.
            // For now, the backend verify-login-otp returns OtpVerifyResponse only;
            // we store the email and navigate — the dashboard reads beneficiaryId
            // lazily. We must do a second login-data fetch using a dedicated endpoint.
            // Since the session is already confirmed, call a lightweight profile API.
            await finaliseSession();

        } catch (err) {
            setOtpError(`Verification failed: ${err.message}`);
        } finally {
            setOtpLoading(false);
        }
    };

    const handleResend = async () => {
        if (resendCooldown > 0) return;
        setOtpError('');

        try {
            const response = await fetch(`${API}/auth/resend-login-otp`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: otpEmail })
            });
            const data = await response.json().catch(() => ({}));

            if (!response.ok || !data.success) {
                setOtpError(data.message || 'Could not resend code. Please wait and try again.');
                return;
            }

            setResendCooldown(60);
        } catch (err) {
            setOtpError('Resend failed. Please try again.');
        }
    };

    // After OTP is verified, resolve beneficiaryId + name by doing a
    // lightweight profile call, then write localStorage.
    const finaliseSession = async () => {
        try {
            // Attempt profile lookup so we get beneficiaryId + name
            const profResp = await fetch(
                `${API}/beneficiaries/profile?email=${encodeURIComponent(otpEmail)}`
            );

            if (profResp.ok) {
                const prof = await profResp.json();
                localStorage.setItem('beneficiaryId', prof.id ?? '');
                localStorage.setItem('beneficiaryName', prof.fullName ?? '');
            } else {
                // Profile endpoint not yet available — store email as fallback
                localStorage.setItem('beneficiaryId', '');
                localStorage.setItem('beneficiaryName', otpEmail);
            }
        } catch {
            localStorage.setItem('beneficiaryId', '');
            localStorage.setItem('beneficiaryName', otpEmail);
        }

        localStorage.setItem('isLoggedIn', 'true');

        if (formData.remember) {
            localStorage.setItem('rememberedEmail', otpEmail);
        } else {
            localStorage.removeItem('rememberedEmail');
        }

        if (onLogin) onLogin();
        navigate('/dashboard');
    };

    // Immediate-success path (legacy accounts)
    const grantSession = (data) => {
        localStorage.setItem('beneficiaryId', data.beneficiaryId ?? '');
        localStorage.setItem('beneficiaryName', data.name ?? '');
        localStorage.setItem('isLoggedIn', 'true');

        if (formData.remember) {
            localStorage.setItem('rememberedEmail', formData.email);
        } else {
            localStorage.removeItem('rememberedEmail');
        }

        if (onLogin) onLogin();
        navigate('/dashboard');
    };

    // ── Render ───────────────────────────────────────────────────────────────
    return (
        <div className="login-page">

            <main className="login-main">
                <div className="login-card">

                    {step === 'password' ? (
                        <>
                            <div className="login-card-header">
                                <h2>Welcome Back</h2>
                                <p>Login to access your DSGP account</p>
                            </div>

                            {error && (
                                <div className="login-error" style={{ color: '#dc2626', marginBottom: '1rem', fontSize: '0.9rem' }}>
                                    {error}
                                </div>
                            )}

                            <form className="login-form" onSubmit={handleSubmit}>

                                <div className="login-form-group">
                                    <label htmlFor="email">Email Address</label>
                                    <input
                                        type="email"
                                        id="email"
                                        name="email"
                                        value={formData.email}
                                        onChange={handleChange}
                                        placeholder="Enter your email address"
                                        required
                                    />
                                </div>

                                <div className="login-form-group">
                                    <label htmlFor="password">Password</label>
                                    <input
                                        type="password"
                                        id="password"
                                        name="password"
                                        value={formData.password}
                                        onChange={handleChange}
                                        placeholder="Enter your password"
                                        required
                                    />
                                </div>

                                <div className="login-options">
                                    <label className="remember-me">
                                        <input
                                            type="checkbox"
                                            name="remember"
                                            checked={formData.remember}
                                            onChange={handleChange}
                                        />
                                        <span>Remember me</span>
                                    </label>

                                    <Link to="/forgot-password" className="forgot-password">
                                        Forgot Password?
                                    </Link>
                                </div>

                                <button
                                    type="submit"
                                    className="login-button"
                                    disabled={loading}
                                >
                                    {loading ? 'Verifying...' : 'Login'}
                                </button>
                            </form>

                            <div className="login-register">
                                <p>Don't have an account?</p>
                                <Link to="/register">Create an Account</Link>
                            </div>

                            <div className="login-demo-notice">
                                <strong>Secure Login</strong>
                                <p>Your email and password are securely verified by the DSGP backend.</p>
                            </div>

                            <div style={{ textAlign: 'center', marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid rgba(0,0,0,0.08)' }}>
                                <p style={{ fontSize: '0.8rem', color: '#6b7280', margin: '0 0 0.4rem' }}>
                                    Government officer?
                                </p>
                                <Link
                                    to="/officer/login"
                                    id="link-officer-portal"
                                    style={{ fontSize: '0.82rem', fontWeight: 600, color: '#1a365d', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}
                                >
                                    🏛️ Officer Portal →
                                </Link>
                            </div>
                        </>
                    ) : (
                        /* ── OTP Step ─────────────────────────────────────────── */
                        <>
                            <div className="login-card-header">
                                <h2>Verify Your Login</h2>
                                <p>
                                    A 6-digit code has been sent to<br />
                                    <strong>{otpEmail}</strong>
                                </p>
                            </div>

                            {otpError && (
                                <div className="login-error" style={{ color: '#dc2626', marginBottom: '1rem', fontSize: '0.9rem' }}>
                                    {otpError}
                                </div>
                            )}

                            <form className="login-form" onSubmit={handleOtpSubmit}>
                                <div className="login-form-group">
                                    <label htmlFor="login-otp">Login Code</label>
                                    <input
                                        type="text"
                                        id="login-otp"
                                        inputMode="numeric"
                                        pattern="\d{6}"
                                        maxLength={6}
                                        value={otp}
                                        onChange={e => setOtp(e.target.value.replace(/\D/g, ''))}
                                        placeholder="Enter 6-digit code"
                                        required
                                        autoFocus
                                        style={{ letterSpacing: '0.3em', fontSize: '1.25rem', textAlign: 'center' }}
                                    />
                                </div>

                                <button
                                    type="submit"
                                    className="login-button"
                                    disabled={otpLoading || otp.length !== 6}
                                >
                                    {otpLoading ? 'Verifying...' : 'Verify & Login'}
                                </button>
                            </form>

                            <div style={{ textAlign: 'center', marginTop: '1.2rem', fontSize: '0.88rem', color: '#6b7280' }}>
                                Didn't receive a code?{' '}
                                <button
                                    type="button"
                                    onClick={handleResend}
                                    disabled={resendCooldown > 0}
                                    style={{
                                        background: 'none',
                                        border: 'none',
                                        color: resendCooldown > 0 ? '#9ca3af' : '#1a365d',
                                        fontWeight: 600,
                                        cursor: resendCooldown > 0 ? 'default' : 'pointer',
                                        padding: 0,
                                        fontSize: '0.88rem'
                                    }}
                                >
                                    {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : 'Resend Code'}
                                </button>
                            </div>

                            <div style={{ textAlign: 'center', marginTop: '1rem' }}>
                                <button
                                    type="button"
                                    onClick={() => { setStep('password'); setOtp(''); setOtpError(''); }}
                                    style={{ background: 'none', border: 'none', color: '#6b7280', fontSize: '0.82rem', cursor: 'pointer', textDecoration: 'underline' }}
                                >
                                    ← Back to Login
                                </button>
                            </div>
                        </>
                    )}

                </div>
            </main>

            <footer className="login-footer">
                <p>&copy; 2024 Digital Subsidy &amp; Grant Platform (DSGP)</p>
            </footer>

        </div>
    );
}

export default Login;