package com.dsgp.notification.repository;

import com.dsgp.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByBeneficiaryIdOrderByCreatedAtDesc(
            Integer beneficiaryId
    );

    List<Notification> findByBeneficiaryIdAndReadFalseOrderByCreatedAtDesc(
            Integer beneficiaryId
    );

    long countByBeneficiaryIdAndReadFalse(
            Integer beneficiaryId
    );
}