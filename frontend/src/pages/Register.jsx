import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Register.css';

function Register() {
    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        mobile: '',
        password: '',
        confirmPassword: ''
    });

    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData({
            ...formData,
            [name]: value
        });
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        if (formData.password !== formData.confirmPassword) {
            alert('Passwords do not match.');
            return;
        }

        console.log('Registration submitted:', formData);

        // Backend registration functionality will be added later.
    };

    return (
        <div className="register-page">

            {/* Header */}
            <header className="register-header">
                <div className="register-header-container">

                    <Link to="/" className="register-brand">
                        <h1>DSGP</h1>
                        <p>Digital Subsidy & Grant Platform</p>
                    </Link>

                    <Link
                        to="/"
                        className="register-home-link"
                    >
                        Back to Home
                    </Link>

                </div>
            </header>


            {/* Main */}
            <main className="register-main">

                <div className="register-card">

                    {/* Heading */}
                    <div className="register-card-header">

                        <h2>Create Your Account</h2>

                        <p>
                            Register to access DSGP services
                        </p>

                    </div>


                    {/* Form */}
                    <form
                        className="register-form"
                        onSubmit={handleSubmit}
                    >

                        {/* Full Name */}
                        <div className="register-form-group">

                            <label htmlFor="fullName">
                                Full Name
                            </label>

                            <input
                                type="text"
                                id="fullName"
                                name="fullName"
                                value={formData.fullName}
                                onChange={handleChange}
                                placeholder="Enter your full name"
                                required
                            />

                        </div>


                        {/* Email */}
                        <div className="register-form-group">

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


                        {/* Mobile */}
                        <div className="register-form-group">

                            <label htmlFor="mobile">
                                Mobile Number
                            </label>

                            <input
                                type="tel"
                                id="mobile"
                                name="mobile"
                                value={formData.mobile}
                                onChange={handleChange}
                                placeholder="Enter your mobile number"
                                pattern="[0-9]{10}"
                                maxLength="10"
                                required
                            />

                        </div>


                        {/* Password */}
                        <div className="register-form-group">

                            <label htmlFor="password">
                                Password
                            </label>

                            <input
                                type="password"
                                id="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                placeholder="Create a password"
                                minLength="6"
                                required
                            />

                        </div>


                        {/* Confirm Password */}
                        <div className="register-form-group">

                            <label htmlFor="confirmPassword">
                                Confirm Password
                            </label>

                            <input
                                type="password"
                                id="confirmPassword"
                                name="confirmPassword"
                                value={formData.confirmPassword}
                                onChange={handleChange}
                                placeholder="Re-enter your password"
                                minLength="6"
                                required
                            />

                        </div>


                        {/* Terms */}
                        <label className="register-terms">

                            <input
                                type="checkbox"
                                required
                            />

                            <span>
                                I agree to the Terms of Service and
                                Privacy Policy.
                            </span>

                        </label>


                        {/* Submit */}
                        <button
                            type="submit"
                            className="register-button"
                        >
                            Create Account
                        </button>

                    </form>


                    {/* Login */}
                    <div className="register-login">

                        <p>
                            Already have an account?
                        </p>

                        <Link to="/login">
                            Login
                        </Link>

                    </div>


                    {/* Notice */}
                    <div className="register-demo-notice">

                        <strong>Academic Project</strong>

                        <p>
                            Account registration is currently for
                            demonstration purposes. Backend authentication
                            will be connected later.
                        </p>

                    </div>

                </div>

            </main>


            {/* Footer */}
            <footer className="register-footer">

                <p>
                    &copy; 2024 Digital Subsidy & Grant Platform (DSGP)
                </p>

            </footer>

        </div>
    );
}

export default Register;