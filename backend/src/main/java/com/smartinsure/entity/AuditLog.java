package com.smartinsure.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "actor_user_id"),
        @Index(name = "idx_audit_entity", columnList = "entityType, entityId")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "actor"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actor;

    @Column(nullable = false, length = 160)
    private String action;

    @Column(nullable = false, length = 80)
    private String entityType;

    @Column(length = 120)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String details;
}
