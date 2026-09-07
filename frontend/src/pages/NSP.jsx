import { Link } from 'react-router-dom';
import './NSP.css';

function NSP() {

    const quickFacts = [
        { label: 'Scheme', value: 'NSP' },
        { label: 'Full Name', value: 'National Scholarship Portal' },
        { label: 'Category', value: 'Education' },
        { label: 'Target Beneficiaries', value: 'Students' },
        { label: 'Scholarship', value: 'Multiple Scholarships' },
        { label: 'Payment Method', value: 'Direct Benefit Transfer' }
    ];

    const eligibilityItems = [
        {
            icon: '🎓',
            title: 'Students',
            desc: 'NSP provides access to multiple government scholarship schemes for eligible students.'
        },
        {
            icon: '🎂',
            title: 'Age Requirement',
            desc: 'For this DSGP demonstration, the configured age range for NSP is 15 to 30 years.'
        },
        {
            icon: '💰',
            title: 'Annual Income',
            desc: 'For this DSGP demonstration, annual income should not exceed ₹2,50,000.'
        },
        {
            icon: '📚',
            title: 'Education',
            desc: 'Applicants should satisfy the academic and education-level requirements of the applicable scholarship.'
        },
        {
            icon: '👥',
            title: 'Applicable Categories',
            desc: 'Different scholarships may be available for different social and economic categories.'
        },
        {
            icon: '📋',
            title: 'Scheme-Specific Conditions',
            desc: 'Actual scholarship eligibility varies according to the particular scholarship scheme.'
        }
    ];

    const benefits = [
        {
            icon: '💰',
            title: 'Tuition Fee Support',
            desc: 'Covers full or partial tuition fees as applicable under the specific scheme and institution category.'
        },
        {
            icon: '🏠',
            title: 'Maintenance Allowance',
            desc: 'Monthly or annual maintenance allowance may support living and boarding expenses under applicable schemes.'
        },
        {
            icon: '📦',
            title: 'Study Materials',
            desc: 'Allowance for books, stationery, and other academic materials under applicable scholarship schemes.'
        },
        {
            icon: '🏦',
            title: 'Direct Bank Transfer',
            desc: 'Eligible scholarship amounts can be transferred directly to the student through DBT.'
        },
        {
            icon: '🔄',
            title: 'Renewal Option',
            desc: 'Eligible students may renew scholarships each academic year subject to the applicable conditions.'
        },
        {
            icon: '📊',
            title: 'Status Tracking',
            desc: 'Students can track the status of their scholarship application through the applicable portal.'
        }
    ];

    const documents = [
        {
            icon: '🪪',
            label: 'Aadhaar Card',
            note: '12-digit Aadhaar number of the student; mandatory for DBT'
        },
        {
            icon: '📄',
            label: 'Income Certificate',
            note: 'Current year certificate issued by competent authority'
        },
        {
            icon: '🎓',
            label: 'Previous Year Marksheet',
            note: 'Last passed exam marksheet to demonstrate academic eligibility'
        },
        {
            icon: '🏦',
            label: 'Bank Passbook / Account Details',
            note: "Aadhaar-linked bank account; student's own account preferred"
        },
        {
            icon: '📷',
            label: 'Passport-size Photograph',
            note: 'Recent colour photograph in prescribed format'
        },
        {
            icon: '🏫',
            label: 'School / College Bonafide Certificate',
            note: 'Issued by the current institution confirming enrolment'
        },
        {
            icon: '📋',
            label: 'Category Certificate',
            note: 'SC/ST/OBC/Minority/Disability certificate as applicable'
        },
        {
            icon: '🏠',
            label: 'Domicile Certificate',
            note: 'State domicile certificate required for state-specific schemes'
        },
        {
            icon: '💳',
            label: 'Fee Receipt',
            note: 'Receipt of fees paid to current institution for the academic year'
        }
    ];

    const steps = [
        {
            num: '01',
            title: 'Register as a Beneficiary',
            desc: 'Register on the DSGP portal with your personal details including identification, date of birth, mobile number, address, and category.'
        },
        {
            num: '02',
            title: 'Select the Applicable Scheme',
            desc: 'Browse available NSP scholarship schemes applicable to your category, education level, and state. Read eligibility criteria carefully.'
        },
        {
            num: '03',
            title: 'Fill the Application Form',
            desc: 'Complete the online application form with accurate personal, academic, and financial details. Double-check all fields before proceeding.'
        },
        {
            num: '04',
            title: 'Upload Required Documents',
            desc: 'Upload scanned copies of all required documents in the prescribed format and size. Ensure every document is clear and readable.'
        },
        {
            num: '05',
            title: 'Institute Verification',
            desc: 'Your application must be verified and forwarded by your school or college nodal officer through the applicable portal.'
        },
        {
            num: '06',
            title: 'District / State Level Processing',
            desc: 'Verified applications are reviewed at the district and state level. Approved applications receive scholarship amounts via DBT.'
        }
    ];

    return (
        <div className="nsp-page">

            {/* HERO */}
            <section
                className="nsp-hero"
                aria-labelledby="nsp-hero-title"
            >
                <div className="nsp-container">

                    <span className="nsp-badge">
                        Education
                    </span>

                    <h1 id="nsp-hero-title">
                        National Scholarship Portal
                    </h1>

                    <p className="nsp-hero-full-name">
                        NSP — Unified Scholarship Platform
                    </p>

                    <p className="nsp-hero-desc">
                        A unified platform for government scholarship schemes
                        enabling eligible students across India to apply,
                        track, and receive scholarships through Direct Benefit
                        Transfer.
                    </p>

                    <div className="nsp-hero-actions">

                        <a
                            href="#nsp-process"
                            className="btn btn-primary"
                        >
                            How to Apply
                        </a>

                        <Link
                            to="/eligibility?scheme=NSP"
                            className="btn btn-primary"
                        >
                            Apply Now
                        </Link>

                        <Link
                            to="/schemes"
                            className="btn btn-secondary"
                        >
                            Back to Schemes
                        </Link>

                    </div>

                </div>
            </section>


            <main className="nsp-main-container">

                {/* OVERVIEW + QUICK FACTS */}
                <section
                    className="nsp-overview-section"
                    id="nsp-overview"
                    aria-labelledby="nsp-overview-title"
                >

                    <div className="nsp-overview-grid">

                        <div className="nsp-overview-text">

                            <h2 id="nsp-overview-title">
                                What is the National Scholarship Portal?
                            </h2>

                            <p>
                                The National Scholarship Portal (NSP) is a
                                Government of India initiative under the
                                Digital India Programme, bringing scholarship
                                schemes onto a unified platform.
                            </p>

                            <p>
                                NSP enables eligible students to apply online,
                                track their application status, and receive
                                scholarship funds through the Direct Benefit
                                Transfer (DBT) mechanism.
                            </p>

                            <p>
                                The portal covers scholarships for students
                                from different backgrounds and education
                                levels, subject to the eligibility conditions
                                of individual scholarship schemes.
                            </p>

                            <div className="nsp-notice nsp-notice--info">

                                <span aria-hidden="true">
                                    ℹ️
                                </span>

                                <div>
                                    <strong>
                                        About This Page:
                                    </strong>{' '}
                                    This page provides general information
                                    about NSP for the DSGP academic
                                    demonstration platform. Actual scholarship
                                    applications may require the official NSP
                                    portal and applicable government process.
                                </div>

                            </div>

                        </div>


                        <aside
                            className="nsp-quick-facts"
                            aria-label="NSP Quick Facts"
                        >

                            <h3>
                                Quick Facts
                            </h3>

                            {quickFacts.map((fact, index) => (

                                <div
                                    key={index}
                                    className="nsp-fact-row"
                                >

                                    <span className="nsp-fact-label">
                                        {fact.label}
                                    </span>

                                    <span className="nsp-fact-value">
                                        {fact.value}
                                    </span>

                                </div>

                            ))}

                        </aside>

                    </div>

                </section>


                {/* WHO CAN APPLY */}
                <section
                    className="nsp-section nsp-section--alt"
                    id="nsp-eligibility"
                    aria-labelledby="nsp-elig-title"
                >

                    <h2
                        id="nsp-elig-title"
                        className="nsp-section-title"
                    >
                        Who Can Apply
                    </h2>

                    <p className="nsp-section-subtitle">
                        NSP scholarships are available to students from
                        diverse backgrounds and education levels across India.
                    </p>

                    <ul
                        className="nsp-eligibility-grid"
                        aria-label="Eligibility criteria"
                    >

                        {eligibilityItems.map((item, index) => (

                            <li
                                key={index}
                                className="nsp-eligibility-item"
                            >

                                <span
                                    className="nsp-eligibility-icon"
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


                    <div className="nsp-notice nsp-notice--warning">

                        <span aria-hidden="true">
                            ⚠️
                        </span>

                        <div>

                            <strong>
                                Important:
                            </strong>{' '}
                            Eligibility criteria including income limits,
                            academic percentage requirements, and age limits
                            vary by specific scholarship scheme.

                        </div>

                    </div>

                </section>


                {/* BENEFITS */}
                <section
                    className="nsp-section"
                    id="nsp-benefits"
                    aria-labelledby="nsp-benefits-title"
                >

                    <h2
                        id="nsp-benefits-title"
                        className="nsp-section-title"
                    >
                        Scheme Benefits
                    </h2>

                    <p className="nsp-section-subtitle">
                        Benefits depend on the specific scholarship scheme
                        available through NSP.
                    </p>

                    <ul
                        className="nsp-benefits-grid"
                        aria-label="Benefits"
                    >

                        {benefits.map((benefit, index) => (

                            <li
                                key={index}
                                className="nsp-benefit-card"
                            >

                                <div
                                    className="nsp-benefit-icon"
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


                {/* REQUIRED DOCUMENTS */}
                <section
                    className="nsp-section nsp-section--alt"
                    id="nsp-documents"
                    aria-labelledby="nsp-docs-title"
                >

                    <h2
                        id="nsp-docs-title"
                        className="nsp-section-title"
                    >
                        Required Documents
                    </h2>

                    <p className="nsp-section-subtitle">
                        Prepare the following documents before beginning your
                        NSP scholarship application.
                    </p>

                    <ul
                        className="nsp-doc-list"
                        aria-label="Required documents"
                    >

                        {documents.map((document, index) => (

                            <li
                                key={index}
                                className="nsp-doc-item"
                            >

                                <span
                                    className="nsp-doc-icon"
                                    aria-hidden="true"
                                >
                                    {document.icon}
                                </span>

                                <div>

                                    <div className="nsp-doc-label">
                                        {document.label}
                                    </div>

                                    <div className="nsp-doc-note">
                                        {document.note}
                                    </div>

                                </div>

                            </li>

                        ))}

                    </ul>

                </section>


                {/* APPLICATION PROCESS */}
                <section
                    className="nsp-section"
                    id="nsp-process"
                    aria-labelledby="nsp-process-title"
                >

                    <h2
                        id="nsp-process-title"
                        className="nsp-section-title"
                    >
                        Application Process
                    </h2>

                    <p className="nsp-section-subtitle">
                        Follow these steps to complete your NSP scholarship
                        application.
                    </p>

                    <ol
                        className="nsp-steps-list"
                        aria-label="Application steps"
                    >

                        {steps.map((step, index) => (

                            <li
                                key={index}
                                className="nsp-step-item"
                            >

                                <div
                                    className="nsp-step-number"
                                    aria-hidden="true"
                                >
                                    {step.num}
                                </div>

                                <div className="nsp-step-body">

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
                    className="nsp-section nsp-section--alt"
                    aria-labelledby="nsp-important-title"
                >

                    <h2
                        id="nsp-important-title"
                        className="nsp-section-title"
                    >
                        Important Information
                    </h2>

                    <div className="nsp-notices-grid">

                        <div className="nsp-notice nsp-notice--warning">

                            <span aria-hidden="true">
                                ⚠️
                            </span>

                            <div>

                                <strong>
                                    One Application Per Student:
                                </strong>{' '}
                                A student should follow the applicable
                                scholarship rules and avoid duplicate
                                applications.

                            </div>

                        </div>


                        <div className="nsp-notice nsp-notice--info">

                            <span aria-hidden="true">
                                ℹ️
                            </span>

                            <div>

                                <strong>
                                    Aadhaar and Bank Details:
                                </strong>{' '}
                                Ensure identification and bank information
                                provided for DBT is accurate.

                            </div>

                        </div>


                        <div className="nsp-notice nsp-notice--danger">

                            <span aria-hidden="true">
                                🚫
                            </span>

                            <div>

                                <strong>
                                    Beware of Fraudsters:
                                </strong>{' '}
                                Do not share passwords, OTPs, or sensitive
                                banking information with unauthorised persons.

                            </div>

                        </div>


                        <div className="nsp-notice nsp-notice--success">

                            <span aria-hidden="true">
                                ✅
                            </span>

                            <div>

                                <strong>
                                    Keep Your Records:
                                </strong>{' '}
                                Keep acknowledgement details and application
                                information safely for future reference.

                            </div>

                        </div>

                    </div>

                </section>


                {/* CTA */}
                <section
                    className="nsp-cta"
                    aria-labelledby="nsp-cta-title"
                >

                    <h2 id="nsp-cta-title">
                        Ready to Check Your Eligibility?
                    </h2>

                    <p>
                        Check whether you may qualify for the configured NSP
                        eligibility criteria and continue with the DSGP
                        application flow.
                    </p>

                    <div className="nsp-cta-actions">

                        <Link
                            to="/eligibility?scheme=NSP"
                            className="btn btn-primary"
                        >
                            Check Eligibility & Apply
                        </Link>

                        <Link
                            to="/schemes"
                            className="btn btn-secondary"
                        >
                            All Schemes
                        </Link>

                    </div>

                    <p className="nsp-cta-note">
                        ⓘ DSGP is an academic demonstration platform.
                        Always verify current scholarship eligibility and
                        application requirements through official government
                        channels.
                    </p>

                </section>

            </main>

        </div>
    );
}

export default NSP;