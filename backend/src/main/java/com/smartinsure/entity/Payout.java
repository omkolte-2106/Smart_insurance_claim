package com.smartinsure.entity;

import com.smartinsure.entity.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false, unique = true)
    private Claim claim;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal recommendedAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal finalAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status = PayoutStatus.ESTIMATED;

    @Column(length = 1000)
    private String notes;
}
