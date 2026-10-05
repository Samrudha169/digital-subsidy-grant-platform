package com.dsgp.audit.service;

import com.dsgp.audit.dto.AuditLogResponse;

import java.util.List;

public interface AuditLogService {

    AuditLogResponse createAuditLog(
            Long officerId,
            String action,
            String entityType,
            Long entityId,
            String description
    );

    List<AuditLogResponse> getAllAuditLogs();

    List<AuditLogResponse> getAuditLogsByOfficer(Long officerId);

    List<AuditLogResponse> getAuditLogsByAction(String action);
}