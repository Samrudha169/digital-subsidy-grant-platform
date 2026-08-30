import { useState } from 'react';
import { Link } from 'react-router-dom';
import './FAQ.css';

const FAQ_DATA = [
    {
        category: 'General',
        items: [
            {
                q: 'What is the Digital Subsidy & Grant Administration Platform (DSGP)?',
                a: 'DSGP is a government enterprise portal designed to manage the complete lifecycle of subsidy and grant schemes — from beneficiary registration and document upload to eligibility assessment, disbursement, and compliance tracking. It serves as a unified platform for field officers, district officers, and finance approvers.',
            },
            {
                q: 'Which government schemes does DSGP manage?',
                a: 'In the current phase, DSGP provides information and beneficiary management for two major schemes: the National Scholarship Portal (NSP) for students, and the Prime Minister\'s Employment Generation Programme (PMEGP) for entrepreneurs. Additional schemes will be added in future phases.',
            },
            {
                q: 'Who can use the DSGP portal?',
                a: 'DSGP is used by authorised government personnel — Field Officers (beneficiary registration and document upload), District Officers (verification and approval), Finance Approvers (disbursement), and Administrators (system configuration). Citizens access scheme information publicly.',
            },
            {
                q: 'Is DSGP a government-official portal?',
                a: 'DSGP is an academic demonstration project designed to showcase a unified digital platform for government subsidy and grant administration. For official scheme information and actual applications, please use the respective official government portals such as scholarships.gov.in for NSP and kviconline.gov.in for PMEGP.',
            },
        ],
    },
    {
        category: 'Registration',
        items: [
            {
                q: 'How do I register a beneficiary in DSGP?',
                a: 'Field Officers can register beneficiaries through the DSGP portal by navigating to the Beneficiary Registration section. You will need to provide the beneficiary\'s personal details (name, date of birth, gender), identity (Aadhaar, mobile number), address, financial information (income, land holding), and category (SC/ST/OBC/General).',
            },
            {
                q: 'What information is required to register a beneficiary?',
                a: 'Required information includes: First name, Last name, Date of birth, Gender, Aadhaar number (12-digit, unique), Mobile number (10-digit, unique), Full address including district, state and PIN code, Annual income, Land holding in acres, and Social category (General / OBC / SC / ST).',
            },
            {
                q: 'Can I update a beneficiary\'s information after registration?',
                a: 'Most fields can be updated by authorised officers after registration. However, the Aadhaar number is immutable once registered — it cannot be changed as it serves as the unique identity anchor for the beneficiary record. If there is an Aadhaar error, contact your system administrator.',
            },
            {
                q: 'What happens when a beneficiary registration is deleted?',
                a: 'DSGP implements soft deletion — deleted records are flagged as deleted but retained in the database for audit purposes. They are removed from all active views and API responses. Hard deletion is not supported to maintain data integrity and audit trails.',
            },
        ],
    },
    {
        category: 'Schemes & Eligibility',
        items: [
            {
                q: 'How do I check scheme eligibility for a beneficiary?',
                a: 'In the current DSGP phase, eligibility is assessed manually by District Officers based on the beneficiary\'s registered data — income, category, land holding, age, and district. Automated eligibility scoring is planned for a future implementation phase.',
            },
            {
                q: 'What is NSP and who is eligible?',
                a: 'NSP (National Scholarship Portal) is a unified scholarship scheme for students from pre-matric to post-doctoral levels. It covers students from SC, ST, OBC, minority communities, economically weaker sections, and students with disabilities. Eligibility depends on the specific scholarship scheme within NSP. Visit the NSP page for detailed eligibility information.',
            },
            {
                q: 'What is PMEGP and who can apply?',
                a: 'PMEGP (Prime Minister\'s Employment Generation Programme) is a credit-linked subsidy scheme for establishing new micro-enterprises. Any Indian citizen aged 18+ may apply (for some projects, an educational qualification may be required). Self-Help Groups, societies, and production cooperatives are also eligible. Existing enterprises that have already received government subsidy are not eligible.',
            },
        ],
    },
    {
        category: 'Documents',
        items: [
            {
                q: 'What documents may be required for scheme applications?',
                a: 'Required documents vary by scheme. Commonly required documents include: Aadhaar card, PAN card, Income certificate, Land ownership records (for agricultural schemes), School / College bonafide certificate (for NSP), Category certificate (SC/ST/OBC/Minority), Passport-size photograph, Bank account details / passbook, and Detailed Project Report (for PMEGP).',
            },
            {
                q: 'What file formats and sizes are accepted for document uploads?',
                a: 'DSGP accepts PDF, JPEG, and PNG files. The maximum size per document is 5 MB, and the maximum total request size is 10 MB. All uploaded documents must be clear, complete, and fully readable.',
            },
        ],
    },
    {
        category: 'Application Status & Support',
        items: [
            {
                q: 'How do I track my beneficiary registration status?',
                a: 'Each beneficiary registration has one of three statuses: PENDING (registered but identity not yet verified), ACTIVE (identity verified, eligible to proceed), or SUSPENDED (registration suspended due to discrepancies or compliance issues). Officers can view and filter beneficiaries by status through the portal dashboard.',
            },
            {
                q: 'What happens after a beneficiary is registered?',
                a: 'After registration, the beneficiary\'s status is PENDING. Field Officers upload required supporting documents. An authorised District Officer then reviews the documents and marks the identity as verified, changing the status to ACTIVE. Active beneficiaries can proceed with scheme application workflows.',
            },
            {
                q: 'What should I do if the beneficiary\'s information is incorrect?',
                a: 'If the information is incorrect (other than Aadhaar), log in as an authorised officer and edit the beneficiary record with the correct information. For Aadhaar-related issues, contact your system administrator. Always verify information with original documents before updating.',
            },
            {
                q: 'How can I get help if I have an issue with the portal?',
                a: 'For portal usage help, refer to the Help & Guidance page. For technical support, submit a request through the Contact page. You can also reach our support team at support@dsgp.gov.in or call the helpline (Mon–Fri, 9 AM–6 PM IST).',
            },
            {
                q: 'Is beneficiary data stored securely in DSGP?',
                a: 'Yes. DSGP uses a structured, audit-logged database. All registration and modification actions are timestamped. Sensitive data (Aadhaar, income) is managed in accordance with data handling policies. Deletions are soft-deletes that preserve audit records.',
            },
            {
                q: 'How do I report a technical issue or a bug in the portal?',
                a: 'Use the Contact page to submit a technical issue report. Include a description of the problem, the steps to reproduce it, any error messages displayed, and the browser and device you are using. Our technical team will investigate and respond within 2 working days.',
            },
        ],
    },
];

