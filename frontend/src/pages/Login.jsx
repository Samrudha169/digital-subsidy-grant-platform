import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Login.css';

function Login({ onLogin }) {
    const navigate = useNavigate();

    const [formData, setFormData] = useState({
        email: '',
        password: '',
        remember: false
    });

    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;

        setFormData({
            ...formData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const response = await fetch(
                'http://localhost:8080/api/v1/auth/login',
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        email: formData.email,
                        password: formData.password
                    })
                }
            );

            // Read response safely
            const responseText = await response.text();

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
                        `Server returned an invalid response (HTTP ${response.status})`
                    );
                }
            }

            console.log(
                'Login response:',
                response.status,
                data
            );

            // Backend returned an error
            if (!response.ok) {
                alert(
                    data.message ||
                    `Login failed. Server returned HTTP ${response.status}`
                );
                return;
            }

            // Login successful
            if (data.success) {

                localStorage.setItem(
                    'beneficiaryId',
                    data.beneficiaryId
                );

                localStorage.setItem(
                    'beneficiaryName',
                    data.name
                );

                localStorage.setItem(
                    'isLoggedIn',
                    'true'
                );

                // Remember email
                if (formData.remember) {
                    localStorage.setItem(
                        'rememberedEmail',
                        formData.email
                    );
                } else {
                    localStorage.removeItem(
                        'rememberedEmail'
                    );
                }

                // Update login state in App.jsx
                if (onLogin) {
                    onLogin();
                }

                alert('Login successful!');

                // Go to dashboard
                navigate('/dashboard');

            } else {

                alert(
                    data.message ||
                    'Invalid email or password'
                );
            }

        } catch (error) {

            console.error(
                'Login error:',
                error
            );

            alert(
                `Login request failed: ${error.message}`
            );

        } finally {

            setLoading(false);
        }
    };

    return (
        <div className="login-page">

            {/* Header */}
            <header className="login-header">
                <div className="login-header-container">

                    <Link
                        to="/"
                        className="login-brand"
                    >
                        <h1>DSGP</h1>

                        <p>
                            Digital Subsidy & Grant Platform
                        </p>
                    </Link>

                    <Link
                        to="/"
                        className="login-home-link"
                    >
                        Back to Home
                    </Link>

                </div>
            </header>


            {/* Login Section */}
            <main className="login-main">

                <div className="login-card">

                    <div className="login-card-header">

                        <h2>
                            Welcome Back
                        </h2>

                        <p>
                            Login to access your DSGP account
                        </p>

                    </div>


                    <form
                        className="login-form"
                        onSubmit={handleSubmit}
                    >

                        {/* Email */}
                        <div className="login-form-group">

                            <label htmlFor="email">
                                Email Address
                            </label>

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


                        {/* Password */}
                        <div className="login-form-group">

                            <label htmlFor="password">
                                Password
                            </label>

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


                        {/* Remember / Forgot */}
                        <div className="login-options">

                            <label className="remember-me">

                                <input
                                    type="checkbox"
                                    name="remember"
                                    checked={formData.remember}
                                    onChange={handleChange}
                                />

                                <span>
                                    Remember me
                                </span>

                            </label>


                            <Link
                                to="/forgot-password"
                                className="forgot-password"
                            >
                                Forgot Password?
                            </Link>

                        </div>


                        {/* Login Button */}
                        <button
                            type="submit"
                            className="login-button"
                            disabled={loading}
                        >
                            {loading
                                ? 'Logging in...'
                                : 'Login'}
                        </button>

                    </form>


                    {/* Register */}
                    <div className="login-register">

                        <p>
                            Don't have an account?
                        </p>

                        <Link to="/register">
                            Create an Account
                        </Link>

                    </div>


                    {/* Secure Login Notice */}
                    <div className="login-demo-notice">

                        <strong>
                            Secure Login
                        </strong>

                        <p>
                            Your email and password are securely
                            verified by the DSGP backend.
                        </p>

                    </div>

                    {/* Officer Portal link */}
                    <div style={{
                        textAlign: 'center',
                        marginTop: '1rem',
                        paddingTop: '1rem',
                        borderTop: '1px solid rgba(0,0,0,0.08)',
                    }}>
                        <p style={{ fontSize: '0.8rem', color: '#6b7280', margin: '0 0 0.4rem' }}>
                            Government officer?
                        </p>
                        <Link
                            to="/officer/login"
                            id="link-officer-portal"
                            style={{
                                fontSize: '0.82rem',
                                fontWeight: 600,
                                color: '#1a365d',
                                textDecoration: 'none',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.3rem',
                            }}
                        >
                            🏛️ Officer Portal →
                        </Link>
                    </div>

                </div>

            </main>


            {/* Footer */}
            <footer className="login-footer">

                <p>
                    &copy; 2024 Digital Subsidy & Grant Platform
                    (DSGP)
                </p>

            </footer>

        </div>
    );
}

export default Login;