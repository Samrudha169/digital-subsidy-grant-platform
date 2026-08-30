import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Login.css';

function Login() {
    const [formData, setFormData] = useState({
        email: '',
        password: '',
        remember: false
    });

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;

        setFormData({
            ...formData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        console.log('Login submitted:', formData);

        // Backend login functionality will be added later.
    };

    return (
        <div className="login-page">

            {/* Header */}
            <header className="login-header">
                <div className="login-header-container">

                    <Link to="/" className="login-brand">
                        <h1>DSGP</h1>
                        <p>Digital Subsidy & Grant Platform</p>
                    </Link>

                    <Link to="/" className="login-home-link">
                        Back to Home
                    </Link>

                </div>
            </header>


            {/* Login Section */}
            <main className="login-main">

                <div className="login-card">

                    <div className="login-card-header">
                        <h2>Welcome Back</h2>

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
                        >
                            Login
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


                    {/* Demo Notice */}
                    <div className="login-demo-notice">

                        <strong>Academic Project</strong>

                        <p>
                            Login functionality is currently for
                            demonstration purposes. Backend authentication
                            will be connected later.
                        </p>

                    </div>

                </div>

            </main>


            {/* Footer */}
            <footer className="login-footer">

                <p>
                    &copy; 2024 Digital Subsidy & Grant Platform (DSGP)
                </p>

            </footer>

        </div>
    );
}

export default Login;