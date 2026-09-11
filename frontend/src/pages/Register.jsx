import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Register.css';

/* ── API base ─────────────────────────────────────────────── */
const API_BASE = '/api/v1';


/* ══════════════════════════════════════════════════════════════
   BUILD PAYLOAD
══════════════════════════════════════════════════════════════ */
function buildPayload(formData) {
    const payload = {
        fullName: formData.fullName.trim(),
        govId: formData.govId.trim(),

        // Use the single mobile number for contact
        contact: formData.mobileNumber.trim(),

        email: formData.email.trim(),
        password: formData.password,
        age: parseInt(formData.age, 10),
        address: formData.address.trim(),

        // No scheme is selected during registration
        schemeName: 'Not Selected',

        aadhaarNumber: formData.aadhaarNumber.trim(),
        mobileNumber: formData.mobileNumber.trim(),
        dateOfBirth: formData.dateOfBirth,
        gender: formData.gender,
        village: formData.village,
        taluka: formData.taluka,
        district: formData.district,
        state: formData.state,
        pinCode: formData.pinCode.trim(),
        annualIncome: parseFloat(formData.annualIncome),
        category: formData.category,
        occupation: formData.occupation
    };

    // Land holding is the ONLY optional field
    if (formData.landHolding.trim()) {
        payload.landHolding = parseFloat(formData.landHolding);
    }

    return payload;
}


/* ══════════════════════════════════════════════════════════════
   CLIENT-SIDE VALIDATION
══════════════════════════════════════════════════════════════ */
function validate(formData) {
    const errors = [];

    if (!formData.fullName.trim()) {
        errors.push('Full name is required.');
    }

    if (!formData.govId.trim()) {
        errors.push('Government ID is required.');
    }

    if (!/^[6-9]\d{9}$/.test(formData.mobileNumber.trim())) {
        errors.push(
            'Mobile number must be a valid 10-digit Indian number starting with 6–9.'
        );
    }

    if (!formData.email.trim() || !/\S+@\S+\.\S+/.test(formData.email)) {
        errors.push('A valid email address is required.');
    }

    if (!formData.password || formData.password.length < 6) {
        errors.push('Password must be at least 6 characters.');
    }

    const age = parseInt(formData.age, 10);

    if (!formData.age || isNaN(age) || age < 1 || age > 120) {
        errors.push('Age must be a number between 1 and 120.');
    }

    if (!formData.address.trim()) {
        errors.push('Address is required.');
    }

    if (!/^\d{12}$/.test(formData.aadhaarNumber.trim())) {
        errors.push('Aadhaar number must be exactly 12 digits.');
    }

    if (!formData.dateOfBirth) {
        errors.push('Date of birth is required.');
    }

    if (!formData.gender) {
        errors.push('Gender is required.');
    }

    if (!formData.category) {
        errors.push('Social category is required.');
    }

    if (!formData.village) {
        errors.push('Village is required.');
    }

    if (!formData.taluka) {
        errors.push('Taluka is required.');
    }

    if (!formData.district) {
        errors.push('District is required.');
    }

    if (!formData.state) {
        errors.push('State is required.');
    }

    if (!/^\d{6}$/.test(formData.pinCode.trim())) {
        errors.push('PIN code must be exactly 6 digits.');
    }

    if (
        !formData.annualIncome ||
        isNaN(parseFloat(formData.annualIncome)) ||
        parseFloat(formData.annualIncome) < 0
    ) {
        errors.push('Annual income is required and must be a valid amount.');
    }

    // Land holding is optional
    if (
        formData.landHolding.trim() &&
        (
            isNaN(parseFloat(formData.landHolding)) ||
            parseFloat(formData.landHolding) < 0
        )
    ) {
        errors.push('Land holding must be a valid positive number.');
    }


    if (!formData.occupation) {
        errors.push('Occupation is required.');
    }

    if (!formData.terms) {
        errors.push('You must agree to the Terms of Service.');
    }

    return errors;
}


