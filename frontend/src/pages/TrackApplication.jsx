import { useState } from 'react';
import './TrackApplication.css';

function TrackApplication() {

    const [applicationId, setApplicationId] = useState('');
    const [status, setStatus] = useState(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!applicationId.trim()) {
            setStatus({
                type: 'error',
                message: 'Please enter your Application ID.'
            });
            return;
        }

        try {
            setLoading(true);
            setStatus(null);

            const response = await fetch(
                `http://localhost:8080/api/v1/applications/${applicationId}`
            );

            if (!response.ok) {
                throw new Error('Application not found');
            }

            const data = await response.json();

            setStatus({
                type: 'success',
                message: `Application ${data.id} found successfully. Status: ${data.status || data.applicationStatus || 'Available'}`
            });

        } catch (error) {
            console.error('Error tracking application:', error);

            setStatus({
                type: 'error',
                message: `Application ${applicationId} was not found.`
            });

        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="track-page">

            {/* Page Header */}
            <section className="track-hero">

                <div className="track-container">

                    <h1>
                        Track Your Application
                    </h1>

                    <p>
                        Enter your application ID below to check the
                        current status of your government scheme application.
                    </p>

                </div>

            </section>


            {/* Tracking Section */}
            <section className="track-section">

                <div className="track-container">

                    <div className="track-card">

                        <div className="track-card-header">

                            <h2>
                                Application Status
                            </h2>

                            <p>
                                Enter the application ID provided to you
                                after submitting your application.
                            </p>

                        </div>


                        {/* Tracking Form */}
                        <form
                            className="track-form"
                            onSubmit={handleSubmit}
                        >

                            <div className="track-form-group">

                                <label htmlFor="applicationId">
                                    Application ID
                                </label>

                                <input
                                    id="applicationId"
                                    type="text"
                                    value={applicationId}
                                    onChange={(e) =>
                                        setApplicationId(e.target.value)
                                    }
                                    placeholder="Enter Application ID"
                                    aria-label="Application ID"
                                />

                            </div>


                            <button
                                type="submit"
                                className="track-button"
                                disabled={loading}
                            >
                                {loading
                                    ? 'Checking...'
                                    : 'Track Status'}
                            </button>

                        </form>


                        {/* Result */}
                        {status && (

                            <div
                                className={`track-result ${status.type}`}
                            >

                                <h3>
                                    {status.type === 'success'
                                        ? 'Application Found'
                                        : 'Unable to Track Application'}
                                </h3>

                                <p>
                                    {status.message}
                                </p>

                            </div>

                        )}


                        {/* Help Information */}
                        <div className="track-help">

                            <h3>
                                Where can I find my Application ID?
                            </h3>

                            <p>
                                Your Application ID is provided after
                                successfully submitting a scheme application.
                                It may also be available in your application
                                confirmation or acknowledgement message.
                            </p>

                        </div>

                    </div>

                </div>

            </section>


            {/* Status Information */}
            <section className="track-info-section">

                <div className="track-container">

                    <div className="track-section-header">

                        <h2>
                            Application Process
                        </h2>

                        <p>
                            Your application generally moves through
                            the following stages.
                        </p>

                    </div>


                    <div className="track-status-grid">

                        <div className="track-status-card">

                            <div className="track-status-number">
                                01
                            </div>

                            <h3>
                                Submitted
                            </h3>

                            <p>
                                Your application has been successfully
                                submitted.
                            </p>

                        </div>


                        <div className="track-status-card">

                            <div className="track-status-number">
                                02
                            </div>

                            <h3>
                                Under Review
                            </h3>

                            <p>
                                The submitted information and documents
                                are being reviewed.
                            </p>

                        </div>


                        <div className="track-status-card">

                            <div className="track-status-number">
                                03
                            </div>

                            <h3>
                                Approved
                            </h3>

                            <p>
                                Your application has been approved
                                after successful verification.
                            </p>

                        </div>


                        <div className="track-status-card">

                            <div className="track-status-number">
                                04
                            </div>

                            <h3>
                                Completed
                            </h3>

                            <p>
                                The application process has been completed
                                and assistance can be processed.
                            </p>

                        </div>

                    </div>

                </div>

            </section>

        </div>
    );
}

export default TrackApplication;