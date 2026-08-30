import { useState } from 'react';
import { Link } from 'react-router-dom';
import './Contact.css';

const INITIAL_FORM = {
    name: '',
    email: '',
    subject: '',
    category: '',
    message: '',
};

const SUBJECT_OPTIONS = [
    { value: '', label: 'Select a subject' },
    { value: 'beneficiary-registration', label: 'Beneficiary Registration' },
    { value: 'document-upload', label: 'Document Upload' },
    { value: 'eligibility', label: 'Scheme Eligibility' },
    { value: 'application-status', label: 'Application Status' },
    { value: 'technical-issue', label: 'Technical Issue / Bug Report' },
    { value: 'scheme-information', label: 'Scheme Information (NSP / PMEGP)' },
    { value: 'other', label: 'Other' },
];

function Contact() {
    const [form, setForm] = useState(INITIAL_FORM);
    const [errors, setErrors] = useState({});
    const [submitted, setSubmitted] = useState(false);

    const validate = (data) => {
        const errs = {};
        if (!data.name.trim()) errs.name = 'Full name is required.';
        if (!data.email.trim()) {
            errs.email = 'Email address is required.';
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email.trim())) {
            errs.email = 'Please enter a valid email address.';
        }
        if (!data.subject) errs.subject = 'Please select a subject.';
        if (!data.message.trim()) {
            errs.message = 'Message is required.';
        } else if (data.message.trim().length < 20) {
            errs.message = 'Message must be at least 20 characters.';
        }
        return errs;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
        // Clear error on change
        if (errors[name]) {
            setErrors((prev) => ({ ...prev, [name]: undefined }));
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        const errs = validate(form);
        if (Object.keys(errs).length > 0) {
            setErrors(errs);
            // Focus first error field
            const firstErrField = document.querySelector('.contact-field--error input, .contact-field--error textarea, .contact-field--error select');
            if (firstErrField) firstErrField.focus();
            return;
        }
        // No backend API in current DSGP phase — UI-only confirmation
        setSubmitted(true);
        setForm(INITIAL_FORM);
        setErrors({});
    };

    const handleReset = () => {
        setSubmitted(false);
        setForm(INITIAL_FORM);
        setErrors({});
    };

    return (
        <div className="contact-page">

            {/* Hero */}
            <section className="contact-hero" aria-labelledby="contact-hero-title">
                <div className="contact-hero-inner">
                    <h1 id="contact-hero-title">Contact &amp; Support</h1>
                    <p>
                        Reach out to the DSGP support team for help with portal usage, scheme information,
                        technical issues, or general enquiries.
                    </p>
                </div>
            </section>

            <main className="contact-main">
                <div className="contact-container">
                    <div className="contact-layout">

                        {/* ── Left: Contact Info ── */}
                        <aside className="contact-info" aria-label="Contact information">

                            <div className="contact-info-card">
                                <div className="contact-info-icon" aria-hidden="true">📧</div>
                                <div>
                                    <div className="contact-info-label">Email Support</div>
                                    <div className="contact-info-value">support@dsgp.gov.in</div>
                                    <div className="contact-info-note">Response within 2 working days</div>
                                </div>
                            </div>

                            <div className="contact-info-card">
                                <div className="contact-info-icon" aria-hidden="true">📞</div>
                                <div>
                                    <div className="contact-info-label">Helpline</div>
                                    <div className="contact-info-value">1800-XXX-XXXX</div>
                                    <div className="contact-info-note">Toll-free &middot; Mon–Fri, 9 AM–6 PM IST</div>
                                </div>
                            </div>

                            <div className="contact-info-card">
                                <div className="contact-info-icon" aria-hidden="true">🕐</div>
                                <div>
                                    <div className="contact-info-label">Working Hours</div>
                                    <div className="contact-info-value">Mon – Fri</div>
                                    <div className="contact-info-note">9:00 AM – 6:00 PM IST (excluding public holidays)</div>
                                </div>
                            </div>

                            <div className="contact-info-card">
                                <div className="contact-info-icon" aria-hidden="true">🏢</div>
                                <div>
                                    <div className="contact-info-label">Nodal Office</div>
                                    <div className="contact-info-value">DSGP Programme Cell</div>
                                    <div className="contact-info-note">Ministry of MSME / MoE &mdash; New Delhi</div>
                                </div>
                            </div>

                            <div className="contact-info-card">
                                <div className="contact-info-icon" aria-hidden="true">🔧</div>
                                <div>
                                    <div className="contact-info-label">Technical Issues</div>
                                    <div className="contact-info-value">tech@dsgp.gov.in</div>
                                    <div className="contact-info-note">Portal bugs &middot; Upload failures &middot; Login issues</div>
                                </div>
                            </div>

                            {/* Quick Links */}
                            <div className="contact-quick-links">
                                <p className="contact-quick-label">Quick Resources</p>
                                <Link to="/faq" className="contact-quick-link">❓ Frequently Asked Questions</Link>
                                <Link to="/help" className="contact-quick-link">📖 Help &amp; Guidance</Link>
                                <Link to="/schemes/nsp" className="contact-quick-link">🎓 NSP Scheme Information</Link>
                                <Link to="/schemes/pmegp" className="contact-quick-link">💼 PMEGP Scheme Information</Link>
                            </div>
                        </aside>

                        {/* ── Right: Form ── */}
                        <section className="contact-form-section" aria-labelledby="contact-form-title">
                            <h2 id="contact-form-title" className="contact-form-title">Send a Support Request</h2>

                            {submitted ? (
                                <div className="contact-success" role="alert">
                                    <div className="contact-success-icon" aria-hidden="true">✅</div>
                                    <h3>Request Received</h3>
                                    <p>
                                        Thank you for reaching out. Your support request has been noted.
                                        Our team will respond to your email address within 2 working days.
                                    </p>
                                    <div className="contact-notice contact-notice--info">
                                        <span aria-hidden="true">ℹ️</span>
                                        <div>
                                            <strong>Note:</strong> The contact form submission API is currently
                                            under development. For immediate assistance, please email{' '}
                                            <strong>support@dsgp.gov.in</strong> or call the helpline.
                                        </div>
                                    </div>
                                    <button
                                        className="btn btn-secondary"
                                        onClick={handleReset}
                                        style={{ marginTop: '20px' }}
                                    >
                                        Submit Another Request
                                    </button>
                                </div>
                            ) : (
                                <form
                                    className="contact-form"
                                    onSubmit={handleSubmit}
                                    noValidate
                                    aria-label="Support request form"
                                >
                                    <div className="contact-notice contact-notice--info">
                                        <span aria-hidden="true">ℹ️</span>
                                        <div>
                                            Form submissions are UI-only in the current DSGP phase.
                                            For immediate support, use the email or phone number listed.
                                        </div>
                                    </div>

                                    {/* Name */}
                                    <div className={`contact-field ${errors.name ? 'contact-field--error' : ''}`}>
                                        <label htmlFor="contact-name" className="contact-label">
                                            Full Name <span aria-hidden="true" className="contact-required">*</span>
                                        </label>
                                        <input
                                            type="text"
                                            id="contact-name"
                                            name="name"
                                            value={form.name}
                                            onChange={handleChange}
                                            className="contact-input"
                                            placeholder="Your full name"
                                            autoComplete="name"
                                            aria-required="true"
                                            aria-describedby={errors.name ? 'contact-name-error' : undefined}
                                        />
                                        {errors.name && (
                                            <span id="contact-name-error" className="contact-error-msg" role="alert">
                                                {errors.name}
                                            </span>
                                        )}
                                    </div>

                                    {/* Email */}
                                    <div className={`contact-field ${errors.email ? 'contact-field--error' : ''}`}>
                                        <label htmlFor="contact-email" className="contact-label">
                                            Email Address <span aria-hidden="true" className="contact-required">*</span>
                                        </label>
                                        <input
                                            type="email"
                                            id="contact-email"
                                            name="email"
                                            value={form.email}
                                            onChange={handleChange}
                                            className="contact-input"
                                            placeholder="your@email.com"
                                            autoComplete="email"
                                            aria-required="true"
                                            aria-describedby={errors.email ? 'contact-email-error' : undefined}
                                        />
                                        {errors.email && (
                                            <span id="contact-email-error" className="contact-error-msg" role="alert">
                                                {errors.email}
                                            </span>
                                        )}
                                    </div>

                                    {/* Subject */}
                                    <div className={`contact-field ${errors.subject ? 'contact-field--error' : ''}`}>
                                        <label htmlFor="contact-subject" className="contact-label">
                                            Subject <span aria-hidden="true" className="contact-required">*</span>
                                        </label>
                                        <select
                                            id="contact-subject"
                                            name="subject"
                                            value={form.subject}
                                            onChange={handleChange}
                                            className="contact-input contact-select"
                                            aria-required="true"
                                            aria-describedby={errors.subject ? 'contact-subject-error' : undefined}
                                        >
                                            {SUBJECT_OPTIONS.map((opt) => (
                                                <option key={opt.value} value={opt.value} disabled={!opt.value}>
                                                    {opt.label}
                                                </option>
                                            ))}
                                        </select>
                                        {errors.subject && (
                                            <span id="contact-subject-error" className="contact-error-msg" role="alert">
                                                {errors.subject}
                                            </span>
                                        )}
                                    </div>

                                    {/* Message */}
                                    <div className={`contact-field ${errors.message ? 'contact-field--error' : ''}`}>
                                        <label htmlFor="contact-message" className="contact-label">
                                            Message <span aria-hidden="true" className="contact-required">*</span>
                                        </label>
                                        <textarea
                                            id="contact-message"
                                            name="message"
                                            value={form.message}
                                            onChange={handleChange}
                                            className="contact-input contact-textarea"
                                            placeholder="Describe your issue or question in detail (minimum 20 characters)…"
                                            rows={6}
                                            aria-required="true"
                                            aria-describedby={errors.message ? 'contact-message-error' : undefined}
                                        />
                                        <span className="contact-char-count">
                                            {form.message.length} characters
                                        </span>
                                        {errors.message && (
                                            <span id="contact-message-error" className="contact-error-msg" role="alert">
                                                {errors.message}
                                            </span>
                                        )}
                                    </div>

                                    <button type="submit" className="btn btn-primary contact-submit-btn">
                                        Send Support Request
                                    </button>
                                </form>
                            )}
                        </section>
                    </div>

                    {/* ── Regional Support Centres ── */}
                    <section className="contact-regional" aria-labelledby="contact-regional-title">
                        <h2 id="contact-regional-title" className="contact-section-title">Regional Support Centres</h2>
                        <div className="contact-regional-grid">
                            <div className="contact-regional-card">
                                <h3>🏛️ District Industries Centre (DIC)</h3>
                                <p>
                                    For PMEGP scheme guidance, project report assistance, and field-level
                                    support, contact your nearest District Industries Centre (DIC).
                                </p>
                                <p className="contact-regional-note">
                                    Contact information available at your district collectorate office.
                                </p>
                            </div>
                            <div className="contact-regional-card">
                                <h3>🪡 KVIC State Directorates</h3>
                                <p>
                                    Khadi and Village Industries Commission (KVIC) state offices handle
                                    PMEGP application processing and EDP training coordination.
                                </p>
                                <p className="contact-regional-note">
                                    Visit <em>kviconline.gov.in</em> for state office contacts.
                                </p>
                            </div>
                            <div className="contact-regional-card">
                                <h3>🎓 School / College Scholarship Cell</h3>
                                <p>
                                    For NSP scholarship queries, your school or college&apos;s Scholarship
                                    Nodal Officer is the first point of contact for application verification.
                                </p>
                                <p className="contact-regional-note">
                                    Visit <em>scholarships.gov.in</em> for NSP helpdesk contacts.
                                </p>
                            </div>
                        </div>
                    </section>

                    {/* ── Response Times ── */}
                    <section className="contact-response" aria-labelledby="contact-response-title">
                        <h2 id="contact-response-title" className="contact-section-title">Support Response Times</h2>
                        <div className="contact-response-grid">
                            <div className="contact-response-item">
                                <span className="contact-response-icon" aria-hidden="true">📧</span>
                                <span className="contact-response-channel">Email</span>
                                <span className="contact-response-time">Within 2 working days</span>
                            </div>
                            <div className="contact-response-item">
                                <span className="contact-response-icon" aria-hidden="true">📞</span>
                                <span className="contact-response-channel">Helpline</span>
                                <span className="contact-response-time">Immediate (during hours)</span>
                            </div>
                            <div className="contact-response-item">
                                <span className="contact-response-icon" aria-hidden="true">📝</span>
                                <span className="contact-response-channel">Contact Form</span>
                                <span className="contact-response-time">Within 2 working days</span>
                            </div>
                            <div className="contact-response-item">
                                <span className="contact-response-icon" aria-hidden="true">🔧</span>
                                <span className="contact-response-channel">Technical Issues</span>
                                <span className="contact-response-time">Within 1 working day</span>
                            </div>
                        </div>
                    </section>
                </div>
            </main>
        </div>
    );
}

export default Contact;
