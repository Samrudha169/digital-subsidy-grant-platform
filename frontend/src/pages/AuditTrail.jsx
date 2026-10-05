import React, { useEffect, useMemo, useState } from "react";
import "./AuditTrail.css";

const API_BASE = "http://localhost:8080/api/v1";

const ALLOWED_ROLES = [
    "ADMIN",
    "FINANCE_APPROVER",
    "DISTRICT_OFFICER"
];

const AuditTrail = () => {

    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [search, setSearch] = useState("");
    const [actionFilter, setActionFilter] = useState("ALL");

    const officerRole = localStorage.getItem("officerRole");

    const isAuthorized = ALLOWED_ROLES.includes(officerRole);

    useEffect(() => {

        if (!isAuthorized) {
            setLoading(false);
            return;
        }

        loadAuditLogs();

    }, [isAuthorized]);

    const loadAuditLogs = async () => {

        try {

            setLoading(true);
            setError("");

            const response = await fetch(
                `${API_BASE}/audit-logs`
            );

            if (!response.ok) {
                throw new Error("Unable to load audit logs.");
            }

            const data = await response.json();

            setLogs(Array.isArray(data) ? data : []);

        } catch (err) {

            console.error("Audit log error:", err);

            setError(
                err.message ||
                "Unable to load audit logs."
            );

        } finally {

            setLoading(false);
        }
    };

    const formatDateTime = (timestamp) => {

        if (!timestamp) return "-";

        const date = new Date(timestamp);

        return date.toLocaleString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    };

    const formatAction = (action) => {

        if (!action) return "Unknown Action";

        return action
            .replaceAll("_", " ")
            .toLowerCase()
            .replace(/\b\w/g, (letter) =>
                letter.toUpperCase()
            );
    };

    const filteredLogs = useMemo(() => {

        return logs.filter((log) => {

            const searchText = search
                .trim()
                .toLowerCase();

            const matchesSearch =
                !searchText ||
                log.officerName?.toLowerCase().includes(searchText) ||
                log.officerUsername?.toLowerCase().includes(searchText) ||
                log.action?.toLowerCase().includes(searchText) ||
                log.entityType?.toLowerCase().includes(searchText) ||
                log.description?.toLowerCase().includes(searchText);

            const matchesAction =
                actionFilter === "ALL" ||
                log.action === actionFilter;

            return matchesSearch && matchesAction;
        });

    }, [logs, search, actionFilter]);

    const actions = [
        ...new Set(
            logs
                .map((log) => log.action)
                .filter(Boolean)
        )
    ];

    const uniqueOfficers = new Set(
        logs
            .map((log) => log.officerId)
            .filter(Boolean)
    ).size;

    if (!isAuthorized) {

        return (
            <div className="audit-page audit-access-denied">

                <div className="audit-denied-card">

                    <div className="audit-denied-icon">
                        🔒
                    </div>

                    <h2>Access Restricted</h2>

                    <p>
                        You are not authorized to view
                        the Audit Trail.
                    </p>

                    <button
                        onClick={() => {
                            window.location.href =
                                "/officer/dashboard";
                        }}
                    >
                        Back to Dashboard
                    </button>

                </div>

            </div>
        );
    }

    return (
        <div className="audit-page">

            {/* HEADER */}

            <div className="audit-header">

                <div>

                    <div className="audit-breadcrumb">
                        Officer Portal / Audit Trail
                    </div>

                    <h1>
                        Audit Trail
                    </h1>

                    <p>
                        Monitor important actions performed
                        across the DSGP platform.
                    </p>

                </div>

                <button
                    className="audit-refresh-btn"
                    onClick={loadAuditLogs}
                    disabled={loading}
                >
                    ↻ {loading ? "Refreshing..." : "Refresh"}
                </button>

            </div>


            {/* SUMMARY CARDS */}

            <div className="audit-summary">

                <div className="audit-stat-card">

                    <div className="audit-stat-icon blue">
                        ◷
                    </div>

                    <div>
                        <span>Total Activities</span>
                        <strong>{logs.length}</strong>
                    </div>

                </div>


                <div className="audit-stat-card">

                    <div className="audit-stat-icon green">
                        ✓
                    </div>

                    <div>
                        <span>Active Officers</span>
                        <strong>{uniqueOfficers}</strong>
                    </div>

                </div>


                <div className="audit-stat-card">

                    <div className="audit-stat-icon purple">
                        ⚡
                    </div>

                    <div>
                        <span>Action Types</span>
                        <strong>{actions.length}</strong>
                    </div>

                </div>

            </div>


            {/* FILTER BAR */}

            <div className="audit-toolbar">

                <div className="audit-search">

                    <span>⌕</span>

                    <input
                        type="text"
                        placeholder="Search officer, action or description..."
                        value={search}
                        onChange={(e) =>
                            setSearch(e.target.value)
                        }
                    />

                </div>


                <select
                    value={actionFilter}
                    onChange={(e) =>
                        setActionFilter(e.target.value)
                    }
                >

                    <option value="ALL">
                        All Actions
                    </option>

                    {actions.map((action) => (

                        <option
                            key={action}
                            value={action}
                        >
                            {formatAction(action)}
                        </option>

                    ))}

                </select>

            </div>


            {/* ERROR */}

            {error && (

                <div className="audit-error">
                    ⚠️ {error}
                </div>

            )}


            {/* LOG TABLE */}

            <div className="audit-card">

                <div className="audit-card-header">

                    <div>

                        <h2>
                            Activity History
                        </h2>

                        <p>
                            {filteredLogs.length} record
                            {filteredLogs.length !== 1
                                ? "s"
                                : ""
                            } found
                        </p>

                    </div>

                </div>


                {loading ? (

                    <div className="audit-loading">

                        <div className="audit-spinner"></div>

                        <p>
                            Loading audit activities...
                        </p>

                    </div>

                ) : filteredLogs.length === 0 ? (

                    <div className="audit-empty">

                        <div>
                            📋
                        </div>

                        <h3>
                            No audit records found
                        </h3>

                        <p>
                            No activities match your
                            current filters.
                        </p>

                    </div>

                ) : (

                    <div className="audit-table-wrapper">

                        <table className="audit-table">

                            <thead>

                            <tr>

                                <th>ACTIVITY</th>
                                <th>OFFICER</th>
                                <th>ROLE</th>
                                <th>ENTITY</th>
                                <th>DESCRIPTION</th>
                                <th>DATE & TIME</th>

                            </tr>

                            </thead>

                            <tbody>

                            {filteredLogs.map((log) => (

                                <tr key={log.id}>

                                    <td>

                                        <div className="audit-action">

                                            <div className="audit-action-icon">
                                                ✓
                                            </div>

                                            <div>

                                                <strong>
                                                    {formatAction(
                                                        log.action
                                                    )}
                                                </strong>

                                                <small>
                                                    #{log.id}
                                                </small>

                                            </div>

                                        </div>

                                    </td>


                                    <td>

                                        <div className="audit-officer">

                                            <div className="audit-avatar">
                                                {(log.officerName ||
                                                    "U")
                                                    .charAt(0)
                                                    .toUpperCase()}
                                            </div>

                                            <div>

                                                <strong>
                                                    {log.officerName ||
                                                        "Unknown Officer"}
                                                </strong>

                                                <small>
                                                    {log.officerUsername ||
                                                        "-"}
                                                </small>

                                            </div>

                                        </div>

                                    </td>


                                    <td>

                                            <span className="audit-role">
                                                {log.role ||
                                                    "-"}
                                            </span>

                                    </td>


                                    <td>

                                        <div className="audit-entity">

                                            <strong>
                                                {log.entityType ||
                                                    "-"}
                                            </strong>

                                            {log.entityId && (
                                                <small>
                                                    ID #{log.entityId}
                                                </small>
                                            )}

                                        </div>

                                    </td>


                                    <td>

                                            <span className="audit-description">
                                                {log.description ||
                                                    "-"}
                                            </span>

                                    </td>


                                    <td>

                                            <span className="audit-time">
                                                {formatDateTime(
                                                    log.timestamp
                                                )}
                                            </span>

                                    </td>

                                </tr>

                            ))}

                            </tbody>

                        </table>

                    </div>

                )}

            </div>

        </div>
    );
};

export default AuditTrail;