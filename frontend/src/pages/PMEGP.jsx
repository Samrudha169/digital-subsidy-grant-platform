import { Link } from 'react-router-dom';
import './PMEGP.css';

function PMEGP() {

    const quickFacts = [
        { label: 'Scheme', value: 'PMEGP' },
        {
            label: 'Full Name',
            value: "Prime Minister's Employment Generation Programme"
        },
        {
            label: 'Category',
            value: 'Business & Entrepreneurship'
        },
        {
            label: 'Target Beneficiaries',
            value: 'Entrepreneurs'
        },
        {
            label: 'Benefit',
            value: 'Credit-linked Subsidy'
        },
        {
            label: 'Implementation',
            value: 'KVIC / DIC / Banks'
        }
    ];

    const objectives = [
        {
            icon: '💼',
            title: 'Self Employment',
            desc: 'Promote self-employment opportunities through establishment of micro-enterprises.'
        },
        {
            icon: '🏭',
            title: 'Micro Enterprises',
            desc: 'Support establishment and development of eligible micro-enterprises in the non-farm sector.'
        },
        {
            icon: '👨‍💼',
            title: 'Entrepreneurship',
            desc: 'Encourage individuals to start sustainable businesses and generate employment.'
        },
        {
            icon: '📈',
            title: 'Economic Development',
            desc: 'Support local economic activity and employment generation through enterprise development.'
        }
    ];

    const eligibilityItems = [
        {
            icon: '👤',
            title: 'Individuals',
            desc: 'PMEGP is available to eligible individuals and other permitted categories under the applicable guidelines.'
        },
        {
            icon: '🎂',
            title: 'Age Requirement',
            desc: 'For this DSGP demonstration, the configured age range is 18 to 55 years.'
        },
        {
            icon: '💰',
            title: 'Annual Income',
            desc: 'For this DSGP demonstration, annual income should not exceed ₹8,00,000.'
        },
        {
            icon: '🏢',
            title: 'Occupation',
            desc: 'The configured DSGP rule allows applicants from any occupation.'
        },
        {
            icon: '🆕',
            title: 'New Projects',
            desc: 'PMEGP supports new project establishments subject to the applicable scheme guidelines.'
        },
        {
            icon: '📋',
            title: 'Scheme Conditions',
            desc: 'Project cost limits, subsidy rates, categories, and other conditions are subject to applicable PMEGP guidelines.'
        }
    ];

    const benefits = [
        {
            icon: '💰',
            title: 'Credit-linked Subsidy',
            desc: 'PMEGP provides margin money subsidy for eligible projects through the applicable credit-linked mechanism.'
        },
        {
            icon: '🏭',
            title: 'Project Support',
            desc: 'Eligible applicants can receive support for establishment of micro-enterprises.'
        },
        {
            icon: '👨‍💼',
            title: 'Self Employment',
            desc: 'The scheme supports individuals seeking to create sustainable self-employment opportunities.'
        },
        {
            icon: '🏦',
            title: 'Bank-linked Process',
            desc: 'Eligible projects are processed through implementing agencies and participating banks.'
        },
        {
            icon: '📚',
            title: 'EDP Training',
            desc: 'Entrepreneurship Development Programme training forms part of the applicable PMEGP process.'
        },
        {
            icon: '📈',
            title: 'Employment Generation',
            desc: 'The scheme aims to encourage enterprise creation and employment generation.'
        }
    ];

    const documents = [
        {
            icon: '🪪',
            label: 'Identity Proof',
            note: 'Valid government-issued identity document as applicable.'
        },
        {
            icon: '📄',
            label: 'Project Report (DPR)',
            note: 'Detailed Project Report including project details, cost estimates, market analysis, and financial projections.'
        },
        {
            icon: '🏠',
            label: 'Residence Proof',
            note: 'Current address proof such as applicable government-issued documentation.'
        },
        {
            icon: '🏦',
            label: 'Bank Account Details',
            note: 'Bank passbook or applicable account details for the credit-linked process.'
        },
        {
            icon: '🎖️',
            label: 'Special Category Proof',
            note: 'Applicable certificate or proof where the applicant belongs to a special category.'
        },
        {
            icon: '📚',
            label: 'EDP Training Certificate',
            note: 'Entrepreneurship Development Programme certificate where required before disbursement.'
        }
    ];

    const steps = [
        {
            num: '01',
            title: 'Register on the Portal',
            desc: 'Register on the DSGP portal as a beneficiary, providing personal details, identification, income information, and category.'
        },
        {
            num: '02',
            title: 'Prepare a Detailed Project Report',
            desc: 'Prepare a viable Detailed Project Report covering the business plan, product or service description, market analysis, and projected financials.'
        },
        {
            num: '03',
            title: 'Submit Application to KVIC / DIC',
            desc: 'Submit the completed application along with the DPR and required documents through the applicable implementing agency process.'
        },
        {
            num: '04',
            title: 'Task Force Committee Scrutiny',
            desc: 'The applicable district-level committee reviews and shortlists applications according to scheme guidelines and project feasibility.'
        },
        {
            num: '05',
            title: 'Bank Loan Sanction',
            desc: 'Approved applications are forwarded to designated banks. The bank evaluates the project and processes the loan according to applicable requirements.'
        },
        {
            num: '06',
            title: 'EDP Training',
            desc: 'Beneficiaries complete Entrepreneurship Development Programme training as required under the applicable process.'
        },
        {
            num: '07',
            title: 'Subsidy Disbursement',
            desc: 'After completion of applicable requirements, the margin money subsidy is processed according to the scheme and banking guidelines.'
        }
    ];

    return (
        <div className="pmegp-page">

            {/* HERO */}
            <section
                className="pmegp-hero"
                aria-labelledby="pmegp-hero-title"
            >

                <div className="pmegp-container">

                    <span className="pmegp-badge">
                        Business &amp; Entrepreneurship
                    </span>

                    <h1 id="pmegp-hero-title">
                        Prime Minister&apos;s Employment Generation Programme
                    </h1>

                    <p className="pmegp-hero-full-name">
                        PMEGP — Credit-Linked Subsidy Scheme
                    </p>

                    <p className="pmegp-hero-desc">
                        A major credit-linked subsidy scheme facilitating
                        self-employment through the establishment of
                        micro-enterprises in the non-farm sector.
                    </p>

                    <div className="pmegp-hero-actions">

                        <a
                            href="#pmegp-process"
                            className="btn btn-primary"
                        >
                            How to Apply
                        </a>

                        <Link
                            to="/eligibility?scheme=PMEGP"
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


            <main className="pmegp-main-container">

                {/* OVERVIEW + QUICK FACTS */}
                <section
                    className="pmegp-overview-section"
                    id="pmegp-overview"
                    aria-labelledby="pmegp-overview-title"
                >

                    <div className="pmegp-overview-grid">

                        <div className="pmegp-overview-text">

                            <h2 id="pmegp-overview-title">
                                What is PMEGP?
                            </h2>

                            <p>
                                The Prime Minister&apos;s Employment Generation
                                Programme (PMEGP) is a credit-linked subsidy
                                scheme aimed at supporting self-employment
                                through establishment of eligible
                                micro-enterprises.
                            </p>

                            <p>
                                The scheme supports entrepreneurs through a
                                structured process involving the applicant,
                                implementing agencies, and participating banks.
                            </p>

                            <p>
                                Under the DSGP demonstration platform,
                                beneficiaries can be checked against the
                                configured PMEGP eligibility conditions before
                                proceeding with the application flow.
                            </p>

                            <div className="pmegp-notice pmegp-notice--info">

                                <span aria-hidden="true">
                                    ℹ️
                                </span>

                                <div>

                                    <strong>
                                        About This Page:
                                    </strong>{' '}
                                    This page provides general information
                                    about PMEGP for the DSGP academic
                                    demonstration platform. Current official
                                    PMEGP guidelines should always be checked
                                    before applying.

                                </div>

                            </div>

                        </div>


                        <aside
                            className="pmegp-quick-facts"
                            aria-label="PMEGP Quick Facts"
                        >

                            <h3>
                                Quick Facts
                            </h3>

                            {quickFacts.map((fact, index) => (

                                <div
                                    key={index}
                                    className="pmegp-fact-row"
                                >

                                    <span className="pmegp-fact-label">
                                        {fact.label}
                                    </span>

                                    <span className="pmegp-fact-value">
                                        {fact.value}
                                    </span>

                                </div>

                            ))}

                        </aside>

                    </div>

                </section>


                {/* OBJECTIVES */}
                <section
                    className="pmegp-section"
                    id="pmegp-objectives"
                    aria-labelledby="pmegp-obj-title"
                >

                    <h2
                        id="pmegp-obj-title"
                        className="pmegp-section-title"
                    >
                        Objectives of PMEGP
                    </h2>

                    <p className="pmegp-section-subtitle">
                        The scheme addresses unemployment through
                        self-employment by supporting micro-enterprise creation.
                    </p>

                    <div className="pmegp-cards-grid">

                        {objectives.map((objective, index) => (

                            <div
                                key={index}
                                className="pmegp-objective-card"
                            >

                                <div
                                    className="pmegp-card-icon"
                                    aria-hidden="true"
                                >
                                    {objective.icon}
                                </div>

                                <h3>
                                    {objective.title}
                                </h3>

                                <p>
                                    {objective.desc}
                                </p>

                            </div>

                        ))}

                    </div>

                </section>


                {/* WHO CAN APPLY */}
                <section
                    className="pmegp-section pmegp-section--alt"
                    id="pmegp-eligibility"
                    aria-labelledby="pmegp-elig-title"
                >

                    <h2
                        id="pmegp-elig-title"
                        className="pmegp-section-title"
                    >
                        Who Can Apply
                    </h2>

                    <p className="pmegp-section-subtitle">
                        PMEGP is open to applicants meeting the applicable
                        eligibility criteria.
                    </p>

                    <ul
                        className="pmegp-eligibility-grid"
                        aria-label="Eligibility criteria"
                    >

                        {eligibilityItems.map((item, index) => (

                            <li
                                key={index}
                                className="pmegp-eligibility-item"
                            >

                                <span
                                    className="pmegp-eligibility-icon"
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


                    <div className="pmegp-notice pmegp-notice--warning">

                        <span aria-hidden="true">
                            ⚠️
                        </span>

                        <div>

                            <strong>
                                Note:
                            </strong>{' '}
                            PMEGP eligibility criteria, project cost limits,
                            subsidy rates, and other conditions are defined by
                            the applicable guidelines and may be revised.

                        </div>

                    </div>

                </section>


                {/* BENEFITS */}
                <section
                    className="pmegp-section"
                    id="pmegp-benefits"
                    aria-labelledby="pmegp-benefits-title"
                >

                    <h2
                        id="pmegp-benefits-title"
                        className="pmegp-section-title"
                    >
                        Scheme Benefits
                    </h2>

                    <p className="pmegp-section-subtitle">
                        PMEGP provides margin money subsidy on eligible
                        projects, with applicable rates and conditions.
                    </p>

                    <div className="pmegp-benefits-grid">

                        {benefits.map((benefit, index) => (

                            <div
                                key={index}
                                className="pmegp-benefit-card"
                            >

                                <div
                                    className="pmegp-benefit-icon"
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

                            </div>

                        ))}

                    </div>

                </section>


                {/* REQUIRED DOCUMENTS */}
                <section
                    className="pmegp-section pmegp-section--alt"
                    id="pmegp-documents"
                    aria-labelledby="pmegp-docs-title"
                >

                    <h2
                        id="pmegp-docs-title"
                        className="pmegp-section-title"
                    >
                        Required Documents
                    </h2>

                    <p className="pmegp-section-subtitle">
                        Prepare the following documents for your PMEGP
                        application.
                    </p>

                    <ul
                        className="pmegp-doc-list"
                        aria-label="Required documents"
                    >

                        {documents.map((document, index) => (

                            <li
                                key={index}
                                className="pmegp-doc-item"
                            >

                                <span
                                    className="pmegp-doc-icon"
                                    aria-hidden="true"
                                >
                                    {document.icon}
                                </span>

                                <div>

                                    <div className="pmegp-doc-label">
                                        {document.label}
                                    </div>

                                    <div className="pmegp-doc-note">
                                        {document.note}
                                    </div>

                                </div>

                            </li>

                        ))}

                    </ul>

                </section>


                {/* APPLICATION PROCESS */}
                <section
                    className="pmegp-section"
                    id="pmegp-process"
                    aria-labelledby="pmegp-process-title"
                >

                    <h2
                        id="pmegp-process-title"
                        className="pmegp-section-title"
                    >
                        Application Process
                    </h2>

                    <p className="pmegp-section-subtitle">
                        PMEGP follows a structured process involving the
                        applicant, implementing agency, and bank.
                    </p>

                    <ol
                        className="pmegp-steps-list"
                        aria-label="PMEGP application steps"
                    >

                        {steps.map((step, index) => (

                            <li
                                key={index}
                                className="pmegp-step-item"
                            >

                                <div
                                    className="pmegp-step-number"
                                    aria-hidden="true"
                                >
                                    {step.num}
                                </div>

                                <div className="pmegp-step-body">

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
                    className="pmegp-section pmegp-section--alt"
                    aria-labelledby="pmegp-important-title"
                >

                    <h2
                        id="pmegp-important-title"
                        className="pmegp-section-title"
                    >
                        Important Information
                    </h2>

                    <div className="pmegp-notices-grid">

                        <div className="pmegp-notice pmegp-notice--warning">

                            <span aria-hidden="true">
                                ⚠️
                            </span>

                            <div>

                                <strong>
                                    New Projects Only:
                                </strong>{' '}
                                PMEGP supports new project establishments
                                subject to the applicable guidelines.

                            </div>

                        </div>


                        <div className="pmegp-notice pmegp-notice--info">

                            <span aria-hidden="true">
                                ℹ️
                            </span>

                            <div>

                                <strong>
                                    One PMEGP Benefit Per Family:
                                </strong>{' '}
                                Applicable family-level restrictions must be
                                followed according to the current scheme
                                guidelines.

                            </div>

                        </div>


                        <div className="pmegp-notice pmegp-notice--success">

                            <span aria-hidden="true">
                                ✅
                            </span>

                            <div>

                                <strong>
                                    Locate Your Nearest Office:
                                </strong>{' '}
                                Visit the appropriate District Industries
                                Centre or KVIC office for application
                                assistance and project guidance.

                            </div>

                        </div>


                        <div className="pmegp-notice pmegp-notice--danger">

                            <span aria-hidden="true">
                                🚫
                            </span>

                            <div>

                                <strong>
                                    No Fee for Application:
                                </strong>{' '}
                                Be cautious of unauthorised agents charging
                                fees for government application services.

                            </div>

                        </div>

                    </div>

                </section>


                {/* CTA */}
                <section
                    className="pmegp-cta"
                    aria-labelledby="pmegp-cta-title"
                >

                    <h2 id="pmegp-cta-title">
                        Start Your Entrepreneurship Journey
                    </h2>

                    <p>
                        Check your eligibility and continue with the DSGP
                        application flow.
                    </p>

                    <div className="pmegp-cta-actions">

                        <Link
                            to="/eligibility?scheme=PMEGP"
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

                    <p className="pmegp-cta-note">
                        ⓘ PMEGP applications involve the applicable
                        implementing agencies and participating banks.
                        DSGP is an academic demonstration platform for
                        beneficiary registration, eligibility checking,
                        verification, and application tracking.
                    </p>

                </section>

            </main>

        </div>
    );
}

export default PMEGP;