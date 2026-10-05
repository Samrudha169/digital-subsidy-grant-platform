package com.dsgp.audit.repository;

import com.dsgp.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByOfficerIdOrderByTimestampDesc(Long officerId);

    List<AuditLog> findByActionOrderByTimestampDesc(String action);
}