import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Help.css';

const ARTICLES = [
    {
        id: 'getting-started',
        icon: '🚀',
        title: 'Getting Started with DSGP',
        content: (
            <>
                <p>
                    The <strong>Digital Subsidy &amp; Grant Administration Platform (DSGP)</strong> is a
                    government enterprise portal designed to manage the complete lifecycle of subsidy and grant
                    schemes — from beneficiary registration to disbursement and compliance tracking.
                </p>
                <h3 className="help-sub-heading">Who Uses DSGP?</h3>
                <ul>
                    <li><strong>Field Officers</strong> — Register beneficiaries, upload documents, and conduct ground-level verification.</li>
                    <li><strong>District Officers</strong> — Review applications, approve or escalate cases, and manage district-level workflows.</li>
                    <li><strong>Finance Approvers</strong> — Review and approve fund disbursement for eligible beneficiaries.</li>
                    <li><strong>Administrators</strong> — Manage system configuration, scheme setup, and user access.</li>
                </ul>
                <h3 className="help-sub-heading">What Can You Do on DSGP?</h3>
                <ul>
                    <li>Register new beneficiaries with full personal, demographic, and financial details</li>
                    <li>Upload and manage supporting documents (Aadhaar, PAN, Income Certificate, etc.)</li>
                    <li>Track registration status (Pending / Active / Suspended)</li>
                    <li>Filter and manage beneficiaries by district and status</li>
                    <li>Mark identities as verified after field verification</li>
                    <li>Browse information about NSP and PMEGP schemes</li>
                </ul>
                <div className="help-notice help-notice--info">
                    <span aria-hidden="true">ℹ️</span>
                    <div>
                        <strong>Platform Phase:</strong> DSGP is currently in the Beneficiary &amp; Scheme Master Data phase.
                        Eligibility scoring, application submission, disbursement, and analytics features are planned for future phases.
                    </div>
                </div>
            </>
        ),
    },
    {
        id: 'registration-help',
        icon: '📝',
        title: 'Beneficiary Registration',
        content: (
            <>
                <p>
                    Beneficiary registration is performed by <strong>Field Officers</strong> through the DSGP
                    portal after meeting the beneficiary in person.
                </p>
                <h3 className="help-sub-heading">Information Required</h3>
                <ul>
                    <li><strong>Personal:</strong> First name, last name, date of birth, gender</li>
                    <li><strong>Identity:</strong> Aadhaar number (12 digits, unique), mobile number (10 digits, unique)</li>
                    <li><strong>Address:</strong> Full address, village, taluka, district, state, PIN code</li>
                    <li><strong>Financial:</strong> Annual income, land holding (in acres)</li>
                    <li><strong>Category:</strong> General / OBC / SC / ST</li>
                </ul>
                <h3 className="help-sub-heading">Registration Steps</h3>
                <ol>
                    <li>Login to DSGP with your Field Officer credentials.</li>
                    <li>Navigate to the Beneficiary Registration section.</li>
                    <li>Fill all required fields accurately.</li>
                    <li>Verify the Aadhaar number carefully — it cannot be changed after registration.</li>
                    <li>Submit the form. A unique Beneficiary ID is generated on success.</li>
                    <li>Note down the Beneficiary ID for all future reference.</li>
                </ol>
                <div className="help-notice help-notice--warning">
                    <span aria-hidden="true">⚠️</span>
                    <div>
                        <strong>Aadhaar Must Be Unique:</strong> Each Aadhaar number can only be registered once in DSGP.
                        Duplicate registrations will be rejected.
                    </div>
                </div>
            </>
        ),
    },
    {
        id: 'schemes-help',
        icon: '📚',
        title: 'Browsing Government Schemes',
        content: (
            <>
                <p>DSGP currently provides detailed information about two major government schemes:</p>
                <ul>
                    <li>
                        <strong><Link to="/schemes/nsp">National Scholarship Portal (NSP)</Link></strong> —
                        A unified scholarship scheme for students from pre-matric to post-doctoral levels,
                        covering SC/ST, OBC, minority, and economically weaker sections.
                    </li>
                    <li>
                        <strong><Link to="/schemes/pmegp">PMEGP — Prime Minister&apos;s Employment Generation Programme</Link></strong> —
                        A credit-linked subsidy scheme for establishing new micro-enterprises in the manufacturing and service sectors.
                    </li>
                </ul>
                <p>
                    Each scheme page provides complete information on eligibility, benefits, required documents,
                    and the application process. Additional schemes will be added in future DSGP phases.
                </p>
            </>
        ),
    },
    {
        id: 'eligibility-help',
        icon: '✅',
        title: 'Eligibility Assessment',
        content: (
            <>
                <p>
                    Eligibility scoring is a planned module in DSGP. In the current phase, eligibility is
                    assessed manually by District Officers and Field Officers based on the beneficiary&apos;s
                    registered information.
                </p>
                <p>Key data points used in eligibility assessment:</p>
                <ul>
                    <li><strong>Annual Income</strong> — Whether the beneficiary falls within scheme income limits</li>
                    <li><strong>Category (SC/ST/OBC/General)</strong> — Determines applicable scheme components and subsidy rates</li>
                    <li><strong>Land Holding</strong> — Relevant for agricultural and rural subsidy schemes</li>
                    <li><strong>Age</strong> — Some schemes have minimum or maximum age limits</li>
                    <li><strong>District/State</strong> — For state-specific or regionally targeted schemes</li>
                </ul>
                <p>
                    For scheme-specific eligibility requirements, refer to the dedicated{' '}
                    <Link to="/schemes/nsp">NSP</Link> and <Link to="/schemes/pmegp">PMEGP</Link> information pages.
                </p>
            </>
        ),
    },
    {
        id: 'document-help',
        icon: '📁',
        title: 'Document Upload',
        content: (
            <>
                <p>
                    Supporting documents can be uploaded for each registered beneficiary by authorised Field Officers.
                </p>
                <h3 className="help-sub-heading">Supported Document Types</h3>
                <ul>
                    <li><strong>AADHAAR</strong> — Aadhaar card (front and back)</li>
                    <li><strong>PAN</strong> — PAN card</li>
                    <li><strong>LAND_RECORD</strong> — Land holding certificate or land record</li>
                    <li><strong>INCOME_CERTIFICATE</strong> — Income certificate issued by competent authority</li>
                    <li><strong>PHOTO</strong> — Passport-size photograph</li>
                    <li><strong>OTHER</strong> — Category certificates, bank passbook, disability certificate, etc.</li>
                </ul>
                <h3 className="help-sub-heading">File Requirements</h3>
                <ul>
                    <li>Accepted formats: <strong>PDF, JPEG, PNG</strong></li>
                    <li>Maximum file size: <strong>5 MB per document</strong></li>
                    <li>Maximum request size: <strong>10 MB</strong></li>
                    <li>Documents must be <strong>clear, complete, and readable</strong></li>
                </ul>
                <div className="help-notice help-notice--info">
                    <span aria-hidden="true">ℹ️</span>
                    <div>
                        <strong>Document Verification:</strong> Uploaded documents are reviewed and can be marked
                        as verified by authorised District Officers. Unverified documents may delay eligibility processing.
                    </div>
                </div>
            </>
        ),
    },
    {
        id: 'status-help',
        icon: '🔍',
        title: 'Application Status Tracking',
        content: (
            <>
                <p>Each beneficiary registration in DSGP has a <strong>Registration Status</strong>:</p>
                <div className="help-status-list">
                    <div className="help-status-item help-status-item--warning">
                        <span className="help-status-badge">PENDING</span>
                        <p>Registered, but identity verification has not yet been completed. Documents may still be pending upload or review.</p>
                    </div>
                    <div className="help-status-item help-status-item--success">
                        <span className="help-status-badge">ACTIVE</span>
                        <p>Identity has been verified. The beneficiary is eligible to proceed with scheme application and disbursement workflows.</p>
                    </div>
                    <div className="help-status-item help-status-item--danger">
                        <span className="help-status-badge">SUSPENDED</span>
                        <p>Registration suspended due to document discrepancies, ineligibility, or compliance reasons.</p>
                    </div>
                </div>
                <p style={{ marginTop: '16px', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                    Full application-level status tracking is planned for a future implementation phase of DSGP.
                </p>
            </>
        ),
    },
    {
        id: 'problems-help',
        icon: '🛠️',
        title: 'Common Problems & Solutions',
        content: (
            <>
                <h3 className="help-sub-heading">Registration fails with &quot;Duplicate Aadhaar&quot; error</h3>
                <p>
                    A beneficiary with the same Aadhaar number is already registered. Search for the existing
                    beneficiary using the Aadhaar lookup and update their record instead.
                </p>
                <h3 className="help-sub-heading">Document upload fails</h3>
                <p>
                    Ensure the file is PDF, JPEG, or PNG and does not exceed 5 MB. Try re-scanning at a lower
                    resolution if the file size is too large.
                </p>
                <h3 className="help-sub-heading">Cannot find a beneficiary by name</h3>
                <p>
                    Search is currently available by Beneficiary ID, Aadhaar number, district, and registration status.
                    Use the Aadhaar number for the most reliable lookup.
                </p>
                <h3 className="help-sub-heading">Beneficiary status is stuck at PENDING</h3>
                <p>
                    A beneficiary remains PENDING until their identity is verified by an authorised officer.
                    Use the identity verification function to mark the beneficiary as verified.
                </p>
                <h3 className="help-sub-heading">Page does not load / server error</h3>
                <p>
                    Ensure the Spring Boot backend is running on port 8080 and the database is accessible.
                    Check application logs for detailed error messages.
                </p>
            </>
        ),
    },
    {
        id: 'support-help',
        icon: '📞',
        title: 'Contact & Support',
        content: (
            <>
                <p>If you need further assistance, please reach out through the following channels:</p>
                <div className="help-support-cards">
                    <div className="help-support-card">
                        <div className="help-support-icon" aria-hidden="true">📧</div>
                        <div>
                            <div className="help-support-label">Email Support</div>
                            <div className="help-support-value">support@dsgp.gov.in</div>
                            <div className="help-support-note">Response within 2 working days</div>
                        </div>
                    </div>
                    <div className="help-support-card">
                        <div className="help-support-icon" aria-hidden="true">📞</div>
                        <div>
                            <div className="help-support-label">Helpline</div>
                            <div className="help-support-value">1800-XXX-XXXX</div>
                            <div className="help-support-note">Mon – Fri, 9:00 AM – 6:00 PM IST</div>
                        </div>
                    </div>
                </div>
                <p style={{ marginTop: '20px' }}>
                    You can also submit a support request from the{' '}
                    <Link to="/contact">Contact page</Link>.
                    For portal-specific technical issues, refer to the{' '}
                    <Link to="/faq">FAQ page</Link>.
                </p>
            </>
        ),
    },
];

const QUICK_TOPICS = [
    { id: 'getting-started', icon: '🚀', title: 'Getting Started', desc: 'First-time user guide — understand what DSGP is and how to begin.' },
    { id: 'registration-help', icon: '📝', title: 'Registration', desc: 'How beneficiaries are registered and what information is needed.' },
    { id: 'eligibility-help', icon: '✅', title: 'Eligibility', desc: 'Understand how eligibility is assessed for different schemes.' },
    { id: 'document-help', icon: '📁', title: 'Documents', desc: 'Which documents are required and how to upload them correctly.' },
];

function Help() {
    const [activeId, setActiveId] = useState('getting-started');

    const activeArticle = ARTICLES.find((a) => a.id === activeId);

    const scrollToArticle = (id) => {
        setActiveId(id);
        // Let state update, then scroll to the content area
        setTimeout(() => {
            const el = document.getElementById('help-article-content');
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 50);
    };

    return (
        <div className="help-page">

            {/* Hero */}
            <section className="help-hero" aria-labelledby="help-hero-title">
                <div className="help-hero-inner">
                    <h1 id="help-hero-title">Help &amp; Guidance</h1>
                    <p>
                        Complete guidance for using the DSGP portal — from registration to document upload,
                        eligibility checking, and tracking application status.
                    </p>
                </div>
            </section>

            {/* Quick Topics */}
            <section className="help-quick-section" aria-labelledby="help-quick-title">
                <div className="help-container">
                    <h2 id="help-quick-title" className="help-section-title">Quick Help Topics</h2>
                    <div className="help-quick-grid">
                        {QUICK_TOPICS.map((t) => (
                            <button
                                key={t.id}
                                className="help-quick-card"
                                onClick={() => scrollToArticle(t.id)}
                            >
                                <div className="help-quick-icon" aria-hidden="true">{t.icon}</div>
                                <h3>{t.title}</h3>
                                <p>{t.desc}</p>
                            </button>
                        ))}
                    </div>
                </div>
            </section>

            {/* Main Content: Sidebar + Article */}
            <section className="help-content-section">
                <div className="help-container">
                    <div className="help-layout">

                        {/* Sidebar */}
                        <nav className="help-sidebar" aria-label="Help sections">
                            <p className="help-sidebar-title">On This Page</p>
                            <ul className="help-sidebar-list">
                                {ARTICLES.map((a) => (
                                    <li key={a.id}>
                                        <button
                                            className={`help-sidebar-link ${activeId === a.id ? 'is-active' : ''}`}
                                            onClick={() => setActiveId(a.id)}
                                            aria-current={activeId === a.id ? 'true' : undefined}
                                        >
                                            <span aria-hidden="true">{a.icon}</span>
                                            {a.title}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </nav>

                        {/* Article */}
                        <article
                            id="help-article-content"
                            className="help-article"
                            aria-label={activeArticle?.title}
                        >
                            {activeArticle && (
                                <>
                                    <h2 className="help-article-heading">
                                        <span aria-hidden="true">{activeArticle.icon}</span>
                                        {activeArticle.title}
                                    </h2>
                                    <div className="help-article-body">
                                        {activeArticle.content}
                                    </div>
                                </>
                            )}
                        </article>

                    </div>
                </div>
            </section>

            {/* CTA */}
            <section className="help-cta" aria-labelledby="help-cta-title">
                <div className="help-container">
                    <h2 id="help-cta-title">Still Need Help?</h2>
                    <p>Our support team is ready to help with any questions about DSGP.</p>
                    <div className="help-cta-actions">
                        <Link to="/contact" className="btn btn-primary">Contact Support</Link>
                        <Link to="/faq" className="btn btn-secondary">Read FAQs</Link>
                    </div>
                </div>
            </section>

        </div>
    );
}

export default Help;
