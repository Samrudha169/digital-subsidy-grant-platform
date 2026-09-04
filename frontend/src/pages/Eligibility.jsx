import React, { useState } from "react";
import "./Eligibility.css";

const Eligibility = () => {
    const [formData, setFormData] = useState({
        state: "",
        ageGroup: "",
        occupation: "",
        category: "",
        incomeGroup: "",
    });

    const [schemes, setSchemes] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [searched, setSearched] = useState(false);

    // States list
    const states = [
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
        "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
        "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
        "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu",
        "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal",
        "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
    ];

    // Age Groups
    const ageGroups = [
        { label: "Below 18", value: 16 },
        { label: "18 - 25", value: 22 },
        { label: "26 - 35", value: 30 },
        { label: "36 - 45", value: 40 },
        { label: "46 - 60", value: 50 },
        { label: "Above 60", value: 65 }
    ];

    // Updated exact 5 Occupation Options
    const occupations = [
        "Farmer",
        "Student",
        "Entrepreneur",
        "Small Business Owner",
        "Other"
    ];

    // Social Categories
    const categories = ["GENERAL", "OBC", "SC", "ST"];

    // Income Groups
    const incomeGroups = [
        { label: "Below ₹1 Lakh", value: 50000 },
        { label: "₹1 - ₹3 Lakhs", value: 200000 },
        { label: "₹3 - ₹5 Lakhs", value: 400000 },
        { label: "₹5 - ₹8 Lakhs", value: 650000 },
        { label: "Above ₹8 Lakhs", value: 1000000 }
    ];

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const handleReset = () => {
        setFormData({
            state: "",
            ageGroup: "",
            occupation: "",
            category: "",
            incomeGroup: "",
        });
        setSchemes([]);
        setError("");
        setSearched(false);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError("");
        setSearched(true);

        // Map UI values to expected backend numbers
        const selectedAgeGroup = ageGroups.find((g) => g.label === formData.ageGroup);
        const selectedIncomeGroup = incomeGroups.find((g) => g.label === formData.incomeGroup);

        const payload = {
            state: formData.state,
            age: selectedAgeGroup ? selectedAgeGroup.value : null,
            occupation: formData.occupation,
            category: formData.category,
            annualIncome: selectedIncomeGroup ? selectedIncomeGroup.value : null,
        };

        try {
            const response = await fetch("http://localhost:8080/api/v1/eligibility/check", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                throw new Error("Failed to fetch eligible schemes. Server error.");
            }

            const data = await response.json();
            setSchemes(data);
        } catch (err) {
            console.error("Error fetching schemes:", err);
            setError("Unable to connect to the backend server. Please make sure Spring Boot is running.");
            setSchemes([]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="eligibility-page">
            <div className="eligibility-hero">
                <div className="eligibility-hero-content">
                    <h1>Scheme Eligibility</h1>
                    <p>Enter your details to find government schemes you may be eligible for.</p>
                </div>
            </div>

            <div className="eligibility-container">
                <div className="eligibility-card">
                    <h2 className="form-heading">Check Your Eligibility</h2>
                    <p className="form-subheading">
                        Provide your details below to discover suitable government schemes.
                    </p>

                    <form onSubmit={handleSubmit}>
                        <div className="eligibility-form-grid">
                            {/* State Select */}
                            <div className="eligibility-form-group">
                                <label>State / Union Territory</label>
                                <select
                                    name="state"
                                    value={formData.state}
                                    onChange={handleChange}
                                    required
                                >
                                    <option value="">Select State</option>
                                    {states.map((st, idx) => (
                                        <option key={idx} value={st}>
                                            {st}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {/* Age Group Select */}
                            <div className="eligibility-form-group">
                                <label>Age Group</label>
                                <select
                                    name="ageGroup"
                                    value={formData.ageGroup}
                                    onChange={handleChange}
                                    required
                                >
                                    <option value="">Select Age Group</option>
                                    {ageGroups.map((ag, idx) => (
                                        <option key={idx} value={ag.label}>
                                            {ag.label}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {/* Occupation Select */}
                            <div className="eligibility-form-group">
                                <label>Occupation / Status</label>
                                <select
                                    name="occupation"
                                    value={formData.occupation}
                                    onChange={handleChange}
                                    required
                                >
                                    <option value="">Select your occupation</option>
                                    {occupations.map((occ, idx) => (
                                        <option key={idx} value={occ}>
                                            {occ}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {/* Social Category Select */}
                            <div className="eligibility-form-group">
                                <label>Social Category</label>
                                <select
                                    name="category"
                                    value={formData.category}
                                    onChange={handleChange}
                                    required
                                >
                                    <option value="">Select Category</option>
                                    {categories.map((cat, idx) => (
                                        <option key={idx} value={cat}>
                                            {cat}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            {/* Annual Income Select */}
                            <div className="eligibility-form-group">
                                <label>Annual Family Income</label>
                                <select
                                    name="incomeGroup"
                                    value={formData.incomeGroup}
                                    onChange={handleChange}
                                    required
                                >
                                    <option value="">Select Income Group</option>
                                    {incomeGroups.map((inc, idx) => (
                                        <option key={idx} value={inc.label}>
                                            {inc.label}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        <div className="eligibility-actions">
                            <button
                                type="submit"
                                className="eligibility-submit"
                                disabled={loading}
                            >
                                {loading ? "Searching..." : "Find Eligible Schemes"}
                            </button>
                            <button
                                type="button"
                                className="eligibility-reset"
                                onClick={handleReset}
                            >
                                Reset
                            </button>
                        </div>
                    </form>

                    {error && <div className="eligibility-error">{error}</div>}
                </div>

                {/* Results Section */}
                {searched && (
                    <div className="eligibility-results">
                        <h3 className="results-heading">
                            {schemes.length > 0
                                ? `Eligible Schemes (${schemes.length})`
                                : "No Eligible Schemes Found"}
                        </h3>

                        {schemes.length > 0 ? (
                            <div className="eligibility-results-grid">
                                {schemes.map((scheme) => (
                                    <div key={scheme.schemeId} className="eligibility-scheme-card">
                                        <div className="result-card-header">
                                            <span className="result-category">Government Scheme</span>
                                            <span className="result-target">
                        Score: {scheme.eligibilityScore}
                      </span>
                                        </div>
                                        <h4>{scheme.schemeName}</h4>
                                        <p>{scheme.description}</p>
                                        <button className="result-button">View Details</button>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            !loading && (
                                <div className="no-schemes">
                                    <p>
                                        Based on your selections, no schemes currently match your eligibility parameters.
                                    </p>
                                </div>
                            )
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default Eligibility;