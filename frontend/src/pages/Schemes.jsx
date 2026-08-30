import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import './Schemes.css';

function Schemes() {
    console.log("SCHEMES COMPONENT RENDERED");

    const schemes = [
        {
            id: 1,
            name: 'PM-KISAN',
            fullName: 'Pradhan Mantri Kisan Samman Nidhi',
            category: 'Agriculture',
            target: 'Farmers',
            description:
                'Income support scheme providing ₹6,000 per year to eligible farmer families in three equal installments.',
            benefits: '₹6,000 per year',
            path: '/schemes/pm-kisan'
        },
        {
            id: 2,
            name: 'NSP',
            fullName: 'National Scholarship Portal',
            category: 'Education',
            target: 'Students',
            description:
                'Centralized platform offering various scholarships for students from pre-matric to post-matric levels.',
            benefits: 'Multiple Scholarships',
            path: '/schemes/nsp'
        },
        {
            id: 3,
            name: 'PMEGP',
            fullName: "Prime Minister's Employment Generation Programme",
            category: 'Business & Entrepreneurship',
            target: 'Entrepreneurs',
            description:
                'Credit-linked subsidy scheme supporting self-employment through the establishment of micro-enterprises.',
            benefits: 'Credit-linked Subsidy',
            path: '/schemes/pmegp'
        }
    ];

    /*
     * Get search value from the URL.
     *
     * Example:
     * /schemes?search=farmer
     */
    const [searchParams, setSearchParams] = useSearchParams();

    const initialSearch = searchParams.get('search') || '';

    const [searchTerm, setSearchTerm] = useState(initialSearch);

    /*
     * Filter schemes based on the search term.
     */
    const filteredSchemes = schemes.filter((scheme) => {
        const search = searchTerm.toLowerCase().trim();

        return (
            scheme.name.toLowerCase().includes(search) ||
            scheme.fullName.toLowerCase().includes(search) ||
            scheme.category.toLowerCase().includes(search) ||
            scheme.target.toLowerCase().includes(search) ||
            scheme.description.toLowerCase().includes(search)
        );
    });

    /*
     * Handle search from the Schemes page.
     */
    const handleSearch = (e) => {
        const value = e.target.value;

        setSearchTerm(value);

        if (value.trim() === '') {
            setSearchParams({});
        } else {
            setSearchParams({
                search: value
            });
        }
    };

    return (
        <div className="schemes-page">

            {/* PAGE HEADER */}
            <section className="schemes-hero">

                <div className="schemes-container">

                    <h1>
                        Find Government Schemes
                    </h1>

                    <p>
                        Explore government subsidies, grants, scholarships,
                        and assistance programs available through DSGP.
                    </p>

                    {/* SEARCH BAR */}
                    <div className="schemes-search">

                        <input
                            type="text"
                            value={searchTerm}
                            onChange={handleSearch}
                            placeholder="Search schemes by name, category, or keyword..."
                            aria-label="Search schemes"
                        />

                    </div>

                </div>

            </section>


            {/* SCHEMES SECTION */}
            <section className="schemes-list-section">

                <div className="schemes-container">

                    <div className="schemes-section-header">

                        <h2>
                            Available Schemes
                        </h2>

                        <p>
                            {filteredSchemes.length} scheme
                            {filteredSchemes.length !== 1 ? 's' : ''}
                            {' '}found
                        </p>

                    </div>


                    {/* RESULTS */}
                    {filteredSchemes.length > 0 ? (

                        <div className="schemes-page-grid">

                            {filteredSchemes.map((scheme) => (

                                <div
                                    className="scheme-page-card"
                                    key={scheme.id}
                                >

                                    {/* CATEGORY + TARGET */}
                                    <div className="scheme-page-header">

                                        <span className="scheme-category">
                                            {scheme.category}
                                        </span>

                                        <span className="scheme-page-target">
                                            {scheme.target}
                                        </span>

                                    </div>


                                    {/* SCHEME NAME */}
                                    <h3>
                                        {scheme.name}
                                    </h3>


                                    {/* FULL NAME */}
                                    <p className="scheme-full-name">
                                        {scheme.fullName}
                                    </p>


                                    {/* DESCRIPTION */}
                                    <p className="scheme-page-description">
                                        {scheme.description}
                                    </p>


                                    {/* BENEFIT */}
                                    <div className="scheme-benefit">

                                        <span>
                                            Key Benefit
                                        </span>

                                        <strong>
                                            {scheme.benefits}
                                        </strong>

                                    </div>


                                    {/* DETAILS BUTTON */}
                                    <Link
                                        to={scheme.path}
                                        className="scheme-view-btn"
                                    >
                                        View Details
                                    </Link>

                                </div>

                            ))}

                        </div>

                    ) : (

                        /* NO RESULTS */
                        <div className="no-schemes">

                            <h3>
                                No Schemes Found
                            </h3>

                            <p>
                                Try searching with a different scheme name,
                                category, or keyword.
                            </p>

                        </div>

                    )}

                </div>

            </section>

        </div>
    );
}

export default Schemes;