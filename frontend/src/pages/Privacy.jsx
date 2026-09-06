import { Link } from 'react-router-dom';
import './Privacy.css';


/* ══════════════════════════════════════════════════════════════
   PRIVACY POLICY SECTIONS
══════════════════════════════════════════════════════════════ */

const privacySections = [
    {
        title: '1. Introduction',
        paragraphs: [
            `Digital Subsidy & Grant Platform (DSGP) is an
            academic demonstration project designed to
            showcase a unified platform for discovering
            government subsidy and grant schemes.`,

            `This Privacy Policy explains how information
            provided while using the platform may be
            handled within this educational project.`
        ]
    },

    {
        title: '2. Information We Collect',
        paragraphs: [
            `Depending on the features being demonstrated,
            DSGP may request information such as:`
        ],
        list: [
            'Name and basic user details',
            'Contact information',
            'State or Union Territory',
            'Age group',
            'Occupation or status',
            'Social category',
            'Application identification details'
        ],
        afterList: [
            `The information fields displayed by the platform
            are intended primarily for demonstration and
            educational purposes.`
        ]
    },

    {
        title: '3. How Information Is Used',
        paragraphs: [
            `Information entered into DSGP may be used to
            demonstrate platform functionality, including:`
        ],
        list: [
            'Displaying relevant government schemes',
            'Demonstrating eligibility checking',
            'Demonstrating application tracking',
            'Improving the academic demonstration',
            'Providing help and support functionality'
        ]
    },

    {
        title: '4. Data Security',
        paragraphs: [
            `Reasonable measures are intended to be followed
            within the scope of this academic project to
            prevent unauthorized access, modification, or
            misuse of information.`,

            `However, this platform is a demonstration project
            and should not be treated as an official
            government service for submitting sensitive
            personal information.`
        ]
    },

    {
        title: '5. Third-Party Government Portals',
        paragraphs: [
            `DSGP may provide information or links relating to
            government schemes and external government
            portals. When users visit an external website,
            that website's own privacy policy and terms apply.`
        ]
    },

    {
        title: '6. Cookies and Local Storage',
        paragraphs: [
            `The demonstration platform may use browser
            technologies such as cookies or local storage
            where required for functionality. These
            technologies may be used to maintain preferences
            or demonstrate application features.`
        ]
    },

    {
        title: "7. Children's Privacy",
        paragraphs: [
            `DSGP is not intended to collect sensitive personal
            information from children. Users should avoid
            submitting unnecessary personal information
            through this academic demonstration platform.`
        ]
    },

    {
        title: '8. Changes to This Policy',
        paragraphs: [
            `This Privacy Policy may be updated when the
            functionality or scope of the academic project
            changes. Any updated version will be displayed
            on this page.`
        ]
    },

    {
        title: '9. Academic Project Disclaimer',
        paragraphs: [
            `DSGP is an academic demonstration project and is
            not an official government website or government
            service.`,

            `Users should verify scheme information and submit
            applications only through authorized government
            portals.`
        ]
    }
];


/* ══════════════════════════════════════════════════════════════
   MAIN COMPONENT
══════════════════════════════════════════════════════════════ */

function Privacy() {

    const currentYear = new Date().getFullYear();

    const lastUpdated = new Date().toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'long',
        year: 'numeric'
    });


    return (
        <div className="privacy-page">

            {/* ══════════════════════════════════════════════════
                HEADER
            ══════════════════════════════════════════════════ */}

            <section className="privacy-hero">

                <div className="privacy-container">

                    <h1>
                        Privacy Policy
                    </h1>

                    <p>
                        Learn how DSGP handles information and protects
                        user privacy while using the platform.
                    </p>

                    <small>
                        Last updated: {lastUpdated}
                    </small>

                </div>

            </section>


            {/* ══════════════════════════════════════════════════
                CONTENT
            ══════════════════════════════════════════════════ */}

            <main className="privacy-content">

                <div className="privacy-container">

                    {privacySections.map((section, index) => (

                        <section
                            className="privacy-card"
                            key={index}
                        >

                            <h2>
                                {section.title}
                            </h2>


                            {/* Paragraphs before list */}

                            {section.paragraphs?.map(
                                (paragraph, paragraphIndex) => (

                                    <p key={paragraphIndex}>
                                        {paragraph}
                                    </p>

                                )
                            )}


                            {/* Optional list */}

                            {section.list && (

                                <ul>

                                    {section.list.map(
                                        (item, itemIndex) => (

                                            <li key={itemIndex}>
                                                {item}
                                            </li>

                                        )
                                    )}

                                </ul>

                            )}


                            {/* Paragraphs after list */}

                            {section.afterList?.map(
                                (paragraph, paragraphIndex) => (

                                    <p key={paragraphIndex}>
                                        {paragraph}
                                    </p>

                                )
                            )}

                        </section>

                    ))}


                    {/* ══════════════════════════════════════════
                        BACK TO HOME
                    ══════════════════════════════════════════ */}

                    <div className="privacy-back">

                        <Link
                            to="/"
                            className="privacy-back-btn"
                        >
                            ← Back to Home
                        </Link>

                    </div>

                </div>

            </main>


            {/* ══════════════════════════════════════════════════
                FOOTER
            ══════════════════════════════════════════════════ */}

            <footer className="privacy-footer">

                <p>
                    &copy; {currentYear} Digital Subsidy &amp;
                    Grant Platform (DSGP)
                </p>

            </footer>

        </div>
    );
}

export default Privacy;