/* ══════════════════════════════════════════════════════════════
   MAIN COMPONENT
══════════════════════════════════════════════════════════════ */
function Register() {

    const navigate = useNavigate();

    const [formData, setFormData] = useState({

        // Required basic fields
        fullName: '',
        govId: '',
        email: '',
        password: '',
        age: '',
        address: '',

        // Required identity fields
        aadhaarNumber: '',
        mobileNumber: '',
        dateOfBirth: '',
        gender: '',
        category: '',

        // Required address fields
        village: '',
        taluka: '',
        district: '',
        state: '',
        pinCode: '',

        // Occupation
        occupation: '',

// Required financial field
        annualIncome: '',

        // ONLY OPTIONAL FIELD
        landHolding: '',

        // Terms
        terms: false
    });

    /* ── Location dropdown data ───────────────────────────── */
    const [states, setStates] = useState([]);
    const [districts, setDistricts] = useState([]);
    const [talukas, setTalukas] = useState([]);
    const [villages, setVillages] = useState([]);

    const [loading, setLoading] = useState(false);
    const [clientErrors, setClientErrors] = useState([]);
    const [serverError, setServerError] = useState('');

    const [locationLoading, setLocationLoading] = useState({
        states: false,
        districts: false,
        talukas: false,
        villages: false
    });


    /* ══════════════════════════════════════════════════════════
       LOAD STATES
    ══════════════════════════════════════════════════════════ */
    useEffect(() => {

        const loadStates = async () => {

            setLocationLoading(prev => ({
                ...prev,
                states: true
            }));

            try {

                const res = await fetch(
                    `${API_BASE}/locations/states`
                );

                if (!res.ok) {
                    throw new Error('Failed to load states.');
                }

                const data = await res.json();

                setStates(data);

            } catch (error) {

                console.error('Error loading states:', error);

                setServerError(
                    'Could not load states. Make sure the backend is running.'
                );

            } finally {

                setLocationLoading(prev => ({
                    ...prev,
                    states: false
                }));

            }
        };

        loadStates();

    }, []);


    /* ══════════════════════════════════════════════════════════
       LOAD DISTRICTS WHEN STATE CHANGES
    ══════════════════════════════════════════════════════════ */
    useEffect(() => {

        if (!formData.state) {
            setDistricts([]);
            setTalukas([]);
            setVillages([]);
            return;
        }

        const selectedState = states.find(
            state => state.name === formData.state
        );

        if (!selectedState) {
            return;
        }

        const loadDistricts = async () => {

            setLocationLoading(prev => ({
                ...prev,
                districts: true
            }));

            try {

                const res = await fetch(
                    `${API_BASE}/locations/districts/${selectedState.id}`
                );

                if (!res.ok) {
                    throw new Error('Failed to load districts.');
                }

                const data = await res.json();

                setDistricts(data);

            } catch (error) {

                console.error('Error loading districts:', error);

                setDistricts([]);

                setServerError(
                    'Could not load districts.'
                );

            } finally {

                setLocationLoading(prev => ({
                    ...prev,
                    districts: false
                }));

            }
        };

        loadDistricts();

    }, [formData.state, states]);


    /* ══════════════════════════════════════════════════════════
       LOAD TALUKAS WHEN DISTRICT CHANGES
    ══════════════════════════════════════════════════════════ */
    useEffect(() => {

        if (!formData.district) {
            setTalukas([]);
            setVillages([]);
            return;
        }

        const selectedDistrict = districts.find(
            district => district.name === formData.district
        );

        if (!selectedDistrict) {
            return;
        }

        const loadTalukas = async () => {

            setLocationLoading(prev => ({
                ...prev,
                talukas: true
            }));

            try {

                const res = await fetch(
                    `${API_BASE}/locations/talukas/${selectedDistrict.id}`
                );

                if (!res.ok) {
                    throw new Error('Failed to load talukas.');
                }

                const data = await res.json();

                setTalukas(data);

            } catch (error) {

                console.error('Error loading talukas:', error);

                setTalukas([]);

                setServerError(
                    'Could not load talukas.'
                );

            } finally {

                setLocationLoading(prev => ({
                    ...prev,
                    talukas: false
                }));

            }
        };

        loadTalukas();

    }, [formData.district, districts]);


    /* ══════════════════════════════════════════════════════════
       LOAD VILLAGES WHEN TALUKA CHANGES
    ══════════════════════════════════════════════════════════ */
    useEffect(() => {

        if (!formData.taluka) {
            setVillages([]);
            return;
        }

        const selectedTaluka = talukas.find(
            taluka => taluka.name === formData.taluka
        );

        if (!selectedTaluka) {
            return;
        }

        const loadVillages = async () => {

            setLocationLoading(prev => ({
                ...prev,
                villages: true
            }));

            try {

                const res = await fetch(
                    `${API_BASE}/locations/villages/${selectedTaluka.id}`
                );

                if (!res.ok) {
                    throw new Error('Failed to load villages.');
                }

                const data = await res.json();

                setVillages(data);

            } catch (error) {

                console.error('Error loading villages:', error);

                setVillages([]);

                setServerError(
                    'Could not load villages.'
                );

            } finally {

                setLocationLoading(prev => ({
                    ...prev,
                    villages: false
                }));

            }
        };

        loadVillages();

    }, [formData.taluka, talukas]);


    /* ══════════════════════════════════════════════════════════
       FIELD CHANGE HANDLER
    ══════════════════════════════════════════════════════════ */
    const handleChange = (e) => {

        const { name, value, type, checked } = e.target;

        /*
         * When a parent location changes, clear all
         * dependent locations.
         */
        if (name === 'state') {

            setFormData(prev => ({
                ...prev,
                state: value,
                district: '',
                taluka: '',
                village: ''
            }));

        } else if (name === 'district') {

            setFormData(prev => ({
                ...prev,
                district: value,
                taluka: '',
                village: ''
            }));

        } else if (name === 'taluka') {

            setFormData(prev => ({
                ...prev,
                taluka: value,
                village: ''
            }));

        } else {

            setFormData(prev => ({
                ...prev,
                [name]: type === 'checkbox' ? checked : value
            }));

        }

        if (clientErrors.length) {
            setClientErrors([]);
        }

        if (serverError) {
            setServerError('');
        }
    };


    /* ══════════════════════════════════════════════════════════
       SUBMIT HANDLER
    ══════════════════════════════════════════════════════════════ */
    const handleSubmit = async (e) => {

        e.preventDefault();

        setServerError('');

        const errors = validate(formData);

        if (errors.length > 0) {
            setClientErrors(errors);
            return;
        }

        setClientErrors([]);
        setLoading(true);

        try {

            const payload = buildPayload(formData);

            const res = await fetch(
                `${API_BASE}/beneficiaries`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(payload)
                }
            );

            const body = await res.json().catch(() => ({}));

            if (!res.ok) {

                const msg =
                    body.message ||
                    body.error ||
                    `Registration failed (HTTP ${res.status}).`;

                setServerError(msg);
                return;
            }

            alert(
                'Registration successful! Please login with your email and password.'
            );

            navigate('/login');

        } catch (error) {

            console.error('Registration error:', error);

            setServerError(
                'Could not reach the server. Make sure the Spring Boot backend is running on port 8080.'
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

                    <Link
                        to="/"
                        className="register-brand"
                    >
                        <h1>DSGP</h1>

                        <p>
                            Digital Subsidy &amp; Grant Platform
                        </p>
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

                    <div className="register-card-header">

                        <h2>
                            Create Account
                        </h2>

                        <p>
                            Register as a beneficiary to access government schemes.
                        </p>

                    </div>


                    {/* Client-side errors */}
                    {clientErrors.length > 0 && (

                        <div
                            className="register-alert register-alert-error"
                            role="alert"
                        >

                            <span className="register-alert-icon">
                                ⚠️
                            </span>

                            <div className="register-alert-body">

                                <strong>
                                    Please fix the following:
                                </strong>

                                <ul>
                                    {clientErrors.map((err, i) => (
                                        <li key={i}>
                                            {err}
                                        </li>
                                    ))}
                                </ul>

                            </div>

                        </div>

                    )}


                    {/* Server error */}
                    {serverError && (

                        <div
                            className="register-alert register-alert-error"
                            role="alert"
                        >

                            <span className="register-alert-icon">
                                ❌
                            </span>

                            <div className="register-alert-body">

                                <strong>
                                    Registration failed
                                </strong>

                                <span>
                                    {serverError}
                                </span>

                            </div>

                        </div>

                    )}


                    <form
                        className="register-form"
                        onSubmit={handleSubmit}
                        noValidate
                    >


                        {/* ══════════════════════════════════════
                            BASIC INFORMATION
                        ══════════════════════════════════════ */}

                        <p className="register-section-label">
                            Basic Information
                        </p>


                        {/* Full Name */}
                        <div className="register-form-group">

                            <label htmlFor="fullName">
                                Full Name <span>*</span>
                            </label>

                            <input
                                id="fullName"
                                name="fullName"
                                type="text"
                                value={formData.fullName}
                                onChange={handleChange}
                                placeholder="e.g. Ravi Kumar"
                                required
                            />

                        </div>


                        {/* Government ID + Age */}
                        <div className="register-form-row">

                            <div className="register-form-group">

                                <label htmlFor="govId">
                                    Government ID <span>*</span>
                                </label>

                                <input
                                    id="govId"
                                    name="govId"
                                    type="text"
                                    value={formData.govId}
                                    onChange={handleChange}
                                    placeholder="Aadhaar / Voter ID"
                                    required
                                />

                            </div>


                            <div className="register-form-group">

                                <label htmlFor="age">
                                    Age <span>*</span>
                                </label>

                                <input
                                    id="age"
                                    name="age"
                                    type="number"
                                    min="1"
                                    max="120"
                                    value={formData.age}
                                    onChange={handleChange}
                                    placeholder="e.g. 35"
                                    required
                                />

                            </div>

                        </div>


                        {/* Email + Password + Mobile */}
                        <div className="register-form-row">

                            <div className="register-form-group">

                                <label htmlFor="email">
                                    Email Address <span>*</span>
                                </label>

                                <input
                                    id="email"
                                    name="email"
                                    type="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    placeholder="you@example.com"
                                    required
                                />

                            </div>


                            <div className="register-form-group">

                                <label htmlFor="password">
                                    Password <span>*</span>
                                </label>

                                <input
                                    id="password"
                                    name="password"
                                    type="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    placeholder="Create a password"
                                    required
                                />

                            </div>


                            <div className="register-form-group">

                                <label htmlFor="mobileNumber">
                                    Mobile Number <span>*</span>
                                </label>

                                <input
                                    id="mobileNumber"
                                    name="mobileNumber"
                                    type="tel"
                                    value={formData.mobileNumber}
                                    onChange={handleChange}
                                    placeholder="10-digit number"
                                    maxLength="10"
                                    required
                                />

                            </div>

                        </div>


                        {/* Address */}
                        <div className="register-form-group">

                            <label htmlFor="address">
                                Address <span>*</span>
                            </label>

                            <input
                                id="address"
                                name="address"
                                type="text"
                                value={formData.address}
                                onChange={handleChange}
                                placeholder="Full residential address"
                                required
                            />

                        </div>


                        {/* ══════════════════════════════════════
                            IDENTITY & DEMOGRAPHICS
                        ══════════════════════════════════════ */}

                        <p className="register-section-label">
                            Identity &amp; Demographics
                        </p>


                        {/* Aadhaar */}
                        <div className="register-form-group">

                            <label htmlFor="aadhaarNumber">
                                Aadhaar Number <span>*</span>
                            </label>

                            <input
                                id="aadhaarNumber"
                                name="aadhaarNumber"
                                type="text"
                                value={formData.aadhaarNumber}
                                onChange={handleChange}
                                placeholder="12-digit Aadhaar"
                                maxLength="12"
                                required
                            />

                        </div>


                        {/* DOB + Gender */}
                        <div className="register-form-row">

                            <div className="register-form-group">

                                <label htmlFor="dateOfBirth">
                                    Date of Birth <span>*</span>
                                </label>

                                <input
                                    id="dateOfBirth"
                                    name="dateOfBirth"
                                    type="date"
                                    value={formData.dateOfBirth}
                                    onChange={handleChange}
                                    required
                                />

                            </div>


                            <div className="register-form-group">

                                <label htmlFor="gender">
                                    Gender <span>*</span>
                                </label>

                                <select
                                    id="gender"
                                    name="gender"
                                    value={formData.gender}
                                    onChange={handleChange}
                                    required
                                >

                                    <option value="">
                                        Select gender
                                    </option>

                                    <option value="MALE">
                                        Male
                                    </option>

                                    <option value="FEMALE">
                                        Female
                                    </option>

                                    <option value="OTHER">
                                        Other
                                    </option>

                                </select>

                            </div>

                        </div>


                        {/* Category */}
                        <div className="register-form-group">

                            <label htmlFor="category">
                                Social Category <span>*</span>
                            </label>

                            <select
                                id="category"
                                name="category"
                                value={formData.category}
                                onChange={handleChange}
                                required
                            >

                                <option value="">
                                    Select category
                                </option>

                                <option value="GENERAL">
                                    General
                                </option>

                                <option value="OBC">
                                    OBC
                                </option>

                                <option value="SC">
                                    SC
                                </option>

                                <option value="ST">
                                    ST
                                </option>

                            </select>

                        </div>


                        {/* ══════════════════════════════════════
                            ADDRESS DETAILS
                        ══════════════════════════════════════ */}

                        <p className="register-section-label">
                            Address Details
                        </p>


                        {/* Village + Taluka */}
                        <div className="register-form-row">

                            <div className="register-form-group">

                                <label htmlFor="village">
                                    Village <span>*</span>
                                </label>

                                <select
                                    id="village"
                                    name="village"
                                    value={formData.village}
                                    onChange={handleChange}
                                    required
                                    disabled={
                                        !formData.taluka ||
                                        locationLoading.villages
                                    }
                                >

                                    <option value="">
                                        {locationLoading.villages
                                            ? 'Loading villages...'
                                            : !formData.taluka
                                                ? 'Select taluka first'
                                                : 'Select village'
                                        }
                                    </option>

                                    {villages.map((village) => (
                                        <option
                                            key={village.id}
                                            value={village.name}
                                        >
                                            {village.name}
                                        </option>
                                    ))}

                                </select>

                            </div>


                            <div className="register-form-group">

                                <label htmlFor="taluka">
                                    Taluka <span>*</span>
                                </label>

                                <select
                                    id="taluka"
                                    name="taluka"
                                    value={formData.taluka}
                                    onChange={handleChange}
                                    required
                                    disabled={
                                        !formData.district ||
                                        locationLoading.talukas
                                    }
                                >

                                    <option value="">
                                        {locationLoading.talukas
                                            ? 'Loading talukas...'
                                            : !formData.district
                                                ? 'Select district first'
                                                : 'Select taluka'
                                        }
                                    </option>

                                    {talukas.map((taluka) => (
                                        <option
                                            key={taluka.id}
                                            value={taluka.name}
                                        >
                                            {taluka.name}
                                        </option>
                                    ))}

                                </select>

                            </div>

                        </div>


                        {/* District + State */}
                        <div className="register-form-row">

                            <div className="register-form-group">

                                <label htmlFor="district">
                                    District <span>*</span>
                                </label>

                                <select
                                    id="district"
                                    name="district"
                                    value={formData.district}
                                    onChange={handleChange}
                                    required
                                    disabled={
                                        !formData.state ||
                                        locationLoading.districts
                                    }
                                >

                                    <option value="">
                                        {locationLoading.districts
                                            ? 'Loading districts...'
                                            : !formData.state
                                                ? 'Select state first'
                                                : 'Select district'
                                        }
                                    </option>

                                    {districts.map((district) => (
                                        <option
                                            key={district.id}
                                            value={district.name}
                                        >
                                            {district.name}
                                        </option>
                                    ))}

                                </select>

                            </div>


                            <div className="register-form-group">

                                <label htmlFor="state">
                                    State <span>*</span>
                                </label>

                                <select
                                    id="state"
                                    name="state"
                                    value={formData.state}
                                    onChange={handleChange}
                                    required
                                    disabled={locationLoading.states}
                                >

                                    <option value="">
                                        {locationLoading.states
                                            ? 'Loading states...'
                                            : 'Select state'
                                        }
                                    </option>

                                    {states.map((state) => (
                                        <option
                                            key={state.id}
                                            value={state.name}
                                        >
                                            {state.name}
                                        </option>
                                    ))}

                                </select>

                            </div>

                        </div>


                        {/* PIN */}
                        <div className="register-form-group">

                            <label htmlFor="pinCode">
                                PIN Code <span>*</span>
                            </label>

                            <input
                                id="pinCode"
                                name="pinCode"
                                type="text"
                                value={formData.pinCode}
                                onChange={handleChange}
                                placeholder="6-digit PIN code"
                                maxLength="6"
                                required
                            />

                        </div>



                        {/* ══════════════════════════════════════
    OCCUPATION
══════════════════════════════════════ */}

                        <p className="register-section-label">
                            Occupation &amp; Status
                        </p>

                        <div className="register-form-group">

                            <label htmlFor="occupation">
                                Occupation / Status <span>*</span>
                            </label>

                            <select
                                id="occupation"
                                name="occupation"
                                value={formData.occupation}
                                onChange={handleChange}
                                required
                            >
                                <option value="">
                                    Select occupation / status
                                </option>

                                <option value="Farmer">
                                    Farmer
                                </option>

                                <option value="Student">
                                    Student
                                </option>

                                <option value="Entrepreneur">
                                    Entrepreneur
                                </option>

                                <option value="Business Owner">
                                    Small Business Owner
                                </option>

                                <option value="Self Employed">
                                    Self Employed
                                </option>

                                <option value="Salaried">
                                    Salaried Employee
                                </option>

                                <option value="Other">
                                    Other
                                </option>
                            </select>

                        </div>


                        {/* ══════════════════════════════════════
                            FINANCIAL INFORMATION
                        ══════════════════════════════════════ */}

                        <p className="register-section-label">
                            Financial Information
                        </p>


                        <div className="register-form-row">

                            {/* Annual Income - REQUIRED */}
                            <div className="register-form-group">

                                <label htmlFor="annualIncome">
                                    Annual Income (₹) <span>*</span>
                                </label>

                                <input
                                    id="annualIncome"
                                    name="annualIncome"
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={formData.annualIncome}
                                    onChange={handleChange}
                                    placeholder="e.g. 120000"
                                    required
                                />

                            </div>


                            {/* Land Holding - ONLY OPTIONAL */}
                            <div className="register-form-group">

                                <label htmlFor="landHolding">
                                    Land Holding (acres)
                                </label>

                                <input
                                    id="landHolding"
                                    name="landHolding"
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={formData.landHolding}
                                    onChange={handleChange}
                                    placeholder="e.g. 1.5"
                                />

                                <small>
                                    Optional
                                </small>

                            </div>

                        </div>


                        {/* ══════════════════════════════════════
                            TERMS
                        ══════════════════════════════════════ */}

                        <label className="register-terms">

                            <input
                                type="checkbox"
                                name="terms"
                                checked={formData.terms}
                                onChange={handleChange}
                            />

                            <span>

                                I agree to the{' '}

                                <Link to="/terms">
                                    Terms of Service
                                </Link>

                                {' '}and{' '}

                                <Link to="/privacy">
                                    Privacy Policy
                                </Link>.

                            </span>

                        </label>


                        {/* Submit */}
                        <button
                            id="register-submit-btn"
                            type="submit"
                            className="register-button"
                            disabled={loading}
                        >

                            {loading && (
                                <span
                                    className="register-button-spinner"
                                    aria-hidden="true"
                                />
                            )}

                            {loading
                                ? 'Registering…'
                                : 'Create Account'
                            }

                        </button>

                    </form>


                    {/* Login link */}
                    <div className="register-login">

                        <p>
                            Already have an account?
                        </p>

                        <Link to="/login">
                            Sign In
                        </Link>

                    </div>

                </div>

            </main>


            {/* Footer */}
            <footer className="register-footer">

                <p>
                    &copy; 2024 Digital Subsidy &amp; Grant Platform (DSGP)
                </p>

            </footer>

        </div>
    );
}

export default Register;