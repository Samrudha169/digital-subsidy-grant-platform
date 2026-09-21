import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getBeneficiaryById } from '../services/api';
import './Profile.css';

const API_BASE = '/api/v1';

function Profile() {
    const navigate = useNavigate();

    const [beneficiary, setBeneficiary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [isEditing, setIsEditing] = useState(false);
    const [editForm, setEditForm] = useState({});

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

    const startEditing = () => {
        setSuccess('');
        setError('');
        setEditForm({
            age: beneficiary?.age ?? '',
            annualIncome: beneficiary?.annualIncome ?? '',
            landHolding: beneficiary?.landHolding ?? '',
            address: beneficiary?.address ?? '',
            village: beneficiary?.village ?? '',
            taluka: beneficiary?.taluka ?? '',
            district: beneficiary?.district ?? '',
            state: beneficiary?.state ?? '',
            pinCode: beneficiary?.pinCode ?? '',
            occupation: beneficiary?.occupation ?? '',
            category: beneficiary?.category ?? '',
        });
        setIsEditing(true);
    };

    const cancelEditing = () => {
        setIsEditing(false);
        setEditForm({});
        setError('');
    };

    const handleChange = (event) => {
        const { name, value } = event.target;
        setEditForm(previous => ({
            ...previous,
            [name]: value,
        }));
    };

    const saveProfile = async (event) => {
        event.preventDefault();
        setSaving(true);
        setError('');
        setSuccess('');

        const beneficiaryId = localStorage.getItem('beneficiaryId');

        const payload = {
            age: editForm.age === '' ? null : Number(editForm.age),

            annualIncome:
                editForm.annualIncome === ''
                    ? null
                    : Number(editForm.annualIncome),

            landHolding:
                editForm.landHolding === ''
                    ? null
                    : Number(editForm.landHolding),

            address: editForm.address,
            village: editForm.village,
            taluka: editForm.taluka,
            district: editForm.district,
            state: editForm.state,
            pinCode: editForm.pinCode,

            occupation: editForm.occupation,
            category: editForm.category,
        };

        try {
            const response = await fetch(
                `${API_BASE}/beneficiaries/${beneficiaryId}`,
                {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(payload),
                }
            );

            const responseText = await response.text();
            let updatedData = null;

            try {
                updatedData = responseText ? JSON.parse(responseText) : null;
            } catch {
                updatedData = null;
            }

            if (!response.ok) {
                throw new Error(
                    updatedData?.message ||
                    responseText ||
                    'Unable to update your profile.'
                );
            }

            if (updatedData && typeof updatedData === 'object') {
                setBeneficiary(updatedData);
            } else {
                const refreshedData = await getBeneficiaryById(beneficiaryId);
                setBeneficiary(refreshedData);
            }

            setIsEditing(false);
            setEditForm({});
            setSuccess('Profile updated successfully.');
        } catch (err) {
            console.error('Profile update error:', err);
            setError(err.message || 'Unable to update your profile.');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="profile-page">
                <div className="profile-loading">Loading your profile...</div>
            </div>
        );
    }

    if (error && !beneficiary) {
        return (
            <div className="profile-page">
                <div className="profile-error">{error}</div>
            </div>
        );
    }

    const fullName = beneficiary?.fullName || 'Beneficiary';
    const firstLetter = fullName.charAt(0).toUpperCase() || 'B';
    const email = beneficiary?.email || 'No email available';
    const registrationStatus = beneficiary?.registrationStatus || 'Registered';
    const identityVerified = beneficiary?.identityVerified === true;

    const formatGender = (gender) => {
        if (!gender) return 'Not provided';
        return gender
            .toLowerCase()
            .replace(/^\w/, char => char.toUpperCase());
    };

    const formatCategory = (category) => {
        if (!category) return 'Not provided';
        const categoryMap = {
            GENERAL: 'General',
            OBC: 'OBC',
            SC: 'SC',
            ST: 'ST',
        };
        return categoryMap[category] || category;
    };

    const formatDate = (date) => {
        if (!date) return 'Not provided';
        const parsedDate = new Date(date);
        if (Number.isNaN(parsedDate.getTime())) return date;
        return parsedDate.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'long',
            year: 'numeric',
        });
    };

    const formatCurrency = (amount) => {
        if (amount === null || amount === undefined || amount === '') {
            return 'Not provided';
        }
        const numericAmount = Number(amount);
        if (Number.isNaN(numericAmount)) return amount;
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
        if (!aadhaar) return 'Not provided';
        const value = String(aadhaar);
        if (value.length < 4) return value;
        return `XXXX-XXXX-${value.slice(-4)}`;
    };

    const profileFields = [
        beneficiary?.fullName,
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
        beneficiary?.landHolding,
        beneficiary?.occupation,
        beneficiary?.category,
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

    const editableField = (label, name, type = 'text', options = null) => (
        <div className="profile-edit-field" key={name}>
            <label htmlFor={name}>{label}</label>
            {options ? (
                <select
                    id={name}
                    name={name}
                    value={editForm[name] ?? ''}
                    onChange={handleChange}
                >
                    <option value="">Select {label}</option>
                    {options.map(option => (
                        <option value={option} key={option}>
                            {option}
                        </option>
                    ))}
                </select>
            ) : (
                <input
                    id={name}
                    name={name}
                    type={type}
                    value={editForm[name] ?? ''}
                    onChange={handleChange}
                    min={name === 'age' ? 1 : undefined}
                    max={name === 'age' ? 120 : undefined}
                    minLength={name === 'pinCode' ? 6 : undefined}
                    maxLength={name === 'pinCode' ? 6 : undefined}
                />
            )}
        </div>
    );

    return (
        <div className="profile-page">
            <header className="profile-header">
                <div className="profile-header-container">
                    <Link to="/dashboard" className="profile-brand">
                        <h1>DSGP</h1>
                        <p>Digital Subsidy &amp; Grant Platform</p>
                    </Link>

                    <Link to="/dashboard" className="profile-back-link">
                        Back to Dashboard
                    </Link>
                </div>
            </header>

            <main className="profile-main">
                <div className="profile-container">
                    <section className="profile-title-section">
                        <h2>My Profile</h2>
                        <p>View and update your registered beneficiary information.</p>
                    </section>

                    {success && <div className="profile-success">{success}</div>}
                    {error && beneficiary && <div className="profile-error">{error}</div>}

                    <section className="profile-card">
                        <div className="profile-card-header">
                            <div className="profile-avatar">{firstLetter}</div>
                            <div>
                                <h3>{fullName}</h3>
                                <p>{email}</p>
                            </div>
                        </div>

                        <div className="profile-completion">
                            <div className="profile-completion-header">
                                <span>Profile Completion</span>
                                <strong>{profileCompletion}%</strong>
                            </div>
                            <div className="profile-progress">
                                <div
                                    className="profile-progress-bar"
                                    style={{ width: `${profileCompletion}%` }}
                                />
                            </div>
                            <p>
                                {profileCompletion === 100
                                    ? 'Your profile is complete.'
                                    : 'Some profile information is still incomplete.'}
                            </p>
                        </div>

                        {!isEditing ? (
                            <>
                                <div className="profile-edit-toolbar">
                                    <button
                                        type="button"
                                        className="profile-edit-button"
                                        onClick={startEditing}
                                    >
                                        ✏️ Edit Profile
                                    </button>
                                </div>

                                <div className="profile-section">
                                    <h3>Personal Information</h3>
                                    <div className="profile-grid">
                                        <ProfileField label="Full Name" value={beneficiary?.fullName} />
                                        <ProfileField label="Date of Birth" value={formatDate(beneficiary?.dateOfBirth)} />
                                        <ProfileField label="Age" value={beneficiary?.age} />
                                        <ProfileField label="Gender" value={formatGender(beneficiary?.gender)} />
                                    </div>
                                </div>

                                <div className="profile-section">
                                    <h3>Contact &amp; Identity</h3>
                                    <div className="profile-grid">
                                        <ProfileField label="Email" value={beneficiary?.email} />
                                        <ProfileField label="Mobile Number" value={beneficiary?.mobileNumber} />
                                        <ProfileField label="Contact" value={beneficiary?.contact} />
                                        <ProfileField label="Government ID" value={beneficiary?.govId} />
                                        <ProfileField label="Aadhaar Number" value={maskAadhaar(beneficiary?.aadhaarNumber)} />
                                    </div>
                                </div>

                                <div className="profile-section">
                                    <h3>Address</h3>
                                    <div className="profile-grid">
                                        <ProfileField label="Address" value={beneficiary?.address} />
                                        <ProfileField label="Village" value={beneficiary?.village} />
                                        <ProfileField label="Taluka" value={beneficiary?.taluka} />
                                        <ProfileField label="District" value={beneficiary?.district} />
                                        <ProfileField label="State" value={beneficiary?.state} />
                                        <ProfileField label="PIN Code" value={beneficiary?.pinCode} />
                                    </div>
                                </div>

                                <div className="profile-section">
                                    <h3>Financial &amp; Scheme Information</h3>
                                    <div className="profile-grid">
                                        <ProfileField label="Annual Income" value={formatCurrency(beneficiary?.annualIncome)} />
                                        <ProfileField label="Land Holding" value={formatLandHolding(beneficiary?.landHolding)} />
                                        <ProfileField label="Occupation" value={beneficiary?.occupation} />
                                        <ProfileField label="Category" value={formatCategory(beneficiary?.category)} />
                                        <ProfileField label="Scheme" value={beneficiary?.schemeName} />
                                    </div>
                                </div>

                                <div className="profile-section">
                                    <h3>Account Status</h3>
                                    <div className="profile-grid">
                                        <ProfileField label="Registration Status" value={registrationStatus} />
                                        <ProfileField
                                            label="Identity Verified"
                                            value={identityVerified ? '✓ Verified' : 'Not Verified'}
                                        />
                                        <ProfileField label="Beneficiary ID" value={beneficiary?.id} />
                                    </div>
                                </div>
                            </>
                        ) : (
                            <form className="profile-edit-form" onSubmit={saveProfile}>
                                <div className="profile-section">
                                    <h3>Edit Important Information</h3>
                                    <p className="profile-edit-note">
                                        Update your information before checking eligibility for another scheme.
                                    </p>
                                    <div className="profile-edit-grid">
                                        {editableField('Age', 'age', 'number')}
                                        {editableField('Annual Income', 'annualIncome', 'number')}
                                        {editableField('Land Holding (acres)', 'landHolding', 'number')}
                                        {editableField('Occupation', 'occupation', 'text', [
                                            'Farmer',
                                            'Student',
                                            'Entrepreneur',
                                            'Business Owner',
                                            'Self Employed',
                                            'Salaried',
                                            'Other',
                                        ])}
                                        {editableField('Category', 'category', 'text', [
                                            'GENERAL',
                                            'OBC',
                                            'SC',
                                            'ST',
                                        ])}
                                        {editableField('PIN Code', 'pinCode', 'text')}
                                        {editableField('Address', 'address')}
                                        {editableField('Village', 'village')}
                                        {editableField('Taluka', 'taluka')}
                                        {editableField('District', 'district')}
                                        {editableField('State', 'state')}
                                    </div>
                                </div>

                                <div className="profile-edit-actions">
                                    <button
                                        type="submit"
                                        className="profile-save-button"
                                        disabled={saving}
                                    >
                                        {saving ? 'Saving...' : 'Save Changes'}
                                    </button>
                                    <button
                                        type="button"
                                        className="profile-cancel-button"
                                        onClick={cancelEditing}
                                        disabled={saving}
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        )}
                    </section>
                </div>
            </main>
        </div>
    );
}

function ProfileField({ label, value }) {
    return (
        <div className="profile-field">
            <span className="profile-field-label">{label}</span>
            <span className="profile-field-value">
                {value !== null && value !== undefined && value !== ''
                    ? value
                    : 'Not provided'}
            </span>
        </div>
    );
}

export default Profile;
