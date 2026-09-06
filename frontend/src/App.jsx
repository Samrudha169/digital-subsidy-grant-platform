import { Link, Routes, Route } from 'react-router-dom';
import { useState } from 'react';
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
import Dashboard from './pages/Dashboard';

function App() {

    const [isLoggedIn, setIsLoggedIn] = useState(
        localStorage.getItem('isLoggedIn') === 'true'
    );

    const navLinks = [
        { name: 'Home', path: '/' },
        { name: 'Find Schemes', path: '/schemes' },
        { name: 'Eligibility', path: '/eligibility' },
        { name: 'Track Application', path: '/track' },
        { name: 'About', path: '/about' }
    ];

    return (
        <div className="dsgp-app">

            <header className="header">
                <div className="site-header">
                    <div className="header-inner">
                        <div className="header-container">

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

                                {isLoggedIn ? (
                                    <Link
                                        to="/dashboard"
                                        className="nav-link nav-login"
                                    >
                                        Dashboard
                                    </Link>
                                ) : (
                                    <Link
                                        to="/login"
                                        className="nav-link nav-login"
                                    >
                                        Login / Register
                                    </Link>
                                )}

                            </nav>

                        </div>
                    </div>
                </div>
            </header>


            <main>

                <Routes>

                    <Route
                        path="/"
                        element={<Home />}
                    />

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
                        element={
                            <Login
                                onLogin={() => setIsLoggedIn(true)}
                            />
                        }
                    />


                    <Route
                        path="/register"
                        element={<Register />}
                    />


                    <Route
                        path="/dashboard"
                        element={
                            <Dashboard
                                onLogout={() => {
                                    setIsLoggedIn(false);
                                }}
                            />
                        }
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

                    <Route
                        path="/forgot-password"
                        element={<ForgotPassword />}
                    />

                    <Route
                        path="/privacy"
                        element={<Privacy />}
                    />

                    <Route
                        path="/terms"
                        element={<Terms />}
                    />

                </Routes>

            </main>

        </div>
    );
}

export default App;