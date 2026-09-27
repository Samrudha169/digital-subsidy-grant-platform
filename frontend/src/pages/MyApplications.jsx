import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import '../App.css';

const API_BASE = 'http://localhost:8080/api/v1';

function MyApplications() {

    const navigate = useNavigate();

    const [applications, setApplications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Stores the upload stage for each application.
    // Example:
    // {
    //     5: 2,
    //     8: 3
    // }
    const [uploadStages, setUploadStages] = useState({});

    // Stores documents already uploaded for each application.
    const [stageDocuments, setStageDocuments] = useState({});

    // Stores selected files before upload.
    const [selectedFiles, setSelectedFiles] = useState({});

    // Stores upload status for individual document types.
    const [uploading, setUploading] = useState({});

    // Stores upload errors.
    const [uploadErrors, setUploadErrors] = useState({});


    // ═══════════════════════════════════════════════════════════════════════
    // LOAD APPLICATIONS
    // ═══════════════════════════════════════════════════════════════════════

    useEffect(() => {

        const beneficiaryId = localStorage.getItem('beneficiaryId');
        const isLoggedIn = localStorage.getItem('isLoggedIn');

        if (!beneficiaryId || isLoggedIn !== 'true') {
            navigate('/login');
            return;
        }

        const fetchApplications = async () => {

            try {

                const response = await fetch(
                    `${API_BASE}/applications/beneficiary/${beneficiaryId}`
                );

                if (!response.ok) {
                    throw new Error('Unable to fetch applications');
                }

                const data = await response.json();

                setApplications(data);

            } catch (err) {

                console.error('My Applications error:', err);
                setError('Unable to load your applications.');

            } finally {

                setLoading(false);

            }
        };

        fetchApplications();

    }, [navigate]);


    // ═══════════════════════════════════════════════════════════════════════
    // LOAD DISBURSEMENT STAGE FOR EACH APPLICATION
    // ═══════════════════════════════════════════════════════════════════════

    useEffect(() => {

        if (!applications.length) {
            return;
        }

        const loadDisbursementStages = async () => {

            const beneficiaryId =
                localStorage.getItem('beneficiaryId');

            if (!beneficiaryId) {
                return;
            }

            const newUploadStages = {};
            const newStageDocuments = {};

            for (const application of applications) {

                try {

                    // -------------------------------------------------------
                    // 1. Get disbursement plan
                    // -------------------------------------------------------

                    const planResponse = await fetch(
                        `${API_BASE}/api/disbursements/application/${application.applicationId}/plan`
                    );

                    if (!planResponse.ok) {

                        // No disbursement plan yet.
                        continue;
                    }

                    const plan = await planResponse.json();

                    if (!plan || !plan.id) {
                        continue;
                    }


                    // -------------------------------------------------------
                    // 2. Get stages belonging to this plan
                    // -------------------------------------------------------

                    const stagesResponse = await fetch(
                        `${API_BASE}/api/disbursements/${plan.id}/stages`
                    );

                    if (!stagesResponse.ok) {
                        continue;
                    }

                    const stages = await stagesResponse.json();

                    if (!Array.isArray(stages) || stages.length === 0) {
                        continue;
                    }


                    // -------------------------------------------------------
                    // 3. Determine the stage where beneficiary should
                    //    currently upload evidence.
                    //
                    // Stage 1 does not require a new beneficiary upload.
                    //
                    // Therefore:
                    //
                    // Stage 1 PENDING       → no upload
                    // Stage 1 RELEASED      → upload Stage 2 evidence
                    // Stage 2 RELEASED      → upload Stage 3 evidence
                    // All RELEASED          → no upload
                    // -------------------------------------------------------

                    const sortedStages = [...stages].sort(
                        (a, b) =>
                            Number(a.stageNumber) -
                            Number(b.stageNumber)
                    );

                    let currentUploadStage = null;

                    for (const stage of sortedStages) {

                        const stageNumber =
                            Number(stage.stageNumber);

                        const status =
                            String(stage.status || '').toUpperCase();

                        if (status !== 'RELEASED') {

                            // Stage 1 does not ask beneficiary
                            // to upload documents.
                            if (stageNumber >= 2) {
                                currentUploadStage = stageNumber;
                            }

                            break;
                        }
                    }


                    if (
                        currentUploadStage === 2 ||
                        currentUploadStage === 3
                    ) {

                        newUploadStages[application.applicationId] =
                            currentUploadStage;

                        // ---------------------------------------------------
                        // Load documents already uploaded for this stage.
                        // ---------------------------------------------------

                        const documentsResponse = await fetch(
                            `${API_BASE}/beneficiaries/${beneficiaryId}/documents/application/${application.applicationId}/stage/${currentUploadStage}`
                        );

                        if (documentsResponse.ok) {

                            const documents =
                                await documentsResponse.json();

                            newStageDocuments[
                                application.applicationId
                                ] = documents;
                        } else {

                            newStageDocuments[
                                application.applicationId
                                ] = [];
                        }
                    }

                } catch (err) {

                    console.error(
                        `Unable to load disbursement stage for application ${application.applicationId}:`,
                        err
                    );

                }
            }

            setUploadStages(newUploadStages);
            setStageDocuments(newStageDocuments);
        };

        loadDisbursementStages();

    }, [applications]);


    // ═══════════════════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════════════════

    const getStatusClass = (status) => {

        if (!status) {
            return 'application-status pending';
        }

        const normalizedStatus = status.toUpperCase();

        if (
            normalizedStatus.includes('APPROVED') ||
            normalizedStatus.includes('SANCTIONED') ||
            normalizedStatus.includes('COMPLETED')
        ) {
            return 'application-status approved';
        }

        if (
            normalizedStatus.includes('REJECTED') ||
            normalizedStatus.includes('FAILED')
        ) {
            return 'application-status rejected';
        }

        return 'application-status pending';
    };


    // ═══════════════════════════════════════════════════════════════════════
    // FORMAT DATE
    // ═══════════════════════════════════════════════════════════════════════

    const formatDate = (date) => {

        if (!date) {
            return '—';
        }

        try {

            return new Date(date).toLocaleDateString('en-IN', {
                day: '2-digit',
                month: 'short',
                year: 'numeric'
            });

        } catch {
            return date;
        }
    };


    // ═══════════════════════════════════════════════════════════════════════
    // FORMAT AMOUNT
    // ═══════════════════════════════════════════════════════════════════════

    const formatAmount = (amount) => {

        if (amount === null || amount === undefined) {
            return 'Not sanctioned yet';
        }

        return `₹${Number(amount).toLocaleString('en-IN')}`;
    };


    // ═══════════════════════════════════════════════════════════════════════
    // DOCUMENT REQUIREMENTS
    // ═══════════════════════════════════════════════════════════════════════

    const getStageRequirements = (stageNumber) => {

        if (Number(stageNumber) === 2) {

            return [
                {
                    type: 'STAGE_2_INVOICE',
                    title: 'Equipment / Purchase Invoice',
                    description:
                        'Upload the invoice for the equipment or approved purchase.',
                    accept:
                        '.pdf,.jpg,.jpeg,.png,.webp'
                },
                {
                    type: 'STAGE_2_PAYMENT_PROOF',
                    title: 'Payment / Transaction Proof',
                    description:
                        'Upload the receipt, bank transaction proof or payment confirmation.',
                    accept:
                        '.pdf,.jpg,.jpeg,.png,.webp'
                },
                {
                    type: 'STAGE_2_ACTIVITY_PHOTO',
                    title: 'Equipment / Activity Photos',
                    description:
                        'Upload photos showing the purchased equipment or completed activity.',
                    accept:
                        '.jpg,.jpeg,.png,.webp'
                }
            ];
        }

        if (Number(stageNumber) === 3) {

            return [
                {
                    type: 'FINAL_COMPLETION_REPORT',
                    title: 'Final Completion Report',
                    description:
                        'Upload the final completion report in PDF format.',
                    accept:
                        '.pdf'
                },
                {
                    type: 'UTILIZATION_STATEMENT',
                    title: 'Utilization / Expenditure Statement',
                    description:
                        'Upload the statement showing utilization of funds already released in Stages 1 and 2.',
                    accept:
                        '.pdf,.jpg,.jpeg,.png,.webp'
                },
                {
                    type: 'FINAL_PROJECT_PHOTO',
                    title: 'Final Project / Equipment Photos',
                    description:
                        'Upload final photos showing the completed project or equipment.',
                    accept:
                        '.jpg,.jpeg,.png,.webp'
                },
                {
                    type: 'FINAL_PAYMENT_PROOF',
                    title: 'Remaining Invoice / Payment Proof',
                    description:
                        'Upload remaining invoices or payment proofs, if applicable.',
                    accept:
                        '.pdf,.jpg,.jpeg,.png,.webp'
                }
            ];
        }

        return [];
    };


    // ═══════════════════════════════════════════════════════════════════════
    // SELECT FILE
    // ═══════════════════════════════════════════════════════════════════════

    const handleFileSelect = (
        applicationId,
        documentType,
        file
    ) => {

        if (!file) {
            return;
        }

        setSelectedFiles((previous) => ({
            ...previous,
            [`${applicationId}_${documentType}`]: file
        }));

        setUploadErrors((previous) => ({
            ...previous,
            [`${applicationId}_${documentType}`]: ''
        }));
    };


    // ═══════════════════════════════════════════════════════════════════════
    // UPLOAD DOCUMENT
    // ═══════════════════════════════════════════════════════════════════════

    const handleUpload = async (
        applicationId,
        stageNumber,
        documentType
    ) => {

        const beneficiaryId =
            localStorage.getItem('beneficiaryId');

        const key =
            `${applicationId}_${documentType}`;

        const file = selectedFiles[key];

        if (!beneficiaryId) {

            setUploadErrors((previous) => ({
                ...previous,
                [key]: 'Beneficiary information is missing. Please log in again.'
            }));

            return;
        }

        if (!file) {

            setUploadErrors((previous) => ({
                ...previous,
                [key]: 'Please select a file first.'
            }));

            return;
        }


        // ---------------------------------------------------------------
        // Basic file size validation
        // ---------------------------------------------------------------

        const maxSize = 10 * 1024 * 1024;

        if (file.size > maxSize) {

            setUploadErrors((previous) => ({
                ...previous,
                [key]: 'File size must not exceed 10 MB.'
            }));

            return;
        }


        setUploading((previous) => ({
            ...previous,
            [key]: true
        }));

        setUploadErrors((previous) => ({
            ...previous,
            [key]: ''
        }));


        try {

            const formData = new FormData();

            formData.append('file', file);
            formData.append('documentType', documentType);
            formData.append(
                'applicationId',
                String(applicationId)
            );
            formData.append(
                'stageNumber',
                String(stageNumber)
            );
            formData.append(
                'uploadedBy',
                `beneficiary-${beneficiaryId}`
            );


            const response = await fetch(
                `${API_BASE}/beneficiaries/${beneficiaryId}/documents`,
                {
                    method: 'POST',
                    body: formData
                }
            );


            if (!response.ok) {

                let message =
                    'Unable to upload document.';

                try {

                    const errorData =
                        await response.json();

                    message =
                        errorData.message ||
                        errorData.error ||
                        message;

                } catch {
                    // Keep default message.
                }

                throw new Error(message);
            }


            const uploadedDocument =
                await response.json();


            // -----------------------------------------------------------
            // Update document list immediately.
            // -----------------------------------------------------------

            setStageDocuments((previous) => {

                const existing =
                    previous[applicationId] || [];

                const withoutSameType =
                    existing.filter(
                        (document) =>
                            document.documentType !==
                            documentType
                    );

                return {
                    ...previous,
                    [applicationId]: [
                        ...withoutSameType,
                        uploadedDocument
                    ]
                };
            });


            // Remove selected file.
            setSelectedFiles((previous) => {

                const updated = {
                    ...previous
                };

                delete updated[key];

                return updated;
            });


        } catch (err) {

            console.error(
                'Document upload error:',
                err
            );

            setUploadErrors((previous) => ({
                ...previous,
                [key]:
                    err.message ||
                    'Unable to upload document.'
            }));

        } finally {

            setUploading((previous) => ({
                ...previous,
                [key]: false
            }));
        }
    };


    // ═══════════════════════════════════════════════════════════════════════
    // GET UPLOADED DOCUMENT
    // ═══════════════════════════════════════════════════════════════════════

    const getUploadedDocument = (
        applicationId,
        documentType
    ) => {

        const documents =
            stageDocuments[applicationId] || [];

        return documents.find(
            (document) =>
                document.documentType === documentType
        );
    };


    // ═══════════════════════════════════════════════════════════════════════
    // DOWNLOAD DOCUMENT
    // ═══════════════════════════════════════════════════════════════════════

    const handleDownload = (
        documentId
    ) => {

        const beneficiaryId =
            localStorage.getItem('beneficiaryId');

        if (!beneficiaryId || !documentId) {
            return;
        }

        window.open(
            `${API_BASE}/beneficiaries/${beneficiaryId}/documents/${documentId}/download`,
            '_blank'
        );
    };


    // ═══════════════════════════════════════════════════════════════════════
    // UPLOAD SECTION
    // ═══════════════════════════════════════════════════════════════════════

    const renderStageUploadSection = (application) => {

        const applicationId =
            application.applicationId;

        const stageNumber =
            uploadStages[applicationId];

        // Stage 1 or no active upload stage.
        if (
            stageNumber !== 2 &&
            stageNumber !== 3
        ) {
            return null;
        }

        const requirements =
            getStageRequirements(stageNumber);

        if (!requirements.length) {
            return null;
        }


        return (
            <div
                style={{
                    marginTop: '25px',
                    padding: '22px',
                    borderRadius: '10px',
                    background: '#f8fafc',
                    border: '1px solid #dbeafe'
                }}
            >

                {/* ================= TITLE ================= */}

                <div
                    style={{
                        marginBottom: '20px'
                    }}
                >

                    <h4
                        style={{
                            margin: 0,
                            color: '#0f172a',
                            fontSize: '18px'
                        }}
                    >
                        Stage {stageNumber} Document Submission
                    </h4>

                    <p
                        style={{
                            margin:
                                '7px 0 0',
                            color: '#64748b',
                            lineHeight: '1.5'
                        }}
                    >
                        {stageNumber === 2
                            ? 'Please upload evidence showing how the Stage 1 funds were used for the approved purchase or activity.'
                            : 'Please upload the final evidence required before the final disbursement can be verified.'}
                    </p>

                </div>


                {/* ================= DOCUMENT CARDS ================= */}

                <div
                    style={{
                        display: 'grid',
                        gap: '15px'
                    }}
                >

                    {requirements.map((requirement) => {

                        const key =
                            `${applicationId}_${requirement.type}`;

                        const selectedFile =
                            selectedFiles[key];

                        const uploadedDocument =
                            getUploadedDocument(
                                applicationId,
                                requirement.type
                            );

                        const isUploading =
                            uploading[key];

                        const uploadError =
                            uploadErrors[key];


                        return (
                            <div
                                key={requirement.type}
                                style={{
                                    background: '#ffffff',
                                    border:
                                        '1px solid #e2e8f0',
                                    borderRadius: '9px',
                                    padding: '18px'
                                }}
                            >

                                <div
                                    style={{
                                        display: 'flex',
                                        justifyContent:
                                            'space-between',
                                        alignItems:
                                            'flex-start',
                                        gap: '15px',
                                        flexWrap: 'wrap'
                                    }}
                                >

                                    <div
                                        style={{
                                            flex: 1,
                                            minWidth: '220px'
                                        }}
                                    >

                                        <h5
                                            style={{
                                                margin: 0,
                                                color: '#0f172a',
                                                fontSize: '15px'
                                            }}
                                        >
                                            {requirement.title}
                                        </h5>

                                        <p
                                            style={{
                                                margin:
                                                    '6px 0 0',
                                                color: '#64748b',
                                                fontSize: '13px',
                                                lineHeight:
                                                    '1.5'
                                            }}
                                        >
                                            {requirement.description}
                                        </p>

                                    </div>


                                    {/* UPLOADED STATUS */}

                                    {uploadedDocument && (

                                        <span
                                            style={{
                                                padding:
                                                    '5px 9px',
                                                borderRadius:
                                                    '15px',
                                                background:
                                                    '#dcfce7',
                                                color:
                                                    '#166534',
                                                fontSize:
                                                    '12px',
                                                fontWeight:
                                                    '700'
                                            }}
                                        >
                                            ✓ Uploaded
                                        </span>

                                    )}

                                </div>


                                {/* ================= FILE INPUT ================= */}

                                <div
                                    style={{
                                        marginTop:
                                            '15px',
                                        display: 'flex',
                                        gap: '10px',
                                        alignItems:
                                            'center',
                                        flexWrap:
                                            'wrap'
                                    }}
                                >

                                    <input
                                        type="file"
                                        accept={
                                            requirement.accept
                                        }
                                        onChange={(event) =>
                                            handleFileSelect(
                                                applicationId,
                                                requirement.type,
                                                event.target.files?.[0]
                                            )
                                        }
                                        disabled={
                                            isUploading
                                        }
                                        style={{
                                            flex: 1,
                                            minWidth:
                                                '230px'
                                        }}
                                    />


                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleUpload(
                                                applicationId,
                                                stageNumber,
                                                requirement.type
                                            )
                                        }
                                        disabled={
                                            isUploading ||
                                            !selectedFile
                                        }
                                        style={{
                                            padding:
                                                '9px 16px',
                                            border: 'none',
                                            borderRadius:
                                                '7px',
                                            background:
                                                isUploading ||
                                                !selectedFile
                                                    ? '#94a3b8'
                                                    : '#2563eb',
                                            color:
                                                '#ffffff',
                                            fontWeight:
                                                '600',
                                            cursor:
                                                isUploading ||
                                                !selectedFile
                                                    ? 'not-allowed'
                                                    : 'pointer'
                                        }}
                                    >
                                        {isUploading
                                            ? 'Uploading...'
                                            : uploadedDocument
                                                ? 'Replace'
                                                : 'Upload'}
                                    </button>

                                </div>


                                {/* ================= SELECTED FILE ================= */}

                                {selectedFile && (

                                    <p
                                        style={{
                                            margin:
                                                '8px 0 0',
                                            fontSize:
                                                '12px',
                                            color:
                                                '#475569'
                                        }}
                                    >
                                        Selected:{' '}
                                        <strong>
                                            {selectedFile.name}
                                        </strong>
                                    </p>

                                )}


                                {/* ================= ERROR ================= */}

                                {uploadError && (

                                    <p
                                        style={{
                                            margin:
                                                '8px 0 0',
                                            fontSize:
                                                '12px',
                                            color:
                                                '#b91c1c'
                                        }}
                                    >
                                        {uploadError}
                                    </p>

                                )}


                                {/* ================= EXISTING FILE ================= */}

                                {uploadedDocument && (

                                    <div
                                        style={{
                                            marginTop:
                                                '12px',
                                            padding:
                                                '10px',
                                            background:
                                                '#f1f5f9',
                                            borderRadius:
                                                '6px',
                                            display: 'flex',
                                            justifyContent:
                                                'space-between',
                                            alignItems:
                                                'center',
                                            gap: '10px',
                                            flexWrap:
                                                'wrap'
                                        }}
                                    >

                                        <div>

                                            <span
                                                style={{
                                                    fontSize:
                                                        '12px',
                                                    color:
                                                        '#64748b'
                                                }}
                                            >
                                                Uploaded file
                                            </span>

                                            <strong
                                                style={{
                                                    display:
                                                        'block',
                                                    fontSize:
                                                        '13px',
                                                    color:
                                                        '#0f172a',
                                                    marginTop:
                                                        '2px'
                                                }}
                                            >
                                                {
                                                    uploadedDocument.originalFileName ||
                                                    uploadedDocument.fileName
                                                }
                                            </strong>

                                        </div>


                                        <button
                                            type="button"
                                            onClick={() =>
                                                handleDownload(
                                                    uploadedDocument.id
                                                )
                                            }
                                            style={{
                                                padding:
                                                    '7px 12px',
                                                border:
                                                    '1px solid #2563eb',
                                                borderRadius:
                                                    '6px',
                                                background:
                                                    '#ffffff',
                                                color:
                                                    '#2563eb',
                                                fontWeight:
                                                    '600',
                                                cursor:
                                                    'pointer'
                                            }}
                                        >
                                            View / Download
                                        </button>

                                    </div>

                                )}

                            </div>
                        );

                    })}

                </div>


                {/* ================= IMPORTANT NOTE ================= */}

                <div
                    style={{
                        marginTop: '18px',
                        padding: '12px 14px',
                        borderRadius: '7px',
                        background: '#eff6ff',
                        color: '#1e40af',
                        fontSize: '13px',
                        lineHeight: '1.5'
                    }}
                >
                    <strong>Note:</strong>{' '}
                    These documents are linked to this specific
                    application and disbursement stage. Stage 1
                    identity, bank and sanction documents do not
                    need to be uploaded again.
                </div>

            </div>
        );
    };


    // ═══════════════════════════════════════════════════════════════════════
    // LOADING
    // ═══════════════════════════════════════════════════════════════════════

    if (loading) {

        return (
            <div
                style={{
                    minHeight: '100vh',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '18px'
                }}
            >
                Loading your applications...
            </div>
        );

    }


    // ═══════════════════════════════════════════════════════════════════════
    // PAGE
    // ═══════════════════════════════════════════════════════════════════════

    return (

        <div
            style={{
                minHeight: '100vh',
                background: '#f8fafc'
            }}
        >

            {/* ================= HEADER ================= */}

            <header
                style={{
                    background: '#ffffff',
                    borderBottom:
                        '1px solid #e2e8f0',
                    padding: '18px 30px'
                }}
            >

                <div
                    style={{
                        maxWidth: '1100px',
                        margin: '0 auto',
                        display: 'flex',
                        justifyContent:
                            'space-between',
                        alignItems: 'center'
                    }}
                >

                    <Link
                        to="/dashboard"
                        style={{
                            textDecoration: 'none',
                            color: '#1e3a8a'
                        }}
                    >

                        <h1
                            style={{
                                margin: 0
                            }}
                        >
                            DSGP
                        </h1>

                        <p
                            style={{
                                margin: '3px 0 0',
                                color: '#64748b'
                            }}
                        >
                            Digital Subsidy & Grant Platform
                        </p>

                    </Link>


                    <Link
                        to="/dashboard"
                        style={{
                            textDecoration: 'none',
                            color: '#1e3a8a',
                            fontWeight: '600'
                        }}
                    >
                        ← Dashboard
                    </Link>

                </div>

            </header>


            {/* ================= MAIN ================= */}

            <main
                style={{
                    maxWidth: '1100px',
                    margin: '0 auto',
                    padding: '40px 20px'
                }}
            >

                <div
                    style={{
                        marginBottom: '30px'
                    }}
                >

                    <h2
                        style={{
                            margin: 0,
                            color: '#0f172a'
                        }}
                    >
                        My Applications
                    </h2>

                    <p
                        style={{
                            color: '#64748b',
                            marginTop: '8px'
                        }}
                    >
                        View and track all your submitted
                        government scheme applications.
                    </p>

                </div>


                {/* ================= ERROR ================= */}

                {error && (

                    <div
                        style={{
                            padding: '15px',
                            marginBottom: '20px',
                            borderRadius: '8px',
                            background: '#fee2e2',
                            color: '#991b1b'
                        }}
                    >
                        {error}
                    </div>

                )}


                {/* ================= NO APPLICATIONS ================= */}

                {!error &&
                    applications.length === 0 && (

                        <div
                            style={{
                                background: '#ffffff',
                                padding: '45px 25px',
                                borderRadius: '12px',
                                border:
                                    '1px solid #e2e8f0',
                                textAlign: 'center'
                            }}
                        >

                            <div
                                style={{
                                    fontSize: '45px'
                                }}
                            >
                                📋
                            </div>

                            <h3
                                style={{
                                    color: '#0f172a'
                                }}
                            >
                                No Applications Yet
                            </h3>

                            <p
                                style={{
                                    color: '#64748b'
                                }}
                            >
                                You have not submitted any
                                scheme applications.
                            </p>

                            <Link
                                to="/schemes"
                                style={{
                                    display:
                                        'inline-block',
                                    marginTop: '15px',
                                    padding:
                                        '11px 20px',
                                    background:
                                        '#2563eb',
                                    color:
                                        '#ffffff',
                                    borderRadius:
                                        '7px',
                                    textDecoration:
                                        'none',
                                    fontWeight:
                                        '600'
                                }}
                            >
                                Browse Schemes
                            </Link>

                        </div>

                    )}


                {/* ================= APPLICATION LIST ================= */}

                {applications.length > 0 && (

                    <div
                        style={{
                            display: 'grid',
                            gap: '20px'
                        }}
                    >

                        {applications.map(
                            (application) => (

                                <div
                                    key={
                                        application.applicationId
                                    }
                                    style={{
                                        background:
                                            '#ffffff',
                                        border:
                                            '1px solid #e2e8f0',
                                        borderRadius:
                                            '12px',
                                        padding: '25px',
                                        boxShadow:
                                            '0 2px 6px rgba(0,0,0,0.04)'
                                    }}
                                >

                                    {/* ================= TOP ROW ================= */}

                                    <div
                                        style={{
                                            display:
                                                'flex',
                                            justifyContent:
                                                'space-between',
                                            alignItems:
                                                'flex-start',
                                            gap: '15px',
                                            flexWrap:
                                                'wrap'
                                        }}
                                    >

                                        <div>

                                            <h3
                                                style={{
                                                    margin: 0,
                                                    color:
                                                        '#0f172a'
                                                }}
                                            >
                                                {
                                                    application.schemeName ||
                                                    'Government Scheme'
                                                }
                                            </h3>

                                            <p
                                                style={{
                                                    margin:
                                                        '7px 0 0',
                                                    color:
                                                        '#64748b'
                                                }}
                                            >
                                                Application ID: #
                                                {
                                                    application.applicationId
                                                }
                                            </p>

                                        </div>


                                        <span
                                            className={
                                                getStatusClass(
                                                    application.applicationStatus
                                                )
                                            }
                                            style={{
                                                padding:
                                                    '7px 13px',
                                                borderRadius:
                                                    '20px',
                                                fontSize:
                                                    '13px',
                                                fontWeight:
                                                    '700',
                                                background:
                                                    application.applicationStatus
                                                        ?.toUpperCase()
                                                        .includes(
                                                            'APPROVED'
                                                        )
                                                        ? '#dcfce7'
                                                        : application.applicationStatus
                                                            ?.toUpperCase()
                                                            .includes(
                                                                'REJECTED'
                                                            )
                                                            ? '#fee2e2'
                                                            : '#fef3c7',
                                                color:
                                                    application.applicationStatus
                                                        ?.toUpperCase()
                                                        .includes(
                                                            'APPROVED'
                                                        )
                                                        ? '#166534'
                                                        : application.applicationStatus
                                                            ?.toUpperCase()
                                                            .includes(
                                                                'REJECTED'
                                                            )
                                                            ? '#991b1b'
                                                            : '#92400e'
                                            }}
                                        >
                                            {
                                                application.applicationStatus ||
                                                'PENDING'
                                            }
                                        </span>

                                    </div>


                                    {/* ================= APPLICATION DETAILS ================= */}

                                    <div
                                        style={{
                                            display:
                                                'grid',
                                            gridTemplateColumns:
                                                'repeat(auto-fit, minmax(200px, 1fr))',
                                            gap: '18px',
                                            marginTop:
                                                '25px',
                                            paddingTop:
                                                '20px',
                                            borderTop:
                                                '1px solid #e2e8f0'
                                        }}
                                    >

                                        <div>

                                            <small
                                                style={{
                                                    color:
                                                        '#64748b'
                                                }}
                                            >
                                                Application Date
                                            </small>

                                            <strong
                                                style={{
                                                    display:
                                                        'block',
                                                    marginTop:
                                                        '5px',
                                                    color:
                                                        '#0f172a'
                                                }}
                                            >
                                                {
                                                    formatDate(
                                                        application.applicationDate
                                                    )
                                                }
                                            </strong>

                                        </div>


                                        <div>

                                            <small
                                                style={{
                                                    color:
                                                        '#64748b'
                                                }}
                                            >
                                                Eligibility Score
                                            </small>

                                            <strong
                                                style={{
                                                    display:
                                                        'block',
                                                    marginTop:
                                                        '5px',
                                                    color:
                                                        '#0f172a'
                                                }}
                                            >
                                                {
                                                    application.eligibilityScore ??
                                                    '—'
                                                }
                                            </strong>

                                        </div>


                                        <div>

                                            <small
                                                style={{
                                                    color:
                                                        '#64748b'
                                                }}
                                            >
                                                Sanctioned Amount
                                            </small>

                                            <strong
                                                style={{
                                                    display:
                                                        'block',
                                                    marginTop:
                                                        '5px',
                                                    color:
                                                        '#0f172a'
                                                }}
                                            >
                                                {
                                                    formatAmount(
                                                        application.sanctionedAmount
                                                    )
                                                }
                                            </strong>

                                        </div>

                                    </div>


                                    {/* ================= BENEFICIARY STAGE UPLOAD ================= */}

                                    {renderStageUploadSection(
                                        application
                                    )}


                                    {/* ================= ACTION ================= */}

                                    <div
                                        style={{
                                            marginTop:
                                                '25px',
                                            display:
                                                'flex',
                                            justifyContent:
                                                'flex-end'
                                        }}
                                    >

                                        <Link
                                            to={`/track?applicationId=${application.applicationId}`}
                                            style={{
                                                padding:
                                                    '11px 20px',
                                                background:
                                                    '#2563eb',
                                                color:
                                                    '#ffffff',
                                                borderRadius:
                                                    '7px',
                                                textDecoration:
                                                    'none',
                                                fontWeight:
                                                    '600'
                                            }}
                                        >
                                            View & Track →
                                        </Link>

                                    </div>

                                </div>

                            )
                        )}

                    </div>

                )}

            </main>

        </div>

    );
}

export default MyApplications;