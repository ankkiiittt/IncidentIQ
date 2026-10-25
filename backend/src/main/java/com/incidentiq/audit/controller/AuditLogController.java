package com.incidentiq.audit.controller;

import com.incidentiq.audit.dto.AuditLogResponse;
import com.incidentiq.audit.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Logs", description = "Append-only immutable system audit logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @Operation(summary = "Browse audit logs with pagination and optional filters")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID userId,
            @PageableDefault(size = 50) Pageable pageable) {

        Page<AuditLogResponse> logs;
        if (action != null && !action.isBlank()) {
            logs = auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable).map(AuditLogResponse::fromEntity);
        } else if (userId != null) {
            logs = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(AuditLogResponse::fromEntity);
        } else {
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(AuditLogResponse::fromEntity);
        }

        return ResponseEntity.ok(logs);
    }
}
