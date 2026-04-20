package com.smartinsure.entity;

import com.smartinsure.entity.enums.FraudFlagSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fraud_flags", indexes = {
        @Index(name = "idx_fraud_claim", columnList = "claim_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudFlag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_user_id")
    private AppUser raisedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudFlagSource source;

    @Column(nullable = false, length = 1200)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
