package com.dsgp.audit.dto;

import com.dsgp.audit.entity.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {

    private Long id;

    private Long officerId;

    private String officerUsername;

    private String officerName;

    private String role;

    private String action;

    private String entityType;

    private Long entityId;

    private String description;

    private LocalDateTime timestamp;

    public static AuditLogResponse fromEntity(AuditLog auditLog) {

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .officerId(auditLog.getOfficer().getId())
                .officerUsername(auditLog.getOfficer().getUsername())
                .officerName(auditLog.getOfficer().getFullName())
                .role(auditLog.getRole())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .description(auditLog.getDescription())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}