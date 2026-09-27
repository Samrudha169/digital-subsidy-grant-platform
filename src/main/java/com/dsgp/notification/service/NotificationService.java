package com.dsgp.notification.service;

import com.dsgp.notification.entity.Notification;
import com.dsgp.notification.entity.NotificationType;

import java.util.List;

public interface NotificationService {

    Notification createNotification(
            Integer beneficiaryId,
            Long applicationId,
            Integer stageNumber,
            NotificationType type,
            String title,
            String message
    );

    List<Notification> getNotificationsByBeneficiary(
            Integer beneficiaryId
    );

    long getUnreadCount(Integer beneficiaryId);

    Notification markAsRead(Long notificationId);
}