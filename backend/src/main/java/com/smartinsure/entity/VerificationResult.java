package com.smartinsure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "verification_results", indexes = {
        @Index(name = "idx_verif_claim", columnList = "claim_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    /** Matches ML module name, e.g. document_verification_service */
    @Column(nullable = false, length = 80)
    private String moduleName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    private Double score;
}
