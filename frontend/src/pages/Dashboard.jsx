import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import '../App.css';
import './Dashboard.css';

function Dashboard({ onLogout }) {

    const navigate = useNavigate();

    const [beneficiary, setBeneficiary] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {

        const beneficiaryId = localStorage.getItem('beneficiaryId');
        const isLoggedIn = localStorage.getItem('isLoggedIn');

        // If user is not logged in, send them to Login
        if (!beneficiaryId || isLoggedIn !== 'true') {
            navigate('/login');
            return;
        }

        const fetchBeneficiary = async () => {

            try {

                const response = await fetch(
                    `http://localhost:8080/api/v1/beneficiaries/${beneficiaryId}`
                );

                if (!response.ok) {
                    throw new Error('Unable to fetch beneficiary details');
                }

                const data = await response.json();

                setBeneficiary(data);

            } catch (error) {

                console.error('Dashboard error:', error);

            } finally {

                setLoading(false);

            }
        };

        fetchBeneficiary();

    }, [navigate]);


    const handleLogout = () => {

        localStorage.removeItem('beneficiaryId');
        localStorage.removeItem('beneficiaryName');
        localStorage.removeItem('isLoggedIn');

        if (onLogout) {
            onLogout();
        }

        navigate('/login');
    };


    if (loading) {
        return (
            <div className="dashboard-page">

                <div className="dashboard-loading">
                    Loading your dashboard...
                </div>

            </div>
        );
    }


    return (

        <div className="dashboard-page">

            {/* ================= HEADER ================= */}

            <header className="dashboard-header">

                <div className="dashboard-header-container">

                    <Link
                        to="/"
                        className="dashboard-brand"
                    >
                        <h1>DSGP</h1>

                        <p>
                            Digital Subsidy & Grant Platform
                        </p>
                    </Link>


                    <div className="dashboard-header-actions">

                        <Link
                            to="/"
                            className="dashboard-home-link"
                        >
                            Home
                        </Link>

                        <button
                            className="dashboard-logout-button"
                            onClick={handleLogout}
                        >
                            Logout
                        </button>

                    </div>

                </div>

            </header>


            {/* ================= MAIN ================= */}

            <main className="dashboard-main">

                <div className="dashboard-container">


                    {/* ================= WELCOME ================= */}

                    <section className="dashboard-welcome">

                        <div>

                            <p className="dashboard-welcome-label">
                                Welcome back,
                            </p>

                            <h2>
                                {beneficiary?.fullName ||
                                    localStorage.getItem('beneficiaryName') ||
                                    'Beneficiary'}
                            </h2>

                            <p>
                                Manage your profile, applications and
                                government scheme information from here.
                            </p>

                        </div>

                    </section>


                    {/* ================= QUICK ACTIONS ================= */}

                    <section className="dashboard-section">

                        <div className="dashboard-section-header">

                            <h2>
                                Quick Actions
                            </h2>

                            <p>
                                Access the most important DSGP services
                            </p>

                        </div>


                        <div className="dashboard-actions-grid">


                            {/* PROFILE */}

                            <Link
                                to="/profile"
                                className="dashboard-action-card"
                            >

                                <div className="dashboard-action-icon">
                                    👤
                                </div>

                                <h3>
                                    My Profile
                                </h3>

                                <p>
                                    View and manage your personal information
                                </p>

                                <span>
                                    View Profile →
                                </span>

                            </Link>


                            {/* SCHEMES */}

                            <Link
                                to="/schemes"
                                className="dashboard-action-card"
                            >

                                <div className="dashboard-action-icon">
                                    📋
                                </div>

                                <h3>
                                    Browse Schemes
                                </h3>

                                <p>
                                    Explore available government subsidy
                                    and grant schemes
                                </p>

                                <span>
                                    Explore Schemes →
                                </span>

                            </Link>


                            {/* ELIGIBILITY */}

                            <Link
                                to="/eligibility"
                                className="dashboard-action-card"
                            >

                                <div className="dashboard-action-icon">
                                    ✓
                                </div>

                                <h3>
                                    Check Eligibility
                                </h3>

                                <p>
                                    Find government schemes you may be
                                    eligible for
                                </p>

                                <span>
                                    Check Eligibility →
                                </span>

                            </Link>


                            {/* TRACK */}

                            <Link
                                to="/track"
                                className="dashboard-action-card"
                            >

                                <div className="dashboard-action-icon">
                                    🔍
                                </div>

                                <h3>
                                    Track Application
                                </h3>

                                <p>
                                    Check the current status of your
                                    submitted application
                                </p>

                                <span>
                                    Track Status →
                                </span>

                            </Link>

                        </div>

                    </section>


                    {/* ================= PROFILE SUMMARY ================= */}

                    <section className="dashboard-section">

                        <div className="dashboard-section-header">

                            <h2>
                                Profile Summary
                            </h2>

                            <p>
                                Your registered information
                            </p>

                        </div>


                        <div className="dashboard-profile-card">

                            <div className="dashboard-profile-row">

                                <span>
                                    Full Name
                                </span>

                                <strong>
                                    {beneficiary?.fullName || '—'}
                                </strong>

                            </div>


                            <div className="dashboard-profile-row">

                                <span>
                                    Government ID
                                </span>

                                <strong>
                                    {beneficiary?.govId || '—'}
                                </strong>

                            </div>


                            <div className="dashboard-profile-row">

                                <span>
                                    Email
                                </span>

                                <strong>
                                    {beneficiary?.email || '—'}
                                </strong>

                            </div>


                            <div className="dashboard-profile-row">

                                <span>
                                    Contact
                                </span>

                                <strong>
                                    {beneficiary?.contact || '—'}
                                </strong>

                            </div>


                            <div className="dashboard-profile-row">

                                <span>
                                    Age
                                </span>

                                <strong>
                                    {beneficiary?.age || '—'}
                                </strong>

                            </div>


                            <div className="dashboard-profile-row">

                                <span>
                                    Address
                                </span>

                                <strong>
                                    {beneficiary?.address || '—'}
                                </strong>

                            </div>


                            <div className="dashboard-profile-row">

                                <span>
                                    Registered Scheme
                                </span>

                                <strong>
                                    {beneficiary?.schemeName || '—'}
                                </strong>

                            </div>


                            <div className="dashboard-profile-button-container">

                                <Link
                                    to="/profile"
                                    className="dashboard-profile-button"
                                >
                                    View Full Profile
                                </Link>

                            </div>

                        </div>

                    </section>


                    {/* ================= INFORMATION ================= */}

                    <section className="dashboard-info">

                        <h3>
                            What's next?
                        </h3>

                        <p>
                            Explore available schemes and check your
                            eligibility before submitting an application.
                            Once you apply, you can use your Application ID
                            to track its progress.
                        </p>

                        <div className="dashboard-info-actions">

                            <Link
                                to="/schemes"
                                className="dashboard-primary-button"
                            >
                                Explore Schemes
                            </Link>

                            <Link
                                to="/eligibility"
                                className="dashboard-secondary-button"
                            >
                                Check Eligibility
                            </Link>

                        </div>

                    </section>

                </div>

            </main>


            {/* ================= FOOTER ================= */}

            <footer className="dashboard-footer">

                <p>
                    © 2024 Digital Subsidy & Grant Platform (DSGP)
                    - Academic Project
                </p>

            </footer>

        </div>

    );
}

export default Dashboard;