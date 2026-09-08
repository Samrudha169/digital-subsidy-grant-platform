import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import './OfficerLogin.css';

/**
 * OfficerLogin — Separate authentication portal for government officers.
 * Calls POST /api/v1/auth/officer-login and stores officer session in localStorage.
 * Does NOT reuse beneficiary login — officer roles and routing are distinct.
 */
function OfficerLogin({ onOfficerLogin }) {

    const navigate = useNavigate();

    const [form, setForm] = useState({ username: '', password: '' });
    const [errors, setErrors] = useState({});
    const [serverError, setServerError] = useState('');
    const [loading, setLoading] = useState(false);

    // ── Input change handler ───────────────────────────────────────────────────

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm(prev => ({ ...prev, [name]: value }));
        if (errors[name]) setErrors(prev => ({ ...prev, [name]: '' }));
        setServerError('');
    };

    // ── Validation ────────────────────────────────────────────────────────────

    const validate = () => {
        const newErrors = {};
        if (!form.username.trim()) newErrors.username = 'Username is required.';
        if (!form.password)        newErrors.password = 'Password is required.';
        return newErrors;
    };

    // ── Form submission ───────────────────────────────────────────────────────

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
            const res = await fetch('/api/v1/auth/officer-login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: form.username.trim(),
                    password: form.password,
                }),
            });

            const data = await res.json();

            if (!data.success) {
                setServerError(data.message || 'Invalid username or password.');
                return;
            }

            // Persist officer session
            localStorage.setItem('officerLoggedIn', 'true');
            localStorage.setItem('officerId',    String(data.officerId));
            localStorage.setItem('officerUsername', data.username);
            localStorage.setItem('officerName',  data.fullName);
            localStorage.setItem('officerRole',  data.role);
            localStorage.setItem('officerDistrict', data.district || '');

            if (onOfficerLogin) onOfficerLogin(data.role);

            // Route to officer dashboard
            navigate('/officer/dashboard');

        } catch {
            setServerError('Unable to connect to the server. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    // ── Render ────────────────────────────────────────────────────────────────

    return (
        <div className="officer-login-page">
            <div className="officer-login-card">

                <div className="officer-login-header">
                    <div className="officer-badge">
                        <span className="officer-badge-icon">🏛️</span>
                        Government Officer Portal
                    </div>
                    <h1>Officer Sign In</h1>
                    <p>Authenticate with your assigned officer credentials.</p>
                </div>

                <div className="role-chips">
                    <span className="role-chip role-chip-field">Field Officer</span>
                    <span className="role-chip role-chip-district">District Officer</span>
                    <span className="role-chip role-chip-finance">Finance Approver</span>
                </div>

                {serverError && (
                    <div className="officer-alert officer-alert-error" role="alert">
                        <span className="officer-alert-icon">✕</span>
                        <span>{serverError}</span>
                    </div>
                )}

                <form className="officer-form" onSubmit={handleSubmit} noValidate>

                    <div className="officer-form-group">
                        <label htmlFor="officer-username">Username</label>
                        <input
                            id="officer-username"
                            name="username"
                            type="text"
                            autoComplete="username"
                            placeholder="e.g. field.officer1"
                            value={form.username}
                            onChange={handleChange}
                            className={errors.username ? 'input-error' : ''}
                            disabled={loading}
                        />
                        {errors.username && (
                            <span className="field-error-msg" role="alert">
                                {errors.username}
                            </span>
                        )}
                    </div>

                    <div className="officer-form-group">
                        <label htmlFor="officer-password">Password</label>
                        <input
                            id="officer-password"
                            name="password"
                            type="password"
                            autoComplete="current-password"
                            placeholder="Enter your password"
                            value={form.password}
                            onChange={handleChange}
                            className={errors.password ? 'input-error' : ''}
                            disabled={loading}
                        />
                        {errors.password && (
                            <span className="field-error-msg" role="alert">
                                {errors.password}
                            </span>
                        )}
                    </div>

                    <button
                        id="btn-officer-login"
                        type="submit"
                        className="officer-submit-btn"
                        disabled={loading}
                    >
                        {loading ? 'Signing in…' : 'Sign In'}
                    </button>

                </form>

                <div className="officer-login-footer">
                    Not an officer?{' '}
                    <Link to="/login">Beneficiary Login</Link>
                </div>

            </div>
        </div>
    );
}

export default OfficerLogin;
