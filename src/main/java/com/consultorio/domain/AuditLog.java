package com.consultorio.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_log_event_type", columnList = "event_type")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "event_type", nullable = false, updatable = false, length = 64)
    private String eventType;

    @Column(name = "outcome", nullable = false, updatable = false, length = 32)
    private String outcome;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @Column(name = "actor_role", updatable = false, length = 32)
    private String actorRole;

    @Column(name = "target_type", updatable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", updatable = false, length = 64)
    private String targetId;

    @Column(name = "summary", updatable = false, length = 255)
    private String summary;
}
