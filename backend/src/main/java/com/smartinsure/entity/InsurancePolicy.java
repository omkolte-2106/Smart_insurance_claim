package com.smartinsure.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartinsure.entity.enums.PolicyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "insurance_policies", indexes = {
        @Index(name = "idx_policy_number", columnList = "policyNumber", unique = true),
        @Index(name = "idx_policy_company", columnList = "company_id"),
        @Index(name = "idx_policy_customer", columnList = "customer_id")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "company", "customer", "vehicle", "claims" })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyProfile company;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, unique = true, length = 48)
    private String policyNumber;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sumInsured;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal annualPremium;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Claim> claims = new ArrayList<>();
}
