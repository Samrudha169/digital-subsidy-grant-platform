package com.dsgp.notification.service;

import com.dsgp.beneficiary.repository.BeneficiaryRepository;
import com.dsgp.notification.entity.Notification;
import com.dsgp.notification.entity.NotificationType;
import com.dsgp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final BeneficiaryRepository beneficiaryRepository;

    @Override
    @Transactional
    public Notification createNotification(
            Integer beneficiaryId,
            Long applicationId,
            Integer stageNumber,
            NotificationType type,
            String title,
            String message) {

        if (beneficiaryId == null) {
            throw new IllegalArgumentException(
                    "Beneficiary ID cannot be null."
            );
        }

        if (!beneficiaryRepository.existsById(beneficiaryId)) {
            throw new IllegalArgumentException(
                    "Beneficiary not found: " + beneficiaryId
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Notification type cannot be null."
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification title cannot be empty."
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification message cannot be empty."
            );
        }

        Notification notification = Notification.builder()
                .beneficiaryId(beneficiaryId)
                .applicationId(applicationId)
                .stageNumber(stageNumber)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByBeneficiary(
            Integer beneficiaryId) {

        if (!beneficiaryRepository.existsById(beneficiaryId)) {
            throw new IllegalArgumentException(
                    "Beneficiary not found: " + beneficiaryId
            );
        }

        return notificationRepository
                .findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer beneficiaryId) {

        if (!beneficiaryRepository.existsById(beneficiaryId)) {
            throw new IllegalArgumentException(
                    "Beneficiary not found: " + beneficiaryId
            );
        }

        return notificationRepository
                .countByBeneficiaryIdAndReadFalse(beneficiaryId);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Notification not found: " + notificationId
                        ));

        notification.setRead(true);

        return notificationRepository.save(notification);
    }
}