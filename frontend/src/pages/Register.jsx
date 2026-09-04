import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Register.css';

function Register() {
    const [formData, setFormData] = useState({
        fullName: '',
        govId: '',
        contact: '',
        email: '',
        age: '',
        address: '',
        schemeName: ''
    });

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const handleChange = (e) => {
        const { name, value } = e.target;

        setFormData({
            ...formData,
            [name]: value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        setLoading(true);
        setMessage('');
        setError('');

        try {
            const response = await fetch(
                'http://localhost:8080/api/v1/beneficiaries',
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        fullName: formData.fullName,
                        govId: formData.govId,
                        contact: formData.contact,
                        email: formData.email,
                        age: Number(formData.age),
                        address: formData.address,
                        schemeName: formData.schemeName
                    })
                }
            );

            const data = await response.json();

            if (!response.ok) {
                throw new Error(
                    data.message || 'Registration failed.'
                );
            }

            console.log('Registration successful:', data);

            setMessage(
                'Registration successful! Your beneficiary account has been created.'
            );

            setFormData({
                fullName: '',
                govId: '',
                contact: '',
                email: '',
                age: '',
                address: '',
                schemeName: ''
            });

        } catch (err) {
            console.error('Registration error:', err);

            setError(
                err.message ||
                'Unable to register. Please make sure the backend is running.'
            );

        } finally {
            setLoading(false);
        }
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


                    {/* Success Message */}
                    {message && (
                        <div className="register-success">
                            {message}
                        </div>
                    )}


                    {/* Error Message */}
                    {error && (
                        <div className="register-error">
                            {error}
                        </div>
                    )}


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


                        {/* Government ID */}
                        <div className="register-form-group">

                            <label htmlFor="govId">
                                Government ID
                            </label>

                            <input
                                type="text"
                                id="govId"
                                name="govId"
                                value={formData.govId}
                                onChange={handleChange}
                                placeholder="Enter Aadhaar / PAN / Voter ID"
                                required
                            />

                        </div>


                        {/* Contact */}
                        <div className="register-form-group">

                            <label htmlFor="contact">
                                Contact Number
                            </label>

                            <input
                                type="tel"
                                id="contact"
                                name="contact"
                                value={formData.contact}
                                onChange={handleChange}
                                placeholder="Enter your 10-digit mobile number"
                                pattern="[0-9]{10}"
                                maxLength="10"
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


                        {/* Age */}
                        <div className="register-form-group">

                            <label htmlFor="age">
                                Age
                            </label>

                            <input
                                type="number"
                                id="age"
                                name="age"
                                value={formData.age}
                                onChange={handleChange}
                                placeholder="Enter your age"
                                min="1"
                                max="120"
                                required
                            />

                        </div>


                        {/* Address */}
                        <div className="register-form-group">

                            <label htmlFor="address">
                                Address
                            </label>

                            <input
                                type="text"
                                id="address"
                                name="address"
                                value={formData.address}
                                onChange={handleChange}
                                placeholder="Enter your address"
                                required
                            />

                        </div>


                        {/* Scheme Name */}
                        <div className="register-form-group">

                            <label htmlFor="schemeName">
                                Scheme Name
                            </label>

                            <input
                                type="text"
                                id="schemeName"
                                name="schemeName"
                                value={formData.schemeName}
                                onChange={handleChange}
                                placeholder="Enter the scheme you are applying for"
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
                            disabled={loading}
                        >
                            {loading
                                ? 'Creating Account...'
                                : 'Create Account'
                            }
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

                        <strong>DSGP Registration</strong>

                        <p>
                            Your registration details will be stored
                            in the DSGP backend database.
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