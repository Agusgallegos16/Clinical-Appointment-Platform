package com.consultorio.service;

import com.consultorio.domain.AuditLog;
import com.consultorio.repository.AuditLogRepository;
import com.consultorio.util.LogSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(String eventType,
                           String outcome,
                           Long actorUserId,
                           String actorRole,
                           String targetType,
                           String targetId,
                           String summary) {
        String sanitizedSummary = LogSanitizer.redactTokens(summary);
        if (sanitizedSummary != null && sanitizedSummary.length() > 255) {
            sanitizedSummary = sanitizedSummary.substring(0, 255);
        }

        AuditLog entry = AuditLog.builder()
                .occurredAt(LocalDateTime.now())
                .eventType(eventType)
                .outcome(outcome)
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .targetType(targetType)
                .targetId(targetId)
                .summary(sanitizedSummary)
                .build();
        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listarAuditoria(String eventType, Pageable pageable) {
        if (eventType != null && !eventType.isBlank()) {
            return auditLogRepository.findByEventTypeOrderByOccurredAtDescIdDesc(eventType.trim(), pageable);
        }
        return auditLogRepository.findAllByOrderByOccurredAtDescIdDesc(pageable);
    }
}
