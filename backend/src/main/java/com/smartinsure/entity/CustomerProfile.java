package com.smartinsure.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_profiles")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "user", "vehicles", "policies" })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 400)
    private String address;

    /** Aggregate loyalty / risk score used for discount analytics (0–100). */
    @Column(nullable = false)
    private double loyaltyScore;

    @Builder.Default
    @Column(nullable = false)
    private boolean banned = false;

    @Column(length = 500)
    private String banReason;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InsurancePolicy> policies = new ArrayList<>();
}
