package com.dsgp.notification.controller;

import com.dsgp.notification.entity.Notification;
import com.dsgp.notification.entity.NotificationType;
import com.dsgp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Create a notification.
     *
     * This will mainly be used by backend workflows such as:
     * - Document request
     * - Application status update
     * - Disbursement update
     */
    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @RequestParam Integer beneficiaryId,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Integer stageNumber,
            @RequestParam NotificationType type,
            @RequestParam String title,
            @RequestParam String message) {

        Notification notification =
                notificationService.createNotification(
                        beneficiaryId,
                        applicationId,
                        stageNumber,
                        type,
                        title,
                        message
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(notification);
    }

    /**
     * Get all notifications for a beneficiary.
     */
    @GetMapping("/beneficiary/{beneficiaryId}")
    public ResponseEntity<List<Notification>> getNotifications(
            @PathVariable Integer beneficiaryId) {

        return ResponseEntity.ok(
                notificationService
                        .getNotificationsByBeneficiary(beneficiaryId)
        );
    }

    /**
     * Get unread notification count.
     */
    @GetMapping("/beneficiary/{beneficiaryId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Integer beneficiaryId) {

        return ResponseEntity.ok(
                notificationService.getUnreadCount(beneficiaryId)
        );
    }

    /**
     * Mark a notification as read.
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long notificationId) {

        return ResponseEntity.ok(
                notificationService.markAsRead(notificationId)
        );
    }
}