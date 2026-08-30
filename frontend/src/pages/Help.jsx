import { Link } from 'react-router-dom';
import './Help.css';

function Help() {
    return (
        <div className="help-page">

            {/* Header */}
            <section className="help-hero">
                <div className="help-container">
                    <h1>Help & Support</h1>
                    <p>
                        Find answers to common questions or get assistance
                        with using the Digital Subsidy & Grant Platform.
                    </p>
                </div>
            </section>

            {/* Help Options */}
            <section className="help-section">
                <div className="help-container">

                    <div className="help-section-header">
                        <h2>How Can We Help?</h2>
                        <p>
                            Choose an option below to get the information
                            or assistance you need.
                        </p>
                    </div>

                    <div className="help-grid">

                        <div className="help-card">
                            <div className="help-icon">?</div>
                            <h3>Frequently Asked Questions</h3>
                            <p>
                                Find quick answers to common questions about
                                schemes, eligibility, applications, and tracking.
                            </p>

                            <Link to="/faq" className="help-btn">
                                View FAQs
                            </Link>
                        </div>

                        <div className="help-card">
                            <div className="help-icon">✓</div>
                            <h3>Eligibility Assistance</h3>
                            <p>
                                Learn how the eligibility process works and
                                understand the basic requirements for schemes.
                            </p>

                            <Link to="/eligibility" className="help-btn">
                                Check Eligibility
                            </Link>
                        </div>

                        <div className="help-card">
                            <div className="help-icon">↗</div>
                            <h3>Application Tracking</h3>
                            <p>
                                Check the current status of your government
                                scheme application using your application ID.
                            </p>

                            <Link to="/track" className="help-btn">
                                Track Application
                            </Link>
                        </div>

                        <div className="help-card">
                            <div className="help-icon">✉</div>
                            <h3>Contact Support</h3>
                            <p>
                                Need additional assistance? Contact our support
                                team for further information.
                            </p>

                            <Link to="/contact" className="help-btn">
                                Contact Us
                            </Link>
                        </div>

                    </div>

                </div>
            </section>

            {/* Getting Started */}
            <section className="help-getting-started">
                <div className="help-container">

                    <div className="help-section-header">
                        <h2>Getting Started with DSGP</h2>
                        <p>
                            Follow these simple steps to find and access
                            government assistance schemes.
                        </p>
                    </div>

                    <div className="help-steps">

                        <div className="help-step">
                            <span>01</span>
                            <div>
                                <h3>Find a Scheme</h3>
                                <p>
                                    Browse available government schemes or
                                    search for a specific scheme.
                                </p>
                            </div>
                        </div>

                        <div className="help-step">
                            <span>02</span>
                            <div>
                                <h3>Check Eligibility</h3>
                                <p>
                                    Review the eligibility requirements before
                                    starting your application.
                                </p>
                            </div>
                        </div>

                        <div className="help-step">
                            <span>03</span>
                            <div>
                                <h3>Apply</h3>
                                <p>
                                    Follow the official application process
                                    for the selected government scheme.
                                </p>
                            </div>
                        </div>

                        <div className="help-step">
                            <span>04</span>
                            <div>
                                <h3>Track Your Application</h3>
                                <p>
                                    Use your application ID to monitor the
                                    progress of your submission.
                                </p>
                            </div>
                        </div>

                    </div>

                </div>
            </section>

            {/* Contact CTA */}
            <section className="help-contact">
                <div className="help-container">

                    <h2>Still Need Help?</h2>

                    <p>
                        If you cannot find the information you are looking for,
                        our contact page can help you get in touch.
                    </p>

                    <Link to="/contact" className="help-contact-btn">
                        Contact Support
                    </Link>

                </div>
            </section>

        </div>
    );
}

export default Help;