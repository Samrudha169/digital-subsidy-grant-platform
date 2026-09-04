import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import './Schemes.css';

function Schemes() {
    console.log("SCHEMES COMPONENT RENDERED");

    const [schemes, setSchemes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

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
     * Load schemes from Spring Boot backend.
     */
    useEffect(() => {
        const fetchSchemes = async () => {
            try {
                setLoading(true);
                setError('');

                const response = await fetch(
                    'http://localhost:8080/api/v1/schemes'
                );

                if (!response.ok) {
                    throw new Error(
                        `Failed to fetch schemes. Status: ${response.status}`
                    );
                }

                const data = await response.json();

                /*
                 * Backend returns an array of schemes.
                 */
                const backendSchemes = Array.isArray(data)
                    ? data
                    : Array.isArray(data.content)
                        ? data.content
                        : [];

                /*
                 * Remove the test scheme.
                 *
                 * ID 4 = UPDATED TEST SCHEME
                 *
                 * We only want the three actual schemes:
                 * 1. PM-KISAN
                 * 2. NSP
                 * 3. PMEGP
                 */
                const actualSchemes = backendSchemes.filter(
                    (scheme) =>
                        scheme.schemeName !== 'UPDATED TEST SCHEME'
                );

                setSchemes(actualSchemes);

            } catch (err) {
                console.error('Error loading schemes:', err);

                setError(
                    'Unable to load schemes from the server. Please make sure the backend is running.'
                );

            } finally {
                setLoading(false);
            }
        };

        fetchSchemes();
    }, []);

    /*
     * Convert backend scheme data into the format
     * required by the existing frontend design.
     */
    const formattedSchemes = schemes.map((scheme) => {
        const schemeName =
            scheme.schemeName || 'Government Scheme';

        let fullName = schemeName;
        let category = 'Government Scheme';
        let target = 'Eligible Beneficiaries';
        let path = '#';
        let benefits = 'Benefits as per scheme';

        /*
         * PM-KISAN
         */
        if (schemeName === 'PM-KISAN') {
            fullName =
                'Pradhan Mantri Kisan Samman Nidhi';

            category = 'Agriculture';
            target = 'Farmers';
            path = '/schemes/pm-kisan';

            benefits =
                scheme.grantAmount !== null &&
                scheme.grantAmount !== undefined
                    ? `₹${Number(scheme.grantAmount).toLocaleString('en-IN')} per year`
                    : '₹6,000 per year';
        }

        /*
         * NSP
         */
        else if (schemeName === 'NSP') {
            fullName =
                'National Scholarship Portal';

            category = 'Education';
            target = 'Students';
            path = '/schemes/nsp';

            benefits = 'Multiple Scholarships';
        }

        /*
         * PMEGP
         */
        else if (schemeName === 'PMEGP') {
            fullName =
                "Prime Minister's Employment Generation Programme";

            category = 'Business & Entrepreneurship';
            target = 'Entrepreneurs';
            path = '/schemes/pmegp';

            benefits = 'Credit-linked Subsidy';
        }

        return {
            id: scheme.id,
            name: schemeName,
            fullName: fullName,
            category: category,
            target: target,
            description:
                scheme.description ||
                'Government scheme providing financial assistance to eligible beneficiaries.',
            benefits: benefits,
            path: path,
            active: scheme.active
        };
    });

    /*
     * Filter schemes based on the search term.
     */
    const filteredSchemes = formattedSchemes.filter((scheme) => {
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
                            {loading
                                ? 'Loading schemes...'
                                : `${filteredSchemes.length} scheme${filteredSchemes.length !== 1 ? 's' : ''} found`
                            }
                        </p>

                    </div>


                    {/* LOADING */}
                    {loading && (

                        <div className="no-schemes">

                            <h3>
                                Loading Schemes...
                            </h3>

                            <p>
                                Please wait while we load the available
                                government schemes.
                            </p>

                        </div>

                    )}


                    {/* ERROR */}
                    {!loading && error && (

                        <div className="no-schemes">

                            <h3>
                                Unable to Load Schemes
                            </h3>

                            <p>
                                {error}
                            </p>

                        </div>

                    )}


                    {/* RESULTS */}
                    {!loading && !error && filteredSchemes.length > 0 && (

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

                    )}


                    {/* NO RESULTS */}
                    {!loading && !error && filteredSchemes.length === 0 && (

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