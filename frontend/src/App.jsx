import { Link, Routes, Route } from 'react-router-dom';
import './App.css';

import Home from './pages/Home';
import Schemes from './pages/Schemes';
import PMKisan from './pages/PMKisan';
import Eligibility from './pages/Eligibility';
import TrackApplication from './pages/TrackApplication';
import About from './pages/About';
import Login from './pages/Login';
import NSP from './pages/NSP';
import PMEGP from './pages/PMEGP';
import Help from './pages/Help';
import FAQ from './pages/FAQ';
import Contact from './pages/Contact';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import Privacy from './pages/Privacy';
import Terms from './pages/Terms';

function App() {

    // Navigation links
    const navLinks = [
        { name: 'Home', path: '/' },
        { name: 'Find Schemes', path: '/schemes' },
        { name: 'Eligibility', path: '/eligibility' },
        { name: 'Track Application', path: '/track' },
        { name: 'About', path: '/about' }
    ];

    return (
        <div className="dsgp-app">

            {/* ================= HEADER ================= */}

            <header className="header">

                <div className="site-header">

                    <div className="header-inner">

                        <div className="header-container">

                            {/* BRAND */}

                            <div className="brand">

                                <Link
                                    to="/"
                                    style={{
                                        textDecoration: 'none',
                                        color: 'inherit'
                                    }}
                                >
                                    <h1 className="brand-title">
                                        DSGP
                                    </h1>

                                    <p className="brand-subtitle">
                                        Digital Subsidy & Grant Platform
                                    </p>
                                </Link>

                            </div>


                            {/* NAVIGATION */}

                            <nav className="nav">

                                {navLinks.map((link, index) => (

                                    <Link
                                        key={index}
                                        to={link.path}
                                        className="nav-link"
                                    >
                                        {link.name}
                                    </Link>

                                ))}


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


            {/* ================= PAGES ================= */}

            <main>

                <Routes>

                    {/* HOME */}

                    <Route
                        path="/"
                        element={<Home />}
                    />


                    {/* SCHEMES */}

                    <Route
                        path="/schemes"
                        element={<Schemes />}
                    />

                    <Route
                        path="/schemes/pm-kisan"
                        element={<PMKisan />}
                    />

                    <Route
                        path="/schemes/nsp"
                        element={<NSP />}
                    />

                    <Route
                        path="/schemes/pmegp"
                        element={<PMEGP />}
                    />


                    {/* OTHER PAGES */}

                    <Route
                        path="/eligibility"
                        element={<Eligibility />}
                    />

                    <Route
                        path="/track"
                        element={<TrackApplication />}
                    />

                    <Route
                        path="/about"
                        element={<About />}
                    />

                    <Route
                        path="/login"
                        element={<Login />}
                    />

                    <Route
                        path="/help"
                        element={<Help />}
                    />

                    <Route
                        path="/faq"
                        element={<FAQ />}
                    />

                    <Route
                        path="/contact"
                        element={<Contact />}
                    />

                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />

                    <Route
                        path="/forgot-password"
                        element={<ForgotPassword />}
                    />

                    <Route path="/privacy" element={<Privacy />} />
                    <Route path="/terms" element={<Terms />} />

                </Routes>

            </main>

        </div>
    );
}

export default App;