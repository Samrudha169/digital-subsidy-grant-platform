const API_BASE_URL = 'http://localhost:8080/api/v1';


// ============================================================
// SCHEMES
// ============================================================

export const getAllSchemes = async () => {
    const response = await fetch(`${API_BASE_URL}/schemes`);

    if (!response.ok) {
        throw new Error(`Failed to fetch schemes. Status: ${response.status}`);
    }

    return await response.json();
};


export const getActiveSchemes = async () => {
    const response = await fetch(`${API_BASE_URL}/schemes/active`);

    if (!response.ok) {
        throw new Error(`Failed to fetch active schemes. Status: ${response.status}`);
    }

    return await response.json();
};


export const getSchemeById = async (schemeId) => {
    const response = await fetch(
        `${API_BASE_URL}/schemes/${schemeId}`
    );

    if (!response.ok) {
        throw new Error(`Failed to fetch scheme. Status: ${response.status}`);
    }

    return await response.json();
};


// ============================================================
// ELIGIBILITY
// ============================================================

export const checkEligibility = async (eligibilityData) => {
    const response = await fetch(
        `${API_BASE_URL}/eligibility/check`,
        {
            method: 'POST',

            headers: {
                'Content-Type': 'application/json'
            },

            body: JSON.stringify(eligibilityData)
        }
    );

    if (!response.ok) {
        throw new Error(
            `Eligibility check failed. Status: ${response.status}`
        );
    }

    return await response.json();
};


// ============================================================
// BENEFICIARY / REGISTRATION
// ============================================================

export const registerBeneficiary = async (beneficiaryData) => {
    const response = await fetch(
        `${API_BASE_URL}/beneficiaries`,
        {
            method: 'POST',

            headers: {
                'Content-Type': 'application/json'
            },

            body: JSON.stringify(beneficiaryData)
        }
    );

    const data = await response.json();

    if (!response.ok) {
        throw new Error(
            data.message || 'Beneficiary registration failed.'
        );
    }

    return data;
};


export const getBeneficiaryById = async (id) => {
    const response = await fetch(
        `${API_BASE_URL}/beneficiaries/${id}`
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch beneficiary. Status: ${response.status}`
        );
    }

    return await response.json();
};


export const getBeneficiaryByGovId = async (govId) => {
    const response = await fetch(
        `${API_BASE_URL}/beneficiaries/gov-id/${govId}`
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch beneficiary. Status: ${response.status}`
        );
    }

    return await response.json();
};


// ============================================================
// APPLICATIONS
// ============================================================

export const submitApplication = async (applicationData) => {
    const response = await fetch(
        `${API_BASE_URL}/applications`,
        {
            method: 'POST',

            headers: {
                'Content-Type': 'application/json'
            },

            body: JSON.stringify(applicationData)
        }
    );

    const data = await response.json();

    if (!response.ok) {
        throw new Error(
            data.message || 'Application submission failed.'
        );
    }

    return data;
};


export const getApplicationById = async (applicationId) => {
    const response = await fetch(
        `${API_BASE_URL}/applications/${applicationId}`
    );

    if (!response.ok) {
        throw new Error(
            `Application not found. Status: ${response.status}`
        );
    }

    return await response.json();
};


// ============================================================
// DOCUMENTS
// ============================================================

/**
 * Uploads a single document file for a beneficiary.
 * Calls POST /beneficiaries/{beneficiaryId}/documents (multipart/form-data).
 *
 * @param {number} beneficiaryId  - the beneficiary's primary key
 * @param {File}   file           - the File object from an <input type="file">
 * @param {string} documentType   - one of AADHAAR | PAN | LAND_RECORD | INCOME_CERTIFICATE | PHOTO | OTHER
 * @param {string} [uploadedBy]   - optional username of the uploader
 * @returns {Promise<DocumentResponse>}
 */
export const uploadBeneficiaryDocument = async (
    beneficiaryId,
    file,
    documentType,
    uploadedBy = ''
) => {
    const form = new FormData();
    form.append('file', file);
    form.append('documentType', documentType);
    if (uploadedBy) form.append('uploadedBy', uploadedBy);

    const response = await fetch(
        `${API_BASE_URL}/beneficiaries/${beneficiaryId}/documents`,
        { method: 'POST', body: form }
    );

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(
            data.message || `Document upload failed. Status: ${response.status}`
        );
    }

    return data;
};


/**
 * Returns all documents submitted for the beneficiary who owns the given application.
 * Calls GET /applications/{applicationId}/documents.
 * Used by the officer dashboard to review supporting documents.
 *
 * @param {number} applicationId
 * @returns {Promise<DocumentResponse[]>}
 */
export const getApplicationDocuments = async (applicationId) => {
    const response = await fetch(
        `${API_BASE_URL}/applications/${applicationId}/documents`
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch documents for application ${applicationId}. Status: ${response.status}`
        );
    }

    return await response.json();
};


/**
 * Returns the URL to stream/download a specific document file.
 * Points to GET /beneficiaries/{beneficiaryId}/documents/{documentId}/download.
 * Open this URL in a new tab or use it as an anchor href so the browser
 * applies the Content-Disposition header set by the backend.
 *
 * @param {number} beneficiaryId
 * @param {number} documentId
 * @returns {string} absolute URL
 */
export const downloadDocumentUrl = (beneficiaryId, documentId) =>
    `${API_BASE_URL}/beneficiaries/${beneficiaryId}/documents/${documentId}/download`;