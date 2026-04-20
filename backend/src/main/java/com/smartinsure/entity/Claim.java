package com.smartinsure.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartinsure.entity.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_public_id", columnList = "claimPublicId", unique = true),
        @Index(name = "idx_claim_company", columnList = "company_id"),
        @Index(name = "idx_claim_customer", columnList = "customer_id"),
        @Index(name = "idx_claim_status", columnList = "status")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "policy", "customer", "company", "documents", "verificationResults", "fraudFlags", "payout", "remarks"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable claim id shown to customers (e.g. CLM-2026-000045). */
    @Column(nullable = false, unique = true, length = 32)
    private String claimPublicId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyProfile company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private ClaimStatus status;

    @Column(length = 2000)
    private String incidentDescription;

    @Column(length = 200)
    private String incidentLocation;

    /** 0–100 aggregate fraud suspicion from ML / rules engine. */
    private Double fraudScore;

    /** Normalized damage severity 0–1 from ML service. */
    private Double damageSeverityScore;

    @Column(precision = 14, scale = 2)
    private BigDecimal estimatedPayoutAmount;

    @Column(nullable = false)
    private boolean fraudFlagged;

    @Column(length = 1000)
    private String damagedParts;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClaimDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VerificationResult> verificationResults = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FraudFlag> fraudFlags = new ArrayList<>();

    @OneToOne(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payout payout;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClaimRemark> remarks = new ArrayList<>();
}
