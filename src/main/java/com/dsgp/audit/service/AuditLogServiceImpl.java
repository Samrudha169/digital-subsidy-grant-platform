package com.dsgp.audit.service;

import com.dsgp.audit.dto.AuditLogResponse;
import com.dsgp.audit.entity.AuditLog;
import com.dsgp.audit.repository.AuditLogRepository;
import com.dsgp.authentication.entity.Officer;
import com.dsgp.authentication.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final OfficerRepository officerRepository;

    @Override
    public AuditLogResponse createAuditLog(
            Long officerId,
            String action,
            String entityType,
            Long entityId,
            String description) {

        Officer officer = officerRepository.findById(officerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Officer not found: " + officerId
                        )
                );

        AuditLog auditLog = AuditLog.builder()
                .officer(officer)
                .role(officer.getRole().name())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();

        AuditLog savedAuditLog = auditLogRepository.save(auditLog);

        return AuditLogResponse.fromEntity(savedAuditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllAuditLogs() {

        return auditLogRepository.findAllByOrderByTimestampDesc()
                .stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByOfficer(Long officerId) {

        return auditLogRepository
                .findByOfficerIdOrderByTimestampDesc(officerId)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogsByAction(String action) {

        return auditLogRepository
                .findByActionOrderByTimestampDesc(action)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }
}