function FAQItem({ item }) {
    const [isOpen, setIsOpen] = useState(false);
    const id = `faq-answer-${item.q.replace(/\s+/g, '-').replace(/[^a-zA-Z0-9-]/g, '').substring(0, 30)}`;

    const toggle = () => setIsOpen((prev) => !prev);

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            toggle();
        }
    };

    return (
        <div className={`faq-item ${isOpen ? 'faq-item--open' : ''}`}>
            <button
                className="faq-question"
                onClick={toggle}
                onKeyDown={handleKeyDown}
                aria-expanded={isOpen}
                aria-controls={id}
            >
                <span className="faq-question-text">{item.q}</span>
                <span className="faq-chevron" aria-hidden="true">
                    {isOpen ? '▲' : '▼'}
                </span>
            </button>
            <div
                id={id}
                className="faq-answer"
                hidden={!isOpen}
                role="region"
                aria-label={item.q}
            >
                <div className="faq-answer-inner">{item.a}</div>
            </div>
        </div>
    );
}

function FAQ() {
    return (
        <div className="faq-page">

            {/* Hero */}
            <section className="faq-hero" aria-labelledby="faq-hero-title">
                <div className="faq-hero-inner">
                    <h1 id="faq-hero-title">Frequently Asked Questions</h1>
                    <p>
                        Find answers to the most common questions about DSGP, beneficiary registration,
                        scheme eligibility, documents, and support.
                    </p>
                </div>
            </section>

            {/* FAQ Categories */}
            <main className="faq-main">
                <div className="faq-container">
                    {FAQ_DATA.map((category) => (
                        <section
                            key={category.category}
                            className="faq-category"
                            aria-labelledby={`faq-cat-${category.category.replace(/\s+/g, '-')}`}
                        >
                            <h2
                                id={`faq-cat-${category.category.replace(/\s+/g, '-')}`}
                                className="faq-category-title"
                            >
                                {category.category}
                            </h2>
                            <div className="faq-list">
                                {category.items.map((item, i) => (
                                    <FAQItem key={i} item={item} />
                                ))}
                            </div>
                        </section>
                    ))}

                    {/* Bottom CTA */}
                    <div className="faq-bottom-cta" aria-label="Additional support options">
                        <h2>Didn&apos;t Find Your Answer?</h2>
                        <p>Our support team is ready to help with any questions not covered here.</p>
                        <div className="faq-cta-actions">
                            <Link to="/contact" className="btn btn-primary">Contact Support</Link>
                            <Link to="/help" className="btn btn-secondary">Help &amp; Guidance</Link>
                        </div>
                    </div>
                </div>
            </main>

        </div>
    );
}

export default FAQ;
