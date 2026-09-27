import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Notifications.css';

const API_BASE = 'http://localhost:8080/api/v1';

function Notifications() {
    const navigate = useNavigate();

    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const beneficiaryId = localStorage.getItem('beneficiaryId');

    useEffect(() => {
        if (!beneficiaryId) {
            navigate('/login');
            return;
        }

        fetchNotifications();
    }, [beneficiaryId, navigate]);

    const fetchNotifications = async () => {
        try {
            setLoading(true);
            setError('');

            const response = await fetch(
                `${API_BASE}/notifications/beneficiary/${beneficiaryId}`
            );

            if (!response.ok) {
                throw new Error('Failed to load notifications.');
            }

            const data = await response.json();
            setNotifications(data);
        } catch (err) {
            console.error('Error loading notifications:', err);
            setError('Unable to load notifications. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const markAsRead = async (notificationId) => {
        try {
            const response = await fetch(
                `${API_BASE}/notifications/${notificationId}/read`,
                {
                    method: 'PUT'
                }
            );

            if (!response.ok) {
                throw new Error('Failed to mark notification as read.');
            }

            setNotifications((previous) =>
                previous.map((notification) =>
                    notification.id === notificationId
                        ? { ...notification, read: true }
                        : notification
                )
            );
        } catch (err) {
            console.error('Error marking notification as read:', err);
        }
    };

    const handleNotificationClick = async (notification) => {
        if (!notification.read) {
            await markAsRead(notification.id);
        }

        if (
            notification.type === 'DOCUMENT_REQUEST' &&
            notification.applicationId
        ) {
            navigate(
                `/track?applicationId=${notification.applicationId}`
            );
            return;
        }

        if (notification.applicationId) {
            navigate(
                `/track?applicationId=${notification.applicationId}`
            );
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return '';

        return new Date(dateString).toLocaleString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const getNotificationIcon = (type) => {
        switch (type) {
            case 'DOCUMENT_REQUEST':
                return '📄';

            case 'APPLICATION_UPDATE':
                return '📋';

            case 'DISBURSEMENT_UPDATE':
                return '💰';

            default:
                return '🔔';
        }
    };

    if (loading) {
        return (
            <div className="notifications-page">
                <div className="notifications-container">
                    <h1>Notifications</h1>
                    <p className="notifications-loading">
                        Loading notifications...
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="notifications-page">
            <div className="notifications-container">

                <div className="notifications-header">
                    <div>
                        <h1>Notifications</h1>
                        <p>
                            Stay updated about your applications and
                            document requirements.
                        </p>
                    </div>

                    <button
                        className="notification-refresh-btn"
                        onClick={fetchNotifications}
                    >
                        Refresh
                    </button>
                </div>

                {error && (
                    <div className="notifications-error">
                        {error}
                    </div>
                )}

                {!error && notifications.length === 0 && (
                    <div className="notifications-empty">
                        <div className="notifications-empty-icon">
                            🔔
                        </div>

                        <h2>No notifications</h2>

                        <p>
                            You are all caught up. New alerts will
                            appear here.
                        </p>
                    </div>
                )}

                {!error && notifications.length > 0 && (
                    <div className="notifications-list">

                        {notifications.map((notification) => (
                            <div
                                key={notification.id}
                                className={`notification-card ${
                                    notification.read
                                        ? 'notification-read'
                                        : 'notification-unread'
                                }`}
                            >

                                <div className="notification-icon">
                                    {getNotificationIcon(
                                        notification.type
                                    )}
                                </div>

                                <div className="notification-content">

                                    <div className="notification-title-row">
                                        <h2>
                                            {notification.title}
                                        </h2>

                                        {!notification.read && (
                                            <span className="notification-new">
                                                NEW
                                            </span>
                                        )}
                                    </div>

                                    <p className="notification-message">
                                        {notification.message}
                                    </p>

                                    {notification.stageNumber && (
                                        <p className="notification-stage">
                                            Stage {notification.stageNumber}
                                        </p>
                                    )}

                                    <div className="notification-footer">

                                        <span className="notification-date">
                                            {formatDate(
                                                notification.createdAt
                                            )}
                                        </span>

                                        {notification.applicationId && (
                                            <button
                                                className="notification-action"
                                                onClick={() =>
                                                    handleNotificationClick(
                                                        notification
                                                    )
                                                }
                                            >
                                                {notification.type ===
                                                'DOCUMENT_REQUEST'
                                                    ? 'View & Upload →'
                                                    : 'View Details →'}
                                            </button>
                                        )}

                                    </div>
                                </div>
                            </div>
                        ))}

                    </div>
                )}
            </div>
        </div>
    );
}

export default Notifications;