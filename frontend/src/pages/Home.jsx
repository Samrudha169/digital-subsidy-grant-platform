import { Link } from 'react-router-dom';
import '../App.css';

function Home() {

    const isOfficerLoggedIn =
        localStorage.getItem('officerLoggedIn') === 'true';

    const stats = [
        {
            value: '3',
            label: 'Featured Schemes'
        },
        {
            value: '3',
            label: 'Focus Sectors'
        },
        {
            value: 'One',
            label: 'Unified Platform'
        },
        {
            value: '4-Step',
            label: 'Application Process'
        }
    ];

    const categories = [
        {
            id: 1,
            name: 'Agriculture',
            description:
                'Financial assistance and support schemes for farmers and agricultural workers',
            count: '1 Scheme Available'
        },
        {
            id: 2,
            name: 'Education',
            description:
                'Scholarships and educational grants for students from all backgrounds',
            count: '1 Scheme Available'
        },
        {
            id: 3,
            name: 'Business & Entrepreneurship',
            description:
                'Support for entrepreneurs and small business development initiatives',
            count: '1 Scheme Available'
        }
    ];

    const featuredSchemes = [
        {
            id: 1,
            name: 'PM-KISAN',
            category: 'Agriculture',
            target: 'Farmers',
            description:
                'Income support scheme providing ₹6,000 per year to eligible farmer families in three equal installments.',
            path: '/schemes/pm-kisan'
        },
        {
            id: 2,
            name: 'National Scholarship Portal (NSP)',
            category: 'Education',
            target: 'Students',
            description:
                'Centralized platform offering various scholarships for students from pre-matric to post-matric levels.',
            path: '/schemes/nsp'
        },
        {
            id: 3,
            name: 'PMEGP',
            category: 'Business & Entrepreneurship',
            target: 'Entrepreneurs',
            description:
                'Credit-linked subsidy scheme for generating self-employment through establishment of micro-enterprises.',
            path: '/schemes/pmegp'
        }
    ];

    const steps = [
        {
            number: '01',
            title: 'Discover',
            description:
                'Browse and search for government schemes relevant to your needs and profile'
        },
        {
            number: '02',
            title: 'Check Eligibility',
            description:
                'Verify your eligibility using our comprehensive criteria matching system'
        },
        {
            number: '03',
            title: 'Apply',
            description:
                'Submit your application with required documents through our guided process'
        },
        {
            number: '04',
            title: 'Track',
            description:
                'Monitor your application status and receive updates at every stage'
        }
    ];

    return (
        <div className="dsgp-app">

            {/* HEADER */}
            {!isOfficerLoggedIn && (
                <header className="header">
                    <div className="site-header">
                        <div className="header-inner">
                            <div className="header-container">

                                <div className="brand">
                                    <h1 className="brand-title">
                                        DSGP
                                    </h1>

                                    <p className="brand-subtitle">
                                        Digital Subsidy & Grant Platform
                                    </p>
                                </div>

                                <nav className="nav">

                                    <Link
                                        to="/"
                                        className="nav-link"
                                    >
                                        Home
                                    </Link>

                                    <Link
                                        to="/schemes"
                                        className="nav-link"
                                    >
                                        Find Schemes
                                    </Link>

                                    <Link
                                        to="/eligibility"
                                        className="nav-link"
                                    >
                                        Eligibility
                                    </Link>

                                    <Link
                                        to="/track"
                                        className="nav-link"
                                    >
                                        Track Application
                                    </Link>

                                    <Link
                                        to="/about"
                                        className="nav-link"
                                    >
                                        About
                                    </Link>

                                    <Link
                                        to="/login"
                                        className="nav-link nav-login"
                                    >
                                        Login / Register
                                    </Link>

                                </nav>

                            </div>
                        </div>
                    </div>
                </header>
            )}

            <main>

                {/* HERO */}
                <section className="hero">

                    <div className="hero-content">

                        <h2 className="hero-title">
                            Find Government Schemes You're Eligible For
                        </h2>

                        <p className="hero-description">
                            DSGP is your unified platform to discover,
                            understand, apply for, and track government
                            subsidies and grants.
                            Access financial assistance schemes designed
                            for farmers, students, entrepreneurs, and
                            citizens across India.
                        </p>

                        <div className="hero-search">

                            <input
                                type="text"
                                className="search-input"
                                placeholder="Search for schemes by name, category, or keyword..."
                                aria-label="Search for schemes"
                            />

                        </div>

                        <div className="hero-actions">

                            <Link
                                to="/schemes"
                                className="btn btn-primary"
                            >
                                Explore Schemes
                            </Link>

                            <Link
                                to="/eligibility"
                                className="btn btn-secondary"
                            >
                                Check Eligibility
                            </Link>

                        </div>

                    </div>

                </section>

                {/* STATISTICS */}
                <section className="stats">

                    <div className="stats-container">

                        {stats.map((stat, index) => (

                            <div
                                key={index}
                                className="stat-card"
                            >

                                <div className="stat-value">
                                    {stat.value}
                                </div>

                                <div className="stat-label">
                                    {stat.label}
                                </div>

                            </div>

                        ))}

                    </div>

                </section>

                {/* FOCUS SECTORS */}
                <section className="categories">

                    <div>

                        <div className="section-header">

                            <h2 className="section-title">
                                Focus Sectors
                            </h2>

                            <p className="section-subtitle">
                                Explore schemes organized by key sectors
                                to find assistance programs relevant
                                to your field
                            </p>

                        </div>

                        <div className="categories-grid">

                            {categories.map((category) => (

                                <div
                                    key={category.id}
                                    className="category-card"
                                >

                                    <h3 className="category-name">
                                        {category.name}
                                    </h3>

                                    <p className="category-description">
                                        {category.description}
                                    </p>

                                    <span className="category-count">
                                        {category.count}
                                    </span>

                                </div>

                            ))}

                        </div>

                    </div>

                </section>

                {/* FEATURED SCHEMES */}
                <section className="featured-schemes">

                    <div>

                        <div className="section-header">

                            <h2 className="section-title">
                                Featured Schemes
                            </h2>

                            <p className="section-subtitle">
                                Explore our curated selection of major
                                government assistance programs
                            </p>

                        </div>

                        <div className="schemes-grid">

                            {featuredSchemes.map((scheme) => (

                                <div
                                    key={scheme.id}
                                    className="scheme-card"
                                >

                                    <div className="scheme-header">

                                        <span className="scheme-badge">
                                            {scheme.category}
                                        </span>

                                        <span className="scheme-target">
                                            {scheme.target}
                                        </span>

                                    </div>

                                    <h3 className="scheme-name">
                                        {scheme.name}
                                    </h3>

                                    <p className="scheme-description">
                                        {scheme.description}
                                    </p>

                                    <Link
                                        to={scheme.path}
                                        className="btn btn-primary scheme-card-button"
                                    >
                                        View Details
                                    </Link>

                                </div>

                            ))}

                        </div>

                    </div>

                </section>

                {/* ELIGIBILITY FINDER */}
                <section className="eligibility-finder">

                    <div>

                        <div className="section-header">

                            <h2 className="section-title">
                                Check Your Eligibility
                            </h2>

                            <p className="section-subtitle">
                                Answer a few questions to discover schemes
                                you may qualify for
                            </p>

                        </div>

                        <form
                            className="eligibility-form"
                            onSubmit={(e) => e.preventDefault()}
                        >

                            <div className="form-group">

                                <label htmlFor="age">
                                    Age
                                </label>

                                <input
                                    id="age"
                                    type="number"
                                    placeholder="Enter your age"
                                />

                            </div>

                            <div className="form-group">

                                <label htmlFor="occupation">
                                    Occupation
                                </label>

                                <select
                                    id="occupation"
                                    defaultValue=""
                                >

                                    <option
                                        value=""
                                        disabled
                                    >
                                        Select your occupation
                                    </option>

                                    <option value="farmer">
                                        Farmer
                                    </option>

                                    <option value="student">
                                        Student
                                    </option>

                                    <option value="entrepreneur">
                                        Entrepreneur
                                    </option>

                                </select>

                            </div>

                            <div className="form-group">

                                <label htmlFor="category">
                                    Social Category
                                </label>

                                <select
                                    id="category"
                                    defaultValue=""
                                >

                                    <option
                                        value=""
                                        disabled
                                    >
                                        Select your category
                                    </option>

                                    <option value="general">
                                        General
                                    </option>

                                    <option value="obc">
                                        OBC
                                    </option>

                                    <option value="sc">
                                        SC
                                    </option>

                                    <option value="st">
                                        ST
                                    </option>

                                    <option value="ews">
                                        EWS
                                    </option>

                                </select>

                            </div>

                            <button
                                type="submit"
                                className="btn btn-primary form-submit"
                            >
                                Find Eligible Schemes
                            </button>

                        </form>

                    </div>

                </section>

                {/* APPLICATION TRACKING */}
                <section className="tracking-section">

                    <div>

                        <div className="section-header">

                            <h2 className="section-title">
                                Track Your Application
                            </h2>

                            <p className="section-subtitle">
                                Enter your application ID to check the
                                current status of your submission
                            </p>

                        </div>

                        <form
                            className="tracking-form"
                            onSubmit={(e) => e.preventDefault()}
                        >

                            <input
                                type="text"
                                className="tracking-input"
                                placeholder="Enter Application ID (e.g., DSGP2024001234)"
                                aria-label="Application ID"
                            />

                            <button
                                type="submit"
                                className="btn btn-primary"
                            >
                                Track Status
                            </button>

                        </form>

                    </div>

                </section>

                {/* HOW IT WORKS */}
                <section className="how-it-works">

                    <div>

                        <div className="section-header">

                            <h2 className="section-title">
                                How DSGP Works
                            </h2>

                            <p className="section-subtitle">
                                A simple four-step process to access
                                government assistance programs
                            </p>

                        </div>

                        <div className="steps-grid">

                            {steps.map((step, index) => (

                                <div
                                    key={index}
                                    className="step-card"
                                >

                                    <div className="step-number">
                                        {step.number}
                                    </div>

                                    <h3 className="step-title">
                                        {step.title}
                                    </h3>

                                    <p className="step-description">
                                        {step.description}
                                    </p>

                                </div>

                            ))}

                        </div>

                    </div>

                </section>

            </main>

            {/* FOOTER */}
            <footer className="footer">

                <div className="footer-container">

                    <div className="footer-info">

                        <div className="footer-brand">

                            <h3>
                                Digital Subsidy & Grant Platform
                            </h3>

                            <p>
                                DSGP is an academic demonstration project
                                designed to showcase a unified digital
                                platform for accessing government subsidy
                                and grant schemes.
                            </p>

                            <p className="footer-disclaimer">

                                <strong>Disclaimer:</strong> This is an
                                educational project. For official scheme
                                information and applications, please visit
                                the respective government portals and verify
                                all details through authorized channels.

                            </p>

                        </div>

                        <div className="footer-links">

                            <h4>Quick Links</h4>

                            <Link to="/">
                                Home
                            </Link>

                            <Link to="/schemes">
                                Find Schemes
                            </Link>

                            <Link to="/eligibility">
                                Check Eligibility
                            </Link>

                            <Link to="/track">
                                Track Application
                            </Link>

                            <Link to="/about">
                                About DSGP
                            </Link>

                        </div>

                        <div className="footer-links">

                            <h4>Resources</h4>

                            <Link to="/help">
                                Help & Support
                            </Link>

                            <Link to="/faq">
                                FAQs
                            </Link>

                            <Link to="/contact">
                                Contact Us
                            </Link>

                            <Link to="/privacy">
                                Privacy Policy
                            </Link>

                            <Link to="/terms">
                                Terms of Service
                            </Link>

                        </div>

                    </div>

                    <div className="footer-bottom">

                        <p>
                            &copy; 2026 Digital Subsidy & Grant Platform
                            (DSGP) - Academic Project
                        </p>

                    </div>

                </div>

            </footer>

        </div>
    );
}

export default Home;