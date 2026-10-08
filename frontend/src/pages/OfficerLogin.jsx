import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import './OfficerLogin.css';

/**
 * OfficerLogin — Separate authentication portal for government officers.
 *
 * Calls POST /api/v1/auth/officer-login and stores officer session
 * information in localStorage.
 *
 * Officer roles:
 * - FIELD_OFFICER
 * - DISTRICT_OFFICER
 * - FINANCE_APPROVER
 * - ADMIN
 */
function OfficerLogin({ onOfficerLogin }) {

    const navigate = useNavigate();

    const [form, setForm] = useState({
        username: '',
        password: ''
    });

    const [errors, setErrors] = useState({});
    const [serverError, setServerError] = useState('');
    const [loading, setLoading] = useState(false);


    // ── Input change handler ───────────────────────────────────────────────

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


    // ── Validation ────────────────────────────────────────────────────────

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


    // ── Form submission ───────────────────────────────────────────────────

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

                    console.error(
                        'Invalid JSON response:',
                        responseText
                    );

                    throw new Error(
                        `Server returned an invalid response (HTTP ${res.status})`
                    );
                }
            }


            console.log(
                'Officer login response:',
                res.status,
                data
            );


            // Backend returned an error

            if (!res.ok || !data.success) {

                setServerError(
                    data.message ||
                    'Invalid username or password.'
                );

                return;
            }


            // ================================================================
            // OFFICER SESSION
            // ================================================================

            localStorage.setItem(
                'officerLoggedIn',
                'true'
            );

            localStorage.setItem(
                'officerId',
                String(data.officerId)
            );

            localStorage.setItem(
                'officerUsername',
                data.username || ''
            );

            localStorage.setItem(
                'officerName',
                data.fullName || ''
            );

            localStorage.setItem(
                'officerRole',
                data.role || ''
            );

            localStorage.setItem(
                'officerDistrict',
                data.district || ''
            );


            // Update login state in App.jsx

            if (onOfficerLogin) {
                onOfficerLogin(data.role);
            }


            // ================================================================
            // ROLE-BASED ROUTING
            // ================================================================

            // Route based on officer role
            if (data.role === 'ADMIN') {
                navigate('/admin/dashboard');
            } else {
                navigate('/officer/dashboard');
            }


        } catch (error) {

            console.error(
                'Officer login error:',
                error
            );

            setServerError(
                error.message ||
                'Unable to connect to the server. Please try again.'
            );

        } finally {

            setLoading(false);
        }
    };


    // ── Render ─────────────────────────────────────────────────────────────

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