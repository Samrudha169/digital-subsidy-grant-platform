import { Link } from 'react-router-dom';
import './Privacy.css';

function Privacy() {
    return (
        <div className="privacy-page">

            {/* Header */}
            <section className="privacy-hero">
                <div className="privacy-container">
                    <h1>Privacy Policy</h1>
                    <p>
                        Learn how DSGP handles information and protects
                        user privacy while using the platform.
                    </p>
                </div>
            </section>

            {/* Content */}
            <main className="privacy-content">
                <div className="privacy-container">

                    <section className="privacy-card">
                        <h2>1. Introduction</h2>
                        <p>
                            Digital Subsidy & Grant Platform (DSGP) is an
                            academic demonstration project designed to
                            showcase a unified platform for discovering
                            government subsidy and grant schemes.
                        </p>
                        <p>
                            This Privacy Policy explains how information
                            provided while using the platform may be
                            handled within this educational project.
                        </p>
                    </section>

                    <section className="privacy-card">
                        <h2>2. Information We Collect</h2>
                        <p>
                            Depending on the features being demonstrated,
                            DSGP may request information such as:
                        </p>

                        <ul>
                            <li>Name and basic user details</li>
                            <li>Contact information</li>
                            <li>State or Union Territory</li>
                            <li>Age group</li>
                            <li>Occupation or status</li>
                            <li>Social category</li>
                            <li>Application identification details</li>
                        </ul>

                        <p>
                            The information fields displayed by the platform
                            are intended primarily for demonstration and
                            educational purposes.
                        </p>
                    </section>

                    <section className="privacy-card">
                        <h2>3. How Information Is Used</h2>
                        <p>
                            Information entered into DSGP may be used to
                            demonstrate platform functionality, including:
                        </p>

                        <ul>
                            <li>Displaying relevant government schemes</li>
                            <li>Demonstrating eligibility checking</li>
                            <li>Demonstrating application tracking</li>
                            <li>Improving the academic demonstration</li>
                            <li>Providing help and support functionality</li>
                        </ul>
                    </section>

                    <section className="privacy-card">
                        <h2>4. Data Security</h2>
                        <p>
                            Reasonable measures are intended to be followed
                            within the scope of this academic project to
                            prevent unauthorized access, modification, or
                            misuse of information.
                        </p>
                        <p>
                            However, this platform is a demonstration project
                            and should not be treated as an official
                            government service for submitting sensitive
                            personal information.
                        </p>
                    </section>

                    <section className="privacy-card">
                        <h2>5. Third-Party Government Portals</h2>
                        <p>
                            DSGP may provide information or links relating to
                            government schemes and external government
                            portals. When users visit an external website,
                            that website's own privacy policy and terms apply.
                        </p>
                    </section>

                    <section className="privacy-card">
                        <h2>6. Cookies and Local Storage</h2>
                        <p>
                            The demonstration platform may use browser
                            technologies such as cookies or local storage
                            where required for functionality. These
                            technologies may be used to maintain preferences
                            or demonstrate application features.
                        </p>
                    </section>

                    <section className="privacy-card">
                        <h2>7. Children's Privacy</h2>
                        <p>
                            DSGP is not intended to collect sensitive personal
                            information from children. Users should avoid
                            submitting unnecessary personal information
                            through this academic demonstration platform.
                        </p>
                    </section>

                    <section className="privacy-card">
                        <h2>8. Changes to This Policy</h2>
                        <p>
                            This Privacy Policy may be updated when the
                            functionality or scope of the academic project
                            changes. Any updated version will be displayed
                            on this page.
                        </p>
                    </section>

                    <section className="privacy-card">
                        <h2>9. Academic Project Disclaimer</h2>
                        <p>
                            DSGP is an academic demonstration project and is
                            not an official government website or government
                            service.
                        </p>
                        <p>
                            Users should verify scheme information and submit
                            applications only through authorized government
                            portals.
                        </p>
                    </section>

                    <div className="privacy-back">
                        <Link to="/" className="privacy-back-btn">
                            ← Back to Home
                        </Link>
                    </div>

                </div>
            </main>

        </div>
    );
}

export default Privacy;