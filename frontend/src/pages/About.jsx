import './About.css';

function About() {

    const features = [
        {
            number: '01',
            title: 'Discover',
            description:
                'Find government subsidies, grants, scholarships, and assistance programs in one unified platform.'
        },
        {
            number: '02',
            title: 'Understand',
            description:
                'Get clear information about scheme benefits, eligibility requirements, target groups, and application procedures.'
        },
        {
            number: '03',
            title: 'Apply',
            description:
                'Follow a simple and guided process to understand how to apply for the schemes relevant to you.'
        },
        {
            number: '04',
            title: 'Track',
            description:
                'Track your application status using your application ID and stay informed throughout the process.'
        }
    ];

    const sectors = [
        {
            title: 'Agriculture',
            description:
                'Financial assistance and support programs designed for farmers and agricultural workers.'
        },
        {
            title: 'Education',
            description:
                'Scholarships and educational assistance programs for students from different backgrounds.'
        },
        {
            title: 'Business & Entrepreneurship',
            description:
                'Government support programs that encourage self-employment, entrepreneurship, and micro-enterprise development.'
        }
    ];

    return (
        <div className="about-page">

            {/* ================= HERO ================= */}

            <section className="about-hero">

                <div className="about-container">

                    <h1>
                        About DSGP
                    </h1>

                    <p>
                        Digital Subsidy & Grant Platform
                    </p>

                    <span>
                        A unified academic platform for discovering and
                        understanding government assistance programs.
                    </span>

                </div>

            </section>


            {/* ================= INTRODUCTION ================= */}

            <section className="about-introduction">

                <div className="about-container">

                    <div className="about-intro-card">

                        <h2>
                            What is DSGP?
                        </h2>

                        <p>
                            DSGP, or the Digital Subsidy & Grant Platform,
                            is an academic demonstration project designed
                            to provide a unified digital experience for
                            discovering government subsidies, grants,
                            scholarships, and assistance programs.
                        </p>

                        <p>
                            The platform brings information about different
                            government schemes together in one place, making
                            it easier for citizens to explore available
                            opportunities, understand eligibility criteria,
                            and learn about the application process.
                        </p>

                        <p>
                            DSGP focuses on creating a simple, accessible,
                            and organized interface so that users can find
                            relevant government assistance programs without
                            having to navigate multiple sections of a website.
                        </p>

                    </div>

                </div>

            </section>


            {/* ================= PLATFORM PURPOSE ================= */}

            <section className="about-purpose">

                <div className="about-container">

                    <div className="about-section-header">

                        <h2>
                            Our Purpose
                        </h2>

                        <p>
                            Making government assistance information easier
                            to discover and understand.
                        </p>

                    </div>


                    <div className="about-purpose-content">

                        <div className="about-purpose-text">

                            <h3>
                                One Platform, Multiple Opportunities
                            </h3>

                            <p>
                                Government schemes can provide valuable
                                financial assistance, scholarships,
                                subsidies, and support to citizens.
                                However, information about these programs
                                can often be spread across different
                                government portals and websites.
                            </p>

                            <p>
                                DSGP demonstrates how a unified digital
                                platform can organize this information and
                                present it in a clear and user-friendly way.
                            </p>

                            <p>
                                The goal is to help users discover schemes
                                relevant to their needs and understand the
                                basic requirements before proceeding to
                                official government portals.
                            </p>

                        </div>


                        <div className="about-purpose-highlight">

                            <div className="about-highlight-number">
                                1
                            </div>

                            <h3>
                                Unified Platform
                            </h3>

                            <p>
                                A single interface for exploring multiple
                                categories of government assistance programs.
                            </p>

                        </div>

                    </div>

                </div>

            </section>


            {/* ================= HOW DSGP WORKS ================= */}

            <section className="about-features">

                <div className="about-container">

                    <div className="about-section-header">

                        <h2>
                            What DSGP Offers
                        </h2>

                        <p>
                            A simple four-step approach to discovering
                            government assistance programs.
                        </p>

                    </div>


                    <div className="about-features-grid">

                        {features.map((feature) => (

                            <div
                                className="about-feature-card"
                                key={feature.number}
                            >

                                <div className="about-feature-number">
                                    {feature.number}
                                </div>

                                <h3>
                                    {feature.title}
                                </h3>

                                <p>
                                    {feature.description}
                                </p>

                            </div>

                        ))}

                    </div>

                </div>

            </section>


            {/* ================= FOCUS SECTORS ================= */}

            <section className="about-sectors">

                <div className="about-container">

                    <div className="about-section-header">

                        <h2>
                            Focus Sectors
                        </h2>

                        <p>
                            DSGP currently demonstrates government assistance
                            programs across three major sectors.
                        </p>

                    </div>


                    <div className="about-sectors-grid">

                        {sectors.map((sector) => (

                            <div
                                className="about-sector-card"
                                key={sector.title}
                            >

                                <h3>
                                    {sector.title}
                                </h3>

                                <p>
                                    {sector.description}
                                </p>

                            </div>

                        ))}

                    </div>

                </div>

            </section>


            {/* ================= ACADEMIC PROJECT ================= */}

            <section className="about-academic">

                <div className="about-container">

                    <div className="about-academic-card">

                        <h2>
                            Academic Demonstration Project
                        </h2>

                        <p>
                            DSGP is developed as an educational project to
                            demonstrate how a digital platform can bring
                            information about government subsidy and grant
                            schemes together in a structured interface.
                        </p>

                        <p>
                            The platform is intended for demonstration and
                            learning purposes. It does not replace official
                            government portals or act as an official
                            government service.
                        </p>

                    </div>

                </div>

            </section>


            {/* ================= DISCLAIMER ================= */}

            <section className="about-disclaimer">

                <div className="about-container">

                    <div className="about-disclaimer-card">

                        <h2>
                            Important Disclaimer
                        </h2>

                        <p>
                            DSGP is an academic demonstration project.
                            Scheme information presented on this platform
                            should be verified through the respective
                            official government portals before making any
                            application or financial decision.
                        </p>

                        <p>
                            Users should always refer to authorized
                            government sources for the latest eligibility
                            requirements, benefits, deadlines, documents,
                            and application procedures.
                        </p>

                    </div>

                </div>

            </section>

        </div>
    );
}

export default About;