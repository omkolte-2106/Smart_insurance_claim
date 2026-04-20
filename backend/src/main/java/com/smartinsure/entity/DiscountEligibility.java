package com.smartinsure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "discount_eligibility", indexes = {
        @Index(name = "idx_discount_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountEligibility extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    /** Percentile rank among all customers (0–100). Higher is better for loyalty programs. */
    @Column(nullable = false)
    private double percentileRank;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal suggestedDiscountPercent;

    @Column(length = 800)
    private String rationaleJson;

    @Column(length = 200)
    private String campaignCode;
}
