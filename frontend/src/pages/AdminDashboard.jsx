import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './AdminDashboard.css';

const API_BASE = '/api/v1';

function AdminDashboard() {
    const navigate = useNavigate();

    const officerLoggedIn =
        localStorage.getItem('officerLoggedIn') === 'true';
    const officerRole = localStorage.getItem('officerRole');
    const officerName =
        localStorage.getItem('officerName') || 'Administrator';
    const officerUsername =
        localStorage.getItem('officerUsername') || '';
    const loggedInOfficerId =
        localStorage.getItem('officerId');

    const [schemes, setSchemes] = useState([]);
    const [officers, setOfficers] = useState([]);

    const [loading, setLoading] = useState(true);
    const [officersLoading, setOfficersLoading] = useState(false);

    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const [activeModule, setActiveModule] = useState('overview');

    const [selectedScheme, setSelectedScheme] = useState(null);

    const [ruleForm, setRuleForm] = useState({
        minAge: '',
        maxAge: '',
        maxAnnualIncome: '',
        maxLandHolding: '',
        requiredCategory: '',
        grantAmount: ''
    });

    /* ============================================================
       OFFICER MANAGEMENT STATE
    ============================================================ */

    const [officerSearch, setOfficerSearch] = useState('');

    const [showOfficerModal, setShowOfficerModal] = useState(false);
    const [editingOfficer, setEditingOfficer] = useState(null);
    const [officerSaving, setOfficerSaving] = useState(false);

    const [officerForm, setOfficerForm] = useState({
        username: '',
        password: '',
        fullName: '',
        email: '',
        role: 'FIELD_OFFICER',
        district: ''
    });

    /* ============================================================
       BUDGET MANAGEMENT STATE
    ============================================================ */

    const [budgets, setBudgets] = useState([]);
    const [budgetSearch, setBudgetSearch] = useState('');
    const [budgetsLoading, setBudgetsLoading] = useState(false);
    const [showBudgetModal, setShowBudgetModal] = useState(false);
    const [editingBudget, setEditingBudget] = useState(null);
    const [budgetSaving, setBudgetSaving] = useState(false);

    const [budgetForm, setBudgetForm] = useState({
        schemeId: '',
        region: '',
        allocatedAmount: ''
    });

    /* ============================================================
       INITIAL LOAD
    ============================================================ */

    useEffect(() => {
        if (!officerLoggedIn || officerRole !== 'ADMIN') {
            navigate('/officer/login');
            return;
        }

        loadSchemes();
        loadBudgets();
    }, [officerLoggedIn, officerRole, navigate]);

    /* ============================================================
       SCHEME MANAGEMENT
    ============================================================ */

    const loadSchemes = async () => {
        try {
            setLoading(true);
            setError('');

            const response = await fetch(
                `${API_BASE}/schemes/admin/all`
            );

            if (!response.ok) {
                throw new Error('Failed to load schemes');
            }

            const data = await response.json();
            setSchemes(data || []);
        } catch (err) {
            console.error(err);
            setError('Unable to load schemes.');
        } finally {
            setLoading(false);
        }
    };

    const openEligibilityRules = () => {
        setActiveModule('eligibility');
        setSelectedScheme(null);
        setSuccess('');
        setError('');
    };

    const selectScheme = (scheme) => {
        setSelectedScheme(scheme);

        setRuleForm({
            minAge: scheme.minAge ?? '',
            maxAge: scheme.maxAge ?? '',
            maxAnnualIncome: scheme.maxAnnualIncome ?? '',
            maxLandHolding: scheme.maxLandHolding ?? '',
            requiredCategory: scheme.requiredCategory ?? '',
            grantAmount: scheme.grantAmount ?? ''
        });

        setSuccess('');
        setError('');
    };

    const handleRuleChange = (e) => {
        const { name, value } = e.target;

        setRuleForm(previous => ({
            ...previous,
            [name]: value
        }));
    };

    const saveRules = async (e) => {
        e.preventDefault();

        if (!selectedScheme) {
            setError('Please select a scheme first.');
            return;
        }

        if (
            ruleForm.minAge === '' ||
            ruleForm.maxAge === '' ||
            ruleForm.maxAnnualIncome === '' ||
            ruleForm.maxLandHolding === '' ||
            ruleForm.grantAmount === ''
        ) {
            setError('Please fill all required rule fields.');
            return;
        }

        if (Number(ruleForm.minAge) > Number(ruleForm.maxAge)) {
            setError(
                'Minimum age cannot be greater than maximum age.'
            );
            return;
        }

        try {
            setError('');
            setSuccess('');

            const updatedScheme = {
                schemeName: selectedScheme.schemeName,
                description: selectedScheme.description,
                minAge: Number(ruleForm.minAge),
                maxAge: Number(ruleForm.maxAge),
                maxAnnualIncome: Number(ruleForm.maxAnnualIncome),
                maxLandHolding: Number(ruleForm.maxLandHolding),
                requiredCategory: ruleForm.requiredCategory,
                grantAmount: Number(ruleForm.grantAmount),
                active: selectedScheme.active
            };

            const response = await fetch(
                `${API_BASE}/schemes/${selectedScheme.id}`,
                {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(updatedScheme)
                }
            );

            if (!response.ok) {
                throw new Error(
                    'Failed to update eligibility rules'
                );
            }

            const savedScheme = await response.json();

            setSchemes(previous =>
                previous.map(scheme =>
                    scheme.id === savedScheme.id
                        ? savedScheme
                        : scheme
                )
            );

            setSelectedScheme(savedScheme);

            setSuccess(
                `Eligibility and grant rules updated successfully for "${savedScheme.schemeName}".`
            );
        } catch (err) {
            console.error(err);
            setError(
                'Unable to update eligibility rules.'
            );
        }
    };

    /* ============================================================
       OFFICER MANAGEMENT
    ============================================================ */

    const loadOfficers = async () => {
        try {
            setOfficersLoading(true);
            setError('');

            const response = await fetch(
                `${API_BASE}/admin/officers`
            );

            if (!response.ok) {
                throw new Error('Failed to load officers');
            }

            const data = await response.json();
            setOfficers(data || []);
        } catch (err) {
            console.error(err);
            setError('Unable to load officers.');
        } finally {
            setOfficersLoading(false);
        }
    };

    const openOfficerManagement = () => {
        setActiveModule('officers');
        setSuccess('');
        setError('');
        loadOfficers();
    };

    const openAddOfficerModal = () => {
        setEditingOfficer(null);

        setOfficerForm({
            username: '',
            password: '',
            fullName: '',
            email: '',
            role: 'FIELD_OFFICER',
            district: ''
        });

        setError('');
        setSuccess('');
        setShowOfficerModal(true);
    };

    const openEditOfficerModal = (officer) => {
        setEditingOfficer(officer);

        setOfficerForm({
            username: officer.username || '',
            password: '',
            fullName: officer.fullName || '',
            email: officer.email || '',
            role: officer.role || 'FIELD_OFFICER',
            district: officer.district || ''
        });

        setError('');
        setSuccess('');
        setShowOfficerModal(true);
    };

    const closeOfficerModal = () => {
        if (officerSaving) {
            return;
        }

        setShowOfficerModal(false);
        setEditingOfficer(null);
    };

    const handleOfficerChange = (e) => {
        const { name, value } = e.target;

        setOfficerForm(previous => ({
            ...previous,
            [name]: value
        }));
    };

    const saveOfficer = async (e) => {
        e.preventDefault();

        setError('');
        setSuccess('');

        if (!officerForm.fullName.trim()) {
            setError('Full name is required.');
            return;
        }

        if (!officerForm.username.trim()) {
            setError('Username is required.');
            return;
        }

        if (!editingOfficer && !officerForm.password.trim()) {
            setError('Password is required when creating an officer.');
            return;
        }

        if (!officerForm.role) {
            setError('Please select an officer role.');
            return;
        }

        try {
            setOfficerSaving(true);

            const payload = {
                username: officerForm.username.trim(),
                fullName: officerForm.fullName.trim(),
                email: officerForm.email.trim(),
                role: officerForm.role,
                district: officerForm.district.trim()
            };

            /*
             * Password is required for creation.
             * During editing it is optional, so we only send
             * it when the Admin entered a new password.
             */
            if (
                !editingOfficer ||
                officerForm.password.trim()
            ) {
                payload.password =
                    officerForm.password.trim();
            }

            const url = editingOfficer
                ? `${API_BASE}/admin/officers/${editingOfficer.id}`
                : `${API_BASE}/admin/officers`;

            const method = editingOfficer
                ? 'PUT'
                : 'POST';

            const response = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            const responseText = await response.text();

            if (!response.ok) {
                let message = 'Unable to save officer.';

                try {
                    const errorData =
                        JSON.parse(responseText);

                    if (errorData.message) {
                        message = errorData.message;
                    } else if (errorData.error) {
                        message = errorData.error;
                    }
                } catch {
                    if (responseText) {
                        message = responseText;
                    }
                }

                throw new Error(message);
            }

            const savedOfficer =
                JSON.parse(responseText);

            if (editingOfficer) {
                setOfficers(previous =>
                    previous.map(officer =>
                        officer.id === savedOfficer.id
                            ? savedOfficer
                            : officer
                    )
                );

                setSuccess(
                    `Officer "${savedOfficer.fullName}" updated successfully.`
                );
            } else {
                setOfficers(previous => [
                    ...previous,
                    savedOfficer
                ]);

                setSuccess(
                    `Officer "${savedOfficer.fullName}" created successfully.`
                );
            }

            setShowOfficerModal(false);
            setEditingOfficer(null);
        } catch (err) {
            console.error(err);
            setError(
                err.message || 'Unable to save officer.'
            );
        } finally {
            setOfficerSaving(false);
        }
    };

    const deactivateOfficer = async (officer) => {
        if (
            String(officer.id) ===
            String(loggedInOfficerId)
        ) {
            setError(
                'You cannot deactivate your own Admin account.'
            );
            return;
        }

        const confirmed = window.confirm(
            `Are you sure you want to deactivate "${officer.fullName}"?`
        );

        if (!confirmed) {
            return;
        }

        try {
            setError('');
            setSuccess('');

            const response = await fetch(
                `${API_BASE}/admin/officers/${officer.id}`,
                {
                    method: 'DELETE'
                }
            );

            if (!response.ok) {
                const responseText =
                    await response.text();

                let message =
                    'Unable to deactivate officer.';

                try {
                    const errorData =
                        JSON.parse(responseText);

                    if (errorData.message) {
                        message = errorData.message;
                    }
                } catch {
                    if (responseText) {
                        message = responseText;
                    }
                }

                throw new Error(message);
            }

            const updatedOfficer =
                await response.json();

            setOfficers(previous =>
                previous.map(item =>
                    item.id === updatedOfficer.id
                        ? updatedOfficer
                        : item
                )
            );

            setSuccess(
                `Officer "${updatedOfficer.fullName}" has been deactivated.`
            );
        } catch (err) {
            console.error(err);
            setError(
                err.message ||
                'Unable to deactivate officer.'
            );
        }
    };

    const reactivateOfficer = async (officer) => {
        try {
            setError('');
            setSuccess('');

            const response = await fetch(
                `${API_BASE}/admin/officers/${officer.id}/reactivate`,
                {
                    method: 'PUT'
                }
            );

            if (!response.ok) {
                const responseText =
                    await response.text();

                let message =
                    'Unable to reactivate officer.';

                try {
                    const errorData =
                        JSON.parse(responseText);

                    if (errorData.message) {
                        message = errorData.message;
                    }
                } catch {
                    if (responseText) {
                        message = responseText;
                    }
                }

                throw new Error(message);
            }

            const updatedOfficer =
                await response.json();

            setOfficers(previous =>
                previous.map(item =>
                    item.id === updatedOfficer.id
                        ? updatedOfficer
                        : item
                )
            );

            setSuccess(
                `Officer "${updatedOfficer.fullName}" has been reactivated.`
            );
        } catch (err) {
            console.error(err);
            setError(
                err.message ||
                'Unable to reactivate officer.'
            );
        }
    };

    const filteredOfficers = officers.filter(officer => {
        const search =
            officerSearch.trim().toLowerCase();

        if (!search) {
            return true;
        }

        return (
            (officer.fullName || '')
                .toLowerCase()
                .includes(search) ||
            (officer.username || '')
                .toLowerCase()
                .includes(search) ||
            (officer.email || '')
                .toLowerCase()
                .includes(search) ||
            (officer.role || '')
                .toLowerCase()
                .includes(search) ||
            (officer.district || '')
                .toLowerCase()
                .includes(search)
        );
    });

    /* ============================================================
       BUDGET MANAGEMENT
    ============================================================ */

    const loadBudgets = async () => {
        try {
            setBudgetsLoading(true);
            setError('');

            const response = await fetch(
                `${API_BASE}/admin/budgets`
            );

            if (!response.ok) {
                throw new Error('Failed to load budgets');
            }

            const data = await response.json();
            setBudgets(data || []);
        } catch (err) {
            console.error(err);
            setError('Unable to load budget allocations.');
        } finally {
            setBudgetsLoading(false);
        }
    };

    const openBudgetManagement = () => {
        setActiveModule('budget');
        setSuccess('');
        setError('');
        loadBudgets();
    };

    const openAddBudgetModal = () => {
        setEditingBudget(null);

        setBudgetForm({
            schemeId: '',
            region: '',
            allocatedAmount: ''
        });

        setError('');
        setSuccess('');
        setShowBudgetModal(true);
    };

    const openEditBudgetModal = budget => {
        setEditingBudget(budget);

        setBudgetForm({
            schemeId: budget.schemeId || '',
            region: budget.region || '',
            allocatedAmount: budget.allocatedAmount ?? ''
        });

        setError('');
        setSuccess('');
        setShowBudgetModal(true);
    };

    const closeBudgetModal = () => {
        if (budgetSaving) {
            return;
        }

        setShowBudgetModal(false);
        setEditingBudget(null);
    };

    const handleBudgetChange = e => {
        const { name, value } = e.target;

        setBudgetForm(previous => ({
            ...previous,
            [name]: value
        }));
    };

    const saveBudget = async e => {
        e.preventDefault();

        setError('');
        setSuccess('');

        if (!budgetForm.schemeId) {
            setError('Please select a scheme.');
            return;
        }

        if (!budgetForm.region.trim()) {
            setError('Region is required.');
            return;
        }

        if (
            budgetForm.allocatedAmount === '' ||
            Number(budgetForm.allocatedAmount) <= 0
        ) {
            setError('Allocated amount must be greater than zero.');
            return;
        }

        try {
            setBudgetSaving(true);

            const payload = {
                schemeId: Number(budgetForm.schemeId),
                region: budgetForm.region.trim(),
                allocatedAmount: Number(budgetForm.allocatedAmount)
            };

            const url = editingBudget
                ? `${API_BASE}/admin/budgets/${editingBudget.id}`
                : `${API_BASE}/admin/budgets`;

            const method = editingBudget ? 'PUT' : 'POST';

            const response = await fetch(url, {
                method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            const responseText = await response.text();

            if (!response.ok) {
                let message = 'Unable to save budget allocation.';

                try {
                    const errorData = JSON.parse(responseText);

                    if (errorData.message) {
                        message = errorData.message;
                    } else if (errorData.error) {
                        message = errorData.error;
                    }
                } catch {
                    if (responseText) {
                        message = responseText;
                    }
                }

                throw new Error(message);
            }

            const savedBudget = JSON.parse(responseText);

            if (editingBudget) {
                setBudgets(previous =>
                    previous.map(budget =>
                        budget.id === savedBudget.id
                            ? savedBudget
                            : budget
                    )
                );

                setSuccess(
                    `Budget allocation for ${savedBudget.region} updated successfully.`
                );
            } else {
                setBudgets(previous => [
                    savedBudget,
                    ...previous
                ]);

                setSuccess(
                    `Budget allocation for ${savedBudget.region} created successfully.`
                );
            }

            setShowBudgetModal(false);
            setEditingBudget(null);
        } catch (err) {
            console.error(err);
            setError(
                err.message || 'Unable to save budget allocation.'
            );
        } finally {
            setBudgetSaving(false);
        }
    };

    const deactivateBudget = async budget => {
        const confirmed = window.confirm(
            `Are you sure you want to deactivate the budget allocation for ${budget.schemeName} - ${budget.region}?`
        );

        if (!confirmed) {
            return;
        }

        try {
            setError('');
            setSuccess('');

            const response = await fetch(
                `${API_BASE}/admin/budgets/${budget.id}`,
                {
                    method: 'DELETE'
                }
            );

            if (!response.ok) {
                const responseText = await response.text();
                let message = 'Unable to deactivate budget allocation.';

                try {
                    const errorData = JSON.parse(responseText);
                    if (errorData.message) {
                        message = errorData.message;
                    }
                } catch {
                    if (responseText) {
                        message = responseText;
                    }
                }

                throw new Error(message);
            }

            setBudgets(previous =>
                previous.map(item =>
                    item.id === budget.id
                        ? { ...item, active: false }
                        : item
                )
            );

            setSuccess(
                `Budget allocation for ${budget.region} has been deactivated.`
            );
        } catch (err) {
            console.error(err);
            setError(
                err.message || 'Unable to deactivate budget allocation.'
            );
        }
    };

    const reactivateBudget = async budget => {
        try {
            setError('');
            setSuccess('');

            const response = await fetch(
                `${API_BASE}/admin/budgets/${budget.id}/reactivate`,
                {
                    method: 'PUT'
                }
            );

            const responseText = await response.text();

            if (!response.ok) {
                let message = 'Unable to reactivate budget allocation.';

                try {
                    const errorData = JSON.parse(responseText);
                    if (errorData.message) {
                        message = errorData.message;
                    }
                } catch {
                    if (responseText) {
                        message = responseText;
                    }
                }

                throw new Error(message);
            }

            const updatedBudget = JSON.parse(responseText);

            setBudgets(previous =>
                previous.map(item =>
                    item.id === updatedBudget.id
                        ? updatedBudget
                        : item
                )
            );

            setSuccess(
                `Budget allocation for ${updatedBudget.region} has been reactivated.`
            );
        } catch (err) {
            console.error(err);
            setError(
                err.message || 'Unable to reactivate budget allocation.'
            );
        }
    };

    const filteredBudgets = budgets.filter(budget => {
        const search = budgetSearch.trim().toLowerCase();

        if (!search) {
            return true;
        }

        return (
            (budget.schemeName || '').toLowerCase().includes(search) ||
            (budget.region || '').toLowerCase().includes(search) ||
            String(budget.schemeId || '').includes(search) ||
            (budget.active ? 'active' : 'inactive').includes(search)
        );
    });

    const formatCurrency = amount =>
        `₹${Number(amount || 0).toLocaleString('en-IN', {
            maximumFractionDigits: 2
        })}`;

    const getBudgetUtilization = budget => {
        const allocated = Number(budget.allocatedAmount || 0);
        const used = Number(budget.usedAmount || 0);

        if (allocated <= 0) {
            return 0;
        }

        return Math.min(100, Math.max(0, (used / allocated) * 100));
    };

    const renderBudgetModal = () => {
        if (!showBudgetModal) {
            return null;
        }

        return (
            <div className="admin-modal-backdrop">
                <div className="admin-modal budget-modal">
                    <div className="admin-modal-header">
                        <div>
                            <span>
                                {editingBudget
                                    ? 'EDIT ALLOCATION'
                                    : 'NEW ALLOCATION'}
                            </span>

                            <h2>
                                {editingBudget
                                    ? 'Edit Budget Allocation'
                                    : 'Add Budget Allocation'}
                            </h2>
                        </div>

                        <button
                            className="admin-modal-close"
                            onClick={closeBudgetModal}
                            disabled={budgetSaving}
                        >
                            ×
                        </button>
                    </div>

                    <form
                        className="admin-form"
                        onSubmit={saveBudget}
                    >
                        <div className="admin-form-grid">
                            <div className="admin-field">
                                <label>
                                    Scheme *
                                </label>

                                <select
                                    name="schemeId"
                                    value={budgetForm.schemeId}
                                    onChange={handleBudgetChange}
                                    required
                                >
                                    <option value="">
                                        Select a scheme
                                    </option>

                                    {schemes
                                        .filter(scheme => scheme.active)
                                        .map(scheme => (
                                            <option
                                                key={scheme.id}
                                                value={scheme.id}
                                            >
                                                {scheme.schemeName}
                                            </option>
                                        ))}
                                </select>
                            </div>

                            <div className="admin-field">
                                <label>
                                    Region / District *
                                </label>

                                <input
                                    type="text"
                                    name="region"
                                    value={budgetForm.region}
                                    onChange={handleBudgetChange}
                                    placeholder="e.g. Nashik"
                                    required
                                />
                            </div>

                            <div className="admin-field">
                                <label>
                                    Allocated Amount (₹) *
                                </label>

                                <input
                                    type="number"
                                    name="allocatedAmount"
                                    value={budgetForm.allocatedAmount}
                                    onChange={handleBudgetChange}
                                    placeholder="Enter allocated amount"
                                    min="0.01"
                                    step="0.01"
                                    required
                                />
                            </div>

                            <div className="admin-field">
                                <label>
                                    Used Amount (₹)
                                </label>

                                <input
                                    type="text"
                                    value={formatCurrency(
                                        editingBudget?.usedAmount || 0
                                    )}
                                    disabled
                                />
                            </div>
                        </div>

                        <div className="budget-form-note">
                            Used amount is read-only. It starts at zero and will be connected to actual disbursements later.
                        </div>

                        <div className="admin-form-footer">
                            <button
                                type="button"
                                className="admin-cancel-button"
                                onClick={closeBudgetModal}
                                disabled={budgetSaving}
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                className="admin-primary-button"
                                disabled={budgetSaving}
                            >
                                {budgetSaving
                                    ? 'Saving...'
                                    : editingBudget
                                        ? 'Save Changes'
                                        : 'Create Allocation'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        );
    };

    const renderBudgetManagement = () => {
        const activeBudgets = budgets.filter(budget => budget.active);
        const totalAllocated = activeBudgets.reduce(
            (sum, budget) => sum + Number(budget.allocatedAmount || 0),
            0
        );
        const totalUsed = activeBudgets.reduce(
            (sum, budget) => sum + Number(budget.usedAmount || 0),
            0
        );
        const totalRemaining = activeBudgets.reduce(
            (sum, budget) => sum + Number(budget.remainingAmount || 0),
            0
        );
        const utilization = totalAllocated > 0
            ? (totalUsed / totalAllocated) * 100
            : 0;

        return (
            <div className="admin-section">
                <div className="admin-section-header">
                    <div>
                        <h2>
                            Budget Management
                        </h2>

                        <p>
                            Manage scheme-wise and regional budget allocations.
                        </p>
                    </div>

                    <div className="officer-header-actions">
                        <button
                            className="admin-secondary-btn"
                            onClick={() => setActiveModule('overview')}
                        >
                            ← Back
                        </button>

                        <button
                            className="admin-primary-button"
                            onClick={openAddBudgetModal}
                        >
                            + Add Allocation
                        </button>
                    </div>
                </div>

                <div className="budget-summary-grid">
                    <div className="budget-summary-card">
                        <span>Allocated Budget</span>
                        <strong>{formatCurrency(totalAllocated)}</strong>
                    </div>

                    <div className="budget-summary-card">
                        <span>Used Budget</span>
                        <strong>{formatCurrency(totalUsed)}</strong>
                    </div>

                    <div className="budget-summary-card">
                        <span>Remaining Budget</span>
                        <strong>{formatCurrency(totalRemaining)}</strong>
                    </div>

                    <div className="budget-summary-card">
                        <span>Utilization</span>
                        <strong>{utilization.toFixed(1)}%</strong>
                    </div>
                </div>

                <div className="admin-toolbar">
                    <div className="admin-search">
                        <span>🔍</span>

                        <input
                            type="text"
                            placeholder="Search by scheme, region or status..."
                            value={budgetSearch}
                            onChange={e => setBudgetSearch(e.target.value)}
                        />
                    </div>

                    <span className="admin-result-count">
                        {filteredBudgets.length} Allocation
                        {filteredBudgets.length !== 1 ? 's' : ''}
                    </span>
                </div>

                {budgetsLoading ? (
                    <div className="admin-loading">
                        <div className="admin-spinner"></div>
                        <p>Loading budget allocations...</p>
                    </div>
                ) : filteredBudgets.length === 0 ? (
                    <div className="admin-empty">
                        <span>💰</span>
                        <h3>No budget allocations found</h3>
                        <p>
                            {budgetSearch
                                ? 'Try a different search.'
                                : 'Create the first regional budget allocation.'}
                        </p>
                    </div>
                ) : (
                    <div className="admin-table-wrapper">
                        <table className="admin-table budget-table">
                            <thead>
                            <tr>
                                <th>Scheme</th>
                                <th>Region</th>
                                <th>Allocated</th>
                                <th>Used</th>
                                <th>Remaining</th>
                                <th>Utilization</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                            </thead>

                            <tbody>
                            {filteredBudgets.map(budget => {
                                const utilizationPercentage = getBudgetUtilization(budget);

                                return (
                                    <tr key={budget.id}>
                                        <td>
                                            <div className="budget-scheme-cell">
                                                <strong>
                                                    {budget.schemeName || '—'}
                                                </strong>
                                                <span>
                                                        Scheme #{budget.schemeId || '—'}
                                                    </span>
                                            </div>
                                        </td>

                                        <td>
                                            {budget.region || '—'}
                                        </td>

                                        <td>
                                            {formatCurrency(budget.allocatedAmount)}
                                        </td>

                                        <td>
                                            {formatCurrency(budget.usedAmount)}
                                        </td>

                                        <td className="budget-remaining-cell">
                                            {formatCurrency(budget.remainingAmount)}
                                        </td>

                                        <td>
                                            <div className="budget-utilization">
                                                <div className="budget-progress-track">
                                                    <div
                                                        className="budget-progress-fill"
                                                        style={{
                                                            width: `${utilizationPercentage}%`
                                                        }}
                                                    ></div>
                                                </div>

                                                <span>
                                                        {utilizationPercentage.toFixed(1)}%
                                                    </span>
                                            </div>
                                        </td>

                                        <td>
                                                <span
                                                    className={
                                                        budget.active
                                                            ? 'budget-status active'
                                                            : 'budget-status inactive'
                                                    }
                                                >
                                                    {budget.active
                                                        ? 'Active'
                                                        : 'Inactive'}
                                                </span>
                                        </td>

                                        <td>
                                            <div className="admin-actions">
                                                <button
                                                    type="button"
                                                    className="admin-action-edit"
                                                    onClick={() => openEditBudgetModal(budget)}
                                                >
                                                    Edit
                                                </button>

                                                {budget.active ? (
                                                    <button
                                                        type="button"
                                                        className="admin-action-delete"
                                                        onClick={() => deactivateBudget(budget)}
                                                    >
                                                        Deactivate
                                                    </button>
                                                ) : (
                                                    <button
                                                        type="button"
                                                        className="admin-action-reactivate"
                                                        onClick={() => reactivateBudget(budget)}
                                                    >
                                                        Reactivate
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        );
    };

    /* ============================================================
       LOGOUT
    ============================================================ */

    const logout = () => {
        localStorage.removeItem('officerLoggedIn');
        localStorage.removeItem('officerId');
        localStorage.removeItem('officerUsername');
        localStorage.removeItem('officerName');
        localStorage.removeItem('officerRole');
        localStorage.removeItem('officerDistrict');

        navigate('/officer/login');
    };

    /* ============================================================
       OVERVIEW
    ============================================================ */

    const renderOverview = () => {
        const totalSchemes = schemes.length;

        const activeSchemes =
            schemes.filter(
                scheme => scheme.active
            ).length;

        const inactiveSchemes =
            schemes.filter(
                scheme => !scheme.active
            ).length;

        // Keep the overview amount aligned with Budget Management.
        // Only active budget allocations are included.
        const totalAllocatedBudget = budgets
            .filter(budget => budget.active)
            .reduce(
                (sum, budget) =>
                    sum + Number(budget.allocatedAmount || 0),
                0
            );

        return (
            <>
                <div className="admin-stats">
                    <div className="admin-stat-card">
                        <span className="admin-stat-label">
                            Total Schemes
                        </span>

                        <strong>
                            {totalSchemes}
                        </strong>
                    </div>

                    <div className="admin-stat-card">
                        <span className="admin-stat-label">
                            Active Schemes
                        </span>

                        <strong>
                            {activeSchemes}
                        </strong>
                    </div>

                    <div className="admin-stat-card">
                        <span className="admin-stat-label">
                            Inactive Schemes
                        </span>

                        <strong>
                            {inactiveSchemes}
                        </strong>
                    </div>

                    <div className="admin-stat-card">
                        <span className="admin-stat-label">
                            Total Allocated Budget
                        </span>

                        <strong>
                            ₹
                            {totalAllocatedBudget.toLocaleString(
                                'en-IN'
                            )}
                        </strong>
                    </div>
                </div>

                <div className="admin-module-grid">
                    <button
                        className="admin-module-card active"
                        onClick={() =>
                            setActiveModule('schemes')
                        }
                    >
                        <h3>
                            Scheme Management
                        </h3>

                        <p>
                            Add, edit, activate and
                            deactivate government
                            schemes.
                        </p>
                    </button>

                    <button
                        className="admin-module-card active"
                        onClick={
                            openEligibilityRules
                        }
                    >
                        <h3>
                            Eligibility & Grant Rules
                        </h3>

                        <p>
                            Configure age, income,
                            land, category and grant
                            rules.
                        </p>
                    </button>

                    <button
                        className="admin-module-card active"
                        onClick={
                            openOfficerManagement
                        }
                    >
                        <h3>
                            Officer Management
                        </h3>

                        <p>
                            Manage officer accounts,
                            roles and districts.
                        </p>
                    </button>

                    <button
                        className="admin-module-card active"
                        onClick={openBudgetManagement}
                    >
                        <h3>
                            Budget Management
                        </h3>

                        <p>
                            Manage scheme and regional
                            budget allocation.
                        </p>
                    </button>
                </div>
            </>
        );
    };

    /* ============================================================
       SCHEME MANAGEMENT
    ============================================================ */

    const renderSchemeManagement = () => {
        return (
            <div className="admin-section">
                <div className="admin-section-header">
                    <div>
                        <h2>
                            Scheme Management
                        </h2>

                        <p>
                            Manage government schemes
                            and their availability.
                        </p>
                    </div>

                    <button
                        className="admin-secondary-btn"
                        onClick={() =>
                            setActiveModule(
                                'overview'
                            )
                        }
                    >
                        ← Back
                    </button>
                </div>

                <div className="admin-table-wrapper">
                    <table className="admin-table">
                        <thead>
                        <tr>
                            <th>
                                Scheme
                            </th>

                            <th>
                                Grant Amount
                            </th>

                            <th>
                                Category
                            </th>

                            <th>
                                Status
                            </th>
                        </tr>
                        </thead>

                        <tbody>
                        {schemes.map(
                            scheme => (
                                <tr
                                    key={
                                        scheme.id
                                    }
                                >
                                    <td>
                                        {
                                            scheme.schemeName
                                        }
                                    </td>

                                    <td>
                                        ₹
                                        {Number(
                                            scheme.grantAmount ||
                                            0
                                        ).toLocaleString(
                                            'en-IN'
                                        )}
                                    </td>

                                    <td>
                                        {scheme.requiredCategory ||
                                            'All'}
                                    </td>

                                    <td>
                                            <span
                                                className={
                                                    scheme.active
                                                        ? 'status-active'
                                                        : 'status-inactive'
                                                }
                                            >
                                                {scheme.active
                                                    ? 'Active'
                                                    : 'Inactive'}
                                            </span>
                                    </td>
                                </tr>
                            )
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    };

    /* ============================================================
       ELIGIBILITY & GRANT RULES
    ============================================================ */

    const renderEligibilityRules = () => {
        return (
            <div className="admin-section">
                <div className="admin-section-header">
                    <div>
                        <h2>
                            Eligibility & Grant Rules
                        </h2>

                        <p>
                            Select a scheme and update
                            the eligibility criteria
                            and grant amount.
                        </p>
                    </div>

                    <button
                        className="admin-secondary-btn"
                        onClick={() => {
                            setActiveModule(
                                'overview'
                            );
                            setSelectedScheme(null);
                        }}
                    >
                        ← Back
                    </button>
                </div>

                <div className="eligibility-layout">
                    <div className="scheme-selector-card">
                        <h3>
                            Select Scheme
                        </h3>

                        <div className="scheme-list">
                            {schemes.map(
                                scheme => (
                                    <button
                                        key={
                                            scheme.id
                                        }
                                        className={
                                            selectedScheme?.id ===
                                            scheme.id
                                                ? 'scheme-select-item selected'
                                                : 'scheme-select-item'
                                        }
                                        onClick={() =>
                                            selectScheme(
                                                scheme
                                            )
                                        }
                                    >
                                        <div>
                                            <strong>
                                                {
                                                    scheme.schemeName
                                                }
                                            </strong>

                                            <small>
                                                {scheme.active
                                                    ? 'Active'
                                                    : 'Inactive'}
                                            </small>
                                        </div>

                                        <span>
                                            ›
                                        </span>
                                    </button>
                                )
                            )}
                        </div>
                    </div>

                    <div className="rules-editor-card">
                        {!selectedScheme ? (
                            <div className="empty-rules-state">
                                <h3>
                                    Select a scheme
                                </h3>

                                <p>
                                    Choose a scheme
                                    from the left to
                                    manage its
                                    eligibility and
                                    grant rules.
                                </p>
                            </div>
                        ) : (
                            <>
                                <div className="rules-editor-header">
                                    <div>
                                        <h3>
                                            {
                                                selectedScheme.schemeName
                                            }
                                        </h3>

                                        <p>
                                            Configure
                                            eligibility
                                            conditions
                                            and grant
                                            amount.
                                        </p>
                                    </div>
                                </div>

                                <form
                                    className="rules-form"
                                    onSubmit={
                                        saveRules
                                    }
                                >
                                    <div className="rules-form-grid">
                                        <div className="admin-form-group">
                                            <label>
                                                Minimum Age
                                            </label>

                                            <input
                                                type="number"
                                                name="minAge"
                                                value={
                                                    ruleForm.minAge
                                                }
                                                onChange={
                                                    handleRuleChange
                                                }
                                                min="0"
                                            />
                                        </div>

                                        <div className="admin-form-group">
                                            <label>
                                                Maximum Age
                                            </label>

                                            <input
                                                type="number"
                                                name="maxAge"
                                                value={
                                                    ruleForm.maxAge
                                                }
                                                onChange={
                                                    handleRuleChange
                                                }
                                                min="0"
                                            />
                                        </div>

                                        <div className="admin-form-group">
                                            <label>
                                                Maximum Annual
                                                Income (₹)
                                            </label>

                                            <input
                                                type="number"
                                                name="maxAnnualIncome"
                                                value={
                                                    ruleForm.maxAnnualIncome
                                                }
                                                onChange={
                                                    handleRuleChange
                                                }
                                                min="0"
                                            />
                                        </div>

                                        <div className="admin-form-group">
                                            <label>
                                                Maximum Land
                                                Holding
                                            </label>

                                            <input
                                                type="number"
                                                name="maxLandHolding"
                                                value={
                                                    ruleForm.maxLandHolding
                                                }
                                                onChange={
                                                    handleRuleChange
                                                }
                                                min="0"
                                                step="0.01"
                                            />
                                        </div>

                                        <div className="admin-form-group">
                                            <label>
                                                Required
                                                Category
                                            </label>

                                            <select
                                                name="requiredCategory"
                                                value={
                                                    ruleForm.requiredCategory
                                                }
                                                onChange={
                                                    handleRuleChange
                                                }
                                            >
                                                <option value="">
                                                    All Categories
                                                </option>

                                                <option value="GENERAL">
                                                    General
                                                </option>

                                                <option value="OBC">
                                                    OBC
                                                </option>

                                                <option value="SC">
                                                    SC
                                                </option>

                                                <option value="ST">
                                                    ST
                                                </option>

                                                <option value="EWS">
                                                    EWS
                                                </option>
                                            </select>
                                        </div>

                                        <div className="admin-form-group">
                                            <label>
                                                Grant Amount
                                                (₹)
                                            </label>

                                            <input
                                                type="number"
                                                name="grantAmount"
                                                value={
                                                    ruleForm.grantAmount
                                                }
                                                onChange={
                                                    handleRuleChange
                                                }
                                                min="0"
                                            />
                                        </div>
                                    </div>

                                    <div className="rules-summary">
                                        <div>
                                            <span>
                                                Current Scheme
                                            </span>

                                            <strong>
                                                {
                                                    selectedScheme.schemeName
                                                }
                                            </strong>
                                        </div>

                                        <div>
                                            <span>
                                                Grant Amount
                                            </span>

                                            <strong>
                                                ₹
                                                {Number(
                                                    ruleForm.grantAmount ||
                                                    0
                                                ).toLocaleString(
                                                    'en-IN'
                                                )}
                                            </strong>
                                        </div>

                                        <div>
                                            <span>
                                                Category
                                            </span>

                                            <strong>
                                                {ruleForm.requiredCategory ||
                                                    'All Categories'}
                                            </strong>
                                        </div>
                                    </div>

                                    <button
                                        type="submit"
                                        className="admin-primary-btn"
                                    >
                                        Save Eligibility
                                        Rules
                                    </button>
                                </form>
                            </>
                        )}
                    </div>
                </div>
            </div>
        );
    };

    /* ============================================================
       OFFICER MANAGEMENT UI
    ============================================================ */

    const renderOfficerManagement = () => {
        return (
            <div className="admin-section">
                <div className="admin-section-header">
                    <div>
                        <h2>
                            Officer Management
                        </h2>

                        <p>
                            Create and manage officer
                            accounts, roles and
                            districts.
                        </p>
                    </div>

                    <div className="officer-header-actions">
                        <button
                            className="admin-secondary-btn"
                            onClick={() =>
                                setActiveModule(
                                    'overview'
                                )
                            }
                        >
                            ← Back
                        </button>

                        <button
                            className="admin-primary-button"
                            onClick={
                                openAddOfficerModal
                            }
                        >
                            + Add Officer
                        </button>
                    </div>
                </div>

                <div className="admin-toolbar">
                    <div className="admin-search">
                        <span>
                            🔍
                        </span>

                        <input
                            type="text"
                            placeholder="Search by name, username, role or district..."
                            value={
                                officerSearch
                            }
                            onChange={e =>
                                setOfficerSearch(
                                    e.target.value
                                )
                            }
                        />
                    </div>

                    <span className="admin-result-count">
                        {filteredOfficers.length}{' '}
                        Officer
                        {filteredOfficers.length !==
                        1
                            ? 's'
                            : ''}
                    </span>
                </div>

                {officersLoading ? (
                    <div className="admin-loading">
                        <div className="admin-spinner"></div>

                        <p>
                            Loading officers...
                        </p>
                    </div>
                ) : filteredOfficers.length ===
                0 ? (
                    <div className="admin-empty">
                        <span>
                            👥
                        </span>

                        <h3>
                            No officers found
                        </h3>

                        <p>
                            {officerSearch
                                ? 'Try a different search.'
                                : 'No officer accounts are available yet.'}
                        </p>
                    </div>
                ) : (
                    <div className="admin-table-wrapper">
                        <table className="admin-table officer-table">
                            <thead>
                            <tr>
                                <th>
                                    Officer
                                </th>

                                <th>
                                    Username
                                </th>

                                <th>
                                    Role
                                </th>

                                <th>
                                    District
                                </th>

                                <th>
                                    Status
                                </th>

                                <th>
                                    Actions
                                </th>
                            </tr>
                            </thead>

                            <tbody>
                            {filteredOfficers.map(
                                officer => (
                                    <tr
                                        key={
                                            officer.id
                                        }
                                    >
                                        <td>
                                            <div className="officer-name-cell">
                                                <strong>
                                                    {
                                                        officer.fullName
                                                    }
                                                </strong>

                                                <span>
                                                        {
                                                            officer.email ||
                                                            'No email'
                                                        }
                                                    </span>
                                            </div>
                                        </td>

                                        <td>
                                            {
                                                officer.username
                                            }
                                        </td>

                                        <td>
                                                <span className="officer-role-badge">
                                                    {formatOfficerRole(
                                                        officer.role
                                                    )}
                                                </span>
                                        </td>

                                        <td>
                                            {
                                                officer.district ||
                                                '—'
                                            }
                                        </td>

                                        <td>
                                                <span
                                                    className={
                                                        officer.active
                                                            ? 'admin-status admin-status-active'
                                                            : 'admin-status admin-status-inactive'
                                                    }
                                                >
                                                    {officer.active
                                                        ? 'Active'
                                                        : 'Inactive'}
                                                </span>
                                        </td>

                                        <td>
                                            <div className="admin-actions">
                                                <button
                                                    className="admin-edit-button"
                                                    onClick={() =>
                                                        openEditOfficerModal(
                                                            officer
                                                        )
                                                    }
                                                >
                                                    Edit
                                                </button>

                                                {officer.active ? (
                                                    <button
                                                        className="admin-delete-button"
                                                        onClick={() =>
                                                            deactivateOfficer(
                                                                officer
                                                            )
                                                        }
                                                        disabled={
                                                            String(
                                                                officer.id
                                                            ) ===
                                                            String(
                                                                loggedInOfficerId
                                                            )
                                                        }
                                                        title={
                                                            String(
                                                                officer.id
                                                            ) ===
                                                            String(
                                                                loggedInOfficerId
                                                            )
                                                                ? 'You cannot deactivate your own account'
                                                                : 'Deactivate officer'
                                                        }
                                                    >
                                                        Deactivate
                                                    </button>
                                                ) : (
                                                    <button
                                                        className="admin-reactivate-button"
                                                        onClick={() =>
                                                            reactivateOfficer(
                                                                officer
                                                            )
                                                        }
                                                    >
                                                        Reactivate
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                )
                            )}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        );
    };

    /* ============================================================
       OFFICER MODAL
    ============================================================ */

    const renderOfficerModal = () => {
        if (!showOfficerModal) {
            return null;
        }

        return (
            <div
                className="admin-modal-backdrop"
                onMouseDown={e => {
                    if (
                        e.target ===
                        e.currentTarget
                    ) {
                        closeOfficerModal();
                    }
                }}
            >
                <div
                    className="admin-modal officer-modal"
                    onMouseDown={e =>
                        e.stopPropagation()
                    }
                >
                    <div className="admin-modal-header">
                        <div>
                            <span>
                                OFFICER MANAGEMENT
                            </span>

                            <h2>
                                {editingOfficer
                                    ? 'Edit Officer'
                                    : 'Add Officer'}
                            </h2>
                        </div>

                        <button
                            className="admin-modal-close"
                            onClick={
                                closeOfficerModal
                            }
                            disabled={
                                officerSaving
                            }
                        >
                            ×
                        </button>
                    </div>

                    <form
                        className="admin-form"
                        onSubmit={saveOfficer}
                    >
                        <div className="admin-form-grid">
                            <div className="admin-field">
                                <label>
                                    Full Name *
                                </label>

                                <input
                                    type="text"
                                    name="fullName"
                                    value={
                                        officerForm.fullName
                                    }
                                    onChange={
                                        handleOfficerChange
                                    }
                                    placeholder="Enter full name"
                                    required
                                />
                            </div>

                            <div className="admin-field">
                                <label>
                                    Username *
                                </label>

                                <input
                                    type="text"
                                    name="username"
                                    value={
                                        officerForm.username
                                    }
                                    onChange={
                                        handleOfficerChange
                                    }
                                    placeholder="Enter username"
                                    required
                                />
                            </div>

                            <div className="admin-field">
                                <label>
                                    Email
                                </label>

                                <input
                                    type="email"
                                    name="email"
                                    value={
                                        officerForm.email
                                    }
                                    onChange={
                                        handleOfficerChange
                                    }
                                    placeholder="Enter email address"
                                />
                            </div>

                            <div className="admin-field">
                                <label>
                                    {editingOfficer
                                        ? 'New Password'
                                        : 'Password *'}
                                </label>

                                <input
                                    type="password"
                                    name="password"
                                    value={
                                        officerForm.password
                                    }
                                    onChange={
                                        handleOfficerChange
                                    }
                                    placeholder={
                                        editingOfficer
                                            ? 'Leave blank to keep current password'
                                            : 'Enter password'
                                    }
                                    required={
                                        !editingOfficer
                                    }
                                />
                            </div>

                            <div className="admin-field">
                                <label>
                                    Role *
                                </label>

                                <select
                                    name="role"
                                    value={
                                        officerForm.role
                                    }
                                    onChange={
                                        handleOfficerChange
                                    }
                                    required
                                >
                                    <option value="FIELD_OFFICER">
                                        Field Officer
                                    </option>

                                    <option value="DISTRICT_OFFICER">
                                        District Officer
                                    </option>

                                    <option value="FINANCE_APPROVER">
                                        Finance Approver
                                    </option>

                                    <option value="ADMIN">
                                        Administrator
                                    </option>
                                </select>
                            </div>

                            <div className="admin-field">
                                <label>
                                    District
                                </label>

                                <input
                                    type="text"
                                    name="district"
                                    value={
                                        officerForm.district
                                    }
                                    onChange={
                                        handleOfficerChange
                                    }
                                    placeholder="Enter district"
                                />
                            </div>
                        </div>

                        {editingOfficer && (
                            <div className="officer-password-note">
                                Leave the password blank if
                                you do not want to change the
                                current password.
                            </div>
                        )}

                        <div className="admin-form-footer">
                            <button
                                type="button"
                                className="admin-cancel-button"
                                onClick={
                                    closeOfficerModal
                                }
                                disabled={
                                    officerSaving
                                }
                            >
                                Cancel
                            </button>

                            <button
                                type="submit"
                                className="admin-primary-button"
                                disabled={
                                    officerSaving
                                }
                            >
                                {officerSaving
                                    ? 'Saving...'
                                    : editingOfficer
                                        ? 'Save Changes'
                                        : 'Create Officer'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        );
    };

    /* ============================================================
       COMING SOON
    ============================================================ */

    const renderComingSoon = (
        title,
        description
    ) => {
        return (
            <div className="admin-section">
                <div className="admin-section-header">
                    <div>
                        <h2>
                            {title}
                        </h2>

                        <p>
                            {description}
                        </p>
                    </div>

                    <button
                        className="admin-secondary-btn"
                        onClick={() =>
                            setActiveModule(
                                'overview'
                            )
                        }
                    >
                        ← Back
                    </button>
                </div>

                <div className="admin-coming-soon">
                    <h3>
                        {title}
                    </h3>

                    <p>
                        This module will be implemented
                        next in the Admin Dashboard.
                    </p>
                </div>
            </div>
        );
    };

    /* ============================================================
       HELPER
    ============================================================ */

    const formatOfficerRole = role => {
        if (!role) {
            return '—';
        }

        return role
            .toLowerCase()
            .split('_')
            .map(
                word =>
                    word.charAt(0).toUpperCase() +
                    word.slice(1)
            )
            .join(' ');
    };

    /* ============================================================
       AUTH GUARD
    ============================================================ */

    if (
        !officerLoggedIn ||
        officerRole !== 'ADMIN'
    ) {
        return null;
    }

    /* ============================================================
       MAIN UI
    ============================================================ */

    return (
        <div className="admin-dashboard">
            <header className="admin-topbar">
                <div>
                    <h1>
                        DSGP Admin Dashboard
                    </h1>

                    <p>
                        Digital Subsidy & Grant Platform
                    </p>
                </div>

                <div className="admin-user-area">
                    <div>
                        <strong>
                            {officerName}
                        </strong>

                        <small>
                            {officerUsername} ·
                            Administrator
                        </small>
                    </div>

                    <button
                        className="admin-logout-btn"
                        onClick={logout}
                    >
                        Logout
                    </button>
                </div>
            </header>

            <div className="admin-body">
                <aside className="admin-sidebar">
                    <button
                        className={
                            activeModule ===
                            'overview'
                                ? 'admin-side-btn active'
                                : 'admin-side-btn'
                        }
                        onClick={() =>
                            setActiveModule(
                                'overview'
                            )
                        }
                    >
                        Overview
                    </button>

                    <button
                        className={
                            activeModule ===
                            'schemes'
                                ? 'admin-side-btn active'
                                : 'admin-side-btn'
                        }
                        onClick={() =>
                            setActiveModule(
                                'schemes'
                            )
                        }
                    >
                        Scheme Management
                    </button>

                    <button
                        className={
                            activeModule ===
                            'eligibility'
                                ? 'admin-side-btn active'
                                : 'admin-side-btn'
                        }
                        onClick={
                            openEligibilityRules
                        }
                    >
                        Eligibility & Grant Rules
                    </button>

                    <button
                        className={
                            activeModule ===
                            'officers'
                                ? 'admin-side-btn active'
                                : 'admin-side-btn'
                        }
                        onClick={
                            openOfficerManagement
                        }
                    >
                        Officer Management
                    </button>

                    <button
                        className={
                            activeModule ===
                            'budget'
                                ? 'admin-side-btn active'
                                : 'admin-side-btn'
                        }
                        onClick={
                            openBudgetManagement
                        }
                    >
                        Budget Management
                    </button>

                    <button
                        className="admin-side-btn"
                        onClick={() =>
                            navigate(
                                '/audit-trail'
                            )
                        }
                    >
                        Audit Trail
                    </button>
                </aside>

                <main className="admin-content">
                    {error && (
                        <div className="admin-alert error">
                            {error}
                        </div>
                    )}

                    {success && (
                        <div className="admin-alert success">
                            {success}
                        </div>
                    )}

                    {loading ? (
                        <div className="admin-loading">
                            Loading admin data...
                        </div>
                    ) : (
                        <>
                            {activeModule ===
                                'overview' &&
                                renderOverview()}

                            {activeModule ===
                                'schemes' &&
                                renderSchemeManagement()}

                            {activeModule ===
                                'eligibility' &&
                                renderEligibilityRules()}

                            {activeModule ===
                                'officers' &&
                                renderOfficerManagement()}

                            {activeModule ===
                                'budget' &&
                                renderBudgetManagement()}
                        </>
                    )}
                </main>
            </div>

            {renderOfficerModal()}
            {renderBudgetModal()}
        </div>
    );
}

export default AdminDashboard;
