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
// APPLICATION VERIFICATION
// ============================================================

export const verifyApplication = async (
    applicationId,
    verificationData
) => {
    const response = await fetch(
        `${API_BASE_URL}/applications/${applicationId}/verify`,
        {
            method: 'POST',

            headers: {
                'Content-Type': 'application/json'
            },

            body: JSON.stringify(verificationData)
        }
    );

    if (!response.ok) {
        throw new Error(
            `Application verification failed. Status: ${response.status}`
        );
    }

    return await response.json();
};


export const requestReVerification = async (
    applicationId,
    remarks
) => {
    const url =
        `${API_BASE_URL}/applications/${applicationId}/reverify` +
        (remarks
            ? `?remarks=${encodeURIComponent(remarks)}`
            : '');

    const response = await fetch(url, {
        method: 'POST'
    });

    if (!response.ok) {
        throw new Error(
            `Re-verification request failed. Status: ${response.status}`
        );
    }

    return await response.json();
};