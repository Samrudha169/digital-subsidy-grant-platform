import { Link } from 'react-router-dom';
import './PMKisan.css';

function PMKisan() {
    return (
        <div className="scheme-detail-page">

            <section className="scheme-detail-hero">
                <div className="scheme-detail-container">
                    <span className="scheme-detail-badge">
                        Agriculture
                    </span>

                    <h1>PM-KISAN</h1>

                    <p className="scheme-full-name">
                        Pradhan Mantri Kisan Samman Nidhi
                    </p>

                    <p>
                        Income support scheme providing financial assistance
                        to eligible farmer families across India.
                    </p>
                </div>
            </section>

            <main className="scheme-detail-container">

                <section className="scheme-info">
                    <h2>About the Scheme</h2>

                    <p>
                        PM-KISAN is a government income support scheme for
                        eligible farmer families. Under the scheme, eligible
                        farmers receive financial assistance directly into
                        their bank accounts.
                    </p>
                </section>

                <section className="scheme-benefits">
                    <h2>Key Benefit</h2>

                    <div className="benefit-card">
                        <h3>₹6,000 per year</h3>
                        <p>
                            Financial assistance provided in three equal
                            installments of ₹2,000.
                        </p>
                    </div>
                </section>

                <section className="scheme-eligibility">
                    <h2>Basic Eligibility</h2>

                    <ul>
                        <li>Applicant should be an eligible farmer.</li>
                        <li>Applicant should meet the scheme's landholding requirements.</li>
                        <li>Valid identification and bank account details are required.</li>
                        <li>The applicant must satisfy the applicable government conditions.</li>
                    </ul>
                </section>

                <section className="scheme-documents">
                    <h2>Required Documents</h2>

                    <ul>
                        <li>Aadhaar Card</li>
                        <li>Bank Account Details</li>
                        <li>Land Ownership Records</li>
                        <li>Other documents as required by the authorities</li>
                    </ul>
                </section>

                <div className="scheme-actions">
                    <Link to="/eligibility" className="btn btn-primary">
                        Check Eligibility
                    </Link>

                    <Link to="/schemes" className="btn btn-secondary">
                        Back to Schemes
                    </Link>
                </div>

            </main>
        </div>
    );
}

export default PMKisan;