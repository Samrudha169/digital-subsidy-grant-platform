package com.dsgp.audit.controller;

import com.dsgp.audit.dto.AuditLogResponse;
import com.dsgp.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public List<AuditLogResponse> getAllAuditLogs() {
        return auditLogService.getAllAuditLogs();
    }

    @GetMapping("/officer/{officerId}")
    public List<AuditLogResponse> getAuditLogsByOfficer(
            @PathVariable Long officerId) {
        return auditLogService.getAuditLogsByOfficer(officerId);
    }

    @GetMapping("/action/{action}")
    public List<AuditLogResponse> getAuditLogsByAction(
            @PathVariable String action) {
        return auditLogService.getAuditLogsByAction(action);
    }
}