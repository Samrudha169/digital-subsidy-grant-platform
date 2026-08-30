import { Link } from 'react-router-dom';
import './PMKisan.css';

function PMKisan() {
    const quickFacts = [
        { label: 'Scheme Type', value: 'Income Support Scheme' },
        { label: 'Ministry', value: 'Ministry of Agriculture & Farmers Welfare' },
        { label: 'Target Beneficiaries', value: 'Eligible Farmer Families' },
        { label: 'Financial Benefit', value: '₹6,000 per year' },
        { label: 'Installments', value: '3 equal installments' },
        { label: 'Transfer Mode', value: 'Direct Benefit Transfer (DBT)' },
        { label: 'Application Mode', value: 'Online / Assisted' },
    ];

    const eligibilityItems = [
        {
            icon: '🌾',
            title: 'Eligible Farmer Families',
            desc: 'The scheme provides income support to eligible farmer families meeting the applicable government conditions.',
        },
        {
            icon: '🚜',
            title: 'Landholding Farmers',
            desc: 'The applicant should meet the applicable landholding and land-record requirements under the scheme.',
        },
        {
            icon: '🇮🇳',
            title: 'Indian Farmers',
            desc: 'The applicant must satisfy the applicable requirements prescribed by the Government of India.',
        },
        {
            icon: '🏦',
            title: 'Valid Bank Account',
            desc: 'A valid bank account is required so that eligible financial assistance can be transferred through DBT.',
        },
        {
            icon: '🪪',
            title: 'Valid Identification',
            desc: 'Applicants need valid identification and other details required for registration and verification.',
        },
        {
            icon: '📋',
            title: 'Verified Land Records',
            desc: 'Land ownership or cultivation-related records may be required for verification according to applicable rules.',
        },
    ];

    const benefits = [
        {
            icon: '💰',
            title: '₹6,000 Annual Support',
            desc: 'Eligible farmer families receive financial assistance of ₹6,000 per year under the scheme.',
        },
        {
            icon: '💳',
            title: 'Three Installments',
            desc: 'The annual assistance is provided in three equal installments of ₹2,000 each.',
        },
        {
            icon: '🏦',
            title: 'Direct Bank Transfer',
            desc: 'Approved financial assistance is transferred directly to the beneficiary bank account through DBT.',
        },
        {
            icon: '🌱',
            title: 'Support for Farmers',
            desc: 'The scheme provides income support to eligible farmer families to assist with agricultural and household needs.',
        },
        {
            icon: '📱',
            title: 'Online Status Checking',
            desc: 'Beneficiaries can use the relevant online services to check registration and payment-related status.',
        },
        {
            icon: '🔄',
            title: 'Periodic Payments',
            desc: 'Eligible beneficiaries receive installments according to the payment cycle and applicable verification requirements.',
        },
    ];

    const documents = [
        {
            icon: '🪪',
            label: 'Aadhaar Card',
            note: 'Valid Aadhaar details required for identification and verification.',
        },
        {
            icon: '🏦',
            label: 'Bank Account Details',
            note: 'Bank account information required for receiving DBT payments.',
        },
        {
            icon: '🌾',
            label: 'Land Ownership Records',
            note: 'Applicable land records or ownership documents for beneficiary verification.',
        },
        {
            icon: '📱',
            label: 'Mobile Number',
            note: 'Active mobile number for registration, communication, and status-related services.',
        },
        {
            icon: '📄',
            label: 'Identity / Address Details',
            note: 'Additional identification or address information may be required where applicable.',
        },
        {
            icon: '📋',
            label: 'Other Supporting Documents',
            note: 'Additional documents may be requested by the concerned authorities during verification.',
        },
    ];

    const steps = [
        {
            num: '01',
            title: 'Check Your Eligibility',
            desc: 'Review the applicable PM-KISAN eligibility conditions and ensure that you meet the requirements before registration.',
        },
        {
            num: '02',
            title: 'Complete Beneficiary Registration',
            desc: 'Enter the required personal, identification, land, and bank account details through the applicable registration process.',
        },
        {
            num: '03',
            title: 'Submit Required Details',
            desc: 'Provide the required documents and information for verification by the concerned authorities.',
        },
        {
            num: '04',
            title: 'Verification',
            desc: 'The submitted information and land-related records are verified through the applicable government process.',
        },
        {
            num: '05',
            title: 'Approval',
            desc: 'Once the beneficiary is found eligible and the required verification is completed, the application can proceed for payment.',
        },
        {
            num: '06',
            title: 'Receive Payment',
            desc: 'Eligible beneficiaries receive the applicable installment directly into their registered bank account through DBT.',
        },
    ];

    return (
        <div className="pmkisan-page">

            {/* HERO */}
            <section
                className="pmkisan-hero"
                aria-labelledby="pmkisan-hero-title"
            >
                <div className="pmkisan-container">

                    <span className="pmkisan-badge">
                        Agriculture
                    </span>

                    <h1 id="pmkisan-hero-title">
                        PM-KISAN
                    </h1>

                    <p className="pmkisan-hero-full-name">
                        Pradhan Mantri Kisan Samman Nidhi
                    </p>

                    <p className="pmkisan-hero-desc">
                        Income support scheme providing financial assistance
                        to eligible farmer families across India through
                        Direct Benefit Transfer.
                    </p>

                    <div className="pmkisan-hero-actions">

                        <a
                            href="#pmkisan-process"
                            className="btn btn-primary"
                        >
                            How to Apply
                        </a>

                        <Link
                            to="/schemes"
                            className="btn btn-secondary"
                        >
                            Back to Schemes
                        </Link>

                    </div>

                </div>
            </section>


            <main className="pmkisan-main-container">

                {/* OVERVIEW + QUICK FACTS */}
                <section
                    className="pmkisan-overview-section"
                    id="pmkisan-overview"
                    aria-labelledby="pmkisan-overview-title"
                >

                    <div className="pmkisan-overview-grid">

                        <div className="pmkisan-overview-text">

                            <h2 id="pmkisan-overview-title">
                                What is PM-KISAN?
                            </h2>

                            <p>
                                Pradhan Mantri Kisan Samman Nidhi (PM-KISAN)
                                is an income support scheme for eligible
                                farmer families. Under the scheme, eligible
                                farmers receive financial assistance directly
                                into their bank accounts.
                            </p>

                            <p>
                                The scheme provides annual financial assistance
                                of ₹6,000 to eligible farmer families. The
                                amount is provided in three equal installments
                                of ₹2,000 through Direct Benefit Transfer.
                            </p>

                            <p>
                                PM-KISAN aims to provide income support to
                                eligible farmers and assist them with
                                agricultural and related household expenses.
                            </p>

                            <div className="pmkisan-notice pmkisan-notice--info">

                                <span aria-hidden="true">
                                    ℹ️
                                </span>

                                <div>
                                    <strong>
                                        About This Page:
                                    </strong>{' '}
                                    This page provides general information
                                    about PM-KISAN for the DSGP academic
                                    demonstration platform. Applicants should
                                    verify current eligibility requirements
                                    and application procedures through
                                    official government sources.
                                </div>

                            </div>

                        </div>


                        <aside
                            className="pmkisan-quick-facts"
                            aria-label="PM-KISAN Quick Facts"
                        >

                            <h3>
                                Quick Facts
                            </h3>

                            {quickFacts.map((fact, index) => (

                                <div
                                    key={index}
                                    className="pmkisan-fact-row"
                                >

                                    <span className="pmkisan-fact-label">
                                        {fact.label}
                                    </span>

                                    <span className="pmkisan-fact-value">
                                        {fact.value}
                                    </span>

                                </div>

                            ))}

                        </aside>

                    </div>

                </section>


                {/* ELIGIBILITY */}
                <section
                    className="pmkisan-section pmkisan-section--alt"
                    id="pmkisan-eligibility"
                    aria-labelledby="pmkisan-eligibility-title"
                >

                    <h2
                        id="pmkisan-eligibility-title"
                        className="pmkisan-section-title"
                    >
                        Who Can Apply?
                    </h2>

                    <p className="pmkisan-section-subtitle">
                        PM-KISAN is intended to provide income support to
                        eligible farmer families who satisfy the applicable
                        government conditions.
                    </p>


                    <ul
                        className="pmkisan-eligibility-grid"
                        aria-label="PM-KISAN eligibility"
                    >

                        {eligibilityItems.map((item, index) => (

                            <li
                                key={index}
                                className="pmkisan-eligibility-item"
                            >

                                <span
                                    className="pmkisan-eligibility-icon"
                                    aria-hidden="true"
                                >
                                    {item.icon}
                                </span>

                                <div>

                                    <strong>
                                        {item.title}
                                    </strong>

                                    <p>
                                        {item.desc}
                                    </p>

                                </div>

                            </li>

                        ))}

                    </ul>


                    <div className="pmkisan-notice pmkisan-notice--warning">

                        <span aria-hidden="true">
                            ⚠️
                        </span>

                        <div>

                            <strong>
                                Important:
                            </strong>{' '}
                            Eligibility is subject to the exclusions,
                            landholding requirements, verification rules,
                            and other conditions prescribed under the
                            applicable PM-KISAN guidelines.

                        </div>

                    </div>

                </section>


                {/* BENEFITS */}
                <section
                    className="pmkisan-section"
                    id="pmkisan-benefits"
                    aria-labelledby="pmkisan-benefits-title"
                >

                    <h2
                        id="pmkisan-benefits-title"
                        className="pmkisan-section-title"
                    >
                        Scheme Benefits
                    </h2>

                    <p className="pmkisan-section-subtitle">
                        PM-KISAN provides financial assistance to eligible
                        farmer families through a direct payment mechanism.
                    </p>


                    <ul
                        className="pmkisan-benefits-grid"
                        aria-label="PM-KISAN benefits"
                    >

                        {benefits.map((benefit, index) => (

                            <li
                                key={index}
                                className="pmkisan-benefit-card"
                            >

                                <div
                                    className="pmkisan-benefit-icon"
                                    aria-hidden="true"
                                >
                                    {benefit.icon}
                                </div>

                                <h3>
                                    {benefit.title}
                                </h3>

                                <p>
                                    {benefit.desc}
                                </p>

                            </li>

                        ))}

                    </ul>

                </section>


                {/* DOCUMENTS */}
                <section
                    className="pmkisan-section pmkisan-section--alt"
                    id="pmkisan-documents"
                    aria-labelledby="pmkisan-documents-title"
                >

                    <h2
                        id="pmkisan-documents-title"
                        className="pmkisan-section-title"
                    >
                        Required Documents & Details
                    </h2>

                    <p className="pmkisan-section-subtitle">
                        Keep the following information and supporting
                        documents ready before beginning the registration
                        process.
                    </p>


                    <ul
                        className="pmkisan-doc-list"
                        aria-label="PM-KISAN required documents"
                    >

                        {documents.map((document, index) => (

                            <li
                                key={index}
                                className="pmkisan-doc-item"
                            >

                                <span
                                    className="pmkisan-doc-icon"
                                    aria-hidden="true"
                                >
                                    {document.icon}
                                </span>

                                <div>

                                    <div className="pmkisan-doc-label">
                                        {document.label}
                                    </div>

                                    <div className="pmkisan-doc-note">
                                        {document.note}
                                    </div>

                                </div>

                            </li>

                        ))}

                    </ul>

                </section>


                {/* APPLICATION PROCESS */}
                <section
                    className="pmkisan-section"
                    id="pmkisan-process"
                    aria-labelledby="pmkisan-process-title"
                >

                    <h2
                        id="pmkisan-process-title"
                        className="pmkisan-section-title"
                    >
                        Application / Registration Process
                    </h2>

                    <p className="pmkisan-section-subtitle">
                        Follow the applicable registration and verification
                        process to become a PM-KISAN beneficiary.
                    </p>


                    <ol
                        className="pmkisan-steps-list"
                        aria-label="PM-KISAN application process"
                    >

                        {steps.map((step, index) => (

                            <li
                                key={index}
                                className="pmkisan-step-item"
                            >

                                <div
                                    className="pmkisan-step-number"
                                    aria-hidden="true"
                                >
                                    {step.num}
                                </div>

                                <div className="pmkisan-step-body">

                                    <h3>
                                        {step.title}
                                    </h3>

                                    <p>
                                        {step.desc}
                                    </p>

                                </div>

                            </li>

                        ))}

                    </ol>

                </section>


                {/* IMPORTANT INFORMATION */}
                <section
                    className="pmkisan-section pmkisan-section--alt"
                    aria-labelledby="pmkisan-important-title"
                >

                    <h2
                        id="pmkisan-important-title"
                        className="pmkisan-section-title"
                    >
                        Important Information
                    </h2>


                    <div className="pmkisan-notices-grid">

                        <div className="pmkisan-notice pmkisan-notice--warning">

                            <span aria-hidden="true">
                                ⚠️
                            </span>

                            <div>

                                <strong>
                                    Eligibility Verification:
                                </strong>{' '}
                                Submission of an application does not
                                automatically guarantee payment. Beneficiary
                                details are subject to applicable verification.

                            </div>

                        </div>


                        <div className="pmkisan-notice pmkisan-notice--info">

                            <span aria-hidden="true">
                                ℹ️
                            </span>

                            <div>

                                <strong>
                                    Bank Details:
                                </strong>{' '}
                                Ensure that your bank account details are
                                accurate and capable of receiving DBT payments.

                            </div>

                        </div>


                        <div className="pmkisan-notice pmkisan-notice--danger">

                            <span aria-hidden="true">
                                🚫
                            </span>

                            <div>

                                <strong>
                                    Beware of Fraud:
                                </strong>{' '}
                                Do not share sensitive banking information,
                                OTPs, or passwords with unauthorised persons
                                claiming to provide PM-KISAN services.

                            </div>

                        </div>


                        <div className="pmkisan-notice pmkisan-notice--success">

                            <span aria-hidden="true">
                                ✅
                            </span>

                            <div>

                                <strong>
                                    Keep Your Records:
                                </strong>{' '}
                                Keep your registration details and relevant
                                acknowledgement information safely for future
                                reference and status checking.

                            </div>

                        </div>

                    </div>

                </section>


                {/* CTA */}
                <section
                    className="pmkisan-cta"
                    aria-labelledby="pmkisan-cta-title"
                >

                    <h2 id="pmkisan-cta-title">
                        Ready to Check Your Eligibility?
                    </h2>

                    <p>
                        Check whether you may qualify for PM-KISAN and
                        explore other government assistance schemes available
                        on DSGP.
                    </p>


                    <div className="pmkisan-cta-actions">

                        <Link
                            to="/eligibility"
                            className="btn btn-primary"
                        >
                            Check Eligibility
                        </Link>

                        <Link
                            to="/schemes"
                            className="btn btn-secondary"
                        >
                            All Schemes
                        </Link>

                    </div>


                    <p className="pmkisan-cta-note">
                        ⓘ DSGP is an academic demonstration platform.
                        Always verify current PM-KISAN eligibility,
                        registration, payment, and beneficiary information
                        through official government channels.
                    </p>

                </section>

            </main>

        </div>
    );
}

export default PMKisan;