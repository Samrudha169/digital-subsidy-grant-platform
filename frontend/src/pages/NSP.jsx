import { Link } from 'react-router-dom';
import './NSP.css';

function NSP() {
    const quickFacts = [
        { label: 'Scheme Type', value: 'Scholarship' },
        { label: 'Ministry', value: 'Multiple Ministries' },
        { label: 'Level', value: 'Pre-matric to Post-doctoral' },
        { label: 'Transfer Mode', value: 'Direct Benefit Transfer (DBT)' },
        { label: 'Identity Required', value: 'Aadhaar' },
        { label: 'Application Mode', value: 'Online Only' },
        { label: 'Categories', value: 'SC / ST / OBC / Minority / General' },
    ];

    const eligibilityItems = [
        {
            icon: '🏫',
            title: 'Pre-Matric Students',
            desc: 'Students studying in Class 1 to Class 10 in recognised schools.',
        },
        {
            icon: '📖',
            title: 'Post-Matric Students',
            desc: 'Students studying in Class 11, Class 12, or equivalent.',
        },
        {
            icon: '🎓',
            title: 'Undergraduate Students',
            desc: "Students enrolled in recognised colleges or universities for bachelor's degree programmes.",
        },
        {
            icon: '🔬',
            title: 'Postgraduate & Research',
            desc: "Students pursuing master's, M.Phil., or doctoral / post-doctoral research.",
        },
        {
            icon: '♿',
            title: 'Students with Disabilities',
            desc: 'Students with a valid disability certificate as per government norms.',
        },
        {
            icon: '🌾',
            title: 'Economically Weaker Sections',
            desc: 'Students from families with income within prescribed limits under the applicable scheme.',
        },
        {
            icon: '🕌',
            title: 'Minority Community Students',
            desc: 'Students belonging to notified minority communities (Muslim, Christian, Sikh, Buddhist, Jain, Zoroastrian).',
        },
        {
            icon: '📋',
            title: 'SC / ST / OBC Students',
            desc: 'Students belonging to Scheduled Castes, Scheduled Tribes, or Other Backward Classes as certified.',
        },
    ];

    const benefits = [
        { icon: '💰', title: 'Tuition Fee Support', desc: 'Covers full or partial tuition fees as applicable under the specific scheme and institution category.' },
        { icon: '🏠', title: 'Maintenance Allowance', desc: 'Monthly/annual maintenance allowance to support living and boarding expenses during the academic year.' },
        { icon: '📦', title: 'Study Materials', desc: 'Allowance for books, stationery, and other academic materials under applicable scholarship schemes.' },
        { icon: '🏦', title: 'Direct Bank Transfer', desc: "All scholarship amounts are transferred directly to the student's Aadhaar-linked bank account via DBT." },
        { icon: '🔄', title: 'Renewal Option', desc: 'Eligible students can renew their scholarship each academic year, subject to satisfactory academic progress.' },
        { icon: '📊', title: 'Status Tracking', desc: 'Real-time application tracking — know exactly where your scholarship application stands at every stage.' },
    ];

    const documents = [
        { icon: '🪪', label: 'Aadhaar Card', note: '12-digit Aadhaar number of the student; mandatory for DBT' },
        { icon: '📄', label: 'Income Certificate', note: 'Current year certificate issued by competent authority' },
        { icon: '🎓', label: 'Previous Year Marksheet', note: 'Last passed exam marksheet to demonstrate academic eligibility' },
        { icon: '🏦', label: 'Bank Passbook / Account Details', note: "Aadhaar-linked bank account (student's own account preferred)" },
        { icon: '📷', label: 'Passport-size Photograph', note: 'Recent colour photograph in prescribed format' },
        { icon: '🏫', label: 'School / College Bonafide Certificate', note: 'Issued by the current institution confirming enrolment' },
        { icon: '📋', label: 'Category Certificate', note: 'SC/ST/OBC/Minority/Disability certificate as applicable' },
        { icon: '🏠', label: 'Domicile Certificate', note: 'State domicile certificate (required for state-specific schemes)' },
        { icon: '💳', label: 'Fee Receipt', note: 'Receipt of fees paid to current institution for the academic year' },
    ];

    const steps = [
        { num: '01', title: 'Register as a Beneficiary', desc: 'Register on the DSGP portal with your personal details including Aadhaar number, date of birth, mobile number, address, and category.' },
        { num: '02', title: 'Select the Applicable Scheme', desc: 'Browse available NSP scholarship schemes applicable to your category, education level, and state. Read eligibility criteria carefully.' },
        { num: '03', title: 'Fill the Application Form', desc: 'Complete the online application form with accurate personal, academic, and financial details. Double-check all fields before proceeding.' },
        { num: '04', title: 'Upload Required Documents', desc: 'Upload scanned copies of all required documents in the prescribed format and size. Ensure every document is clear and readable.' },
        { num: '05', title: 'Institute Verification', desc: 'Your application must be verified and forwarded by your school or college nodal officer through the portal.' },
        { num: '06', title: 'District / State Level Processing', desc: 'Verified applications are reviewed at the district and state level. Approved applications receive scholarship amounts via DBT.' },
    ];

    return (
        <div className="nsp-page">

            {/* Hero */}
            <section className="nsp-hero" aria-labelledby="nsp-hero-title">
                <div className="nsp-container">
                    <span className="nsp-badge">Education</span>
                    <h1 id="nsp-hero-title">National Scholarship Portal</h1>
                    <p className="nsp-hero-full-name">NSP — Unified Scholarship Platform</p>
                    <p className="nsp-hero-desc">
                        A unified platform for all government scholarship schemes — enabling eligible students
                        across India to apply, track, and receive scholarships through Direct Benefit Transfer.
                    </p>
                    <div className="nsp-hero-actions">
                        <a href="#nsp-process" className="btn btn-primary">How to Apply</a>
                        <Link to="/schemes" className="btn btn-secondary">Back to Schemes</Link>
                    </div>
                </div>
            </section>

            <main className="nsp-main-container">

                {/* Overview + Quick Facts */}
                <section className="nsp-overview-section" id="nsp-overview" aria-labelledby="nsp-overview-title">
                    <div className="nsp-overview-grid">
                        <div className="nsp-overview-text">
                            <h2 id="nsp-overview-title">What is the National Scholarship Portal?</h2>
                            <p>
                                The National Scholarship Portal (NSP) is a Government of India initiative under the
                                Digital India Programme, bringing all Central and State scholarship schemes onto a
                                single, unified platform.
                            </p>
                            <p>
                                NSP enables eligible students to apply online, track their application status, and
                                receive scholarship funds directly into their bank accounts through the Direct Benefit
                                Transfer (DBT) mechanism — eliminating middlemen and ensuring transparent delivery.
                            </p>
                            <p>
                                The portal covers scholarships for students belonging to minority communities,
                                SC/ST categories, OBC, economically weaker sections, students with disabilities,
                                and meritorious students at all levels of education — from pre-matric to post-doctoral.
                            </p>
                            <div className="nsp-notice nsp-notice--info">
                                <span aria-hidden="true">ℹ️</span>
                                <div>
                                    <strong>About This Page:</strong> This page provides general information about
                                    the NSP scheme. Actual scholarship applications are submitted through the official
                                    NSP portal at <em>scholarships.gov.in</em>.
                                </div>
                            </div>
                        </div>

                        <aside className="nsp-quick-facts" aria-label="NSP Quick Facts">
                            <h3>Quick Facts</h3>
                            {quickFacts.map((fact, i) => (
                                <div key={i} className="nsp-fact-row">
                                    <span className="nsp-fact-label">{fact.label}</span>
                                    <span className="nsp-fact-value">{fact.value}</span>
                                </div>
                            ))}
                        </aside>
                    </div>
                </section>

                {/* Who Can Apply */}
                <section className="nsp-section nsp-section--alt" id="nsp-eligibility" aria-labelledby="nsp-elig-title">
                    <h2 id="nsp-elig-title" className="nsp-section-title">Who Can Apply</h2>
                    <p className="nsp-section-subtitle">
                        NSP scholarships are available to students from diverse backgrounds and education levels across India.
                    </p>
                    <ul className="nsp-eligibility-grid" aria-label="Eligibility criteria">
                        {eligibilityItems.map((item, i) => (
                            <li key={i} className="nsp-eligibility-item">
                                <span className="nsp-eligibility-icon" aria-hidden="true">{item.icon}</span>
                                <div>
                                    <strong>{item.title}</strong>
                                    <p>{item.desc}</p>
                                </div>
                            </li>
                        ))}
                    </ul>
                    <div className="nsp-notice nsp-notice--warning">
                        <span aria-hidden="true">⚠️</span>
                        <div>
                            <strong>Important:</strong> Eligibility criteria — including income limits, academic
                            percentage requirements, and age limits — vary by specific scholarship scheme.
                            Applicants must verify eligibility against the specific scheme on the official NSP portal.
                        </div>
                    </div>
                </section>

                {/* Benefits */}
                <section className="nsp-section" id="nsp-benefits" aria-labelledby="nsp-benefits-title">
                    <h2 id="nsp-benefits-title" className="nsp-section-title">Scholarship Benefits</h2>
                    <p className="nsp-section-subtitle">
                        NSP scholarships provide financial support covering tuition, maintenance, and other
                        educational expenses — delivered directly to the student's bank account.
                    </p>
                    <ul className="nsp-benefits-grid" aria-label="Benefits">
                        {benefits.map((b, i) => (
                            <li key={i} className="nsp-benefit-card">
                                <div className="nsp-benefit-icon" aria-hidden="true">{b.icon}</div>
                                <h3>{b.title}</h3>
                                <p>{b.desc}</p>
                            </li>
                        ))}
                    </ul>
                </section>

                {/* Required Documents */}
                <section className="nsp-section nsp-section--alt" id="nsp-documents" aria-labelledby="nsp-docs-title">
                    <h2 id="nsp-docs-title" className="nsp-section-title">Required Documents</h2>
                    <p className="nsp-section-subtitle">
                        Prepare the following documents before beginning your NSP scholarship application.
                    </p>
                    <ul className="nsp-doc-list" aria-label="Required documents">
                        {documents.map((doc, i) => (
                            <li key={i} className="nsp-doc-item">
                                <span className="nsp-doc-icon" aria-hidden="true">{doc.icon}</span>
                                <div>
                                    <div className="nsp-doc-label">{doc.label}</div>
                                    <div className="nsp-doc-note">{doc.note}</div>
                                </div>
                            </li>
                        ))}
                    </ul>
                </section>

                {/* Application Process */}
                <section className="nsp-section" id="nsp-process" aria-labelledby="nsp-process-title">
                    <h2 id="nsp-process-title" className="nsp-section-title">Application Process</h2>
                    <p className="nsp-section-subtitle">
                        Follow these steps to complete your NSP scholarship application.
                    </p>
                    <ol className="nsp-steps-list" aria-label="Application steps">
                        {steps.map((step, i) => (
                            <li key={i} className="nsp-step-item">
                                <div className="nsp-step-number" aria-hidden="true">{step.num}</div>
                                <div className="nsp-step-body">
                                    <h3>{step.title}</h3>
                                    <p>{step.desc}</p>
                                </div>
                            </li>
                        ))}
                    </ol>
                </section>

                {/* Important Information */}
                <section className="nsp-section nsp-section--alt" aria-labelledby="nsp-important-title">
                    <h2 id="nsp-important-title" className="nsp-section-title">Important Information</h2>
                    <div className="nsp-notices-grid">
                        <div className="nsp-notice nsp-notice--warning">
                            <span aria-hidden="true">⚠️</span>
                            <div>
                                <strong>One Application Per Student:</strong> A student cannot apply for more than
                                one scholarship at the same level under NSP in the same academic year.
                                Duplicate applications will be rejected.
                            </div>
                        </div>
                        <div className="nsp-notice nsp-notice--info">
                            <span aria-hidden="true">ℹ️</span>
                            <div>
                                <strong>Aadhaar Mandatory:</strong> Aadhaar number is mandatory for all NSP
                                applications. The bank account used for DBT must be seeded with the same
                                Aadhaar number.
                            </div>
                        </div>
                        <div className="nsp-notice nsp-notice--danger">
                            <span aria-hidden="true">🚫</span>
                            <div>
                                <strong>Beware of Fraudsters:</strong> The Government of India does not charge any
                                fee for NSP scholarship applications. Beware of agents claiming to process
                                applications for a fee.
                            </div>
                        </div>
                        <div className="nsp-notice nsp-notice--success">
                            <span aria-hidden="true">✅</span>
                            <div>
                                <strong>Keep Records:</strong> Save your application number and acknowledgement
                                receipt. These will be required for tracking status and future renewals.
                            </div>
                        </div>
                    </div>
                </section>

                {/* CTA */}
                <section className="nsp-cta" aria-labelledby="nsp-cta-title">
                    <h2 id="nsp-cta-title">Ready to Get Started?</h2>
                    <p>Check your eligibility and explore other government schemes available on DSGP.</p>
                    <div className="nsp-cta-actions">
                        <Link to="/eligibility" className="btn btn-primary">Check Eligibility</Link>
                        <Link to="/schemes" className="btn btn-secondary">All Schemes</Link>
                    </div>
                    <p className="nsp-cta-note">
                        ⓘ Actual scholarship applications are submitted through the official NSP portal.
                        DSGP manages beneficiary registration and disbursement tracking for authorised officers.
                    </p>
                </section>

            </main>
        </div>
    );
}

export default NSP;
