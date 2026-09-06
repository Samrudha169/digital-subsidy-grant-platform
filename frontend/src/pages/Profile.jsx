import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getBeneficiaryById } from '../services/api';
import './Profile.css';

function Profile() {
    const navigate = useNavigate();

    const [beneficiary, setBeneficiary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const beneficiaryId = localStorage.getItem('beneficiaryId');
        const isLoggedIn = localStorage.getItem('isLoggedIn');

        if (!beneficiaryId || isLoggedIn !== 'true') {
            navigate('/login');
            return;
        }

        const loadProfile = async () => {
            try {
                const data = await getBeneficiaryById(beneficiaryId);
                setBeneficiary(data);
            } catch (err) {
                console.error('Profile error:', err);
                setError('Unable to load your profile.');
            } finally {
                setLoading(false);
            }
        };

        loadProfile();
    }, [navigate]);

    /* ─────────────────────────────────────────────────────────
       LOADING
    ───────────────────────────────────────────────────────── */

    if (loading) {
        return (
            <div className="profile-page">
                <div className="profile-loading">
                    Loading your profile...
                </div>
            </div>
        );
    }

    /* ─────────────────────────────────────────────────────────
       ERROR
    ───────────────────────────────────────────────────────── */

    if (error) {
        return (
            <div className="profile-page">
                <div className="profile-error">
                    {error}
                </div>
            </div>
        );
    }

    /* ─────────────────────────────────────────────────────────
       DYNAMIC VALUES
    ───────────────────────────────────────────────────────── */

    const fullName =
        beneficiary?.fullName ||
        `${beneficiary?.firstName || ''} ${beneficiary?.lastName || ''}`.trim() ||
        'Beneficiary';

    const firstLetter =
        fullName.charAt(0).toUpperCase() || 'B';

    const email =
        beneficiary?.email || 'No email available';

    const registrationStatus =
        beneficiary?.registrationStatus || 'Registered';

    const identityVerified =
        beneficiary?.identityVerified === true;

    /* ─────────────────────────────────────────────────────────
       FORMAT HELPERS
    ───────────────────────────────────────────────────────── */

    const formatGender = (gender) => {
        if (!gender) {
            return 'Not provided';
        }

        return gender
            .toLowerCase()
            .replace(/^\w/, char => char.toUpperCase());
    };

    const formatCategory = (category) => {
        if (!category) {
            return 'Not provided';
        }

        const categoryMap = {
            GENERAL: 'General',
            OBC: 'OBC',
            SC: 'SC',
            ST: 'ST'
        };

        return categoryMap[category] || category;
    };

    const formatDate = (date) => {
        if (!date) {
            return 'Not provided';
        }

        const parsedDate = new Date(date);

        if (Number.isNaN(parsedDate.getTime())) {
            return date;
        }

        return parsedDate.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'long',
            year: 'numeric'
        });
    };

    const formatCurrency = (amount) => {
        if (
            amount === null ||
            amount === undefined ||
            amount === ''
        ) {
            return 'Not provided';
        }

        const numericAmount = Number(amount);

        if (Number.isNaN(numericAmount)) {
            return amount;
        }

        return `₹${numericAmount.toLocaleString('en-IN')}`;
    };

    const formatLandHolding = (landHolding) => {
        if (
            landHolding === null ||
            landHolding === undefined ||
            landHolding === ''
        ) {
            return 'Not provided';
        }

        return `${landHolding} acres`;
    };

    const maskAadhaar = (aadhaar) => {
        if (!aadhaar) {
            return 'Not provided';
        }

        const value = String(aadhaar);

        if (value.length < 4) {
            return value;
        }

        return `XXXX-XXXX-${value.slice(-4)}`;
    };

    /* ─────────────────────────────────────────────────────────
       PROFILE COMPLETION
    ───────────────────────────────────────────────────────── */

    const profileFields = [
        beneficiary?.fullName,
        beneficiary?.firstName,
        beneficiary?.lastName,
        beneficiary?.dateOfBirth,
        beneficiary?.age,
        beneficiary?.gender,
        beneficiary?.email,
        beneficiary?.mobileNumber,
        beneficiary?.govId,
        beneficiary?.aadhaarNumber,
        beneficiary?.address,
        beneficiary?.village,
        beneficiary?.taluka,
        beneficiary?.district,
        beneficiary?.state,
        beneficiary?.pinCode,
        beneficiary?.annualIncome,
        beneficiary?.category,
        beneficiary?.schemeName
    ];

    const completedFields = profileFields.filter(
        field =>
            field !== null &&
            field !== undefined &&
            String(field).trim() !== ''
    ).length;

    const profileCompletion = Math.round(
        (completedFields / profileFields.length) * 100
    );

    return (
        <div className="profile-page">

            {/* Header */}
            <header className="profile-header">
                <div className="profile-header-container">

                    <Link
                        to="/dashboard"
                        className="profile-brand"
                    >
                        <h1>DSGP</h1>

                        <p>
                            Digital Subsidy &amp; Grant Platform
                        </p>
                    </Link>

                    <Link
                        to="/dashboard"
                        className="profile-back-link"
                    >
                        Back to Dashboard
                    </Link>

                </div>
            </header>


            {/* Main */}
            <main className="profile-main">

                <div className="profile-container">

                    {/* Title */}
                    <section className="profile-title-section">

                        <h2>
                            My Profile
                        </h2>

                        <p>
                            View your registered personal and beneficiary
                            information.
                        </p>

                    </section>


                    {/* Profile Card */}
                    <section className="profile-card">

                        {/* ══════════════════════════════════════
                            PROFILE HEADER
                        ══════════════════════════════════════ */}

                        <div className="profile-card-header">

                            <div className="profile-avatar">
                                {firstLetter}
                            </div>

                            <div>

                                <h3>
                                    {fullName}
                                </h3>

                                <p>
                                    {email}
                                </p>

                            </div>

                        </div>


                        {/* ══════════════════════════════════════
                            PROFILE COMPLETION
                        ══════════════════════════════════════ */}

                        <div className="profile-completion">

                            <div className="profile-completion-header">

                                <span>
                                    Profile Completion
                                </span>

                                <strong>
                                    {profileCompletion}%
                                </strong>

                            </div>

                            <div className="profile-progress">

                                <div
                                    className="profile-progress-bar"
                                    style={{
                                        width: `${profileCompletion}%`
                                    }}
                                />

                            </div>

                            <p>
                                {profileCompletion === 100
                                    ? 'Your profile is complete.'
                                    : 'Some profile information is still incomplete.'
                                }
                            </p>

                        </div>


                        {/* ══════════════════════════════════════
                            PERSONAL INFORMATION
                        ══════════════════════════════════════ */}

                        <div className="profile-section">

                            <h3>
                                Personal Information
                            </h3>

                            <div className="profile-grid">

                                <ProfileField
                                    label="Full Name"
                                    value={beneficiary?.fullName}
                                />

                                <ProfileField
                                    label="First Name"
                                    value={beneficiary?.firstName}
                                />

                                <ProfileField
                                    label="Last Name"
                                    value={beneficiary?.lastName}
                                />

                                <ProfileField
                                    label="Date of Birth"
                                    value={formatDate(
                                        beneficiary?.dateOfBirth
                                    )}
                                />

                                <ProfileField
                                    label="Age"
                                    value={beneficiary?.age}
                                />

                                <ProfileField
                                    label="Gender"
                                    value={formatGender(
                                        beneficiary?.gender
                                    )}
                                />

                            </div>

                        </div>


                        {/* ══════════════════════════════════════
                            CONTACT & IDENTITY
                        ══════════════════════════════════════ */}

                        <div className="profile-section">

                            <h3>
                                Contact &amp; Identity
                            </h3>

                            <div className="profile-grid">

                                <ProfileField
                                    label="Email"
                                    value={beneficiary?.email}
                                />

                                <ProfileField
                                    label="Mobile Number"
                                    value={beneficiary?.mobileNumber}
                                />

                                <ProfileField
                                    label="Contact"
                                    value={beneficiary?.contact}
                                />

                                <ProfileField
                                    label="Government ID"
                                    value={beneficiary?.govId}
                                />

                                <ProfileField
                                    label="Aadhaar Number"
                                    value={maskAadhaar(
                                        beneficiary?.aadhaarNumber
                                    )}
                                />

                            </div>

                        </div>


                        {/* ══════════════════════════════════════
                            ADDRESS
                        ══════════════════════════════════════ */}

                        <div className="profile-section">

                            <h3>
                                Address
                            </h3>

                            <div className="profile-grid">

                                <ProfileField
                                    label="Address"
                                    value={beneficiary?.address}
                                />

                                <ProfileField
                                    label="Village"
                                    value={beneficiary?.village}
                                />

                                <ProfileField
                                    label="Taluka"
                                    value={beneficiary?.taluka}
                                />

                                <ProfileField
                                    label="District"
                                    value={beneficiary?.district}
                                />

                                <ProfileField
                                    label="State"
                                    value={beneficiary?.state}
                                />

                                <ProfileField
                                    label="PIN Code"
                                    value={beneficiary?.pinCode}
                                />

                            </div>

                        </div>


                        {/* ══════════════════════════════════════
                            FINANCIAL & SCHEME
                        ══════════════════════════════════════ */}

                        <div className="profile-section">

                            <h3>
                                Financial &amp; Scheme Information
                            </h3>

                            <div className="profile-grid">

                                <ProfileField
                                    label="Annual Income"
                                    value={formatCurrency(
                                        beneficiary?.annualIncome
                                    )}
                                />

                                <ProfileField
                                    label="Land Holding"
                                    value={formatLandHolding(
                                        beneficiary?.landHolding
                                    )}
                                />

                                <ProfileField
                                    label="Category"
                                    value={formatCategory(
                                        beneficiary?.category
                                    )}
                                />

                                <ProfileField
                                    label="Scheme"
                                    value={beneficiary?.schemeName}
                                />

                            </div>

                        </div>


                        {/* ══════════════════════════════════════
                            ACCOUNT STATUS
                        ══════════════════════════════════════ */}

                        <div className="profile-section">

                            <h3>
                                Account Status
                            </h3>

                            <div className="profile-grid">

                                <ProfileField
                                    label="Registration Status"
                                    value={registrationStatus}
                                />

                                <ProfileField
                                    label="Identity Verified"
                                    value={
                                        identityVerified
                                            ? '✓ Verified'
                                            : 'Not Verified'
                                    }
                                />

                                <ProfileField
                                    label="Beneficiary ID"
                                    value={beneficiary?.id}
                                />

                            </div>

                        </div>

                    </section>

                </div>

            </main>

        </div>
    );
}


/* ══════════════════════════════════════════════════════════════
   PROFILE FIELD
══════════════════════════════════════════════════════════════ */

function ProfileField({ label, value }) {

    return (
        <div className="profile-field">

            <span className="profile-field-label">
                {label}
            </span>

            <span className="profile-field-value">
                {value !== null &&
                value !== undefined &&
                value !== ''
                    ? value
                    : 'Not provided'}
            </span>

        </div>
    );
}

export default Profile;