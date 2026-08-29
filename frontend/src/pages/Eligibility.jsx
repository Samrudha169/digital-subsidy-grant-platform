import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Eligibility.css';

function Eligibility() {
    const [formData, setFormData] = useState({
        state: '',
        age: '',
        occupation: '',
        category: '',
        income: ''
    });

    const [results, setResults] = useState([]);
    const [submitted, setSubmitted] = useState(false);

    const schemes = [
        {
            id: 1,
            name: 'PM-KISAN',
            category: 'Agriculture',
            target: 'Farmers',
            description:
                'Income support scheme providing ₹6,000 per year to eligible farmer families.',
            path: '/schemes/pm-kisan'
        },
        {
            id: 2,
            name: 'National Scholarship Portal (NSP)',
            category: 'Education',
            target: 'Students',
            description:
                'Centralized platform offering various scholarships for eligible students.',
            path: '/schemes/nsp'
        },
        {
            id: 3,
            name: 'PMEGP',
            category: 'Business & Entrepreneurship',
            target: 'Entrepreneurs',
            description:
                'Credit-linked subsidy scheme supporting self-employment through micro-enterprises.',
            path: '/schemes/pmegp'
        }
    ];

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        let eligibleSchemes = [];

        if (formData.occupation === 'farmer') {
            eligibleSchemes.push(schemes[0]);
        }

        if (formData.occupation === 'student') {
            eligibleSchemes.push(schemes[1]);
        }

        if (
            formData.occupation === 'entrepreneur' ||
            formData.occupation === 'business-owner'
        ) {
            eligibleSchemes.push(schemes[2]);
        }

        setResults(eligibleSchemes);
        setSubmitted(true);
    };

    return (
        <div className="eligibility-page">

            {/* Header */}
            <section className="eligibility-hero">
                <div className="eligibility-hero-content">
                    <h1>Check Your Eligibility</h1>
                    <p>
                        Enter your details to discover government schemes
                        that may be suitable for you.
                    </p>
                </div>
            </section>

            {/* Form */}
            <main className="eligibility-container">

                <div className="eligibility-card">

                    <div className="form-heading">
                        <h2>Your Profile</h2>
                        <p>
                            Provide the following information to find relevant
                            government schemes.
                        </p>
                    </div>

                    <form onSubmit={handleSubmit}>

                        {/* State */}
                        <div className="eligibility-form-group">
                            <label htmlFor="state">
                                State / Union Territory
                            </label>

                            <select
                                id="state"
                                name="state"
                                value={formData.state}
                                onChange={handleChange}
                                required
                            >
                                <option value="">Select your state</option>
                                <option value="andhra-pradesh">
                                    Andhra Pradesh
                                </option>
                                <option value="karnataka">
                                    Karnataka
                                </option>
                                <option value="maharashtra">
                                    Maharashtra
                                </option>
                                <option value="tamil-nadu">
                                    Tamil Nadu
                                </option>
                                <option value="delhi">
                                    Delhi
                                </option>
                                <option value="other">
                                    Other
                                </option>
                            </select>
                        </div>

                        {/* Age */}
                        <div className="eligibility-form-group">
                            <label htmlFor="age">
                                Age Group
                            </label>

                            <select
                                id="age"
                                name="age"
                                value={formData.age}
                                onChange={handleChange}
                                required
                            >
                                <option value="">
                                    Select your age group
                                </option>
                                <option value="below-18">
                                    Below 18
                                </option>
                                <option value="18-25">
                                    18 - 25
                                </option>
                                <option value="26-40">
                                    26 - 40
                                </option>
                                <option value="41-60">
                                    41 - 60
                                </option>
                                <option value="above-60">
                                    Above 60
                                </option>
                            </select>
                        </div>

                        {/* Occupation */}
                        <div className="eligibility-form-group">
                            <label htmlFor="occupation">
                                Occupation / Status
                            </label>

                            <select
                                id="occupation"
                                name="occupation"
                                value={formData.occupation}
                                onChange={handleChange}
                                required
                            >
                                <option value="">
                                    Select your occupation
                                </option>
                                <option value="farmer">
                                    Farmer
                                </option>
                                <option value="student">
                                    Student
                                </option>
                                <option value="entrepreneur">
                                    Entrepreneur
                                </option>
                                <option value="business-owner">
                                    Small Business Owner
                                </option>
                                <option value="other">
                                    Other
                                </option>
                            </select>
                        </div>

                        {/* Social Category */}
                        <div className="eligibility-form-group">
                            <label htmlFor="category">
                                Social Category
                            </label>

                            <select
                                id="category"
                                name="category"
                                value={formData.category}
                                onChange={handleChange}
                                required
                            >
                                <option value="">
                                    Select your category
                                </option>
                                <option value="general">
                                    General
                                </option>
                                <option value="obc">
                                    OBC
                                </option>
                                <option value="sc">
                                    SC
                                </option>
                                <option value="st">
                                    ST
                                </option>
                                <option value="ews">
                                    EWS
                                </option>
                            </select>
                        </div>

                        {/* Income */}
                        <div className="eligibility-form-group">
                            <label htmlFor="income">
                                Annual Family Income
                            </label>

                            <select
                                id="income"
                                name="income"
                                value={formData.income}
                                onChange={handleChange}
                                required
                            >
                                <option value="">
                                    Select annual income
                                </option>
                                <option value="below-1">
                                    Below ₹1 Lakh
                                </option>
                                <option value="1-3">
                                    ₹1 - ₹3 Lakhs
                                </option>
                                <option value="3-5">
                                    ₹3 - ₹5 Lakhs
                                </option>
                                <option value="5-10">
                                    ₹5 - ₹10 Lakhs
                                </option>
                                <option value="above-10">
                                    Above ₹10 Lakhs
                                </option>
                            </select>
                        </div>

                        <button
                            type="submit"
                            className="eligibility-submit"
                        >
                            Find Eligible Schemes
                        </button>

                    </form>
                </div>

                {/* Results */}
                {submitted && (
                    <section className="eligibility-results">

                        <div className="results-heading">
                            <h2>Eligible Schemes</h2>

                            {results.length > 0 ? (
                                <p>
                                    Based on the information provided,
                                    you may be eligible for the following schemes.
                                </p>
                            ) : (
                                <p>
                                    No matching schemes were found based on
                                    the information provided.
                                </p>
                            )}
                        </div>

                        <div className="eligibility-results-grid">

                            {results.map((scheme) => (
                                <div
                                    className="eligibility-scheme-card"
                                    key={scheme.id}
                                >
                                    <div className="result-card-header">
                                        <span className="result-category">
                                            {scheme.category}
                                        </span>

                                        <span className="result-target">
                                            {scheme.target}
                                        </span>
                                    </div>

                                    <h3>{scheme.name}</h3>

                                    <p>
                                        {scheme.description}
                                    </p>

                                    <Link
                                        to={scheme.path}
                                        className="result-button"
                                    >
                                        View Details
                                    </Link>
                                </div>
                            ))}

                        </div>

                    </section>
                )}

            </main>
        </div>
    );
}

export default Eligibility;