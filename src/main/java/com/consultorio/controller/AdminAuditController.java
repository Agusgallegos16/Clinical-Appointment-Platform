package com.consultorio.controller;

import com.consultorio.domain.AuditLog;
import com.consultorio.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auditoria")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Auditoría de Seguridad", description = "Consulta de solo lectura del registro append-only de eventos de seguridad y clínicos (Exclusivo ADMIN).")
public class AdminAuditController {

    private final AuditLogService auditLogService;

    @Autowired
    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Listar eventos de auditoría paginados (Exclusivo ADMIN)")
    public ResponseEntity<Page<AuditLog>> listarAuditoria(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        return ResponseEntity.ok(auditLogService.listarAuditoria(eventType, PageRequest.of(safePage, safeSize)));
    }
}
