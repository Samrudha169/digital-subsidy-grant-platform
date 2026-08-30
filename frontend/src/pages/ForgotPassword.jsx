import { useState } from 'react';
import { Link } from 'react-router-dom';
import './ForgotPassword.css';

function ForgotPassword() {
    const [email, setEmail] = useState('');
    const [submitted, setSubmitted] = useState(false);

    const handleSubmit = (e) => {
        e.preventDefault();
        setSubmitted(true);

        // Password reset functionality will be connected
        // to the backend later.
    };

    return (
        <div className="forgot-password-page">

            {/* Header */}
            <header className="forgot-password-header">
                <div className="forgot-password-header-container">

                    <Link to="/" className="forgot-password-brand">
                        <h1>DSGP</h1>
                        <p>Digital Subsidy & Grant Platform</p>
                    </Link>

                    <Link
                        to="/"
                        className="forgot-password-home-link"
                    >
                        Back to Home
                    </Link>

                </div>
            </header>


            {/* Main */}
            <main className="forgot-password-main">

                <div className="forgot-password-card">

                    {!submitted ? (
                        <>
                            <div className="forgot-password-card-header">

                                <h2>Forgot Password?</h2>

                                <p>
                                    Enter your registered email address and
                                    we'll help you reset your password.
                                </p>

                            </div>


                            <form
                                className="forgot-password-form"
                                onSubmit={handleSubmit}
                            >

                                <div className="forgot-password-form-group">

                                    <label htmlFor="reset-email">
                                        Email Address
                                    </label>

                                    <input
                                        type="email"
                                        id="reset-email"
                                        value={email}
                                        onChange={(e) =>
                                            setEmail(e.target.value)
                                        }
                                        placeholder="Enter your email address"
                                        required
                                    />

                                </div>


                                <button
                                    type="submit"
                                    className="forgot-password-button"
                                >
                                    Send Reset Instructions
                                </button>

                            </form>


                            <div className="forgot-password-back">

                                <Link to="/login">
                                    ← Back to Login
                                </Link>

                            </div>
                        </>
                    ) : (
                        <div className="forgot-password-success">

                            <div className="forgot-password-success-icon">
                                ✓
                            </div>

                            <h2>Check Your Email</h2>

                            <p>
                                If an account exists for
                                <strong> {email}</strong>, password reset
                                instructions will be sent to that email
                                address.
                            </p>

                            <p className="forgot-password-note">
                                Please check your inbox and spam folder.
                            </p>

                            <Link
                                to="/login"
                                className="forgot-password-login-button"
                            >
                                Back to Login
                            </Link>

                        </div>
                    )}


                    {/* Demo Notice */}
                    <div className="forgot-password-demo-notice">

                        <strong>Academic Project</strong>

                        <p>
                            Password recovery is currently for demonstration
                            purposes. Backend email functionality will be
                            connected later.
                        </p>

                    </div>

                </div>

            </main>


            {/* Footer */}
            <footer className="forgot-password-footer">

                <p>
                    &copy; 2024 Digital Subsidy & Grant Platform (DSGP)
                </p>

            </footer>

        </div>
    );
}

export default ForgotPassword;