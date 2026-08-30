import { Link } from 'react-router-dom';
import './Terms.css';

function Terms() {
    return (
        <div className="terms-page">

            {/* Hero */}
            <section className="terms-hero">
                <div className="terms-container">
                    <h1>Terms of Service</h1>
                    <p>
                        Please review the terms and conditions for using
                        the DSGP academic demonstration platform.
                    </p>
                </div>
            </section>

            {/* Content */}
            <main className="terms-content">
                <div className="terms-container">

                    <section className="terms-card">
                        <h2>1. Acceptance of Terms</h2>
                        <p>
                            By accessing or using the Digital Subsidy & Grant
                            Platform (DSGP), you acknowledge that you have read,
                            understood, and agree to these Terms of Service.
                        </p>
                        <p>
                            If you do not agree with these terms, please do not
                            use the platform.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>2. About DSGP</h2>
                        <p>
                            DSGP is an academic demonstration project created
                            to showcase a unified digital platform for
                            discovering, understanding, and tracking government
                            subsidy and grant schemes.
                        </p>
                        <p>
                            DSGP is not an official government website,
                            government department, or government service.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>3. Use of the Platform</h2>
                        <p>
                            Users may use DSGP for lawful and educational
                            purposes, including:
                        </p>

                        <ul>
                            <li>Exploring available government schemes</li>
                            <li>Reviewing scheme information</li>
                            <li>Checking demonstrated eligibility features</li>
                            <li>Exploring application tracking functionality</li>
                            <li>Using the platform for academic demonstration</li>
                        </ul>
                    </section>

                    <section className="terms-card">
                        <h2>4. User Responsibilities</h2>
                        <p>
                            Users are responsible for providing accurate
                            information when using interactive features of
                            the platform.
                        </p>

                        <p>
                            Users must not attempt to misuse, damage, disrupt,
                            or gain unauthorized access to the platform or
                            its associated systems.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>5. Scheme Information</h2>
                        <p>
                            Information displayed on DSGP about government
                            schemes is provided for demonstration and
                            educational purposes.
                        </p>

                        <p>
                            Scheme eligibility requirements, benefits,
                            application procedures, deadlines, and other
                            information may change. Users should verify the
                            latest information through the respective official
                            government portals before making any decisions.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>6. Applications and Payments</h2>
                        <p>
                            DSGP does not guarantee acceptance, approval, or
                            processing of any government scheme application.
                        </p>

                        <p>
                            Users should submit actual applications only
                            through authorized government portals or other
                            officially designated channels.
                        </p>

                        <p>
                            DSGP does not directly process government
                            payments, subsidies, grants, or financial
                            assistance.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>7. Accuracy of Information</h2>
                        <p>
                            While reasonable efforts may be made to present
                            useful information, DSGP does not guarantee that
                            all information displayed on the platform is
                            complete, current, or error-free.
                        </p>

                        <p>
                            Users should independently verify important
                            information before relying on it.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>8. Intellectual Property</h2>
                        <p>
                            The design, layout, code, branding, and original
                            content created specifically for the DSGP academic
                            project may be protected by applicable intellectual
                            property rights.
                        </p>

                        <p>
                            Government scheme names, information, logos, and
                            trademarks remain the property of their respective
                            owners.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>9. External Links</h2>
                        <p>
                            DSGP may provide links or references to external
                            websites, including official government portals.
                        </p>

                        <p>
                            DSGP is not responsible for the availability,
                            content, security, privacy practices, or policies
                            of external websites.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>10. Limitation of Liability</h2>
                        <p>
                            DSGP is provided as an academic demonstration
                            platform. The project team does not guarantee
                            uninterrupted availability or error-free operation
                            of the platform.
                        </p>

                        <p>
                            Users should not rely solely on DSGP for financial,
                            legal, or government-service decisions.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>11. Changes to These Terms</h2>
                        <p>
                            These Terms of Service may be updated when the
                            functionality or scope of the academic project
                            changes.
                        </p>

                        <p>
                            Updated terms will be displayed on this page.
                        </p>
                    </section>

                    <section className="terms-card">
                        <h2>12. Academic Project Disclaimer</h2>
                        <p>
                            DSGP is developed solely as an educational and
                            academic demonstration project.
                        </p>

                        <p>
                            For official information, applications, payments,
                            and government services, users should always use
                            authorized government channels.
                        </p>
                    </section>

                    <div className="terms-back">
                        <Link to="/" className="terms-back-btn">
                            ← Back to Home
                        </Link>
                    </div>

                </div>
            </main>

        </div>
    );
}

export default Terms;