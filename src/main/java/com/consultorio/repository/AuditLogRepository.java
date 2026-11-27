package com.consultorio.repository;

import com.consultorio.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Append-only repository for security and clinical audit logs (ISO/IEC 27001 A.8.15).
 * Intentionally exposes only insert and read operations (no delete or bulk update methods).
 */
@org.springframework.stereotype.Repository
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog auditLog);

    Page<AuditLog> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);

    Page<AuditLog> findByEventTypeOrderByOccurredAtDescIdDesc(String eventType, Pageable pageable);
}
