import { Link } from 'react-router-dom';
import './PMEGP.css';

function PMEGP() {
    const quickFacts = [
        { label: 'Scheme Type', value: 'Credit-Linked Subsidy' },
        { label: 'Ministry', value: 'MSME' },
        { label: 'Implementing Agency', value: 'KVIC / KVIB / DIC' },
        { label: 'Sector', value: 'Manufacturing / Service / Trade' },
        { label: 'Beneficiary', value: 'Individuals / SHGs / Institutions' },
        { label: 'Application', value: 'Online + Bank / DIC' },
        { label: 'Subsidy Type', value: 'Margin Money (% of project cost)' },
        { label: 'Priority Categories', value: 'General / SC / ST / OBC / Women / Ex-servicemen / Minority' },
    ];

    const objectives = [
        { icon: '💼', title: 'Self-Employment Creation', desc: 'Generate sustainable self-employment opportunities for unemployed youth and traditional artisans across India.' },
        { icon: '🌾', title: 'Rural Enterprise Development', desc: 'Bring together dispersed labour and rural raw materials to establish new micro-enterprises in non-farm sectors.' },
        { icon: '📈', title: 'Artisan Livelihood Support', desc: 'Increase the wage-earning capacity of traditional artisans by providing access to credit and enterprise support.' },
        { icon: '🌆', title: 'Urban Employment', desc: 'Support unemployed urban youth, including school and college dropouts, in establishing micro-enterprises.' },
        { icon: '♻️', title: 'Livelihood Sustainability', desc: 'Enable long-term livelihoods by combining margin money subsidy with bank credit to make enterprises viable.' },
        { icon: '🤝', title: 'Inclusive Growth', desc: 'Prioritise women, SC/ST, minorities, ex-servicemen, physically challenged, and other vulnerable groups.' },
    ];

    const eligibilityItems = [
        { icon: '👤', title: 'Individual Applicants', desc: 'Any Indian citizen who has attained the age of 18 years (general criterion) may apply.' },
        { icon: '🏫', title: 'Educational Qualification', desc: 'For manufacturing projects above a project cost threshold, a minimum educational qualification may be required. Refer to current scheme guidelines.' },
        { icon: '🌿', title: 'Self-Help Groups (SHGs)', desc: 'Self-Help Groups (including SHGs not covered under other government schemes for margin money subsidy) are eligible.' },
        { icon: '🏛️', title: 'Registered Institutions', desc: 'Charitable trusts, societies registered under Societies Registration Act 1860, and production co-operative societies are eligible.' },
        { icon: '🆕', title: 'New Projects Only', desc: 'PMEGP assistance is only for new projects. Existing units already covered under PMRY, REGP, or any government subsidy scheme are not eligible.' },
        { icon: '⭐', title: 'Special Categories', desc: 'SC, ST, OBC, Minorities, Women, Ex-servicemen, Physically Challenged, and people from Hill/Border areas may be eligible for a higher subsidy rate.' },
    ];

    const benefits = [
        { icon: '💵', title: 'Margin Money Subsidy', desc: 'Government provides margin money subsidy (percentage of project cost) for general and special category beneficiaries in urban and rural areas.' },
        { icon: '🏦', title: 'Bank Loan Linkage', desc: 'PMEGP is linked to bank credit, making entrepreneurship accessible to those without large personal savings.' },
        { icon: '🌿', title: 'Rural / Special Category Priority', desc: 'Beneficiaries in rural areas and special categories may be eligible for a higher subsidy rate compared to general urban applicants.' },
        { icon: '🏭', title: 'Wide Sector Coverage', desc: 'Eligible projects span manufacturing, service, and trading sectors across a broad range of industries.' },
        { icon: '📚', title: 'Training & Capacity Building', desc: 'Beneficiaries undergo Entrepreneurship Development Programme (EDP) training, building business skills alongside project implementation.' },
        { icon: '🔄', title: 'Second PMEGP Loan', desc: 'Successful PMEGP beneficiaries who have repaid their loans may apply for a second loan under PMEGP for upgrading their existing unit.' },
    ];

    const documents = [
        { icon: '🪪', label: 'Aadhaar Card', note: 'Identity and address proof — mandatory for all applicants' },
        { icon: '📄', label: 'PAN Card', note: 'Permanent Account Number — for financial linkage and verification' },
        { icon: '📋', label: 'Category Certificate', note: 'SC/ST/OBC/Minority/Ex-servicemen certificate as applicable' },
        { icon: '🎓', label: 'Educational Qualification Certificate', note: 'Marksheets and certificates of highest educational qualification' },
        { icon: '📷', label: 'Passport-size Photograph', note: 'Recent colour photograph of the applicant' },
        { icon: '📝', label: 'Project Report (DPR)', note: 'Detailed Project Report including cost estimates, market analysis, and financial projections' },
        { icon: '🏠', label: 'Residence Proof', note: 'Current address proof — voter ID, utility bill, ration card, etc.' },
        { icon: '🏦', label: 'Bank Account Details', note: 'Bank passbook / cancelled cheque for direct bank linkage' },
        { icon: '🎖️', label: 'EDP Training Certificate', note: 'Entrepreneurship Development Programme certificate (submitted before disbursement)' },
        { icon: '📜', label: 'Special Category Proof', note: 'Disability certificate, ex-serviceman discharge book, or other applicable proof' },
    ];

    const steps = [
        { num: '01', title: 'Register on the Portal', desc: 'Register on the DSGP portal as a beneficiary, providing personal details, Aadhaar, income information, and category.' },
        { num: '02', title: 'Prepare a Detailed Project Report', desc: 'Prepare a viable Detailed Project Report (DPR) covering business plan, product/service description, market analysis, and projected financials.' },
        { num: '03', title: 'Submit Application to KVIC / DIC', desc: 'Submit the completed application form along with your DPR and all required documents to the nearest KVIC Directorate or District Industries Centre (DIC).' },
        { num: '04', title: 'Task Force Committee Scrutiny', desc: 'A district-level Task Force Committee reviews and shortlists applications based on scheme guidelines, feasibility, and priority categories.' },
        { num: '05', title: 'Bank Loan Sanction', desc: 'Approved applications are forwarded to designated banks. The bank evaluates the project and on sanctioning the loan, issues a formal sanction letter.' },
        { num: '06', title: 'EDP Training', desc: 'Before disbursement, all beneficiaries must complete an Entrepreneurship Development Programme (EDP) training of the prescribed duration. An EDP certificate is issued on completion.' },
        { num: '07', title: 'Subsidy Disbursement', desc: 'On submission of the EDP certificate, the margin money subsidy is credited into a term deposit account held by the bank on the beneficiary\'s behalf.' },
    ];

    return (
        <div className="pmegp-page">

            {/* Hero */}
            <section className="pmegp-hero" aria-labelledby="pmegp-hero-title">
                <div className="pmegp-container">
                    <span className="pmegp-badge">Business &amp; Entrepreneurship</span>
                    <h1 id="pmegp-hero-title">Prime Minister&apos;s Employment Generation Programme</h1>
                    <p className="pmegp-hero-full-name">PMEGP — Credit-Linked Subsidy Scheme</p>
                    <p className="pmegp-hero-desc">
                        A major credit-linked subsidy scheme facilitating self-employment through the establishment
                        of micro-enterprises in the non-farm sector — empowering entrepreneurs across India.
                    </p>
                    <div className="pmegp-hero-actions">
                        <a href="#pmegp-process" className="btn btn-primary">How to Apply</a>
                        <Link to="/schemes" className="btn btn-secondary">Back to Schemes</Link>
                    </div>
                </div>
            </section>

            <main className="pmegp-main-container">

                {/* Overview + Quick Facts */}
                <section className="pmegp-overview-section" id="pmegp-overview" aria-labelledby="pmegp-overview-title">
                    <div className="pmegp-overview-grid">
                        <div className="pmegp-overview-text">
                            <h2 id="pmegp-overview-title">What is PMEGP?</h2>
                            <p>
                                The Prime Minister&apos;s Employment Generation Programme (PMEGP) is a major credit-linked
                                subsidy scheme administered by the Ministry of Micro, Small and Medium Enterprises (MSME),
                                Government of India — launched by merging PMRY and REGP.
                            </p>
                            <p>
                                PMEGP aims to generate employment opportunities in rural as well as urban areas through
                                setting up new self-employment ventures / projects / micro enterprises. It is implemented
                                at the national level by KVIC and at the state level through State KVIC Directorates,
                                KVIBs, and District Industries Centres (DICs).
                            </p>
                            <p>
                                The programme supports entrepreneurs in establishing new micro-enterprises across the
                                manufacturing, service, and trading sectors, with government subsidy provided as
                                a margin money contribution to the project cost.
                            </p>
                            <div className="pmegp-notice pmegp-notice--info">
                                <span aria-hidden="true">ℹ️</span>
                                <div>
                                    <strong>About This Page:</strong> This page provides general information about
                                    the PMEGP scheme. DSGP manages beneficiary registration, verification, and
                                    disbursement tracking for authorised field officers.
                                </div>
                            </div>
                        </div>

                        <aside className="pmegp-quick-facts" aria-label="PMEGP Quick Facts">
                            <h3>Quick Facts</h3>
                            {quickFacts.map((fact, i) => (
                                <div key={i} className="pmegp-fact-row">
                                    <span className="pmegp-fact-label">{fact.label}</span>
                                    <span className="pmegp-fact-value">{fact.value}</span>
                                </div>
                            ))}
                        </aside>
                    </div>
                </section>

                {/* Objectives */}
                <section className="pmegp-section" id="pmegp-objectives" aria-labelledby="pmegp-obj-title">
                    <h2 id="pmegp-obj-title" className="pmegp-section-title">Objectives of PMEGP</h2>
                    <p className="pmegp-section-subtitle">
                        The scheme addresses unemployment through self-employment by supporting micro-enterprise creation.
                    </p>
                    <div className="pmegp-cards-grid">
                        {objectives.map((obj, i) => (
                            <div key={i} className="pmegp-objective-card">
                                <div className="pmegp-card-icon" aria-hidden="true">{obj.icon}</div>
                                <h3>{obj.title}</h3>
                                <p>{obj.desc}</p>
                            </div>
                        ))}
                    </div>
                </section>

                {/* Who Can Apply */}
                <section className="pmegp-section pmegp-section--alt" id="pmegp-eligibility" aria-labelledby="pmegp-elig-title">
                    <h2 id="pmegp-elig-title" className="pmegp-section-title">Who Can Apply</h2>
                    <p className="pmegp-section-subtitle">
                        PMEGP is open to individuals and institutions meeting the general eligibility criteria below.
                    </p>
                    <ul className="pmegp-eligibility-grid" aria-label="Eligibility criteria">
                        {eligibilityItems.map((item, i) => (
                            <li key={i} className="pmegp-eligibility-item">
                                <span className="pmegp-eligibility-icon" aria-hidden="true">{item.icon}</span>
                                <div>
                                    <strong>{item.title}</strong>
                                    <p>{item.desc}</p>
                                </div>
                            </li>
                        ))}
                    </ul>
                    <div className="pmegp-notice pmegp-notice--warning">
                        <span aria-hidden="true">⚠️</span>
                        <div>
                            <strong>Note:</strong> PMEGP eligibility criteria, project cost limits, and subsidy
                            rates are defined by the Ministry of MSME and are subject to revision. Always refer
                            to current official PMEGP guidelines and your nearest DIC or KVIC office.
                        </div>
                    </div>
                </section>

                {/* Benefits */}
                <section className="pmegp-section" id="pmegp-benefits" aria-labelledby="pmegp-benefits-title">
                    <h2 id="pmegp-benefits-title" className="pmegp-section-title">Scheme Benefits</h2>
                    <p className="pmegp-section-subtitle">
                        PMEGP provides margin money subsidy on eligible projects, with higher rates for special
                        category beneficiaries and rural areas.
                    </p>
                    <div className="pmegp-benefits-grid">
                        {benefits.map((b, i) => (
                            <div key={i} className="pmegp-benefit-card">
                                <div className="pmegp-benefit-icon" aria-hidden="true">{b.icon}</div>
                                <h3>{b.title}</h3>
                                <p>{b.desc}</p>
                            </div>
                        ))}
                    </div>
                </section>

                {/* Required Documents */}
                <section className="pmegp-section pmegp-section--alt" id="pmegp-documents" aria-labelledby="pmegp-docs-title">
                    <h2 id="pmegp-docs-title" className="pmegp-section-title">Required Documents</h2>
                    <p className="pmegp-section-subtitle">
                        Prepare the following documents for your PMEGP application.
                    </p>
                    <ul className="pmegp-doc-list" aria-label="Required documents">
                        {documents.map((doc, i) => (
                            <li key={i} className="pmegp-doc-item">
                                <span className="pmegp-doc-icon" aria-hidden="true">{doc.icon}</span>
                                <div>
                                    <div className="pmegp-doc-label">{doc.label}</div>
                                    <div className="pmegp-doc-note">{doc.note}</div>
                                </div>
                            </li>
                        ))}
                    </ul>
                </section>

                {/* Application Process */}
                <section className="pmegp-section" id="pmegp-process" aria-labelledby="pmegp-process-title">
                    <h2 id="pmegp-process-title" className="pmegp-section-title">Application Process</h2>
                    <p className="pmegp-section-subtitle">
                        PMEGP follows a structured process involving the applicant, implementing agency, and bank.
                    </p>
                    <ol className="pmegp-steps-list" aria-label="PMEGP application steps">
                        {steps.map((step, i) => (
                            <li key={i} className="pmegp-step-item">
                                <div className="pmegp-step-number" aria-hidden="true">{step.num}</div>
                                <div className="pmegp-step-body">
                                    <h3>{step.title}</h3>
                                    <p>{step.desc}</p>
                                </div>
                            </li>
                        ))}
                    </ol>
                </section>

                {/* Important Information */}
                <section className="pmegp-section pmegp-section--alt" aria-labelledby="pmegp-important-title">
                    <h2 id="pmegp-important-title" className="pmegp-section-title">Important Information</h2>
                    <div className="pmegp-notices-grid">
                        <div className="pmegp-notice pmegp-notice--warning">
                            <span aria-hidden="true">⚠️</span>
                            <div>
                                <strong>New Projects Only:</strong> PMEGP supports only new project establishments.
                                Existing businesses or enterprises already assisted under other government subsidy
                                schemes are not eligible for PMEGP margin money subsidy.
                            </div>
                        </div>
                        <div className="pmegp-notice pmegp-notice--info">
                            <span aria-hidden="true">ℹ️</span>
                            <div>
                                <strong>One PMEGP Benefit Per Family:</strong> Only one family member can avail
                                PMEGP benefits. If any family member has already received PMEGP, PMRY, or REGP
                                assistance, other members of the same family are not eligible.
                            </div>
                        </div>
                        <div className="pmegp-notice pmegp-notice--success">
                            <span aria-hidden="true">✅</span>
                            <div>
                                <strong>Locate Your Nearest Office:</strong> Visit your nearest District Industries
                                Centre (DIC) or KVIC Directorate office for application assistance, project guidance,
                                and EDP training information.
                            </div>
                        </div>
                        <div className="pmegp-notice pmegp-notice--danger">
                            <span aria-hidden="true">🚫</span>
                            <div>
                                <strong>No Fee for Application:</strong> PMEGP application and processing is free
                                through official government channels. Be cautious of agents charging fees.
                                Report any such incidents to the local DIC office.
                            </div>
                        </div>
                    </div>
                </section>

                {/* CTA */}
                <section className="pmegp-cta" aria-labelledby="pmegp-cta-title">
                    <h2 id="pmegp-cta-title">Start Your Entrepreneurship Journey</h2>
                    <p>Check your eligibility and explore other government schemes available on DSGP.</p>
                    <div className="pmegp-cta-actions">
                        <Link to="/eligibility" className="btn btn-primary">Check Eligibility</Link>
                        <Link to="/schemes" className="btn btn-secondary">All Schemes</Link>
                    </div>
                    <p className="pmegp-cta-note">
                        ⓘ PMEGP applications are processed through KVIC/DIC offices and linked banks.
                        DSGP manages beneficiary registration, verification, and disbursement tracking by authorised officers.
                    </p>
                </section>

            </main>
        </div>
    );
}

export default PMEGP;
